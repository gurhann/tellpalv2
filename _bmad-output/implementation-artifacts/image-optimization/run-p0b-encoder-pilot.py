#!/usr/bin/env python3
"""Execute the approved frozen r3 schedule in a new exclusive private batch."""

import argparse
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from PIL import Image

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("p0b_readback", HERE / "verify-p0b-encoder-pilot.py")
V = importlib.util.module_from_spec(spec)
spec.loader.exec_module(V)


def exclusive_json(path, value):
    """All retained receipts are created once and fsynced before the next stage."""
    V.no_symlinks(path)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "wb") as stream:
        stream.write(V.canonical(value) + b"\n")
        stream.flush()
        os.fsync(stream.fileno())
    os.chmod(path, 0o400)


def exclusive_bytes(path, value):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "wb") as stream:
        stream.write(value)
        stream.flush()
        os.fsync(stream.fileno())
    os.chmod(path, 0o400)


def mkdir(path):
    V.no_symlinks(path)
    path.mkdir(mode=0o700, parents=False, exist_ok=False)


class LimitedWriter:
    """Reject the write that would exceed the cap; retain already written partials."""
    def __init__(self, stream):
        self.stream, self.total = stream, 0

    def write(self, value):
        if self.total + len(value) > V.LIMITS["output_bytes"]:
            raise V.GateFailure("output_byte_limit")
        written = self.stream.write(value)
        self.total += written
        return written

    def flush(self):
        return self.stream.flush()


def encode_worker(request):
    args = argparse.Namespace(**{k: Path(v) for k, v in request["inputs"].items()})
    scope = V.load_json(Path(request["scope"]))
    V.check_integrity(args, scope)
    pair, arm = request["pair"], request["arm"]
    if pair not in scope["planned_pairs"] or arm not in ("control", "candidate"):
        raise V.GateFailure("prerequisite_integrity_failed")
    output = V.safe_batch(Path(request["output"]), V.input_paths(args), new=True)
    # This is an RGB projection of the admitted stream, not another normalization.
    rgb = V.reference_rgb(Path(request["reference"]), pair)
    image = Image.frombytes("RGB", (pair["geometry"]["width"], pair["geometry"]["height"]), rgb)
    settings = {key: value for key, value in pair[arm + "_settings"].items()
                if key not in ("mode", "profile_handling")}
    fd = os.open(output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(fd, "wb") as stream:
            image.save(LimitedWriter(stream), **settings)
            stream.flush()
            os.fsync(stream.fileno())
    finally:
        if output.exists(): os.chmod(output, 0o400)
    print(json.dumps({"status": "PASS"}))


def supervised(command, timeout, directory, stage):
    """One subprocess at a time, bounded without deleting timeout partials."""
    stdout = stderr = b""
    try:
        child = subprocess.run(command, capture_output=True, timeout=timeout)
        stdout, stderr = child.stdout, child.stderr
        status = "success" if child.returncode == 0 else "failure"
    except subprocess.TimeoutExpired as exc:
        stdout, stderr = exc.stdout or b"", exc.stderr or b""
        status = "timeout"
    except OSError as exc:
        stdout = V.canonical({"status": "INVALID", "category": "subprocess_spawn_failure"})
        stderr = ("subprocess_spawn_failure:" + type(exc).__name__).encode("ascii")
        status = "failure"
    logs = {}
    for suffix, raw in (("stdout", stdout), ("stderr", stderr)):
        name = stage + "." + suffix
        exclusive_bytes(directory / name, raw)
        logs[name] = V.sha(raw)
    return {"status": status, "stdout": stdout, "logs": logs}


def input_request(args):
    return {key: str(getattr(args, key)) for key in
            ("source_root", "reference_root", "preparation_receipt", "freeze_manifest", "corpus", "protocol")}


def execute_arm(scope, row, batch, args, *, invoke=supervised, integrity=None):
    """Retry output failure once; integrity failure stops the whole batch."""
    pair = next(p for p in scope["planned_pairs"] if p["id"] == row["pair_id"])
    if integrity is None:
        integrity = lambda admitted_args, _: V.bounded_integrity(admitted_args, batch / "scope.json")
    scope_hash = V.stream_hash(batch / "scope.json")[0]
    result = {"pair_id": row["pair_id"], "arm": row["arm"], "status": "not_run", "attempts": [], "output": None}
    for number, attempt_id in enumerate((row["initial_id"], row["retry_id"])):
        try:
            integrity(args, scope)
        except Exception:
            return result, True
        kind = "initial" if number == 0 else "retry"
        directory = batch / "attempts" / attempt_id
        mkdir(directory)
        exclusive_json(directory / "started.json", V.started_receipt(row, attempt_id, kind, scope_hash))
        request = V.expected_request(args, batch, directory, pair, row["arm"])
        exclusive_json(directory / "request.json", request)
        # The started receipt already exists before any encoder subprocess call.
        encoded = invoke([sys.executable, str(Path(__file__).resolve()), "--encode-request", str(directory / "request.json")],
                         V.LIMITS["encode_seconds"], directory, "encode")
        # A killed writer cannot run its finally block; freeze its isolated partial here.
        if (directory / "output.bin").exists():
            V.no_symlinks(directory / "output.bin")
            os.chmod(directory / "output.bin", 0o400)
        attempt = {"id": attempt_id, "kind": kind, "status": "failed", "failure": None,
                   "observation": None, "logs": encoded["logs"]}
        if encoded["status"] != "success":
            try:
                integrity_failed = json.loads(encoded["stdout"]).get("category") == "prerequisite_integrity_failed"
            except (ValueError, AttributeError): integrity_failed = False
            attempt["failure"] = "prerequisite_integrity_failed" if integrity_failed else "encode_" + encoded["status"]
        else:
            readback = invoke([sys.executable, str(HERE / "verify-p0b-encoder-pilot.py"),
                               "--inspect-request", str(directory / "request.json")],
                              V.LIMITS["readback_seconds"], directory, "readback")
            attempt["logs"].update(readback["logs"])
            if readback["status"] == "success":
                try:
                    observation = json.loads(readback["stdout"])["observation"]
                    if observation.get("verified") is not True or type(observation.get("bytes")) is not int or observation["bytes"] <= 0:
                        raise ValueError("invalid readback receipt")
                    attempt.update(status="success", observation=observation)
                except (ValueError, KeyError, TypeError): attempt["failure"] = "readback_failure"
            else:
                attempt["failure"] = "readback_" + readback["status"]
        exclusive_json(directory / "completed.json", attempt)
        result["attempts"].append(attempt)
        if attempt["status"] == "success":
            result.update(status="success", output=attempt["observation"])
            return result, False
        result["status"] = "failed"
        if attempt["failure"] == "prerequisite_integrity_failed":
            return result, True
    return result, False


def run_batch(args, *, invoke=supervised):
    if not re.fullmatch(r"[a-z0-9][a-z0-9._-]{0,95}", args.batch_id):
        raise V.GateFailure("unsafe_batch_identity")
    batch = V.safe_batch(args.output_root, V.input_paths(args), new=True)
    corpus = V.load_json(args.corpus)
    schedule = V.make_schedule(corpus)
    # Only the final batch directory is created: caller owns and selects its parent.
    mkdir(batch)
    mkdir(batch / "attempts")
    arms = [{"pair_id": row["pair_id"], "arm": row["arm"], "status": "not_run", "attempts": [], "output": None}
            for row in schedule]
    outcome = {"schema_version": 1, "artifact": "p0b-encoder-pilot", "batch_id": args.batch_id,
               "scope_sha256": None, "prerequisites": "failed", "fixtures": [], "arms": arms,
               "stop_reason": "prerequisite_failure", "input_snapshot_after": None,
               "limitations": V.LIMITATIONS}
    exclusive_json(batch / "private-inputs.json", input_request(args))
    exclusive_json(batch / "planned-ledger.json", {"schedule": schedule, "arms": arms})
    try:
        admitted = invoke([sys.executable, str(HERE / "verify-p0b-encoder-pilot.py"), "--preflight", *V.cli_inputs(args)],
                          V.LIMITS["preflight_seconds"], batch, "preflight")
        if admitted["status"] != "success": raise V.GateFailure("prerequisite_failure")
        evidence = json.loads(admitted["stdout"])
        if evidence.get("status") != "PASS": raise V.GateFailure("prerequisite_failure")
        exclusive_json(batch / "preflight-evidence.json", evidence)
        scope = V.make_scope(corpus, evidence, args.batch_id)
        V.scan_public(scope)
        # Freeze the entire execution scope before the first output directory.
        exclusive_json(batch / "scope.json", scope)
        outcome.update(scope_sha256=V.stream_hash(batch / "scope.json")[0], prerequisites="passed",
                       fixtures=evidence["fixtures"], stop_reason=None)
        for index, row in enumerate(schedule):
            arms[index], stopped = execute_arm(scope, row, batch, args, invoke=invoke)
            if stopped:
                outcome["stop_reason"] = "prerequisite_integrity_failed"
                break
        V.bounded_integrity(args, batch / "scope.json")
        outcome["input_snapshot_after"] = V.input_snapshot(args, corpus)
        if not V.same(outcome["input_snapshot_after"], scope["input_snapshot"]):
            raise V.GateFailure("prerequisite_integrity_failed")
    except Exception as exc:
        outcome["stop_reason"] = str(exc) if isinstance(exc, V.GateFailure) else "batch_failure:" + type(exc).__name__
        exclusive_json(batch / "stopped.json", {"category": outcome["stop_reason"]})
    outcome["counts"] = V.derive_counts(arms)
    exclusive_json(batch / "execution-result.json", outcome)
    print(json.dumps({"batch_id": args.batch_id, "execution": "complete" if outcome["stop_reason"] is None else "stopped",
                      "counts": outcome["counts"], "actual_output_qualification": "separate_verifier_required"}, indent=2))
    return 0 if outcome["stop_reason"] is None else 2


def main():
    parser = V.parser()
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--batch-id", default="encoder-pilot-r3-v1")
    parser.add_argument("--encode-request", type=Path)
    args = parser.parse_args()
    try:
        if args.encode_request:
            encode_worker(V.load_json(V.private_path(args.encode_request)))
            return 0
        return run_batch(args)
    except Exception as exc:
        category = str(exc) if isinstance(exc, V.GateFailure) else "encoding_failure:" + type(exc).__name__
        print(json.dumps({"status": "INVALID", "category": category}))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())

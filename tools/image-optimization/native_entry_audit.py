#!/usr/bin/env python3
"""Token-free installed iOS byte audit; no fixture/configuration or credential inputs."""
import base64
from hashlib import sha256
from pathlib import Path
import os
import plistlib
import secrets
import sys
import stat
import time

from native_entry_config import (EntryError, HASH, SCOPE, exact_keys, frozen_json,
    no_symlink, open_nofollow, private_file, private_output, require, strict_json, write_private)
import native_entry_driver as host
from native_entry_destination import AUDIT_TEST, destination_run

AUDIT_SCOPE = "image-optimization-installation-audit-r1"
REPORT_PREFIX = b"IMAGE_EXPERIMENT_INSTALLATION_AUDIT "
REPORT_FIELDS = ("schemaVersion", "scope", "nonce", "appId", "runnerId", "appSha256", "runnerSha256",
    "appFileCount", "appByteCount", "runnerFileCount", "runnerByteCount")


class InstallationReport:
    """Accept exactly one fresh device-computed report; expected digests stay on the host."""
    def __init__(self, request, build):
        self.request, self.build, self.report = request, build, None

    def __call__(self, line):
        require(len(line) <= 8192, "audit_report_rejected")
        require(not line.startswith(b"IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED"), "audit_native_refused")
        if not line.startswith(REPORT_PREFIX):
            return
        require(self.report is None, "audit_report_duplicate")
        encoded = line[len(REPORT_PREFIX):]
        try:
            decoded = base64.b64decode(encoded, validate=True)
            require(base64.b64encode(decoded) == encoded, "audit_report_rejected")
            report = strict_json(decoded, 4096)
        except (ValueError, TypeError):
            raise EntryError("audit_report_rejected") from None
        exact_keys(report, REPORT_FIELDS)
        require(type(report["schemaVersion"]) is int and report["schemaVersion"] == 1 and
            report["scope"] == AUDIT_SCOPE and report["nonce"] == self.request["nonce"] and
            report["appId"] == self.build["expectedAppId"] and report["runnerId"] == self.build["testAppId"],
            "audit_report_binding_rejected")
        for name in ("appSha256", "runnerSha256"):
            require(type(report[name]) is str and HASH.fullmatch(report[name]), "audit_report_rejected")
        for prefix in ("app", "runner"):
            require(type(report[prefix + "FileCount"]) is int and 1 <= report[prefix + "FileCount"] <= 65536 and
                type(report[prefix + "ByteCount"]) is int and 0 <= report[prefix + "ByteCount"] <= 512 * 1024 * 1024,
                "audit_report_rejected")
        require(int(time.time() * 1000) < self.request["deadline"], "audit_deadline_exceeded")
        self.report = report

    def verified(self, app, runner):
        require(self.report is not None, "audit_report_unavailable")
        require(int(time.time() * 1000) < self.request["deadline"], "audit_deadline_exceeded")
        for prefix, expected in (("app", app), ("runner", runner)):
            require(self.report[prefix + "Sha256"] == expected["sha256"] and
                self.report[prefix + "FileCount"] == expected["fileCount"] and
                self.report[prefix + "ByteCount"] == expected["byteCount"], "audit_installed_bytes_changed")
        return self.report


def validate_build(build):
    """Rehash source/copy/artifacts and exact bundle IDs without reading fixture settings."""
    require(type(build) is dict and build.get("schemaVersion") == 1 and type(build.get("schemaVersion")) is int and
        build.get("scope") == SCOPE and build.get("platform") == "ios" and build.get("mode") in ("profile", "release") and
        build.get("expectedAppId") == "com.tellpal.tellpalMobilePoc.imageexperiment." + build["mode"] and
        build.get("testAppId") == build["expectedAppId"] + ".uitests.xctrunner" and
        build.get("dartTarget") == "lib/main_image_experiment.dart" and build.get("nativeTarget") == "ImageExperiment",
        "audit_build_binding_rejected")
    sources, source_hash = host.source_snapshot()
    require(build.get("sourceFiles") == sources and build.get("sourceSha256") == source_hash,
        "audit_source_changed")
    copy = no_symlink(build.get("sourceCopyPath"))
    require(copy.is_dir() and host.validate_copied_sources(copy, sources) == build.get("copiedSourceSha256"),
        "audit_copy_changed")
    require(build.get("lockSha256") == sha256((Path(__file__).parent / "uv.lock").read_bytes()).hexdigest(),
        "audit_lock_changed")
    observations = []
    for path_field, hash_field, id_field in (("artifactPath", "artifactSha256", "expectedAppId"),
        ("testArtifactPath", "testArtifactSha256", "testAppId")):
        path = no_symlink(build.get(path_field))
        require(path.is_dir(), "audit_artifact_rejected")
        observed = host.artifact_observation(path)
        require(observed["sha256"] == build.get(hash_field), "audit_artifact_changed")
        descriptor = None
        try:
            descriptor = open_nofollow(path / "Info.plist", os.O_RDONLY | os.O_NONBLOCK)
            file_info = os.fstat(descriptor)
            require(stat.S_ISREG(file_info.st_mode) and file_info.st_size <= 65536, "audit_identity_rejected")
            with os.fdopen(descriptor, "rb") as handle:
                descriptor = None
                info_bytes = handle.read(65537)
            require(len(info_bytes) <= 65536, "audit_identity_rejected")
            info = plistlib.loads(info_bytes)
            require(type(info) is dict and info.get("CFBundleIdentifier") == build[id_field], "audit_identity_rejected")
        except (OSError, ValueError, TypeError, plistlib.InvalidFileException):
            raise EntryError("audit_identity_rejected") from None
        finally:
            if descriptor is not None:
                os.close(descriptor)
        observations.append(observed)
    require(type(build.get("certificateSha256")) is str and HASH.fullmatch(build["certificateSha256"]),
        "audit_signing_rejected")
    return observations


def verify_signing(build, output, deadline):
    for name, field in (("audit-app-signing", "artifactPath"), ("audit-runner-signing", "testArtifactPath")):
        path, prefix = build[field], Path(output) / name
        host.checked_run(["codesign", "--verify", "--strict", path], output, name + "-verify", deadline=deadline)
        host.checked_run(["codesign", "--display", "--extract-certificates=" + str(prefix), path], output, name, deadline=deadline)
        require(sha256(private_file(str(prefix) + "0", exact_mode=False)).hexdigest() == build["certificateSha256"], "audit_signing_changed")


def audit_installation(build_path, selector, output, deadline):
    data = private_file(build_path, 2 * 1024 * 1024)
    build = strict_json(data, 2 * 1024 * 1024)
    app, runner = validate_build(build)
    verify_signing(build, output, deadline)
    request = {"schemaVersion": 1, "scope": AUDIT_SCOPE, "expectedAppId": build["expectedAppId"],
        "nonce": secrets.token_hex(32), "deadline": int(time.time() * 1000) + 30000}
    encoded = base64.b64encode(frozen_json(request)).decode("ascii")
    run_file = destination_run(build, output, encoded, audit=True)
    observer = InstallationReport(request, build)
    command = ["xcodebuild", "-xctestrun", str(run_file), "-destination", "id=" + selector,
        "test-without-building", "-only-testing:ImageExperimentUITests/" + AUDIT_TEST]
    host.checked_run(command, output, "installation-audit", deadline=min(deadline, time.monotonic() + 30),
        env=host.tool_environment(), on_line=observer)
    report = observer.verified(app, runner)
    require(private_file(build_path, 2 * 1024 * 1024) == data, "audit_build_receipt_changed")
    require(validate_build(build) == [app, runner], "audit_binding_changed")
    require(int(time.time() * 1000) < request["deadline"], "audit_deadline_exceeded")
    receipt = {"schemaVersion": 1, "scope": AUDIT_SCOPE, "platform": "ios", "mode": build["mode"],
        "expectedAppId": build["expectedAppId"], "testAppId": build["testAppId"],
        "deviceSelectorSha256": sha256(selector.encode()).hexdigest(), "nonce": request["nonce"],
        "observedAt": int(time.time() * 1000), "buildReceiptSha256": sha256(data).hexdigest(),
        "sourceSha256": build["sourceSha256"], "copiedSourceSha256": build["copiedSourceSha256"],
        "artifactSha256": app["sha256"], "testArtifactSha256": runner["sha256"],
        "installedArtifactSha256": report["appSha256"], "installedTestArtifactSha256": report["runnerSha256"],
        "certificateSha256": build["certificateSha256"], "certificateBinding": "exact_bundle_equivalence",
        "appFileCount": report["appFileCount"], "appByteCount": report["appByteCount"],
        "runnerFileCount": report["runnerFileCount"], "runnerByteCount": report["runnerByteCount"],
        "nativeEntryGate": "open", "authentication": "not_run", "fixtureMatrix": "not_run"}
    write_private(Path(output) / "installation-receipt.json", receipt)
    return receipt


def main(argv=None):
    parser = host.SafeParser(description="Token-free installed iOS artifact audit")
    parser.add_argument("--build-receipt", required=True)
    parser.add_argument("--device-selector", required=True)
    parser.add_argument("--private-output", required=True)
    args = parser.parse_args(argv)
    output = None
    try:
        require(type(args.device_selector) is str and 1 <= len(args.device_selector) <= 256 and
            all(32 < ord(char) < 127 for char in args.device_selector), "selector_rejected")
        output = private_output(args.private_output, [args.build_receipt], create=True)
        receipt = audit_installation(args.build_receipt, args.device_selector, output, time.monotonic() + host.TOTAL_SECONDS)
        print(frozen_json({"scope": AUDIT_SCOPE, "status": "completed", "receiptSha256": sha256(frozen_json(receipt)).hexdigest(),
            "nativeEntryGate": "open"}).decode("ascii"))
        return 0
    except (EntryError, OSError, ValueError, KeyError, TypeError) as error:
        reason = str(error) if isinstance(error, EntryError) else "audit_operation_failed"
        if output is not None:
            write_private(output / "failure-receipt.json", {"scope": AUDIT_SCOPE, "status": "failed", "reason": reason,
                "nativeEntryGate": "open"})
        print(frozen_json({"scope": AUDIT_SCOPE, "status": "refused", "reason": reason, "nativeEntryGate": "open"}).decode("ascii"))
        return 4


if __name__ == "__main__":
    sys.exit(main())

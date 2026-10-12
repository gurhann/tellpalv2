"""Execute experiment Kotlin with controlled Android/Flutter boundaries and real JSON/JCA."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import selectors
import shutil
import signal
import subprocess
import tempfile
import time
import zipfile

HERE = Path(__file__).resolve().parent
MOBILE = HERE.parents[1]
MAX_OUTPUT_BYTES = 262_144


def private_file(path, data):
    path.write_bytes(data if isinstance(data, bytes) else data.encode())
    path.chmod(0o600)


def copy_source(path, destination):
    """Hash the exact immutable read copied into the compiler's private snapshot."""
    data = path.read_bytes()
    private_file(destination, data)
    return hashlib.sha256(data).hexdigest()


def cached_jar(group, artifact, version):
    base = Path.home() / ".gradle/caches/modules-2/files-2.1" / group / artifact / version
    matches = sorted(base.glob(f"*/{artifact}-{version}.jar"))
    if not matches:
        raise RuntimeError(f"Required cached Kotlin dependency unavailable: {artifact}-{version}")
    return matches[0]


def toolchain():
    homes = sorted((Path.home() / "Library/Java/JavaVirtualMachines").glob("corretto-17*/Contents/Home"))
    java = homes[-1] / "bin/java" if homes else Path(shutil.which("java") or "")
    if not java.is_file():
        raise RuntimeError("Java 17 runtime unavailable")
    standard = cached_jar("org.jetbrains.kotlin", "kotlin-stdlib", "2.2.20")
    compiler = [cached_jar("org.jetbrains.kotlin", item, "2.2.20") for item in
                ("kotlin-compiler-embeddable", "kotlin-stdlib", "kotlin-script-runtime", "kotlin-daemon-embeddable")]
    compiler += [cached_jar("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10"),
                 cached_jar("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.8.0"),
                 cached_jar("org.jetbrains", "annotations", "13.0")]
    json_jar = Path.home() / ".m2/repository/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar"
    if not json_jar.is_file():
        raise RuntimeError("Executable Android-compatible org.json dependency unavailable")
    if any(path.name == "android.jar" for path in compiler + [standard, json_jar]):
        raise RuntimeError("Android SDK stubs must never enter the executable harness")
    return java, compiler, [standard, json_jar]


def execute(command, output, label, timeout=90):
    """Retain bounded original pipe bytes and terminal status, including failed runs."""
    arguments = [str(value) for value in command]
    process = subprocess.Popen(arguments, stdout=subprocess.PIPE, stderr=subprocess.PIPE, start_new_session=True)
    selector = selectors.DefaultSelector()
    streams = [bytearray(), bytearray()]
    status, truncated = None, False
    deadline = time.monotonic() + timeout

    def collect(pipe, index):
        nonlocal truncated
        chunk = os.read(pipe.fileno(), 4096)
        if not chunk:
            selector.unregister(pipe)
            return
        remaining = MAX_OUTPUT_BYTES - sum(map(len, streams))
        streams[index].extend(chunk[:remaining])
        truncated = truncated or len(chunk) > remaining

    def stop_owned():
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass

    try:
        for index, pipe in enumerate((process.stdout, process.stderr)):
            selector.register(pipe, selectors.EVENT_READ, index)
        while selector.get_map() or process.poll() is None:
            if time.monotonic() >= deadline:
                status = "timeout"
                break
            for key, _ in selector.select(min(0.05, max(0, deadline - time.monotonic()))):
                collect(key.fileobj, key.data)
                if truncated:
                    status = "output_limit"
                    break
            if status is not None:
                break
        if status is not None:
            stop_owned()
            # Drain only the owned killed process's pipes under a separate finite cleanup cap.
            cleanup_deadline = time.monotonic() + 2
            while selector.get_map() and time.monotonic() < cleanup_deadline:
                for key, _ in selector.select(0.05):
                    collect(key.fileobj, key.data)
        process.wait(timeout=2)
    finally:
        if process.poll() is None:
            stop_owned()
        selector.close()
        for pipe in (process.stdout, process.stderr):
            pipe.close()
        process.wait(timeout=2)

    stdout, stderr = map(bytes, streams)
    status = status or ("ok" if process.returncode == 0 else "failed")
    private_file(output / f"{label}.stdout", stdout)
    private_file(output / f"{label}.stderr", stderr)
    private_file(output / f"{label}.exit", str(process.returncode) + "\n")
    private_file(output / f"{label}.status.json", json.dumps({
        "status": status, "exitCode": process.returncode, "traceBytes": len(stdout) + len(stderr),
        "traceSha256": [hashlib.sha256(value).hexdigest() for value in (stdout, stderr)],
        "truncated": truncated,
    }, sort_keys=True) + "\n")
    if status == "timeout":
        raise subprocess.TimeoutExpired(arguments, timeout, output=stdout, stderr=stderr)
    if status == "output_limit":
        raise RuntimeError("Host harness output exceeded its private bound")
    return subprocess.CompletedProcess(arguments, process.returncode, stdout, stderr)


def run(output=None, mutant=False):
    output = Path(output) if output else Path(tempfile.mkdtemp(prefix="tellpal-android-kotlin-", dir="/private/tmp"))
    output.mkdir(parents=True, exist_ok=True, mode=0o700)
    output.chmod(0o700)
    java, compiler, runtime = toolchain()
    # The executable JSON jar predates Android's generic Iterator<String> signature.
    # Compile against the installed Android JSON API; never execute SDK stubs.
    sdk = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "Library/Android/sdk")))
    android_apis = sorted((sdk / "platforms").glob("android-*/android.jar"))
    if not android_apis:
        raise RuntimeError("Installed Android JSON compile API unavailable")
    json_api = output / "json-api.jar"
    with zipfile.ZipFile(android_apis[-1]) as source, zipfile.ZipFile(json_api, "w") as destination:
        for name in source.namelist():
            if name.startswith("org/json/") and name.endswith(".class"):
                destination.writestr(name, source.read(name))
    json_api.chmod(0o600)
    source_root = output / "sources"
    source_root.mkdir(mode=0o700)
    production = MOBILE / "android/app/src"
    inputs = sorted((production / "imageExperiment/kotlin/com/tellpal/imageexperiment").glob("*.kt"))
    inputs += sorted((production / "imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment").glob("*.kt"))
    inputs += sorted((HERE / "android_host_stubs").glob("*.kt"))
    inputs += sorted((HERE / "android_host_harness").glob("*.kt"))
    frozen = {}
    for path in inputs:
        frozen[str(path.relative_to(MOBILE.parents[1]))] = copy_source(path, source_root / path.name)
    dart_producer = MOBILE / "lib/app/image_experiment_configuration.dart"
    copied_dart = source_root / dart_producer.name
    frozen[str(dart_producer.relative_to(MOBILE.parents[1]))] = copy_source(dart_producer, copied_dart)
    for path in (Path(__file__).resolve(), HERE / "test_build_guard.py"):
        frozen[str(path.relative_to(MOBILE.parents[1]))] = hashlib.sha256(path.read_bytes()).hexdigest()
    private_file(output / "source-freeze.json", json.dumps(frozen, indent=2, sort_keys=True) + "\n")
    classes = output / "classes"
    classpath = os.pathsep.join(str(path) for path in runtime)
    compile_classpath = os.pathsep.join([str(runtime[0]), str(json_api)])
    compiled = execute([java, "-cp", os.pathsep.join(str(path) for path in compiler),
                        "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-no-stdlib", "-no-reflect",
                        "-jvm-target", "17", "-classpath", compile_classpath, "-d", classes,
                        *sorted(source_root.glob("*.kt"))], output, "compile")
    if compiled.returncode:
        raise AssertionError(f"Kotlin host compilation failed; inspect {output / 'compile.stderr'}")
    openssl = shutil.which("openssl")
    if not openssl:
        raise RuntimeError("OpenSSL fixture generator unavailable")
    certificate = output / "synthetic-certificate.der"
    generated = execute([openssl, "req", "-x509", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:prime256v1",
                         "-nodes", "-keyout", output / "synthetic-key.pem", "-out", certificate,
                         "-outform", "DER", "-days", "2", "-subj", "/CN=fixture.invalid"], output, "certificate")
    if generated.returncode:
        raise AssertionError(f"Synthetic certificate generation failed; inspect {output / 'certificate.stderr'}")
    for fixture in (certificate, output / "synthetic-key.pem"):
        fixture.chmod(0o600)
    expired_certificate = output / "synthetic-expired-certificate.der"
    locked_python = MOBILE.parents[1] / "tools/image-optimization/.venv/bin/python"
    if not locked_python.is_file():
        raise RuntimeError("Locked host certificate fixture runtime unavailable")
    generated_expired = execute([locked_python, "-B", "-c", '''
from datetime import datetime, timedelta, timezone
from pathlib import Path
import sys
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID
key = ec.generate_private_key(ec.SECP256R1())
name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "fixture.invalid")])
now = datetime.now(timezone.utc)
certificate = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
    .serial_number(x509.random_serial_number()).not_valid_before(now - timedelta(days=2))
    .not_valid_after(now - timedelta(days=1)).sign(key, hashes.SHA256()))
path = Path(sys.argv[1])
path.write_bytes(certificate.public_bytes(serialization.Encoding.DER))
path.chmod(0o600)
''', expired_certificate], output, "expired-certificate")
    if generated_expired.returncode:
        raise AssertionError("Expired synthetic certificate generation failed")
    command = [java, "-cp", os.pathsep.join([str(classes), classpath]),
               "com.tellpal.imageexperiment.host.HarnessAssertionsKt", output / "private-app", certificate,
               copied_dart, expired_certificate]
    checked = execute(command, output, "matrix")
    if checked.returncode:
        raise AssertionError(f"Kotlin host matrix failed; inspect {output / 'matrix.stderr'}")
    cases = [line.split()[1] for line in checked.stdout.decode().splitlines() if line.startswith("HARNESS_CASE ")]
    if not cases or len(cases) != len(set(cases)) or checked.stdout.decode().splitlines()[-1] != f"HARNESS_TOTAL {len(cases)}":
        raise AssertionError("Kotlin host matrix did not execute all registered cases")
    summary = {"caseCount": len(cases), "cases": cases, "matrixExit": checked.returncode,
               "evidenceRoot": str(output), "runtime": "controlled Android/Flutter; real JSON/JCA; no android.jar"}
    if mutant:
        activity = source_root / "ImageExperimentActivity.kt"
        text = activity.read_text()
        seam = "observeRefusal = { refusalDiagnostics.record(it) }"
        if text.count(seam) != 1:
            raise AssertionError("Private propagation mutant could not isolate the Bootstrap observer seam")
        changed = text.replace(seam, "observeRefusal = null")
        private_file(activity, changed)
        mutant_classes = output / "mutant-classes"
        mutated = execute([java, "-cp", os.pathsep.join(str(path) for path in compiler),
                           "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-no-stdlib", "-no-reflect",
                           "-jvm-target", "17", "-classpath", os.pathsep.join([str(classes), compile_classpath]),
                           "-d", mutant_classes, activity], output, "mutant-compile")
        if mutated.returncode:
            raise AssertionError("Private propagation mutant must compile before its runtime failure counts")
        failed = execute([java, "-cp", os.pathsep.join([str(mutant_classes), str(classes), classpath]),
                          "com.tellpal.imageexperiment.host.HarnessAssertionsKt", output / "mutant-app", certificate,
                          copied_dart, expired_certificate, "propagation"], output, "mutant")
        if failed.returncode == 0 or b"HARNESS_ASSERTION first-specific-stage-propagation" not in failed.stderr:
            raise AssertionError("Removing private Bootstrap observer propagation was not detected")
        summary["mutantExit"] = failed.returncode
        summary["mutantFailure"] = "first-specific-stage-propagation"
        summary["mutant"] = {"file": "ImageExperimentActivity.kt", "replacementCount": 1,
                             "originalSha256": hashlib.sha256(text.encode()).hexdigest(),
                             "mutatedSha256": hashlib.sha256(changed.encode()).hexdigest(),
                             "removedBehavior": "Bootstrap observeRefusal propagation"}
    private_file(output / "summary.json", json.dumps(summary, indent=2) + "\n")
    return summary


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    parser.add_argument("--mutant", action="store_true")
    args = parser.parse_args()
    print(json.dumps(run(args.output, args.mutant), indent=2))

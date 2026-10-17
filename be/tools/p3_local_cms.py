#!/usr/bin/env python3
"""P3 yerel CMS hazırlığı. Varsayılan dry-run dosya, kimlik veya ağ erişimi yapmaz."""
import argparse
import os
from pathlib import Path
import re
import shlex
import shutil
import socket
import subprocess
import sys
import uuid
from urllib.parse import urlsplit

BE = Path(__file__).resolve().parents[1]
DIRECTORY = BE / "var/p3-local-cms"
MAIN = "com.tellpal.v2.support.P3LocalCmsSetup"
BUCKET = "tellpal-ee0dd.appspot.com"
DEFAULT_DATABASE_URL = "jdbc:postgresql://localhost:5432/tellpal_v2"
EVIDENCE = "P3_LOCAL_CMS_SYNTHETIC_RGB8_80X60_TO_40X30_PNG_GROWTH_APPROVED"
LIMITS = {"width": 40, "height": 30, "max-source-bytes": 1000000, "max-output-bytes": 1000000,
          "max-pixels": 100000, "parent-memory-bytes": 16000000, "heap-mi-b": 64,
          "direct-mi-b": 8, "process-mi-b": 256, "disk-bytes": 2100000,
          "source-millis": 5000, "revision-millis": 30000, "concurrency": 1,
          "encoder-threads": 1, "allow-growth": "true", "growth-evidence": EVIDENCE}


def overlay(prefix):
    validate_prefix(prefix)
    values = {"tellpal.asset.storage.firebase.path-prefix": prefix,
              "tellpal.asset.processing.poller.enabled": "false",
              "tellpal.asset.reading.test-engine-enabled": "true",
              "tellpal.content.reading-publication.enabled": "true"}
    for role in ("READING_COVER", "READING_DETAIL", "READING_PAGE"):
        for field, value in LIMITS.items():
            values[f"tellpal.content.reading-publication.profiles.{role}.{field}"] = value
    def property_value(value):
        return "".join(character if ord(character) < 128 else f"\\u{ord(character):04x}" for character in str(value))
    return "\n".join(f"{key}={property_value(value)}" for key, value in values.items()) + "\n"


def validate_prefix(prefix):
    if not re.fullmatch(r"local/p3-cms-[0-9a-f-]{36}", prefix):
        raise ValueError("PREFIX_OUT_OF_SCOPE")
    identity = uuid.UUID(prefix.removeprefix("local/p3-cms-"))
    if identity.version != 4 or "local/p3-cms-" + str(identity) != prefix:
        raise ValueError("PREFIX_OUT_OF_SCOPE")


def validate_environment(environment):
    if environment.get("SPRING_PROFILES_ACTIVE", "local") != "local":
        raise ValueError("EXPLICIT_LOCAL_ONLY")
    # Spring'ın alternatif öncelik yolları ve JVM enjeksiyonu hedef kontrolünü aşamaz.
    if any(name in environment for name in ("SPRING_APPLICATION_JSON", "SPRING_CONFIG_LOCATION",
            "SPRING_CONFIG_ADDITIONAL_LOCATION", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")):
        raise ValueError("AMBIGUOUS_EXTERNAL_CONFIGURATION")
    for name in environment:
        if name.startswith(("TELLPAL_CONTENT_READING_PUBLICATION_", "TELLPAL_ASSET_READING_", "SPRING_DATASOURCE_")):
            raise ValueError("AMBIGUOUS_EXTERNAL_CONFIGURATION")
        if name.startswith("SPRING_CONFIG_") or (name.startswith("SPRING_PROFILES_") and name != "SPRING_PROFILES_ACTIVE"):
            raise ValueError("AMBIGUOUS_EXTERNAL_CONFIGURATION")
    jdbc = environment.get("TELLPAL_DB_URL", DEFAULT_DATABASE_URL)
    if not jdbc.startswith("jdbc:postgresql://"):
        raise ValueError("LOCAL_DB_REQUIRED")
    target = urlsplit(jdbc[5:])
    if target.hostname not in {"localhost", "127.0.0.1", "::1"} or target.username or target.password \
            or target.query or target.fragment or not re.fullmatch(r"/[A-Za-z0-9_]+", target.path):
        raise ValueError("LOCAL_DB_REQUIRED")
    if environment.get("TELLPAL_ASSET_STORAGE_FIREBASE_BUCKET_NAME", BUCKET) != BUCKET:
        raise ValueError("BUCKET_OUT_OF_SCOPE")
    if environment.get("TELLPAL_ASSET_STORAGE_FIREBASE_FAKE_CLIENT_ENABLED", "false").lower() != "false":
        raise ValueError("REAL_SDK_REQUIRED")
    environment["SPRING_PROFILES_ACTIVE"] = "local"


def load_environment(path):
    environment = os.environ.copy()
    if not path.is_file():
        raise ValueError("ENV_FILE_REQUIRED")
    for raw in path.read_text().splitlines():
        raw = raw.strip()
        if not raw or raw.startswith("#"):
            continue
        if raw.startswith("export "):
            raw = raw[7:]
        match = re.fullmatch(r"([A-Za-z_][A-Za-z0-9_]*)=(.*)", raw)
        if match is None:
            raise ValueError("ENV_FILE_INVALID")
        value = match[2]
        if value[:1].isspace() and value.lstrip().startswith("#"):
            raise ValueError("ENV_INLINE_COMMENT_UNSUPPORTED")
        values = shlex.split(value, comments=False)
        if len(values) > 1:
            raise ValueError("ENV_FILE_INVALID")
        environment.setdefault(match[1], values[0] if values else "")
    validate_environment(environment)
    return environment


def private_create(path, text):
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600)
    with os.fdopen(descriptor, "w", encoding="utf-8") as output:
        output.write(text)
        output.flush()
        os.fsync(output.fileno())


def private_directory():
    DIRECTORY.mkdir(parents=True, mode=0o700, exist_ok=True)
    info = DIRECTORY.stat()
    if DIRECTORY.is_symlink() or DIRECTORY.resolve() != DIRECTORY or info.st_uid != os.getuid() or info.st_mode & 0o077:
        raise ValueError("PRIVATE_DIRECTORY_REQUIRED")


def classpath():
    dependency = BE / "target/p3-local-cms.classpath"
    if not dependency.is_file() or not (BE / "target/test-classes" / (MAIN.replace(".", "/") + ".class")).is_file():
        raise ValueError("PREPARATION_REQUIRED")
    return os.pathsep.join((str(BE / "target/test-classes"), str(BE / "target/classes"), dependency.read_text().strip()))


def java_command(java, main):
    executable = java or shutil.which("java")
    if not executable:
        raise ValueError("JAVA_REQUIRED")
    return [str(executable), "-Xmx512m", "-Djava.awt.headless=true", "-cp", classpath(), main]


def prepare(environment):
    private_directory()
    with open(DIRECTORY / "prepare.log", "ab") as output:
        os.chmod(DIRECTORY / "prepare.log", 0o600)
        result = subprocess.run(["bash", str(BE / "mvnw"), "-o", "-q", "-DskipTests", "test-compile",
                                 "dependency:build-classpath", "-Dmdep.includeScope=test",
                                 "-Dmdep.outputFile=target/p3-local-cms.classpath"],
                                cwd=BE, env=environment, stdout=output, stderr=subprocess.STDOUT)
    print("PREPARATION_PASS" if result.returncode == 0 else "PREPARATION_FAILED_PRIVATE_LOG")
    return 0 if result.returncode == 0 else 2


def preflight(environment, java):
    private_directory()
    config = DIRECTORY / "application-p3.properties"
    if not config.exists():
        private_create(config, overlay("local/p3-cms-" + str(uuid.uuid4())))
    # Mevcut overlay yalnız bu aracın birebir kapsamıyla kullanılabilir.
    prefix = overlay_prefix()
    with open(DIRECTORY / "preflight.log", "ab") as output:
        os.chmod(DIRECTORY / "preflight.log", 0o600)
        result = subprocess.run(java_command(java, MAIN) + ["--preflight"], cwd=BE, env=scoped_environment(environment, prefix),
                                stdout=output, stderr=subprocess.STDOUT)
    print("PREFLIGHT_PASS_READONLY_NO_CLOUD" if result.returncode == 0 else "PREFLIGHT_FAILED_PRIVATE_LOG_NO_CLOUD")
    return result.returncode


def overlay_prefix():
    text = (DIRECTORY / "application-p3.properties").read_text()
    values = dict(line.split("=", 1) for line in text.splitlines())
    prefix = values["tellpal.asset.storage.firebase.path-prefix"]
    if text != overlay(prefix):
        raise ValueError("OVERLAY_CHANGED")
    return prefix


def scoped_environment(environment, prefix):
    """Spring env önceliğini yalnız onaylanan iki P3 ayarı için sabitler."""
    validate_prefix(prefix)
    scoped = environment.copy()
    scoped["TELLPAL_ASSET_PROCESSING_POLLER_ENABLED"] = "false"
    scoped["TELLPAL_ASSET_STORAGE_FIREBASE_PATH_PREFIX"] = prefix
    return scoped


def setup(environment, java):
    command = java_command(java, MAIN)
    require_free_port(environment)
    private_directory()
    if (DIRECTORY / "attempt.properties").exists():
        raise FileExistsError("EXISTING_ATTEMPT")
    if preflight(environment, java) != 0:
        return 2
    # Bu işaret bootstrap/seed başarısız olsa da kalır; yeni prefix ile sessiz tekrar yoktur.
    private_create(DIRECTORY / "attempt.properties", "state=PLANNED_NO_RETRY_NO_CLEANUP\n")
    with open(DIRECTORY / "setup.log", "xb") as output:
        os.chmod(DIRECTORY / "setup.log", 0o600)
        result = subprocess.run(command + ["--execute"], cwd=BE, env=scoped_environment(environment, overlay_prefix()),
                                stdout=output, stderr=subprocess.STDOUT)
    print("SETUP_PASS_DRAFT_READY" if result.returncode == 0 else "SETUP_FAILED_PRIVATE_RECEIPT_NO_RETRY")
    return 0 if result.returncode == 0 else 2


def receipt_values():
    values = {}
    for line in (DIRECTORY / "receipt.properties").read_text().splitlines():
        key, value = line.split("=", 1)
        values[key] = value
    if values.get("state") != "DRAFT_READY_PUBLISH_IN_CMS":
        raise ValueError("SUCCESSFUL_SEED_REQUIRED")
    validate_prefix(values["prefix"])
    if (DIRECTORY / "application-p3.properties").read_text() != overlay(values["prefix"]):
        raise ValueError("OVERLAY_CHANGED")
    return values


def start(environment, java, foreground=False):
    private_directory()
    receipt = receipt_values()
    if receipt.get("databaseUrl") != environment.get("TELLPAL_DB_URL", DEFAULT_DATABASE_URL):
        raise ValueError("RECEIPT_DATABASE_MISMATCH")
    # Bilinmeyen süreci durdurmaz. Önce mevcut sahip tanımlanıp açıkça durdurulmalıdır.
    require_free_port(environment)
    command = java_command(java, "com.tellpal.v2.TellPalApplication")
    command += ["--spring.profiles.active=local", "--spring.config.additional-location=file:"
                + str(DIRECTORY / "application-p3.properties"), "--spring.datasource.url=" + receipt["databaseUrl"]]
    with open(DIRECTORY / "backend.log", "ab") as output:
        os.chmod(DIRECTORY / "backend.log", 0o600)
        if foreground:
            print("BACKEND_FOREGROUND_START_REQUESTED PRIVATE_LOG be/var/p3-local-cms/backend.log", flush=True)
            result = subprocess.run(command, cwd=BE, env=scoped_environment(environment, receipt["prefix"]),
                                    stdout=output, stderr=subprocess.STDOUT)
            print("BACKEND_FOREGROUND_EXIT", result.returncode)
            return 0 if result.returncode == 0 else 2
        child = subprocess.Popen(command, cwd=BE, env=scoped_environment(environment, receipt["prefix"]), stdout=output,
                                 stderr=subprocess.STDOUT, start_new_session=True)
    (DIRECTORY / "backend.pid").write_text(str(child.pid) + "\n")
    os.chmod(DIRECTORY / "backend.pid", 0o600)
    print("BACKEND_START_REQUESTED PID", child.pid, "PRIVATE_LOG be/var/p3-local-cms/backend.log")
    return 0


def require_free_port(environment):
    port = int(environment.get("SERVER_PORT", environment.get("PORT", "8080")))
    if not 1 <= port <= 65535:
        raise ValueError("INVALID_LOCAL_PORT")
    with socket.socket() as probe:
        try:
            probe.bind(("127.0.0.1", port))
        except OSError as error:
            raise ValueError("PORT_BUSY_IDENTIFY_EXISTING_PROCESS") from error


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--prepare", action="store_true")
    modes.add_argument("--preflight", action="store_true")
    modes.add_argument("--setup", action="store_true")
    modes.add_argument("--start", action="store_true")
    parser.add_argument("--foreground", action="store_true", help="--start backend'ini yönetilen komut oturumunda beklet")
    parser.add_argument("--java", help="Java 25 executable")
    parser.add_argument("--env-file", type=Path, default=BE / ".env")
    args = parser.parse_args(argv)
    if args.foreground and not args.start:
        parser.error("--foreground yalnız --start ile kullanılabilir")
    if not any((args.prepare, args.preflight, args.setup, args.start)):
        print("DRY_RUN_ZERO_CREDENTIAL_READS_ZERO_NETWORK_ZERO_DB_WRITES")
        print("SCOPE", BUCKET, "local/p3-cms-<UUID> SINGLE_DRAFT_STORY tr ONE_PAGE")
        print("RGB8_PNG 80x60 OUTPUT 40x30 OBJECTS_MAX 6 BYTES_EACH_MAX 1000000 TOTAL_MAX 6000000")
        print("EXPLICIT_MODES --prepare --preflight --setup --start; NO_STOP_NO_RETRY_NO_CLEANUP")
        return 0
    try:
        environment = load_environment(args.env_file)
        if args.prepare:
            return prepare(environment)
        if args.preflight:
            return preflight(environment, args.java)
        if args.setup:
            return setup(environment, args.java)
        return start(environment, args.java, args.foreground)
    except (OSError, ValueError, KeyError):
        print("REFUSED_LOCAL_CONFIGURATION_OR_EXISTING_ATTEMPT_NO_RETRY")
        return 2


if __name__ == "__main__":
    sys.exit(main())

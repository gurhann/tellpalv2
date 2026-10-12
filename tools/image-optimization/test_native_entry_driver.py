"""Bounded driver, strict receipt, launcher ownership and CLI privacy checks."""
from contextlib import ExitStack, redirect_stdout
from hashlib import sha256
from io import StringIO
import base64
import json
import os
from pathlib import Path
import sys
import time
import unittest
from unittest.mock import patch

from native_entry_config import Configuration, EntryError, SCOPE, frozen_json
import native_entry_driver as driver
from test_native_entry_config import PrivateInputs
from test_native_entry_server import BrokerInputs


class CommandTests(PrivateInputs):
    def output(self):
        path = self.base / "traces"
        path.mkdir(mode=0o700)
        return path

    def test_bounded_success_records_only_argument_hash_and_private_trace(self):
        output = self.output()
        result = driver.run_bounded([sys.executable, "-c", "print('synthetic private output')"], output, "success", seconds=2)
        self.assertEqual(result["status"], "ok")
        self.assertNotIn("argv", result)
        self.assertNotIn(str(self.base), json.dumps(result))
        for path in output.iterdir():
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    def test_timeout_kills_owned_process_group_and_flood_never_exceeds_disk_cap(self):
        output = self.output()
        before = time.monotonic()
        result = driver.run_bounded([sys.executable, "-c", "import time;time.sleep(30)"], output, "timeout", seconds=0.15)
        self.assertEqual(result["status"], "timeout")
        self.assertLess(time.monotonic() - before, 3)
        result = driver.run_bounded([sys.executable, "-c", "import os;os.write(1,b'x'*65536)"], output, "flood", seconds=2, max_bytes=1024)
        self.assertEqual(result["status"], "output_limit")
        self.assertLessEqual(result["traceBytes"], 1024)
        self.assertLessEqual(sum((output / ("flood" + suffix)).stat().st_size for suffix in (".stdout", ".stderr")), 1024)

    def test_stdout_lf_and_crlf_observers_preserve_exact_private_trace_bytes_and_hashes(self):
        output = self.output()
        raw = b"alpha\nbeta\r\n\ngamma \t\r\n"
        stderr = b"unchanged stderr\r\n"
        observed = []
        command = [sys.executable, "-c",
            "import os,sys;os.write(1,bytes.fromhex(sys.argv[1]));os.write(2,bytes.fromhex(sys.argv[2]))",
            raw.hex(), stderr.hex()]
        result = driver.run_bounded(command, output, "line-endings", seconds=3, on_line=observed.append)
        self.assertEqual(observed, [b"alpha", b"beta", b"", b"gamma \t"])
        self.assertEqual((result["status"], result["exitCode"]), ("ok", 0))
        self.assertEqual((output / "line-endings.stdout").read_bytes(), raw)
        self.assertEqual((output / "line-endings.stderr").read_bytes(), stderr)
        self.assertEqual(result["traceBytes"], len(raw) + len(stderr))
        self.assertEqual(result["traceSha256"], [sha256(raw).hexdigest(), sha256(stderr).hexdigest()])
        self.assertEqual(result["argumentSha256"], sha256(frozen_json(command)).hexdigest())

    def test_real_pipe_crlf_split_between_reads_preserves_embedded_repeated_and_bare_cr(self):
        output = self.output()
        raw = b"A\r\nB\r\r\nC\rD\nE\t \r\nF\r"
        observed, chunks = [], []
        real_read = os.read
        def byte_read(descriptor, capacity):
            chunk = real_read(descriptor, min(capacity, 1))
            if chunk:
                chunks.append(chunk)
            return chunk
        command = [sys.executable, "-c", "import os,sys;os.write(1,bytes.fromhex(sys.argv[1]))", raw.hex()]
        with patch.object(driver.os, "read", side_effect=byte_read):
            result = driver.run_bounded(command, output, "split-crlf", seconds=3, on_line=observed.append)
        self.assertEqual(chunks, [raw[index:index + 1] for index in range(len(raw))])
        self.assertEqual(observed, [b"A", b"B\r", b"C\rD", b"E\t "])
        self.assertEqual(result["status"], "ok")
        self.assertEqual((output / "split-crlf.stdout").read_bytes(), raw)
        self.assertEqual((output / "split-crlf.stderr").read_bytes(), b"")
        self.assertEqual(result["traceBytes"], len(raw))
        self.assertEqual(result["traceSha256"], [sha256(raw).hexdigest(), sha256(b"").hexdigest()])

    def test_failed_missing_tool_and_reused_trace_refuse(self):
        output = self.output()
        result = driver.run_bounded(["/nonexistent/synthetic-tool"], output, "missing", seconds=1)
        self.assertEqual(result["status"], "missing_executable")
        with self.assertRaises(EntryError):
            driver.checked_run([sys.executable, "-c", "raise SystemExit(1)"], output, "fail", deadline=time.monotonic() + 2)
        with self.assertRaises(EntryError):
            driver.run_bounded([sys.executable, "-c", "print(1)"], output, "missing", seconds=1)

    def test_inherited_credentials_are_not_forwarded_to_native_tools(self):
        with patch.dict(os.environ, {"FIREBASE_TOKEN": "not-for-tools", "GOOGLE_APPLICATION_CREDENTIALS": "private-path"}):
            environment = driver.tool_environment()
            self.assertNotIn("FIREBASE_TOKEN", environment)
            self.assertNotIn("GOOGLE_APPLICATION_CREDENTIALS", environment)
            self.assertIn("PATH", environment)

    def test_private_process_observation_has_finite_memory_and_deadline(self):
        self.assertEqual(driver.capture_observation([sys.executable, "-c", "print('synthetic')"]), b"synthetic\n")
        with self.assertRaisesRegex(EntryError, "backend_observation_rejected"):
            driver.capture_observation([sys.executable, "-c", "import os;os.write(1,b'x'*65536)"], max_bytes=128)
        with self.assertRaisesRegex(EntryError, "backend_observation_unavailable"):
            driver.capture_observation([sys.executable, "-c", "import time;time.sleep(30)"], seconds=0.15)

    def test_ios_native_test_linkage_gate_uses_copied_helper_and_requires_exact_success(self):
        mobile, runner, output = self.base / "copied-mobile", self.base / "native-runner.app", self.output()
        deadline = time.monotonic() + 5
        with patch.object(driver, "checked_run", return_value=b"NATIVE_TEST_LINKAGE_PASS\n") as command:
            self.assertEqual(driver.validate_ios_native_test_linkage(mobile, runner, output, deadline), "apple_native_only")
        command.assert_called_once_with([sys.executable, "-B",
            str(mobile / "ios/ImageExperimentUITests/verify_native_linkage.py"), "--runner", str(runner)],
            output, "native-test-linkage", deadline=deadline)
        for result in (b"", b"NATIVE_TEST_LINKAGE_REFUSED\n", b"NATIVE_TEST_LINKAGE_PASS\nextra\n"):
            with self.subTest(result=result), patch.object(driver, "checked_run", return_value=result), \
                    self.assertRaisesRegex(EntryError, "native_test_linkage_rejected"):
                driver.validate_ios_native_test_linkage(mobile, runner, output, deadline)
        with patch.object(driver, "checked_run", side_effect=EntryError("native_command_failed")), \
                self.assertRaisesRegex(EntryError, "native_command_failed"):
            driver.validate_ios_native_test_linkage(mobile, runner, output, deadline)

    def test_ios_build_rejects_linkage_before_signing_or_receipt_and_records_valid_gate(self):
        app_id = "com.tellpal.tellpalMobilePoc.imageexperiment.profile"
        config = Configuration(self.values | {"platform": "ios", "mode": "profile", "expectedAppId": app_id}, "0" * 64)
        for index, status in enumerate((b"NATIVE_TEST_LINKAGE_REFUSED\n", b"NATIVE_TEST_LINKAGE_PASS\n")):
            with self.subTest(status=status):
                output = self.base / ("build-" + str(index))
                output.mkdir(mode=0o700)
                mobile = output / "source/repository/app/mobile"
                (mobile / "ios").mkdir(parents=True)
                (mobile / "pubspec.lock").write_bytes(b"pinned synthetic lock")
                products = output / "derived-data/Build/Products/Profile-imageexperiment-iphoneos"
                app, runner = products / "ImageExperiment.app", products / "ImageExperimentUITests-Runner.app"
                app.mkdir(parents=True)
                runner.mkdir()
                runfile = products.parent / "synthetic.xctestrun"
                runfile.write_bytes(b"synthetic original runfile")
                runfile.chmod(0o600)
                calls = []
                def native_command(argv, target, alias, **kwargs):
                    calls.append((argv, alias))
                    if alias == "app-id":
                        return app_id.encode()
                    if alias == "test-id":
                        return (app_id + ".uitests.xctrunner").encode()
                    if alias == "native-test-linkage":
                        return status
                    if alias in ("app-signing", "test-signing"):
                        certificate = output / (alias + "0")
                        certificate.write_bytes(b"same selected synthetic certificate")
                        certificate.chmod(0o600)
                    if alias == "flutter-version":
                        return frozen_json({"engineRevision": "synthetic-engine"})
                    return b""
                with ExitStack() as stack:
                    stack.enter_context(patch.object(driver, "source_snapshot", return_value=({}, "0" * 64)))
                    stack.enter_context(patch.object(driver, "certificate_identity"))
                    stack.enter_context(patch.object(driver, "private_mobile_snapshot", return_value=mobile))
                    stack.enter_context(patch.object(driver, "validate_copied_sources", return_value="1" * 64))
                    stack.enter_context(patch.object(driver, "artifact_hash", return_value="2" * 64))
                    stack.enter_context(patch.object(driver, "checked_run", side_effect=native_command))
                    if status.endswith(b"REFUSED\n"):
                        with self.assertRaisesRegex(EntryError, "native_test_linkage_rejected"):
                            driver.build_entry(config, None, output, time.monotonic() + 5)
                        self.assertFalse((output / "build-receipt.json").exists())
                        self.assertFalse(any(argv[0] == "codesign" for argv, alias in calls))
                    else:
                        receipt = driver.build_entry(config, None, output, time.monotonic() + 5)
                        self.assertEqual(receipt["nativeTestLinkage"], "apple_native_only")
                        self.assertTrue((output / "build-receipt.json").exists())
                self.assertEqual([alias for argv, alias in calls].count("native-test-linkage"), 1)

    def test_private_snapshot_preserves_actual_native_repository_exclusion_topology(self):
        origin = self.base / "original/app/mobile"
        (origin / "android").mkdir(parents=True, mode=0o700)
        (origin / "ios").mkdir(mode=0o700)
        (origin / "pubspec.lock").write_bytes(b"synthetic pinned lock")
        (origin / "android/build.gradle.kts").write_bytes(b"synthetic gradle")
        (origin / "ios/Podfile").write_bytes(b"synthetic podfile")
        output = self.output()
        with patch.object(driver, "MOBILE", origin):
            mobile = driver.private_mobile_snapshot(output)
        self.assertEqual(mobile, output / "source/repository/app/mobile")
        android_repository = (mobile / "android").parents[2]
        ios_repository = (mobile / "ios").parents[2]
        self.assertEqual(android_repository, output / "source/repository")
        self.assertEqual(ios_repository, android_repository)
        self.assertFalse(output.is_relative_to(android_repository))
        self.assertFalse(Path(self.values["signingConfigFile"]).is_relative_to(android_repository))
        self.assertEqual((mobile / "pubspec.lock").read_bytes(), b"synthetic pinned lock")


class BackendReceiptTests(PrivateInputs):
    def setUp(self):
        super().setUp()
        source = "be/pom.xml"
        files = {source: sha256((driver.ROOT / source).read_bytes()).hexdigest()}
        artifact = self.file("backend.jar", b"synthetic artifact")
        configuration = self.file("backend.properties", b"spring.profiles.active=local\n")
        self.command = b"java -Dspring.profiles.active=local -jar synthetic.jar"
        self.receipt = {"schemaVersion": 1, "backendEnvironment": "local", "backendOrigin": self.values["backendOrigin"],
            "pid": 1234, "processStartTime": "synthetic-start", "commandSha256": sha256(self.command).hexdigest(),
            "sourceFiles": files, "sourceSha256": sha256(frozen_json(files)).hexdigest(),
            "artifactPath": str(artifact), "artifactSha256": sha256(artifact.read_bytes()).hexdigest(),
            "configPath": str(configuration), "configSha256": sha256(configuration.read_bytes()).hexdigest()}

    def inspect(self, pid, port):
        self.assertEqual(pid, 1234)
        return "synthetic-start", self.command, b"p1234\nn127.0.0.1:18080\n"

    def validate(self):
        Path(self.values["backendStartupReceiptPath"]).write_bytes(frozen_json(self.receipt))
        return driver.validate_backend_startup(self.configuration(), inspector=self.inspect)

    def test_full_live_process_profile_source_artifact_config_listener_binding(self):
        self.assertRegex(self.validate(), r"^[0-9a-f]{64}$")

    def test_complete_522_and_1024_source_maps_fit_and_every_changed_input_refuses(self):
        repository = self.base / "backend-source"
        directory = repository / "be" / ("runtime_" + "x" * 90)
        directory.mkdir(parents=True)
        sources = {}
        for index in range(1024):
            path = directory / (str(index).zfill(4) + ".java")
            path.write_bytes(b"synthetic runtime input " + str(index).encode())
            sources[str(path.relative_to(repository))] = sha256(path.read_bytes()).hexdigest()
        for count in (522, 1024):
            self.receipt["sourceFiles"] = dict(list(sources.items())[:count])
            self.receipt["sourceSha256"] = sha256(frozen_json(self.receipt["sourceFiles"])).hexdigest()
            self.assertGreater(len(frozen_json(self.receipt)), 65536)
            with patch.object(driver, "ROOT", repository):
                self.assertRegex(self.validate(), r"^[0-9a-f]{64}$")
                selected = repository / next(reversed(self.receipt["sourceFiles"]))
                original = selected.read_bytes()
                selected.write_bytes(original + b"tampered")
                with self.assertRaisesRegex(EntryError, "backend_source_changed"):
                    self.validate()
                selected.write_bytes(original)
        self.receipt["sourceFiles"]["be/extra.java"] = "0" * 64
        with self.assertRaisesRegex(EntryError, "backend_receipt_rejected"):
            self.validate()

    def test_startup_receipt_256_kib_boundary_is_shared_by_config_and_validator(self):
        path = Path(self.values["backendStartupReceiptPath"])
        data = frozen_json(self.receipt)
        path.write_bytes(data + b" " * (256 * 1024 - len(data)))
        self.assertRegex(driver.validate_backend_startup(self.configuration(), inspector=self.inspect), r"^[0-9a-f]{64}$")
        path.write_bytes(path.read_bytes() + b" ")
        with self.assertRaisesRegex(EntryError, "file_size_rejected"):
            self.configuration()
        with self.assertRaisesRegex(EntryError, "file_size_rejected"):
            driver.validate_backend_startup(Configuration(self.values, "0" * 64), inspector=self.inspect)

    def test_label_or_401_and_tampered_pid_start_hash_profile_or_listener_cannot_admit(self):
        for key, value in (("schemaVersion", True), ("backendEnvironment", "production"), ("pid", True),
            ("processStartTime", "other"), ("sourceSha256", "0" * 64), ("artifactSha256", "0" * 64), ("configSha256", "0" * 64)):
            original = self.receipt[key]
            self.receipt[key] = value
            with self.subTest(key=key), self.assertRaises(EntryError):
                self.validate()
            self.receipt[key] = original
        self.command = b"java -Dspring.profiles.active=production -jar synthetic.jar"
        self.receipt["commandSha256"] = sha256(self.command).hexdigest()
        with self.assertRaisesRegex(EntryError, "backend_profile_unverified"):
            self.validate()
        self.command = b"java -Dspring.profiles.active=local -jar synthetic.jar"
        self.receipt["commandSha256"] = sha256(self.command).hexdigest()
        self.inspect = lambda pid, port: ("synthetic-start", self.command, b"p1234\nn*:18080\n")
        with self.assertRaisesRegex(EntryError, "backend_listener_unverified"):
            self.validate()


class SourceBindingInputs(PrivateInputs):
    def setUp(self):
        super().setUp()
        self.repository = self.base / "source-repository"
        self.mobile = self.repository / "app/mobile"
        fixtures = {"AGENTS.md": b"synthetic instructions", "tools/mobile-test-account.json": b"synthetic selection",
            "tools/image-optimization/native_entry_driver.py": b"synthetic host",
            "tools/image-optimization/native_fixture_server.py": b"synthetic server",
            "tools/image-optimization/pyproject.toml": b"synthetic project", "tools/image-optimization/uv.lock": b"synthetic lock"}
        self.inputs = {"pubspec.yaml": b"synthetic pubspec", "pubspec.lock": b"synthetic pub lock",
            "lib/entry.dart": b"synthetic Dart", "lib/generated.g.dart": b"synthetic generated Dart build input",
            "android/app/src/imageExperiment/AndroidManifest.xml": b"synthetic manifest",
            "android/app/proguard-rules.pro": b"synthetic proguard",
            "android/app/src/main/res/drawable/icon.xml": b"synthetic Android resource",
            "ios/Podfile": b"synthetic podfile", "ios/Podfile.lock": b"synthetic pod lock",
            "ios/ImageExperiment/build_guard.py": b"synthetic guard",
            "assets/fixture.svg": b"synthetic asset"}
        fixtures.update({"app/mobile/" + name: data for name, data in self.inputs.items()})
        self.ignored = {"build/private.bin": b"build", ".dart_tool/transient.json": b"transient",
            "ios/Pods/private.bin": b"pods", "ios/Flutter/Generated.xcconfig": b"generated",
            "android/local.properties": b"private local paths", "android/key.properties": b"private signing",
            "android/private.jks": b"private signing key", "ios/Runner/GeneratedPluginRegistrant.m": b"generated plugins"}
        self.ignored["android/app/profile/generated.apk"] = b"generated Android artifact"
        fixtures.update({"app/mobile/" + name: data for name, data in self.ignored.items()})
        for name, data in fixtures.items():
            path = self.repository / name
            path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
            path.write_bytes(data)
        self.enterContext(patch.object(driver, "ROOT", self.repository))
        self.enterContext(patch.object(driver, "MOBILE", self.mobile))


class SourceBindingTests(SourceBindingInputs):
    def test_every_selected_build_input_is_copied_and_hashed_and_transient_inputs_are_excluded(self):
        sources, digest = driver.source_snapshot()
        self.assertEqual(digest, sha256(frozen_json(sources)).hexdigest())
        output = self.base / "private-build"
        output.mkdir(mode=0o700)
        copy = driver.private_mobile_snapshot(output, sources)
        for name, data in self.inputs.items():
            with self.subTest(name=name):
                self.assertEqual(sources["app/mobile/" + name], sha256(data).hexdigest())
                self.assertEqual((copy / name).read_bytes(), data)
        for name in self.ignored:
            self.assertNotIn("app/mobile/" + name, sources)
            self.assertFalse((copy / name).exists())
        expected = {name: sha256(data).hexdigest() for name, data in sorted(self.inputs.items())}
        self.assertEqual(driver.validate_copied_sources(copy, sources), sha256(frozen_json(expected)).hexdigest())

    def test_representative_source_mutations_change_receipt_binding_while_transient_mutations_do_not(self):
        _, before = driver.source_snapshot()
        for name, data in self.inputs.items():
            path = self.mobile / name
            path.write_bytes(data + b"changed")
            with self.subTest(name=name):
                self.assertNotEqual(driver.source_snapshot()[1], before)
            path.write_bytes(data)
        for name in self.ignored:
            (self.mobile / name).write_bytes(b"changed transient")
        self.assertEqual(driver.source_snapshot()[1], before)

    def test_mutation_between_hash_and_copy_and_later_copy_mutation_refuse(self):
        sources, _ = driver.source_snapshot()
        output = self.base / "private-build"
        output.mkdir(mode=0o700)
        path = self.mobile / "pubspec.yaml"
        path.write_bytes(b"changed before copy")
        with self.assertRaisesRegex(EntryError, "source_changed_during_copy"):
            driver.private_mobile_snapshot(output, sources)
        path.write_bytes(self.inputs["pubspec.yaml"])
        second = self.base / "second-private-build"
        second.mkdir(mode=0o700)
        copy = driver.private_mobile_snapshot(second, sources)
        (copy / "ios/ImageExperiment/build_guard.py").write_bytes(b"changed copied build guard")
        with self.assertRaisesRegex(EntryError, "copied_source_changed"):
            driver.validate_copied_sources(copy, sources)

    def test_selection_file_count_and_symlink_bounds_refuse(self):
        with patch.object(driver, "MAX_SOURCE_FILES", 1), self.assertRaisesRegex(EntryError, "source_selection_bound_rejected|source_copy_capacity_rejected"):
            driver.selected_mobile_sources()
        (self.mobile / "assets/link.svg").symlink_to(self.mobile / "assets/fixture.svg")
        with self.assertRaisesRegex(EntryError, "source_symlink_rejected"):
            driver.source_snapshot()

    def test_android_local_settings_are_generated_privately_from_selected_tools(self):
        sources, _ = driver.source_snapshot()
        output = self.base / "private-build"
        output.mkdir(mode=0o700)
        copy = driver.private_mobile_snapshot(output, sources)
        flutter = self.base / "selected flutter"
        (flutter / "packages/flutter_tools/gradle").mkdir(parents=True, mode=0o700)
        (flutter / "bin").mkdir(mode=0o700)
        sdk = self.base / "selected Android sdk"
        sdk.mkdir(mode=0o700)
        with patch.object(driver.shutil, "which", return_value=str(flutter / "bin/flutter")), \
            patch.dict(os.environ, {"ANDROID_SDK_ROOT": str(sdk)}):
            driver.prepare_android_tool_settings(copy)
        settings = copy / "android/local.properties"
        self.assertEqual(settings.stat().st_mode & 0o777, 0o600)
        self.assertIn(b"flutter.sdk=", settings.read_bytes())
        self.assertIn(b"selected\\ flutter", settings.read_bytes())
        self.assertNotIn(b"private local paths", settings.read_bytes())
        driver.validate_copied_sources(copy, sources)


class PhysicalPrerequisiteTests(SourceBindingInputs):
    def setUp(self):
        super().setUp()
        self.app = self.file("signed-app.apk", b"synthetic signed application")
        self.test_app = self.file("signed-test.apk", b"synthetic same-signed test")
        sources, source_hash = driver.source_snapshot()
        self.build_output = self.base / "signed-build"
        self.build_output.mkdir(mode=0o700)
        copy = driver.private_mobile_snapshot(self.build_output, sources)
        self.build = {"scope": SCOPE, "platform": "android", "mode": "release",
            "expectedAppId": self.values["expectedAppId"], "dartTarget": "lib/main_image_experiment.dart",
            "artifactPath": str(self.app), "artifactSha256": sha256(self.app.read_bytes()).hexdigest(),
            "testArtifactPath": str(self.test_app), "testArtifactSha256": sha256(self.test_app.read_bytes()).hexdigest(),
            "certificateSha256": "c" * 64, "sourceSha256": source_hash, "sourceFiles": sources,
            "sourceCopyPath": str(copy), "copiedSourceSha256": driver.validate_copied_sources(copy, sources),
            "signingConfigSha256": sha256(Path(self.values["signingConfigFile"]).read_bytes()).hexdigest()}
        self.now = int(time.time() * 1000)
        self.proof = {"schemaVersion": 1, "scope": SCOPE, "batchId": self.values["batchId"], "platform": "android",
            "mode": "release", "deviceAlias": self.values["deviceAlias"],
            "deviceSelectorSha256": sha256(self.values["deviceSelector"].encode()).hexdigest(),
            "expectedAppId": self.values["expectedAppId"], "firebaseProjectId": self.values["firebaseProjectId"],
            "artifactSha256": self.build["artifactSha256"], "testArtifactSha256": self.build["testArtifactSha256"],
            "certificateSha256": self.build["certificateSha256"], "physical": True, "connected": True,
            "deviceModel": "synthetic physical device", "provider": "android_device_streaming", "observedAt": self.now,
            "projectAccepted": True, "iamVerified": True, "quotaVerified": True,
            "reservationExpiresAt": self.now + 300000, "reservationReceiptSha256": "d" * 64,
            "installedArtifactSha256": self.build["artifactSha256"], "installedTestArtifactSha256": self.build["testArtifactSha256"],
            "installationReceiptSha256": "e" * 64, "installationAuthorized": True, "authenticationAuthorized": True}
        self.build_path = self.file("build-receipt.json", frozen_json(self.build))
        self.proof_path = self.file("prerequisite-receipt.json", frozen_json(self.proof))

    def validate(self):
        self.build_path.write_bytes(frozen_json(self.build))
        self.proof_path.write_bytes(frozen_json(self.proof))
        return driver.validate_physical_prerequisites(self.configuration(), self.build_path, self.proof_path)

    def test_complete_synthetic_android_and_iphone_prerequisites_pass_real_validator(self):
        self.assertEqual(self.validate(), (self.build, self.proof))
        self.values.update(platform="ios", expectedAppId="com.tellpal.tellpalMobilePoc.imageexperiment.release")
        self.build.update(platform="ios", expectedAppId=self.values["expectedAppId"])
        self.proof.update(platform="ios", expectedAppId=self.values["expectedAppId"], deviceModel="iPhone 12", provider="local",
            reservationExpiresAt=None, reservationReceiptSha256=None)
        self.assertEqual(self.validate(), (self.build, self.proof))

    def test_false_authentication_and_installation_refuse_before_firebase_broker_server_or_launcher(self):
        for field in ("authenticationAuthorized", "installationAuthorized"):
            self.proof[field] = False
            self.proof_path.write_bytes(frozen_json(self.proof))
            with ExitStack() as stack:
                guards = [stack.enter_context(patch.object(driver, name)) for name in
                    ("FirebaseVerifier", "Broker", "create_server", "LauncherPairing", "checked_run", "validate_backend_startup")]
                with self.subTest(field=field), self.assertRaisesRegex(EntryError, "physical_prerequisite_unavailable"):
                    driver.verify_entry(self.configuration(), self.admitted_manifest(), self.base,
                        time.monotonic() + 10, case_id="case_a", build_path=self.build_path, prerequisite_path=self.proof_path)
                for guard in guards:
                    guard.assert_not_called()
            self.proof[field] = True

    def test_stale_device_project_artifact_certificate_installed_quota_reservation_and_authorization_bindings_refuse(self):
        cases = (("observedAt", self.now - 300001), ("observedAt", True), ("deviceAlias", "other"),
            ("deviceSelectorSha256", "0" * 64), ("firebaseProjectId", "other-project"), ("expectedAppId", "ordinary.app"),
            ("artifactSha256", "0" * 64), ("testArtifactSha256", "0" * 64), ("certificateSha256", "0" * 64),
            ("installedArtifactSha256", "0" * 64), ("installedTestArtifactSha256", "0" * 64),
            ("quotaVerified", False), ("reservationExpiresAt", self.now + 195000), ("reservationReceiptSha256", None),
            ("physical", False), ("connected", False), ("projectAccepted", False), ("iamVerified", False),
            ("authenticationAuthorized", False), ("installationAuthorized", False))
        for field, value in cases:
            original = self.proof[field]
            self.proof[field] = value
            with self.subTest(field=field, value=value), self.assertRaises(EntryError):
                self.validate()
            self.proof[field] = original

    def test_source_artifact_signing_and_copied_input_mutations_refuse_before_using_build_receipt(self):
        paths = (self.mobile / "android/app/src/imageExperiment/AndroidManifest.xml", self.app, self.test_app,
            Path(self.values["signingConfigFile"]), Path(self.build["sourceCopyPath"]) / "ios/Podfile.lock")
        for path in paths:
            original = path.read_bytes()
            path.write_bytes(original + b"changed")
            with self.subTest(path=path.name), self.assertRaises(EntryError):
                self.validate()
            path.write_bytes(original)
        self.build["sourceFiles"] = {}
        with self.assertRaisesRegex(EntryError, "build_binding_rejected"):
            self.validate()


class LauncherTests(BrokerInputs):
    def test_real_crlf_stdout_pairs_one_key_and_exact_native_result_without_changing_raw_trace(self):
        self.broker.probe(self.broker.session_id, self.broker.nonce)
        self.broker.probe(self.broker.session_id, self.broker.nonce, backend=True)
        parser = driver.LauncherPairing(self.broker, "ios")
        self.addCleanup(parser.close)
        raw = (b"IMAGE_EXPERIMENT_PAIRING " + base64.b64encode(frozen_json(self.pairing)) + b"\r\n"
            b"IMAGE_EXPERIMENT_RESULT settled\r\n")
        output = self.base / "paired-crlf"
        output.mkdir(mode=0o700)
        result = driver.run_bounded([sys.executable, "-c",
            "import os,sys;os.write(1,bytes.fromhex(sys.argv[1]))", raw.hex()],
            output, "paired-crlf", seconds=3, on_line=parser)
        self.assertEqual(result["status"], "ok")
        self.assertTrue(parser.ready.wait(timeout=1))
        parser.poll()
        self.assertEqual(self.broker.paired, self.der_key)
        self.assertEqual((parser.count, parser.native_result_count, parser.native_result), (1, 1, "settled"))
        self.assertEqual((output / "paired-crlf.stdout").read_bytes(), raw)
        self.assertEqual(result["traceSha256"][0], sha256(raw).hexdigest())

    def test_native_results_require_exact_positive_or_refused_platform_receipt(self):
        android = driver.LauncherPairing(self.broker, "android")
        android(b"INSTRUMENTATION_RESULT: reason=NATIVE_LAUNCHER_SETTLED")
        android(b"INSTRUMENTATION_CODE: -1")
        self.assertEqual((android.native_result, android.native_result_count), ("settled", 1))
        ios = driver.LauncherPairing(self.broker, "ios")
        ios(b"IMAGE_EXPERIMENT_RESULT refused")
        self.assertEqual((ios.native_result, ios.native_result_count), ("refused", 1))

    def test_android_diagnostic_result_field_cannot_pair_or_settle_a_refused_launcher(self):
        for stage in (b"NATIVE_METADATA", b"DART_COMPOSITION", b"unknown", b"raw_marker"):
            with self.subTest(stage=stage):
                parser = driver.LauncherPairing(self.broker, "android")
                self.addCleanup(parser.close)
                parser(b"INSTRUMENTATION_RESULT: diagnosticStage=" + stage)
                self.assertEqual((parser.count, parser.native_result_count), (0, 0))
                self.assertIsNone(parser.pending)
                self.assertFalse(parser.ready.is_set())
                self.assertEqual(parser.rows, {})
                parser(b"INSTRUMENTATION_RESULT: reason=NATIVE_LAUNCHER_REFUSED")
                parser(b"INSTRUMENTATION_CODE: 0")
                self.assertEqual((parser.count, parser.native_result_count, parser.native_result), (0, 1, "refused"))
                self.assertIsNone(self.broker.paired)
                self.assertEqual(self.calls, [])
                with self.assertRaisesRegex(EntryError, "launcher_pairing_unsettled"):
                    driver.validate_entry_outcome(self.broker, parser)

    def test_mismatched_missing_duplicate_or_unknown_native_results_refuse(self):
        for reason, code in ((None, b"-1"), (b"NATIVE_LAUNCHER_REFUSED", b"-1"),
            (b"NATIVE_LAUNCHER_SETTLED", b"0")):
            parser = driver.LauncherPairing(self.broker, "android")
            if reason:
                parser(b"INSTRUMENTATION_RESULT: reason=" + reason)
            with self.subTest(reason=reason), self.assertRaisesRegex(EntryError, "native_result_rejected"):
                parser(b"INSTRUMENTATION_CODE: " + code)
        parser = driver.LauncherPairing(self.broker, "ios")
        with self.assertRaisesRegex(EntryError, "native_result_rejected"):
            parser(b"IMAGE_EXPERIMENT_RESULT completed")
        parser(b"IMAGE_EXPERIMENT_RESULT settled")
        with self.assertRaisesRegex(EntryError, "native_result_rejected"):
            parser(b"IMAGE_EXPERIMENT_RESULT settled")

    def test_android_report_pairs_exactly_one_matching_launcher_key(self):
        self.broker.probe(self.broker.session_id, self.broker.nonce)
        self.broker.probe(self.broker.session_id, self.broker.nonce, backend=True)
        parser = driver.LauncherPairing(self.broker, "android")
        self.addCleanup(parser.close)
        for key, value in {"event": "pairing-key", **self.pairing}.items():
            parser(("INSTRUMENTATION_STATUS: " + key + "=" + value).encode())
        parser(b"INSTRUMENTATION_STATUS_CODE: 1")
        self.assertTrue(parser.ready.wait(timeout=1))
        parser.poll()
        self.assertEqual(parser.count, 1)
        self.assertEqual(self.broker.paired, self.der_key)
        for key, value in {"event": "pairing-key", **self.pairing}.items():
            parser(("INSTRUMENTATION_STATUS: " + key + "=" + value).encode())
        with self.assertRaisesRegex(EntryError, "launcher_replacement_rejected"):
            parser(b"INSTRUMENTATION_STATUS_CODE: 1")

    def test_ios_public_report_is_bound_and_not_an_untrusted_hello(self):
        parser = driver.LauncherPairing(self.broker, "ios")
        self.addCleanup(parser.close)
        line = b"IMAGE_EXPERIMENT_PAIRING " + base64.b64encode(frozen_json(self.pairing))
        parser(line)
        self.assertIsNone(self.broker.paired)
        self.assertEqual(parser.pending, self.pairing)
        self.assertEqual(self.calls, [])
        self.assertEqual(self.backend_calls, [])

    def test_report_before_probes_stays_pending_then_binds_once_after_both_legs(self):
        parser = driver.LauncherPairing(self.broker, "ios")
        self.addCleanup(parser.close)
        parser(b"IMAGE_EXPERIMENT_PAIRING " + base64.b64encode(frozen_json(self.pairing)))
        self.assertIsNone(self.broker.paired)
        self.broker.probe(self.broker.session_id, self.broker.nonce)
        self.assertIsNone(self.broker.paired)
        self.assertEqual(self.calls, [])
        self.broker.probe(self.broker.session_id, self.broker.nonce, backend=True)
        self.assertTrue(parser.ready.wait(timeout=1))
        parser.poll()
        self.assertEqual(self.broker.paired, self.der_key)
        self.assertIsNone(parser.pending)
        self.assertEqual(parser.count, 1)
        self.assertEqual(self.calls, [])
        self.assertEqual(self.backend_calls, [None])

    def test_pending_report_timeout_and_owned_close_cannot_admit_credentials(self):
        parser = driver.LauncherPairing(self.broker, "ios")
        self.addCleanup(parser.close)
        parser(b"IMAGE_EXPERIMENT_PAIRING " + base64.b64encode(frozen_json(self.pairing)))
        self.broker.deadline = 0
        self.assertTrue(parser.ready.wait(timeout=1))
        with self.assertRaisesRegex(EntryError, "reachability_deadline_exceeded"):
            parser.poll()
        parser.close()
        self.assertFalse(parser.worker.is_alive())
        self.assertIsNone(self.broker.token)
        self.assertEqual(self.calls, [])

    def test_host_revocation_wakes_and_settles_pending_report_without_pairing(self):
        parser = driver.LauncherPairing(self.broker, "ios")
        self.addCleanup(parser.close)
        parser(b"IMAGE_EXPERIMENT_PAIRING " + base64.b64encode(frozen_json(self.pairing)))
        self.broker.revoke("host_revoked")
        self.assertTrue(parser.ready.wait(timeout=1))
        with self.assertRaises(EntryError):
            parser.poll()
        self.assertIsNone(self.broker.paired)
        self.assertIsNone(self.broker.token)
        self.assertEqual(self.calls, [])

    def test_duplicate_fields_and_mismatched_session_refuse(self):
        parser = driver.LauncherPairing(self.broker, "android")
        parser(b"INSTRUMENTATION_STATUS: event=pairing-key")
        with self.assertRaisesRegex(EntryError, "launcher_duplicate_rejected"):
            parser(b"INSTRUMENTATION_STATUS: event=pairing-key")


class EntryOutcomeTests(BrokerInputs):
    def run_synthetic_entry(self, outcome):
        """Exercise the real driver and receipt writer with synthetic native I/O only."""
        class SyntheticServer:
            def serve_forever(self, **kwargs):
                pass
            def shutdown(self):
                pass
            def server_close(self):
                pass
        build = {"testAppId": "synthetic.test", "artifactSha256": "a" * 64,
            "testArtifactSha256": "b" * 64}
        def native_command(argv, output, alias, **kwargs):
            if alias == "device-state":
                return b"device\n"
            self.assertEqual(alias, "launcher")
            parser = kwargs["on_line"]
            self.broker.probe(self.broker.session_id, self.broker.nonce)
            self.broker.probe(self.broker.session_id, self.broker.nonce, backend=True)
            for name, value in {"event": "pairing-key", **self.pairing}.items():
                parser(("INSTRUMENTATION_STATUS: " + name + "=" + value).encode())
            parser(b"INSTRUMENTATION_STATUS_CODE: 1")
            self.assertTrue(parser.ready.wait(timeout=1))
            parser.poll()
            self.pair = lambda: None  # Already bound through the actual launcher parser.
            if outcome == "admission_failed":
                self.disabled = True
                with self.assertRaises(EntryError):
                    self.admit()
            else:
                self.admit()
                if outcome != "missing_fixture":
                    self.broker.completed_fixture("case_a", len(self.image), sha256(self.image).hexdigest())
                if outcome == "expired":
                    self.broker.expires_at = 0
                    self.assertEqual(self.broker.revoke_from_app(self.broker.session_id, self.auth)["status"], "expired")
                elif outcome == "host_revoked":
                    self.broker.revoke("host_revoked")
                elif outcome == "invalid_revoke":
                    self.disabled = True
                    self.broker.revoke_from_app(self.broker.session_id, self.auth)
                else:
                    self.broker.revoke_from_app(self.broker.session_id, self.auth)
            if outcome != "missing_native_result":
                refused = outcome == "native_refused"
                parser(b"INSTRUMENTATION_RESULT: reason=" +
                    (b"NATIVE_LAUNCHER_REFUSED" if refused else b"NATIVE_LAUNCHER_SETTLED"))
                parser(b"INSTRUMENTATION_CODE: " + (b"0" if refused else b"-1"))
            return b""
        stream = StringIO()
        with ExitStack() as stack:
            for name, value in (("validate_physical_prerequisites", lambda *args: (build, {})),
                ("validate_backend_startup", lambda *args: "c" * 64),
                ("FirebaseVerifier", lambda *args: self.verifier), ("Broker", lambda *args, **kwargs: self.broker),
                ("create_server", lambda *args: SyntheticServer()), ("checked_run", native_command)):
                stack.enter_context(patch.object(driver, name, value))
            stack.enter_context(redirect_stdout(stream))
            code = driver.main(["verify-entry", "--config", str(self.config_path), "--case-id", "case_a",
                "--build-receipt", str(self.base / "synthetic-build.json"),
                "--prerequisite-receipt", str(self.base / "synthetic-prerequisite.json")])
        return code, json.loads(stream.getvalue()), Path(self.values["privateOutputRoot"])

    def assert_refused_receipt(self, outcome, reason):
        code, report, output = self.run_synthetic_entry(outcome)
        self.assertEqual((code, report["status"], report["reason"]), (4, "refused", reason))
        self.assertFalse((output / "verification-receipt.json").exists())
        failure = json.loads((output / "failure-receipt.json").read_bytes())
        self.assertEqual((failure["status"], failure["reason"], failure["nativeEntryGate"]), ("failed", reason, "open"))

    def test_success_requires_admission_full_transfer_authenticated_app_revoke_and_native_settlement(self):
        code, report, output = self.run_synthetic_entry("success")
        self.assertEqual((code, report["status"]), (0, "completed"))
        receipt = json.loads((output / "verification-receipt.json").read_bytes())
        self.assertEqual(receipt["admission"], "observed")
        self.assertEqual(receipt["fixtureDeliveryCount"], 1)
        self.assertEqual(receipt["fixtureBodyBytes"], len(self.image))
        self.assertEqual(receipt["fixtureOutputSha256"], sha256(self.image).hexdigest())
        self.assertEqual(receipt["nativeResult"], "settled")
        self.assertEqual(receipt["teardown"], "authenticated_app_revoke_observed")
        self.assertIsNone(receipt["decodedAdmission"])
        self.assertIsNone(receipt["physicalPresentation"])
        self.assertEqual(receipt["nativeEntryGate"], "open")

    def test_consumed_claim_with_failed_admission_never_writes_positive_receipt(self):
        self.assert_refused_receipt("admission_failed", "entry_admission_unverified")
        self.assertTrue(self.broker.claimed)
        self.assertFalse(self.broker.admission_successful)

    def test_expired_session_cannot_be_promoted_by_late_app_revoke_or_settled_marker(self):
        self.assert_refused_receipt("expired", "entry_teardown_unverified")
        self.assertFalse(self.broker.explicit_app_revoked)

    def test_host_revocation_cannot_replace_authenticated_app_teardown(self):
        self.assert_refused_receipt("host_revoked", "entry_teardown_unverified")

    def test_failed_fresh_verification_at_app_revoke_never_writes_positive_receipt(self):
        self.assert_refused_receipt("invalid_revoke", "entry_teardown_unverified")

    def test_native_refusal_cannot_be_promoted_by_successful_broker_teardown(self):
        self.assert_refused_receipt("native_refused", "native_entry_refused")

    def test_absent_native_result_never_writes_positive_receipt(self):
        self.assert_refused_receipt("missing_native_result", "native_result_unverified")

    def test_missing_full_fixture_delivery_never_writes_positive_receipt(self):
        self.assert_refused_receipt("missing_fixture", "fixture_delivery_unverified")


class AdminRootCLITests(PrivateInputs):
    def test_nonobject_admin_roots_refuse_with_fixed_code_and_no_private_value_or_traceback(self):
        for index, value in enumerate((None, [], "private-value", 42, True)):
            self.values["privateOutputRoot"] = str(self.base / ("root-check-" + str(index)))
            self.configuration()
            Path(self.values["firebaseAdminCredentialFile"]).write_bytes(frozen_json(value))
            stream = StringIO()
            with patch.object(driver, "validate_physical_prerequisites", return_value=({}, {})), \
                patch.object(driver, "validate_backend_startup", return_value="a" * 64), \
                patch.object(driver, "Broker") as broker, patch.object(driver, "checked_run") as launcher, redirect_stdout(stream):
                code = driver.main(["verify-entry", "--config", str(self.config_path), "--case-id", "case_a",
                    "--build-receipt", str(self.base / "unused-build.json"),
                    "--prerequisite-receipt", str(self.base / "unused-prerequisite.json")])
                broker.assert_not_called()
                launcher.assert_not_called()
            with self.subTest(value=value):
                self.assertEqual(code, 4)
                self.assertEqual(json.loads(stream.getvalue())["reason"], "admin_credential_rejected")
                for forbidden in (str(self.base), "private-value", "Traceback", "AttributeError"):
                    self.assertNotIn(forbidden, stream.getvalue())
                self.assertFalse((Path(self.values["privateOutputRoot"]) / "verification-receipt.json").exists())


class CLITests(PrivateInputs):
    def test_validate_config_has_only_sanitized_hashes_and_keeps_native_gate_open(self):
        stream = StringIO()
        with redirect_stdout(stream):
            code = driver.main(["validate-config", "--config", str(self.config_path)])
        self.assertEqual(code, 0)
        report = json.loads(stream.getvalue())
        self.assertEqual(report["nativeEntryGate"], "open")
        self.assertEqual(report["physicalVerification"], "not_run")
        self.assertNotIn(str(self.base), stream.getvalue())
        self.assertNotIn("synthetic-device", stream.getvalue())
        self.assertNotIn("synthetic.id.token", stream.getvalue())

    def test_physical_verify_without_exact_receipts_cannot_create_broker_or_call_auth(self):
        stream = StringIO()
        with patch.object(driver, "FirebaseVerifier") as verifier, redirect_stdout(stream):
            code = driver.main(["verify-entry", "--config", str(self.config_path)])
        self.assertEqual(code, 4)
        verifier.assert_not_called()
        self.assertIn("physical_prerequisite_unavailable", stream.getvalue())
        self.assertFalse(Path(self.values["privateOutputRoot"]).exists())

    def test_error_console_omits_private_values_paths_and_stack(self):
        self.values["secret"] = "must-not-appear"
        self.configuration = lambda: None
        self.config_path.write_bytes(frozen_json(self.values))
        stream = StringIO()
        with redirect_stdout(stream):
            code = driver.main(["validate-config", "--config", str(self.config_path)])
        self.assertEqual(code, 4)
        self.assertNotIn("must-not-appear", stream.getvalue())
        self.assertNotIn(str(self.base), stream.getvalue())
        self.assertNotIn("Traceback", stream.getvalue())


if __name__ == "__main__":
    unittest.main()

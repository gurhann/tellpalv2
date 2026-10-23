"""Emülatör zincirinin yanlış ortam ve eski kanıtı reddetme testleri."""
from copy import deepcopy
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import local_lossless_emulator as emulator
import local_lossless_mobile as base
import test_local_lossless_mobile as shared_tests
from native_entry_config import EntryError, frozen_json


class EmulatorTest(unittest.TestCase):
    def setUp(self):
        shared_tests.LocalLosslessMobileTest.setUp(self)
        self.tool_patch = patch.object(emulator.shutil, "which", return_value="/usr/bin/sandbox-exec")
        self.tool_patch.start()
        self.public = self.directory / "public"
        self.public.mkdir()
        self.target = self.public / "attempts/emulator-new-attempt"
        self.observed = {"selector": emulator.SELECTOR, "state": "device", "qemu": "1", "abi": emulator.ABI,
            "api": "36", "fingerprint": "private/emulator/fingerprint", "avd": "private-avd-name",
            "wmSize": "Physical size: 1080x2400\nOverride size: 822x1828",
            "wmDensity": "Physical density: 420\nOverride density: 320"}

    def tearDown(self):
        self.tool_patch.stop()
        shared_tests.LocalLosslessMobileTest.tearDown(self)

    def trace(self, output, alias, argv, stdout=b"", status="ok"):
        base.write_new(output / (alias + ".stdout"), stdout)
        base.write_new(output / (alias + ".stderr"), b"")
        base.write_new(output / (alias + ".json"), {"status": status,
            "argumentSha256": base.digest(frozen_json(argv)),
            "runtimeEnvironmentSha256": base.digest(frozen_json(base.runtime_environment_binding(self.directory))),
            "traceSha256": [base.file_hash(output / (alias + suffix)) for suffix in (".stdout", ".stderr")]})

    def fixture(self):
        output = self.directory / "build-output"
        mobile = output / "source/repository/app/mobile"
        (mobile / "build/app/outputs/apk/profile").mkdir(parents=True)
        (mobile / "android").mkdir()
        (mobile / "assets/local_lossless").mkdir(parents=True)
        base.write_new(mobile / "pubspec.yaml", b"name: qa\nflutter:\n  assets:\n    - assets/library/\n")
        base.write_new(mobile / "pubspec.lock", b"pub-lock")
        (mobile / "test_driver").mkdir()
        base.write_new(mobile / emulator.ENTRY, b"emulator-entry")
        base.write_new(mobile / emulator.DRIVER, b"unchanged-driver")
        fixture_rows = deepcopy(self.cases)
        for row in fixture_rows:
            data = (row["caseId"] * 8).encode()
            row["bytes"], row["sha256"] = len(data), base.digest(data)
            base.write_new(mobile / row["asset"], data)
        manifest = {"schemaVersion": 1, "evidenceSha256": base.EVIDENCE_SHA, "cases": fixture_rows}
        base.write_new(mobile / emulator.MANIFEST, manifest)
        sources = {"app/mobile/" + str(path.relative_to(mobile)): base.file_hash(path)
            for path in (mobile / "pubspec.yaml", mobile / "pubspec.lock", mobile / emulator.ENTRY, mobile / emulator.DRIVER)}
        prepared = {"schemaVersion": 1, "scope": emulator.SCOPE, "mode": "profile", "appId": emulator.APP_ID,
            "dartTarget": emulator.ENTRY, "driver": emulator.DRIVER, "abi": emulator.ABI, "targetPlatform": "android-arm64",
            "deviceSelector": emulator.SELECTOR, "physicalDevice": False, "runNonce": self.nonce,
            "sourceSha256": base.digest(frozen_json(sources)), "sourceFiles": sources,
            "sourceCopyPath": str(mobile), "buildOutputPath": str(output), "fixtureEvidenceSha256": base.EVIDENCE_SHA,
            "manifestSha256": base.file_hash(mobile / emulator.MANIFEST), "privatePubspecSha256": base.file_hash(mobile / "pubspec.yaml"),
            "pubspecLockSha256": base.file_hash(mobile / "pubspec.lock"), "entrySha256": base.file_hash(mobile / emulator.ENTRY),
            "driverSha256": base.file_hash(mobile / emulator.DRIVER)}
        prepared["runtimeEnvironment"] = base.runtime_environment_binding(self.directory)
        base.write_new(self.directory / emulator.INIT_NAME, emulator.init_script(self.nonce))
        prepared["initScriptSha256"] = base.file_hash(self.directory / emulator.INIT_NAME)
        selected = {"flutter": "/offline/flutter/bin/flutter", "aapt": "/offline/sdk/aapt", "adb": "/offline/sdk/adb",
            "profileEngineJar": str(self.directory / "profile-engine.jar"), "sdk": "/offline/sdk", "java": "/offline/java"}
        engine = b"profile-engine"
        with ZipFile(selected["profileEngineJar"], "w") as archive:
            archive.writestr(emulator.JAR_ENGINE, engine)
        os.chmod(selected["profileEngineJar"], 0o600)
        base.write_new(mobile / "android/local.properties", b"offline-sdk-paths")
        apk = mobile / "build/app/outputs/apk/profile/app-profile.apk"
        elf = bytearray(100)
        elf[:6] = b"\x7fELF\x02\x01"
        elf[18:20] = (183).to_bytes(2, "little")
        with ZipFile(apk, "w") as archive:
            archive.writestr(emulator.JAR_ENGINE, engine)
            archive.writestr("lib/arm64-v8a/libapp.so", elf)
            for path in (mobile / "assets/local_lossless").iterdir():
                archive.writestr("assets/flutter_assets/" + str(path.relative_to(mobile)), path.read_bytes())
        os.chmod(apk, 0o600)
        badging = (f"package: name='{emulator.APP_ID}' versionCode='1'\n"
            f"launchable-activity: name='{emulator.ACTIVITY}' label=''\n"
            "native-code: 'arm64-v8a'\n").encode()
        with patch.object(base, "fixtures", return_value=({}, fixture_rows)):
            identity = emulator.inspect_apk(apk, badging, prepared, base.digest(engine))
        command = emulator.build_command(mobile, self.directory, self.nonce)
        self.trace(output, "gradle-profile", ["sandbox-exec", "-p", emulator.LOCAL_NETWORK_POLICY, *command])
        self.trace(output, "apk-badging", ["sandbox-exec", "-p", emulator.LOCAL_NETWORK_POLICY,
            selected["aapt"], "dump", "badging", str(apk)], badging)
        built = {"schemaVersion": 1, "scope": emulator.SCOPE, "mode": "profile", "appId": emulator.APP_ID,
            "dartTarget": emulator.ENTRY, "abi": emulator.ABI,
            **{key: prepared[key] for key in ("runNonce", "sourceSha256", "manifestSha256", "entrySha256",
                "initScriptSha256", "pubspecLockSha256")},
            "offlineBuild": True, "compiledDartDefinesSha256": base.digest(base.dart_defines(self.nonce).encode()),
            "runtimeEnvironmentSha256": base.digest(frozen_json(prepared["runtimeEnvironment"])),
            "artifactPath": str(apk), "artifactSha256": base.file_hash(apk), "artifactIdentity": identity, "tools": selected,
            "flutterVersion": "3.41.1", "engineRevision": "3" * 40,
            "localPropertiesSha256": base.file_hash(mobile / "android/local.properties"),
            "buildArgumentSha256": base.digest(frozen_json(command)),
            "buildToolReceiptSha256": base.file_hash(output / "gradle-profile.json"),
            "badgingToolReceiptSha256": base.file_hash(output / "apk-badging.json")}
        base.write_new(self.directory / "build-receipt.json", built)
        return prepared, built, fixture_rows

    def fixture_patches(self, prepared, built, rows):
        from contextlib import ExitStack
        stack = ExitStack()
        stack.enter_context(patch.object(emulator, "tools", return_value=built["tools"]))
        stack.enter_context(patch.object(emulator.shutil, "which", return_value="/usr/bin/sandbox-exec"))
        stack.enter_context(patch.object(base, "fixtures", return_value=({}, rows)))
        stack.enter_context(patch.object(emulator, "local_sources", return_value=(prepared["sourceFiles"], prepared["sourceSha256"])))
        return stack

    def launch_fixture(self):
        prepared, built, rows = self.fixture()
        result_dir = self.directory / "device-run"
        result_dir.mkdir(mode=0o700)
        result = {"runNonce": self.nonce, "vmServiceUrlSha256": "f" * 64,
            "vmServiceUrlSource": "flutter_drive_environment"}
        result_path = result_dir / "evidence.json"
        base.write_new(result_path, result)
        with self.fixture_patches(prepared, built, rows):
            binding = emulator.launch_binding(prepared, built, self.directory)
        base.write_new(self.directory / "launch-attempt.json", {**binding, "status": "STARTED"})
        base.write_new(self.directory / "run-device.private.json", self.observed)
        self.trace(self.directory / "build-output", "flutter-drive", emulator.launch_command(prepared, built))
        launch = {**binding, "status": "PASS", "toolStatus": "ok", "driverResultPath": str(result_path),
            "driverResultSha256": base.file_hash(result_path), "launchAttemptSha256": base.file_hash(self.directory / "launch-attempt.json"),
            "deviceEvidenceSha256": base.file_hash(self.directory / "run-device.private.json"),
            "deviceIdentitySha256": emulator.validate_device(self.observed), **{key: result[key] for key in
                ("vmServiceUrlSha256", "vmServiceUrlSource")},
            "toolReceiptSha256": base.file_hash(self.directory / "build-output/flutter-drive.json")}
        return prepared, built, rows, launch, result, result_path

    def test_exact_emulator_observation_accepts_only_the_pinned_environment(self):
        self.assertRegex(emulator.validate_device(self.observed), r"^[a-f0-9]{64}$")
        for field, wrong in (("selector", "physical-123"), ("selector", "emulator-5556"), ("state", "offline"),
            ("qemu", "0"), ("abi", "x86_64"), ("api", "35"), ("fingerprint", ""), ("avd", ""),
            ("wmSize", "Physical size: 1080x2400"), ("wmDensity", "Override density: 420")):
            value = {**self.observed, field: wrong}
            with self.subTest(field=field, wrong=wrong), self.assertRaisesRegex(EntryError, "emulator_identity_rejected"):
                emulator.validate_device(value)

    def test_device_queries_are_read_only_exact_and_stage_exclusive(self):
        (self.directory / "build-output").mkdir()
        commands = []
        def query(argv, output, alias, **kwargs):
            commands.append(argv)
            key = {"state": "state", "qemu": "qemu", "abi": "abi", "api": "api", "fingerprint": "fingerprint",
                "avd": "avd", "wmsize": "wmSize", "wmdensity": "wmDensity"}[alias.split("-device-")[-1]]
            return self.observed[key].encode()
        with patch.object(emulator, "checked", side_effect=query):
            self.assertEqual(emulator.observe_device(self.directory, {"adb": "/offline/adb"}, "run"), self.observed)
            with self.assertRaises(FileExistsError):
                emulator.observe_device(self.directory, {"adb": "/offline/adb"}, "run")
        self.assertTrue(all(command[1:3] == ["-s", emulator.SELECTOR] for command in commands))
        self.assertFalse(any(word in ("setprop", "install", "start", "reset", "kill-server", "size", "density")
            for command in commands for word in command[:3]))
        self.assertFalse(any(command[-1].isdigit() for command in commands))

    def test_profile_init_script_and_command_lock_target_package_abi_nonce_and_offline(self):
        value = emulator.init_script(self.nonce).decode()
        command = emulator.build_command(self.directory / "mobile", self.directory, self.nonce)
        for text in (emulator.ENTRY, emulator.APP_ID, "android-arm64", ":app:assembleProfile", "imageExperiment",
            "gradle.startParameter.offline", base.dart_defines(self.nonce)):
            self.assertIn(text, value)
        self.assertIn("--offline", command)
        self.assertIn("--no-daemon", command)
        self.assertIn("-Ptarget=" + emulator.ENTRY, command)
        self.assertNotIn("-PimageExperiment=true", command)
        self.assertIn("android.buildTypes.getByName('profile').ndk.abiFilters.clear()", value)
        self.assertIn("android.buildTypes.getByName('profile').ndk.abiFilters.add('arm64-v8a')", value)
        self.assertIn("android.packaging.jniLibs.keepDebugSymbols.add('**/libflutter.so')", value)
        with self.assertRaisesRegex(EntryError, "run_nonce_rejected"):
            emulator.init_script("stale")

    def test_rehashed_init_cannot_remove_profile_filter_or_broaden_symbol_retention(self):
        prepared, built, rows = self.fixture()
        init = self.directory / emulator.INIT_NAME
        original = init.read_text()
        for changed in (original.replace("android.buildTypes.getByName('profile').ndk.abiFilters.clear()", ""),
            original.replace("keepDebugSymbols.add('**/libflutter.so')", "keepDebugSymbols.add('**/*.so')")):
            init.write_text(changed)
            altered = {**prepared, "initScriptSha256": base.file_hash(init)}
            with self.fixture_patches(prepared, built, rows), self.assertRaisesRegex(EntryError, "private_snapshot_changed"):
                emulator.validate_snapshot(altered, self.directory)

    def test_large_profile_engine_has_a_specific_bound_and_other_archive_entries_keep_their_limit(self):
        archive_path = self.directory / "large-engine.jar"
        chunk = b"\0" * (1024 * 1024)
        expected = base.sha256()
        with ZipFile(archive_path, "w", compression=8) as archive:
            with archive.open(emulator.JAR_ENGINE, "w") as stream:
                for _ in range(129):
                    stream.write(chunk)
                    expected.update(chunk)
        self.assertEqual(emulator.profile_engine({"profileEngineJar": str(archive_path)}), expected.hexdigest())
        with ZipFile(archive_path) as archive, self.assertRaisesRegex(EntryError, "apk_archive_rejected"):
            emulator.archive_bytes(archive, emulator.JAR_ENGINE)

    def test_apk_profile_engine_package_launcher_abi_and_fixture_are_verified_from_archive(self):
        prepared, built, rows = self.fixture()
        apk = Path(built["artifactPath"])
        badging = (self.directory / "build-output/apk-badging.stdout").read_bytes()
        with self.fixture_patches(prepared, built, rows):
            self.assertEqual(emulator.validate_build(prepared, built, self.directory), built["artifactSha256"])
            for original, replacement in ((emulator.APP_ID, "normal.package"), (emulator.ACTIVITY, "WrongActivity"),
                ("'arm64-v8a'", "'x86_64'")):
                with self.subTest(original=original), self.assertRaises(EntryError):
                    emulator.inspect_apk(apk, badging.replace(original.encode(), replacement.encode()), prepared,
                        built["artifactIdentity"]["profileEngineSha256"])
            with self.assertRaisesRegex(EntryError, "apk_profile_engine_rejected"):
                emulator.inspect_apk(apk, badging, prepared, "0" * 64)
            with self.assertRaisesRegex(EntryError, "apk_fixture_rejected"):
                emulator.inspect_apk(apk, badging, {**prepared, "manifestSha256": "0" * 64},
                    built["artifactIdentity"]["profileEngineSha256"])

    def test_rehashed_wrong_abi_and_non_aot_binary_are_rejected_inside_apk(self):
        prepared, built, rows = self.fixture()
        apk = Path(built["artifactPath"])
        with ZipFile(apk) as archive:
            contents = {name: archive.read(name) for name in archive.namelist()}
        badging = (self.directory / "build-output/apk-badging.stdout").read_bytes()
        for mutate in (lambda data: data.update({"lib/x86_64/libwrong.so": b"wrong"}),
            lambda data: data.update({"lib/arm64-v8a/libapp.so": b"debug-or-wrong-architecture"}),
            lambda data: data.update({"assets/flutter_assets/assets/local_lossless/9778.png": b"changed"})):
            altered = dict(contents)
            mutate(altered)
            with ZipFile(apk, "w") as archive:
                for name, value in altered.items():
                    archive.writestr(name, value)
            with self.fixture_patches(prepared, built, rows), self.assertRaises(EntryError):
                emulator.inspect_apk(apk, badging, prepared, built["artifactIdentity"]["profileEngineSha256"])

    def test_build_binding_rejects_stale_mode_target_lock_init_nonce_and_apk(self):
        prepared, built, rows = self.fixture()
        with self.fixture_patches(prepared, built, rows):
            for field in ("mode", "appId", "dartTarget", "abi", "runNonce", "sourceSha256", "manifestSha256",
                "entrySha256", "initScriptSha256", "pubspecLockSha256", "compiledDartDefinesSha256", "artifactPath",
                "artifactSha256", "buildArgumentSha256", "buildToolReceiptSha256", "badgingToolReceiptSha256"):
                value = {**built, field: "wrong-binding"}
                with self.subTest(field=field), self.assertRaises((EntryError, FileNotFoundError)):
                    emulator.validate_build(prepared, value, self.directory)
            Path(built["artifactPath"]).write_bytes(b"changed APK")
            with self.assertRaisesRegex(EntryError, "build_binding_rejected"):
                emulator.validate_build(prepared, built, self.directory)

    def test_private_snapshot_rejects_wrong_device_mode_entry_and_changed_fixture_or_init(self):
        prepared, built, rows = self.fixture()
        with self.fixture_patches(prepared, built, rows):
            emulator.validate_snapshot(prepared, self.directory)
            for field, value in (("deviceSelector", "physical-123"), ("mode", "debug"), ("dartTarget", "lib/main.dart"),
                ("appId", "normal.package"), ("physicalDevice", True), ("abi", "x86_64")):
                with self.subTest(field=field), self.assertRaisesRegex(EntryError, "prepare_scope_rejected"):
                    emulator.validate_snapshot({**prepared, field: value}, self.directory)
            copied = Path(prepared["sourceCopyPath"]) / rows[0]["asset"]
            copied.write_bytes(b"changed fixture")
            with self.assertRaisesRegex(EntryError, "copied_fixture_changed"):
                emulator.validate_snapshot(prepared, self.directory)

    def test_launch_requires_exact_apk_emulator_entry_driver_and_loopback_vm_nonce_chain(self):
        prepared, built, rows, launch, result, path = self.launch_fixture()
        command = emulator.launch_command(prepared, built)
        for required in ("--profile", "--no-pub", "--no-keep-app-running",
            "--use-application-binary=" + built["artifactPath"], emulator.ENTRY, emulator.DRIVER, emulator.SELECTOR):
            self.assertIn(required, command)
        self.assertNotIn("--flavor", command)
        with self.fixture_patches(prepared, built, rows):
            self.assertEqual(emulator.validate_launch(prepared, built, launch, result, path, self.directory), built["artifactSha256"])
            for field in ("deviceSelector", "artifactPath", "artifactSha256", "argumentSha256", "runNonce", "dartTarget",
                "sourceSha256", "buildReceiptSha256", "driverResultPath", "driverResultSha256", "launchAttemptSha256",
                "toolReceiptSha256", "deviceEvidenceSha256", "deviceIdentitySha256", "vmServiceUrlSha256", "vmServiceUrlSource"):
                with self.subTest(field=field), self.assertRaises(EntryError):
                    emulator.validate_launch(prepared, built, {**launch, field: "wrong-binding"}, result, path, self.directory)
            for field, value in (("runNonce", "b2" * 32), ("vmServiceUrlSha256", "1" * 64), ("vmServiceUrlSource", "guessed")):
                with self.subTest(result_field=field), self.assertRaises(EntryError):
                    emulator.validate_launch(prepared, built, launch, {**result, field: value}, path, self.directory)

    def test_successful_driver_without_launch_chain_publishes_fail_and_zero_accepted_samples(self):
        prepared, built, rows = self.fixture()
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        path = self.directory / "driver.json"
        base.write_new(path, self.driver)
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "PUBLIC", self.public):
            report = emulator.verify(self.directory, path, public=self.target)
        self.assertEqual(report["status"], "FAIL")
        self.assertEqual(report["acceptedCaseCount"], 0)
        self.assertEqual(report["acceptedSamplesPerFormatPerAsset"], 0)
        self.assertEqual(report["plannedSamplesPerFormatPerAsset"], 1)
        self.assertFalse(report["physicalDevice"])
        self.assertFalse(report["performanceAccepted"])
        self.assertFalse(report["humanQualityAccepted"])

    def test_no_run_does_not_claim_observed_samples_and_public_projection_hides_private_values(self):
        prepared, built, rows = self.fixture()
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "PUBLIC", self.public):
            report = emulator.verify(self.directory, not_run_reason="OFFLINE_CACHE_MISSING", public=self.target)
        self.assertEqual(report["status"], "NOT_RUN")
        self.assertEqual(report["acceptedSamplesPerFormatPerAsset"], 0)
        public = (self.target / "verification.json").read_text()
        for private in (self.nonce, emulator.SELECTOR, self.observed["avd"], self.observed["fingerprint"],
            prepared["sourceCopyPath"], "VM_SERVICE_URL", "signingConfig"):
            self.assertNotIn(private, public)
        self.assertEqual(report["environment"], "android_emulator")
        self.assertEqual(report["realOsBackgroundTest"], "NOT_RUN")
        text = (self.target / "verification.md").read_text()
        self.assertNotIn("# Fiziksel iPhone", text)
        self.assertIn("HTTP transferi ölçülmedi", text)

    def test_public_evidence_only_uses_new_attempt_and_never_overwrites(self):
        with patch.object(emulator, "PUBLIC", self.public):
            for bad in (None, self.public, self.directory / "outside", self.public / "elsewhere/id",
                self.public / "attempts/../escape", self.public / "attempts/unsafe_name"):
                with self.subTest(path=bad), self.assertRaises(EntryError):
                    emulator.destination(bad)
            report = {"status": "NOT_RUN", "reason": "DRIVER_NOT_RUN", "cases": []}
            emulator.publish(report, [], None, self.target)
            before = base.tree_hashes(self.target)
            with self.assertRaisesRegex(EntryError, "public_evidence_exists"):
                emulator.publish(report, [], None, self.target)
            self.assertEqual(base.tree_hashes(self.target), before)
        other = self.directory / "outside"
        other.mkdir()
        linked = self.directory / "linked-public"
        linked.mkdir()
        (linked / "attempts").symlink_to(other, target_is_directory=True)
        with patch.object(emulator, "PUBLIC", linked), self.assertRaisesRegex(EntryError, "public_path_rejected"):
            emulator.destination(linked / "attempts/new")

    def test_private_paths_symlink_escape_and_receipt_overwrite_are_rejected(self):
        path = self.directory / "receipt.json"
        base.write_new(path, {"original": True})
        before = path.read_bytes()
        with self.assertRaises(FileExistsError):
            base.write_new(path, {"original": False})
        self.assertEqual(path.read_bytes(), before)
        link = self.directory / "link.json"
        link.symlink_to(path)
        with self.assertRaises(EntryError):
            base.private_json(link)
        with self.assertRaisesRegex(EntryError, "private_binding_path_rejected"):
            emulator.inside(Path("/etc/passwd"), self.directory)
        with self.assertRaisesRegex(EntryError, "output_not_unique"):
            emulator.prepare(self.directory)

    def test_offline_build_uses_existing_guard_and_environment_removes_credentials(self):
        with patch.dict(os.environ, {"FIREBASE_TOKEN": "private-token", "VM_SERVICE_URL": "private-vm", "PUB_HOSTED_URL": "private-host"}):
            env = emulator.environment()
        for secret in ("FIREBASE_TOKEN", "VM_SERVICE_URL", "PUB_HOSTED_URL"):
            self.assertNotIn(secret, env)
        with patch.object(emulator.shutil, "which", return_value=None), self.assertRaisesRegex(EntryError, "offline_guard_unavailable"):
            emulator.offline_command(["flutter", "pub", "get", "--offline"])
        with patch.object(emulator.shutil, "which", return_value="/usr/bin/sandbox-exec"):
            self.assertEqual(emulator.offline_command(["gradlew", "--offline"])[:3],
                ["sandbox-exec", "-p", emulator.LOCAL_NETWORK_POLICY])

    def test_missing_tools_record_not_run_without_starting_build_or_online_fallback(self):
        prepared, built, rows = self.fixture()
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "validate_snapshot"), \
            patch.object(emulator, "tools", side_effect=EntryError("build_tools_unavailable")), \
            patch.object(emulator, "checked") as checked:
            with self.assertRaises(EntryError):
                emulator.build(self.directory)
            checked.assert_not_called()
        receipt = base.private_json(self.directory / "build-failure.json")
        self.assertEqual(receipt["status"], "NOT_RUN")
        self.assertEqual(receipt["reason"], "TOOLS_UNAVAILABLE")
        with self.assertRaises(FileExistsError):
            base.write_new(self.directory / "build-attempt.json", {})

    def test_second_run_is_rejected_before_device_or_driver_execution(self):
        prepared, built, rows, launch, result, path = self.launch_fixture()
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "observe_device") as observe, \
            patch.object(base, "bounded_tool") as bounded:
            with self.assertRaisesRegex(EntryError, "launch_attempt_exists"):
                emulator.run(self.directory)
            observe.assert_not_called()
            bounded.assert_not_called()

    def test_inherited_driver_rejects_stale_nonce_missing_screen_rgb_paint_and_owned_resources(self):
        base.validate_driver(self.driver, self.cases, self.directory, self.nonce)
        mutations = [lambda result: result.update(runNonce="b2" * 32), lambda result: result["captures"].pop(),
            lambda result: result["stopped"]["cases"][0].update(rgbSha256="0" * 64),
            lambda result: result["observations"][0]["painted"].update(paintedEpoch=999),
            lambda result: result["stopped"].update(activeImages=1)]
        for mutate in mutations:
            value = deepcopy(self.driver)
            mutate(value)
            with self.subTest(mutate=mutate), self.assertRaises(EntryError):
                base.validate_driver(value, self.cases, self.directory, self.nonce)

    def test_preservation_detects_old_evidence_and_native_output_mutation(self):
        tree = self.directory / "old-build"
        tree.mkdir()
        old = tree / "artifact"
        base.write_new(old, b"unchanged")
        protected = {"files": {}, "trees": {str(tree): base.tree_hashes(tree)}}
        self.assertTrue(emulator.assert_protected(protected)["unchanged"])
        old.write_bytes(b"changed")
        with self.assertRaisesRegex(EntryError, "preservation_changed"):
            emulator.assert_protected(protected)

    def test_timeout_only_stops_owned_process_group(self):
        external = subprocess.Popen([sys.executable, "-B", "-c", "import time; time.sleep(20)"], start_new_session=True)
        try:
            status = base.bounded_tool([sys.executable, "-B", "-c", "import time; time.sleep(10)"],
                self.directory, "owned-timeout", seconds=.05)
            self.assertEqual(status, "timeout")
            self.assertIsNone(external.poll())
        finally:
            external.terminate()
            external.wait(timeout=5)


    def test_malicious_prepare_digest_never_leaks_into_failed_public_report(self):
        prepared, built, rows = self.fixture()
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        for index, key in enumerate(("sourceSha256", "pubspecLockSha256", "manifestSha256", "entrySha256", "initScriptSha256")):
            changed = {**prepared, key: "/private/tmp/private-token-and-nonce"}
            receipt = self.directory / "prepare-receipt.json"
            if receipt.exists():
                receipt.unlink()
            base.write_new(receipt, changed)
            with patch.object(emulator, "PUBLIC", self.public):
                report = emulator.verify(self.directory, public=self.public / f"attempts/rejected-{index}")
            self.assertEqual(report["status"], "FAIL")
            self.assertNotIn("private-token-and-nonce", json.dumps(report))

    def test_successful_verify_accepts_six_samples_and_redacts_raw_runtime_and_device(self):
        prepared, built, rows, launch, result, path = self.launch_fixture()
        for measured, pin in zip(self.results, rows):
            measured.update(encodedBytes=pin["bytes"], encodedSha256=pin["sha256"])
        result = {**self.driver, "vmServiceUrlSha256": "f" * 64, "vmServiceUrlSource": "flutter_drive_environment"}
        path.unlink()
        base.write_new(path, result)
        launch["driverResultSha256"] = base.file_hash(path)
        base.write_new(self.directory / "launch-receipt.json", launch)
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        for screen in self.screenshots:
            base.write_new(path.parent / screen["screenshotName"], (self.directory / screen["screenshotName"]).read_bytes())
        def observe(*args):
            base.write_new(self.directory / "verify-device.private.json", self.observed)
            return self.observed
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "PUBLIC", self.public), \
            patch.object(emulator, "observe_device", side_effect=observe):
            report = emulator.verify(self.directory, path, public=self.target)
        self.assertEqual(report["status"], "PASS")
        self.assertEqual(report["acceptedCaseCount"], 6)
        self.assertEqual(report["acceptedSamplesPerFormatPerAsset"], 1)
        self.assertEqual(len(report["cases"]), 6)
        self.assertEqual(len(report["screenshots"]), 6)
        public = json.dumps(report)
        for private in (self.nonce, emulator.SELECTOR, self.observed["avd"], self.observed["fingerprint"], str(self.directory)):
            self.assertNotIn(private, public)
        self.assertFalse(report["performanceAccepted"])
        self.assertFalse(report["physicalDevice"])
        self.assertEqual(report["stopped"]["activeImages"], 0)

    def test_changed_emulator_after_launch_is_rejected_even_with_valid_six_driver_cases(self):
        prepared, built, rows, launch, result, path = self.launch_fixture()
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        base.write_new(self.directory / "launch-receipt.json", launch)
        changed = {**self.observed, "fingerprint": "other-emulator-fingerprint"}
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "PUBLIC", self.public), \
            patch.object(base, "validate_driver", return_value={"cases": [], "screenshots": [], "stopped": {}}), \
            patch.object(emulator, "observe_device", return_value=changed):
            report = emulator.verify(self.directory, path, public=self.target)
        self.assertEqual(report["status"], "FAIL")
        self.assertEqual(report["acceptedCaseCount"], 0)

    def test_preservation_failure_still_has_an_exclusive_failure_receipt(self):
        protected = {"files": {}, "trees": {"/private/tmp/does-not-exist-emulator-test": {}}}
        self.assertFalse(emulator.preservation_result(protected)["unchanged"])
        prepared, built, rows = self.fixture()
        base.write_new(self.directory / "prepare-receipt.json", prepared)
        base.write_new(self.directory / "preservation-before.private.json", {"files": {}, "trees": {}})
        with self.fixture_patches(prepared, built, rows), patch.object(emulator, "validate_snapshot"), \
            patch.object(emulator, "tools", side_effect=EntryError("build_tools_unavailable")), \
            patch.object(emulator, "preservation_result", return_value={"unchanged": False}):
            with self.assertRaises(EntryError):
                emulator.build(self.directory)
        self.assertFalse(base.private_json(self.directory / "build-failure.json")["preservation"]["unchanged"])


    def test_prepare_only_copies_byte_identical_assets_and_modifies_private_pubspec(self):
        target = self.directory / "new-private-run"
        source = self.directory / "fixture-source"
        base.write_new(source, b"unchanged-fixture-stream")
        rows = [{**pin, "sourcePath": str(source), "bytes": source.stat().st_size,
            "sha256": base.file_hash(source)} for pin in self.cases]
        original = b"name: qa\nflutter:\n  assets:\n    - assets/library/\n"
        sources = {"app/mobile/" + emulator.ENTRY: "e" * 64}
        def snapshot(output, selected):
            mobile = output / "source/repository/app/mobile"
            (mobile / "assets").mkdir(parents=True)
            (mobile / "test_driver").mkdir()
            base.write_new(mobile / emulator.ENTRY, b"entry")
            base.write_new(mobile / emulator.DRIVER, b"driver")
            base.write_new(mobile / "pubspec.yaml", original)
            base.write_new(mobile / "pubspec.lock", b"lock-bytes")
            return mobile
        with patch.object(base, "fixtures", return_value=({}, rows)), \
            patch.object(emulator, "local_sources", return_value=(sources, "s" * 64)), \
            patch.object(emulator, "protection", return_value={"files": {}, "trees": {}}), \
            patch.object(emulator, "private_mobile_snapshot", side_effect=snapshot):
            receipt = emulator.prepare(target)
        mobile = Path(receipt["sourceCopyPath"])
        self.assertEqual((mobile / "pubspec.lock").read_bytes(), b"lock-bytes")
        self.assertEqual(source.read_bytes(), b"unchanged-fixture-stream")
        self.assertIn("    - assets/local_lossless/\n", (mobile / "pubspec.yaml").read_text())
        for row in rows:
            self.assertEqual((mobile / row["asset"]).read_bytes(), source.read_bytes())
        self.assertRegex(receipt["runNonce"], r"^[a-f0-9]{64}$")
        self.assertFalse((target / "signing.xcconfig").exists())

    def test_sdk_retry_log_is_never_accepted_as_single_launch_evidence(self):
        prepared, built, rows, launch, result, path = self.launch_fixture()
        trace = self.directory / "build-output/flutter-drive.stderr"
        trace.write_bytes(b"Application failed to start on attempt: 1\n")
        with self.fixture_patches(prepared, built, rows), self.assertRaisesRegex(EntryError, "sdk_launch_retry_rejected"):
            emulator.validate_launch(prepared, built, launch, result, path, self.directory)

    def test_tool_trace_changes_are_rejected_even_when_apk_is_unchanged(self):
        prepared, built, rows = self.fixture()
        (self.directory / "build-output/gradle-profile.stdout").write_bytes(b"changed trace")
        with self.fixture_patches(prepared, built, rows), self.assertRaisesRegex(EntryError, "tool_receipt_rejected"):
            emulator.validate_build(prepared, built, self.directory)


    def test_protection_ignores_unrelated_live_cache_but_keeps_old_evidence_and_build_absence(self):
        root = self.directory / "protection-repo"
        mobile = root / "app/mobile"
        public = root / "be/docs/evidence/local-lossless-mobile"
        evidence_path = root / "be/docs/evidence/local-lossless-formats/verification.json"
        for name in ("be/docs/evidence/png-lossless-compression", "be/docs/evidence/p4-reading-images",
            "be/docs/evidence/local-lossless-formats", "be/docs/evidence/local-lossless-mobile",
            "be/var/exact-old-run", "be/var/live-backend", "_bmad-output/implementation-artifacts/image-optimization",
            "app/mobile/.dart_tool", "app/mobile/android"):
            (root / name).mkdir(parents=True, exist_ok=True)
        native = mobile / "android/guard"
        names = ("_bmad-output/party-mode/memories/installed/.memlog.md",
            "_bmad-output/specs/spec-publish-time-image-optimization/.memlog.md",
            "_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/.memlog.md")
        for path in (evidence_path, evidence_path.with_suffix(".md"), mobile / "pubspec.yaml", mobile / "pubspec.lock",
            native, *(root / name for name in names)):
            path.parent.mkdir(parents=True, exist_ok=True)
            base.write_new(path, b"preserved")
        live = root / "be/var/live-backend/current.log"
        cache = mobile / ".dart_tool/cache"
        base.write_new(live, b"live")
        base.write_new(cache, b"cache")
        evidence = {"preservation": {"afterSha256": {}}, "privateRun": "be/var/exact-old-run"}
        with patch.object(emulator, "ROOT", root), patch.object(emulator, "MOBILE", mobile), \
            patch.object(emulator, "PUBLIC", public), patch.object(base, "EVIDENCE", evidence_path), \
            patch.object(base, "PREFLIGHT", root / "absent-preflight"), \
            patch.object(emulator, "selected_mobile_sources", return_value=[native]):
            protected = emulator.protection(evidence)
        self.assertNotIn(str(root / "be/var"), protected["trees"])
        self.assertNotIn(str(mobile / ".dart_tool"), protected["trees"])
        live.write_bytes(b"backend-keeps-working")
        cache.write_bytes(b"ide-keeps-working")
        with patch.object(emulator, "PUBLIC", public):
            self.assertTrue(emulator.assert_protected(protected)["unchanged"])
        (mobile / "build").mkdir()
        with patch.object(emulator, "PUBLIC", public), self.assertRaisesRegex(EntryError, "preservation_changed"):
            emulator.assert_protected(protected)

    def test_cli_limits_public_driver_and_preservation_arguments_to_their_stages(self):
        for mode, arguments in (("--prepare", ["--public-dir", str(self.target)]),
            ("--build", ["--driver-result", "/private/tmp/evidence.json"]),
            ("--run", ["--preserve-private-dir", str(self.directory)]),
            ("--verify", ["--driver-result", "/private/tmp/evidence.json", "--not-run-reason", "DRIVER_NOT_RUN"])):
            with patch.object(emulator, "prepare") as prepare, patch.object(emulator, "build") as build, \
                patch.object(emulator, "run") as run, patch.object(emulator, "verify") as verify:
                self.assertEqual(emulator.main([mode, "--private-dir", str(self.directory), *arguments]), 2)
                for operation in (prepare, build, run, verify):
                    operation.assert_not_called()

    def test_missing_or_wrong_runtime_binding_is_rejected_before_build_and_driver(self):
        prepared, built, rows = self.fixture()
        with self.fixture_patches(prepared, built, rows):
            for key in (None, *base.RUNTIME_ENV_KEYS):
                value = deepcopy(prepared)
                if key is None:
                    value.pop("runtimeEnvironment")
                else:
                    value["runtimeEnvironment"][key] = "/wrong-or-internal-path"
                with self.subTest(key=key), self.assertRaisesRegex(EntryError, "runtime_environment_changed"):
                    emulator.validate_snapshot(value, self.directory)

    def test_tool_environment_digest_is_bound_to_build_and_launch_receipts(self):
        prepared, built, rows = self.fixture()
        with self.fixture_patches(prepared, built, rows):
            with self.assertRaisesRegex(EntryError, "build_binding_rejected"):
                emulator.validate_build(prepared, {**built, "runtimeEnvironmentSha256": "0" * 64}, self.directory)
            receipt = self.directory / "build-output/gradle-profile.json"
            value = json.loads(receipt.read_bytes())
            value["runtimeEnvironmentSha256"] = "0" * 64
            receipt.write_text(json.dumps(value))
            with self.assertRaisesRegex(EntryError, "runtime_environment_receipt_rejected"):
                emulator.validate_build(prepared, built, self.directory)

    def test_java_25_is_rejected_before_sdk_or_gradle_execution(self):
        java = self.directory / "jdk-25"
        (java / "bin").mkdir(parents=True)
        base.write_new(java / "bin/java", b"unused-test-binary")
        base.write_new(java / "release", b'JAVA_VERSION="25.0.2"\n')
        with patch.dict(os.environ, {"JAVA_HOME": str(java), "ANDROID_HOME": str(self.directory)}), \
            patch.object(emulator.shutil, "which", return_value="/usr/bin/true"), \
            self.assertRaisesRegex(EntryError, "java_runtime_incompatible"):
            emulator.tools()

    def test_old_private_signing_output_accepts_system_tmp_outside_private_tmp(self):
        temporary_root = Path(os.environ.get("TMPDIR", tempfile.gettempdir())).resolve()
        with tempfile.TemporaryDirectory(dir=temporary_root) as name:
            old = Path(name).resolve()
            os.chmod(old, 0o700)
            self.assertFalse(old.is_relative_to(Path("/private/tmp")))
            self.assertEqual(emulator.preserved_private_directory(old), old)
            link = self.directory / "old-signing-link"
            link.symlink_to(old, target_is_directory=True)
            with self.assertRaisesRegex(EntryError, "preserved_private_directory_rejected"):
                emulator.preserved_private_directory(link)
            os.chmod(old, 0o755)
            with self.assertRaisesRegex(EntryError, "preserved_private_directory_rejected"):
                emulator.preserved_private_directory(old)
            os.chmod(old, 0o700)

    def test_public_append_keeps_old_files_and_allows_only_the_authorized_new_attempt(self):
        old = self.public / "verification.json"
        base.write_new(old, b"old-canonical-report")
        baseline = {"files": {}, "trees": {}, "publicTree": {
            "path": str(self.public), "files": base.tree_hashes(self.public)}}
        report = {"status": "NOT_RUN", "reason": "DRIVER_NOT_RUN", "cases": []}
        with patch.object(emulator, "PUBLIC", self.public):
            emulator.publish(report, [], None, self.target, protected=baseline)
            self.assertTrue(emulator.assert_protected(baseline, authorized_public=self.target)["unchanged"])
            with self.assertRaisesRegex(EntryError, "preservation_changed"):
                emulator.assert_protected(baseline)
            old.write_bytes(b"changed-old-report")
            with self.assertRaisesRegex(EntryError, "preservation_changed"):
                emulator.assert_protected(baseline, authorized_public=self.target)
            old.write_bytes(b"old-canonical-report")
            old.unlink()
            with self.assertRaisesRegex(EntryError, "preservation_changed"):
                emulator.assert_protected(baseline, authorized_public=self.target)
            base.write_new(old, b"old-canonical-report")
            other = self.public / "attempts/not-authorized"
            other.mkdir()
            base.write_new(other / "verification.json", b"unapproved-addition")
            with self.assertRaisesRegex(EntryError, "preservation_changed"):
                emulator.assert_protected(baseline, authorized_public=self.target)

    def test_verify_cli_publishes_prepare_failure_without_a_prepare_receipt(self):
        base.write_new(self.directory / "prepare-failure.json", {
            "status": "FAIL", "reason": "VERIFICATION_REJECTED", "failureType": "private-token"})
        with patch.object(emulator, "PUBLIC", self.public):
            result = emulator.main(["--verify", "--private-dir", str(self.directory),
                "--public-dir", str(self.target)])
        self.assertEqual(result, 2)
        self.assertFalse((self.directory / "prepare-receipt.json").exists())
        report = json.loads((self.target / "verification.json").read_bytes())
        self.assertEqual(report["status"], "FAIL")
        self.assertEqual(report["failureStage"], "prepare")
        self.assertEqual(report["acceptedCaseCount"], 0)
        self.assertEqual(report["build"]["status"], "NOT_RUN")
        self.assertEqual(report["launch"]["status"], "NOT_RUN")
        self.assertNotIn("private-token", json.dumps(report))


if __name__ == "__main__":
    unittest.main()

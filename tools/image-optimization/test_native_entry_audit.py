"""Token-free audit, installed tamper refusal and destination-only launcher tests."""
from contextlib import ExitStack, redirect_stdout
from hashlib import sha256
from io import StringIO
import base64
import json
import os
from pathlib import Path
import plistlib
import sys
import time
import unittest
from unittest.mock import Mock, patch

import native_entry_audit as audit
from native_entry_config import EntryError, SCOPE, frozen_json
import native_entry_destination as destination
import native_entry_driver as driver
from test_native_entry_driver import SourceBindingInputs


class AuditInputs(SourceBindingInputs):
    def setUp(self):
        super().setUp()
        self.build_root = self.base / "signed-build"
        self.build_root.mkdir(mode=0o700)
        sources, digest = driver.source_snapshot()
        copy = driver.private_mobile_snapshot(self.build_root, sources)
        self.products = self.build_root / "derived-data/Build/Products"
        self.product = self.products / "Release-imageexperiment-iphoneos"
        self.app = self.product / "ImageExperiment.app"
        self.runner = self.product / "ImageExperimentUITests-Runner.app"
        self.plugin = self.runner / "PlugIns/ImageExperimentUITests.xctest"
        self.app.mkdir(parents=True)
        self.plugin.mkdir(parents=True)
        self.app_id = "com.tellpal.tellpalMobilePoc.imageexperiment.release"
        for path, identifier in ((self.app, self.app_id), (self.runner, self.app_id + ".uitests.xctrunner")):
            (path / "Info.plist").write_bytes(plistlib.dumps({"CFBundleIdentifier": identifier}))
            (path / ".hidden").write_bytes(b"hidden installed input")
            (path / "_CodeSignature").mkdir()
            (path / "_CodeSignature/CodeResources").write_bytes(b"actual signature input")
        (self.plugin / "ImageExperimentUITests").write_bytes(b"actual test executable")
        self.run_path = self.products / "imageexperiment_iphoneos.xctestrun"
        self.target = {"BlueprintName": "ImageExperimentUITests", "IsUITestBundle": True,
            "TestHostPath": "__TESTROOT__/Release-imageexperiment-iphoneos/ImageExperimentUITests-Runner.app",
            "UITargetAppPath": "__TESTROOT__/Release-imageexperiment-iphoneos/ImageExperiment.app",
            "TestBundlePath": "__TESTHOST__/PlugIns/ImageExperimentUITests.xctest",
            "EnvironmentVariables": {"DYLD_INSERT_LIBRARIES": "__PLATFORMS__/iPhoneOS.platform/lib/libMainThreadChecker.dylib"},
            "TestingEnvironmentVariables": {"XCODE_SCHEME_NAME": "imageexperiment"},
            "UITargetAppEnvironmentVariables": {"XCODE_SCHEME_NAME": "imageexperiment"},
            "OnlyTestIdentifiers": ["unrelated/test"], "DependentProductPaths": [str(self.app)]}
        self.original = {"ImageExperimentUITests": self.target, "__xctestrun_metadata__": {"FormatVersion": 1}}
        self.build = {"schemaVersion": 1, "scope": SCOPE, "platform": "ios", "mode": "release",
            "expectedAppId": self.app_id, "testAppId": self.app_id + ".uitests.xctrunner",
            "nativeTarget": "ImageExperiment", "dartTarget": "lib/main_image_experiment.dart",
            "sourceFiles": sources, "sourceSha256": digest, "sourceCopyPath": str(copy),
            "copiedSourceSha256": driver.validate_copied_sources(copy, sources),
            "lockSha256": sha256((Path(audit.__file__).parent / "uv.lock").read_bytes()).hexdigest(),
            "artifactPath": str(self.app), "testArtifactPath": str(self.runner),
            "artifactSha256": driver.artifact_hash(self.app), "testArtifactSha256": driver.artifact_hash(self.runner),
            "certificateSha256": sha256(b"synthetic certificate").hexdigest(), "xctestrunPath": str(self.run_path)}
        self.save_run()
        self.build_path = self.file("build-receipt.json", frozen_json(self.build))
        self.request = {"schemaVersion": 1, "scope": audit.AUDIT_SCOPE, "expectedAppId": self.app_id,
            "nonce": "a" * 64, "deadline": int(time.time() * 1000) + 30000}

    def save_run(self):
        self.run_path.write_bytes(plistlib.dumps(self.original))
        os.chmod(self.run_path, 0o600)
        self.build["xctestrunSha256"] = sha256(self.run_path.read_bytes()).hexdigest()

    def report(self, request=None):
        app, runner = driver.artifact_observation(self.app), driver.artifact_observation(self.runner)
        return {"schemaVersion": 1, "scope": audit.AUDIT_SCOPE, "nonce": (request or self.request)["nonce"],
            "appId": self.app_id, "runnerId": self.build["testAppId"], "appSha256": app["sha256"],
            "runnerSha256": runner["sha256"], "appFileCount": app["fileCount"], "appByteCount": app["byteCount"],
            "runnerFileCount": runner["fileCount"], "runnerByteCount": runner["byteCount"]}

    def emit(self, observer, report):
        observer(audit.REPORT_PREFIX + base64.b64encode(frozen_json(report)))


class DestinationTests(AuditInputs):
    def test_actual_generated_container_info_and_older_minimal_metadata_are_bounded(self):
        for index, metadata in enumerate(({"FormatVersion": 1}, {"FormatVersion": 1,
            "ContainerInfo": {"ContainerName": "Runner", "SchemeName": "imageexperiment"}})):
            self.original["__xctestrun_metadata__"] = metadata
            self.save_run()
            before = self.run_path.read_bytes()
            output = self.base / ("metadata-shape-" + str(index))
            output.mkdir(mode=0o700)
            run = destination.destination_run(self.build, output, "public-only", audit=True)
            self.assertEqual(plistlib.loads(run.read_bytes())["__xctestrun_metadata__"], metadata)
            self.assertEqual(self.run_path.read_bytes(), before)

    def test_audit_and_fixture_runs_preserve_original_and_never_contain_install_paths(self):
        original = self.run_path.read_bytes()
        for is_audit in (True, False):
            output = self.base / ("audit-output" if is_audit else "fixture-output")
            output.mkdir(mode=0o700)
            run = destination.destination_run(self.build, output, "public-only", audit=is_audit)
            target = plistlib.loads(run.read_bytes())["ImageExperimentUITests"]
            self.assertIs(target["UseDestinationArtifacts"], True)
            self.assertEqual(target["TestHostBundleIdentifier"], self.build["testAppId"])
            self.assertEqual(target["UITargetAppBundleIdentifier"], self.app_id)
            self.assertEqual(target["TestBundleDestinationRelativePath"], destination.TEST_BUNDLE)
            self.assertEqual(target["OnlyTestIdentifiers"], [destination.AUDIT_TEST if is_audit else destination.FIXTURE_TEST])
            for key in ("TestHostPath", "TestBundlePath", "UITargetAppPath", "DependentProductPaths"):
                self.assertNotIn(key, target)
            self.assertEqual(target["TestingEnvironmentVariables"], self.target["TestingEnvironmentVariables"])
            self.assertEqual(target["EnvironmentVariables"]["DYLD_INSERT_LIBRARIES"], self.target["EnvironmentVariables"]["DYLD_INSERT_LIBRARIES"])
            self.assertEqual(run.stat().st_mode & 0o777, 0o600)
        self.assertEqual(self.run_path.read_bytes(), original)

    def test_broker_metadata_extra_targets_paths_missing_env_and_unknown_env_refuse(self):
        cases = (
            lambda: self.target["EnvironmentVariables"].update(IMAGE_EXPERIMENT_METADATA="broker"),
            lambda: self.original.update(RunnerTests={}),
            lambda: self.target.update(TestHostPath=str(self.app)),
            lambda: self.target.update(TestingEnvironmentVariables={}),
            lambda: self.target["TestingEnvironmentVariables"].update(GOOGLE_APPLICATION_CREDENTIALS="private"),
            lambda: self.original["__xctestrun_metadata__"].update(FormatVersion=True),
            lambda: self.original["__xctestrun_metadata__"].update(Unknown=True),
            lambda: self.original["__xctestrun_metadata__"].update(ContainerInfo={"ContainerName": "Runner", "SchemeName": "ordinary"}),
            lambda: self.original["__xctestrun_metadata__"].update(ContainerInfo={"ContainerName": "Other", "SchemeName": "imageexperiment"}),
            lambda: self.original["__xctestrun_metadata__"].update(ContainerInfo={"ContainerName": "Runner", "SchemeName": "imageexperiment", "Unknown": True}),
        )
        original = plistlib.dumps(self.original)
        for index, mutation in enumerate(cases):
            self.original = plistlib.loads(original)
            self.target = self.original["ImageExperimentUITests"]
            mutation()
            self.save_run()
            output = self.base / ("unsafe-run-" + str(index))
            output.mkdir(mode=0o700)
            with self.subTest(index=index), self.assertRaises(EntryError):
                destination.destination_run(self.build, output, "public-only", audit=True)
            self.assertEqual(list(output.iterdir()), [])


class ReportTests(AuditInputs):
    def run_report_process(self, raw, alias, observer, *, fragmented=False, exit_code=0, checked=False, hold=False):
        output = self.base / alias
        output.mkdir(mode=0o700)
        command = [sys.executable, "-c",
            "import os,sys,time;os.write(1,bytes.fromhex(sys.argv[1]));"
            "time.sleep(30) if sys.argv[3]=='hold' else None;raise SystemExit(int(sys.argv[2]))",
            raw.hex(), str(exit_code), "hold" if hold else "exit"]
        with ExitStack() as stack:
            if fragmented:
                real_read = os.read
                stack.enter_context(patch.object(driver.os, "read",
                    side_effect=lambda descriptor, capacity: real_read(descriptor, min(capacity, 1))))
            try:
                if checked:
                    return driver.checked_run(command, output, "report", deadline=time.monotonic() + 3, on_line=observer)
                return driver.run_bounded(command, output, "report", seconds=3, on_line=observer)
            finally:
                # Even refusal closes the owned handles without altering original stdout.
                self.assertEqual((output / "report.stdout").read_bytes(), raw)
                self.assertEqual((output / "report.stderr").read_bytes(), b"")
                for path in output.iterdir():
                    self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    def test_real_lf_crlf_and_fragmented_report_lines_accept_only_exact_fresh_payloads(self):
        expected = (driver.artifact_observation(self.app), driver.artifact_observation(self.runner))
        report = self.report()
        line = audit.REPORT_PREFIX + base64.b64encode(frozen_json(report))
        for index, (ending, fragmented) in enumerate(((b"\n", False), (b"\r\n", False),
            (b"\n", True), (b"\r\n", True))):
            with self.subTest(ending=ending, fragmented=fragmented):
                observer = audit.InstallationReport(self.request, self.build)
                raw = line + ending
                result = self.run_report_process(raw, "report-framing-" + str(index), observer, fragmented=fragmented)
                self.assertEqual((result["status"], result["exitCode"]), ("ok", 0))
                self.assertEqual(result["traceBytes"], len(raw))
                self.assertEqual(result["traceSha256"], [sha256(raw).hexdigest(), sha256(b"").hexdigest()])
                self.assertEqual(observer.verified(*expected), report)

    def test_real_report_framing_keeps_malformed_cr_whitespace_and_base64_noncanonical(self):
        line = audit.REPORT_PREFIX + base64.b64encode(frozen_json(self.report()))
        malformed = [line + b"\r\r\n", line[:50] + b"\r" + line[50:] + b"\n",
            line + b" \r\n", line + b"\t\r\n", line + b"=\r\n"]
        for index, raw in enumerate(malformed):
            observer = audit.InstallationReport(self.request, self.build)
            with self.subTest(index=index), self.assertRaisesRegex(EntryError, "audit_report_rejected"):
                # Keep a live owned child so refusal exercises real process-group cleanup.
                self.run_report_process(raw, "malformed-report-" + str(index), observer, fragmented=True, hold=True)
            self.assertIsNone(observer.report)
        observer = audit.InstallationReport(self.request, self.build)
        self.run_report_process(line + b"\r", "unterminated-report", observer, fragmented=True)
        self.assertIsNone(observer.report)
        with self.assertRaisesRegex(EntryError, "audit_report_unavailable"):
            observer.verified(driver.artifact_observation(self.app), driver.artifact_observation(self.runner))

    def test_real_crlf_report_preserves_nonce_schema_duplicate_and_installed_tamper_refusals(self):
        for index, (field, value) in enumerate((("nonce", "b" * 64), ("schemaVersion", True))):
            report = self.report() | {field: value}
            observer = audit.InstallationReport(self.request, self.build)
            raw = audit.REPORT_PREFIX + base64.b64encode(frozen_json(report)) + b"\r\n"
            with self.subTest(field=field), self.assertRaises(EntryError):
                self.run_report_process(raw, "binding-report-" + str(index), observer, hold=True)
            self.assertIsNone(observer.report)
        line = audit.REPORT_PREFIX + base64.b64encode(frozen_json(self.report())) + b"\r\n"
        observer = audit.InstallationReport(self.request, self.build)
        with self.assertRaisesRegex(EntryError, "audit_report_duplicate"):
            self.run_report_process(line + line, "duplicate-report", observer, hold=True)
        observer = audit.InstallationReport(self.request, self.build)
        tampered = self.report() | {"runnerSha256": "0" * 64}
        self.run_report_process(audit.REPORT_PREFIX + base64.b64encode(frozen_json(tampered)) + b"\r\n",
            "tampered-report", observer)
        with self.assertRaisesRegex(EntryError, "audit_installed_bytes_changed"):
            observer.verified(driver.artifact_observation(self.app), driver.artifact_observation(self.runner))

    def test_real_crlf_report_cannot_qualify_failed_xctest_exit(self):
        observer = audit.InstallationReport(self.request, self.build)
        raw = audit.REPORT_PREFIX + base64.b64encode(frozen_json(self.report())) + b"\r\n"
        with self.assertRaisesRegex(EntryError, "tool_failed"):
            self.run_report_process(raw, "failed-xctest", observer, exit_code=1, checked=True)
        self.assertIsNotNone(observer.report)
        result = json.loads((self.base / "failed-xctest/report.json").read_text())
        self.assertEqual((result["status"], result["exitCode"]), ("failed", 1))
        self.assertEqual(result["traceSha256"][0], sha256(raw).hexdigest())

    def test_single_fresh_exact_report_requires_matching_independently_observed_counts(self):
        observer = audit.InstallationReport(self.request, self.build)
        self.emit(observer, self.report())
        self.assertEqual(observer.verified(driver.artifact_observation(self.app), driver.artifact_observation(self.runner)), self.report())
        with self.assertRaisesRegex(EntryError, "audit_report_duplicate"):
            self.emit(observer, self.report())

    def test_replayed_wrong_ids_boolean_counts_unknown_duplicate_malformed_and_expired_refuse(self):
        for key, value in (("nonce", "b" * 64), ("appId", "ordinary.app"), ("runnerId", "ordinary.runner"),
            ("scope", SCOPE), ("appFileCount", True), ("runnerByteCount", 512 * 1024 * 1024 + 1), ("schemaVersion", True), ("secret", "private")):
            report = self.report()
            report[key] = value
            with self.subTest(key=key), self.assertRaises(EntryError):
                self.emit(audit.InstallationReport(self.request, self.build), report)
        for line in (audit.REPORT_PREFIX + b"!bad", audit.REPORT_PREFIX + base64.b64encode(b'{"nonce":"a","nonce":"b"}'),
            b"IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED", b"x" * 8193):
            with self.subTest(line=line[:60]), self.assertRaises(EntryError):
                audit.InstallationReport(self.request, self.build)(line)
        self.request["deadline"] = int(time.time() * 1000) - 1
        with self.assertRaisesRegex(EntryError, "audit_deadline_exceeded"):
            self.emit(audit.InstallationReport(self.request, self.build), self.report())

    def test_actual_installed_digest_and_counter_mismatch_remain_failure(self):
        expected = (driver.artifact_observation(self.app), driver.artifact_observation(self.runner))
        for key, value in (("appSha256", "0" * 64), ("runnerSha256", "0" * 64), ("appFileCount", 1), ("runnerByteCount", 0)):
            observer = audit.InstallationReport(self.request, self.build)
            report = self.report()
            report[key] = value
            self.emit(observer, report)
            with self.subTest(key=key), self.assertRaisesRegex(EntryError, "audit_installed_bytes_changed"):
                observer.verified(*expected)


class AuditCliTests(AuditInputs):
    def run_cli(self, outcome="match"):
        self.build_path.write_bytes(frozen_json(self.build))
        self.commands = []
        def synthetic_run(command, output, alias, *, deadline, **kwargs):
            self.commands.append(command)
            if command[0] == "codesign":
                if "--display" in command:
                    prefix = next(arg.split("=", 1)[1] for arg in command if arg.startswith("--extract-certificates="))
                    path = Path(prefix + "0")
                    path.write_bytes(b"wrong certificate" if outcome == "signing_changed" else b"synthetic certificate")
                    os.chmod(path, 0o600)
                return b""
            self.assertEqual(command[0], "xcodebuild")
            self.assertNotIn("-workspace", command)
            self.assertNotIn("-xcconfig", command)
            self.assertEqual(command[-1], "-only-testing:ImageExperimentUITests/" + destination.AUDIT_TEST)
            run = plistlib.loads(Path(command[command.index("-xctestrun") + 1]).read_bytes())
            encoded = run["ImageExperimentUITests"]["EnvironmentVariables"][destination.AUDIT_ENV]
            request = json.loads(base64.b64decode(encoded))
            self.assertEqual(set(request), {"schemaVersion", "scope", "expectedAppId", "nonce", "deadline"})
            self.assertLessEqual(request["deadline"] - int(time.time() * 1000), 30000)
            report = self.report(request)
            if outcome == "tamper":
                report["runnerSha256"] = "0" * 64
            if outcome == "tool_failure":
                raise EntryError("tool_failed")
            self.emit(kwargs["on_line"], report)
            if outcome == "duplicate":
                self.emit(kwargs["on_line"], report)
            if outcome == "host_changed":
                (self.app / ".hidden").write_bytes(b"changed after launch")
            return b""
        stream = StringIO()
        output = self.base / ("audit-cli-" + outcome)
        with ExitStack() as stack:
            guards = [stack.enter_context(patch.object(driver, name)) for name in
                ("load_config", "load_manifest", "canonical_uid", "FirebaseVerifier", "Broker", "create_server", "build_entry")]
            stack.enter_context(patch.object(driver, "checked_run", side_effect=synthetic_run))
            stack.enter_context(redirect_stdout(stream))
            result = audit.main(["--build-receipt", str(self.build_path), "--device-selector", "synthetic-device",
                "--private-output", str(output)])
            for guard in guards:
                guard.assert_not_called()
        self.assertNotIn(str(self.base), stream.getvalue())
        self.assertNotIn("synthetic-device", stream.getvalue())
        return result, output

    def test_success_rebinds_signing_source_copy_and_actual_bytes_without_auth_inputs(self):
        status, output = self.run_cli()
        self.assertEqual(status, 0)
        receipt = json.loads((output / "installation-receipt.json").read_bytes())
        self.assertEqual(receipt["installedArtifactSha256"], self.build["artifactSha256"])
        self.assertEqual(receipt["installedTestArtifactSha256"], self.build["testArtifactSha256"])
        self.assertEqual(receipt["certificateBinding"], "exact_bundle_equivalence")
        self.assertEqual(receipt["authentication"], "not_run")
        self.assertNotIn(str(self.base), json.dumps(receipt))
        for path in output.iterdir():
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)

    def test_actual_tamper_duplicate_tool_failure_or_changed_host_never_writes_positive(self):
        original = (self.app / ".hidden").read_bytes()
        for outcome in ("tamper", "duplicate", "tool_failure", "host_changed", "signing_changed"):
            status, output = self.run_cli(outcome)
            self.assertEqual(status, 4)
            self.assertFalse((output / "installation-receipt.json").exists())
            self.assertTrue((output / "failure-receipt.json").exists())
            (self.app / ".hidden").write_bytes(original)

    def test_source_copy_artifact_identity_and_lock_mutations_refuse_before_tools(self):
        for field, value in (("sourceSha256", "0" * 64), ("copiedSourceSha256", "0" * 64),
            ("artifactSha256", "0" * 64), ("lockSha256", "0" * 64), ("expectedAppId", "ordinary.app")):
            original = self.build[field]
            self.build[field] = value
            with self.subTest(field=field), patch.object(driver, "checked_run") as tool:
                with self.assertRaises(EntryError):
                    audit.validate_build(self.build)
                tool.assert_not_called()
            self.build[field] = original

    def test_authenticated_apple_launcher_uses_only_prevalidated_destination_artifacts(self):
        self.values.update(platform="ios", expectedAppId=self.app_id)
        config, manifest = self.configuration(), self.admitted_manifest()
        output = self.base / "authenticated-launcher"
        output.mkdir(mode=0o700)
        broker, server = Mock(), Mock()
        broker.metadata.return_value = {"schemaVersion": 1, "caseId": "case_a"}
        calls = []
        def launch(command, output, alias, **kwargs):
            calls.append(command)
            if alias == "device-state":
                return b"connected selected synthetic physical device"
            self.assertEqual(alias, "launcher")
            self.assertEqual(command[-1], "-only-testing:ImageExperimentUITests/" + destination.FIXTURE_TEST)
            self.assertNotIn("-workspace", command)
            self.assertNotIn("-xcconfig", command)
            self.assertFalse(any("FLUTTER_APPLICATION_PATH=" in item for item in command))
            run = plistlib.loads(Path(command[command.index("-xctestrun") + 1]).read_bytes())
            target = run["ImageExperimentUITests"]
            self.assertIs(target["UseDestinationArtifacts"], True)
            self.assertEqual(target["TestHostBundleIdentifier"], self.build["testAppId"])
            self.assertEqual(target["UITargetAppBundleIdentifier"], self.build["expectedAppId"])
            self.assertIn(destination.FIXTURE_ENV, target["EnvironmentVariables"])
            self.assertNotIn(destination.FIXTURE_ENV, kwargs["env"])
            self.assertNotIn("TEST_RUNNER_IMAGE_EXPERIMENT_METADATA", kwargs["env"])
            return b""
        with patch.object(driver, "validate_physical_prerequisites", return_value=(self.build, {})), \
            patch.object(driver, "validate_backend_startup", return_value="0" * 64), \
            patch.object(driver, "FirebaseVerifier"), patch.object(driver, "Broker", return_value=broker), \
            patch.object(driver, "create_server", return_value=server), patch.object(driver, "LauncherPairing"), \
            patch.object(driver, "validate_entry_outcome", return_value={}), patch.object(driver, "checked_run", side_effect=launch):
            driver.verify_entry(config, manifest, output, time.monotonic() + 10, case_id="case_a",
                build_path=self.build_path, prerequisite_path=self.base / "synthetic-proof.json")
        self.assertEqual(len(calls), 2)


class ArtifactAlgorithmTests(AuditInputs):
    def test_unreadable_subtree_never_yields_an_incomplete_positive_hash(self):
        root = self.base / "unreadable-tree.app"
        subtree = root / "sealed"
        subtree.mkdir(parents=True)
        (subtree / "unreadable-input").write_bytes(b"must never be silently excluded")
        self.addCleanup(os.chmod, subtree, 0o700)
        os.chmod(subtree, 0)
        with self.assertRaisesRegex(EntryError, "artifact_unavailable"):
            driver.artifact_observation(root)

    def test_early_file_mutation_replacement_or_symlink_during_later_read_refuses(self):
        original_read = os.read
        for kind in ("in_place", "replacement", "symlink"):
            root = self.base / ("whole-scan-" + kind + ".app")
            root.mkdir()
            early, later = root / "a-early", root / "z-later"
            early.write_bytes(b"before")
            later.write_bytes(b"later bytes")
            later_inode = later.stat().st_ino
            parent_before = root.stat()
            mutated = False
            def mutate_during_later_read(descriptor, size):
                nonlocal mutated
                if not mutated and os.fstat(descriptor).st_ino == later_inode:
                    mutated = True
                    if kind == "in_place":
                        before = early.stat()
                        early.write_bytes(b"after!")
                        os.utime(early, ns=(before.st_atime_ns, before.st_mtime_ns + 1_000_000_000))
                    else:
                        early.unlink()
                        if kind == "replacement":
                            early.write_bytes(b"before")
                        else:
                            early.symlink_to(later)
                return original_read(descriptor, size)
            with self.subTest(kind=kind), patch.object(driver.os, "read", side_effect=mutate_during_later_read):
                with self.assertRaises(EntryError):
                    driver.artifact_observation(root)
            self.assertTrue(mutated)
            if kind == "in_place":
                self.assertEqual(root.stat().st_mtime_ns, parent_before.st_mtime_ns)
                self.assertEqual(root.stat().st_ctime_ns, parent_before.st_ctime_ns)

    def test_full_relative_utf8_order_includes_hidden_signature_profile_and_dsym(self):
        root = self.base / "tree-order.app"
        root.mkdir()
        data = {"a/file": b"nested", "a-early": b"early", ".hidden": b"hidden",
            "_CodeSignature/CodeResources": b"signature", "embedded.mobileprovision": b"profile", "Symbols.dSYM/bytes": b"symbols"}
        for name, content in data.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        digest = sha256()
        for name in sorted(data, key=lambda value: value.encode("utf-8")):
            encoded = name.encode("utf-8")
            digest.update(len(encoded).to_bytes(4, "big") + encoded + sha256(data[name]).digest())
        self.assertEqual(driver.artifact_observation(root), {"sha256": digest.hexdigest(),
            "fileCount": len(data), "byteCount": sum(map(len, data.values()))})
        (root / ".hidden").write_bytes(b"changed")
        self.assertNotEqual(driver.artifact_hash(root), digest.hexdigest())

    def test_symlink_special_file_oversize_and_deadline_refuse(self):
        for kind in ("symlink", "fifo", "oversize"):
            root = self.base / (kind + ".app")
            root.mkdir()
            item = root / "input"
            if kind == "symlink":
                item.symlink_to(self.app / ".hidden")
            elif kind == "fifo":
                os.mkfifo(item)
            else:
                with item.open("wb") as handle:
                    handle.truncate(256 * 1024 * 1024 + 1)
            with self.subTest(kind=kind), self.assertRaises(EntryError):
                driver.artifact_observation(root)
        with self.assertRaises(EntryError):
            driver.artifact_observation(self.app, deadline=time.monotonic() - 1)


if __name__ == "__main__":
    unittest.main()

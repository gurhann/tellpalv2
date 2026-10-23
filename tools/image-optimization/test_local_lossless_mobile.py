"""Altı fixture, özel build ve public kanıt sınırlarının testleri."""
from copy import deepcopy
from hashlib import sha256
import json
import os
from pathlib import Path
import stat
import sys
import tempfile
import unittest
from unittest.mock import patch

import local_lossless_mobile as mobile
from native_entry_config import EntryError


class LocalLosslessMobileTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(dir=Path(tempfile.gettempdir()).resolve())
        self.directory = Path(self.temporary.name).resolve()
        os.chmod(self.directory, 0o700)
        self.external_root_patch = patch.object(mobile, "EXTERNAL_QA_ROOT", self.directory)
        self.external_root_patch.start()
        self.actual_home = Path.home().resolve()
        fake_home = self.directory / "fixture-home"
        fake_home.mkdir(mode=0o700)
        for name in (".gradle", ".pub-cache"):
            (fake_home / name).mkdir(mode=0o700)
        self.home_patch = patch.object(mobile.Path, "home", return_value=fake_home)
        self.home_patch.start()
        self.cases = []
        self.nonce = "a1" * 32
        self.results = []
        self.screenshots = []
        for index, case_id in enumerate(mobile.CASE_IDS):
            asset, kind = case_id.split("-")
            pin = {"caseId": case_id, "assetId": int(asset), "asset": f"assets/local_lossless/{asset}.{kind}",
                "format": kind.upper(), "bytes": 100 + index, "sha256": str(index) * 64,
                "width": 822, "height": 1334, "rgbBytes": 3289644, "rgbSha256": "a" * 64}
            self.cases.append(pin)
            row = {"caseId": case_id, "assetId": int(asset), "asset": pin["asset"], "format": kind.upper(),
                "encodedBytes": pin["bytes"], "encodedSha256": pin["sha256"], "decodedWidth": 822,
                "decodedHeight": 1334, "rgbBytes": 3289644, "rgbSha256": "a" * 64, "rgbMatchesPin": True,
                "paintCompleted": True, "assetReadMicros": 1, "checksumMicros": 2, "decodeMicros": 3,
                "rgbReadbackMicros": 4, "widget": {"logicalWidth": 274, "logicalHeight": 1334 / 3,
                    "dpr": 3, "physicalWidth": 822, "physicalHeight": 1334, "logicalLeft": 58,
                    "logicalTop": 100, "physicalLeft": 174, "physicalTop": 300, "pixelAligned": True, "fit": "contain"},
                "rssBefore": {"available": True, "bytes": 1000}, "rssLoaded": {"available": True, "bytes": 2000}}
            self.results.append(row)
            name = f"{index + 1}-{case_id}.png"
            data = b"\x89PNG\r\n\x1a\n" + bytes([index]) * 32
            mobile.write_new(self.directory / name, data)
            self.screenshots.append({"caseIndex": index, "screenshotName": name, "byteSize": len(data),
                "afterPaintedEpoch": index + 1, "sha256": sha256(data).hexdigest()})
        self.observations = [{"caseIndex": index, "painted": {"enabled": True, "profileMode": True,
            "runNonce": self.nonce,
            "stopped": False, "status": "loaded", "caseIndex": index, "epoch": index + 1,
            "paintedEpoch": index + 1, "completedCases": index + 1, "cases": self.results[:index + 1],
            "activeBuffers": 0, "activeDescriptors": 0, "activeCodecs": 0, "activeImages": 1}} for index in range(6)]
        self.driver = {"status": "PASS", "passed": True, "runNonce": self.nonce,
            "initial": {"enabled": True, "profileMode": True, "ready": True, "stopped": False,
                "runNonce": self.nonce,
                "completedCases": 0, "cases": [], **{key: 0 for key in mobile.COUNTERS}},
            "observations": self.observations, "captures": self.screenshots,
            "stopped": {"ok": True, "stopped": True, "runNonce": self.nonce,
                "allCasesPassed": True, "completedCases": 6, "cases": self.results,
                **{key: 0 for key in mobile.COUNTERS}, "rssStop": {"available": False}}}

    def tearDown(self):
        self.home_patch.stop()
        self.external_root_patch.stop()
        self.temporary.cleanup()

    def test_external_root_requires_exact_canonical_owned_private_directory(self):
        approved = self.directory / "approved"
        approved.mkdir(mode=0o700)
        file = approved / "receipt.json"
        mobile.write_new(file, {"private": True})
        with patch.object(mobile, "EXTERNAL_QA_ROOT", approved):
            self.assertEqual(mobile.private_path(file), file)
            self.assertEqual(mobile.new_private_directory(approved / "new"), approved / "new")
            with self.assertRaises(EntryError):
                mobile.new_private_directory(Path("/private/tmp/forbidden-new-qa"))
            sibling = self.directory / "approved-other"
            sibling.mkdir(mode=0o700)
            mobile.write_new(sibling / "receipt.json", {})
            with self.assertRaises(EntryError):
                mobile.private_path(sibling / "receipt.json")
            os.chmod(approved, 0o755)
            with self.assertRaisesRegex(EntryError, "external_qa_root_rejected"):
                mobile.private_path(file)
            os.chmod(approved, 0o700)
            with patch.object(mobile.os, "getuid", return_value=-1), self.assertRaisesRegex(EntryError, "external_qa_root_rejected"):
                mobile.approved_external_root()
        link = self.directory / "linked-root"
        link.symlink_to(approved, target_is_directory=True)
        with patch.object(mobile, "EXTERNAL_QA_ROOT", link), self.assertRaisesRegex(EntryError, "external_qa_root_rejected"):
            mobile.approved_external_root()

    def test_runtime_environment_uses_run_root_and_canonical_default_warmed_caches(self):
        home = self.directory / "fake-home"
        home.mkdir(mode=0o700)
        gradle = self.directory / "warm-gradle"
        pub = self.directory / "warm-pub"
        gradle.mkdir(mode=0o700)
        pub.mkdir(mode=0o700)
        (home / ".gradle").symlink_to(gradle, target_is_directory=True)
        (home / ".pub-cache").symlink_to(pub, target_is_directory=True)
        with patch.object(mobile.Path, "home", return_value=home), patch.dict(os.environ, {
                "GRADLE_USER_HOME": "/wrong-gradle", "PUB_CACHE": "/wrong-pub",
                "JAVA_TOOL_OPTIONS": "-Dprivate-token=bad", "TMPDIR": "/wrong-temp"}):
            expected = mobile.runtime_environment_binding(self.directory)
            environment = mobile.qa_tool_environment(self.directory)
        self.assertEqual(expected["TMPDIR"], str(self.directory))
        self.assertEqual(expected["JAVA_TOOL_OPTIONS"], "-Djava.io.tmpdir=" + str(self.directory))
        self.assertEqual(expected["GRADLE_USER_HOME"], str(gradle))
        self.assertEqual(expected["PUB_CACHE"], str(pub))
        self.assertTrue((self.directory / "device-run").is_relative_to(Path(environment["TMPDIR"])))
        self.assertTrue(all(environment[key] == value for key, value in expected.items()))
        self.assertNotIn("private-token", json.dumps(environment))
        self.assertEqual(sorted(path.name for path in gradle.iterdir()), [])
        self.assertEqual(sorted(path.name for path in pub.iterdir()), [])

    def test_main_disk_default_cache_is_rejected_without_copy_or_fallback(self):
        actual_home = self.actual_home
        home = self.directory / "fake-cache-home"
        home.mkdir(mode=0o700)
        (home / ".gradle").symlink_to(actual_home, target_is_directory=True)
        with patch.object(mobile.Path, "home", return_value=home), self.assertRaisesRegex(EntryError, "runtime_cache_not_external"):
            mobile.runtime_environment_binding(self.directory)

    def preflight_fixture(self):
        output = self.directory / "old-signing-output"
        output.mkdir(mode=0o700)
        signing = self.directory / "original-signing.xcconfig"
        flutter = self.directory / "flutter-sdk/bin/flutter"
        flutter.parent.mkdir(parents=True, mode=0o700)
        mobile.write_new(flutter, b"fixture-flutter")
        os.chmod(flutter, 0o700)
        settings = {"DEVELOPMENT_TEAM": "private-team", "CODE_SIGN_IDENTITY": "private-identity",
            "CODE_SIGN_STYLE": "Manual", "IMAGE_EXPERIMENT_APP_PROVISIONING_PROFILE_SPECIFIER": "private-app-profile",
            "IMAGE_EXPERIMENT_TEST_PROVISIONING_PROFILE_SPECIFIER": "private-test-profile",
            "IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT": str(output), "IMAGE_EXPERIMENT_SIGNING_CONFIG": str(signing),
            "FLUTTER_ROOT": str(flutter.parent.parent), "FLUTTER_BUILD_DIR": "build"}
        mobile.write_new(signing, ("\n".join(f"{key} = {value}" for key, value in settings.items()) + "\n").encode())
        hardware = self.directory / "hardware.private.json"
        mobile.write_new(hardware, {"result": {"identifier": "private-core-device-uuid",
            "hardwareProperties": {"udid": "private-device", "reality": "physical", "platform": "iOS",
                "marketingName": "iPhone 12"}}})
        devices = self.directory / "flutter-devices.stdout"
        mobile.write_new(devices, [{"id": "private-device", "isSupported": True, "emulator": False,
            "targetPlatform": "ios"}])
        receipt = self.directory / "flutter-devices.json"
        command = [str(flutter), "devices", "--machine", "--device-connection=attached", "--device-timeout=15"]
        mobile.write_new(receipt, {"status": "ok", "argumentSha256": mobile.digest(mobile.frozen_json(command)),
            "traceSha256": [mobile.file_hash(devices), mobile.digest(b"")]})
        preflight = self.directory / "new-preflight.private.json"
        mobile.write_new(preflight, {"mode": "profile", "expectedAppId": mobile.APP_ID,
            "device": {"model": "iPhone 12", "selector": "private-device", "hardwareUdid": "private-device",
                "physical": True, "transportType": "wired", "hardwareEvidencePath": str(hardware),
                "hardwareEvidenceSha256": mobile.file_hash(hardware)},
            "flutterDiscovery": {"deviceConnectionInterface": "attached", "sourcePath": str(devices),
                "sha256": mobile.file_hash(devices), "toolReceiptPath": str(receipt),
                "toolReceiptSha256": mobile.file_hash(receipt), "flutterExecutable": str(flutter)},
            "signingConfig": {"sourcePath": str(signing), "sha256": mobile.file_hash(signing)}})
        return preflight, mobile.file_hash(preflight), signing, settings

    def test_explicit_preflight_pair_and_unchanged_legacy_default(self):
        path, pin, signing, settings = self.preflight_fixture()
        self.assertEqual(mobile.preflight_selection(), (mobile.PREFLIGHT, mobile.PREFLIGHT_SHA))
        observed, selected, values = mobile.signing_preflight(path, pin)
        self.assertEqual((selected, values), (signing, settings))
        self.assertEqual(observed["device"]["selector"], "private-device")
        with patch.object(mobile, "read_source") as read:
            for args in ((path, None), (None, pin), (path, "bad-pin")):
                with self.subTest(args=args), self.assertRaises(EntryError):
                    mobile.prepare(self.directory / "new-run", *args)
            read.assert_not_called()

    def save_preflight(self, path, data):
        path.write_bytes(json.dumps(data).encode())
        return mobile.file_hash(path)

    def assert_selection_rejected_before_prepare(self, path, data):
        pin = self.save_preflight(path, data)
        output = self.directory / "new-run"
        with patch.object(mobile, "fixtures") as fixtures, patch.object(mobile, "private_mobile_snapshot") as copy, \
            patch.object(mobile, "checked_tool") as tool, self.assertRaises(EntryError):
            mobile.prepare(output, path, pin)
        fixtures.assert_not_called()
        copy.assert_not_called()
        tool.assert_not_called()
        self.assertFalse(output.exists())

    def test_explicit_preflight_rejects_coredevice_wrong_missing_and_inconsistent_identity(self):
        path, _, _, _ = self.preflight_fixture()
        original = mobile.private_json(path)
        mutations = [lambda d: d["device"].update(selector="private-core-device-uuid"),
            lambda d: d["device"].update(selector="private-core-device-uuid", hardwareUdid="private-core-device-uuid"),
            lambda d: d["device"].pop("hardwareUdid"), lambda d: d["device"].update(physical=False),
            lambda d: d["device"].update(transportType="localNetwork"), lambda d: d.pop("flutterDiscovery"),
            lambda d: d["device"].pop("hardwareEvidenceSha256"),
            lambda d: d["flutterDiscovery"].pop("toolReceiptPath")]
        for mutation in mutations:
            data = deepcopy(original)
            mutation(data)
            with self.subTest(mutation=mutation):
                self.assert_selection_rejected_before_prepare(path, data)

    def test_legacy_fixed_default_keeps_its_original_selector_behavior(self):
        path, _, signing, settings = self.preflight_fixture()
        data = mobile.private_json(path)
        data.pop("flutterDiscovery")
        data["device"] = {"model": "iPhone 12", "selector": "historical-core-device-id"}
        pin = self.save_preflight(path, data)
        with patch.object(mobile, "PREFLIGHT", path), patch.object(mobile, "PREFLIGHT_SHA", pin):
            observed, source, values = mobile.signing_preflight()
        self.assertEqual((source, values), (signing, settings))
        self.assertEqual(observed["device"]["selector"], "historical-core-device-id")

    def test_pinned_discovery_requires_one_supported_real_ios_match(self):
        path, _, _, _ = self.preflight_fixture()
        original = mobile.private_json(path)
        source = Path(original["flutterDiscovery"]["sourcePath"])
        receipt_path = Path(original["flutterDiscovery"]["toolReceiptPath"])
        receipt = mobile.private_json(receipt_path)
        row = {"id": "private-device", "isSupported": True, "emulator": False, "targetPlatform": "ios"}
        for rows in ([], [{**row, "id": "other-device"}], [row, row], [{**row, "isSupported": False}],
                [{**row, "emulator": True}], [{**row, "targetPlatform": "android-arm64"}],
                [{**row, "emulator": 0}], {"devices": [row]}):
            data = deepcopy(original)
            source.write_bytes(json.dumps(rows).encode())
            data["flutterDiscovery"]["sha256"] = mobile.file_hash(source)
            tool = deepcopy(receipt)
            tool["traceSha256"][0] = mobile.file_hash(source)
            receipt_path.write_bytes(json.dumps(tool).encode())
            data["flutterDiscovery"]["toolReceiptSha256"] = mobile.file_hash(receipt_path)
            with self.subTest(rows=rows):
                self.assert_selection_rejected_before_prepare(path, data)

    def test_hardware_proof_requires_actual_physical_iphone12_and_same_udid(self):
        path, _, _, _ = self.preflight_fixture()
        original = mobile.private_json(path)
        proof = Path(original["device"]["hardwareEvidencePath"])
        hardware = mobile.private_json(proof)
        for replacement in ({"reality": "simulated"}, {"platform": "macOS"},
                {"marketingName": "iPhone 13"}, {"udid": "another-hardware-device"}):
            observed = deepcopy(hardware)
            observed["result"]["hardwareProperties"].update(replacement)
            proof.write_bytes(json.dumps(observed).encode())
            data = deepcopy(original)
            data["device"]["hardwareEvidenceSha256"] = mobile.file_hash(proof)
            with self.subTest(replacement=replacement):
                self.assert_selection_rejected_before_prepare(path, data)

    def test_discovery_executable_must_be_current_signing_flutter_root(self):
        path, _, _, _ = self.preflight_fixture()
        data = mobile.private_json(path)
        other = self.directory / "different-flutter"
        mobile.write_new(other, b"other-flutter")
        os.chmod(other, 0o700)
        data["flutterDiscovery"]["flutterExecutable"] = str(other)
        self.assert_selection_rejected_before_prepare(path, data)

    def test_discovery_receipt_requires_exact_pinned_attached_command_and_stdout(self):
        path, _, _, _ = self.preflight_fixture()
        original = mobile.private_json(path)
        receipt_path = Path(original["flutterDiscovery"]["toolReceiptPath"])
        original_receipt = mobile.private_json(receipt_path)
        mutations = [lambda r: r.update(status="failed"), lambda r: r.update(argumentSha256="0" * 64),
            lambda r: r.update(argumentSha256=mobile.digest(mobile.frozen_json([
                original["flutterDiscovery"]["flutterExecutable"], "devices", "--machine", "--device-timeout=15"]))),
            lambda r: r.update(traceSha256=["f" * 64, mobile.digest(b"")])]
        for mutation in mutations:
            data, receipt = deepcopy(original), deepcopy(original_receipt)
            mutation(receipt)
            receipt_path.write_bytes(json.dumps(receipt).encode())
            data["flutterDiscovery"]["toolReceiptSha256"] = mobile.file_hash(receipt_path)
            with self.subTest(mutation=mutation):
                self.assert_selection_rejected_before_prepare(path, data)
        data = deepcopy(original)
        receipt_path.write_bytes(json.dumps(original_receipt).encode())
        data["flutterDiscovery"]["deviceConnectionInterface"] = "wireless"
        self.assert_selection_rejected_before_prepare(path, data)

    def test_changed_discovery_hardware_or_receipt_and_symlink_cannot_reach_tools(self):
        path, pin, signing, _ = self.preflight_fixture()
        data = mobile.private_json(path)
        run = self.directory / "bound-run"
        run.mkdir(mode=0o700)
        mobile.write_new(run / "prepare-receipt.json", {"preflightPath": str(path), "preflightSha256": pin,
            "preflightSigningConfigPath": str(signing), "preflightSigningConfigSha256": mobile.file_hash(signing),
            "deviceSelector": "private-device"})
        mobile.write_new(run / "build-receipt.json", {})
        for name in (data["device"]["hardwareEvidencePath"], data["flutterDiscovery"]["sourcePath"],
                data["flutterDiscovery"]["toolReceiptPath"]):
            proof = Path(name)
            original = proof.read_bytes()
            proof.write_bytes(b"changed-proof")
            with patch.object(mobile, "checked_tool") as tool, patch.object(mobile, "bounded_tool") as launch:
                for stage in (mobile.build, mobile.run):
                    with self.subTest(proof=proof.name, stage=stage.__name__), self.assertRaisesRegex(EntryError, "device_proof_changed"):
                        stage(run)
                tool.assert_not_called()
                launch.assert_not_called()
            proof.write_bytes(original)
        proof = Path(data["flutterDiscovery"]["sourcePath"])
        saved = self.directory / "saved-discovery.stdout"
        proof.rename(saved)
        proof.symlink_to(saved)
        self.assert_selection_rejected_before_prepare(path, data)

    def test_changed_preflight_or_signing_is_rejected_before_fixture_read_or_tools(self):
        path, pin, signing, _ = self.preflight_fixture()
        with patch.object(mobile, "fixtures") as fixtures, patch.object(mobile, "checked_tool") as tool:
            with self.assertRaisesRegex(EntryError, "preflight_changed"):
                mobile.prepare(self.directory / "new-run", path, "0" * 64)
            signing.write_bytes(b"changed-signing")
            with self.assertRaisesRegex(EntryError, "signing_changed"):
                mobile.prepare(self.directory / "new-run", path, pin)
            fixtures.assert_not_called()
            tool.assert_not_called()
        self.assertFalse((self.directory / "new-run").exists())

    def test_preflight_symlink_loose_mode_wrong_owner_and_signing_symlink_are_rejected(self):
        path, pin, signing, _ = self.preflight_fixture()
        link = self.directory / "preflight-link"
        link.symlink_to(path)
        with self.assertRaises(EntryError):
            mobile.signing_preflight(link, pin)
        os.chmod(path, 0o644)
        with patch.object(mobile, "read_source") as read, self.assertRaises(EntryError):
            mobile.signing_preflight(path, pin)
        read.assert_not_called()
        os.chmod(path, 0o600)
        with patch.object(mobile.os, "getuid", return_value=-1), self.assertRaises(EntryError):
            mobile.signing_preflight(path, pin)
        saved = self.directory / "saved-signing"
        signing.rename(saved)
        signing.symlink_to(saved)
        with self.assertRaises(EntryError):
            mobile.signing_preflight(path, pin)

    def test_preflight_receipt_binding_is_checked_before_snapshot_or_fixture_read(self):
        path, pin, signing, _ = self.preflight_fixture()
        receipt = {"preflightPath": str(path), "preflightSha256": pin,
            "preflightSigningConfigPath": str(signing), "preflightSigningConfigSha256": mobile.file_hash(signing),
            "deviceSelector": "wrong-device"}
        with patch.object(mobile, "local_sources") as sources, patch.object(mobile, "fixtures") as fixtures:
            with self.assertRaisesRegex(EntryError, "preflight_binding_rejected"):
                mobile.validate_snapshot(receipt)
            receipt["deviceSelector"] = "private-device"
            receipt["preflightSigningConfigSha256"] = "f" * 64
            with self.assertRaisesRegex(EntryError, "preflight_binding_rejected"):
                mobile.validate_snapshot(receipt)
            sources.assert_not_called()
            fixtures.assert_not_called()

    def test_changed_selected_inputs_block_build_and_run_before_tool_calls(self):
        path, pin, signing, _ = self.preflight_fixture()
        run = self.directory / "new-run"
        run.mkdir(mode=0o700)
        receipt = {"preflightPath": str(path), "preflightSha256": pin,
            "preflightSigningConfigPath": str(signing), "preflightSigningConfigSha256": mobile.file_hash(signing),
            "deviceSelector": "private-device"}
        mobile.write_new(run / "prepare-receipt.json", receipt)
        mobile.write_new(run / "build-receipt.json", {})
        with patch.object(mobile, "checked_tool") as tool, patch.object(mobile, "bounded_tool") as launch:
            signing.write_bytes(b"changed-signing")
            for stage in (mobile.build, mobile.run):
                with self.subTest(stage=stage.__name__), self.assertRaisesRegex(EntryError, "signing_changed"):
                    stage(run)
            path.write_bytes(b"changed-preflight")
            for stage in (mobile.build, mobile.run):
                with self.subTest(stage=stage.__name__), self.assertRaisesRegex(EntryError, "preflight_changed"):
                    stage(run)
            tool.assert_not_called()
            launch.assert_not_called()

    def test_cli_preflight_and_preservation_arguments_only_apply_to_prepare(self):
        path, pin, _, _ = self.preflight_fixture()
        for mode in ("--build", "--run", "--verify"):
            for extra in (("--preflight", str(path), "--preflight-sha256", pin),
                    ("--preserve-private-dir", str(self.directory))):
                with self.subTest(mode=mode, extra=extra), patch.object(mobile, "build") as build, \
                    patch.object(mobile, "run") as run, patch.object(mobile, "verify") as verify:
                    self.assertEqual(mobile.main([mode, "--private-dir", str(self.directory), *extra]), 2)
                    build.assert_not_called()
                    run.assert_not_called()
                    verify.assert_not_called()
        with patch.object(mobile, "prepare") as prepare:
            self.assertEqual(mobile.main(["--prepare", "--private-dir", str(self.directory / "new-run"),
                "--preflight", str(path), "--preflight-sha256", pin,
                "--preserve-private-dir", str(self.directory / "old-signing-output")]), 0)
            self.assertEqual(prepare.call_args.args[1:], (path, pin, [self.directory / "old-signing-output"]))

    def validate(self, result=None):
        return mobile.validate_driver(result or self.driver, self.cases, self.directory, self.nonce)

    def test_pinned_existing_six_files_are_accepted_without_encode(self):
        _, cases = mobile.fixtures()
        self.assertEqual(tuple(row["caseId"] for row in cases), mobile.CASE_IDS)
        self.assertEqual(len(cases), 6)
        self.assertTrue(all(row["width"] == 822 and row["height"] == 1334 for row in cases))

    def test_changed_fixture_evidence_is_rejected_before_paths_are_used(self):
        path = self.directory / "evidence.json"
        mobile.write_new(path, {"images": [{"path": "/private/secret"}]})
        with patch.object(mobile, "EVIDENCE", path), self.assertRaisesRegex(EntryError, "fixture_evidence_changed"):
            mobile.fixtures()

    def test_wrong_header_format_and_dimensions_are_rejected(self):
        for kind, data in (("PNG", b"RIFF"), ("WEBP", b"\x89PNG\r\n\x1a\n"), ("JPEG", bytes(40))):
            with self.subTest(kind=kind), self.assertRaisesRegex(EntryError, "fixture_header_rejected"):
                mobile.validate_header(data, kind)

    def test_six_cases_paint_rgb_geometry_and_empty_stop_are_accepted(self):
        result = self.validate()
        self.assertEqual(len(result["cases"]), 6)
        self.assertEqual(len(result["screenshots"]), 6)
        self.assertEqual(result["stopped"]["activeImages"], 0)

    def test_missing_case_wrong_order_hash_rgb_paint_and_scaled_geometry_are_rejected(self):
        mutations = [lambda data: data["stopped"]["cases"].pop(),
            lambda data: data["stopped"]["cases"].reverse(),
            lambda data: data["stopped"]["cases"][0].update(encodedSha256="f" * 64),
            lambda data: data["stopped"]["cases"][0].update(rgbSha256="f" * 64),
            lambda data: data["stopped"]["cases"][0].update(rgbBytes=3289643),
            lambda data: data["stopped"]["cases"][0].update(paintCompleted=False),
            lambda data: data["stopped"]["cases"][0]["widget"].update(physicalWidth=1170),
            lambda data: data["stopped"]["cases"][0]["widget"].update(logicalWidth=390),
            lambda data: data["stopped"]["cases"][0]["widget"].update(physicalLeft=174.1),
            lambda data: data["stopped"]["cases"][0]["widget"].update(pixelAligned=False),
            lambda data: data["stopped"]["cases"][0].update(decodeMicros=True),
            lambda data: data["stopped"]["cases"][0].update(rgbReadbackMicros=-1),
            lambda data: data["stopped"]["cases"][0].update(rssBefore="unavailable"),
            lambda data: data["observations"][1]["painted"].update(paintedEpoch=99),
            lambda data: data["stopped"].update(allCasesPassed=False)]
        for mutation in mutations:
            value = deepcopy(self.driver)
            mutation(value)
            with self.subTest(mutation=mutation), self.assertRaises(EntryError):
                self.validate(value)

    def test_debug_disabled_and_nonzero_or_missing_owned_resources_are_rejected(self):
        for key, replacement in (("profileMode", False), ("enabled", False)):
            value = deepcopy(self.driver)
            value["initial"][key] = replacement
            with self.subTest(key=key), self.assertRaises(EntryError):
                self.validate(value)
        for counter in mobile.COUNTERS:
            value = deepcopy(self.driver)
            value["stopped"][counter] = 1
            with self.subTest(counter=counter), self.assertRaisesRegex(EntryError, "stop_resources_retained"):
                self.validate(value)
        value = deepcopy(self.driver)
        del value["stopped"]["activeImages"]
        with self.assertRaisesRegex(EntryError, "stop_resources_retained"):
            self.validate(value)

    def test_rss_unavailable_is_explicit_and_does_not_claim_peak(self):
        self.results[0].update(rssBefore={"available": False}, rssLoaded={"available": False})
        self.assertEqual(self.validate()["cases"][0]["rssBefore"], {"available": False})

    def test_screenshot_traversal_symlink_and_changed_bytes_are_rejected(self):
        value = deepcopy(self.driver)
        value["captures"][0]["screenshotName"] = "../private.png"
        with self.assertRaisesRegex(EntryError, "screenshot_path_rejected"):
            self.validate(value)
        first = self.directory / self.screenshots[0]["screenshotName"]
        first.write_bytes(b"changed")
        with self.assertRaisesRegex(EntryError, "screenshot_changed"):
            self.validate()
        first.unlink()
        first.symlink_to(self.directory / self.screenshots[1]["screenshotName"])
        with self.assertRaises(EntryError):
            self.validate()

    def test_unknown_driver_fields_do_not_enter_public_projection(self):
        self.results[0]["token"] = "private-token"
        self.driver["stopped"]["deviceSelector"] = "private-device"
        projection = json.dumps(self.validate())
        self.assertNotIn("private-token", projection)
        self.assertNotIn("private-device", projection)

    def test_new_receipts_and_public_evidence_never_overwrite(self):
        path = self.directory / "receipt.json"
        mobile.write_new(path, {"first": True})
        before = path.read_bytes()
        with self.assertRaises(FileExistsError):
            mobile.write_new(path, {"first": False})
        self.assertEqual(path.read_bytes(), before)
        public = self.directory / "public"
        report = {"status": "NOT_RUN", "reason": "DRIVER_NOT_RUN"}
        with patch.object(mobile, "PUBLIC", public):
            mobile.publish(report, public=public)
        before = (public / "verification.json").read_bytes()
        text = (public / "verification.md").read_text()
        self.assertIn("**0/6 durum**", text)
        self.assertIn("kabul edilen screenshot: **0/6**", text)
        self.assertNotIn("Her format için tek seri örnek vardır", text)
        with patch.object(mobile, "PUBLIC", public), self.assertRaisesRegex(EntryError, "public_evidence_exists"):
            mobile.publish({"status": "PASS"}, public=public)
        self.assertEqual((public / "verification.json").read_bytes(), before)

    def test_attempt_public_destination_preserves_root_reports_and_never_overwrites(self):
        root = self.directory / "public"
        root.mkdir()
        mobile.write_new(root / "verification.json", {"status": "NOT_RUN", "launch": "FAIL"})
        mobile.write_new(root / "verification.md", b"Eski basarisiz deneme.\n")
        hashes = mobile.tree_hashes(root)
        attempt = root / "attempts/20261018-fresh-attempt"
        with patch.object(mobile, "PUBLIC", root):
            mobile.publish({"status": "NOT_RUN", "reason": "DRIVER_NOT_RUN"}, public=attempt)
            self.assertEqual(mobile.file_hash(root / "verification.json"), hashes["verification.json"])
            self.assertEqual(mobile.file_hash(root / "verification.md"), hashes["verification.md"])
            before = mobile.tree_hashes(attempt)
            with self.assertRaisesRegex(EntryError, "public_evidence_exists"):
                mobile.publish({"status": "PASS"}, public=attempt)
            self.assertEqual(mobile.tree_hashes(attempt), before)
            with self.assertRaisesRegex(EntryError, "public_evidence_exists"):
                mobile.public_destination(root)

    def test_public_destination_rejects_escape_traversal_and_symlink(self):
        root = self.directory / "public"
        root.mkdir()
        outside = self.directory / "outside"
        outside.mkdir()
        (root / "attempts").symlink_to(outside, target_is_directory=True)
        with patch.object(mobile, "PUBLIC", root):
            for target in [outside / "new", root / "other/new", root / "attempts/../escaped",
                root / "attempts/unsafe_name", root / "attempts/new"]:
                with self.subTest(target=target), self.assertRaisesRegex(EntryError, "public_path_rejected"):
                    mobile.public_destination(target)
        self.assertEqual(mobile.tree_hashes(outside), {})

    def test_private_report_copies_all_six_screens_exclusively_with_private_modes(self):
        run = self.directory / "new-run"
        run.mkdir(mode=0o700)
        target = run / "report"
        projection = self.validate()
        report = {"status": "PASS", "acceptedCases": 6, "acceptedScreenshots": 6, "cases": projection["cases"]}
        mobile.publish(report, projection["screenshots"], self.directory, private_report=target)
        self.assertEqual(stat.S_IMODE(target.stat().st_mode), 0o700)
        self.assertEqual(len(list(target.iterdir())), 8)
        for path in target.iterdir():
            self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
        for screen in projection["screenshots"]:
            self.assertEqual(mobile.file_hash(target / screen["path"]), screen["sha256"])
        text = (target / "verification.json").read_text()
        self.assertNotIn("private-device", text)
        self.assertNotIn("private-team", text)
        before = mobile.tree_hashes(target)
        with self.assertRaises(EntryError):
            mobile.publish(report, private_report=target)
        self.assertEqual(mobile.tree_hashes(target), before)

    def test_private_report_rejects_repo_similar_root_symlink_and_loose_parent(self):
        run = self.directory / "new-run"
        run.mkdir(mode=0o700)
        link = self.directory / "linked-run"
        link.symlink_to(run, target_is_directory=True)
        for path in (mobile.ROOT / "be/var/report", link / "report", self.directory / "report",
                run / "other-name"):
            with self.subTest(path=path), self.assertRaises(EntryError):
                mobile.private_report_destination(path)
        sibling = self.directory.parent / (self.directory.name + "-similar")
        sibling.mkdir(mode=0o700)
        try:
            with self.assertRaises(EntryError):
                mobile.private_report_destination(sibling / "report")
        finally:
            sibling.rmdir()
        os.chmod(run, 0o755)
        with self.assertRaises(EntryError):
            mobile.private_report_destination(run / "report")
        self.assertEqual(list(run.iterdir()), [])

    def test_verify_binds_private_report_to_same_run_and_cli_limits_destination(self):
        run = self.directory / "new-run"
        run.mkdir(mode=0o700)
        target = run / "report"
        mobile.write_new(run / "prepare-receipt.json", {"sourceSha256": "source-pin",
            "pubspecLockSha256": "pub-pin", "podLockSha256": "pod-pin"})
        mobile.write_new(run / "preservation-before.private.json", {})
        with patch.object(mobile, "validate_snapshot"), \
            patch.object(mobile, "assert_preserved", return_value={"unchanged": True}):
            report = mobile.verify(run, private_report=target)
        self.assertEqual(report["status"], "NOT_RUN")
        self.assertEqual(stat.S_IMODE(target.stat().st_mode), 0o700)
        other = self.directory / "other-run"
        other.mkdir(mode=0o700)
        with patch.object(mobile, "private_json") as read, self.assertRaisesRegex(EntryError, "private_report_binding_rejected"):
            mobile.verify(run, private_report=other / "report")
        read.assert_not_called()
        for mode in ("--prepare", "--build", "--run"):
            with self.subTest(mode=mode), patch.object(mobile, "prepare") as prepare, \
                patch.object(mobile, "build") as build, patch.object(mobile, "run") as launch:
                self.assertEqual(mobile.main([mode, "--private-dir", str(run), "--report-dir", str(target)]), 2)
                prepare.assert_not_called()
                build.assert_not_called()
                launch.assert_not_called()
        with patch.object(mobile, "verify") as verify:
            self.assertEqual(mobile.main(["--verify", "--private-dir", str(run), "--report-dir", str(target),
                "--public-dir", str(mobile.PUBLIC / "attempts/conflict")]), 2)
            verify.assert_not_called()

    def test_verify_forwards_official_public_argument_and_cli_limits_it_to_verify(self):
        root = self.directory / "public"
        root.mkdir()
        target = root / "attempts/20261018-cli-attempt"
        mobile.write_new(self.directory / "prepare-receipt.json", {"sourceSha256": "source-pin",
            "pubspecLockSha256": "pub-pin", "podLockSha256": "pod-pin"})
        mobile.write_new(self.directory / "preservation-before.private.json", {})
        with patch.object(mobile, "PUBLIC", root), patch.object(mobile, "validate_snapshot"), \
            patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
            patch.object(mobile, "publish", side_effect=lambda report, *args, **kwargs: report) as publish:
            result = mobile.verify(self.directory, public=target)
            self.assertEqual(result["status"], "NOT_RUN")
            self.assertEqual(publish.call_args.kwargs["public"], target)
        for mode in ["--prepare", "--build", "--run"]:
            with patch.object(mobile, "prepare") as prepare, patch.object(mobile, "build") as build, \
                patch.object(mobile, "run") as run:
                self.assertEqual(mobile.main([mode, "--private-dir", str(self.directory),
                    "--public-dir", str(target)]), 2)
                prepare.assert_not_called()
                build.assert_not_called()
                run.assert_not_called()

    def test_not_run_and_failed_launch_have_planned_but_zero_accepted_samples(self):
        mobile.write_new(self.directory / "prepare-receipt.json", {"sourceSha256": "source-pin",
            "pubspecLockSha256": "pub-pin", "podLockSha256": "pod-pin"})
        mobile.write_new(self.directory / "preservation-before.private.json", {})
        with patch.object(mobile, "PUBLIC", self.directory / "public"), \
            patch.object(mobile, "validate_snapshot"), \
            patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
            patch.object(mobile, "publish", side_effect=lambda report, *args, **kwargs: report):
            not_run = mobile.verify(self.directory)
            mobile.write_new(self.directory / "launch-attempt.json", {"status": "STARTED"})
            failed = mobile.verify(self.directory, not_run_reason="DRIVER_FAILED")
        self.assertEqual(not_run["status"], "NOT_RUN")
        self.assertEqual((failed["status"], failed["reason"]), ("FAIL", "DRIVER_FAILED"))
        for report in [not_run, failed]:
            self.assertEqual(report["plannedSamplesPerFormatPerAsset"], 1)
            self.assertEqual(report["acceptedSamplesPerFormatPerAsset"], 0)
            self.assertEqual(report["acceptedCases"], 0)
            self.assertEqual(report["acceptedScreenshots"], 0)
            self.assertNotIn("samplesPerFormatPerAsset", report)

    def test_only_fully_verified_driver_and_device_accept_six_runtime_samples(self):
        mobile.write_new(self.directory / "prepare-receipt.json", {"sourceSha256": "source-pin",
            "runNonce": self.nonce, "deviceSelector": "private-selected-iphone",
            "pubspecLockSha256": "pub-pin", "podLockSha256": "pod-pin"})
        mobile.write_new(self.directory / "preservation-before.private.json", {})
        mobile.write_new(self.directory / "build-receipt.json", {"engineRevision": "engine", "flutterVersion": "version"})
        mobile.write_new(self.directory / "launch-receipt.json", {"vmServiceUrlSha256": "f" * 64})
        driver = self.directory / "driver.json"
        mobile.write_new(driver, self.driver)
        with patch.object(mobile, "PUBLIC", self.directory / "public"), \
            patch.object(mobile, "validate_snapshot"), \
            patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
            patch.object(mobile, "fixtures", return_value=({}, self.cases)), \
            patch.object(mobile, "validate_launch", return_value="binary-pin"), \
            patch.object(mobile, "physical_device", return_value={"physicalDevice": True}) as device, \
            patch.object(mobile, "publish", side_effect=lambda report, *args, **kwargs: report):
            passed = mobile.verify(self.directory, driver)
            device.side_effect = EntryError("physical_device_unverified")
            rejected = mobile.verify(self.directory, driver)
        self.assertEqual(passed["status"], "PASS")
        self.assertEqual(passed["plannedSamplesPerFormatPerAsset"], 1)
        self.assertEqual(passed["acceptedSamplesPerFormatPerAsset"], 1)
        self.assertEqual((passed["acceptedCases"], passed["acceptedScreenshots"]), (6, 6))
        self.assertEqual((rejected["status"], rejected["reason"]), ("FAIL", "VERIFICATION_REJECTED"))
        self.assertEqual(rejected["acceptedSamplesPerFormatPerAsset"], 0)
        self.assertEqual((rejected["acceptedCases"], rejected["acceptedScreenshots"]), (0, 0))

    def test_offline_build_command_is_exact_profile_without_provision_download(self):
        command = mobile.build_command(self.directory / "mobile", self.directory / "output", self.directory / "signing.xcconfig", self.nonce)
        self.assertIn("Profile-imageexperiment", command)
        self.assertIn("iphoneos", command)
        self.assertIn("FLUTTER_BUILD_MODE=profile", command)
        self.assertIn("FLUTTER_TARGET=lib/main_image_experiment.dart", command)
        self.assertIn("DART_DEFINES=" + mobile.dart_defines(self.nonce), command)
        self.assertIn(",", mobile.dart_defines(self.nonce))
        self.assertFalse(any("ProvisioningUpdates" in arg or "ProvisioningDeviceRegistration" in arg for arg in command))
        self.assertEqual(command[command.index("-jobs") + 1], "2")

    def test_private_launcher_caps_time_and_keeps_trace_outside_public_output(self):
        status = mobile.bounded_tool([sys.executable, "-c", "print('yerel araç')"], self.directory, "success", seconds=1)
        self.assertEqual(status, "ok")
        self.assertEqual(stat.S_IMODE((self.directory / "success.stdout").stat().st_mode), 0o600)
        status = mobile.bounded_tool([sys.executable, "-c", "import time; time.sleep(5)"], self.directory, "timeout", seconds=.05)
        self.assertEqual(status, "timeout")
        self.assertEqual(json.loads((self.directory / "timeout.json").read_text())["status"], "timeout")

    def test_offline_tool_is_wrapped_with_inherited_network_denial(self):
        with patch.object(mobile.shutil, "which", return_value="/usr/bin/sandbox-exec"), \
            patch.object(mobile, "bounded_tool", return_value="ok") as launch:
            mobile.write_new(self.directory / "offline.stdout", b"ok")
            mobile.checked_tool(["flutter", "pub", "get", "--offline"], self.directory, "offline", offline=True)
        args = launch.call_args.args[0]
        self.assertEqual(args[:3], ["sandbox-exec", "-p", mobile.NETWORK_POLICY])
        self.assertIn("--offline", args)
        environment = launch.call_args.kwargs["env"]
        self.assertNotIn("FIREBASE_TOKEN", environment)

    def test_stale_missing_and_wrong_runtime_nonce_are_rejected(self):
        for target in ("top", "initial", "painted", "stopped"):
            value = deepcopy(self.driver)
            if target == "top":
                value["runNonce"] = "b2" * 32
            elif target == "painted":
                value["observations"][2]["painted"].pop("runNonce")
            else:
                value[target]["runNonce"] = "b2" * 32
            with self.subTest(target=target), self.assertRaises(EntryError):
                self.validate(value)
        with self.assertRaisesRegex(EntryError, "run_nonce_rejected"):
            mobile.dart_defines("stale")

    def launch_fixture(self):
        output = self.directory / "build-output"
        app = output / "derived-data/Build/Products/Profile-imageexperiment-iphoneos/ImageExperiment.app"
        app.mkdir(parents=True, mode=0o700)
        mobile.write_new(app / "ImageExperiment", b"fixed-compiled-binary")
        prepared = {"sourceSha256": "source-pin", "sourceCopyPath": str(output / "source/repository/app/mobile"),
            "buildOutputPath": str(output), "runNonce": self.nonce, "manifestSha256": "manifest-pin",
            "deviceSelector": "private-selected-iphone"}
        built = {"scope": mobile.SCOPE, "mode": "profile", "appId": mobile.APP_ID, "sourceSha256": "source-pin",
            "runNonce": self.nonce, "manifestSha256": "manifest-pin", "offlineBuild": True,
            "compiledDartDefinesSha256": mobile.digest(mobile.dart_defines(self.nonce).encode()),
            "artifactPath": str(app), "artifactFiles": mobile.tree_hashes(app)}
        mobile.write_new(self.directory / "build-receipt.json", built)
        run_dir = self.directory / "device-run"
        run_dir.mkdir(mode=0o700)
        result = {"runNonce": self.nonce, "vmServiceUrlSha256": "f" * 64, "vmServiceUrlSource": "flutter_drive_environment"}
        result_path = run_dir / "evidence.json"
        mobile.write_new(result_path, result)
        mobile.write_new(self.directory / "launch-attempt.json", {"status": "STARTED", "runNonce": self.nonce})
        mobile.write_new(output / "flutter-drive.json", {"status": "ok",
            "argumentSha256": mobile.digest(mobile.frozen_json(mobile.launch_command(prepared, built)))})
        launch = {"scope": mobile.SCOPE, "status": "PASS", "toolStatus": "ok", "mode": "profile", "appId": mobile.APP_ID,
            "runNonce": self.nonce, "sourceSha256": "source-pin", "sourceCopyPath": prepared["sourceCopyPath"],
            "deviceSelector": prepared["deviceSelector"], "artifactPath": str(app),
            "artifactSha256": mobile.digest(mobile.frozen_json(built["artifactFiles"])),
            "argumentSha256": mobile.digest(mobile.frozen_json(mobile.launch_command(prepared, built))),
            "buildReceiptSha256": mobile.file_hash(self.directory / "build-receipt.json"),
            "driverResultPath": str(result_path), "driverResultSha256": mobile.file_hash(result_path),
            "vmServiceUrlSha256": result["vmServiceUrlSha256"], "vmServiceUrlSource": "flutter_drive_environment",
            "launchAttemptSha256": mobile.file_hash(self.directory / "launch-attempt.json"),
            "toolReceiptSha256": mobile.file_hash(output / "flutter-drive.json")}
        return prepared, built, launch, result, result_path

    def test_launch_requires_exact_compiled_binary_selected_device_source_nonce_and_result(self):
        prepared, built, launch, result, result_path = self.launch_fixture()
        self.assertEqual(mobile.validate_launch(prepared, built, launch, result, result_path, self.directory), launch["artifactSha256"])
        command = mobile.launch_command(prepared, built)
        self.assertIn("--no-pub", command)
        self.assertIn("--profile", command)
        self.assertIn("--no-keep-app-running", command)
        self.assertIn("--device-connection=attached", command)
        self.assertIn("--no-publish-port", command)
        self.assertNotIn("--disable-service-auth-codes", command)
        self.assertIn("--use-application-binary=" + built["artifactPath"], command)
        self.assertEqual(command[command.index("--device-id") + 1], prepared["deviceSelector"])
        for field in ("runNonce", "deviceSelector", "sourceSha256", "sourceCopyPath", "artifactPath",
            "artifactSha256", "argumentSha256", "buildReceiptSha256", "driverResultPath", "driverResultSha256", "vmServiceUrlSha256",
            "vmServiceUrlSource", "launchAttemptSha256", "toolReceiptSha256"):
            value = deepcopy(launch)
            value[field] = "stale-or-wrong-binding"
            with self.subTest(field=field), self.assertRaises(EntryError):
                mobile.validate_launch(prepared, built, value, result, result_path, self.directory)
        for field in ("runNonce", "vmServiceUrlSha256"):
            value = {**result, field: "b" * 64}
            with self.subTest(result_field=field), self.assertRaises(EntryError):
                mobile.validate_launch(prepared, built, launch, value, result_path, self.directory)
        (Path(built["artifactPath"]) / "ImageExperiment").write_bytes(b"changed-binary")
        with self.assertRaisesRegex(EntryError, "build_binding_rejected"):
            mobile.validate_launch(prepared, built, launch, result, result_path, self.directory)

    def test_launch_and_verify_observe_physical_device_in_separate_exclusive_files(self):
        (self.directory / "build-output").mkdir(mode=0o700)
        commands = []
        def capture(argv, output, alias, **kwargs):
            commands.append((argv, alias))
            path = Path(argv[argv.index("--json-output") + 1])
            mobile.write_new(path, {"result": {"hardwareProperties": {"reality": "physical", "platform": "iOS",
                "marketingName": "iPhone 12"}, "deviceProperties": {"developerModeStatus": "enabled", "osVersionNumber": "26.7"},
                "connectionProperties": {"transportType": "wired",
                    "localHostnames": ["private-host"], "tunnelIPAddress": "private-address"}}})
        with patch.object(mobile, "checked_tool", side_effect=capture):
            for stage in ("run", "verify"):
                result = mobile.physical_device(self.directory, "private-selected-iphone", stage)
                self.assertEqual(result["model"], "iPhone 12")
                self.assertEqual(result["transportType"], "wired")
                self.assertNotIn("private-", json.dumps(result))
        self.assertNotEqual(commands[0][1], commands[1][1])
        self.assertEqual(commands[0][0][commands[0][0].index("--device") + 1], commands[1][0][commands[1][0].index("--device") + 1])
        with self.assertRaisesRegex(EntryError, "device_evidence_exists"):
            mobile.physical_device(self.directory, "private-selected-iphone", "run")

    def test_network_missing_or_unknown_transport_is_rejected_and_raw_evidence_preserved(self):
        for index, transport in enumerate(["localNetwork", None, "private-host", {"token": "private-value"}, []]):
            directory = self.directory / str(index)
            directory.mkdir(mode=0o700)
            (directory / "build-output").mkdir(mode=0o700)
            def capture(argv, output, alias, **kwargs):
                path = Path(argv[argv.index("--json-output") + 1])
                mobile.write_new(path, {"result": {"hardwareProperties": {"reality": "physical", "platform": "iOS",
                    "marketingName": "iPhone 12"}, "deviceProperties": {"developerModeStatus": "enabled"},
                    "connectionProperties": {"transportType": transport, "localHostnames": ["private-host"]}}})
            with self.subTest(transport=transport), patch.object(mobile, "checked_tool", side_effect=capture):
                with self.assertRaisesRegex(EntryError, "wired_device_required"):
                    mobile.physical_device(directory, "private-selected-iphone", "run")
                self.assertTrue((directory / "run-device-evidence.private.json").is_file())

    def test_fresh_nonwired_device_blocks_flutter_launch_before_attempt_is_created(self):
        for index, transport in enumerate(["localNetwork", None, "unobserved"]):
            directory = self.directory / str(index)
            directory.mkdir(mode=0o700)
            (directory / "build-output").mkdir(mode=0o700)
            mobile.write_new(directory / "prepare-receipt.json", {"deviceSelector": "private-selected-iphone"})
            mobile.write_new(directory / "build-receipt.json", {})
            mobile.write_new(directory / "preservation-before.private.json", {})
            def capture(argv, output, alias, **kwargs):
                path = Path(argv[argv.index("--json-output") + 1])
                mobile.write_new(path, {"result": {"hardwareProperties": {"reality": "physical", "platform": "iOS",
                    "marketingName": "iPhone 12"}, "deviceProperties": {"developerModeStatus": "enabled"},
                    "connectionProperties": {"transportType": transport}}})
            with self.subTest(transport=transport), patch.object(mobile, "validate_snapshot"), \
                patch.object(mobile, "validate_build", return_value="artifact-pin"), \
                patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
                patch.object(mobile, "checked_tool", side_effect=capture) as query, \
                patch.object(mobile, "bounded_tool") as flutter:
                with self.assertRaisesRegex(EntryError, "wired_device_required"):
                    mobile.run(directory)
                query.assert_called_once()
                self.assertEqual(query.call_args.args[0][:5], ["xcrun", "devicectl", "device", "info", "details"])
                flutter.assert_not_called()
                self.assertFalse((directory / "launch-attempt.json").exists())
                self.assertFalse((directory / "device-run").exists())

    def test_exit_zero_without_driver_evidence_remains_failed_in_run_and_verify(self):
        output = self.directory / "build-output"
        output.mkdir(mode=0o700)
        mobile.write_new(self.directory / "prepare-receipt.json", {"sourceSha256": "source-pin",
            "runNonce": self.nonce, "deviceSelector": "private-selected-iphone", "sourceCopyPath": str(self.directory / "source"),
            "pubspecLockSha256": "pub-pin", "podLockSha256": "pod-pin"})
        mobile.write_new(self.directory / "build-receipt.json", {"artifactPath": str(output / "profile.app")})
        mobile.write_new(self.directory / "preservation-before.private.json", {})
        with patch.object(mobile, "validate_snapshot"), \
            patch.object(mobile, "validate_build", return_value="artifact-pin"), \
            patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
            patch.object(mobile, "physical_device", return_value={"physicalDevice": True, "transportType": "wired"}), \
            patch.object(mobile, "bounded_tool", return_value="ok") as flutter, \
            patch.object(mobile, "PUBLIC", self.directory / "public"), \
            patch.object(mobile, "publish", side_effect=lambda report, *args, **kwargs: report):
            with self.assertRaisesRegex(EntryError, "driver_launch_failed"):
                mobile.run(self.directory)
            flutter.assert_called_once()
            launch = mobile.private_json(self.directory / "launch-receipt.json")
            self.assertEqual((launch["status"], launch["reason"], launch["toolStatus"]), ("FAIL", "DRIVER_FAILED", "ok"))
            report = mobile.verify(self.directory, not_run_reason="DRIVER_FAILED")
            self.assertEqual((report["status"], report["reason"]), ("FAIL", "DRIVER_FAILED"))
            self.assertEqual((report["acceptedCases"], report["acceptedScreenshots"]), (0, 0))

    def test_successful_driver_without_successful_launch_receipt_cannot_publish_pass(self):
        prepared, built, _, _, result_path = self.launch_fixture()
        prepared.update(pubspecLockSha256="pub-lock", podLockSha256="pod-lock")
        mobile.write_new(self.directory / "prepare-receipt.json", prepared)
        mobile.write_new(self.directory / "preservation-before.private.json", {})
        with patch.object(mobile, "PUBLIC", self.directory / "public"), \
            patch.object(mobile, "validate_snapshot"), patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
            patch.object(mobile, "fixtures", return_value=({}, self.cases)), \
            patch.object(mobile, "publish", side_effect=lambda report, *args, **kwargs: report):
            result = mobile.verify(self.directory, result_path)
        self.assertEqual(result["status"], "FAIL")
        self.assertEqual(result["reason"], "VERIFICATION_REJECTED")

    def test_private_source_tree_preserves_symlink_text_without_following(self):
        old = self.directory / "old"
        old.mkdir(mode=0o700)
        mobile.write_new(old / "artifact", b"unchanged")
        (old / "link").symlink_to("artifact")
        hashes = mobile.tree_hashes(old)
        self.assertEqual(hashes["link"], "symlink:artifact")
        self.assertEqual(hashes["artifact"], sha256(b"unchanged").hexdigest())

    def test_preservation_detects_changed_old_signing_output(self):
        old = self.directory / "old"
        old.mkdir(mode=0o700)
        path = old / "artifact"
        mobile.write_new(path, b"old")
        protected = {"files": {str(path): mobile.file_hash(path)}, "signingOutputPath": str(old),
            "signingOutputFiles": mobile.tree_hashes(old)}
        self.assertTrue(mobile.assert_preserved(protected)["unchanged"])
        path.write_bytes(b"new")
        with self.assertRaisesRegex(EntryError, "preservation_changed"):
            mobile.assert_preserved(protected)

    def test_extra_private_tree_hash_detects_mutation_and_prepare_rejects_overlap(self):
        old = self.directory / "protected-private"
        old.mkdir(mode=0o700)
        path = old / "old-report.json"
        mobile.write_new(path, {"private": True})
        protected = {"files": {}, "signingOutputPath": str(old), "signingOutputFiles": mobile.tree_hashes(old),
            "extraTrees": {str(old): mobile.tree_hashes(old)}}
        self.assertTrue(mobile.assert_preserved(protected)["unchanged"])
        mobile.write_new(old / "unapproved-addition", b"changed")
        with self.assertRaisesRegex(EntryError, "preservation_changed"):
            mobile.assert_preserved(protected)
        with patch.object(mobile, "signing_preflight") as preflight, self.assertRaisesRegex(EntryError, "preservation_scope_rejected"):
            mobile.prepare(old / "nested-run", preserve_private=[old])
        preflight.assert_not_called()

    def test_prepare_modifies_only_new_private_snapshot_pubspec_and_assets(self):
        output = self.directory / "new-run"
        source = self.directory / "source.png"
        mobile.write_new(source, b"fixture-byte-stream")
        cases = [{**row, "sha256": mobile.file_hash(source), "sourcePath": str(source)} for row in self.cases]
        original_pubspec = b"name: qa\nflutter:\n  assets:\n    - assets/library/\n"
        sources = {"app/mobile/pubspec.yaml": sha256(original_pubspec).hexdigest()}
        state = {}
        def fake_snapshot(build_output, expected):
            path = build_output / "source/repository/app/mobile"
            path.mkdir(parents=True, mode=0o700)
            (path / "assets").mkdir(mode=0o700)
            (path / "ios").mkdir(mode=0o700)
            mobile.write_new(path / "pubspec.yaml", original_pubspec)
            mobile.write_new(path / "pubspec.lock", b"pinned-pub")
            mobile.write_new(path / "ios/Podfile.lock", b"pinned-pods")
            state["path"] = path
            return path
        settings = {"CODE_SIGN_STYLE": "Manual", "FLUTTER_ROOT": "/offline/flutter", "FLUTTER_BUILD_DIR": "build"}
        with patch.object(mobile, "fixtures", return_value=({}, cases)), \
            patch.object(mobile, "signing_preflight", return_value=({"device": {"selector": "private-selector"}}, source, settings)), \
            patch.object(mobile, "preservation", return_value={"files": {}}), \
            patch.object(mobile, "assert_preserved", return_value={"unchanged": True}), \
            patch.object(mobile, "local_sources", return_value=(sources, "source-pin")), \
            patch.object(mobile, "private_mobile_snapshot", side_effect=fake_snapshot):
            receipt = mobile.prepare(output)
        self.assertEqual(receipt["sourceSha256"], "source-pin")
        self.assertRegex(receipt["runNonce"], r"^[0-9a-f]{64}$")
        copied = state["path"]
        manifest = json.loads((copied / mobile.MANIFEST).read_text())
        self.assertEqual(len(manifest["cases"]), 6)
        self.assertTrue(all("sourcePath" not in row for row in manifest["cases"]))
        self.assertIn("    - assets/local_lossless/\n", (copied / "pubspec.yaml").read_text())
        self.assertEqual((copied / "pubspec.lock").read_bytes(), b"pinned-pub")
        self.assertEqual(source.read_bytes(), b"fixture-byte-stream")
        config = (output / "signing.xcconfig").read_text()
        self.assertIn("IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT = " + str(output / "build-output"), config)
        self.assertIn("FLUTTER_BUILD_DIR = build\n", config)
        self.assertFalse((output / "signing.xcconfig").is_relative_to(output / "build-output"))
        self.assertEqual(stat.S_IMODE(output.stat().st_mode), 0o700)
        self.assertEqual(stat.S_IMODE((output / "signing.xcconfig").stat().st_mode), 0o600)
        with self.assertRaisesRegex(EntryError, "output_not_unique"):
            mobile.prepare(output)


if __name__ == "__main__":
    unittest.main()

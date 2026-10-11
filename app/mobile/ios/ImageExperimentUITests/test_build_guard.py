"""Synthetic build admission matrix; no signing, provisioning or devices are used."""
import importlib.util
import hashlib
import os
import re
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("image_experiment_guard", Path(__file__).parents[1] / "ImageExperiment" / "build_guard.py")
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class BuildGuardTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(dir="/private/tmp")
        self.root = Path(self.temp.name)
        self.source = self.root / "repo" / "app" / "mobile" / "ios"
        (self.source / "Flutter").mkdir(parents=True)
        self.output = self.root / "output"
        self.output.mkdir(mode=0o700)
        self.signing = self.root / "synthetic.xcconfig"
        self.signing.write_text("SYNTHETIC_TEST_INPUT=1\n", encoding="utf-8")
        self.signing.chmod(0o600)
        self.env = {
            "SRCROOT": str(self.source), "TARGET_NAME": "ImageExperiment", "CONFIGURATION": "Profile-imageexperiment",
            "FLUTTER_TARGET": "lib/main_image_experiment.dart", "FLUTTER_BUILD_MODE": "profile",
            "IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT": str(self.output), "IMAGE_EXPERIMENT_SIGNING_CONFIG": str(self.signing),
            "BUILD_DIR": str(self.output / "derived" / "Products"),
            "PROJECT_TEMP_DIR": str(self.output / "derived" / "Intermediates"),
            "FLUTTER_BUILD_DIR": os.path.relpath(self.output / "flutter", self.source.parent),
            "FLUTTER_ROOT": "synthetic-sdk", "CODE_SIGN_STYLE": "Manual", "DEVELOPMENT_TEAM": "SYNTHETIC",
            "CODE_SIGN_IDENTITY": "SYNTHETIC", "PROVISIONING_PROFILE_SPECIFIER": "SYNTHETIC",
            "PRODUCT_BUNDLE_IDENTIFIER": "com.tellpal.tellpalMobilePoc.imageexperiment.profile",
            "PLATFORM_NAME": "iphoneos", "CODE_SIGNING_ALLOWED": "YES",
        }

    def tearDown(self):
        self.temp.cleanup()

    def invoke(self, **changes):
        with patch.dict(os.environ, self.env | changes, clear=True):
            guard.main()

    def test_isolated_profile_and_release_app_and_test_modes(self):
        for mode in ("profile", "release"):
            for target in ("ImageExperiment", "ImageExperimentUITests"):
                self.invoke(TARGET_NAME=target, CONFIGURATION=f"{mode.title()}-imageexperiment", FLUTTER_BUILD_MODE=mode,
                    PRODUCT_BUNDLE_IDENTIFIER=f"com.tellpal.tellpalMobilePoc.imageexperiment.{mode}" +
                        (".uitests" if target.endswith("UITests") else ""))

    def test_ordinary_runner_requires_ordinary_entry_and_generated_settings(self):
        self.invoke(TARGET_NAME="Runner", FLUTTER_TARGET="lib/main.dart")
        with self.assertRaises(ValueError):
            self.invoke(TARGET_NAME="Runner")
        (self.source / "Flutter" / "Generated.xcconfig").write_text("FLUTTER_TARGET=lib/main_image_experiment.dart\n")
        with self.assertRaises(ValueError):
            self.invoke(TARGET_NAME="Runner", FLUTTER_TARGET="lib/main.dart")

    def test_mode_entry_identity_output_and_signing_overrides_refuse(self):
        changes = [
            {"CONFIGURATION": "Debug-imageexperiment"}, {"FLUTTER_TARGET": "lib/main.dart"},
            {"PRODUCT_BUNDLE_IDENTIFIER": "com.tellpal.tellpalMobilePoc"}, {"CODE_SIGN_STYLE": "Automatic"},
            {"DEVELOPMENT_TEAM": ""}, {"PROVISIONING_PROFILE_SPECIFIER": ""}, {"CODE_SIGNING_ALLOWED": "NO"},
            {"PLATFORM_NAME": "iphonesimulator"}, {"BUILD_DIR": str(self.source / "build")},
            {"FLUTTER_BUILD_DIR": "build"},
        ]
        for change in changes:
            with self.subTest(field=next(iter(change))), self.assertRaises(ValueError):
                self.invoke(**change)

    def test_output_symlink_and_unprivate_signing_refuse(self):
        link = self.root / "output-link"
        link.symlink_to(self.output, target_is_directory=True)
        with self.assertRaises(ValueError):
            self.invoke(IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT=str(link))
        self.signing.chmod(0o644)
        with self.assertRaises(ValueError):
            self.invoke()

    def test_native_ledger_whitelists_match_the_dart_recorder(self):
        mobile = Path(__file__).parents[2]
        dart = (mobile / "lib/core/diagnostics/image_experiment_recorder.dart").read_text()
        kotlin = (mobile / "android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentLedger.kt").read_text()
        swift = (mobile / "ios/ImageExperiment/ImageExperimentLedger.swift").read_text()
        def quoted(text):
            return set(re.findall(r"['\"]([^'\"]+)['\"]", text))
        for dart_field, native_field in [("_hashKeys", "hashes"), ("_intKeys", "integers"),
            ("_doubleKeys", "doubles"), ("_boolKeys", "booleans")]:
            dart_values = quoted(re.search(rf"{dart_field}\s*=\s*\{{(.*?)\}}", dart, re.S).group(1))
            kotlin_values = quoted(re.search(rf"{native_field}\s*=\s*setOf\((.*?)\)", kotlin, re.S).group(1))
            swift_values = quoted(re.search(rf"{native_field}: Set<String>\s*=\s*\[(.*?)\]", swift, re.S).group(1))
            self.assertEqual(dart_values, kotlin_values)
            self.assertEqual(dart_values, swift_values)
        dart_strings = quoted(re.search(r"_strings = <String, Set<String>>\{(.*?)\n  \};", dart, re.S).group(1))
        kotlin_strings = quoted(re.search(r"strings = mapOf\((.*?)\)\)\n", kotlin, re.S).group(1))
        swift_strings = quoted(re.search(r"strings: \[String: Set<String>\] = \[(.*?)\]\n\n", swift, re.S).group(1))
        self.assertEqual(dart_strings, kotlin_strings)
        self.assertEqual(dart_strings, swift_strings)

    def test_android_launcher_requires_revocation_and_explicit_success(self):
        mobile = Path(__file__).parents[2]
        sources = mobile / "android/app/src"
        launcher = (sources / "imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt").read_text()
        activity = (sources / "imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentActivity.kt").read_text()
        self.assertRegex(launcher, r"while \(!bootstrap\.isRevoked && !activity\.isFinishing && !activity\.isDestroyed &&\s*SystemClock\.elapsedRealtime\(\) - start < 180_000\)")
        self.assertRegex(launcher, r"require\(bootstrap\.isRevoked && bootstrap\.launcherOutcome\.isSettled\(\)\)\s*result\.putString\(\"reason\", \"NATIVE_LAUNCHER_SETTLED\"\)\s*finish\(Activity.RESULT_OK, result\)")
        self.assertIn('result.putString("reason", "NATIVE_LAUNCHER_REFUSED")', launcher)
        self.assertIn("finish(Activity.RESULT_CANCELED, result)", launcher)
        self.assertIn('arguments?.keys == setOf("result")', activity)
        self.assertLess(activity.index("launcherOutcome.complete(requestedResult, it.isRevoked)"), activity.index("bootstrap?.revoke()"))
        self.assertIn("if (stored) bootstrap?.launcherOutcome?.markLedgerStored()", activity)

    def test_apple_launcher_reports_only_an_explicit_positive_result(self):
        ios = Path(__file__).parents[1]
        app = (ios / "ImageExperiment/AppDelegate.swift").read_text()
        launcher = (ios / "ImageExperimentUITests/ImageExperimentBootstrapTests.swift").read_text()
        self.assertIn('var launcherResult = "refused"', launcher)
        self.assertIn('defer { print("IMAGE_EXPERIMENT_RESULT " + launcherResult) }', launcher)
        self.assertIn('guard closed.exists, closed.value as? String == "settled"', launcher)
        self.assertLess(launcher.index('guard closed.exists, closed.value as? String == "settled"'), launcher.index('launcherResult = "settled"'))
        self.assertIn('Set(arguments.keys) == ["result"]', app)
        self.assertLess(app.index("launcherOutcome.complete(requestedResult, alreadyRevoked: bootstrap.isRevoked)"), app.index("self.bootstrap?.revoke()"))
        self.assertIn('self.bootstrap?.launcherOutcome.isSettled == true ? "settled" : "refused"', app)
        self.assertIn("if stored { self?.bootstrap?.launcherOutcome.markLedgerStored() }", app)

    def test_kotlin_untrusted_json_paths_use_the_depth_scan(self):
        mobile = Path(__file__).parents[2]
        sources = mobile / "android/app/src"
        bootstrap = (sources / "imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentBootstrap.kt").read_text()
        ledger = (sources / "imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentLedger.kt").read_text()
        launcher = (sources / "imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt").read_text()
        self.assertIn("val stack = CharArray(32)", bootstrap)
        self.assertRegex(bootstrap, r"checkStructure\(raw, maximumBytes\)\s*return JSONObject\(raw\)")
        self.assertEqual(re.findall(r"JSONObject\(([^)]*)\)", bootstrap), ["raw", ""])
        self.assertIn("val data = ImageExperimentJson.parseObject(raw, 16_384)", bootstrap)
        self.assertIn("val envelope = ImageExperimentJson.parseObject(raw)", bootstrap)
        self.assertIn('ImageExperimentJson.parseObject(request("POST", "/experiment/v1/bootstrap/challenge"', bootstrap)
        self.assertIn('ImageExperimentJson.parseObject(request("GET", "/experiment/v1/bootstrap/paired?', bootstrap)
        self.assertIn("ImageExperimentJson.parseObject(raw, 262_144)", ledger)
        self.assertIn("ImageExperimentJson.parseObject(metadata, 16_384)", launcher)

    def test_swift_wire_time_checks_use_overflow_safe_bounds(self):
        swift = (Path(__file__).parents[1] / "ImageExperiment/ImageExperimentBootstrap.swift").read_text()
        self.assertIn("later.subtractingReportingOverflow(earlier)", swift)
        self.assertNotRegex(swift, r"(?:expires|deadline)\s*-\s*(?:Self\.now|now|current|issued)")
        self.assertIn("positiveDelta(expires, since: challengeNow, maximum: 30_000)", swift)
        self.assertIn("positiveDelta(deadline, since: now, maximum: 30_000)", swift)
        self.assertIn("positiveDelta(deadline, since: current, maximum: 30_000)", swift)
        self.assertIn("allowsSession(issued: issued, expires: expires, now: now)", swift)

    def test_podfile_lock_matches_the_current_podfile(self):
        ios = Path(__file__).parents[1]
        checksum = re.search(r"^PODFILE CHECKSUM: ([0-9a-f]{40})$", (ios / "Podfile.lock").read_text(), re.M).group(1)
        self.assertEqual(checksum, hashlib.sha1((ios / "Podfile").read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()

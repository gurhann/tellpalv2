"""Synthetic build admission matrix; no signing, provisioning or devices are used."""
import importlib.util
import hashlib
import json
import os
import plistlib
import re
import shutil
import struct
import subprocess
import sys
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location("image_experiment_guard", Path(__file__).parents[1] / "ImageExperiment" / "build_guard.py")
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)
linkage_spec = importlib.util.spec_from_file_location("native_linkage", Path(__file__).with_name("verify_native_linkage.py"))
linkage = importlib.util.module_from_spec(linkage_spec)
linkage_spec.loader.exec_module(linkage)


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

    def test_apple_audit_dispatch_never_enters_flutter_or_automatic_storyboards(self):
        ios = Path(__file__).parents[1]
        app = (ios / "ImageExperiment/AppDelegate.swift").read_text()
        scene = (ios / "ImageExperiment/SceneDelegate.swift").read_text()
        audit = (ios / "ImageExperiment/ImageExperimentAuditAppDelegate.swift").read_text()
        hasher = (ios / "ImageExperiment/ImageExperimentInstallationAudit.swift").read_text()
        launcher = (ios / "ImageExperimentUITests/ImageExperimentBootstrapTests.swift").read_text()
        entry = app.split("@objc class AppDelegate", 1)[0]
        self.assertIn('@main\nenum ImageExperimentMain', entry)
        self.assertIn('environment["IMAGE_EXPERIMENT_INSTALLATION_AUDIT"] != nil', entry)
        self.assertIn('? ImageExperimentAuditAppDelegate.self : AppDelegate.self', entry)
        self.assertIn('UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil, NSStringFromClass(delegate))', entry)
        for source in (audit, hasher):
            self.assertNotRegex(source, r'ImageExperimentBootstrap|GeneratedPluginRegistrant|FlutterMethodChannel|URLSession|FileManager.*Caches')
        self.assertIn('configuration.storyboard = nil', audit)
        self.assertIn('ImageExperimentAuditSceneDelegate: SceneDelegate', audit)
        self.assertIn('class SceneDelegate: UIResponder, UIWindowSceneDelegate, FlutterSceneLifeCycleProvider', scene)
        self.assertLess(scene.index('UIApplication.shared.delegate as? ImageExperimentAuditAppDelegate'), scene.index('let flutter = FlutterSceneDelegate()'))
        self.assertLess(scene.index('return\n    }'), scene.index('UIStoryboard(name: "FlutterMain"'))
        for callback in ('sceneDidDisconnect', 'sceneWillEnterForeground', 'sceneDidBecomeActive',
            'sceneWillResignActive', 'sceneDidEnterBackground', 'stateRestorationActivity'):
            self.assertIn('ordinaryDelegate?.' + callback + '(', scene)
        info = plistlib.loads((ios / 'ImageExperiment/Info.plist').read_bytes())
        self.assertNotIn('UIMainStoryboardFile', info)
        configurations = info['UIApplicationSceneManifest']['UISceneConfigurations']['UIWindowSceneSessionRoleApplication']
        self.assertTrue(all('UISceneStoryboardFile' not in config for config in configurations))
        fixture = launcher.split('func testInstallationAudit()', 1)[0]
        self.assertIn('environment[ImageExperimentAuditRequest.environmentKey] == nil', fixture)
        audit_test = launcher.split('func testInstallationAudit()', 1)[1].split('func testSynthetic', 1)[0]
        self.assertIn('let runner = Bundle.main', audit_test)
        self.assertIn('PlugIns/ImageExperimentUITests.xctest', audit_test)
        self.assertIn('hashBundle(runner.bundleURL', audit_test)
        self.assertIn('brokerMetadataPresent: environment["IMAGE_EXPERIMENT_METADATA"] != nil', audit_test)
        self.assertEqual(audit_test.count('print("IMAGE_EXPERIMENT_INSTALLATION_AUDIT "'), 1)

    def test_apple_audit_sources_are_excluded_from_ordinary_targets(self):
        project = (Path(__file__).parents[1] / 'Runner.xcodeproj/project.pbxproj').read_text()
        def source_names(target):
            block = re.search(rf'[A-F0-9]+ /\* {target} \*/ = \{{\s*isa = PBXNativeTarget;(.*?)\n\t\t\}};', project, re.S).group(1)
            phases = re.search(r'buildPhases = \((.*?)\);', block, re.S).group(1)
            names = []
            for phase in re.findall(r'([A-F0-9]+) /\* Sources \*/', phases):
                source = re.search(rf'{phase} /\* Sources \*/ = \{{(.*?)\n\t\t\}};', project, re.S).group(1)
                names.extend(re.findall(r'/\* ([^*]+) in Sources \*/', source))
            return names
        self.assertEqual(source_names('Runner'), ['AppDelegate.swift', 'GeneratedPluginRegistrant.m', 'SceneDelegate.swift'])
        self.assertEqual(source_names('RunnerTests'), ['RunnerTests.swift'])
        for target in ('ImageExperiment', 'ImageExperimentUITests'):
            self.assertEqual(source_names(target).count('ImageExperimentInstallationAudit.swift'), 1)
        self.assertEqual(source_names('ImageExperiment').count('ImageExperimentAuditAppDelegate.swift'), 1)
        self.assertNotIn('ImageExperimentAuditAppDelegate.swift', source_names('ImageExperimentUITests'))

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_audit_refusal_stages_are_fixed_bounded_and_preserve_host_refusal(self):
        ios = Path(__file__).parents[1]
        source = (ios / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift').read_text()
        declaration = source.split('enum ImageExperimentAuditStage', 1)[1].split('@MainActor', 1)[0]
        main = self.root / 'main.swift'
        main.write_text('import Foundation\nenum ImageExperimentAuditStage' + declaration + '''
for stage in ImageExperimentAuditStage.allCases {
  for line in stage.refusalLines(reported: false) { print(line) }
  precondition(stage.refusalLines(reported: true).isEmpty)
}
for stage in ImageExperimentAuditHashStage.allCases {
  for line in ImageExperimentAuditStage.runnerHash.refusalLines(reported: false, hashStage: stage) { print(line) }
  precondition(ImageExperimentAuditStage.runnerHash.refusalLines(reported: true, hashStage: stage).isEmpty)
  precondition(ImageExperimentAuditStage.appReport.refusalLines(reported: false, hashStage: stage) ==
    ImageExperimentAuditStage.appReport.refusalLines(reported: false))
}
let fixtures = URL(fileURLWithPath: CommandLine.arguments[1])
let good = fixtures.appendingPathComponent("good")
var observed: [ImageExperimentAuditHashStage] = []
let watched = try ImageExperimentInstallationAudit.hashBundle(good, checkDeadline: {}, observeStage: { observed.append($0) })
let normal = try ImageExperimentInstallationAudit.hashBundle(good, checkDeadline: {})
precondition(watched.sha256 == normal.sha256 && watched.fileCount == normal.fileCount && watched.byteCount == normal.byteCount)
for stage in [ImageExperimentAuditHashStage.rootStat, .rootOpen, .rootIdentity, .directoryRead, .entryName,
  .entryStat, .entryType, .directoryChildOpen, .fileRead, .fileFinalIdentity, .reopenFile, .reopenIdentity, .finalRoot, .deadline] {
  precondition(observed.contains(stage))
}
func mustFail(_ name: String, _ expected: ImageExperimentAuditHashStage,
  deadline: () throws -> Void = {}, mutation: (ImageExperimentAuditHashStage) -> Void = { _ in }) throws {
  var failed: ImageExperimentAuditHashStage?
  do {
    _ = try ImageExperimentInstallationAudit.hashBundle(fixtures.appendingPathComponent(name),
      checkDeadline: deadline, observeStage: { failed = $0; mutation($0) })
  } catch {
    precondition(failed == expected)
    return
  }
  throw ImageExperimentAuditRefusal.refused
}
try mustFail("missing", .rootStat)
try mustFail("plain-file", .rootStat)
try mustFail("unreadable", .rootOpen)
try mustFail("unsafe", .entryType)
try mustFail("oversized", .fileLimits)
try mustFail("good", .deadline, deadline: { throw ImageExperimentAuditRefusal.refused })
var mutated = false
try mustFail("race", .reopenIdentity, mutation: { stage in
  if stage == .reopenIdentity && !mutated {
    mutated = true
    let file = try! FileHandle(forWritingTo: fixtures.appendingPathComponent("race/leaf"))
    file.write(Data("changed!".utf8))
    try! file.close()
  }
})
var replaced = false
try mustFail("replacement", .reopenFile, mutation: { stage in
  if stage == .reopenFile && !replaced {
    replaced = true
    let file = fixtures.appendingPathComponent("replacement/leaf")
    try! FileManager.default.removeItem(at: file)
    try! FileManager.default.createSymbolicLink(at: file, withDestinationURL: fixtures.appendingPathComponent("plain-file"))
  }
})
try mustFail("final-root", .finalRoot, mutation: { stage in
  if stage == .finalRoot { try! FileManager.default.setAttributes([.posixPermissions: 0o755], ofItemAtPath: fixtures.appendingPathComponent("final-root").path) }
})
try mustFail("root-identity", .rootMode, mutation: { stage in
  if stage == .rootIdentity { try! FileManager.default.setAttributes([.posixPermissions: 0o755], ofItemAtPath: fixtures.appendingPathComponent("root-identity").path) }
})
try mustFail("good", .rootFstat, mutation: { stage in
  if stage == .rootFstat { for descriptor: Int32 in 3..<256 { close(descriptor) } }
})
''')
        binary = self.root / 'audit-diagnostic'
        compiled = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(ios / 'ImageExperiment/ImageExperimentInstallationAudit.swift'), str(main), '-o', str(binary)],
            capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, 'Native diagnostic compilation failed')
        fixtures = self.root / 'diagnostic-fixtures'
        fixtures.mkdir(mode=0o700)
        for name in ('good/child', 'unreadable', 'unsafe', 'oversized', 'race', 'replacement', 'final-root', 'root-identity'):
            (fixtures / name).mkdir(parents=True, mode=0o700)
        for name in ('good/child/leaf', 'plain-file', 'race/leaf', 'replacement/leaf', 'final-root/leaf', 'root-identity/leaf'):
            (fixtures / name).write_bytes(b'snapshot')
        (fixtures / 'unsafe/leaf').symlink_to(fixtures / 'plain-file')
        with (fixtures / 'oversized/leaf').open('wb') as handle:
            handle.truncate(256 * 1024 * 1024 + 1)
        (fixtures / 'unreadable').chmod(0)
        try:
            executed = subprocess.run([str(binary), str(fixtures)], capture_output=True, timeout=10)
        finally:
            (fixtures / 'unreadable').chmod(0o700)
        self.assertEqual(executed.returncode, 0, 'Native diagnostic execution failed')
        cases = ['REQUEST', 'RUNNER_ID', 'PLUGIN_ID', 'PLUGIN_PATH_LEXICAL', 'PLUGIN_PATH_CANONICAL', 'RUNNER_HASH',
            'APP_LAUNCH', 'APP_REPORT', 'REPORT_MERGE', 'FINAL_DEADLINE']
        cases += ['HASH_' + case for case in ['ROOT_STAT', 'ROOT_OPEN', 'ROOT_IDENTITY', 'ROOT_FSTAT', 'ROOT_DEVICE',
            'ROOT_INODE', 'ROOT_MODE', 'ROOT_SIZE', 'ROOT_MTIME_SECONDS', 'ROOT_MTIME_NANOSECONDS',
            'ROOT_CTIME_SECONDS', 'ROOT_CTIME_NANOSECONDS', 'DIRECTORY_LIMITS',
            'DIRECTORY_STAT', 'DIRECTORY_DUPLICATE', 'DIRECTORY_OPEN', 'DIRECTORY_READ', 'ENTRY_NAME', 'ENTRY_LIMITS',
            'ENTRY_STAT', 'ENTRY_TYPE', 'DIRECTORY_CHILD_OPEN', 'DIRECTORY_IDENTITY', 'FILE_LIMITS', 'FILE_OPEN',
            'FILE_IDENTITY', 'FILE_READ', 'FILE_LENGTH', 'FILE_FINAL_IDENTITY', 'DIRECTORY_FINAL_IDENTITY',
            'REOPEN_PARENT', 'REOPEN_PATH', 'REOPEN_FILE', 'REOPEN_IDENTITY', 'FINAL_ROOT', 'DEADLINE']]
        expected = ''.join('IMAGE_EXPERIMENT_INSTALLATION_AUDIT_STAGE ' + stage + '\n' +
            'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED\n' for stage in cases).encode('ascii')
        self.assertEqual(executed.stdout, expected)
        self.assertEqual(executed.stderr, b'')
        self.assertTrue(all(len(line) <= 80 for line in executed.stdout.splitlines()))
        audit_test = source.split('func testInstallationAudit()', 1)[1].split('func testSynthetic', 1)[0]
        self.assertIn('defer { for line in stage.refusalLines(reported: reported, hashStage: hashStage) { print(line) } }', audit_test)
        self.assertIn('observeStage: { hashStage = $0 }', audit_test)
        checkpoints = [
            ('var stage = ImageExperimentAuditStage.request', 'let request = try ImageExperimentAuditRequest'),
            ('stage = .runnerId', 'guard runner.bundleIdentifier == request.runnerId'),
            ('stage = .pluginId', 'guard plugin.bundleIdentifier == request.appId + ".uitests"'),
            ('stage = .pluginPathLexical', 'guard plugin.bundleURL.standardizedFileURL == expectedPlugin.standardizedFileURL'),
            ('stage = .pluginPathCanonical', 'guard plugin.bundleURL.resolvingSymlinksInPath() == canonicalPlugin.standardizedFileURL'),
            ('stage = .runnerHash', 'let digest = try ImageExperimentInstallationAudit.hashBundle'),
            ('stage = .appLaunch', 'let app = XCUIApplication'),
            ('stage = .appReport', 'let element = app.otherElements'),
            ('stage = .reportMerge', 'let result = try ImageExperimentInstallationAudit.report'),
            ('stage = .finalDeadline', 'try request.checkDeadline()'),
        ]
        for index, (stage, operation) in enumerate(checkpoints):
            self.assertEqual(audit_test.count(stage), 1)
            self.assertLess(audit_test.index(stage), audit_test.index(operation))
            if index + 1 < len(checkpoints):
                self.assertLess(audit_test.index(operation), audit_test.index(checkpoints[index + 1][0]))
        self.assertIn('plugin.bundleURL.resolvingSymlinksInPath() == canonicalPlugin.standardizedFileURL', audit_test)
        self.assertIn('throw NSError(domain: "INSTALLATION_AUDIT_REFUSED", code: 1)', audit_test)
        self.assertLess(audit_test.index('print("IMAGE_EXPERIMENT_INSTALLATION_AUDIT " + result)'),
            audit_test.index('reported = true'))
        host_tools = Path(__file__).parents[4] / 'tools/image-optimization'
        integration = subprocess.run([str(host_tools / '.venv/bin/python'), '-B', '-c', '''
import sys
from native_entry_audit import InstallationReport
from native_entry_config import EntryError
lines = sys.stdin.buffer.read().splitlines()
for index in range(0, len(lines), 2):
    observer = InstallationReport({}, {})
    observer(lines[index])
    assert observer.report is None
    try:
        observer(lines[index + 1])
    except EntryError as error:
        assert str(error) == 'audit_native_refused'
    else:
        raise AssertionError('Host accepted native refusal')
print('DIAGNOSTIC_HOST_REFUSAL_PASS')
'''], input=executed.stdout, cwd=host_tools, capture_output=True, timeout=10)
        self.assertEqual(integration.returncode, 0, 'Host diagnostic refusal changed')
        self.assertEqual(integration.stdout, b'DIAGNOSTIC_HOST_REFUSAL_PASS\n')

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_root_identity_stages_preserve_every_predicate_and_short_circuit(self):
        swift = Path(__file__).parents[1] / 'ImageExperiment/ImageExperimentInstallationAudit.swift'
        main = self.root / 'main.swift'
        main.write_text(swift.read_text() + r'''
extension ImageExperimentInstallationAudit {
  static func verifyRootIdentityPredicates() {
    let stages: [ImageExperimentAuditHashStage] = [.rootDevice, .rootInode, .rootMode, .rootSize,
      .rootMtimeSeconds, .rootMtimeNanoseconds, .rootCtimeSeconds, .rootCtimeNanoseconds]
    let mutations: [(inout stat) -> Void] = [
      { $0.st_dev += 1 }, { $0.st_ino += 1 }, { $0.st_mode += 1 }, { $0.st_size += 1 },
      { $0.st_mtimespec.tv_sec += 1 }, { $0.st_mtimespec.tv_nsec += 1 },
      { $0.st_ctimespec.tv_sec += 1 }, { $0.st_ctimespec.tv_nsec += 1 }
    ]
    let original = stat()
    for mask in 0..<256 {
      var changed = original
      var firstMismatch: Int?
      for index in mutations.indices where mask & (1 << index) != 0 {
        mutations[index](&changed)
        if firstMismatch == nil { firstMismatch = index }
      }
      let expected = original.st_dev == changed.st_dev && original.st_ino == changed.st_ino &&
        original.st_mode == changed.st_mode && original.st_size == changed.st_size &&
        original.st_mtimespec.tv_sec == changed.st_mtimespec.tv_sec &&
        original.st_mtimespec.tv_nsec == changed.st_mtimespec.tv_nsec &&
        original.st_ctimespec.tv_sec == changed.st_ctimespec.tv_sec &&
        original.st_ctimespec.tv_nsec == changed.st_ctimespec.tv_nsec
      var observed: [ImageExperimentAuditHashStage] = []
      let actual = unchanged(original, changed, observeRootStage: { observed.append($0) })
      precondition(actual == expected && unchanged(original, changed) == expected)
      precondition(observed == Array(stages.prefix(firstMismatch.map { $0 + 1 } ?? stages.count)))
    }
  }
}
ImageExperimentInstallationAudit.verifyRootIdentityPredicates()
print("ROOT_IDENTITY_PREDICATES_PASS")
''')
        binary = self.root / 'root-identity-predicates'
        compiled = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, 'Root identity predicate compilation failed')
        executed = subprocess.run([str(binary)], capture_output=True, timeout=10)
        self.assertEqual(executed.returncode, 0, 'Root identity predicate execution failed')
        self.assertEqual(executed.stdout, b'ROOT_IDENTITY_PREDICATES_PASS\n')
        self.assertEqual(executed.stderr, b'')

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_hasher_preserves_both_root_api_signatures_and_refuses_every_view_mutation(self):
        swift = Path(__file__).parents[1] / 'ImageExperiment/ImageExperimentInstallationAudit.swift'
        main = self.root / 'main.swift'
        main.write_text(swift.read_text() + r'''
let root = URL(fileURLWithPath: CommandLine.arguments[1])
var rawRoot = stat()
var rootDescriptor: Int32?
var pathOffset = 0
var descriptorOffset = 0
var pathMutation = 0
var descriptorMutation = 0
var crossIdentityMutation = 0
var finalObservation = false
let mutations: [(inout stat) -> Void] = [
  { $0.st_dev += 1 }, { $0.st_ino += 1 }, { $0.st_mode += 1 }, { $0.st_size += 1 },
  { $0.st_mtimespec.tv_sec += 1 }, { $0.st_mtimespec.tv_nsec += 1 },
  { $0.st_ctimespec.tv_sec += 1 }, { $0.st_ctimespec.tv_nsec += 1 }
]
func mutate(_ metadata: inout stat, _ mask: Int) {
  for index in mutations.indices where mask & (1 << index) != 0 { mutations[index](&metadata) }
}
// Test-only interposition uses real Darwin metadata and changes only the root API views.
func lstat(_ path: String, _ metadata: UnsafeMutablePointer<stat>) -> Int32 {
  let result = Darwin.lstat(path, metadata)
  if result == 0 && path == root.path {
    metadata.pointee.st_mtimespec.tv_sec += pathOffset
    if finalObservation { mutate(&metadata.pointee, pathMutation) }
  }
  return result
}
func fstat(_ descriptor: Int32, _ metadata: UnsafeMutablePointer<stat>) -> Int32 {
  let result = Darwin.fstat(descriptor, metadata)
  if result == 0 && metadata.pointee.st_dev == rawRoot.st_dev && metadata.pointee.st_ino == rawRoot.st_ino {
    rootDescriptor = descriptor
    metadata.pointee.st_mtimespec.tv_sec += descriptorOffset
    mutate(&metadata.pointee, crossIdentityMutation)
    if finalObservation { mutate(&metadata.pointee, descriptorMutation) }
  }
  return result
}
func prepare() {
  precondition(Darwin.lstat(root.path, &rawRoot) == 0)
  rootDescriptor = nil
  pathMutation = 0
  descriptorMutation = 0
  crossIdentityMutation = 0
  finalObservation = false
}
func mustRefuse(_ expected: ImageExperimentAuditHashStage,
  mutation: () throws -> Void = {}) throws {
  var last: ImageExperimentAuditHashStage?
  do {
    _ = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {}, observeStage: {
      last = $0
      if $0 == .finalRoot { finalObservation = true; try! mutation() }
    })
  } catch {
    precondition(last == expected)
    return
  }
  throw ImageExperimentAuditRefusal.refused
}
prepare()
let ordinary = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {})
pathOffset = 7
descriptorOffset = 13
prepare()
let separateViews = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {})
precondition(ordinary.sha256 == separateViews.sha256 && ordinary.fileCount == separateViews.fileCount &&
  ordinary.byteCount == separateViews.byteCount)
for side in 0..<2 {
  for mask in 1..<256 {
    prepare()
    if side == 0 { pathMutation = mask } else { descriptorMutation = mask }
    try mustRefuse(.finalRoot)
  }
}
let crossStages: [ImageExperimentAuditHashStage] = [.rootDevice, .rootInode, .rootMode, .rootSize]
for mask in 1..<16 {
  prepare()
  crossIdentityMutation = mask
  let first = (0..<4).first { mask & (1 << $0) != 0 }!
  try mustRefuse(crossStages[first])
}
prepare()
try mustRefuse(.finalRoot, mutation: { precondition(Darwin.fchmod(rootDescriptor!, 0o755) == 0) })
precondition(Darwin.chmod(root.path, 0o700) == 0)
prepare()
try mustRefuse(.finalRoot, mutation: {
  var current = stat()
  precondition(Darwin.fstat(rootDescriptor!, &current) == 0)
  let times = [timeval(tv_sec: current.st_atimespec.tv_sec, tv_usec: 0),
    timeval(tv_sec: current.st_mtimespec.tv_sec + 1, tv_usec: 0)]
  precondition(times.withUnsafeBufferPointer { Darwin.futimes(rootDescriptor!, $0.baseAddress!) } == 0)
})
prepare()
try mustRefuse(.finalRoot, mutation: {
  var current = stat()
  precondition(Darwin.lstat(root.path, &current) == 0)
  let times = [timeval(tv_sec: current.st_atimespec.tv_sec, tv_usec: 0),
    timeval(tv_sec: current.st_mtimespec.tv_sec + 1, tv_usec: 0)]
  precondition(times.withUnsafeBufferPointer { Darwin.utimes(root.path, $0.baseAddress!) } == 0)
})
prepare()
try mustRefuse(.finalRoot, mutation: {
  let previous = root.appendingPathExtension("previous")
  try FileManager.default.moveItem(at: root, to: previous)
  try FileManager.default.createDirectory(at: root, withIntermediateDirectories: false)
  try Data("same bytes".utf8).write(to: root.appendingPathComponent("leaf"))
})
prepare()
try mustRefuse(.finalRoot, mutation: {
  let previous = root.appendingPathExtension("symlink-previous")
  try FileManager.default.moveItem(at: root, to: previous)
  try FileManager.default.createSymbolicLink(at: root, withDestinationURL: previous)
})
try FileManager.default.removeItem(at: root)
try FileManager.default.createDirectory(at: root, withIntermediateDirectories: false)
try Data("same bytes".utf8).write(to: root.appendingPathComponent("leaf"))
let replacement = root.appendingPathExtension("fd-replacement")
try FileManager.default.createDirectory(at: replacement, withIntermediateDirectories: false)
prepare()
try mustRefuse(.finalRoot, mutation: {
  let retained = rootDescriptor!
  precondition(Darwin.close(retained) == 0)
  let changed = Darwin.open(replacement.path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC)
  precondition(changed >= 0)
  if changed != retained { precondition(Darwin.dup2(changed, retained) == retained); Darwin.close(changed) }
})
print("ROOT_API_SIGNATURES_PASS")
print(separateViews.sha256 + " " + String(separateViews.fileCount) + " " + String(separateViews.byteCount))
''')
        binary = self.root / 'root-api-signatures'
        compiled = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, 'Root API signature compilation failed')
        root = self.root / 'root-api-fixture'
        root.mkdir(mode=0o700)
        files = {'.hidden': b'hidden', 'leaf': b'same bytes'}
        for name, value in files.items():
            (root / name).write_bytes(value)
        executed = subprocess.run([str(binary), str(root)], capture_output=True, timeout=10)
        self.assertEqual(executed.returncode, 0, 'Root API signature execution failed')
        digest = hashlib.sha256()
        for name in sorted(files, key=lambda value: value.encode('utf-8')):
            relative = name.encode('utf-8')
            digest.update(len(relative).to_bytes(4, 'big') + relative + hashlib.sha256(files[name]).digest())
        self.assertEqual(executed.stdout, ('ROOT_API_SIGNATURES_PASS\n' + digest.hexdigest() +
            f' {len(files)} {sum(map(len, files.values()))}\n').encode('ascii'))
        self.assertEqual(executed.stderr, b'')
        source = swift.read_text()
        self.assertIn('sameRootIdentity(initial, opened, observeRootStage: observeStage)', source)
        self.assertIn('fstat(root, &finalRoot) == 0, unchanged(opened, finalRoot)', source)
        self.assertIn('lstat(url.path, &finalPath) == 0, unchanged(initial, finalPath), sameRootIdentity(finalPath, finalRoot)', source)
        self.assertNotIn('unchanged(opened, finalPath)', source)

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_root_view_probe_observes_stability_mutation_and_bounds_without_changing_refusal(self):
        ios = Path(__file__).parents[1]
        swift = ios / 'ImageExperiment/ImageExperimentInstallationAudit.swift'
        main = self.root / 'main.swift'
        main.write_text(r'''
import Darwin
import Foundation
let mutations: [(inout stat) -> Void] = [
  { $0.st_dev += 1 }, { $0.st_ino += 1 }, { $0.st_mode += 1 }, { $0.st_size += 1 },
  { $0.st_mtimespec.tv_sec += 1 }, { $0.st_mtimespec.tv_nsec += 1 },
  { $0.st_ctimespec.tv_sec += 1 }, { $0.st_ctimespec.tv_nsec += 1 }
]
var original = stat()
original.st_mode = S_IFDIR | 0o700
precondition(ImageExperimentAuditRootViews.classify([(original, original), (original, original),
  (original, original)]) == .same)
for side in 0..<2 {
  for mask in 1..<256 {
    var changed = original
    for index in mutations.indices where mask & (1 << index) != 0 { mutations[index](&changed) }
    let second = side == 0 ? (changed, original) : (original, changed)
    precondition(ImageExperimentAuditRootViews.classify([(original, original), second,
      (original, original)]) == .mismatch)
  }
}
for index in 0..<4 {
  var changed = original
  mutations[index](&changed)
  precondition(ImageExperimentAuditRootViews.classify(Array(repeating: (original, changed), count: 3)) == .mismatch)
}
for mask in 0..<16 {
  var differentView = original
  for index in 0..<4 where mask & (1 << index) != 0 { mutations[index + 4](&differentView) }
  precondition(ImageExperimentAuditRootViews.classify(Array(repeating: (original, differentView), count: 3)) ==
    (mask == 0 ? .same : .stable))
}
for count in [0, 1, 2, 4] {
  precondition(ImageExperimentAuditRootViews.classify(Array(repeating: (original, original), count: count)) == .unavailable)
}
let expectedRootStages: Set<ImageExperimentAuditHashStage> = [.rootIdentity, .rootFstat, .rootDevice,
  .rootInode, .rootMode, .rootSize, .rootMtimeSeconds, .rootMtimeNanoseconds, .rootCtimeSeconds, .rootCtimeNanoseconds]
for stage in ImageExperimentAuditHashStage.allCases {
  precondition(ImageExperimentAuditRootViews.isRootIdentityStage(stage) == expectedRootStages.contains(stage))
}
let root = URL(fileURLWithPath: CommandLine.arguments[1])
func descriptors() -> Set<Int32> { Set((Int32(0)..<256).filter { fcntl($0, F_GETFD) >= 0 }) }
precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {}) == .same)
let before = descriptors()
for _ in 0..<10 { precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {}) == .same) }
precondition(descriptors() == before)
var checks = 0
precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {
  checks += 1
  if checks == 4 { precondition(chmod(root.path, 0o755) == 0) }
}) == .mismatch)
precondition(descriptors() == before && chmod(root.path, 0o700) == 0)
checks = 0
precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {
  checks += 1
  if checks == 4 {
    try FileManager.default.moveItem(at: root, to: root.appendingPathExtension("previous"))
    try FileManager.default.createDirectory(at: root, withIntermediateDirectories: false,
      attributes: [.posixPermissions: 0o700])
  }
}) == .mismatch)
precondition(descriptors() == before)
checks = 0
precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {
  checks += 1
  if checks == 4 { throw ImageExperimentAuditRefusal.refused }
}) == .unavailable)
precondition(checks == 4 && descriptors() == before)
var ticks = 0
precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {}, monotonicNow: {
  ticks += 1
  return ticks < 5 ? 10 : 1_000_000_010
}) == .unavailable)
precondition(ticks == 5 && descriptors() == before)
precondition(ImageExperimentAuditRootViews.probe(root, checkDeadline: {}, monotonicNow: { UInt64.max }) == .unavailable)
precondition(ImageExperimentAuditRootViews.probe(root.appendingPathComponent("missing"), checkDeadline: {}) == .unavailable)
let link = root.appendingPathExtension("link")
try FileManager.default.createSymbolicLink(at: link, withDestinationURL: root)
precondition(ImageExperimentAuditRootViews.probe(link, checkDeadline: {}) == .unavailable)
let plain = root.appendingPathComponent("plain")
try Data("plain".utf8).write(to: plain)
precondition(ImageExperimentAuditRootViews.probe(plain, checkDeadline: {}) == .unavailable)
precondition(descriptors() == before)
for result in ImageExperimentAuditRootViewResult.allCases { print(result.line) }
''')
        binary = self.root / 'root-view-probe'
        compiled = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(swift), str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, 'Root view probe compilation failed')
        root = self.root / 'root-view-fixture'
        root.mkdir(mode=0o700)
        executed = subprocess.run([str(binary), str(root)], capture_output=True, timeout=10)
        self.assertEqual(executed.returncode, 0, 'Root view probe execution failed')
        expected = ''.join('IMAGE_EXPERIMENT_INSTALLATION_AUDIT_ROOT_VIEWS ' + code + '\n'
            for code in ['SAME', 'STABLE', 'MISMATCH', 'UNAVAILABLE']).encode('ascii')
        self.assertEqual(executed.stdout, expected)
        self.assertEqual(executed.stderr, b'')
        self.assertTrue(all(len(line) <= 80 for line in executed.stdout.splitlines()))
        source = (ios / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift').read_text()
        audit = source.split('func testInstallationAudit()', 1)[1].split('func testSynthetic', 1)[0]
        caught = audit.split('} catch {', 1)[1]
        self.assertIn('if stage == .runnerHash, let hashStage, ImageExperimentAuditRootViews.isRootIdentityStage(hashStage),', caught)
        self.assertIn('let rootProbe { print(rootProbe().line) }', caught)
        self.assertNotRegex(caught, r'(?:hashStage|stage|reported)\s*=(?!=)|hashBundle\(|app\.launch\(')
        self.assertLess(caught.index('print(rootProbe().line)'), caught.index('throw NSError(domain: "INSTALLATION_AUDIT_REFUSED", code: 1)'))
        self.assertIn('rootProbe = { ImageExperimentAuditRootViews.probe(runner.bundleURL, checkDeadline: request.checkDeadline) }', audit)
        for filename in ['AppDelegate.swift', 'ImageExperimentAuditAppDelegate.swift', 'SceneDelegate.swift']:
            self.assertNotIn('ImageExperimentAuditRootViews', (ios / 'ImageExperiment' / filename).read_text())
        utility = swift.read_text().split('enum ImageExperimentAuditRootViews', 1)[1]
        self.assertNotRegex(utility, r'hashBundle|appReport|report\(|print\(|Thread\.sleep|Flutter|URLSession')
        host_tools = Path(__file__).parents[4] / 'tools/image-optimization'
        integration = subprocess.run([str(host_tools / '.venv/bin/python'), '-B', '-c', '''
import sys
from native_entry_audit import InstallationReport
from native_entry_config import EntryError
for line in sys.stdin.buffer.read().splitlines():
    observer = InstallationReport({}, {})
    observer(line)
    observer(b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_STAGE HASH_ROOT_MTIME_SECONDS')
    assert observer.report is None
    try:
        observer(b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED')
    except EntryError as error:
        assert str(error) == 'audit_native_refused'
    else:
        raise AssertionError('Host accepted root view diagnostic')
print('ROOT_VIEW_HOST_REFUSAL_PASS')
'''], input=executed.stdout, cwd=host_tools, capture_output=True, timeout=10)
        self.assertEqual(integration.returncode, 0, 'Root view diagnostic altered host refusal')
        self.assertEqual(integration.stdout, b'ROOT_VIEW_HOST_REFUSAL_PASS\n')

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_entry_probe_preserves_refusal_metadata_bounds_and_borrowed_descriptors(self):
        ios = Path(__file__).parents[1]
        swift = ios / 'ImageExperiment/ImageExperimentInstallationAudit.swift'
        main = self.root / 'main.swift'
        main.write_text(swift.read_text() + r'''
let root = URL(fileURLWithPath: CommandLine.arguments[1])
let parent = Darwin.open(root.path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC)
let child = Darwin.openat(parent, "child", O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC)
precondition(parent >= 0 && child >= 0)
var childSignature = stat()
precondition(Darwin.fstat(child, &childSignature) == 0)
var pathReads = 0
var descriptorReads = 0
var offsets = (0, 0)
var mutationSide = -1
var mutationRead = 0
var mutationMask = 0
var failPath = false
var failDescriptor = false
let mutations: [(inout stat) -> Void] = [
  { $0.st_dev += 1 }, { $0.st_ino += 1 }, { $0.st_mode += 1 }, { $0.st_size += 1 },
  { $0.st_mtimespec.tv_sec += 1 }, { $0.st_mtimespec.tv_nsec += 1 },
  { $0.st_ctimespec.tv_sec += 1 }, { $0.st_ctimespec.tv_nsec += 1 }
]
func mutate(_ metadata: inout stat, side: Int, read: Int) {
  if side == mutationSide && read == mutationRead {
    for index in mutations.indices where mutationMask & (1 << index) != 0 { mutations[index](&metadata) }
  }
}
// Only test observations are interposed; the filesystem and descriptors use real Darwin calls.
func fstatat(_ descriptor: Int32, _ name: String, _ metadata: UnsafeMutablePointer<stat>, _ flags: Int32) -> Int32 {
  precondition(flags == AT_SYMLINK_NOFOLLOW)
  let result = Darwin.fstatat(descriptor, name, metadata, flags)
  if result == 0 && name == "child" {
    pathReads += 1
    if failPath { return -1 }
    metadata.pointee.st_mtimespec.tv_sec += offsets.0
    mutate(&metadata.pointee, side: 0, read: pathReads)
  }
  return result
}
func fstat(_ descriptor: Int32, _ metadata: UnsafeMutablePointer<stat>) -> Int32 {
  let result = Darwin.fstat(descriptor, metadata)
  if result == 0 && metadata.pointee.st_dev == childSignature.st_dev && metadata.pointee.st_ino == childSignature.st_ino {
    descriptorReads += 1
    if failDescriptor { return -1 }
    metadata.pointee.st_mtimespec.tv_sec += offsets.1
    mutate(&metadata.pointee, side: 1, read: descriptorReads)
  }
  return result
}
func reset() {
  pathReads = 0; descriptorReads = 0; offsets = (0, 0)
  mutationSide = -1; mutationRead = 0; mutationMask = 0
  failPath = false; failDescriptor = false
}
func descriptors() -> Set<Int32> { Set((Int32(0)..<256).filter { fcntl($0, F_GETFD) >= 0 }) }
let before = descriptors()
func probe(_ name: String = "child", _ descriptor: Int32 = child,
  check: () throws -> Void = {}, clock: () -> UInt64 = { DispatchTime.now().uptimeNanoseconds }) -> ImageExperimentAuditEntryViewResult {
  let result = ImageExperimentAuditEntryViews.probe(parent: parent, name: name, child: descriptor,
    checkDeadline: check, monotonicNow: clock)
  precondition(descriptors() == before)
  var metadata = stat()
  precondition(Darwin.fstat(parent, &metadata) == 0 && Darwin.fstat(child, &metadata) == 0)
  return result
}
reset()
precondition(probe() == .same && pathReads == 3 && descriptorReads == 3)
reset(); offsets = (7, 13)
precondition(probe() == .stable && pathReads == 3 && descriptorReads == 3)
// Every nonempty combination of all eight fields must fail each API's later signature.
for side in 0..<2 {
  for read in 2...3 {
    for mask in 1..<256 {
      reset(); mutationSide = side; mutationRead = read; mutationMask = mask
      precondition(probe() == .mismatch)
    }
  }
}
var original = childSignature
for mask in 1..<16 {
  var changed = original
  for index in 0..<4 where mask & (1 << index) != 0 { mutations[index](&changed) }
  precondition(ImageExperimentAuditEntryViews.classify(Array(repeating: (original, changed), count: 3)) == .mismatch)
}
for mask in 0..<16 {
  var changed = original
  for index in 0..<4 where mask & (1 << index) != 0 { mutations[index + 4](&changed) }
  precondition(ImageExperimentAuditEntryViews.classify(Array(repeating: (original, changed), count: 3)) ==
    (mask == 0 ? .same : .stable))
}
for count in [0, 1, 2, 4] {
  precondition(ImageExperimentAuditEntryViews.classify(Array(repeating: (original, original), count: count)) == .unavailable)
}
reset()
for name in ["", ".", "..", "a/b", "child/", "child\0hidden", String(repeating: "a", count: 256),
  String(repeating: "é", count: 128), "missing", "plain", "link"] {
  precondition(probe(name) == .unavailable)
}
precondition(probe("child", -1) == .unavailable)
precondition(probe("child", parent) == .mismatch)
let closed = Darwin.dup(child)
precondition(closed >= 0 && Darwin.close(closed) == 0)
precondition(probe("child", closed) == .unavailable)
precondition(ImageExperimentAuditEntryViews.probe(parent: -1, name: "child", child: child,
  checkDeadline: {}) == .unavailable && descriptors() == before)
reset(); failPath = true; precondition(probe() == .unavailable)
reset(); failDescriptor = true; precondition(probe() == .unavailable)
reset()
var checks = 0
precondition(probe(check: { checks += 1; if checks == 4 { throw ImageExperimentAuditRefusal.refused } }) == .unavailable)
precondition(checks == 4 && pathReads == 1 && descriptorReads == 1)
reset()
var ticks = 0
precondition(probe(clock: { ticks += 1; return ticks < 5 ? 10 : 1_000_000_010 }) == .unavailable)
precondition(ticks == 5 && pathReads == 1 && descriptorReads == 1)
precondition(probe(clock: { UInt64.max }) == .unavailable)
reset(); checks = 0
precondition(probe(check: { checks += 1; if checks == 4 { precondition(Darwin.fchmod(child, 0o755) == 0) } }) == .mismatch)
precondition(Darwin.fchmod(child, 0o700) == 0)
reset(); checks = 0
precondition(probe(check: {
  checks += 1
  if checks == 4 {
    var current = stat(); precondition(Darwin.fstat(child, &current) == 0)
    let times = [timeval(tv_sec: current.st_atimespec.tv_sec, tv_usec: 0),
      timeval(tv_sec: current.st_mtimespec.tv_sec + 1, tv_usec: 0)]
    precondition(times.withUnsafeBufferPointer { Darwin.futimes(child, $0.baseAddress!) } == 0)
  }
}) == .mismatch)
reset(); checks = 0
precondition(probe(check: {
  checks += 1
  if checks == 4 {
    try FileManager.default.moveItem(at: root.appendingPathComponent("child"), to: root.appendingPathComponent("previous"))
    try FileManager.default.createDirectory(at: root.appendingPathComponent("child"), withIntermediateDirectories: false)
  }
}) == .mismatch)
try FileManager.default.removeItem(at: root.appendingPathComponent("child"))
try FileManager.default.moveItem(at: root.appendingPathComponent("previous"), to: root.appendingPathComponent("child"))
reset(); checks = 0
precondition(probe(check: {
  checks += 1
  if checks == 4 {
    try FileManager.default.moveItem(at: root.appendingPathComponent("child"), to: root.appendingPathComponent("previous"))
    try FileManager.default.createSymbolicLink(at: root.appendingPathComponent("child"), withDestinationURL: root.appendingPathComponent("previous"))
  }
}) == .unavailable)
try FileManager.default.removeItem(at: root.appendingPathComponent("child"))
try FileManager.default.moveItem(at: root.appendingPathComponent("previous"), to: root.appendingPathComponent("child"))
try FileManager.default.removeItem(at: root.appendingPathComponent("plain"))
try FileManager.default.removeItem(at: root.appendingPathComponent("link"))
// A real successful bundle scan stays silent; a stable split still refuses at the original guard.
reset()
var diagnostics: [ImageExperimentAuditEntryViewResult] = []
_ = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {}, observeEntryViews: { diagnostics.append($0) })
precondition(diagnostics.isEmpty)
reset(); offsets = (7, 13); mutationSide = 1; mutationRead = 1; mutationMask = 8
var stage: ImageExperimentAuditHashStage?
var refused = false
var afterDiagnostic = false
let readsBefore = descriptors()
do {
  _ = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {}, observeStage: {
    precondition(!afterDiagnostic); stage = $0
  }, observeEntryViews: {
    precondition(stage == .directoryIdentity); afterDiagnostic = true; print($0.line)
  })
} catch { refused = true }
precondition(refused && stage == .directoryIdentity && descriptors() == readsBefore)
print("IMAGE_EXPERIMENT_INSTALLATION_AUDIT_STAGE HASH_DIRECTORY_IDENTITY")
print("IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED")
precondition(Darwin.close(child) == 0 && Darwin.close(parent) == 0)
for result in ImageExperimentAuditEntryViewResult.allCases { print(result.line) }
''')
        binary = self.root / 'entry-view-probe'
        compiled = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, compiled.stderr.decode())
        root = self.root / 'entry-view-fixture'
        (root / 'child').mkdir(parents=True, mode=0o700)
        (root / 'child' / 'leaf').write_bytes(b'full bytes')
        (root / 'plain').write_bytes(b'plain')
        (root / 'link').symlink_to(root / 'child')
        executed = subprocess.run([str(binary), str(root)], capture_output=True, timeout=15)
        self.assertEqual(executed.returncode, 0, executed.stderr.decode())
        expected = (b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_ENTRY_VIEWS STABLE\n'
            b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_STAGE HASH_DIRECTORY_IDENTITY\n'
            b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED\n' +
            ''.join('IMAGE_EXPERIMENT_INSTALLATION_AUDIT_ENTRY_VIEWS ' + code + '\n'
                for code in ['SAME', 'STABLE', 'MISMATCH', 'UNAVAILABLE']).encode('ascii'))
        self.assertEqual(executed.stdout, expected)
        self.assertEqual(executed.stderr, b'')
        host_tools = Path(__file__).parents[4] / 'tools/image-optimization'
        integration = subprocess.run([str(host_tools / '.venv/bin/python'), '-B', '-c', '''
import sys
from native_entry_audit import InstallationReport
from native_entry_config import EntryError
for code in ['SAME', 'STABLE', 'MISMATCH', 'UNAVAILABLE']:
    observer = InstallationReport({}, {})
    observer(('IMAGE_EXPERIMENT_INSTALLATION_AUDIT_ENTRY_VIEWS ' + code).encode('ascii'))
    observer(b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_STAGE HASH_DIRECTORY_IDENTITY')
    assert observer.report is None
    try:
        observer(b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED')
    except EntryError as error:
        assert str(error) == 'audit_native_refused'
    else:
        raise AssertionError('Host accepted entry view diagnostic')
print('ENTRY_VIEW_HOST_REFUSAL_PASS')
'''], cwd=host_tools, capture_output=True, timeout=10)
        self.assertEqual(integration.returncode, 0, 'Entry diagnostic altered host refusal')
        self.assertEqual(integration.stdout, b'ENTRY_VIEW_HOST_REFUSAL_PASS\n')
        utility = swift.read_text().split('enum ImageExperimentAuditEntryViews', 1)[1].split('enum ImageExperimentAuditRootViewResult', 1)[0]
        self.assertNotRegex(utility, r'\b(?:open|openat|close|dup|lstat|hashBundle|print)\(|Thread\.sleep|Flutter|URLSession')
        launcher = (ios / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift').read_text()
        self.assertIn('observeEntryViews: { print($0.line) }', launcher)

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_directory_consistency_preserves_both_api_signatures_and_full_digest(self):
        ios = Path(__file__).parents[1]
        swift = ios / 'ImageExperiment/ImageExperimentInstallationAudit.swift'
        main = self.root / 'main.swift'
        main.write_text(swift.read_text() + r'''
let root = URL(fileURLWithPath: CommandLine.arguments[1])
let target = root.appendingPathComponent("a-early/nested")
let later = root.appendingPathComponent("z-later")
var baseline = stat()
var laterBaseline = stat()
var phase = ImageExperimentAuditHashStage.rootStat
var stage: ImageExperimentAuditHashStage?
var timestampOffsets = false
var mutationSide = -1
var mutationMask = 0
var mutationPhase = 0
var mutationActive = false
var targetVisited = false
var laterReading = false
var realAction: ((Int32) throws -> Void)?
var realActionDone = false
let mutations: [(inout stat) -> Void] = [
  { $0.st_dev += 1 }, { $0.st_ino += 1 }, { $0.st_mode += 1 }, { $0.st_size += 1 },
  { $0.st_mtimespec.tv_sec += 1 }, { $0.st_mtimespec.tv_nsec += 1 },
  { $0.st_ctimespec.tv_sec += 1 }, { $0.st_ctimespec.tv_nsec += 1 }
]
func observe(_ metadata: inout stat, side: Int) {
  if metadata.st_mode & S_IFMT == S_IFDIR && timestampOffsets {
    // Every descendant directory uses stable distinct API timestamps; root is untouched.
    if metadata.st_dev != rootBaseline.st_dev || metadata.st_ino != rootBaseline.st_ino {
      metadata.st_mtimespec.tv_sec += side == 0 ? 7 : 13
      metadata.st_ctimespec.tv_nsec += side == 0 ? 17 : 23
    }
  }
  if metadata.st_dev == baseline.st_dev && metadata.st_ino == baseline.st_ino {
    if phase == .directoryFinalIdentity { targetVisited = true }
    let shouldMutate = mutationPhase == 0 ? phase == .directoryFinalIdentity :
      (mutationPhase == 1 ? phase == .reopenIdentity :
        (mutationPhase == 3 ? phase == (side == 0 ? .entryStat : .directoryIdentity) : mutationActive))
    if side == mutationSide && shouldMutate {
      for index in mutations.indices where mutationMask & (1 << index) != 0 { mutations[index](&metadata) }
    }
  }
}
var rootBaseline = stat()
func fstatat(_ descriptor: Int32, _ name: String, _ metadata: UnsafeMutablePointer<stat>, _ flags: Int32) -> Int32 {
  let result = Darwin.fstatat(descriptor, name, metadata, flags)
  if result == 0 { observe(&metadata.pointee, side: 0) }
  return result
}
func fstat(_ descriptor: Int32, _ metadata: UnsafeMutablePointer<stat>) -> Int32 {
  var result = Darwin.fstat(descriptor, metadata)
  if result == 0 && metadata.pointee.st_dev == baseline.st_dev && metadata.pointee.st_ino == baseline.st_ino,
    phase == .directoryFinalIdentity && !realActionDone, let action = realAction {
    realActionDone = true
    try! action(descriptor)
    result = Darwin.fstat(descriptor, metadata)
  }
  if result == 0 {
    if metadata.pointee.st_mode & S_IFMT == S_IFREG {
      laterReading = targetVisited
    }
    observe(&metadata.pointee, side: 1)
  }
  return result
}
func reset() {
  precondition(Darwin.lstat(root.path, &rootBaseline) == 0 && Darwin.lstat(target.path, &baseline) == 0 &&
    Darwin.lstat(later.path, &laterBaseline) == 0)
  stage = nil; phase = .rootStat; timestampOffsets = true
  mutationSide = -1; mutationMask = 0; mutationPhase = 0; mutationActive = false
  targetVisited = false; laterReading = false; realAction = nil; realActionDone = false
}
func scan() throws -> ImageExperimentBundleDigest {
  try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {}, observeStage: {
    stage = $0; phase = $0
    if $0 == .fileRead && laterReading {
      mutationActive = true
    }
  })
}
func descriptors() -> Set<Int32> { Set((Int32(0)..<256).filter { fcntl($0, F_GETFD) >= 0 }) }
func refuse(_ expected: ImageExperimentAuditHashStage) throws {
  let before = descriptors()
  do { _ = try scan() } catch {
    precondition(stage == expected && descriptors() == before)
    return
  }
  throw ImageExperimentAuditRefusal.refused
}
reset(); timestampOffsets = false
let ordinary = try scan()
reset()
let distinct = try scan()
precondition(ordinary.sha256 == distinct.sha256 && ordinary.fileCount == distinct.fileCount &&
  ordinary.byteCount == distinct.byteCount)
for point in 0..<3 {
  for side in 0..<2 {
    for mask in 1..<256 {
      reset(); mutationPhase = point; mutationSide = side; mutationMask = mask
      try refuse(point == 0 ? .directoryFinalIdentity : .reopenIdentity)
    }
  }
}
for side in 0..<2 {
  for mask in 1..<16 {
    reset(); mutationPhase = 3; mutationSide = side; mutationMask = mask
    try refuse(.directoryIdentity)
  }
}
// A component used only as an intermediate parent retains its path baseline too.
reset(); mutationPhase = 1; mutationSide = 0; mutationMask = 16
try refuse(.reopenIdentity)
reset()
realAction = { descriptor in precondition(Darwin.fchmod(descriptor, 0o755) == 0) }
try refuse(.directoryFinalIdentity)
precondition(Darwin.chmod(target.path, 0o700) == 0)
reset()
realAction = { descriptor in
  var current = stat(); precondition(Darwin.fstat(descriptor, &current) == 0)
  let times = [timeval(tv_sec: current.st_atimespec.tv_sec, tv_usec: 0),
    timeval(tv_sec: current.st_mtimespec.tv_sec + 1, tv_usec: 0)]
  precondition(times.withUnsafeBufferPointer { Darwin.futimes(descriptor, $0.baseAddress!) } == 0)
}
try refuse(.directoryFinalIdentity)
reset()
realAction = { _ in
  var current = stat(); precondition(Darwin.lstat(target.path, &current) == 0)
  let times = [timeval(tv_sec: current.st_atimespec.tv_sec, tv_usec: 0),
    timeval(tv_sec: current.st_mtimespec.tv_sec + 1, tv_usec: 0)]
  precondition(times.withUnsafeBufferPointer { Darwin.utimes(target.path, $0.baseAddress!) } == 0)
}
try refuse(.directoryFinalIdentity)
let backup = target.appendingPathExtension("previous")
reset()
realAction = { _ in
  try FileManager.default.moveItem(at: target, to: backup)
  try FileManager.default.createDirectory(at: target, withIntermediateDirectories: false)
}
try refuse(.directoryFinalIdentity)
try FileManager.default.removeItem(at: target)
try FileManager.default.moveItem(at: backup, to: target)
reset()
realAction = { _ in
  try FileManager.default.moveItem(at: target, to: backup)
  try FileManager.default.createSymbolicLink(at: target, withDestinationURL: backup)
}
try refuse(.directoryFinalIdentity)
try FileManager.default.removeItem(at: target)
try FileManager.default.moveItem(at: backup, to: target)
let replacement = root.appendingPathComponent("replacement")
try FileManager.default.createDirectory(at: replacement, withIntermediateDirectories: false)
reset()
realAction = { descriptor in
  let changed = Darwin.open(replacement.path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC)
  precondition(changed >= 0 && Darwin.dup2(changed, descriptor) == descriptor)
  if changed != descriptor { precondition(Darwin.close(changed) == 0) }
}
try refuse(.directoryFinalIdentity)
print("DIRECTORY_API_SIGNATURES_PASS")
print(distinct.sha256 + " " + String(distinct.fileCount) + " " + String(distinct.byteCount))
''')
        binary = self.root / 'directory-api-signatures'
        compiled = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, compiled.stderr.decode())
        root = self.root / 'directory-api-fixture'
        files = {'a-early/nested/.hidden': b'hidden', 'a-early/nested/leaf': b'full bytes',
            'z-later': b'later bytes', '_CodeSignature/CodeResources': b'signature',
            'embedded.mobileprovision': b'profile', 'symbols.dSYM/data': b'symbols'}
        for name, data in files.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        (root / 'a-early/nested').chmod(0o700)
        executed = subprocess.run([str(binary), str(root)], capture_output=True, timeout=30)
        self.assertEqual(executed.returncode, 0, executed.stderr.decode())
        digest = hashlib.sha256()
        for name in sorted(files, key=lambda value: value.encode('utf-8')):
            relative = name.encode('utf-8')
            digest.update(len(relative).to_bytes(4, 'big') + relative + hashlib.sha256(files[name]).digest())
        self.assertEqual(executed.stdout, ('DIRECTORY_API_SIGNATURES_PASS\n' + digest.hexdigest() +
            f' {len(files)} {sum(map(len, files.values()))}\n').encode('ascii'))
        self.assertEqual(executed.stderr, b'')

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_fixture_diagnostic_is_fixed_private_first_failure_only_and_preserves_native_refusal(self):
        mobile = Path(__file__).parents[2]
        ios = mobile / 'ios'
        swift = ios / 'ImageExperiment/ImageExperimentBootstrap.swift'
        main = self.root / 'main.swift'
        main.write_text(r'''
import Foundation
@main struct FixtureDiagnosticHarness {
  @MainActor static func main() throws {
    let unsafe: [Any?] = [nil, true, 1, ["token": "private-marker"], "private-marker",
      "DART_MODE\n", "DART_MODE\0private-marker", "dart_mode", String(repeating: "x", count: 8193),
      "NATIVE_ENVELOPE", "DIAGNOSTIC_UNAVAILABLE"]
    for value in unsafe {
      let diagnostic = ImageExperimentFixtureDiagnostic()
      precondition(!diagnostic.recordDart(value) && diagnostic.stage == nil)
    }
    for stage in ImageExperimentFixtureStage.allCases {
      precondition(stage.rawValue.utf8.count <= 48 && stage.line.utf8.count <= 80)
      let diagnostic = ImageExperimentFixtureDiagnostic()
      precondition(diagnostic.recordDart(stage.rawValue) == stage.isDart)
      precondition(diagnostic.stage == (stage.isDart ? stage : nil))
      print(stage.line)
    }
    let diagnostic = ImageExperimentFixtureDiagnostic()
    let outcome = ImageExperimentLauncherOutcome()
    diagnostic.record(.nativeEnvelope)
    precondition(diagnostic.recordDart("DART_MODE"))
    precondition(diagnostic.stage == .nativeEnvelope && !outcome.isSettled)
    var observed: [ImageExperimentFixtureStage] = []
    do {
      _ = try ImageExperimentBootstrap(encodedMetadata: Data("private-marker".utf8).base64EncodedString(),
        applicationId: "private-marker", observeRefusal: { observed.append($0) })
      throw ImageExperimentRefusal.refused
    } catch { precondition(observed == [.nativeMetadata]) }
    let appId = "com.tellpal.tellpalMobilePoc.imageexperiment.profile"
    let fakeCertificate = Data([1, 2, 3])
    let metadata: [String: Any] = ["schemaVersion": 1, "scope": "image-optimization-native-fixture-r1",
      "batchId": "synthetic", "sessionId": "synthetic", "caseId": "synthetic",
      "launchNonce": String(repeating: "a", count: 64), "deadline": ImageExperimentBootstrap.now + 30_000,
      "expectedUid": "jLRypTmoBrhhX21HsDJojurcKTC3", "expectedAppId": appId,
      "firebaseProjectId": "synthetic-project", "backendEnvironment": "test",
      "brokerOrigin": "https://localhost:8443", "brokerCertificateDerBase64": fakeCertificate.base64EncodedString(),
      "brokerCertificateSha256": ImageExperimentBootstrap.sha256(fakeCertificate),
      "manifestSha256": String(repeating: "b", count: 64), "publicManifestSha256": String(repeating: "c", count: 64)]
    observed = []
    do {
      _ = try ImageExperimentBootstrap(encodedMetadata: JSONSerialization.data(withJSONObject: metadata).base64EncodedString(),
        applicationId: appId, observeRefusal: { observed.append($0) })
      throw ImageExperimentRefusal.refused
    } catch { precondition(observed == [.nativeTransport]) }
    precondition(!outcome.isSettled)
  }
}
''')
        binary = self.root / 'fixture-diagnostic'
        compiled = subprocess.run(['xcrun', 'swiftc', '-parse-as-library', '-module-cache-path', str(self.root / 'module-cache'),
            str(swift), str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, compiled.stderr.decode())
        executed = subprocess.run([str(binary)], capture_output=True, timeout=10)
        self.assertEqual(executed.returncode, 0, executed.stderr.decode())
        enum = swift.read_text().split('enum ImageExperimentFixtureStage', 1)[1].split('// Retain only', 1)[0]
        codes = re.findall(r'case \w+ = "([A-Z_]+)"', enum)
        self.assertEqual(executed.stdout, ''.join('IMAGE_EXPERIMENT_FIXTURE_STAGE ' + code + '\n' for code in codes).encode())
        self.assertEqual(executed.stderr, b'')
        dart = (mobile / 'lib/app/image_experiment_configuration.dart').read_text()
        dart_codes = set(re.findall(r"\w+\('(DART_[A-Z_]+)'\)", dart))
        self.assertEqual(dart_codes, {code for code in codes if code.startswith('DART_')})
        app = (ios / 'ImageExperiment/AppDelegate.swift').read_text()
        self.assertIn('self.fixtureDiagnostic.recordDart(call.arguments)', app)
        publish = app.split('private func publishFixtureDiagnostic', 1)[1].split('func didInitialize', 1)[0]
        self.assertIn('ImageExperimentAuditRequest.environmentKey] == nil', publish)
        self.assertIn('ImageExperimentAuditRequest.appIds.contains(Bundle.main.bundleIdentifier', publish)
        self.assertNotRegex(publish, r'print\(|metadata|envelope|idToken|URLSession|revoke\(|complete\(')
        launcher = (ios / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift').read_text()
        fixture = launcher.split('func testPairedPublicKeyLaunch()', 1)[1].split('func testInstallationAudit()', 1)[0]
        self.assertIn('if launcherResult == "refused"', fixture)
        self.assertLess(fixture.index('print((stage ?? .diagnosticUnavailable).line)'), fixture.index('app.terminate()'))
        audit = launcher.split('func testInstallationAudit()', 1)[1].split('func testSynthetic', 1)[0]
        self.assertNotIn('FixtureDiagnostic', audit)
        self.assertNotIn('FixtureStage', audit)
        for filename in ['ImageExperimentAuditAppDelegate.swift', 'ImageExperimentInstallationAudit.swift']:
            self.assertNotRegex((ios / 'ImageExperiment' / filename).read_text(), r'FixtureDiagnostic|FixtureStage|reportDiagnostic')
        native = swift.read_text().split('func takeSession()', 1)[1].split('func revoke()', 1)[0]
        ordered = ['refusalStage = .nativeProbe', '"/experiment/v1/probe"', 'refusalStage = .nativeBackendProbe',
            '"/experiment/v1/backend-probe"', 'refusalStage = .nativePairing', 'try await awaitPairing()',
            'refusalStage = .nativeChallenge', '"/experiment/v1/bootstrap/challenge"', 'refusalStage = .nativeProof',
            'try Self.signedPayload', 'refusalStage = .nativeClaim', '"/experiment/v1/bootstrap/claim"',
            'refusalStage = .nativeEnvelope', 'try validateEnvelope(bytes)', 'refusalStage = .nativeHandoff',
            'return ["envelope": envelope, "metadata": metadata]']
        indices = [native.index(value) for value in ordered]
        self.assertEqual(indices, sorted(indices))
        self.assertIn('observeRefusal(refusalStage)\n      revoke()\n      throw ImageExperimentRefusal.refused', native)
        entry = (mobile / 'lib/main_image_experiment.dart').read_text()
        self.assertIn('if (kDebugMode || kIsWeb) throw', entry)
        self.assertIn('configuration.mode != (kProfileMode ? \'profile\' : \'release\')', entry)
        self.assertIn('stage = ImageExperimentFixtureStage.dartComposition;', entry)
        self.assertIn('bootstrap.reportRefusal(stage);\n    await bootstrap.revoke();', entry)

    @unittest.skipUnless(shutil.which('xcrun') and shutil.which('openssl'), 'Apple Swift/OpenSSL is unavailable')
    def test_native_envelope_substages_preserve_original_guard_semantics_for_real_swift(self):
        ios = Path(__file__).parents[1]
        swift = ios / 'ImageExperiment/ImageExperimentBootstrap.swift'
        key, certificate, der = (self.root / name for name in ['synthetic.key', 'synthetic.pem', 'synthetic.der'])
        for command in ([ 'openssl', 'req', '-x509', '-newkey', 'ec', '-pkeyopt', 'ec_paramgen_curve:P-256',
            '-nodes', '-subj', '/CN=localhost', '-days', '1', '-keyout', str(key), '-out', str(certificate)],
            ['openssl', 'x509', '-in', str(certificate), '-outform', 'DER', '-out', str(der)]):
            generated = subprocess.run(command, capture_output=True, timeout=10)
            self.assertEqual(generated.returncode, 0, 'Synthetic certificate generation failed')
        key.chmod(0o600)
        main = self.root / 'main.swift'
        main.write_text(swift.read_text() + r'''
extension ImageExperimentBootstrap {
  // The original joined guard is retained verbatim as an independent test reference.
  func originalEnvelopeAcceptance(_ bytes: Data) -> Bool {
    do {
      let now = Self.now
      guard bytes.count <= 65_536,
        let envelope = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
        Self.integer(envelope["schemaVersion"]) == 1,
        let generation = Self.integer(envelope["generation"]), generation >= 0,
        let issued = Self.integer(envelope["issuedAt"]), let expires = Self.integer(envelope["expiresAt"]),
        ImageExperimentTimeBounds.allowsSession(issued: issued, expires: expires, now: now),
        let token = envelope["idToken"] as? String, (1...8192).contains(token.utf8.count),
        let publicManifest = envelope["publicManifestBase64"] as? String,
        let manifestBytes = Data(base64Encoded: publicManifest),
        Self.sha256(manifestBytes) == string("publicManifestSha256") else { return false }
      for field in ["scope", "batchId", "sessionId", "expectedUid", "backendEnvironment", "brokerOrigin",
        "brokerCertificateSha256", "manifestSha256", "publicManifestSha256"] {
        guard envelope[field] as? String == string(field) else { return false }
      }
      return true
    } catch { return false }
  }
  func envelopeStageForTest(_ bytes: Data) -> ImageExperimentFixtureStage? {
    do { try validateEnvelope(bytes); return nil } catch { return refusalStage }
  }
}
@main struct EnvelopeDiagnosticHarness {
  @MainActor static func main() throws {
    let appId = "com.tellpal.tellpalMobilePoc.imageexperiment.profile"
    let certificate = try Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[1]))
    let manifest = Data("synthetic public bytes".utf8)
    let metadata: [String: Any] = ["schemaVersion": 1, "scope": "image-optimization-native-fixture-r1",
      "batchId": "synthetic", "sessionId": "synthetic", "caseId": "synthetic",
      "launchNonce": String(repeating: "a", count: 64), "deadline": ImageExperimentBootstrap.now + 30_000,
      "expectedUid": "jLRypTmoBrhhX21HsDJojurcKTC3", "expectedAppId": appId,
      "firebaseProjectId": "synthetic-project", "backendEnvironment": "test",
      "brokerOrigin": "https://localhost:8443", "brokerCertificateDerBase64": certificate.base64EncodedString(),
      "brokerCertificateSha256": ImageExperimentBootstrap.sha256(certificate),
      "manifestSha256": String(repeating: "b", count: 64), "publicManifestSha256": ImageExperimentBootstrap.sha256(manifest)]
    var observed: [ImageExperimentFixtureStage] = []
    let bootstrap = try ImageExperimentBootstrap(encodedMetadata: JSONSerialization.data(withJSONObject: metadata).base64EncodedString(),
      applicationId: appId, observeRefusal: { observed.append($0) })
    defer { bootstrap.revoke() }
    let now = ImageExperimentBootstrap.now
    var envelope: [String: Any] = metadata
    envelope["generation"] = 0
    envelope["issuedAt"] = now - 1_000
    envelope["expiresAt"] = now + 60_000
    envelope["idToken"] = "private-marker"
    envelope["publicManifestBase64"] = manifest.base64EncodedString()
    func bytes(_ values: [String: Any]) throws -> Data { try JSONSerialization.data(withJSONObject: values) }
    func check(_ input: Data, expected: ImageExperimentFixtureStage?) {
      let original = bootstrap.originalEnvelopeAcceptance(input)
      let stage = bootstrap.envelopeStageForTest(input)
      precondition(original == (stage == nil) && stage == expected)
    }
    try check(bytes(envelope), expected: nil)
    let cases: [(String, Any, ImageExperimentFixtureStage)] = [
      ("schemaVersion", true, .nativeEnvelopeFields), ("schemaVersion", 2, .nativeEnvelopeFields),
      ("generation", true, .nativeEnvelopeFields), ("generation", -1, .nativeEnvelopeFields),
      ("issuedAt", true, .nativeEnvelopeTimeFields), ("expiresAt", "private-marker", .nativeEnvelopeTimeFields),
      ("issuedAt", now + 60_000, .nativeEnvelopeIssuedFuture),
      ("expiresAt", now - 1_000, .nativeEnvelopeExpired),
      ("expiresAt", now + 200_000, .nativeEnvelopeTimeBounds),
      ("issuedAt", now - 200_000, .nativeEnvelopeTimeBounds),
      ("idToken", NSNull(), .nativeEnvelopeToken), ("idToken", "", .nativeEnvelopeToken),
      ("idToken", String(repeating: "x", count: 8193), .nativeEnvelopeToken),
      ("idToken", String(repeating: "é", count: 4097), .nativeEnvelopeToken),
      ("publicManifestBase64", "!private-marker", .nativeEnvelopeManifest),
      ("publicManifestBase64", Data("changed".utf8).base64EncodedString(), .nativeEnvelopeManifest)
    ]
    for (field, value, stage) in cases {
      var changed = envelope; changed[field] = value
      try check(bytes(changed), expected: stage)
    }
    for field in ["scope", "batchId", "sessionId", "expectedUid", "backendEnvironment", "brokerOrigin",
      "brokerCertificateSha256", "manifestSha256", "publicManifestSha256"] {
      var changed = envelope; changed[field] = "private-marker"
      try check(bytes(changed), expected: .nativeEnvelopeBinding)
    }
    check(Data(repeating: 120, count: 65_537), expected: .nativeEnvelopeFields)
    check(Data("private-marker".utf8), expected: .nativeEnvelopeFields)
    let times: [Int64] = [.min, .min + 1, -1, 0, now - 200_000, now - 1_000,
      now + 1_000, now + 200_000, .max]
    for issued in times {
      for expires in times {
        var changed = envelope; changed["issuedAt"] = issued; changed["expiresAt"] = expires
        let input = try bytes(changed)
        precondition(bootstrap.originalEnvelopeAcceptance(input) == (bootstrap.envelopeStageForTest(input) == nil))
      }
    }
    precondition(observed.isEmpty && !bootstrap.launcherOutcome.isSettled)
    let stages: [ImageExperimentFixtureStage] = [.nativeEnvelopeFields, .nativeEnvelopeTimeFields,
      .nativeEnvelopeIssuedFuture, .nativeEnvelopeExpired, .nativeEnvelopeTimeBounds,
      .nativeEnvelopeToken, .nativeEnvelopeManifest, .nativeEnvelopeBinding]
    for stage in stages { print(stage.line) }
  }
}
''')
        binary = self.root / 'envelope-diagnostic'
        compiled = subprocess.run(['xcrun', 'swiftc', '-parse-as-library', '-module-cache-path', str(self.root / 'module-cache'),
            str(main), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compiled.returncode, 0, compiled.stderr.decode())
        executed = subprocess.run([str(binary), str(der)], capture_output=True, timeout=10)
        self.assertEqual(executed.returncode, 0, executed.stderr.decode())
        self.assertEqual(executed.stdout, ''.join('IMAGE_EXPERIMENT_FIXTURE_STAGE ' + code + '\n' for code in [
            'NATIVE_ENVELOPE_FIELDS', 'NATIVE_ENVELOPE_TIME_FIELDS', 'NATIVE_ENVELOPE_ISSUED_FUTURE',
            'NATIVE_ENVELOPE_EXPIRED', 'NATIVE_ENVELOPE_TIME_BOUNDS', 'NATIVE_ENVELOPE_TOKEN',
            'NATIVE_ENVELOPE_MANIFEST', 'NATIVE_ENVELOPE_BINDING']).encode())
        self.assertEqual(executed.stderr, b'')

    def test_restored_legacy_storyboard_is_plain_and_flutter_resource_is_experiment_only(self):
        ios = Path(__file__).parents[1]
        ordinary = (ios / 'Runner/Base.lproj/Main.storyboard').read_bytes()
        self.assertEqual(hashlib.sha256(ordinary).hexdigest(),
            'fd367b6c43006d61b730f674970dd48b9f662c8f2264e52ffc1558102a2c9f2d')
        plain = ET.parse(ios / 'ImageExperiment/Base.lproj/Main.storyboard').getroot()
        self.assertEqual(len(plain.findall('.//viewController')), 1)
        self.assertTrue(all('customClass' not in node.attrib and 'customModule' not in node.attrib for node in plain.iter()))
        flutter = ET.parse(ios / 'ImageExperiment/Base.lproj/FlutterMain.storyboard').getroot()
        self.assertEqual(flutter.find('.//viewController').get('customClass'), 'FlutterViewController')
        project = (ios / 'Runner.xcodeproj/project.pbxproj').read_text()
        self.assertRegex(project, r'12A7902460314C292A853FC3 /\* Main.storyboard in Resources \*/ = \{isa = PBXBuildFile; fileRef = B2DA0F1AB37EC0196887FD16')
        self.assertRegex(project, r'97C146FC1CF9000F007C117D /\* Main.storyboard in Resources \*/ = \{isa = PBXBuildFile; fileRef = 97C146FA1CF9000F007C117D')
        self.assertRegex(project, r'B2DA0F1AB37EC0196887FD16 /\* Main.storyboard \*/ = \{\s*isa = PBXVariantGroup;\s*children = \(\s*B2DA0F1AB37EC0196887FD15 /\* Base \*/')
        self.assertRegex(project, r'B2DA0F1AB37EC0196887FD18 /\* FlutterMain.storyboard \*/ = \{\s*isa = PBXVariantGroup;\s*children = \(\s*B2DA0F1AB37EC0196887FD17 /\* Base \*/')
        experiment_resources = re.search(r'23059CDACCC5B655BC5363E9 /\* Resources \*/ = \{(.*?)\n\t\t\};', project, re.S).group(1)
        ordinary_resources = re.search(r'97C146EC1CF9000F007C117D /\* Resources \*/ = \{(.*?)\n\t\t\};', project, re.S).group(1)
        self.assertIn('B2DA0F1AB37EC0196887FD19 /* FlutterMain.storyboard in Resources */', experiment_resources)
        self.assertIn('12A7902460314C292A853FC3 /* Main.storyboard in Resources */', experiment_resources)
        self.assertNotIn('FlutterMain.storyboard', ordinary_resources)
        self.assertIn('97C146FC1CF9000F007C117D /* Main.storyboard in Resources */', ordinary_resources)

    @unittest.skipUnless(shutil.which('xcrun'), 'Apple Swift compiler is unavailable')
    def test_native_swift_audit_hash_matches_full_utf8_bytes_and_refuses_unsafe_files(self):
        swift = Path(__file__).parents[1] / 'ImageExperiment/ImageExperimentInstallationAudit.swift'
        harness = self.root / 'main.swift'
        harness.write_text(r'''
import Foundation
import Darwin
func mustRefuse(_ body: () throws -> Void) throws {
  do { try body() } catch { return }
  throw ImageExperimentAuditRefusal.refused
}
do {
  let app = ImageExperimentAuditRequest.appIds[0]
  let nonce = String(repeating: "a", count: 64)
  let request = try ImageExperimentAuditJSON.encode(["schemaVersion": 1, "scope": ImageExperimentAuditRequest.scope,
    "expectedAppId": app, "nonce": nonce, "deadline": 31_000])
  _ = try ImageExperimentAuditRequest(encoded: request, brokerMetadataPresent: false, applicationId: app, now: 1_000)
  try mustRefuse { _ = try ImageExperimentAuditRequest(encoded: request, brokerMetadataPresent: true, now: 1_000) }
  try mustRefuse { _ = try ImageExperimentAuditRequest(encoded: request, brokerMetadataPresent: false, now: 999) }
  try mustRefuse { _ = try ImageExperimentAuditRequest(encoded: request, brokerMetadataPresent: false, now: Int64.min) }
  for raw in [#"{"schemaVersion":1,"schemaVersion":1}"#, #"{"deadline":true}"#, #"{"deadline":01}"#,
    #"{"deadline":1} trailing"#, #"{"deadline":1,}"#, #"{"nonce":{}}"#] {
    try mustRefuse { _ = try ImageExperimentAuditJSON.parse(Data(raw.utf8)) }
  }
  let root = URL(fileURLWithPath: CommandLine.arguments[1])
  let mode = CommandLine.arguments.count > 2 ? CommandLine.arguments[2] : ""
  if mode == "race" {
    guard let directory = opendir(root.path) else { throw ImageExperimentAuditRefusal.refused }
    var names: [String] = []
    while let entry = readdir(directory) {
      names.append(withUnsafePointer(to: &entry.pointee.d_name) {
        $0.withMemoryRebound(to: CChar.self, capacity: Int(entry.pointee.d_namlen) + 1) { String(cString: $0) }
      })
    }
    closedir(directory)
    let positions = names.indices.filter { names[$0] != "." && names[$0] != ".." }
    guard positions.count == 2 else { throw ImageExperimentAuditRefusal.refused }
    let early = root.appendingPathComponent(names[positions[0]])
    let later = root.appendingPathComponent(names[positions[1]])
    try Data("original".utf8).write(to: early)
    try Data(repeating: 120, count: 3 * 1024 * 1024).write(to: later)
    var calls = 0
    var mutated = false
    try mustRefuse {
      _ = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {
        calls += 1
        // The first file's EOF is past; this callback precedes the later file's second chunk.
        if calls == positions[1] + 6 { try Data("mutation".utf8).write(to: early); mutated = true }
      })
    }
    guard mutated else { throw ImageExperimentAuditRefusal.refused }
    print("EARLY_FILE_RACE_REFUSED")
  } else {
    let digest = try ImageExperimentInstallationAudit.hashBundle(root,
      checkDeadline: { if mode == "deadline" { throw ImageExperimentAuditRefusal.refused } })
    if mode == "report" {
      let now = Int64(Date().timeIntervalSince1970 * 1000)
      let encodedRequest = try ImageExperimentAuditJSON.encode(["schemaVersion": 1, "scope": ImageExperimentAuditRequest.scope,
        "expectedAppId": app, "nonce": nonce, "deadline": now + 30_000])
      let live = try ImageExperimentAuditRequest(encoded: encodedRequest, brokerMetadataPresent: false)
      let appReport = try ImageExperimentInstallationAudit.appReport(request: live, applicationId: app, digest: digest)
      let fields = try JSONSerialization.jsonObject(with: Data(base64Encoded: appReport)!) as! [String: Any]
      for (field, value) in [("nonce", String(repeating: "b", count: 64)), ("scope", "image-optimization-native-fixture-r1")] {
        var invalid = fields
        invalid[field] = value
        let encoded = try ImageExperimentAuditJSON.encode(invalid)
        try mustRefuse { _ = try ImageExperimentInstallationAudit.report(request: live, encodedAppReport: encoded,
          runnerId: live.runnerId, digest: digest) }
      }
      for invalid in [ImageExperimentBundleDigest(sha256: digest.sha256, fileCount: 0, byteCount: 1),
        ImageExperimentBundleDigest(sha256: digest.sha256, fileCount: 65_537, byteCount: 1),
        ImageExperimentBundleDigest(sha256: digest.sha256, fileCount: 1, byteCount: 512 * 1024 * 1024 + 1),
        ImageExperimentBundleDigest(sha256: "invalid", fileCount: 1, byteCount: 1)] {
        try mustRefuse { _ = try ImageExperimentInstallationAudit.appReport(request: live, applicationId: app, digest: invalid) }
        try mustRefuse { _ = try ImageExperimentInstallationAudit.report(request: live, encodedAppReport: appReport,
          runnerId: live.runnerId, digest: invalid) }
      }
      try mustRefuse { _ = try ImageExperimentInstallationAudit.appReport(request: live, applicationId: "ordinary", digest: digest) }
      try mustRefuse { _ = try ImageExperimentInstallationAudit.report(request: live, encodedAppReport: appReport,
        runnerId: "ordinary", digest: digest) }
      let expired = try ImageExperimentAuditRequest(encoded: ImageExperimentAuditJSON.encode(["schemaVersion": 1,
        "scope": ImageExperimentAuditRequest.scope, "expectedAppId": app, "nonce": nonce, "deadline": now - 1]),
        brokerMetadataPresent: false, now: now - 1_000)
      try mustRefuse { _ = try ImageExperimentInstallationAudit.appReport(request: expired, applicationId: app, digest: digest) }
      try mustRefuse { _ = try ImageExperimentInstallationAudit.report(request: expired, encodedAppReport: appReport,
        runnerId: live.runnerId, digest: digest) }
      let merged = try ImageExperimentInstallationAudit.report(request: live, encodedAppReport: appReport,
        runnerId: live.runnerId, digest: digest)
      print("IMAGE_EXPERIMENT_INSTALLATION_AUDIT " + merged)
    } else { print(digest.sha256 + " " + String(digest.fileCount) + " " + String(digest.byteCount)) }
  }
} catch { print("INSTALLATION_AUDIT_REFUSED"); exit(1) }
''', encoding='utf-8')
        binary = self.root / 'audit-synthetic'
        compile_result = subprocess.run(['xcrun', 'swiftc', '-module-cache-path', str(self.root / 'module-cache'),
            str(swift), str(harness), '-o', str(binary)], capture_output=True, timeout=30)
        self.assertEqual(compile_result.returncode, 0, 'Native Swift audit compilation failed')
        bundle = self.root / 'Synthetic.app'
        files = {'.hidden': b'hidden', '_CodeSignature/CodeResources': b'signature',
            'embedded.mobileprovision': b'profile', 'z.dSYM': b'symbols', 'a/file': b'nested', 'a-': b'ordered',
            '\u00e9': b'utf8'}
        for relative, content in files.items():
            destination = bundle / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(content)
        def run(*extra):
            return subprocess.run([str(binary), str(bundle), *extra], capture_output=True, timeout=10)
        def expected():
            digest = hashlib.sha256()
            for path in sorted(files, key=lambda value: value.encode('utf-8')):
                relative = path.encode('utf-8')
                digest.update(len(relative).to_bytes(4, 'big') + relative + hashlib.sha256(files[path]).digest())
            return f'{digest.hexdigest()} {len(files)} {sum(map(len, files.values()))}\n'.encode()
        result = run()
        self.assertEqual(result.returncode, 0)
        self.assertEqual(result.stdout, expected())
        report_result = run('report')
        self.assertEqual(report_result.returncode, 0)
        self.assertTrue(report_result.stdout.startswith(b'IMAGE_EXPERIMENT_INSTALLATION_AUDIT '))
        host_tools = Path(__file__).parents[4] / 'tools/image-optimization'
        host_python = host_tools / '.venv/bin/python'
        self.assertTrue(host_python.is_file(), 'Prepare the locked host environment before native audit integration tests')
        sha, count, byte_count = expected().decode().split()
        identity = 'com.tellpal.tellpalMobilePoc.imageexperiment.profile'
        state = {'request': {'nonce': 'a' * 64, 'deadline': int(time.time() * 1000) + 30_000},
            'build': {'expectedAppId': identity, 'testAppId': identity + '.uitests.xctrunner'},
            'expected': {'sha256': sha, 'fileCount': int(count), 'byteCount': int(byte_count)}}
        integration = subprocess.run([str(host_python), '-B', '-c', '''
import json, sys
from native_entry_audit import InstallationReport
state = json.loads(sys.stdin.buffer.readline())
observer = InstallationReport(state['request'], state['build'])
observer(sys.stdin.buffer.readline().rstrip(b'\\n'))
observer.verified(state['expected'], state['expected'])
print('SWIFT_HOST_REPORT_PASS')
'''], input=json.dumps(state).encode() + b'\n' + report_result.stdout,
            cwd=host_tools, capture_output=True, timeout=10)
        self.assertEqual(integration.returncode, 0, 'Host rejected the real Swift merged report')
        self.assertEqual(integration.stdout, b'SWIFT_HOST_REPORT_PASS\n')
        race = self.root / 'Race.app'
        race.mkdir()
        (race / 'first').write_bytes(b'placeholder')
        (race / 'second').write_bytes(b'placeholder')
        race_result = subprocess.run([str(binary), str(race), 'race'], capture_output=True, timeout=10)
        self.assertEqual(race_result.returncode, 0)
        self.assertEqual(race_result.stdout, b'EARLY_FILE_RACE_REFUSED\n')
        files['_CodeSignature/CodeResources'] = b'changed signature'
        (bundle / '_CodeSignature/CodeResources').write_bytes(files['_CodeSignature/CodeResources'])
        self.assertEqual(run().stdout, expected())
        self.assertNotEqual(result.stdout, run().stdout)
        self.assertEqual(run('deadline').stdout, b'INSTALLATION_AUDIT_REFUSED\n')
        unsafe = bundle / 'unsafe'
        unsafe.symlink_to(bundle / '.hidden')
        self.assertEqual(run().stdout, b'INSTALLATION_AUDIT_REFUSED\n')
        unsafe.unlink()
        os.mkfifo(unsafe)
        self.assertEqual(run().stdout, b'INSTALLATION_AUDIT_REFUSED\n')
        unsafe.unlink()
        with unsafe.open('wb') as stream:
            stream.truncate(256 * 1024 * 1024 + 1)
        self.assertEqual(run().stdout, b'INSTALLATION_AUDIT_REFUSED\n')

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

    def test_native_test_sources_and_pod_inheritance_exclude_application_plugins(self):
        ios = Path(__file__).parents[1]
        podfile = (ios / 'Podfile').read_text()
        native = re.search(r"target 'ImageExperimentUITests' do(.*?)\n  end", podfile, re.S).group(1)
        ordinary = re.search(r"target 'RunnerTests' do(.*?)\n  end", podfile, re.S).group(1)
        self.assertRegex(native, r'inherit!\s*:none\b')
        self.assertNotRegex(native, r'\bpod\s|flutter_install_all_ios_pods|use_frameworks!')
        self.assertRegex(ordinary, r'inherit!\s*:search_paths\b')
        for name in ('ImageExperimentBootstrapTests.swift', 'ImageExperimentBootstrap.swift',
            'ImageExperimentInstallationAudit.swift', 'ImageExperimentLedger.swift'):
            source = ios / ('ImageExperimentUITests' if name.endswith('Tests.swift') else 'ImageExperiment') / name
            self.assertTrue(set(re.findall(r'^\s*(?:@testable\s+)?import\s+(\w+)', source.read_text(), re.M)) <=
                {'CryptoKit', 'Darwin', 'Foundation', 'Security', 'UIKit', 'XCTest'})

    @unittest.skipUnless(Path('/usr/bin/ruby').is_file(), 'Apple Ruby runtime is unavailable')
    def test_generated_native_pod_aggregate_refuses_plugin_linkage(self):
        probe = subprocess.run(['/usr/bin/ruby', '-rxcodeproj', '-e', 'exit'], capture_output=True, timeout=10)
        if probe.returncode:
            self.skipTest('Xcodeproj gem is unavailable')
        harness = r'''
require 'xcodeproj'
require 'fileutils'
root, mode = ARGV
FileUtils.mkdir_p(root)
project = Xcodeproj::Project.new(File.join(root, 'Pods.xcodeproj'))
target = project.new_target(:static_library, 'Pods-ImageExperimentUITests', :ios, '15.0')
target.frameworks_build_phase.files.each(&:remove_from_project)
if mode == 'sdk_foundation'
  framework = project.new_file('Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS18.0.sdk/System/Library/Frameworks/Foundation.framework', 'DEVELOPER_DIR')
else
  framework = project.new_file('System/Library/Frameworks/Foundation.framework', 'SDKROOT')
end
target.frameworks_build_phase.add_file_reference(framework)
target.source_build_phase.add_file_reference(project.new_file('Pods-ImageExperimentUITests-dummy.m'))
base = project.new_file('native.xcconfig')
File.write(File.join(root, 'native.xcconfig'), mode == 'search_paths' ? "FRAMEWORK_SEARCH_PATHS=/plugin\n" : "SYNTHETIC=1\n")
target.build_configurations.each do |config|
  config.base_configuration_reference = base
  config.build_settings['MACH_O_TYPE'] = mode == 'dynamic' ? 'mh_dylib' : 'staticlib'
  config.build_settings['OTHER_LDFLAGS'] = mode == 'link_flags' ? '-framework audio_session' : ''
end
if mode == 'dependency'
  target.add_dependency(project.new_target(:static_library, 'audio_session', :ios, '15.0'))
elsif mode == 'framework'
  target.frameworks_build_phase.add_file_reference(project.new_file('audio_session.framework'))
end
project.save
'''
        script = Path(__file__).with_name('test_target_isolation.rb')
        for mode in ('native', 'sdk_foundation', 'dependency', 'framework', 'search_paths', 'link_flags', 'dynamic'):
            with self.subTest(mode=mode):
                fixture = self.root / mode
                setup = subprocess.run(['/usr/bin/ruby', '-e', harness, str(fixture), mode],
                    capture_output=True, timeout=10)
                self.assertEqual(setup.returncode, 0, setup.stderr.decode())
                result = subprocess.run(['/usr/bin/ruby', str(script), '--pods-only', str(fixture / 'Pods.xcodeproj')],
                    capture_output=True, timeout=10)
                if mode in ('native', 'sdk_foundation'):
                    self.assertEqual(result.returncode, 0, result.stderr.decode())
                    self.assertEqual(result.stdout, b'XCODE_TARGET_ISOLATION_PASS\n')
                else:
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn(b'TARGET_ISOLATION_REFUSED', result.stderr)

    @staticmethod
    def macho(dependencies, filetype, extra=b''):
        commands = []
        for name in dependencies:
            encoded = name.encode('ascii') + b'\0'
            size = (24 + len(encoded) + 7) // 8 * 8
            commands.append(struct.pack('<6I', 0xC, size, 24, 0, 0, 0) + encoded +
                bytes(size - 24 - len(encoded)))
        data = b''.join(commands) + extra
        return struct.pack('<8I', 0xFEEDFACF, 0x0100000C, 0, filetype,
            len(commands) + bool(extra), len(data), 0, 0) + data

    def native_runner(self, mode='profile'):
        runner = self.root / 'ImageExperimentUITests-Runner.app'
        plugin = runner / 'PlugIns/ImageExperimentUITests.xctest'
        plugin.mkdir(parents=True, exist_ok=True)
        app = f'com.tellpal.tellpalMobilePoc.imageexperiment.{mode}'
        (runner / 'Info.plist').write_bytes(plistlib.dumps({'CFBundleIdentifier': app + '.uitests.xctrunner',
            'CFBundleExecutable': 'ImageExperimentUITests-Runner'}))
        (plugin / 'Info.plist').write_bytes(plistlib.dumps({'CFBundleIdentifier': app + '.uitests',
            'CFBundleExecutable': 'ImageExperimentUITests'}))
        (runner / 'ImageExperimentUITests-Runner').write_bytes(self.macho([
            '@rpath/XCTest.framework/XCTest', '/System/Library/Frameworks/UIKit.framework/UIKit',
            '/usr/lib/libSystem.B.dylib'], 2))
        (plugin / 'ImageExperimentUITests').write_bytes(self.macho([
            '@rpath/XCTest.framework/XCTest', '@rpath/libXCTestSwiftSupport.dylib',
            '/System/Library/Frameworks/CryptoKit.framework/CryptoKit',
            '/System/Library/Frameworks/Foundation.framework/Foundation',
            '/System/Library/Frameworks/Security.framework/Security',
            '/usr/lib/swift/libswiftCore.dylib'], 8))
        return runner, plugin

    def linkage_cli(self, runner):
        return subprocess.run([sys.executable, '-B', str(Path(__file__).with_name('verify_native_linkage.py')),
            '--runner', str(runner)], capture_output=True, timeout=15)

    def test_native_macho_accepts_both_exact_identities_and_fixed_cli_output(self):
        for mode in ('profile', 'release'):
            runner, _plugin = self.native_runner(mode)
            report = linkage.verify_runner(runner)
            self.assertEqual(report['imageCount'], 2)
            self.assertEqual(report['dependencyCount'], 9)
            result = self.linkage_cli(runner)
            self.assertEqual(result.returncode, 0)
            self.assertEqual(result.stdout, b'NATIVE_TEST_LINKAGE_PASS\n')
            self.assertEqual(result.stderr, b'')

    def test_native_macho_refuses_custom_unknown_and_malformed_dependencies(self):
        runner, plugin = self.native_runner()
        test_binary = plugin / 'ImageExperimentUITests'
        for name in ('@rpath/Flutter.framework/Flutter', '@rpath/audio_session.framework/audio_session',
            '@rpath/Unknown.framework/Unknown', '@rpath/../XCTest.framework/XCTest',
            '/custom/libSystem.B.dylib'):
            with self.subTest(dependency=name):
                test_binary.write_bytes(self.macho([name], 8))
                with self.assertRaises(linkage.NativeLinkageRefusal):
                    linkage.verify_runner(runner)
        good = self.macho(['/usr/lib/libSystem.B.dylib'], 8)
        corruptions = [b'not Mach-O', good[:-1], good[:8] + struct.pack('<I', 0x0200000C) + good[12:],
            good[:12] + struct.pack('<I', 2) + good[16:],
            good[:16] + struct.pack('<I', linkage.MAX_COMMANDS + 1) + good[20:],
            good[:36] + struct.pack('<I', 7) + good[40:],
            good[:40] + struct.pack('<I', 4) + good[44:],
            self.macho(['/usr/lib/libSystem.B.dylib'], 8, struct.pack('<2I', 0x27, 8)),
            self.macho(['/usr/lib/libSystem.B.dylib'], 8, struct.pack('<2I', 0x777, 8))]
        for number, data in enumerate(corruptions):
            with self.subTest(malformed=number):
                test_binary.write_bytes(data)
                with self.assertRaises(linkage.NativeLinkageRefusal):
                    linkage.verify_runner(runner)
        result = self.linkage_cli(runner)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(result.stdout, b'NATIVE_TEST_LINKAGE_REFUSED\n')
        self.assertEqual(result.stderr, b'')

    def test_native_macho_checks_every_fat_slice_and_lazy_weak_reexport_load(self):
        runner, plugin = self.native_runner()
        binary = runner / 'ImageExperimentUITests-Runner'
        thin = binary.read_bytes()
        def fat(slices):
            table, body, offset = b'', b'', 8 + 20 * len(slices)
            for subtype, data in slices:
                table += struct.pack('>5I', 0x0100000C, subtype, offset, len(data), 0)
                body += data
                offset += len(data)
            return struct.pack('>2I', 0xCAFEBABE, len(slices)) + table + body
        binary.write_bytes(fat([(0, thin)]))
        self.assertEqual(linkage.verify_runner(runner)['dependencyCount'], 9)
        arm64e = thin[:8] + struct.pack('<I', 2) + thin[12:]
        binary.write_bytes(fat([(0, thin), (2, arm64e)]))
        self.assertEqual(linkage.verify_runner(runner)['dependencyCount'], 12)
        bad = self.macho(['@rpath/Flutter.framework/Flutter'], 2)
        bad = bad[:8] + struct.pack('<I', 2) + bad[12:]
        binary.write_bytes(fat([(0, thin), (2, bad)]))
        with self.assertRaises(linkage.NativeLinkageRefusal):
            linkage.verify_runner(runner)
        binary.write_bytes(thin)
        test_binary = plugin / 'ImageExperimentUITests'
        for command in linkage.DYLIB_COMMANDS:
            data = self.macho(['@rpath/integration_test.framework/integration_test'], 8)
            test_binary.write_bytes(data[:32] + struct.pack('<I', command) + data[36:])
            with self.subTest(command=command), self.assertRaises(linkage.NativeLinkageRefusal):
                linkage.verify_runner(runner)

    def test_native_macho_refuses_embedded_plugins_aliases_and_unbounded_inputs(self):
        runner, plugin = self.native_runner()
        framework = runner / 'Frameworks/Flutter.framework'
        framework.mkdir(parents=True)
        with self.assertRaises(linkage.NativeLinkageRefusal):
            linkage.verify_runner(runner)

        framework.rmdir()
        support = runner / 'Frameworks/libXCTestSwiftSupport.dylib'
        support.write_bytes(self.macho(['/usr/lib/libSystem.B.dylib'], 6))
        self.assertEqual(linkage.verify_runner(runner)['imageCount'], 3)
        support.unlink()
        support.symlink_to(plugin / 'ImageExperimentUITests')
        with self.assertRaises(linkage.NativeLinkageRefusal):
            linkage.verify_runner(runner)
        support.unlink()
        with self.assertRaises(linkage.NativeLinkageRefusal):
            linkage.verify_runner(runner, deadline=time.monotonic() - 1)
        binary = plugin / 'ImageExperimentUITests'
        with binary.open('wb') as stream:
            stream.truncate(linkage.MAX_IMAGE_BYTES + 1)
        with self.assertRaises(linkage.NativeLinkageRefusal):
            linkage.verify_runner(runner)
        runner, _plugin = self.native_runner()
        info = plistlib.loads((runner / 'Info.plist').read_bytes())
        info['CFBundleIdentifier'] = 'com.tellpal.tellpalMobilePoc'
        (runner / 'Info.plist').write_bytes(plistlib.dumps(info))
        with self.assertRaises(linkage.NativeLinkageRefusal):
            linkage.verify_runner(runner)

    def test_native_macho_revalidates_identity_and_earlier_images_before_success(self):
        for change in ('identity', 'earlier_image'):
            with self.subTest(change=change):
                runner, plugin = self.native_runner()
                original = linkage.image_dependencies
                def mutate_before_later_image(path, **arguments):
                    if path == plugin / 'ImageExperimentUITests':
                        if change == 'identity':
                            info = plistlib.loads((runner / 'Info.plist').read_bytes())
                            info['CFBundleIdentifier'] = 'com.tellpal.tellpalMobilePoc'
                            (runner / 'Info.plist').write_bytes(plistlib.dumps(info))
                        else:
                            (runner / 'ImageExperimentUITests-Runner').write_bytes(
                                self.macho(['@rpath/Flutter.framework/Flutter'], 2))
                    return original(path, **arguments)
                with patch.object(linkage, 'image_dependencies', mutate_before_later_image):
                    with self.assertRaises(linkage.NativeLinkageRefusal):
                        linkage.verify_runner(runner)


if __name__ == "__main__":
    unittest.main()

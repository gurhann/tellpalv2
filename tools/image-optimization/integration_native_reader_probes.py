"""Explicit Swift/Kotlin host integration; never physical-device evidence or a portable suite."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
MOBILE = ROOT / 'app/mobile'
IOS = MOBILE / 'ios'
ANDROID = MOBILE / 'android/app/src'


def checked(command, timeout=60):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=timeout)
    if result.returncode:
        raise AssertionError(result.stdout[-6000:] + result.stderr[-6000:])
    return result.stdout


class NativeReaderProbeTests(unittest.TestCase):
    def test_kotlin_activity_callbacks_reach_reader_channel_and_launcher(self):
        harness = IOS / 'ImageExperimentUITests/android_diagnostic_harness.py'
        spec = importlib.util.spec_from_file_location('reader_activity_harness', harness)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        output = Path(tempfile.mkdtemp(prefix='reader-activity-lifecycle-',
            dir=os.environ.get('NATIVE_INTEGRATION_EVIDENCE_ROOT', '/private/tmp')))
        result = module.run(output=output, reader_lifecycle=True)
        self.assertEqual(result['cases'], ['actual-reader-activity-callbacks-reach-channel-and-launcher'])
        self.assertEqual(result['caseCount'], 1)
        self.assertEqual(result['matrixExit'], 0)
        frozen = json.loads((output / 'source-freeze.json').read_text())
        activity = 'app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentActivity.kt'
        self.assertEqual({row['callback'] for row in result['lifecycleMutants']}, {'onStop', 'onResume'})
        for mutation in result['lifecycleMutants']:
            self.assertEqual(mutation['compileExit'], 0)
            self.assertNotEqual(mutation['runtimeExit'], 0)
            self.assertEqual(mutation['originalSha256'], frozen[activity])
            status = json.loads((output / (mutation['runtimeLabel'] + '.status.json')).read_text())
            self.assertEqual(status['status'], 'failed')

    def test_swift_scene_restoration_preserves_activity_and_forwards_same_scene(self):
        scene_delegate = IOS / 'ImageExperiment/SceneDelegate.swift'
        source = scene_delegate.read_text()
        start = source.index('  func stateRestorationActivity(for scene: UIScene) -> NSUserActivity? {')
        method = source[start:source.index('\n  func scene(_ scene: UIScene, restoreInteractionStateWith', start)]
        with tempfile.TemporaryDirectory(prefix='reader-restoration-swift-', dir='/private/tmp') as temporary:
            base = Path(temporary)
            main = base / 'main.swift'
            main.write_text('import Foundation\n' + r'''
final class NSUserActivity {
  static var allocations = 0
  let activityType: String
  init(activityType: String) { Self.allocations += 1; self.activityType = activityType }
}
final class UIScene { var userActivity: NSUserActivity? }
final class FlutterSceneDelegate {
  static var calls = 0
  var scenes = [UIScene]()
  var activityAtForward: NSUserActivity?
  var returnedActivity: NSUserActivity?
  func stateRestorationActivity(for scene: UIScene) -> NSUserActivity? {
    Self.calls += 1; scenes.append(scene); activityAtForward = scene.userActivity
    return returnedActivity
  }
}
final class SceneDelegate {
  private let ordinaryDelegate: FlutterSceneDelegate?
  init(_ ordinaryDelegate: FlutterSceneDelegate?) { self.ordinaryDelegate = ordinaryDelegate }
''' + method + r'''
}
let flutter = FlutterSceneDelegate()
let delegate = SceneDelegate(flutter)
let scene = UIScene()
let returned = NSUserActivity(activityType: "com.tellpal.host.returned")
flutter.returnedActivity = returned
let beforeFallback = NSUserActivity.allocations
precondition(delegate.stateRestorationActivity(for: scene) === returned)
precondition(NSUserActivity.allocations == beforeFallback + 1)
let fallback = scene.userActivity!
precondition(fallback.activityType == "com.tellpal.imageexperiment.scene-restoration")
precondition(flutter.scenes.count == 1 && flutter.scenes[0] === scene)
precondition(flutter.activityAtForward === fallback && fallback !== returned)
precondition(delegate.stateRestorationActivity(for: scene) === returned)
precondition(scene.userActivity === fallback && NSUserActivity.allocations == beforeFallback + 1)
let existing = NSUserActivity(activityType: "com.tellpal.host.existing")
let existingScene = UIScene(); existingScene.userActivity = existing
let beforeExisting = NSUserActivity.allocations
precondition(delegate.stateRestorationActivity(for: existingScene) === returned)
precondition(existingScene.userActivity === existing && flutter.activityAtForward === existing)
precondition(flutter.scenes.last === existingScene && NSUserActivity.allocations == beforeExisting)
flutter.returnedActivity = nil
precondition(delegate.stateRestorationActivity(for: existingScene) == nil)
precondition(existingScene.userActivity === existing && NSUserActivity.allocations == beforeExisting)
let audit = SceneDelegate(nil)
let auditScene = UIScene()
let beforeAudit = NSUserActivity.allocations; let callsBeforeAudit = FlutterSceneDelegate.calls
precondition(audit.stateRestorationActivity(for: auditScene) == nil)
precondition(auditScene.userActivity == nil && NSUserActivity.allocations == beforeAudit)
precondition(audit.stateRestorationActivity(for: existingScene) == nil)
precondition(existingScene.userActivity === existing && NSUserActivity.allocations == beforeAudit)
precondition(FlutterSceneDelegate.calls == callsBeforeAudit)
print("PASS Swift scene restoration: stable fallback, existing identity, audit no allocation, same scene and delegate return")
''')
            checked(['xcrun', 'swiftc', '-module-cache-path', base / 'module-cache', main, '-o', base / 'check'])
            self.assertIn('PASS Swift scene restoration:', checked([base / 'check']))
        self.assertEqual(scene_delegate.read_text(), source, 'Scene delegate changed during exact method execution')
        checked(['xcrun', 'swiftc', '-frontend', '-parse', scene_delegate])

    def test_swift_reader_background_wait_has_one_monotonic_bound_and_finite_states(self):
        launcher = IOS / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift'
        source = launcher.read_text()
        helper = source[source.index('enum ImageExperimentReaderBackgroundWait {'):source.index('\nenum ImageExperimentAuditStage:')]
        with tempfile.TemporaryDirectory(prefix='reader-background-swift-', dir='/private/tmp') as temporary:
            base = Path(temporary)
            main = base / 'main.swift'
            main.write_text('import Foundation\n' + r'''
// The executable host fixture models only XCTest's finite state boundary, never an OS transition.
class XCUIApplication { enum State { case runningBackground, runningBackgroundSuspended, runningForeground, notRunning, unknown }; init(bundleIdentifier: String) {}; var state: State { .runningForeground }; var otherElements: XCUIElementQuery { XCUIElementQuery() }; func activate() {} }
class XCUIElement { var exists: Bool { false }; var value: Any? { nil } }
class XCUIElementQuery { subscript(identifier: String) -> XCUIElement { XCUIElement() } }
''' + helper + r'''
typealias Wait = ImageExperimentReaderBackgroundWait
precondition(Wait.state(.runningBackground) == .background)
precondition(Wait.state(.runningBackgroundSuspended) == .suspended)
precondition(Wait.state(.runningForeground) == .foreground)
precondition(Wait.state(.notRunning) == .notRunning)
precondition(Wait.state(.unknown) == .unknown)
for state in [Wait.State.background, .suspended] {
  var clock = 10.0; var observations = 0; var pauses = 0
  let outcome = Wait.wait(absoluteDeadline: 180, now: { clock }, pause: { clock += $0; pauses += 1 }, observe: { observations += 1; return observations == 1 ? .foreground : state })
  precondition(outcome.accepted && outcome.observed == state && observations == 2 && pauses == 1)
}
for state in [Wait.State.notRunning, .unknown] {
  var observations = 0
  let outcome = Wait.wait(absoluteDeadline: 180, now: { 10 }, pause: { _ in preconditionFailure("terminal state must not poll") }, observe: { observations += 1; return state })
  precondition(!outcome.accepted && outcome.observed == state && observations == 1)
}
for nativeDeadline in [180.0, 11.0] {
  var clock = 10.0; var observations = 0; var paused = 0.0
  let outcome = Wait.wait(absoluteDeadline: nativeDeadline, now: { clock }, pause: { amount in precondition(amount > 0 && amount <= 0.02); clock += amount; paused += amount }, observe: { observations += 1; return .foreground })
  let expected = min(13, nativeDeadline)
  precondition(!outcome.accepted && outcome.observed == .foreground && clock == expected)
  precondition(abs(paused - (expected - 10)) < 0.000001 && observations > 0)
}
for observedAt in [13.0, 13.1] {
  var clock = 10.0
  let outcome = Wait.wait(absoluteDeadline: 180, now: { clock }, pause: { _ in preconditionFailure("expired query must not pause") }, observe: { clock = observedAt; return .suspended })
  precondition(!outcome.accepted && outcome.observed == .suspended)
}
for observedAt in [180.0, 180.1] {
  var clock = 179.5
  let outcome = Wait.wait(absoluteDeadline: 180, now: { clock }, observe: { clock = observedAt; return .background })
  precondition(!outcome.accepted && outcome.observed == .background)
}
for value in [Double.nan, Double.infinity, -Double.infinity] {
  precondition(!Wait.wait(absoluteDeadline: 180, now: { value }, observe: { preconditionFailure("invalid start must not observe") }).accepted)
  precondition(!Wait.wait(absoluteDeadline: value, now: { 10 }, observe: { preconditionFailure("invalid deadline must not observe") }).accepted)
  var clock = 10.0
  precondition(!Wait.wait(absoluteDeadline: 180, now: { clock }, observe: { clock = value; return .background }).accepted)
}
var backward = 10.0
precondition(!Wait.wait(absoluteDeadline: 180, now: { backward }, observe: { backward = 9; return .background }).accepted)
for deadline in [9.0, 10.0] {
  precondition(!Wait.wait(absoluteDeadline: deadline, now: { 10 }, observe: { preconditionFailure("expired wait must not observe") }).accepted)
}
precondition(Set([Wait.State.foreground, .background, .suspended, .notRunning, .unknown].map { $0.rawValue }) == Set(["FOREGROUND", "BACKGROUND", "BACKGROUND_SUSPENDED", "NOT_RUNNING", "UNKNOWN"]))
print("PASS Swift reader background: finite states, shared 3s/native deadline, post-query time, backward/nonfinite clocks")
''')
            checked(['xcrun', 'swiftc', '-module-cache-path', base / 'module-cache', main, '-o', base / 'check'])
            self.assertIn('PASS Swift reader background:', checked([base / 'check']))
        self.assertEqual(launcher.read_text(), source, 'Launcher changed during the exact helper execution')
        checked(['xcrun', 'swiftc', '-frontend', '-parse', launcher])

    def test_swift_settings_activation_observes_both_proxies_and_restores_same_original(self):
        launcher = IOS / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift'
        source = launcher.read_text()
        helper = source[source.index('enum ImageExperimentReaderBackgroundWait {'):source.index('\nenum ImageExperimentAuditStage:')]
        with tempfile.TemporaryDirectory(prefix='reader-settings-swift-', dir='/private/tmp') as temporary:
            base = Path(temporary)
            main = base / 'main.swift'
            main.write_text('import Foundation\n' + r'''
final class XCUIApplication {
  enum State { case runningBackground, runningBackgroundSuspended, runningForeground, notRunning, unknown }
  static var clock = 10.0
  static var events = [String]()
  static var activationDuration = 0.0
  static var queryDuration = 0.0
  static var settingsState = State.runningForeground
  static var backgroundState = State.runningBackground
  static var returnState = State.runningForeground
  static var initialState = State.runningForeground
  static var phase = 0
  static var pauses = [Double]()
  var otherElements: XCUIElementQuery { XCUIElementQuery() }
  let identifier: String
  init(bundleIdentifier: String) { identifier = bundleIdentifier; Self.events.append("init:" + identifier) }
  var state: State {
    Self.events.append("observe:" + identifier)
    Self.clock += Self.queryDuration
    if identifier == "com.apple.Preferences" { return Self.settingsState }
    precondition(identifier == "com.tellpal.tellpalMobilePoc.imageexperiment.release")
    return Self.phase == 0 ? Self.initialState : Self.phase == 1 ? Self.backgroundState : Self.returnState
  }
  func activate() {
    Self.events.append("activate:" + identifier)
    Self.clock += Self.activationDuration
    Self.phase = identifier == "com.apple.Preferences" ? 1 : 2
  }
  static func reset() {
    clock = 10; events = []; activationDuration = 0; queryDuration = 0
    settingsState = .runningForeground; backgroundState = .runningBackground
    returnState = .runningForeground; initialState = .runningForeground; phase = 0; pauses = []
  }
  static func pause(_ amount: Double) {
    precondition(amount > 0 && amount <= 0.02)
    pauses.append(amount); clock += amount
  }
}
class XCUIElement { var exists: Bool { false }; var value: Any? { nil } }
class XCUIElementQuery { subscript(identifier: String) -> XCUIElement { XCUIElement() } }
''' + helper + r'''
@main struct SettingsHostChecks {
  @MainActor static func main() {
    typealias App = XCUIApplication
    typealias Wait = ImageExperimentReaderBackgroundWait
    let originalID = "com.tellpal.tellpalMobilePoc.imageexperiment.release"
    func original() -> App { App(bundleIdentifier: originalID) }
    func background(_ app: App, _ deadline: Double = 180) -> Wait.Outcome {
      Wait.background(original: app, absoluteDeadline: deadline, now: { App.clock }, pause: App.pause)
    }
    func restore(_ app: App, _ deadline: Double = 180) -> Wait.Outcome {
      Wait.restore(original: app, absoluteDeadline: deadline, now: { App.clock }, pause: App.pause)
    }
    for state in [App.State.runningBackground, .runningBackgroundSuspended] {
      App.reset(); let app = original(); App.backgroundState = state
      precondition(background(app).accepted)
      precondition(App.events == ["init:" + originalID, "init:com.apple.Preferences", "observe:" + originalID,
        "activate:com.apple.Preferences", "observe:com.apple.Preferences", "observe:" + originalID])
      precondition(restore(app).accepted)
      precondition(Array(App.events.suffix(3)) == ["observe:" + originalID, "activate:" + originalID, "observe:" + originalID])
      precondition(App.events.filter { $0 == "activate:com.apple.Preferences" }.count == 1)
      precondition(App.events.filter { $0 == "activate:" + originalID }.count == 1)
      precondition(App.events.filter { $0 == "init:" + originalID }.count == 1)
    }
    for state in [App.State.runningForeground, .notRunning, .unknown] {
      App.reset(); let app = original(); App.backgroundState = state
      precondition(!background(app).accepted)
      precondition(App.events.filter { $0 == "activate:com.apple.Preferences" }.count == 1)
      precondition(App.events.filter { $0 == "activate:" + originalID }.isEmpty)
      if state == .runningForeground { precondition(App.clock == 13 && !App.pauses.isEmpty) }
      else { precondition(App.pauses.isEmpty) }
    }
    for state in [App.State.runningBackground, .runningBackgroundSuspended, .notRunning, .unknown] {
      App.reset(); let app = original(); App.settingsState = state
      let result = background(app)
      precondition(!result.accepted && result.foregroundObserved != .foreground && App.pauses.isEmpty)
      precondition(App.events.filter { $0 == "observe:" + originalID }.count == 1)
    }
    for state in [App.State.notRunning, .unknown] {
      App.reset(); let app = original(); App.initialState = state
      precondition(!background(app).accepted && !App.events.contains(where: { $0.hasPrefix("activate:") }))
      App.reset(); let returning = original(); App.phase = 1; App.backgroundState = state
      precondition(!restore(returning).accepted && !App.events.contains(where: { $0.hasPrefix("activate:") }))
      App.reset(); let dying = original(); App.phase = 1; App.returnState = state
      precondition(!restore(dying).accepted && App.events.filter { $0 == "activate:" + originalID }.count == 1)
    }
    for delay in [3.0, 3.1, Double.nan, Double.infinity, -1.0] {
      App.reset(); let app = original(); App.activationDuration = delay
      precondition(!background(app).accepted)
      precondition(App.events.filter { $0 == "activate:com.apple.Preferences" }.count == 1)
      precondition(!App.events.contains("observe:com.apple.Preferences"))
      App.reset(); let returning = original(); App.phase = 1; App.activationDuration = delay
      precondition(!restore(returning).accepted)
      precondition(App.events.filter { $0 == "activate:" + originalID }.count == 1)
    }
    for delay in [1.0, 1.5, 3.0, Double.nan, Double.infinity, -1.0] {
      App.reset(); let app = original(); App.queryDuration = delay
      precondition(!background(app).accepted)
    }
    App.reset(); let lateReturn = original(); App.phase = 1; App.queryDuration = 1.5
    precondition(!restore(lateReturn).accepted && App.clock == 13)
    for state in [App.State.runningBackground, .runningBackgroundSuspended] {
      App.reset(); let app = original(); App.phase = 1; App.returnState = state
      precondition(!restore(app).accepted && App.clock == 13)
      precondition(App.events.filter { $0 == "activate:" + originalID }.count == 1)
    }
    App.reset(); let capped = original(); App.clock = 179.5; App.activationDuration = 0.5
    precondition(!background(capped).accepted && App.clock == 180)
    App.reset(); let returnCapped = original(); App.phase = 1; App.clock = 179.5; App.activationDuration = 0.5
    precondition(!restore(returnCapped).accepted && App.clock == 180)
    for invalid in [Double.nan, Double.infinity, -Double.infinity, 10.0] {
      App.reset(); let app = original()
      precondition(!background(app, invalid).accepted)
      precondition(!App.events.contains(where: { $0.hasPrefix("activate:") }))
    }
    App.reset(); let bounded = original(); App.backgroundState = .runningForeground
    precondition(!background(bounded, 10.015).accepted && App.clock == 10.015)
    precondition(App.pauses.count == 1 && abs(App.pauses[0] - 0.015) < 0.000001)
    // The real production pause processes this run loop; a sleeping replacement cannot fire the timer.
    var timerFired = false
    let timer = Timer.scheduledTimer(withTimeInterval: 0.001, repeats: false) { _ in timerFired = true }
    let timerDeadline = ProcessInfo.processInfo.systemUptime + 0.1
    while !timerFired && ProcessInfo.processInfo.systemUptime < timerDeadline {
      Wait.processRunLoop(min(0.02, timerDeadline - ProcessInfo.processInfo.systemUptime))
    }
    precondition(timerFired); timer.invalidate()
    print("PASS Swift Settings: exact public proxy identities, one activation, same original return, finite states, shared deadline and runloop slices")
  }
}
''')
            checked(['xcrun', 'swiftc', '-parse-as-library', '-module-cache-path', base / 'module-cache', main, '-o', base / 'check'])
            self.assertIn('PASS Swift Settings:', checked([base / 'check']))
        self.assertEqual(launcher.read_text(), source, 'Launcher changed during exact Settings adapter execution')

    def test_swift_reader_launcher_gates_queries_and_holds_poll_cadence(self):
        launcher = IOS / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift'
        source = launcher.read_text()
        helper = source[source.index('enum ImageExperimentReaderBackgroundWait {'):source.index('\nenum ImageExperimentAuditStage:')]
        with tempfile.TemporaryDirectory(prefix='reader-polling-swift-', dir='/private/tmp') as temporary:
            base = Path(temporary)
            main = base / 'main.swift'
            main.write_text('import Foundation\n' + r'''
class XCUIApplication {
  enum State { case runningBackground, runningBackgroundSuspended, runningForeground, notRunning, unknown }
  static var events = [String]()
  static var markerExists = true
  static var bindingExists = true
  static var requested = true
  init(bundleIdentifier: String) {}
  var state: State { .runningForeground }
  var otherElements: XCUIElementQuery { Self.events.append("collection"); return XCUIElementQuery() }
  func activate() { preconditionFailure("query polling must not activate an application") }
}
class XCUIElementQuery {
  subscript(identifier: String) -> XCUIElement {
    XCUIApplication.events.append("lookup:" + identifier)
    return XCUIElement(identifier)
  }
}
class XCUIElement {
  let identifier: String
  init(_ identifier: String) { self.identifier = identifier }
  var exists: Bool {
    XCUIApplication.events.append("exists:" + identifier)
    return identifier == "imageExperimentReaderBackground" ? XCUIApplication.markerExists : XCUIApplication.bindingExists
  }
  var value: Any? {
    XCUIApplication.events.append("value:" + identifier)
    return XCUIApplication.requested ? "requested" : "other"
  }
}
''' + helper + r'''
@main struct PollingHostChecks {
  @MainActor static func main() throws {
    typealias App = XCUIApplication
    typealias Wait = ImageExperimentReaderBackgroundWait
    let app = App(bundleIdentifier: "com.tellpal.tellpalMobilePoc.imageexperiment.release")
    for primary in [false, true] {
      for complete in [false, true] {
        for capture in [false, true] {
          App.events = []; var backgrounds = 0; var qualities = 0
          Wait.pollElements(original: app, readerPrimary: primary, backgroundCompleted: complete,
            capture: capture, onBackground: { backgrounds += 1 }, onQuality: { _ in qualities += 1 })
          precondition(backgrounds == (primary && !complete ? 1 : 0))
          precondition(qualities == (capture ? 1 : 0))
          precondition(App.events.filter { $0 == "lookup:imageExperimentReaderBackground" }.count == backgrounds)
          precondition(App.events.filter { $0 == "lookup:imageExperimentQualityBinding" }.count == qualities)
          precondition(App.events.filter { $0 == "collection" }.count == backgrounds + qualities)
        }
      }
    }
    App.markerExists = false; App.bindingExists = false; App.events = []
    Wait.pollElements(original: app, readerPrimary: true, backgroundCompleted: false, capture: true,
      onBackground: { preconditionFailure("missing marker") }, onQuality: { _ in preconditionFailure("missing binding") })
    precondition(!App.events.contains(where: { $0.hasPrefix("value:") }))
    App.markerExists = true; App.requested = false
    Wait.pollElements(original: app, readerPrimary: true, backgroundCompleted: false, capture: false,
      onBackground: { preconditionFailure("wrong marker value") }, onQuality: { _ in preconditionFailure("capture disabled") })
    App.requested = true; App.bindingExists = true; App.events = []
    enum Expected: Error { case stop }
    do {
      try Wait.pollElements(original: app, readerPrimary: true, backgroundCompleted: false, capture: true,
        onBackground: { throw Expected.stop }, onQuality: { _ in preconditionFailure("must preserve original refusal") })
      preconditionFailure("callback refusal swallowed")
    } catch Expected.stop { }
    precondition(!App.events.contains("lookup:imageExperimentQualityBinding"))
    for primary in [false, true] {
      for reader in [false, true] {
        var clock = 10.0; var slices = [Double](); App.events = []
        let accepted = Wait.pausePolling(readerPrimary: primary, readerDeadline: reader ? 180 : nil,
          now: { clock }, pause: { amount in
            precondition(amount > 0 && amount <= 0.02)
            slices.append(amount); clock += min(amount, 0.003)
          })
        precondition(accepted && abs(clock - (primary ? 10.5 : 10.1)) < 0.000001)
        precondition(slices.count > (primary ? 25 : 5)) // Early runloop input never accelerates outer queries.
        precondition(App.events.isEmpty)
      }
    }
    var clock = 179.985; var slices = [Double]()
    precondition(!Wait.pausePolling(readerPrimary: true, readerDeadline: 180, now: { clock }, pause: { amount in
      precondition(amount <= 180 - clock); slices.append(amount); clock += min(amount, 0.003)
    }))
    precondition(clock == 180 && !slices.isEmpty)
    precondition(!Wait.pausePolling(readerPrimary: true, readerDeadline: 180, now: { 180 }, pause: { _ in preconditionFailure("expired pause") }))
    for invalid in [Double.nan, Double.infinity, -Double.infinity] {
      precondition(!Wait.pausePolling(readerPrimary: true, readerDeadline: 180, now: { invalid }, pause: { _ in preconditionFailure("invalid clock") }))
      precondition(!Wait.pausePolling(readerPrimary: true, readerDeadline: invalid, now: { 10 }, pause: { _ in preconditionFailure("invalid deadline") }))
    }
    clock = 10
    precondition(!Wait.pausePolling(readerPrimary: true, readerDeadline: 180, now: { clock }, pause: { _ in clock = 9 }))
    clock = 179.99
    precondition(!Wait.pausePolling(readerPrimary: true, readerDeadline: 180, now: { clock }, pause: { _ in clock = 180.01 }))
    print("PASS Swift reader polling: gated UI queries, primary 2Hz, legacy/quality 10Hz, early-return runloop and native deadline cap")
  }
}
''')
            checked(['xcrun', 'swiftc', '-parse-as-library', '-module-cache-path', base / 'module-cache', main, '-o', base / 'check'])
            self.assertIn('PASS Swift reader polling:', checked([base / 'check']))
        self.assertEqual(launcher.read_text(), source, 'Launcher changed during exact polling helper execution')
        loop = source.split('while app.state == .runningForeground', 1)[1].split('guard !capture || captured', 1)[0]
        self.assertNotIn('app.otherElements[', loop)
        self.assertIn('pollElements(original: app', loop)
        self.assertIn('pausePolling(readerPrimary: readerPrimary', loop)

    def test_swift_actual_policy_lifecycle_quality_and_nullable_facts(self):
        ledger = IOS / 'ImageExperiment/ImageExperimentLedger.swift'
        with tempfile.TemporaryDirectory(prefix='reader-swift-', dir='/private/tmp') as temporary:
            base = Path(temporary)
            main = base / 'main.swift'
            main.write_text(r'''
import Foundation
enum ImageExperimentRefusal: Error { case refused }
func entries(_ batch: String) -> [[String: Any]] { (1...(ImageExperimentReaderPolicy.qualityBatches.contains(batch) ? 2 : 6)).map { ["caseId": "reader_page_\($0)", "mime": "image/png", "purposeAcceptanceReceipt": batch] } }
precondition(ImageExperimentReaderPolicy.primaryBatches == Set(["reader_resolution_a_r1", "reader_resolution_b_r1", "reader_resolution_c_r1", "reader_resolution_075_r1"]))
precondition(!ImageExperimentReaderPolicy.qualityBatches.contains("reader_resolution_075_r1"))
for batch in ImageExperimentReaderPolicy.batches {
  precondition(ImageExperimentReaderPolicy.accepts(batch: batch, selected: "reader_page_1", entries: entries(batch)))
  var wrong = entries(batch); wrong.swapAt(0, 1)
  precondition(!ImageExperimentReaderPolicy.accepts(batch: batch, selected: "reader_page_1", entries: wrong))
  precondition(!ImageExperimentReaderPolicy.accepts(batch: batch, selected: "reader_page_2", entries: entries(batch)))
}
precondition(!ImageExperimentReaderPolicy.accepts(batch: "native_comparison_r1", selected: "reader_page_1", entries: entries("native_comparison_r1")))
var clock: Int64 = 1000
let lifecycle = ImageExperimentReaderLifecycle(now: { clock })
lifecycle.background(); lifecycle.foreground()
precondition(lifecycle.snapshot()["backgroundCount"] as? Int == 0)
lifecycle.enable(); lifecycle.foreground()
precondition(lifecycle.snapshot()["returnCount"] as? Int == 0)
lifecycle.background(); lifecycle.background()
precondition(lifecycle.snapshot()["backgroundCount"] as? Int == 1)
precondition(lifecycle.snapshot()["returnedEpochMilliseconds"] is NSNull)
clock = 1700; lifecycle.foreground(); lifecycle.foreground()
precondition(lifecycle.snapshot()["returnCount"] as? Int == 1)
precondition((lifecycle.snapshot()["returnedEpochMilliseconds"] as? NSNumber)?.int64Value == 1700)
let quality = ImageExperimentReaderQualityLifetime(now: { clock })
precondition(quality.begin(sessionExpiresAt: 40000) && quality.deadline == 11700)
quality.release(); clock = 9700
precondition(quality.begin(sessionExpiresAt: 40000) && quality.deadline == 19700)
quality.release(); precondition(!quality.begin(sessionExpiresAt: 40000))
let expired = ImageExperimentReaderQualityLifetime(now: { clock })
precondition(expired.begin(sessionExpiresAt: 40000)); expired.release(); clock += 20000
precondition(!expired.begin(sessionExpiresAt: 50000))
let cancelled = ImageExperimentReaderQualityLifetime(now: { 1000 }); cancelled.invalidate()
precondition(!cancelled.begin(sessionExpiresAt: 50000))
precondition(!ImageExperimentReaderQualityLifetime(now: { Int64.max }).begin(sessionExpiresAt: Int64.max))
let raw = #"{"schemaVersion":1,"records":[{"event":"device_facts","isPhysicalDevice":null,"lowRamDevice":null,"apiLevel":null,"platform":"ios","qemu":"unavailable"},{"event":"lifecycle","backgrounded":false,"backgroundCount":0,"returnCount":0}],"droppedCount":0}"#
precondition((try? ImageExperimentLedger.validate(raw)) != nil)
precondition((try? ImageExperimentLedger.validate(raw.replacingOccurrences(of: "\"backgrounded\":false", with: "\"backgrounded\":null"))) == nil)
precondition((try? ImageExperimentLedger.validate(raw.replacingOccurrences(of: "device_facts", with: "RAW_SECRET"))) == nil)
let facts = ImageExperimentDeviceFacts.read()
precondition((try? ImageExperimentDeviceFacts.validatedBytes(facts)) != nil)
var wrongFacts = facts; wrongFacts["schemaVersion"] = true
precondition((try? ImageExperimentDeviceFacts.validatedBytes(wrongFacts)) == nil)
wrongFacts = facts; wrongFacts["extra"] = "refused"
precondition((try? ImageExperimentDeviceFacts.validatedBytes(wrongFacts)) == nil)
precondition(facts["apiLevel"] is NSNull && facts["lowRamDevice"] is NSNull)
precondition((facts["unavailableReasons"] as? [String: String])?["apiLevel"] == "unsupported")
print("PASS Swift reader native policy, lifecycle, finite quality and nullable facts")
''')
            output = checked(['xcrun', 'swiftc', '-module-cache-path', base / 'module-cache', ledger, main, '-o', base / 'check'])
            self.assertIn('PASS Swift reader native', checked([base / 'check']))
        checked(['xcrun', 'swiftc', '-frontend', '-parse', IOS / 'ImageExperiment/AppDelegate.swift',
                 IOS / 'ImageExperiment/SceneDelegate.swift', IOS / 'ImageExperimentUITests/ImageExperimentBootstrapTests.swift'])

    def test_kotlin_actual_policy_lifecycle_quality_memtotal_and_sdk_compile(self):
        harness = IOS / 'ImageExperimentUITests/android_diagnostic_harness.py'
        spec = importlib.util.spec_from_file_location('reader_native_toolchain', harness)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        java, compiler, runtime = module.toolchain()
        with tempfile.TemporaryDirectory(prefix='reader-kotlin-', dir='/private/tmp') as temporary:
            base = Path(temporary)
            source = (ANDROID / 'imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentLedger.kt').read_text()
            (base / 'Reader.kt').write_text('package com.tellpal.imageexperiment\nimport org.json.JSONArray\nimport org.json.JSONObject\nimport java.io.File\nimport android.content.Context\n' + source[source.index('object ImageExperimentReaderPolicy {'):])
            stubs = {
                'Context.kt': 'package android.content\nopen class Context { fun getSystemService(name: String): Any? = null; companion object { const val ACTIVITY_SERVICE = "activity" } }',
                'ActivityManager.kt': 'package android.app\nclass ActivityManager { val isLowRamDevice = true; val memoryClass = 128 }',
                'Build.kt': 'package android.os\nobject Build { const val MODEL = "synthetic-model"; val SUPPORTED_ABIS = arrayOf("armeabi-v7a"); const val FINGERPRINT = "physical-observation"; const val HARDWARE = "aruba"; const val PRODUCT = "aruba"; object VERSION { const val SDK_INT = 30 } }; object SystemClock { fun elapsedRealtime() = 1000L }',
            }
            for name, content in stubs.items(): (base / name).write_text(content)
            (base / 'Main.kt').write_text(r'''
package com.tellpal.imageexperiment
import org.json.JSONArray
import org.json.JSONObject
fun entries(batch: String) = JSONArray((1..(if (batch in ImageExperimentReaderPolicy.qualityBatches) 2 else 6)).map { JSONObject(mapOf("caseId" to "reader_page_$it", "mime" to "image/png", "purposeAcceptanceReceipt" to batch)) })
fun main() {
  check(ImageExperimentReaderPolicy.primaryBatches == setOf("reader_resolution_a_r1", "reader_resolution_b_r1", "reader_resolution_c_r1", "reader_resolution_075_r1"))
  check("reader_resolution_075_r1" !in ImageExperimentReaderPolicy.qualityBatches)
  for (batch in ImageExperimentReaderPolicy.batches) {
    check(ImageExperimentReaderPolicy.accepts(batch, "reader_page_1", entries(batch)))
    check(!ImageExperimentReaderPolicy.accepts(batch, "reader_page_2", entries(batch)))
    val wrong = entries(batch); wrong.getJSONObject(1).put("mime", "image/jpeg")
    check(!ImageExperimentReaderPolicy.accepts(batch, "reader_page_1", wrong))
  }
  check(!ImageExperimentReaderPolicy.accepts("native_comparison_r1", "reader_page_1", entries("native_comparison_r1")))
  var now = 1000L; val life = ImageExperimentReaderLifecycle { now }
  life.background(); life.foreground(); check(life.snapshot()["backgroundCount"] == 0)
  life.enable(); life.foreground(); check(life.snapshot()["returnCount"] == 0)
  life.background(); life.background(); check(life.snapshot()["backgroundCount"] == 1)
  check(life.snapshot()["returnedEpochMilliseconds"] == null)
  now = 1700; life.foreground(); life.foreground(); check(life.snapshot()["returnCount"] == 1)
  check(life.snapshot()["returnedEpochMilliseconds"] == 1700L)
  val quality = ImageExperimentReaderQualityLifetime { now }
  check(quality.begin(40000) && quality.deadline == 11700L)
  quality.release(); now = 9700; check(quality.begin(40000) && quality.deadline == 19700L)
  quality.release(); check(!quality.begin(40000))
  val expired = ImageExperimentReaderQualityLifetime { now }; check(expired.begin(40000)); expired.release()
  now += 20000; check(!expired.begin(50000))
  val cancelled = ImageExperimentReaderQualityLifetime { 1000L }; cancelled.invalidate(); check(!cancelled.begin(50000))
  check(!ImageExperimentReaderQualityLifetime { Long.MAX_VALUE }.begin(Long.MAX_VALUE))
  check(ImageExperimentDeviceFacts.memTotalBytes("MemTotal:  1940396 kB\n") == 1986965504L)
  for (raw in listOf("MemTotal: 0 kB\n", "MemTotal: 20 MB\n", "MemTotal: 3 kB\nMemTotal: 4 kB\n", "MemTotal: 9223372036854775807 kB\n", "x".repeat(8193))) check(ImageExperimentDeviceFacts.memTotalBytes(raw) == null)
  val facts = ImageExperimentDeviceFacts.read(android.content.Context())
  check(ImageExperimentDeviceFacts.validatedBytes(facts).isNotEmpty())
  check(runCatching { ImageExperimentDeviceFacts.validatedBytes(facts + ("extra" to "refused")) }.isFailure)
  check(runCatching { ImageExperimentDeviceFacts.validatedBytes(facts + ("schemaVersion" to true)) }.isFailure)
  check(facts["model"] == "synthetic-model" && facts["apiLevel"] == 30)
  check(facts["lowRamDevice"] == null && facts["isPhysicalDevice"] == null)
  check((facts["unavailableReasons"] as Map<*, *>)["isPhysicalDevice"] == "unsupported")
  println("PASS Kotlin reader native policy, lifecycle, finite quality, MemTotal and unavailable facts")
}
''')
            compiler_cp = os.pathsep.join(map(str, compiler))
            runtime_cp = os.pathsep.join(map(str, runtime))
            classes = base / 'classes'
            compile_args = [java, '-cp', compiler_cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-jvm-target', '17']
            checked([*compile_args, '-classpath', runtime_cp, '-d', classes, *base.glob('*.kt')])
            self.assertIn('PASS Kotlin reader native', checked([java, '-cp', str(classes) + os.pathsep + runtime_cp, 'com.tellpal.imageexperiment.MainKt']))
            # Compile the actual full launcher and Activity against installed Android APIs.
            sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk')))
            android_jar = sorted((sdk / 'platforms').glob('android-*/android.jar'))[-1]
            api = base / 'api'; api.mkdir()
            (api / 'FlutterActivity.kt').write_text('package io.flutter.embedding.android\nopen class FlutterActivity : android.app.Activity() { open fun configureFlutterEngine(engine: io.flutter.embedding.engine.FlutterEngine) {} }')
            (api / 'FlutterEngine.kt').write_text('package io.flutter.embedding.engine\nclass FlutterEngine { val dartExecutor = Executor() }; class Executor { val binaryMessenger = Any() }')
            (api / 'MethodChannel.kt').write_text('package io.flutter.plugin.common\nclass MethodCall(val method: String, val arguments: Any?); class MethodChannel(messenger: Any, name: String) { interface Result { fun success(value: Any?); fun error(code: String, message: String?, details: Any?); fun notImplemented() }; fun setMethodCallHandler(handler: (MethodCall, Result) -> Unit) {} }')
            actual = list((ANDROID / 'imageExperiment/kotlin/com/tellpal/imageexperiment').glob('*.kt'))
            actual += list((ANDROID / 'imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment').glob('*.kt'))
            checked([*compile_args, '-classpath', str(android_jar) + os.pathsep + str(runtime[0]), '-d', base / 'api-classes', *actual, *api.glob('*.kt')])


if __name__ == '__main__':
    unittest.main()

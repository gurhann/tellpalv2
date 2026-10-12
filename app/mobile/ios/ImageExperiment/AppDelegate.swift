import Flutter
import UIKit

@main
enum ImageExperimentMain {
  static func main() {
    // Dispatch before FlutterAppDelegate is instantiated, including malformed audit requests.
    let delegate: AnyClass = ProcessInfo.processInfo.environment["IMAGE_EXPERIMENT_INSTALLATION_AUDIT"] != nil
      ? ImageExperimentAuditAppDelegate.self : AppDelegate.self
    UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil, NSStringFromClass(delegate))
  }
}

@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  private(set) var bootstrap: ImageExperimentBootstrap?
  private var channel: FlutterMethodChannel?
  weak var pairingElement: UIView?
  weak var ownedWindow: UIWindow? { didSet { exposeFixtureDiagnostic() } }
  private let fixtureDiagnostic = ImageExperimentFixtureDiagnostic()
  private let ledgerQueue = DispatchQueue(label: "com.tellpal.imageexperiment.ledger")
  private var ledgerWork: DispatchWorkItem?
  private let ledgerGate = ImageExperimentAdmissionGate()

  override func application(_ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    bootstrap = try? ImageExperimentBootstrap(
      encodedMetadata: ProcessInfo.processInfo.environment["IMAGE_EXPERIMENT_METADATA"],
      applicationId: Bundle.main.bundleIdentifier,
      observeRefusal: { [weak self] in self?.publishFixtureDiagnostic($0) })
    bootstrap?.onReachabilityQualified = { [weak self] in self?.exposePairingKey() }
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func exposePairingKey() {
    guard pairingElement == nil, let window = ownedWindow, let bootstrap, bootstrap.isReachabilityQualified else { return }
    let element = UIView(frame: CGRect(x: 8, y: 8, width: 32, height: 32))
    element.isAccessibilityElement = true
    element.accessibilityIdentifier = "imageExperimentPairingKey"
    element.accessibilityLabel = "Experiment pairing"
    element.accessibilityValue = bootstrap.publicKeySpkiBase64
    element.isUserInteractionEnabled = false
    window.addSubview(element)
    pairingElement = element
  }

  private func publishFixtureDiagnostic(_ stage: ImageExperimentFixtureStage) {
    guard ProcessInfo.processInfo.environment[ImageExperimentAuditRequest.environmentKey] == nil,
      ImageExperimentAuditRequest.appIds.contains(Bundle.main.bundleIdentifier ?? "") else { return }
    fixtureDiagnostic.record(stage)
    exposeFixtureDiagnostic()
  }

  private func exposeFixtureDiagnostic() {
    guard let stage = fixtureDiagnostic.stage, let window = ownedWindow,
      window.viewWithTag(73_020) == nil else { return }
    let element = UIView(frame: CGRect(x: 8, y: 48, width: 32, height: 32))
    element.tag = 73_020
    element.isAccessibilityElement = true
    element.accessibilityIdentifier = "imageExperimentFixtureDiagnostic"
    element.accessibilityLabel = "Experiment diagnostic"
    element.accessibilityValue = stage.rawValue
    element.isUserInteractionEnabled = false
    window.addSubview(element)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
    let owned = FlutterMethodChannel(name: "tellpal/image_experiment",
      binaryMessenger: engineBridge.applicationRegistrar.messenger())
    channel = owned
    owned.setMethodCallHandler { [weak self] call, result in
      guard let self else { result(FlutterError(code: "NATIVE_BOOTSTRAP_REFUSED", message: nil, details: nil)); return }
      switch call.method {
      case "reportDiagnostic":
        guard ProcessInfo.processInfo.environment[ImageExperimentAuditRequest.environmentKey] == nil,
          ImageExperimentAuditRequest.appIds.contains(Bundle.main.bundleIdentifier ?? ""),
          self.fixtureDiagnostic.recordDart(call.arguments)
        else { result(false); return }
        self.exposeFixtureDiagnostic()
        result(true)
      case "takeSession":
        guard let bootstrap = self.bootstrap else {
          self.publishFixtureDiagnostic(.nativeGate)
          result(FlutterError(code: "NATIVE_BOOTSTRAP_REFUSED", message: nil, details: nil)); return
        }
        Task { @MainActor in
          do {
            let session = try await bootstrap.takeSession()
            self.pairingElement?.removeFromSuperview()
            self.pairingElement = nil
            bootstrap.launcherOutcome.markSessionHandedOff()
            result(session)
          } catch {
            self.pairingElement?.removeFromSuperview()
            self.pairingElement = nil
            result(FlutterError(code: "NATIVE_BOOTSTRAP_REFUSED", message: nil, details: nil))
          }
        }
      case "revoke":
        let arguments = call.arguments as? [String: Any]
        let requestedResult: String?
        if let arguments, Set(arguments.keys) == ["result"] { requestedResult = arguments["result"] as? String }
        else { requestedResult = nil }
        if let bootstrap = self.bootstrap {
          bootstrap.launcherOutcome.complete(requestedResult, alreadyRevoked: bootstrap.isRevoked)
        }
        _ = self.ledgerGate.revoke()
        self.ledgerWork?.cancel()
        self.bootstrap?.revoke()
        self.pairingElement?.removeFromSuperview()
        self.pairingElement = nil
        if let window = self.ownedWindow {
          let closed = UIView(frame: CGRect(x: 8, y: 8, width: 32, height: 32))
          closed.isAccessibilityElement = true
          closed.accessibilityIdentifier = "imageExperimentClosed"
          closed.accessibilityLabel = "Experiment closed"
          closed.accessibilityValue = self.bootstrap?.launcherOutcome.isSettled == true ? "settled" : "refused"
          closed.isUserInteractionEnabled = false
          window.addSubview(closed)
        }
        result(nil)
      case "storeLedger":
        guard let raw = call.arguments as? String, self.ledgerGate.take() else { result(false); return }
        let cancellation = self.ledgerGate
        let work = DispatchWorkItem { [weak self] in
          guard self != nil else { DispatchQueue.main.async { result(false) }; return }
          let stored = ImageExperimentLedger.store(raw, isCancelled: { cancellation.isRevoked })
          DispatchQueue.main.async {
            if stored { self?.bootstrap?.launcherOutcome.markLedgerStored() }
            result(stored)
          }
        }
        self.ledgerWork = work
        self.ledgerQueue.async(execute: work)
      default: result(FlutterMethodNotImplemented)
      }
    }
  }

  override func applicationWillTerminate(_ application: UIApplication) {
    _ = ledgerGate.revoke()
    ledgerWork?.cancel()
    bootstrap?.revoke()
    channel?.setMethodCallHandler(nil)
    bootstrap = nil
    super.applicationWillTerminate(application)
  }
}

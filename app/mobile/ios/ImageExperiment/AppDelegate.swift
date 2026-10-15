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
  private let memoryQueue = DispatchQueue(label: "com.tellpal.imageexperiment.memory")
  private var memoryBusy = false
  private var admittedExpiry: Int64 = 0
  private var comparisonAdmitted = false
  private var qualityAdmitted = false
  private var expectedQualitySession = ""
  private var expectedQualityCase = ""
  private var expectedQualityGeneration: Int64 = -1
  private let qualityLifetime = ImageExperimentQualityLifetime()
  private var qualityTimer: Timer?
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
            self.admitMeasurement(session)
            bootstrap.launcherOutcome.markSessionHandedOff()
            result(session)
          } catch {
            self.pairingElement?.removeFromSuperview()
            self.pairingElement = nil
            result(FlutterError(code: "NATIVE_BOOTSTRAP_REFUSED", message: nil, details: nil))
          }
        }
      case "revoke":
        self.clearQualityBinding()
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
      case "sampleMemory":
        guard call.arguments == nil, self.measurementActive, self.comparisonAdmitted, !self.memoryBusy
        else { result(FlutterError(code: "MEMORY_REFUSED", message: nil, details: nil)); return }
        self.memoryBusy = true
        self.memoryQueue.async {
          let sample = ImageExperimentMemory.sample()
          DispatchQueue.main.async {
            self.memoryBusy = false
            guard self.measurementActive else {
              result(FlutterError(code: "MEMORY_REFUSED", message: nil, details: nil)); return
            }
            result(sample)
          }
        }
      case "clearQualityBinding":
        guard call.arguments == nil else { result(false); return }
        self.clearQualityBinding()
        result(true)
      case "publishQualityBinding":
        guard self.measurementActive, self.qualityAdmitted,
          let binding = call.arguments as? [String: Any], self.validQualityBinding(binding),
          let window = self.ownedWindow, window.isKeyWindow, window.viewWithTag(73_021) == nil,
          UIApplication.shared.applicationState == .active,
          window.windowScene?.interfaceOrientation == .portrait,
          self.qualityLifetime.begin(sessionExpiresAt: self.admittedExpiry)
        else { result(false); return }
        let element = UIView(frame: CGRect(x: 8, y: 88, width: 1, height: 1))
        element.tag = 73_021
        element.isAccessibilityElement = true
        element.accessibilityIdentifier = "imageExperimentQualityBinding"
        element.accessibilityLabel = "Experiment quality binding"
        let published = binding.merging(["phase": "hold", "orientation": "portrait",
          "nativeContext": "foreground_application", "holdDeadlineEpochMilliseconds": self.qualityLifetime.deadline],
          uniquingKeysWith: { _, new in new })
        guard let data = try? JSONSerialization.data(withJSONObject: published, options: [.sortedKeys])
        else { self.clearQualityBinding(); result(false); return }
        element.accessibilityValue = data.base64EncodedString()
        element.isUserInteractionEnabled = false
        window.addSubview(element)
        self.qualityTimer = Timer.scheduledTimer(withTimeInterval:
          Double(self.qualityLifetime.deadline - Int64(Date().timeIntervalSince1970 * 1000)) / 1000,
          repeats: false) { [weak self] _ in self?.clearQualityBinding() }
        result(true)
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

  private func clearQualityBinding() {
    qualityLifetime.invalidate()
    qualityTimer?.invalidate()
    qualityTimer = nil
    ownedWindow?.viewWithTag(73_021)?.removeFromSuperview()
  }
  override func applicationWillResignActive(_ application: UIApplication) {
    clearQualityBinding()
    super.applicationWillResignActive(application)
  }

  private var measurementActive: Bool {
    bootstrap != nil && bootstrap?.isRevoked == false &&
      Int64(Date().timeIntervalSince1970 * 1000) < admittedExpiry
  }
  private func admitMeasurement(_ session: [String: Any]) {
    guard Bundle.main.bundleIdentifier?.hasSuffix(".release") == true,
      let raw = session["envelope"] as? String,
      let envelope = try? JSONSerialization.jsonObject(with: Data(raw.utf8)) as? [String: Any],
      let batch = envelope["batchId"] as? String,
      ["native_comparison_r1", "native_quality_r1"].contains(batch),
      let encoded = envelope["publicManifestBase64"] as? String,
      let bytes = Data(base64Encoded: encoded),
      let manifest = try? JSONSerialization.jsonObject(with: bytes) as? [String: Any],
      let metadata = session["metadata"] as? [String: Any], let selected = metadata["caseId"] as? String,
      let entries = manifest["entries"] as? [[String: Any]],
      let entry = entries.first(where: { $0["caseId"] as? String == selected }),
      entry["purposeAcceptanceReceipt"] as? String == batch,
      let expires = envelope["expiresAt"] as? NSNumber,
      let generation = envelope["generation"] as? NSNumber,
      let sessionId = envelope["sessionId"] as? String else { return }
    admittedExpiry = expires.int64Value
    comparisonAdmitted = batch == "native_comparison_r1"
    qualityAdmitted = batch == "native_quality_r1"
    expectedQualitySession = ImageExperimentBootstrap.sha256(Data(sessionId.utf8))
    expectedQualityCase = ImageExperimentBootstrap.sha256(Data(selected.utf8))
    expectedQualityGeneration = generation.int64Value
  }
  private func validQualityBinding(_ row: [String: Any]) -> Bool {
    let fields: Set<String> = ["caseSha256", "providerSha256", "attemptSha256", "sessionSha256", "generation",
      "observedSlotWidth", "observedSlotHeight", "observedDpr", "actualDecodeWidth", "actualDecodeHeight"]
    guard Set(row.keys) == fields, row["sessionSha256"] as? String == expectedQualitySession,
      row["caseSha256"] as? String == expectedQualityCase,
      (row["generation"] as? NSNumber)?.int64Value == expectedQualityGeneration,
      let bytes = try? JSONSerialization.data(withJSONObject: ["schemaVersion": 1, "records": [row], "droppedCount": 0]),
      let raw = String(data: bytes, encoding: .utf8), (try? ImageExperimentLedger.validate(raw)) != nil
    else { return false }
    return ["observedSlotWidth", "observedSlotHeight", "observedDpr", "actualDecodeWidth", "actualDecodeHeight"].allSatisfy {
      ((row[$0] as? NSNumber)?.doubleValue ?? 0) > 0
    }
  }

  override func applicationWillTerminate(_ application: UIApplication) {
    clearQualityBinding()
    _ = ledgerGate.revoke()
    ledgerWork?.cancel()
    bootstrap?.revoke()
    channel?.setMethodCallHandler(nil)
    bootstrap = nil
    super.applicationWillTerminate(application)
  }
}

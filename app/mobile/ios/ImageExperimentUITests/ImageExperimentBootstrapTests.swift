import CryptoKit
import Foundation
import XCTest

enum ImageExperimentAuditStage: String, CaseIterable {
  case request = "REQUEST"
  case runnerId = "RUNNER_ID"
  case pluginId = "PLUGIN_ID"
  case pluginPathLexical = "PLUGIN_PATH_LEXICAL"
  case pluginPathCanonical = "PLUGIN_PATH_CANONICAL"
  case runnerHash = "RUNNER_HASH"
  case appLaunch = "APP_LAUNCH"
  case appReport = "APP_REPORT"
  case reportMerge = "REPORT_MERGE"
  case finalDeadline = "FINAL_DEADLINE"

  func refusalLines(reported: Bool, hashStage: ImageExperimentAuditHashStage? = nil) -> [String] {
    let code = self == .runnerHash ? hashStage?.rawValue ?? rawValue : rawValue
    return reported ? [] : ["IMAGE_EXPERIMENT_INSTALLATION_AUDIT_STAGE " + code,
      "IMAGE_EXPERIMENT_INSTALLATION_AUDIT_REFUSED"]
  }
}

@MainActor final class ImageExperimentBootstrapTests: XCTestCase {
  func testPairedPublicKeyLaunch() throws {
    var launcherResult = "refused"
    defer { print("IMAGE_EXPERIMENT_RESULT " + launcherResult) }
    continueAfterFailure = false
    guard ProcessInfo.processInfo.environment[ImageExperimentAuditRequest.environmentKey] == nil,
      let encoded = ProcessInfo.processInfo.environment["IMAGE_EXPERIMENT_METADATA"],
      encoded.utf8.count <= 24_000, let bytes = Data(base64Encoded: encoded), bytes.count <= 16_384,
      let metadata = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
      let appId = metadata["expectedAppId"] as? String,
      ["com.tellpal.tellpalMobilePoc.imageexperiment.profile", "com.tellpal.tellpalMobilePoc.imageexperiment.release"].contains(appId),
      let sessionId = metadata["sessionId"] as? String, let nonce = metadata["launchNonce"] as? String
    else { throw NSError(domain: "NATIVE_LAUNCHER_REFUSED", code: 1) }
    let app = XCUIApplication(bundleIdentifier: appId)
    app.terminate()
    app.launchEnvironment = ["IMAGE_EXPERIMENT_METADATA": encoded]
    if ProcessInfo.processInfo.environment["IMAGE_EXPERIMENT_QUALITY_CAPTURE"] == "1" { XCUIDevice.shared.orientation = .portrait }
    app.launch()
    defer {
      if launcherResult == "refused" {
        let diagnostic = app.otherElements["imageExperimentFixtureDiagnostic"]
        let value = diagnostic.exists ? diagnostic.value as? String : nil
        let stage = value.flatMap { $0.utf8.count <= 48 ? ImageExperimentFixtureStage(rawValue: $0) : nil }
        print((stage ?? .diagnosticUnavailable).line)
      }
      app.terminate()
    }
    let element = app.otherElements["imageExperimentPairingKey"]
    guard element.waitForExistence(timeout: 30), let publicKey = element.value as? String,
      let der = Data(base64Encoded: publicKey), der.count <= 256,
      (try? P256.Signing.PublicKey(derRepresentation: der)) != nil
    else { throw NSError(domain: "NATIVE_LAUNCHER_REFUSED", code: 1) }
    let publicRecord = ["sessionId": sessionId, "launchNonce": nonce,
      "publicKeySpkiBase64": publicKey]
    let publicBytes = try JSONSerialization.data(withJSONObject: publicRecord, options: [.sortedKeys])
    print("IMAGE_EXPERIMENT_PAIRING " + publicBytes.base64EncodedString())
    // Keep the same selected test launcher alive; never attach screenshots or raw responses.
    let deadline = Date().addingTimeInterval(180)
    let closed = app.otherElements["imageExperimentClosed"]
    let capture = ProcessInfo.processInfo.environment["IMAGE_EXPERIMENT_QUALITY_CAPTURE"] == "1"
    var captured = false
    while app.state == .runningForeground && !closed.exists && Date() < deadline {
      let binding = app.otherElements["imageExperimentQualityBinding"]
      if capture && !captured && binding.exists {
        try storeQualityCapture(app: app, binding: binding, sessionId: sessionId, caseId: metadata["caseId"] as? String)
        captured = true
      }
      Thread.sleep(forTimeInterval: 0.1)
    }
    guard !capture || captured else { throw NSError(domain: "QUALITY_CAPTURE_REFUSED", code: 1) }
    guard closed.exists, closed.value as? String == "settled"
    else { throw NSError(domain: "NATIVE_TEARDOWN_REFUSED", code: 1) }
    launcherResult = "settled"
  }

  private func storeQualityCapture(app: XCUIApplication, binding: XCUIElement, sessionId: String, caseId: String?) throws {
    let beforeAt = Int64(Date().timeIntervalSince1970 * 1000)
    guard let before = binding.value as? String, before.utf8.count <= 4096,
      let bytes = Data(base64Encoded: before),
      let row = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
      row["sessionSha256"] as? String == ImageExperimentBootstrap.sha256(Data(sessionId.utf8)),
      let caseId, row["caseSha256"] as? String == ImageExperimentBootstrap.sha256(Data(caseId.utf8)),
      row["phase"] as? String == "hold", row["orientation"] as? String == "portrait",
      row["nativeContext"] as? String == "foreground_application",
      let holdDeadline = row["holdDeadlineEpochMilliseconds"] as? NSNumber,
      beforeAt >= holdDeadline.int64Value - 10_000, beforeAt < holdDeadline.int64Value,
      XCUIDevice.shared.orientation == .portrait, app.state == .runningForeground
      else { throw ImageExperimentRefusal.refused }
    let png = XCUIScreen.main.screenshot().pngRepresentation
    let afterAt = Int64(Date().timeIntervalSince1970 * 1000)
    guard afterAt < holdDeadline.int64Value, XCUIDevice.shared.orientation == .portrait,
      binding.exists, binding.value as? String == before, app.state == .runningForeground,
      !app.otherElements["imageExperimentClosed"].exists else { throw ImageExperimentRefusal.refused }
    let manager = FileManager.default
    let support = manager.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    let directory = support.appendingPathComponent("image_experiment", isDirectory: true)
    try manager.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
    guard directory.resolvingSymlinksInPath().deletingLastPathComponent() == support.resolvingSymlinksInPath()
    else { throw ImageExperimentRefusal.refused }
    let pngPath = directory.appendingPathComponent("quality.png")
    let receiptPath = directory.appendingPathComponent("quality-capture.json")
    for path in [pngPath, receiptPath] where manager.fileExists(atPath: path.path) {
      guard try manager.attributesOfItem(atPath: path.path)[.type] as? FileAttributeType == .typeRegular
      else { throw ImageExperimentRefusal.refused }
      try manager.removeItem(at: path)
    }
    try png.write(to: pngPath, options: [.atomic])
    let receipt: [String: Any] = ["schemaVersion": 1, "before": row, "after": row,
      "beforeReadEpochMilliseconds": beforeAt, "afterReadEpochMilliseconds": afterAt,
      "pngSha256": ImageExperimentBootstrap.sha256(png), "pngBytes": png.count,
      "capture": "private_xctest_screen", "orientation": "portrait", "attachments": false]
    try JSONSerialization.data(withJSONObject: receipt, options: [.sortedKeys]).write(to: receiptPath, options: [.atomic])
    for path in [pngPath, receiptPath] { try manager.setAttributes([.posixPermissions: 0o600], ofItemAtPath: path.path) }
  }

  func testInstallationAudit() throws {
    var reported = false
    var stage = ImageExperimentAuditStage.request
    var hashStage: ImageExperimentAuditHashStage?
    var rootProbe: (() -> ImageExperimentAuditRootViewResult)?
    defer { for line in stage.refusalLines(reported: reported, hashStage: hashStage) { print(line) } }
    continueAfterFailure = false
    do {
      let environment = ProcessInfo.processInfo.environment
      let request = try ImageExperimentAuditRequest(encoded: environment[ImageExperimentAuditRequest.environmentKey],
        brokerMetadataPresent: environment["IMAGE_EXPERIMENT_METADATA"] != nil)
      let runner = Bundle.main
      let plugin = Bundle(for: ImageExperimentBootstrapTests.self)
      let expectedPlugin = runner.bundleURL.appendingPathComponent("PlugIns/ImageExperimentUITests.xctest")
      let canonicalPlugin = runner.bundleURL.resolvingSymlinksInPath().appendingPathComponent("PlugIns/ImageExperimentUITests.xctest")
      stage = .runnerId
      guard runner.bundleIdentifier == request.runnerId else { throw ImageExperimentAuditRefusal.refused }
      stage = .pluginId
      guard plugin.bundleIdentifier == request.appId + ".uitests" else { throw ImageExperimentAuditRefusal.refused }
      stage = .pluginPathLexical
      guard plugin.bundleURL.standardizedFileURL == expectedPlugin.standardizedFileURL
      else { throw ImageExperimentAuditRefusal.refused }
      stage = .pluginPathCanonical
      guard plugin.bundleURL.resolvingSymlinksInPath() == canonicalPlugin.standardizedFileURL
      else { throw ImageExperimentAuditRefusal.refused }
      // Hash the installed runner, including its contained plugin, rather than the test bundle alone.
      stage = .runnerHash
      rootProbe = { ImageExperimentAuditRootViews.probe(runner.bundleURL, checkDeadline: request.checkDeadline) }
      let digest = try ImageExperimentInstallationAudit.hashBundle(runner.bundleURL, checkDeadline: request.checkDeadline,
        observeStage: { hashStage = $0 }, observeEntryViews: { print($0.line) })
      stage = .appLaunch
      let app = XCUIApplication(bundleIdentifier: request.appId)
      app.launchEnvironment = [ImageExperimentAuditRequest.environmentKey: environment[ImageExperimentAuditRequest.environmentKey]!]
      app.launch()
      defer { app.terminate() }
      stage = .appReport
      let element = app.otherElements["imageExperimentInstallationAudit"]
      guard element.waitForExistence(timeout: request.remainingSeconds), let encodedReport = element.value as? String
      else { throw ImageExperimentAuditRefusal.refused }
      stage = .reportMerge
      let result = try ImageExperimentInstallationAudit.report(request: request, encodedAppReport: encodedReport,
        runnerId: runner.bundleIdentifier, digest: digest)
      stage = .finalDeadline
      try request.checkDeadline()
      print("IMAGE_EXPERIMENT_INSTALLATION_AUDIT " + result)
      reported = true
    } catch {
      if stage == .runnerHash, let hashStage, ImageExperimentAuditRootViews.isRootIdentityStage(hashStage),
        let rootProbe { print(rootProbe().line) }
      throw NSError(domain: "INSTALLATION_AUDIT_REFUSED", code: 1)
    }
  }

  func testSyntheticInstallationAuditRequestRefusesUnsafeInput() throws {
    let appId = ImageExperimentAuditRequest.appIds[0]
    let nonce = String(repeating: "a", count: 64)
    func encoded(_ extra: [String: Any] = [:]) throws -> String {
      try ImageExperimentAuditJSON.encode(["schemaVersion": 1, "scope": ImageExperimentAuditRequest.scope,
        "expectedAppId": appId, "nonce": nonce, "deadline": Int64(Date().timeIntervalSince1970 * 1000) + 30_000]
        .merging(extra, uniquingKeysWith: { _, new in new }))
    }
    _ = try ImageExperimentAuditRequest(encoded: encoded(), brokerMetadataPresent: false, applicationId: appId)
    let invalid = [try encoded(["schemaVersion": true]), try encoded(["scope": "image-optimization-native-fixture-r1"]),
      try encoded(["expectedAppId": "com.tellpal.tellpalMobilePoc"]), try encoded(["nonce": String(repeating: "A", count: 64)]),
      try encoded(["deadline": Int64.max]), try encoded(["deadline": Int64.min]), try encoded(["appSha256": nonce])]
    for raw in invalid {
      guard (try? ImageExperimentAuditRequest(encoded: raw, brokerMetadataPresent: false)) == nil
      else { throw ImageExperimentAuditRefusal.refused }
    }
    guard (try? ImageExperimentAuditRequest(encoded: encoded(), brokerMetadataPresent: true)) == nil,
      (try? ImageExperimentAuditRequest(encoded: encoded(), brokerMetadataPresent: false, applicationId: "ordinary")) == nil
    else { throw ImageExperimentAuditRefusal.refused }
    for raw in [#"{"schemaVersion":1,"schemaVersion":1}"#, #"{"deadline":true}"#, #"{"nonce":{}}"#,
      #"{"deadline":01}"#, #"{"deadline":1} trailing"#, #"{"deadline":1,}"#] {
      guard (try? ImageExperimentAuditJSON.parse(Data(raw.utf8))) == nil else { throw ImageExperimentAuditRefusal.refused }
    }
  }

  func testSyntheticInstallationAuditHashesHiddenAndSignatureBytes() throws {
    let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    try FileManager.default.createDirectory(at: root.appendingPathComponent("_CodeSignature"), withIntermediateDirectories: true)
    defer { try? FileManager.default.removeItem(at: root) }
    let files = [".hidden": Data("hidden".utf8), "_CodeSignature/CodeResources": Data("signature".utf8),
      "embedded.mobileprovision": Data("profile".utf8), "z.dSYM": Data("symbols".utf8)]
    for (path, bytes) in files { try bytes.write(to: root.appendingPathComponent(path)) }
    let before = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {})
    var expected = SHA256()
    for path in files.keys.sorted(by: { Array($0.utf8).lexicographicallyPrecedes(Array($1.utf8)) }) {
      let bytes = Data(path.utf8)
      var length = UInt32(bytes.count).bigEndian
      withUnsafeBytes(of: &length) { expected.update(data: Data($0)) }
      expected.update(data: bytes)
      expected.update(data: Data(SHA256.hash(data: files[path]!)))
    }
    guard before.fileCount == 4, before.byteCount == 29,
      before.sha256 == expected.finalize().map({ String(format: "%02x", $0) }).joined()
    else { throw ImageExperimentAuditRefusal.refused }
    try Data("changed".utf8).write(to: root.appendingPathComponent("_CodeSignature/CodeResources"))
    let after = try ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {})
    guard before.sha256 != after.sha256 else { throw ImageExperimentAuditRefusal.refused }
    try FileManager.default.createSymbolicLink(at: root.appendingPathComponent("link"), withDestinationURL: root.appendingPathComponent(".hidden"))
    guard (try? ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: {})) == nil,
      (try? ImageExperimentInstallationAudit.hashBundle(root, checkDeadline: { throw ImageExperimentAuditRefusal.refused })) == nil
    else { throw ImageExperimentAuditRefusal.refused }
  }

  func testSyntheticProofRejectsMutation() throws {
    let key = P256.Signing.PrivateKey()
    let payload = try ImageExperimentBootstrap.signedPayload(scope: "scope", batch: "batch", session: "session",
      nonce: String(repeating: "0", count: 64), challenge: String(repeating: "1", count: 64),
      publicKey: key.publicKey.derRepresentation)
    let signature = try key.signature(for: payload)
    guard key.publicKey.isValidSignature(signature, for: payload),
      !key.publicKey.isValidSignature(signature, for: Data("mutated".utf8))
    else { throw NSError(domain: "NATIVE_PROOF_REFUSED", code: 1) }
  }

  func testSharedProofVector() throws {
    let der = Data(base64Encoded: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEaxfR8uEsQkf4vOblY6RA8ncDfYEt6zOg9KE5RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7LtkBoN79R9Q==")!
    let encoded = Data(base64Encoded: "MEQCIAQ13SrX9he8nb5rlo7aWhB9DEcacbzNKMS6wr8xog4sAiAlRPElM4ZFIt2C6DDdv3fOhR2FFPx+kJ7ETRADBHnDow==")!
    let key = try P256.Signing.PublicKey(derRepresentation: der)
    let signature = try P256.Signing.ECDSASignature(derRepresentation: encoded)
    func payload(batch: String = "vector_batch", challenge: String = String(repeating: "2", count: 64)) throws -> Data {
      try ImageExperimentBootstrap.signedPayload(scope: "image-optimization-native-fixture-r1", batch: batch,
        session: "vector_session", nonce: String(repeating: "1", count: 64), challenge: challenge, publicKey: der)
    }
    guard ImageExperimentBootstrap.sha256(der) == "5cd252fb0ce8932436faf8ccd1040981b89ee4ad6b9fe9e2a2b7e71aacb27cd3",
      key.isValidSignature(signature, for: try payload()),
      !key.isValidSignature(signature, for: try payload(challenge: String(repeating: "2", count: 63) + "3")),
      !key.isValidSignature(signature, for: try payload(batch: "changed_batch")),
      !P256.Signing.PrivateKey().publicKey.isValidSignature(signature, for: try payload()),
      (try? P256.Signing.ECDSASignature(derRepresentation: Data([0x30, 0x01]))) == nil
    else { throw NSError(domain: "NATIVE_PROOF_REFUSED", code: 1) }
  }

  func testOnceOnlyGateAndEarlyRevocation() throws {
    let gate = ImageExperimentAdmissionGate()
    let resultLock = NSLock()
    var winners = 0
    DispatchQueue.concurrentPerform(iterations: 16) { _ in
      if gate.take() { resultLock.lock(); winners += 1; resultLock.unlock() }
    }
    let early = ImageExperimentAdmissionGate()
    guard winners == 1, !gate.take(), gate.revoke(), !gate.revoke(), gate.isRevoked, !gate.take(),
      early.revoke(), !early.take()
    else { throw NSError(domain: "NATIVE_ONCE_REFUSED", code: 1) }
  }

  func testExplicitLauncherOutcome() throws {
    func prepared() -> ImageExperimentLauncherOutcome {
      let outcome = ImageExperimentLauncherOutcome()
      outcome.markSessionHandedOff()
      outcome.markLedgerStored()
      return outcome
    }
    let positive = prepared()
    guard !positive.isSettled else { throw ImageExperimentRefusal.refused }
    positive.complete("settled", alreadyRevoked: false)
    guard positive.isSettled else { throw ImageExperimentRefusal.refused }
    positive.complete("refused", alreadyRevoked: true)
    guard positive.isSettled else { throw ImageExperimentRefusal.refused }
    let refusedResults: [String?] = [nil, "refused", "other"]
    for result in refusedResults {
      let missingPositive = prepared()
      missingPositive.complete(result, alreadyRevoked: false)
      missingPositive.complete("settled", alreadyRevoked: false)
      guard !missingPositive.isSettled else { throw ImageExperimentRefusal.refused }
    }
    let noHandoff = ImageExperimentLauncherOutcome()
    noHandoff.markLedgerStored()
    noHandoff.complete("settled", alreadyRevoked: false)
    let noLedger = ImageExperimentLauncherOutcome()
    noLedger.markSessionHandedOff()
    noLedger.complete("settled", alreadyRevoked: false)
    let earlyRevoked = prepared()
    earlyRevoked.complete("settled", alreadyRevoked: true)
    guard !noHandoff.isSettled, !noLedger.isSettled, !earlyRevoked.isSettled
    else { throw ImageExperimentRefusal.refused }
  }

  func testUntrustedTimeBoundsNeverOverflow() throws {
    let limits: [Int64] = [.min, .max]
    for value in limits {
      guard ImageExperimentTimeBounds.positiveDelta(value, since: 0, maximum: 30_000) == nil,
        ImageExperimentTimeBounds.positiveDelta(0, since: value, maximum: 30_000) == nil,
        !ImageExperimentTimeBounds.allowsSession(issued: value, expires: 1000, now: 0),
        !ImageExperimentTimeBounds.allowsSession(issued: 0, expires: value, now: 0)
      else { throw ImageExperimentRefusal.refused }
    }
    guard ImageExperimentTimeBounds.positiveDelta(.max, since: .min, maximum: 30_000) == nil,
      ImageExperimentTimeBounds.positiveDelta(.min, since: .max, maximum: 30_000) == nil,
      ImageExperimentTimeBounds.positiveDelta(30_000, since: 0, maximum: 30_000) == 30_000,
      ImageExperimentTimeBounds.positiveDelta(30_001, since: 0, maximum: 30_000) == nil,
      ImageExperimentTimeBounds.positiveDelta(0, since: 0, maximum: 30_000) == nil,
      ImageExperimentTimeBounds.positiveDelta(.max, since: .max - 100, maximum: 30_000) == 100,
      ImageExperimentTimeBounds.allowsSession(issued: 0, expires: 180_000, now: 1),
      !ImageExperimentTimeBounds.allowsSession(issued: 0, expires: 180_001, now: 1),
      !ImageExperimentTimeBounds.allowsSession(issued: 2, expires: 100, now: 1),
      !ImageExperimentTimeBounds.allowsSession(issued: 0, expires: 1, now: 1),
      !ImageExperimentTimeBounds.allowsSession(issued: .min, expires: .max, now: 0)
    else { throw ImageExperimentRefusal.refused }
  }

  func testNativeQualityLifetimeRefusesExpiredClearedAndOverflowingHolds() throws {
    var clock: Int64 = 1000
    let lease = ImageExperimentQualityLifetime(now: { clock })
    guard lease.begin(sessionExpiresAt: 31_000), lease.isActive, lease.deadline == 11_000,
      !lease.begin(sessionExpiresAt: 31_000) else { throw ImageExperimentRefusal.refused }
    clock = 11_000
    guard !lease.isActive else { throw ImageExperimentRefusal.refused }
    lease.invalidate()
    guard !lease.isActive, !lease.begin(sessionExpiresAt: 31_000) else { throw ImageExperimentRefusal.refused }
    let revoked = ImageExperimentQualityLifetime(now: { 1000 })
    revoked.invalidate()
    guard !revoked.begin(sessionExpiresAt: 31_000), !revoked.isActive,
      !ImageExperimentQualityLifetime(now: { 1000 }).begin(sessionExpiresAt: 1000),
      !ImageExperimentQualityLifetime(now: { Int64.max }).begin(sessionExpiresAt: Int64.max)
      else { throw ImageExperimentRefusal.refused }
  }

  func testLedgerWhitelistAndShadowedBytes() throws {
    let valid = #"{"schemaVersion":1,"records":[{"event":"status_request","observerOverheadMicroseconds":2,"dpr":2.0,"fit":"contain","firstRasterizedTargetFrame":null}],"droppedCount":0}"#
    guard !(try ImageExperimentLedger.validate(valid)).isEmpty,
      (try? ImageExperimentLedger.validate(valid.replacingOccurrences(of: "status_request", with: "detached_secret"))) == nil,
      (try? ImageExperimentLedger.validate(#"{"schemaVersion":1,"records":[{"idToken":"detached_secret"}],"droppedCount":0}"#)) == nil
    else { throw NSError(domain: "NATIVE_LEDGER_REFUSED", code: 1) }
    let duplicate = #"{"schemaVersion":1,"records":[{"event":"detached_secret","event":"status"}],"droppedCount":0}"#
    if let bytes = try? ImageExperimentLedger.validate(duplicate) {
      guard let output = String(data: bytes, encoding: .utf8), !output.contains("detached_secret")
      else { throw NSError(domain: "NATIVE_LEDGER_REFUSED", code: 1) }
    }
  }

  func testCertificateValidityAndMalformedDer() throws {
    func node(_ tag: UInt8, _ bytes: Data) -> Data {
      precondition(bytes.count < 128)
      return Data([tag, UInt8(bytes.count)]) + bytes
    }
    func fixture(before: String, after: String) -> Data {
      let validity = node(0x30, node(0x17, Data(before.utf8)) + node(0x17, Data(after.utf8)))
      let body = node(0x30, node(0x02, Data([1])) + node(0x30, Data()) + node(0x30, Data()) + validity)
      return node(0x30, body)
    }
    let now = Date(timeIntervalSince1970: 1_790_000_000)
    guard ImageExperimentCertificateValidity.allows(fixture(before: "250101000000Z", after: "270101000000Z"), now: now),
      !ImageExperimentCertificateValidity.allows(fixture(before: "250101000000Z", after: "260101000000Z"), now: now),
      !ImageExperimentCertificateValidity.allows(fixture(before: "270101000000Z", after: "280101000000Z"), now: now),
      !ImageExperimentCertificateValidity.allows(fixture(before: "250231000000Z", after: "270101000000Z"), now: now),
      !ImageExperimentCertificateValidity.allows(Data([0x30, 0x80]), now: now)
    else { throw NSError(domain: "NATIVE_CERTIFICATE_REFUSED", code: 1) }
  }
}

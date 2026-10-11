import CryptoKit
import Foundation
import XCTest

@MainActor final class ImageExperimentBootstrapTests: XCTestCase {
  func testPairedPublicKeyLaunch() throws {
    var launcherResult = "refused"
    defer { print("IMAGE_EXPERIMENT_RESULT " + launcherResult) }
    continueAfterFailure = false
    guard let encoded = ProcessInfo.processInfo.environment["IMAGE_EXPERIMENT_METADATA"],
      encoded.utf8.count <= 24_000, let bytes = Data(base64Encoded: encoded), bytes.count <= 16_384,
      let metadata = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
      let appId = metadata["expectedAppId"] as? String,
      ["com.tellpal.tellpalMobilePoc.imageexperiment.profile", "com.tellpal.tellpalMobilePoc.imageexperiment.release"].contains(appId),
      let sessionId = metadata["sessionId"] as? String, let nonce = metadata["launchNonce"] as? String
    else { throw NSError(domain: "NATIVE_LAUNCHER_REFUSED", code: 1) }
    let app = XCUIApplication(bundleIdentifier: appId)
    app.launchEnvironment = ["IMAGE_EXPERIMENT_METADATA": encoded]
    app.launch()
    defer { app.terminate() }
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
    while app.state == .runningForeground && !closed.exists && Date() < deadline { Thread.sleep(forTimeInterval: 0.1) }
    guard closed.exists, closed.value as? String == "settled"
    else { throw NSError(domain: "NATIVE_TEARDOWN_REFUSED", code: 1) }
    launcherResult = "settled"
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

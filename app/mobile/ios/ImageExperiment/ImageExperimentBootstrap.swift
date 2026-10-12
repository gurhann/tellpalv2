import CryptoKit
import Foundation
import Security

enum ImageExperimentRefusal: Error { case refused }

enum ImageExperimentFixtureStage: String, CaseIterable {
  case nativeMetadata = "NATIVE_METADATA"
  case nativeKey = "NATIVE_KEY"
  case nativeTransport = "NATIVE_TRANSPORT"
  case nativeGate = "NATIVE_GATE"
  case nativeProbe = "NATIVE_PROBE"
  case nativeBackendProbe = "NATIVE_BACKEND_PROBE"
  case nativePairing = "NATIVE_PAIRING"
  case nativeChallenge = "NATIVE_CHALLENGE"
  case nativeProof = "NATIVE_PROOF"
  case nativeClaim = "NATIVE_CLAIM"
  case nativeEnvelope = "NATIVE_ENVELOPE"
  case nativeEnvelopeFields = "NATIVE_ENVELOPE_FIELDS"
  case nativeEnvelopeTimeFields = "NATIVE_ENVELOPE_TIME_FIELDS"
  case nativeEnvelopeIssuedFuture = "NATIVE_ENVELOPE_ISSUED_FUTURE"
  case nativeEnvelopeExpired = "NATIVE_ENVELOPE_EXPIRED"
  case nativeEnvelopeTimeBounds = "NATIVE_ENVELOPE_TIME_BOUNDS"
  case nativeEnvelopeToken = "NATIVE_ENVELOPE_TOKEN"
  case nativeEnvelopeManifest = "NATIVE_ENVELOPE_MANIFEST"
  case nativeEnvelopeBinding = "NATIVE_ENVELOPE_BINDING"
  case nativeHandoff = "NATIVE_HANDOFF"
  case dartNativeHandoff = "DART_NATIVE_HANDOFF"
  case dartNativeResponse = "DART_NATIVE_RESPONSE"
  case dartConfiguration = "DART_CONFIGURATION"
  case dartMode = "DART_MODE"
  case dartComposition = "DART_COMPOSITION"
  case compositionRegistration = "DART_COMPOSITION_REGISTRATION"
  case compositionCache = "DART_COMPOSITION_CACHE"
  case compositionStatus = "DART_COMPOSITION_STATUS"
  case statusAdmission = "DART_STATUS_ADMISSION"
  case statusClient = "DART_STATUS_CLIENT"
  case statusValidation = "DART_STATUS_VALIDATION"
  case clientPreconditions = "DART_CLIENT_PRECONDITIONS"
  case clientConstruction = "DART_CLIENT_CONSTRUCTION"
  case trustContext = "DART_TRUST_CONTEXT"
  case certificateImport = "DART_CERTIFICATE_IMPORT"
  case certificateImportDerFailPemPass = "DART_CERT_IMPORT_DER_FAIL_PEM_PASS"
  case certificateImportDerPassPemFail = "DART_CERT_IMPORT_DER_PASS_PEM_FAIL"
  case certificateImportBothFail = "DART_CERT_IMPORT_BOTH_FAIL"
  case certificateImportBothPass = "DART_CERT_IMPORT_BOTH_PASS"
  case certificateImportUnavailable = "DART_CERT_IMPORT_UNAVAILABLE"
  case requestPreconditions = "DART_REQUEST_PRECONDITIONS"
  case tlsConnect = "DART_TLS_CONNECT"
  case tlsHandshake = "DART_TLS_HANDSHAKE"
  case tlsPinValidity = "DART_TLS_PIN_VALIDITY"
  case outgoingRequest = "DART_OUTGOING_REQUEST"
  case responseValidation = "DART_RESPONSE_VALIDATION"
  case responseBody = "DART_RESPONSE_BODY"
  case configurationEnvelope = "DART_CONFIG_ENVELOPE"
  case configurationFields = "DART_CONFIG_FIELDS"
  case configurationIdentity = "DART_CONFIG_IDENTITY"
  case configurationIds = "DART_CONFIG_IDS"
  case configurationLauncher = "DART_CONFIG_LAUNCHER"
  case configurationTime = "DART_CONFIG_TIME"
  case configurationEnvironment = "DART_CONFIG_ENVIRONMENT"
  case configurationOrigin = "DART_CONFIG_ORIGIN"
  case configurationApp = "DART_CONFIG_APP"
  case configurationHashes = "DART_CONFIG_HASHES"
  case configurationCertificate = "DART_CONFIG_CERTIFICATE"
  case configurationToken = "DART_CONFIG_TOKEN"
  case configurationClaims = "DART_CONFIG_CLAIMS"
  case configurationManifestHash = "DART_CONFIG_MANIFEST_HASH"
  case configurationManifest = "DART_CONFIG_MANIFEST"
  case configurationBindings = "DART_CONFIG_BINDINGS"
  case configurationCapacity = "DART_CONFIG_CAPACITY"
  case configurationFixture = "DART_CONFIG_FIXTURE"
  case configurationGeometry = "DART_CONFIG_GEOMETRY"
  case configurationSelection = "DART_CONFIG_SELECTION"
  case diagnosticUnavailable = "DIAGNOSTIC_UNAVAILABLE"

  var line: String { "IMAGE_EXPERIMENT_FIXTURE_STAGE " + rawValue }
  var isDart: Bool { rawValue.hasPrefix("DART_") }
}

// Retain only the first fixed failure code; diagnostics cannot complete admission or teardown.
final class ImageExperimentFixtureDiagnostic {
  private(set) var stage: ImageExperimentFixtureStage?
  func record(_ value: ImageExperimentFixtureStage) {
    if stage == nil { stage = value }
  }
  func recordDart(_ value: Any?) -> Bool {
    guard let code = value as? String, code.utf8.count <= 48,
      let stage = ImageExperimentFixtureStage(rawValue: code), stage.isDart else { return false }
    record(stage)
    return true
  }
}

/// Untrusted epoch integers must never trap while checking finite session bounds.
enum ImageExperimentTimeBounds {
  static func positiveDelta(_ later: Int64, since earlier: Int64, maximum: Int64) -> Int64? {
    let (delta, overflow) = later.subtractingReportingOverflow(earlier)
    guard !overflow, delta > 0, delta <= maximum else { return nil }
    return delta
  }
  static func allowsSession(issued: Int64, expires: Int64, now: Int64) -> Bool {
    issued <= now && positiveDelta(expires, since: now, maximum: 180_000) != nil &&
      positiveDelta(expires, since: issued, maximum: 180_000) != nil
  }
}

/// Enforces leaf validity explicitly, including when that same leaf is a private trust anchor.
enum ImageExperimentCertificateValidity {
  private struct Reader {
    let bytes: [UInt8]
    var index = 0
    mutating func node(_ expected: UInt8? = nil) throws -> (tag: UInt8, value: [UInt8]) {
      guard index + 2 <= bytes.count else { throw ImageExperimentRefusal.refused }
      let tag = bytes[index]
      index += 1
      guard expected == nil || expected == tag else { throw ImageExperimentRefusal.refused }
      let first = Int(bytes[index])
      index += 1
      var length = first
      if first >= 128 {
        let count = first & 127
        guard (1...4).contains(count), index + count <= bytes.count, bytes[index] != 0 else { throw ImageExperimentRefusal.refused }
        length = 0
        for _ in 0..<count { length = (length << 8) | Int(bytes[index]); index += 1 }
        guard length >= 128 else { throw ImageExperimentRefusal.refused }
      }
      guard length <= bytes.count - index else { throw ImageExperimentRefusal.refused }
      let result = Array(bytes[index..<(index + length)])
      index += length
      return (tag, result)
    }
  }

  static func allows(_ der: Data, now: Date = Date()) -> Bool {
    do {
      guard der.count <= 8192 else { return false }
      var certificate = Reader(bytes: Array(der))
      let outer = try certificate.node(0x30)
      guard certificate.index == certificate.bytes.count else { return false }
      var content = Reader(bytes: outer.value)
      var body = Reader(bytes: try content.node(0x30).value)
      if body.bytes.first == 0xa0 { _ = try body.node(0xa0) }
      _ = try body.node(0x02) // Serial number.
      _ = try body.node(0x30) // Signature algorithm.
      _ = try body.node(0x30) // Issuer name.
      var validity = Reader(bytes: try body.node(0x30).value)
      let beforeNode = try validity.node(), afterNode = try validity.node()
      guard validity.index == validity.bytes.count,
        let before = date(tag: beforeNode.tag, bytes: beforeNode.value),
        let after = date(tag: afterNode.tag, bytes: afterNode.value), before < after
      else { return false }
      return before <= now && now <= after
    } catch { return false }
  }

  private static func date(tag: UInt8, bytes: [UInt8]) -> Date? {
    guard (tag == 0x17 && bytes.count == 13) || (tag == 0x18 && bytes.count == 15),
      bytes.last == 90, bytes.dropLast().allSatisfy({ (48...57).contains($0) }) else { return nil }
    let digits = bytes.dropLast().map { Int($0 - 48) }
    let yearDigits = tag == 0x17 ? 2 : 4
    var year = digits.prefix(yearDigits).reduce(0) { $0 * 10 + $1 }
    if yearDigits == 2 { year += year >= 50 ? 1900 : 2000 }
    func pair(_ offset: Int) -> Int { digits[yearDigits + offset] * 10 + digits[yearDigits + offset + 1] }
    let month = pair(0), day = pair(2), hour = pair(4), minute = pair(6), second = pair(8)
    guard (1...12).contains(month), (1...31).contains(day), (0...23).contains(hour),
      (0...59).contains(minute), (0...59).contains(second) else { return nil }
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0)!
    let components = DateComponents(year: year, month: month, day: day, hour: hour, minute: minute, second: second)
    guard let date = calendar.date(from: components) else { return nil }
    let actual = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
    guard actual.year == year, actual.month == month, actual.day == day, actual.hour == hour,
      actual.minute == minute, actual.second == second else { return nil }
    return date
  }
}

/// Admission itself has a single winner even when a test races take against revoke.
final class ImageExperimentAdmissionGate: @unchecked Sendable {
  private let lock = NSLock()
  private var state = 0
  func take() -> Bool {
    lock.lock(); defer { lock.unlock() }
    guard state == 0 else { return false }
    state = 1
    return true
  }
  func revoke() -> Bool {
    lock.lock(); defer { lock.unlock() }
    guard state != 2 else { return false }
    state = 2
    return true
  }
  var isRevoked: Bool {
    lock.lock(); defer { lock.unlock() }
    return state == 2
  }
}

/// Teardown alone never supplies a positive fixture result.
final class ImageExperimentLauncherOutcome: @unchecked Sendable {
  private let lock = NSLock()
  private var handedOff = false
  private var ledgerStored = false
  private var completed = false
  private var settled = false
  func markSessionHandedOff() {
    lock.lock(); defer { lock.unlock() }
    if !completed { handedOff = true }
  }
  func markLedgerStored() {
    lock.lock(); defer { lock.unlock() }
    if !completed { ledgerStored = true }
  }
  func complete(_ requestedResult: String?, alreadyRevoked: Bool) {
    lock.lock(); defer { lock.unlock() }
    guard !completed else { return }
    settled = requestedResult == "settled" && handedOff && ledgerStored && !alreadyRevoked
    completed = true
  }
  var isSettled: Bool {
    lock.lock(); defer { lock.unlock() }
    return completed && settled
  }
}

/// Holds only a finite launch key and hands the response to Dart exactly once.
@MainActor final class ImageExperimentBootstrap {
  let launcherOutcome = ImageExperimentLauncherOutcome()
  private let metadata: [String: Any]
  private var key: P256.Signing.PrivateKey?
  let publicKeySpkiBase64: String
  private let gate = ImageExperimentAdmissionGate()
  var isRevoked: Bool { gate.isRevoked }
  private(set) var isReachabilityQualified = false
  var onReachabilityQualified: (() -> Void)?
  private let started = ProcessInfo.processInfo.systemUptime
  private let transport: ImageExperimentPinnedTransport
  private let observeRefusal: (ImageExperimentFixtureStage) -> Void
  private var refusalStage = ImageExperimentFixtureStage.nativeGate

  init(encodedMetadata: String?, applicationId: String?,
    observeRefusal: @escaping (ImageExperimentFixtureStage) -> Void = { _ in }) throws {
    self.observeRefusal = observeRefusal
    var stage = ImageExperimentFixtureStage.nativeMetadata
    do {
      guard let encodedMetadata, encodedMetadata.utf8.count <= 24_000,
        let bytes = Data(base64Encoded: encodedMetadata), bytes.count <= 16_384,
        let object = try JSONSerialization.jsonObject(with: bytes) as? [String: Any]
      else { throw ImageExperimentRefusal.refused }
      try Self.validateMetadata(object, applicationId: applicationId)
      metadata = object
      stage = .nativeKey
      let generated = P256.Signing.PrivateKey()
      key = generated
      publicKeySpkiBase64 = generated.publicKey.derRepresentation.base64EncodedString()
      stage = .nativeTransport
      transport = try ImageExperimentPinnedTransport(metadata: object)
    } catch {
      observeRefusal(stage)
      throw error
    }
  }

  func takeSession() async throws -> [String: Any] {
    guard gate.take() else { observeRefusal(.nativeGate); throw ImageExperimentRefusal.refused }
    defer { key = nil }
    do {
      refusalStage = .nativeProbe
      _ = try await request("GET", "/experiment/v1/probe")
      refusalStage = .nativeBackendProbe
      _ = try await request("GET", "/experiment/v1/backend-probe")
      isReachabilityQualified = true
      onReachabilityQualified?()
      refusalStage = .nativePairing
      try await awaitPairing()
      refusalStage = .nativeChallenge
      let challengeBytes = try await request("POST", "/experiment/v1/bootstrap/challenge", body: publicProof())
      guard let challenge = try JSONSerialization.jsonObject(with: challengeBytes) as? [String: Any]
      else { throw ImageExperimentRefusal.refused }
      for field in ["scope", "batchId", "sessionId", "launchNonce"] {
        guard challenge[field] as? String == metadata[field] as? String else { throw ImageExperimentRefusal.refused }
      }
      let challengeNow = Self.now
      guard let value = challenge["challenge"] as? String, Self.matches(value, "^[a-f0-9]{64}$"),
        let expires = Self.integer(challenge["expiresAt"]),
        ImageExperimentTimeBounds.positiveDelta(expires, since: challengeNow, maximum: 30_000) != nil,
        let retained = key
      else { throw ImageExperimentRefusal.refused }
      refusalStage = .nativeProof
      var proof = publicProof()
      proof["challenge"] = value
      let payload = try Self.signedPayload(scope: string("scope"), batch: string("batchId"),
        session: string("sessionId"), nonce: string("launchNonce"), challenge: value,
        publicKey: retained.publicKey.derRepresentation)
      let signature = try retained.signature(for: payload).derRepresentation
      guard signature.count <= 128 else { throw ImageExperimentRefusal.refused }
      proof["signatureBase64"] = signature.base64EncodedString()
      refusalStage = .nativeClaim
      let bytes = try await request("POST", "/experiment/v1/bootstrap/claim", body: proof)
      refusalStage = .nativeEnvelope
      try validateEnvelope(bytes)
      refusalStage = .nativeHandoff
      try checkAlive()
      guard let envelope = String(data: bytes, encoding: .utf8) else { throw ImageExperimentRefusal.refused }
      return ["envelope": envelope, "metadata": metadata]
    } catch {
      observeRefusal(refusalStage)
      revoke()
      throw ImageExperimentRefusal.refused
    }
  }

  func revoke() {
    guard gate.revoke() else { return }
    key = nil
    onReachabilityQualified = nil
    transport.close()
  }

  private func string(_ field: String) -> String { metadata[field] as? String ?? "" }
  private func publicProof() -> [String: Any] {
    var proof: [String: Any] = ["publicKeySpkiBase64": publicKeySpkiBase64]
    for field in ["scope", "batchId", "sessionId", "launchNonce"] { proof[field] = string(field) }
    return proof
  }

  private func awaitPairing() async throws {
    guard let publicKey = Data(base64Encoded: publicKeySpkiBase64) else { throw ImageExperimentRefusal.refused }
    while true {
      try checkAlive()
      let bytes = try await request("GET", "/experiment/v1/bootstrap/paired?sessionId=\(string("sessionId"))&launchNonce=\(string("launchNonce"))")
      guard let response = try JSONSerialization.jsonObject(with: bytes) as? [String: Any]
      else { throw ImageExperimentRefusal.refused }
      if response["paired"] as? Bool == true {
        guard response["publicKeySha256"] as? String == Self.sha256(publicKey) else { throw ImageExperimentRefusal.refused }
        return
      }
      try await Task.sleep(nanoseconds: 100_000_000)
    }
  }

  private func checkAlive() throws {
    let now = Self.now
    guard !gate.isRevoked, ProcessInfo.processInfo.systemUptime - started < 30,
      let deadline = Self.integer(metadata["deadline"]), now < deadline
    else { throw ImageExperimentRefusal.refused }
  }

  private func request(_ method: String, _ path: String, body: [String: Any]? = nil) async throws -> Data {
    try checkAlive()
    let now = Self.now
    guard let deadline = Self.integer(metadata["deadline"]),
      let remaining = ImageExperimentTimeBounds.positiveDelta(deadline, since: now, maximum: 30_000),
      let url = URL(string: string("brokerOrigin") + path)
    else { throw ImageExperimentRefusal.refused }
    let timeout = min(10, min(30 - (ProcessInfo.processInfo.systemUptime - started), Double(remaining) / 1000))
    guard timeout > 0 else { throw ImageExperimentRefusal.refused }
    var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: timeout)
    request.httpMethod = method
    request.setValue(string("sessionId"), forHTTPHeaderField: "X-Experiment-Session")
    request.setValue(string("launchNonce"), forHTTPHeaderField: "X-Experiment-Launch-Nonce")
    if let body {
      let bytes = try JSONSerialization.data(withJSONObject: body)
      guard bytes.count <= 4096 else { throw ImageExperimentRefusal.refused }
      request.httpBody = bytes
      request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    }
    let bytes = try await transport.send(request)
    try checkAlive()
    guard String(data: bytes, encoding: .utf8) != nil else { throw ImageExperimentRefusal.refused }
    return bytes
  }

  private func validateEnvelope(_ bytes: Data) throws {
    let now = Self.now
    refusalStage = .nativeEnvelopeFields
    guard bytes.count <= 65_536,
      let envelope = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
      Self.integer(envelope["schemaVersion"]) == 1,
      let generation = Self.integer(envelope["generation"]), generation >= 0
    else { throw ImageExperimentRefusal.refused }
    refusalStage = .nativeEnvelopeTimeFields
    guard let issued = Self.integer(envelope["issuedAt"]), let expires = Self.integer(envelope["expiresAt"])
    else { throw ImageExperimentRefusal.refused }
    refusalStage = .nativeEnvelopeTimeBounds
    if issued > now { refusalStage = .nativeEnvelopeIssuedFuture }
    else if expires <= now { refusalStage = .nativeEnvelopeExpired }
    guard ImageExperimentTimeBounds.allowsSession(issued: issued, expires: expires, now: now)
    else { throw ImageExperimentRefusal.refused }
    refusalStage = .nativeEnvelopeToken
    guard let token = envelope["idToken"] as? String, (1...8192).contains(token.utf8.count)
    else { throw ImageExperimentRefusal.refused }
    refusalStage = .nativeEnvelopeManifest
    guard let publicManifest = envelope["publicManifestBase64"] as? String,
      let manifestBytes = Data(base64Encoded: publicManifest),
      Self.sha256(manifestBytes) == string("publicManifestSha256")
    else { throw ImageExperimentRefusal.refused }
    refusalStage = .nativeEnvelopeBinding
    for field in ["scope", "batchId", "sessionId", "expectedUid", "backendEnvironment", "brokerOrigin",
      "brokerCertificateSha256", "manifestSha256", "publicManifestSha256"] {
      guard envelope[field] as? String == string(field) else { throw ImageExperimentRefusal.refused }
    }
  }

  static var now: Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
  static func sha256(_ data: Data) -> String { SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined() }
  static func matches(_ value: String, _ pattern: String) -> Bool { value.range(of: pattern, options: .regularExpression) != nil }
  static func integer(_ value: Any?) -> Int64? {
    guard let value = value as? NSNumber, CFGetTypeID(value) != CFBooleanGetTypeID(),
      ["q", "i", "s", "l"].contains(String(cString: value.objCType)) else { return nil }
    return value.int64Value
  }

  static func signedPayload(scope: String, batch: String, session: String, nonce: String,
    challenge: String, publicKey: Data) throws -> Data {
    guard [scope, batch, session].allSatisfy({ matches($0, "^[A-Za-z0-9_-]{1,64}$") }),
      [nonce, challenge].allSatisfy({ matches($0, "^[a-f0-9]{64}$") }), (1...256).contains(publicKey.count)
    else { throw ImageExperimentRefusal.refused }
    return Data("tellpal-native-fixture-v1\n\(scope)\n\(batch)\n\(session)\n\(nonce)\n\(challenge)\n\(sha256(publicKey))\n".utf8)
  }

  private static func validateMetadata(_ data: [String: Any], applicationId: String?) throws {
    let current = now
    let fields: Set<String> = ["schemaVersion", "scope", "batchId", "sessionId", "caseId", "launchNonce", "deadline",
      "expectedUid", "expectedAppId", "firebaseProjectId", "backendEnvironment", "brokerOrigin",
      "brokerCertificateDerBase64", "brokerCertificateSha256", "manifestSha256", "publicManifestSha256"]
    guard Set(data.keys) == fields, integer(data["schemaVersion"]) == 1,
      data["scope"] as? String == "image-optimization-native-fixture-r1",
      data["expectedUid"] as? String == "jLRypTmoBrhhX21HsDJojurcKTC3",
      let app = data["expectedAppId"] as? String, app == applicationId,
      ["com.tellpal.tellpalMobilePoc.imageexperiment.profile", "com.tellpal.tellpalMobilePoc.imageexperiment.release"].contains(app),
      let environment = data["backendEnvironment"] as? String, ["local", "test"].contains(environment),
      let project = data["firebaseProjectId"] as? String, matches(project, "^[a-z][a-z0-9-]{4,62}$"),
      let deadline = integer(data["deadline"]),
      ImageExperimentTimeBounds.positiveDelta(deadline, since: current, maximum: 30_000) != nil,
      let origin = data["brokerOrigin"] as? String, let components = URLComponents(string: origin),
      components.scheme == "https", components.host != nil, components.user == nil, components.password == nil,
      components.path.isEmpty, components.query == nil, components.fragment == nil,
      components.port == nil || (1...65535).contains(components.port!),
      let encodedCert = data["brokerCertificateDerBase64"] as? String, let cert = Data(base64Encoded: encodedCert),
      (1...8192).contains(cert.count), sha256(cert) == data["brokerCertificateSha256"] as? String
    else { throw ImageExperimentRefusal.refused }
    for field in ["scope", "batchId", "sessionId", "caseId", "expectedUid"] {
      guard let value = data[field] as? String, matches(value, "^[A-Za-z0-9_-]{1,64}$") else { throw ImageExperimentRefusal.refused }
    }
    for field in ["launchNonce", "brokerCertificateSha256", "manifestSha256", "publicManifestSha256"] {
      guard let value = data[field] as? String, matches(value, "^[a-f0-9]{64}$") else { throw ImageExperimentRefusal.refused }
    }
  }
}

/// A session-owned streaming transport bounds bytes and validates certificate, anchor and host.
final class ImageExperimentPinnedTransport: NSObject, URLSessionDataDelegate {
  private struct Pending {
    let continuation: CheckedContinuation<Data, Error>
    var data = Data()
  }
  private let certificate: SecCertificate
  private let pin: String
  private let host: String
  private let lock = NSLock()
  private var pending: [Int: Pending] = [:]
  private var closed = false
  private var session: URLSession!

  init(metadata: [String: Any]) throws {
    guard let encoded = metadata["brokerCertificateDerBase64"] as? String,
      let bytes = Data(base64Encoded: encoded), let certificate = SecCertificateCreateWithData(nil, bytes as CFData),
      ImageExperimentCertificateValidity.allows(bytes),
      let pin = metadata["brokerCertificateSha256"] as? String,
      let origin = metadata["brokerOrigin"] as? String, let host = URL(string: origin)?.host
    else { throw ImageExperimentRefusal.refused }
    self.certificate = certificate
    self.pin = pin
    self.host = host
    super.init()
    let config = URLSessionConfiguration.ephemeral
    config.timeoutIntervalForRequest = 10
    config.timeoutIntervalForResource = 10
    config.httpCookieStorage = nil
    config.httpShouldSetCookies = false
    config.urlCache = nil
    config.httpMaximumConnectionsPerHost = 1
    let queue = OperationQueue()
    queue.maxConcurrentOperationCount = 1
    session = URLSession(configuration: config, delegate: self, delegateQueue: queue)
  }

  func send(_ request: URLRequest) async throws -> Data {
    try await withCheckedThrowingContinuation { continuation in
      lock.lock()
      guard !closed else { lock.unlock(); continuation.resume(throwing: ImageExperimentRefusal.refused); return }
      let task = session.dataTask(with: request)
      pending[task.taskIdentifier] = Pending(continuation: continuation)
      lock.unlock()
      task.resume()
    }
  }

  func close() {
    lock.lock()
    guard !closed else { lock.unlock(); return }
    closed = true
    let owned = pending.values.map(\.continuation)
    pending.removeAll()
    lock.unlock()
    session.invalidateAndCancel()
    owned.forEach { $0.resume(throwing: ImageExperimentRefusal.refused) }
  }

  func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge,
    completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
    guard challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust,
      challenge.protectionSpace.host == host, let trust = challenge.protectionSpace.serverTrust,
      let chain = SecTrustCopyCertificateChain(trust) as? [SecCertificate], let leaf = chain.first,
      SHA256.hash(data: SecCertificateCopyData(leaf) as Data).map({ String(format: "%02x", $0) }).joined() == pin,
      ImageExperimentCertificateValidity.allows(SecCertificateCopyData(leaf) as Data),
      SecTrustSetPolicies(trust, SecPolicyCreateSSL(true, host as CFString)) == errSecSuccess,
      SecTrustSetAnchorCertificates(trust, [certificate] as CFArray) == errSecSuccess,
      SecTrustSetAnchorCertificatesOnly(trust, true) == errSecSuccess,
      SecTrustSetVerifyDate(trust, Date() as CFDate) == errSecSuccess,
      SecTrustEvaluateWithError(trust, nil)
    else { completionHandler(.cancelAuthenticationChallenge, nil); return }
    completionHandler(.useCredential, URLCredential(trust: trust))
  }

  func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }

  func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse,
    completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
    guard let response = response as? HTTPURLResponse, response.statusCode == 200,
      response.expectedContentLength <= 65_536 else { completionHandler(.cancel); return }
    completionHandler(.allow)
  }

  func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
    lock.lock()
    guard var owned = pending[dataTask.taskIdentifier], owned.data.count + data.count <= 65_536
    else { lock.unlock(); dataTask.cancel(); return }
    owned.data.append(data)
    pending[dataTask.taskIdentifier] = owned
    lock.unlock()
  }

  func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
    lock.lock()
    let owned = pending.removeValue(forKey: task.taskIdentifier)
    lock.unlock()
    guard let owned else { return }
    if error != nil { owned.continuation.resume(throwing: ImageExperimentRefusal.refused) }
    else { owned.continuation.resume(returning: owned.data) }
  }
}

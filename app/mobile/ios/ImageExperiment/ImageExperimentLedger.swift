import Foundation

/// Validates every field again and writes only sanitized projection bytes in this app's container.
enum ImageExperimentLedger {
  private static let hashes: Set<String> = ["caseSha256", "providerSha256", "attemptSha256"]
  private static let integers: Set<String> = ["generation", "monotonicMicroseconds", "requestCount", "retryCount", "bodyBytes",
    "sourceWidth", "sourceHeight", "requiredWidth", "requiredHeight", "decodeWidth", "decodeHeight",
    "actualDecodeWidth", "actualDecodeHeight", "frameNumber", "dartHeapBytes", "dartExternalBytes",
    "decodedLruBytes", "decodedLiveCount", "decodedPendingCount", "processBytes", "nativeBytes",
    "graphicsBytes", "diskBytes", "transferBytes", "firstRasterizedTargetFrame", "observerOverheadMicroseconds"]
  private static let doubles: Set<String> = ["logicalSlotWidth", "logicalSlotHeight", "dpr", "alignmentX", "alignmentY"]
  private static let booleans: Set<String> = ["encodedVerified", "sourceShortage", "physicalPresentationVerified"]
  private static let strings: [String: Set<String>] = [
    "clockDomain": ["dart_monotonic"], "clockUnit": ["microseconds"], "fit": ["contain", "cover"],
    "role": ["isolated_raster_fixture"],
    "event": ["request", "body", "encoded_verified", "decoded", "draw", "failure", "revoke", "cache_state", "status", "status_request", "status_body", "ownership"],
    "ownership": ["owned", "settling", "settled", "released", "unavailable"],
    "reason": ["none", "invalid_fixture", "session_inactive", "status_unavailable", "status_revoked", "missing_counter",
      "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity",
      "route_departed", "stale_attempt", "internal_settlement_unavailable", "export_capacity", "pin_refused", "transport_refused", "decode_refused", "cleanup_unavailable"],
    "counterReason": ["missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity", "internal_settlement_unavailable"],
    "cacheState": ["network-cold", "disk-warm/decoded-cold", "decoded-warm", "unavailable"]]

  static func validate(_ raw: String) throws -> Data {
    let bytes = Data(raw.utf8)
    guard bytes.count <= 262_144,
      let root = try JSONSerialization.jsonObject(with: bytes) as? [String: Any],
      Set(root.keys) == ["schemaVersion", "records", "droppedCount"], integer(root["schemaVersion"]) == 1,
      let dropped = integer(root["droppedCount"]), dropped >= 0,
      let records = root["records"] as? [[String: Any]], records.count <= 512
    else { throw ImageExperimentRefusal.refused }
    for row in records {
      for (field, value) in row {
        let valid: Bool
        if hashes.contains(field) {
          valid = (value as? String)?.range(of: "^[a-f0-9]{64}$", options: .regularExpression) != nil
        } else if integers.contains(field) {
          valid = value is NSNull || (integer(value).map { $0 >= (field == "frameNumber" ? -1 : 0) } ?? false)
        } else if doubles.contains(field) {
          valid = (value as? NSNumber).map { CFGetTypeID($0) != CFBooleanGetTypeID() && $0.doubleValue.isFinite } ?? false
        } else if booleans.contains(field) {
          valid = (value as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } ?? false
        } else if let allowed = strings[field] {
          valid = (value as? String).map { allowed.contains($0) } ?? false
        } else { valid = false }
        guard valid else { throw ImageExperimentRefusal.refused }
      }
    }
    // Re-serialization discards duplicate-key shadowed strings from the original input.
    let sanitized = try JSONSerialization.data(withJSONObject: root, options: [.sortedKeys])
    guard sanitized.count <= 262_144 else { throw ImageExperimentRefusal.refused }
    return sanitized
  }

  private static func integer(_ value: Any?) -> Int64? {
    guard let value = value as? NSNumber, CFGetTypeID(value) != CFBooleanGetTypeID(),
      ["q", "i", "s", "l"].contains(String(cString: value.objCType)) else { return nil }
    return value.int64Value
  }

  static func store(_ raw: String, isCancelled: () -> Bool = { false }) -> Bool {
    var temporary: URL?
    let started = ProcessInfo.processInfo.systemUptime
    func active() -> Bool { !isCancelled() && ProcessInfo.processInfo.systemUptime - started < 2 }
    defer { if let temporary { try? FileManager.default.removeItem(at: temporary) } }
    do {
      guard active() else { return false }
      let bytes = try validate(raw)
      guard active() else { return false }
      let manager = FileManager.default
      guard let support = manager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return false }
      let directory = support.appendingPathComponent("image_experiment", isDirectory: true)
      try manager.createDirectory(at: directory, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
      guard directory.resolvingSymlinksInPath().deletingLastPathComponent() == support.resolvingSymlinksInPath(),
        try manager.attributesOfItem(atPath: directory.path)[.type] as? FileAttributeType == .typeDirectory else { return false }
      try manager.setAttributes([.posixPermissions: 0o700], ofItemAtPath: directory.path)
      let destination = directory.appendingPathComponent("ledger.json")
      if manager.fileExists(atPath: destination.path) {
        guard try manager.attributesOfItem(atPath: destination.path)[.type] as? FileAttributeType == .typeRegular else { return false }
      }
      let selected = directory.appendingPathComponent("ledger-\(UUID().uuidString).tmp")
      temporary = selected
      guard manager.createFile(atPath: selected.path, contents: bytes, attributes: [.posixPermissions: 0o600]) else { return false }
      guard active() else { return false }
      if manager.fileExists(atPath: destination.path) { try manager.removeItem(at: destination) }
      try manager.moveItem(at: selected, to: destination)
      return true
    } catch { return false }
  }
}

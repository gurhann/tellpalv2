import Foundation
import Darwin

/// Validates every field again and writes only sanitized projection bytes in this app's container.
enum ImageExperimentLedger {
  private static let hashes: Set<String> = ["caseSha256", "providerSha256", "attemptSha256", "sessionSha256", "sourceSha256", "referenceSha256", "settingsSha256", "scopeSha256", "modelSha256", "abisSha256", "deviceFactsSha256"]
  private static let integers: Set<String> = ["generation", "monotonicMicroseconds", "requestCount", "retryCount", "bodyBytes", "sourceWidth", "sourceHeight", "requiredWidth", "requiredHeight", "decodeWidth", "decodeHeight", "actualDecodeWidth", "actualDecodeHeight", "frameNumber", "dartHeapBytes", "dartExternalBytes", "decodedLruBytes", "decodedLiveCount", "decodedPendingCount", "processBytes", "nativeBytes", "graphicsBytes", "diskBytes", "transferBytes", "firstRasterizedTargetFrame", "observerOverheadMicroseconds", "memoryBytes", "nativeReadMicroseconds", "nativeResultCode", "nativeResultCount", "sampleStartMicroseconds", "sampleEndMicroseconds", "sampleCadenceMicroseconds", "pageIndex", "selectionGeneration", "decodedKeepAliveCount", "frameCount", "frameBuildTotalMicroseconds", "frameRasterTotalMicroseconds", "frameBuildMaxMicroseconds", "frameRasterMaxMicroseconds", "apiLevel", "memTotalBytes", "memoryClassMiB", "backgroundCount", "returnCount", "backgroundEpochMilliseconds", "returnedEpochMilliseconds"]
  private static let doubles: Set<String> = ["logicalSlotWidth", "logicalSlotHeight", "dpr", "alignmentX", "alignmentY", "observedSlotWidth", "observedSlotHeight", "observedDpr", "textScaleFactor", "paddingLeft", "paddingTop", "paddingRight", "paddingBottom", "viewInsetLeft", "viewInsetTop", "viewInsetRight", "viewInsetBottom"]
  private static let booleans: Set<String> = ["encodedVerified", "sourceShortage", "physicalPresentationVerified", "lowRamDevice", "isPhysicalDevice", "backgrounded"]
  private static let strings: [String: Set<String>] = [
    "phase": ["idle", "baseline", "load", "hold", "releasing", "post_release", "complete", "reader", "natural_retention", "owned_eviction"],
    "memoryCounter": ["ios_phys_footprint", "android_total_pss", "unavailable"],
    "memoryReason": ["none", "mach_result", "mach_count", "missing_counter", "sample_failed", "session_inactive", "sample_timeout"],
    "thermalState": ["nominal", "fair", "serious", "critical", "unavailable"],
    "clockDomain": ["dart_monotonic"],
    "clockUnit": ["microseconds"],
    "fit": ["contain", "cover"],
    "orientation": ["portrait", "landscape"],
    "platform": ["ios", "android"],
    "deviceReason": ["none", "unsupported", "read_failed", "invalid_value", "unavailable"],
    "qemu": ["0", "1", "unavailable"],
    "role": ["isolated_raster_fixture"],
    "event": ["request", "body", "encoded_verified", "decoded", "draw", "failure", "revoke", "cache_state", "status", "status_request", "status_body", "ownership", "phase", "memory_sample", "release_complete", "layout", "dispatch", "reader_geometry", "reader_selection", "page_paint", "frame_aggregate", "device_facts", "lifecycle"],
    "ownership": ["owned", "settling", "settled", "released", "unavailable"],
    "reason": ["none", "invalid_fixture", "session_inactive", "status_unavailable", "status_revoked", "missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity", "route_departed", "stale_attempt", "internal_settlement_unavailable", "export_capacity", "pin_refused", "transport_refused", "decode_refused", "cleanup_unavailable", "phase_budget_refused", "load_timeout", "layout_unavailable", "lifecycle_unavailable", "device_facts_unavailable", "reader_incomplete"],
    "counterReason": ["missing_counter", "unverified_visibility", "frame_unavailable", "timing_unavailable", "clock_unmapped", "retained_layer_ambiguity", "internal_settlement_unavailable"],
    "cacheState": ["namespace_keys_absent", "network-cold", "disk-warm/decoded-cold", "decoded-warm", "unavailable"]]

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
          valid = (["lowRamDevice", "isPhysicalDevice"].contains(field) && value is NSNull) ||
            ((value as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } ?? false)
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

/// Self phys_footprint is one OS accounting view; it is never added to heap/GPU counters.
enum ImageExperimentMemory {
  static func sample() -> [String: Any] {
    let start = DispatchTime.now().uptimeNanoseconds
    var info = task_vm_info_data_t()
    let expected = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<integer_t>.size)
    var count = expected
    let status = withUnsafeMutablePointer(to: &info) {
      $0.withMemoryRebound(to: integer_t.self, capacity: Int(expected)) {
        task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
      }
    }
    let valid = status == KERN_SUCCESS && count >= expected && info.phys_footprint <= UInt64(Int64.max)
    let reason = status != KERN_SUCCESS ? "mach_result" : count < expected ? "mach_count" : valid ? "none" : "missing_counter"
    let thermal: String
    switch ProcessInfo.processInfo.thermalState {
    case .nominal: thermal = "nominal"
    case .fair: thermal = "fair"
    case .serious: thermal = "serious"
    case .critical: thermal = "critical"
    @unknown default: thermal = "unavailable"
    }
    return ["memoryBytes": valid ? NSNumber(value: info.phys_footprint) : NSNull(),
      "memoryCounter": "ios_phys_footprint", "memoryReason": reason,
      "nativeReadMicroseconds": (DispatchTime.now().uptimeNanoseconds - start) / 1000,
      "nativeResultCode": status, "nativeResultCount": count, "thermalState": thermal]
  }
}

/// A single owned native hold lease. Invalidation is permanent for this capture attempt.
final class ImageExperimentQualityLifetime {
  private let now: () -> Int64
  private var started = false
  private(set) var deadline: Int64 = 0
  init(now: @escaping () -> Int64 = { Int64(Date().timeIntervalSince1970 * 1000) }) { self.now = now }
  func begin(sessionExpiresAt: Int64) -> Bool {
    let current = now()
    let (limit, overflow) = current.addingReportingOverflow(10_000)
    guard !started, !overflow, sessionExpiresAt > current else { return false }
    started = true
    deadline = min(limit, sessionExpiresAt)
    return isActive
  }
  var isActive: Bool { deadline > now() }
  func invalidate() { started = true; deadline = 0 }
}

/// Reader permissions leave all original single-case batches unchanged.
enum ImageExperimentReaderPolicy {
  static let primaryBatches: Set<String> = ["reader_resolution_a_r1", "reader_resolution_b_r1", "reader_resolution_c_r1", "reader_resolution_075_r1"]
  static let qualityBatches: Set<String> = ["reader_quality_full_r1", "reader_quality_100_r1", "reader_quality_075_r1"]
  static let batches = primaryBatches.union(qualityBatches)
  static func accepts(batch: String, selected: String, entries: [[String: Any]]) -> Bool {
    guard batches.contains(batch), selected == "reader_page_1", entries.count == (qualityBatches.contains(batch) ? 2 : 6) else { return false }
    return entries.enumerated().allSatisfy { index, entry in
      entry["caseId"] as? String == "reader_page_\(index + 1)" && entry["mime"] as? String == "image/png" &&
        entry["purposeAcceptanceReceipt"] as? String == batch
    }
  }
}

/// Only actual scene callbacks establish a background and subsequent return.
final class ImageExperimentReaderLifecycle {
  private let now: () -> Int64
  private var enabled = false
  private var backgroundCount = 0
  private var returnCount = 0
  private var backgrounded = false
  private var backgroundAt: Int64?
  private var returnedAt: Int64?
  init(now: @escaping () -> Int64 = { Int64(Date().timeIntervalSince1970 * 1000) }) { self.now = now }
  func enable() { enabled = true }
  func background() {
    guard enabled, !backgrounded else { return }
    backgrounded = true; backgroundCount += 1; backgroundAt = now(); returnedAt = nil
  }
  func foreground() {
    guard enabled, backgrounded else { return }
    backgrounded = false; returnCount += 1; returnedAt = now()
  }
  func snapshot() -> [String: Any] {
    ["schemaVersion": 1, "backgroundCount": backgroundCount, "returnCount": returnCount,
      "backgrounded": backgrounded, "backgroundEpochMilliseconds": backgroundAt.map { NSNumber(value: $0) } ?? NSNull(),
      "returnedEpochMilliseconds": returnedAt.map { NSNumber(value: $0) } ?? NSNull()]
  }
}

/// Two reader bindings share a total twenty-second deadline and individual ten-second holds.
final class ImageExperimentReaderQualityLifetime {
  private let now: () -> Int64
  private var globalDeadline: Int64 = 0
  private var publications = 0
  private var invalidated = false
  private(set) var deadline: Int64 = 0
  init(now: @escaping () -> Int64 = { Int64(Date().timeIntervalSince1970 * 1000) }) { self.now = now }
  func begin(sessionExpiresAt: Int64) -> Bool {
    let current = now()
    let (total, totalOverflow) = current.addingReportingOverflow(20_000)
    let (local, localOverflow) = current.addingReportingOverflow(10_000)
    guard !invalidated, publications < 2, !totalOverflow, !localOverflow, sessionExpiresAt > current else { return false }
    let cap = globalDeadline == 0 ? total : globalDeadline
    let selected = min(cap, local, sessionExpiresAt)
    guard selected > current else { return false }
    globalDeadline = cap; deadline = selected; publications += 1
    return true
  }
  var isActive: Bool { !invalidated && deadline > now() }
  func release() { deadline = 0 }
  func invalidate() { invalidated = true; deadline = 0 }
}

/// Hardware model and total physical RAM come from this process's OS view, without unique identifiers.
enum ImageExperimentDeviceFacts {
  static func validatedBytes(_ value: [String: Any]) throws -> Data {
    let nullable: Set<String> = ["model", "apiLevel", "abis", "memTotalBytes", "lowRamDevice", "memoryClassMiB", "isPhysicalDevice", "qemu"]
    guard Set(value.keys) == nullable.union(["schemaVersion", "platform", "unavailableReasons"]),
      let version = value["schemaVersion"] as? NSNumber, CFGetTypeID(version) != CFBooleanGetTypeID(),
      ["q", "i", "s", "l"].contains(String(cString: version.objCType)), version.int64Value == 1, value["platform"] as? String == "ios",
      let reasons = value["unavailableReasons"] as? [String: String],
      Set(reasons.keys) == Set(nullable.filter { value[$0] is NSNull }),
      reasons.values.allSatisfy({ ["unsupported", "read_failed", "invalid_value"].contains($0) })
    else { throw ImageExperimentRefusal.refused }
    for field in nullable where !(value[field] is NSNull) {
      let item = value[field]
      let valid: Bool
      switch field {
      case "model": valid = (item as? String).map { !$0.isEmpty && $0.utf8.count <= 128 && $0.unicodeScalars.allSatisfy { !CharacterSet.controlCharacters.contains($0) } } ?? false
      case "abis": valid = (item as? [String]).map { !$0.isEmpty && $0.count <= 8 && $0.allSatisfy { $0.range(of: "^[A-Za-z0-9_-]{1,32}$", options: .regularExpression) != nil } } ?? false
      case "lowRamDevice", "isPhysicalDevice": valid = (item as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } ?? false
      case "qemu": valid = (item as? String).map { ["0", "1"].contains($0) } ?? false
      default: valid = (item as? NSNumber).map { CFGetTypeID($0) != CFBooleanGetTypeID() && ["q", "i", "s", "l", "Q"].contains(String(cString: $0.objCType)) && $0.int64Value > 0 } ?? false
      }
      guard valid else { throw ImageExperimentRefusal.refused }
    }
    let bytes = try JSONSerialization.data(withJSONObject: value, options: [.sortedKeys])
    guard bytes.count <= 4096 else { throw ImageExperimentRefusal.refused }
    return bytes
  }
  static func read() -> [String: Any] {
    var system = utsname()
    let status = uname(&system)
    let model = status == 0 ? withUnsafeBytes(of: system.machine) { bytes in
      String(bytes: bytes.prefix { $0 != 0 }, encoding: .utf8)
    } : nil
    var reasons = ["apiLevel": "unsupported", "lowRamDevice": "unsupported", "memoryClassMiB": "unsupported", "qemu": "unsupported"]
    if model == nil || model?.isEmpty == true { reasons["model"] = "read_failed" }
    let total = ProcessInfo.processInfo.physicalMemory
    let memory: Any = total > 0 && total <= UInt64(Int64.max) ? NSNumber(value: total) : NSNull()
    if memory is NSNull { reasons["memTotalBytes"] = "invalid_value" }
    let abi: String
    #if arch(arm64)
    abi = "arm64"
    #elseif arch(x86_64)
    abi = "x86_64"
    #else
    abi = "unavailable"
    reasons["abis"] = "unsupported"
    #endif
    #if targetEnvironment(simulator)
    let physical = false
    #else
    let physical = true
    #endif
    return ["schemaVersion": 1, "platform": "ios", "model": model.map { $0 as Any } ?? NSNull(),
      "apiLevel": NSNull(), "abis": abi == "unavailable" ? NSNull() : [abi] as Any,
      "memTotalBytes": memory, "lowRamDevice": NSNull(), "memoryClassMiB": NSNull(),
      "isPhysicalDevice": physical, "qemu": NSNull(), "unavailableReasons": reasons]
  }
}

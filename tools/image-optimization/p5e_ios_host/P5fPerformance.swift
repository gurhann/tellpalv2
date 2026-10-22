import Foundation
import UIKit
import Flutter
import CryptoKit

/// Yalnız self phys_footprint okur; GPU veya gerçek tepe ölçümü değildir.
final class P5fPerformance {
    private let directory: URL
    private let nonce: String
    private let sessionId = UUID().uuidString
    private let queue = DispatchQueue(label: "tellpal.p5f.memory", qos: .userInteractive)
    private var timer: DispatchSourceTimer?
    private var started: UInt64 = 0
    private var trialStarted: UInt64 = 0
    private var lastSample: UInt64 = 0
    private var maximum: UInt64 = 0
    private var maxGap: UInt64 = 0
    private var gaps: [UInt64] = []
    private var samples = 0
    private var baseline: UInt64 = 0
    private var loaded: UInt64?
    private var trialEpoch: Int?
    private var pendingEpoch: Int?
    private var lifetime = P5fLifetime()
    private var invalid: String? {
        get { lifetime.invalidReason }
        set { lifetime.invalidReason = newValue }
    }
    private var completed = 0
    private var rows = Data()
    private var sequence = 0
    private lazy var idle = P5fIdleLease(
        read: { UIApplication.shared.isIdleTimerDisabled },
        write: { UIApplication.shared.isIdleTimerDisabled = $0 })
    var permitted: () -> Bool = { false }
    var didInvalidate: (String) -> Void = { _ in }

    init(documents: URL, nonce: String) throws {
        self.nonce = nonce
        directory = documents.appendingPathComponent("p5f-" + nonce, isDirectory: true)
        guard !FileManager.default.fileExists(atPath: directory.path) else { throw CocoaError(.fileWriteFileExists) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: false,
                                               attributes: [.posixPermissions: 0o700])
        try append(["event": "start", "schemaVersion": 1, "scope": "P5F_IPHONE_PERFORMANCE"])
    }

    func invalidate(_ reason: String) {
        let active = queue.sync { () -> Bool in
            guard lifetime.invalidate(reason) else { return false }
            stop(); return true
        }
        if active { restoreIdle(); didInvalidate(reason) }
    }

    private func restoreIdle() {
        if Thread.isMainThread { idle.release() }
        else { DispatchQueue.main.async { [weak self] in self?.idle.release() } }
    }

    private func footprint() throws -> UInt64 {
        var info = task_vm_info_data_t()
        let expected = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<integer_t>.size)
        var count = expected
        let status = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(expected)) {
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
            }
        }
        guard status == KERN_SUCCESS, count >= expected, info.phys_footprint > 0 else {
            throw NSError(domain: "p5f", code: 1, userInfo: [NSLocalizedDescriptionKey: "counter_refused"])
        }
        guard info.phys_footprint <= 512 * 1024 * 1024 else {
            throw NSError(domain: "p5f", code: 2, userInfo: [NSLocalizedDescriptionKey: "memory_limit"])
        }
        return info.phys_footprint
    }

    private func check() throws {
        guard invalid == nil, !lifetime.sealed, ProcessInfo.processInfo.thermalState == .nominal,
              !ProcessInfo.processInfo.isLowPowerModeEnabled else { throw CocoaError(.userCancelled) }
        let now = DispatchTime.now().uptimeNanoseconds
        guard started == 0 || now - started < 600_000_000_000,
              trialStarted == 0 || now - trialStarted < 10_000_000_000 else { throw CocoaError(.userCancelled) }
    }

    private func sample() throws -> UInt64 {
        try check()
        let now = DispatchTime.now().uptimeNanoseconds
        let value = try footprint()
        if lastSample != 0 {
            let gap = (now - lastSample) / 1000
            gaps.append(gap); maxGap = max(maxGap, gap)
        }
        lastSample = now; samples += 1; maximum = max(maximum, value)
        return value
    }
    private func stop() { timer?.cancel(); timer = nil }

    func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        do {
            let values = call.arguments as? [String: Any] ?? [:]
            if call.method == "abort" {
                defer { restoreIdle() }
                queue.sync { invalid = invalid ?? "dart_abort"; stop(); trialStarted = 0 }
                try append(["event": "abort", "reason": invalid ?? "aborted", "cleanup": values["cleanup"] ?? false])
                try persist(); result([:]); return
            }
            guard permitted() else { throw CocoaError(.userCancelled) }
            var output: [String: Any] = [:]
            try queue.sync {
                try check()
                switch call.method {
                case "begin":
                    guard started == 0 else { throw CocoaError(.userCancelled) }
                    try lifetime.begin(permitted: true, nominal: ProcessInfo.processInfo.thermalState == .nominal,
                                       lowPower: ProcessInfo.processInfo.isLowPowerModeEnabled)
                    started = DispatchTime.now().uptimeNanoseconds
                    try append(["event": "begin", "network": "unsatisfied", "thermal": "nominal", "lowPower": false])
                case "beginTrial":
                    guard started != 0, trialEpoch == nil, pendingEpoch == nil,
                          values["ordinal"] as? Int == completed, let epoch = values["epoch"] as? Int else { throw CocoaError(.userCancelled) }
                    trialStarted = DispatchTime.now().uptimeNanoseconds
                    trialEpoch = epoch; samples = 0; maximum = 0; maxGap = 0; gaps = []; lastSample = 0; loaded = nil
                    baseline = try sample()
                    let source = DispatchSource.makeTimerSource(queue: queue)
                    source.schedule(deadline: .now() + .milliseconds(10), repeating: .milliseconds(10), leeway: .microseconds(500))
                    source.setEventHandler { [weak self] in
                        guard let self = self else { return }
                        do { _ = try self.sample() }
                        catch {
                            self.invalid = "sampler_condition_or_counter_refused"; self.stop()
                            DispatchQueue.main.async { self.restoreIdle(); self.didInvalidate("sampler_condition_or_counter_refused") }
                        }
                    }
                    timer = source; source.resume()
                case "loaded":
                    guard trialEpoch == values["epoch"] as? Int, loaded == nil else { throw CocoaError(.userCancelled) }
                    loaded = try sample()
                case "finishTrial":
                    guard trialEpoch == values["epoch"] as? Int, let loadedValue = loaded else { throw CocoaError(.userCancelled) }
                    let after = try sample(); stop()
                    output = ["counter": "ios_phys_footprint", "baselineBytes": baseline, "loadedBytes": loadedValue,
                              "afterReleaseBytes": after, "sampledMaxBytes": maximum, "samples": samples,
                              "sampleIntervalMicros": 10000, "maxGapMicros": maxGap, "sampleGapsMicros": gaps,
                              "sampledDurationMicros": (lastSample - trialStarted) / 1000,
                              "thermal": "nominal", "lowPower": false, "valid": true]
                    pendingEpoch = trialEpoch; trialEpoch = nil; trialStarted = 0
                case "recordTrial":
                    guard timer == nil, pendingEpoch == values["epoch"] as? Int,
                          values["ordinal"] as? Int == completed, completed < 156,
                          values["cleanup"] as? Bool == true else { throw CocoaError(.userCancelled) }
                    var row = values; row["event"] = "trial"; row["nativeNetwork"] = "unsatisfied"
                    try append(row); completed += 1; pendingEpoch = nil
                case "seal":
                    guard completed == 156, trialEpoch == nil, pendingEpoch == nil, timer == nil,
                          values["completed"] as? Int == 144, values["warmups"] as? Int == 12,
                          values["cleanup"] as? Bool == true else { throw CocoaError(.userCancelled) }
                    try append(["event": "seal", "completed": 144, "warmups": 12, "cleanup": true,
                                "sessionMicros": (DispatchTime.now().uptimeNanoseconds - started) / 1000,
                                "prefixSha256": SHA256.hash(data: rows).map { String(format: "%02x", $0) }.joined()])
                    try lifetime.seal()
                default: throw CocoaError(.featureUnsupported)
                }
            }
            if call.method == "begin" { try idle.acquire() }
            if call.method == "seal" { try persist(); restoreIdle() }
            result(output)
        } catch {
            queue.sync { invalid = invalid ?? "native_refused"; stop() }
            restoreIdle()
            result(FlutterError(code: "p5f_native_refused", message: nil, details: nil))
        }
    }

    private func append(_ entry: [String: Any]) throws {
        var row = entry; sequence += 1
        row["runNonce"] = nonce; row["sessionId"] = sessionId; row["eventSequence"] = sequence
        var data = try JSONSerialization.data(withJSONObject: row, options: [.sortedKeys]); data.append(10)
        guard rows.count + data.count < 4 * 1024 * 1024 else { throw CocoaError(.fileWriteOutOfSpace) }
        rows.append(data)
    }
    private func persist() throws {
        guard timer == nil else { throw CocoaError(.userCancelled) }
        try rows.write(to: directory.appendingPathComponent("session.jsonl"), options: .withoutOverwriting)
    }
}

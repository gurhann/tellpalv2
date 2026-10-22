import Foundation

/// Aynı build yeniden açılınca günlük korunur; yazma hatası kalıcı ret üretir.
final class P5eSession {
    private let directory: URL
    private let nonce: String
    private let sessionId = UUID().uuidString
    private var eventSequence = 0
    private var started = false
    private var sealed = false
    private var invalidated = false
    private(set) var healthy = true

    init(documents: URL, nonce: String) throws {
        guard nonce.range(of: "^[a-f0-9]{32}$", options: .regularExpression) != nil else {
            throw CocoaError(.fileWriteInvalidFileName)
        }
        self.nonce = nonce
        directory = documents.appendingPathComponent("p5e-" + nonce, isDirectory: true)
        var isDirectory: ObjCBool = false
        if FileManager.default.fileExists(atPath: directory.path, isDirectory: &isDirectory) {
            let attributes = try FileManager.default.attributesOfItem(atPath: directory.path)
            guard isDirectory.boolValue, attributes[.type] as? FileAttributeType == .typeDirectory else {
                throw CocoaError(.fileWriteFileExists)
            }
        } else {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: false)
        }
        try append(["event": "start"])
    }

    func append(_ entry: [String: Any]) throws {
        guard healthy else { throw CocoaError(.fileWriteUnknown) }
        do {
            let event = entry["event"] as? String ?? ""
            if started && !sealed && ((event == "network" && entry["network"] as? String != "unsatisfied") ||
                (event == "lifecycle" && entry["active"] as? Bool == false)) { invalidated = true }
            if event == "presentation" {
                guard !sealed, !invalidated, entry["network"] as? String == "unsatisfied" else {
                    throw CocoaError(.fileWriteUnknown)
                }
                started = true
            }
            if event == "seal" {
                guard started, !sealed, !invalidated, entry["network"] as? String == "unsatisfied" else {
                    throw CocoaError(.fileWriteUnknown)
                }
                sealed = true
            }
            try write(entry)
        }
        catch { healthy = false; throw error }
    }

    private func write(_ entry: [String: Any]) throws {
        var row = entry
        row["runNonce"] = nonce
        row["sessionId"] = sessionId
        eventSequence += 1
        row["eventSequence"] = eventSequence
        row["timestamp"] = ISO8601DateFormatter().string(from: Date())
        let file = directory.appendingPathComponent("session.jsonl")
        var data = try JSONSerialization.data(withJSONObject: row, options: [.sortedKeys])
        data.append(0x0a)
        let currentBytes = (try? FileManager.default.attributesOfItem(atPath: file.path)[.size] as? NSNumber)?.intValue ?? 0
        guard currentBytes + data.count <= 16 * 1024 * 1024 else { throw CocoaError(.fileWriteOutOfSpace) }
        if !FileManager.default.fileExists(atPath: file.path) {
            try data.write(to: file, options: .withoutOverwriting)
        } else {
            let attributes = try FileManager.default.attributesOfItem(atPath: file.path)
            guard attributes[.type] as? FileAttributeType == .typeRegular else { throw CocoaError(.fileWriteFileExists) }
            let handle = try FileHandle(forWritingTo: file)
            defer { try? handle.close() }
            try handle.seekToEnd()
            try handle.write(contentsOf: data)
            try handle.synchronize()
        }
    }
}

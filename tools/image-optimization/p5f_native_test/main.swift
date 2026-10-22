import Foundation
import UIKit
import Flutter

/// Aynı modüldeki isim üretim kaynağının ProcessInfo okumasını kontrollü kılar.
final class ProcessInfo {
    enum ThermalState { case nominal, fair }
    static let processInfo = ProcessInfo()
    private let lock = NSLock()
    private var thermal: ThermalState = .nominal
    private var power = false
    var thermalState: ThermalState {
        get { lock.lock(); defer { lock.unlock() }; return thermal }
        set { lock.lock(); defer { lock.unlock() }; thermal = newValue }
    }
    var isLowPowerModeEnabled: Bool {
        get { lock.lock(); defer { lock.unlock() }; return power }
        set { lock.lock(); defer { lock.unlock() }; power = newValue }
    }
    func reset() { thermalState = .nominal; isLowPowerModeEnabled = false }
}

func field(_ sampler: P5fPerformance, _ name: String) -> Any {
    Mirror(reflecting: sampler).children.first { $0.label == name }!.value
}

func call(_ sampler: P5fPerformance, _ method: String, _ values: [String: Any] = [:]) -> Any? {
    var returned = false
    var output: Any?
    sampler.handle(FlutterMethodCall(methodName: method, arguments: values)) { output = $0; returned = true }
    precondition(returned)
    return output
}

func timerActive(_ sampler: P5fPerformance) -> Bool {
    let queue = field(sampler, "queue") as! DispatchQueue
    return queue.sync { !Mirror(reflecting: field(sampler, "timer")).children.isEmpty }
}

let documents = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
for lowPower in [false, true] {
    ProcessInfo.processInfo.reset()
    UIApplication.shared.isIdleTimerDisabled = false
    let sampler = try P5fPerformance(documents: documents, nonce: UUID().uuidString)
    sampler.permitted = { true }
    var notifications = 0
    sampler.didInvalidate = { reason in
        precondition(Thread.isMainThread && reason == "sampler_condition_or_counter_refused")
        notifications += 1
    }
    precondition(!(call(sampler, "begin") is FlutterError))
    precondition(UIApplication.shared.isIdleTimerDisabled)
    precondition(!(call(sampler, "beginTrial", ["ordinal": 0, "epoch": 1]) is FlutterError))
    precondition(timerActive(sampler))
    if lowPower { ProcessInfo.processInfo.isLowPowerModeEnabled = true }
    else { ProcessInfo.processInfo.thermalState = .fair }
    let deadline = Date().addingTimeInterval(2)
    while notifications == 0 && Date() < deadline { RunLoop.current.run(until: Date().addingTimeInterval(0.005)) }
    precondition(notifications == 1)
    precondition(!timerActive(sampler))
    precondition(!UIApplication.shared.isIdleTimerDisabled)
    let samples = field(sampler, "samples") as! Int
    RunLoop.current.run(until: Date().addingTimeInterval(0.05))
    precondition(field(sampler, "samples") as! Int == samples && notifications == 1)
    ProcessInfo.processInfo.reset()
    precondition(call(sampler, "seal", ["completed":144,"warmups":12,"cleanup":true]) is FlutterError)
    precondition((field(sampler, "lifetime") as! P5fLifetime).invalidReason != nil)
}

// Tamamlanmış 156 native turdan sonra koşul değişirse seal check de ret verir.
for lowPower in [false, true] {
    ProcessInfo.processInfo.reset()
    let nonce = UUID().uuidString
    let sampler = try P5fPerformance(documents: documents, nonce: nonce)
    sampler.permitted = { true }
    precondition(!(call(sampler, "begin") is FlutterError))
    for ordinal in 0..<156 {
        let values = ["ordinal": ordinal, "epoch": ordinal + 1]
        precondition(!(call(sampler, "beginTrial", values) is FlutterError))
        precondition(!(call(sampler, "loaded", values) is FlutterError))
        precondition(!(call(sampler, "finishTrial", values) is FlutterError))
        precondition(!(call(sampler, "recordTrial", ["ordinal": ordinal, "epoch": ordinal + 1, "cleanup": true]) is FlutterError))
    }
    precondition(field(sampler, "completed") as! Int == 156 && !timerActive(sampler))
    if lowPower { ProcessInfo.processInfo.isLowPowerModeEnabled = true }
    else { ProcessInfo.processInfo.thermalState = .fair }
    precondition(call(sampler, "seal", ["completed":144,"warmups":12,"cleanup":true]) is FlutterError)
    ProcessInfo.processInfo.reset()
    precondition(call(sampler, "seal", ["completed":144,"warmups":12,"cleanup":true]) is FlutterError)
    precondition(!UIApplication.shared.isIdleTimerDisabled)
    precondition(!FileManager.default.fileExists(atPath: documents.appendingPathComponent("p5f-" + nonce + "/session.jsonl").path))
}
print("native-performance-pass: timer thermal/lowPower stop+callback+seal refusal; completed-trial seal checks")

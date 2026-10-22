import Flutter
import Network
import UIKit

@main
final class AppDelegate: FlutterAppDelegate {
    private let monitor = NWPathMonitor()
    private var channel: FlutterMethodChannel?
    private var network = "unknown"
    private var sequence = 0
    private var session: P5eSession!
    private var nonce: String = ""
    private var availableDiskBytes: Int64 = -1

    override func application(_ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        nonce = Bundle.main.object(forInfoDictionaryKey: "P5eRunNonce") as? String ?? ""
        guard nonce.range(of: "^[a-f0-9]{32}$", options: .regularExpression) != nil else { return false }
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        do {
            let disk = try FileManager.default.attributesOfFileSystem(forPath: documents.path)
            availableDiskBytes = (disk[.systemFreeSize] as? NSNumber)?.int64Value ?? -1
            guard availableDiskBytes >= 16 * 1024 * 1024 else { return false }
            session = try P5eSession(documents: documents, nonce: nonce)
            try session.append(["event": "deviceSpace", "availableDiskBytesAfterInstall": availableDiskBytes])
        }
        catch { return false }
        let controller = FlutterViewController(project: nil, nibName: nil, bundle: nil)
        let appWindow = UIWindow(frame: UIScreen.main.bounds)
        appWindow.rootViewController = controller
        window = appWindow
        appWindow.makeKeyAndVisible()
        channel = FlutterMethodChannel(name: "tellpal.p5e.offline", binaryMessenger: controller.binaryMessenger)
        channel?.setMethodCallHandler { [weak self] call, result in
            self?.handle(call, result)
        }
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                guard let self = self else { return }
                switch path.status {
                case .unsatisfied: self.network = "unsatisfied"
                case .satisfied: self.network = "satisfied"
                case .requiresConnection: self.network = "requiresConnection"
                @unknown default: self.network = "unknown"
                }
                self.sequence += 1
                do { try self.session.append(["event": "network", "network": self.network, "sequence": self.sequence]) }
                catch { self.network = "unknown" }
                self.channel?.invokeMethod("networkChanged", arguments: self.network)
            }
        }
        monitor.start(queue: DispatchQueue(label: "tellpal.p5e.path"))
        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }

    private func handle(_ call: FlutterMethodCall, _ result: @escaping FlutterResult) {
        switch call.method {
        case "environment":
            #if targetEnvironment(simulator)
            let physical = false
            #else
            let physical = true
            #endif
            result(["packageId": Bundle.main.bundleIdentifier ?? "", "plugins": 0,
                    "physicalDevice": physical, "runNonce": nonce, "availableDiskBytesAfterInstall": availableDiskBytes])
        case "network": result(session.healthy ? network : "unknown")
        case "record":
            guard session.healthy, network == "unsatisfied", UIApplication.shared.applicationState == .active,
                  var entry = call.arguments as? [String: Any], entry["network"] as? String == network else {
                result(FlutterError(code: "offline_record_refused", message: nil, details: nil)); return
            }
            entry["nativeNetwork"] = network
            entry["nativeNetworkSequence"] = sequence
            do { try session.append(entry); result(nil) }
            catch { result(FlutterError(code: "session_write_refused", message: nil, details: nil)) }
        default: result(FlutterMethodNotImplemented)
        }
    }

    override func applicationWillResignActive(_ application: UIApplication) {
        do { try session.append(["event": "lifecycle", "active": false, "network": network]) }
        catch {
            network = "unknown"
            channel?.invokeMethod("networkChanged", arguments: "unknown")
        }
        super.applicationWillResignActive(application)
    }
    deinit { monitor.cancel() }
}

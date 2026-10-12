import UIKit

@objc final class ImageExperimentAuditAppDelegate: UIResponder, UIApplicationDelegate {
  private weak var ownedWindow: UIWindow?
  private var report: String?

  func application(_ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    do {
      let environment = ProcessInfo.processInfo.environment
      let request = try ImageExperimentAuditRequest(encoded: environment[ImageExperimentAuditRequest.environmentKey],
        brokerMetadataPresent: environment["IMAGE_EXPERIMENT_METADATA"] != nil, applicationId: Bundle.main.bundleIdentifier)
      DispatchQueue.main.asyncAfter(deadline: .now() + request.remainingSeconds) { [weak self] in
        self?.publish("INSTALLATION_AUDIT_REFUSED")
      }
      DispatchQueue.global(qos: .utility).async { [weak self] in
        let result: String
        do {
          let digest = try ImageExperimentInstallationAudit.hashBundle(Bundle.main.bundleURL, checkDeadline: request.checkDeadline)
          result = try ImageExperimentInstallationAudit.appReport(request: request, applicationId: Bundle.main.bundleIdentifier, digest: digest)
        } catch { result = "INSTALLATION_AUDIT_REFUSED" }
        DispatchQueue.main.async { self?.publish(result) }
      }
    } catch { publish("INSTALLATION_AUDIT_REFUSED") }
    return true
  }

  func application(_ application: UIApplication, configurationForConnecting session: UISceneSession,
    options: UIScene.ConnectionOptions) -> UISceneConfiguration {
    let configuration = UISceneConfiguration(name: "installation-audit", sessionRole: session.role)
    configuration.sceneClass = UIWindowScene.self
    configuration.delegateClass = ImageExperimentAuditSceneDelegate.self
    configuration.storyboard = nil
    return configuration
  }

  func connect(_ window: UIWindow) {
    guard ownedWindow == nil else { return }
    ownedWindow = window
    exposeReport()
  }

  private func publish(_ value: String) {
    guard report == nil else { return }
    report = value
    exposeReport()
  }

  private func exposeReport() {
    guard let report, let window = ownedWindow, window.viewWithTag(73_019) == nil else { return }
    let element = UIView(frame: CGRect(x: 8, y: 8, width: 32, height: 32))
    element.tag = 73_019
    element.isAccessibilityElement = true
    element.accessibilityIdentifier = "imageExperimentInstallationAudit"
    element.accessibilityLabel = "Installation audit"
    element.accessibilityValue = report
    element.isUserInteractionEnabled = false
    window.addSubview(element)
  }
}

@objc final class ImageExperimentAuditSceneDelegate: SceneDelegate {}

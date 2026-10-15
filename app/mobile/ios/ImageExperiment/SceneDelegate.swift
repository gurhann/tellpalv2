import Flutter
import UIKit

// Persisted scene configurations always enter a plain delegate and choose the current mode.
class SceneDelegate: UIResponder, UIWindowSceneDelegate, FlutterSceneLifeCycleProvider {
  var window: UIWindow?
  private var ordinaryDelegate: FlutterSceneDelegate?

  var sceneLifeCycleDelegate: FlutterPluginSceneLifeCycleDelegate {
    guard let provider = ordinaryDelegate as? FlutterSceneLifeCycleProvider
    else { preconditionFailure("NATIVE_SCENE_REFUSED") }
    return provider.sceneLifeCycleDelegate
  }

  func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options: UIScene.ConnectionOptions) {
    guard let windowScene = scene as? UIWindowScene else { return }
    let owned = UIWindow(windowScene: windowScene)
    if let audit = UIApplication.shared.delegate as? ImageExperimentAuditAppDelegate {
      owned.rootViewController = UIViewController()
      window = owned
      owned.makeKeyAndVisible()
      audit.connect(owned)
      return
    }
    guard let app = UIApplication.shared.delegate as? AppDelegate else { return }
    let flutter = FlutterSceneDelegate()
    ordinaryDelegate = flutter
    // Storyboard creation occurs only after the ordinary mode and lifecycle owner are selected.
    owned.rootViewController = UIStoryboard(name: "FlutterMain", bundle: nil).instantiateInitialViewController()
    window = owned
    flutter.window = owned
    flutter.scene(scene, willConnectTo: session, options: options)
    owned.makeKeyAndVisible()
    app.ownedWindow = owned
    app.exposePairingKey()
  }

  func sceneDidDisconnect(_ scene: UIScene) {
    (UIApplication.shared.delegate as? AppDelegate)?.bootstrap?.revoke()
    ordinaryDelegate?.sceneDidDisconnect(scene)
  }

  func sceneWillEnterForeground(_ scene: UIScene) { ordinaryDelegate?.sceneWillEnterForeground(scene) }
  func sceneDidBecomeActive(_ scene: UIScene) {
    (UIApplication.shared.delegate as? AppDelegate)?.readerDidBecomeActive()
    ordinaryDelegate?.sceneDidBecomeActive(scene)
  }
  func sceneWillResignActive(_ scene: UIScene) {
    (UIApplication.shared.delegate as? AppDelegate)?.readerWillResignActive()
    ordinaryDelegate?.sceneWillResignActive(scene)
  }
  func sceneDidEnterBackground(_ scene: UIScene) {
    (UIApplication.shared.delegate as? AppDelegate)?.readerDidEnterBackground()
    ordinaryDelegate?.sceneDidEnterBackground(scene)
  }
  func scene(_ scene: UIScene, openURLContexts contexts: Set<UIOpenURLContext>) {
    ordinaryDelegate?.scene(scene, openURLContexts: contexts)
  }
  func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    ordinaryDelegate?.scene(scene, continue: userActivity)
  }
  func stateRestorationActivity(for scene: UIScene) -> NSUserActivity? {
    guard let ordinaryDelegate else { return nil }
    if scene.userActivity == nil {
      scene.userActivity = NSUserActivity(activityType: "com.tellpal.imageexperiment.scene-restoration")
    }
    return ordinaryDelegate.stateRestorationActivity(for: scene)
  }
  func scene(_ scene: UIScene, restoreInteractionStateWith stateRestorationActivity: NSUserActivity) {
    ordinaryDelegate?.scene(scene, restoreInteractionStateWith: stateRestorationActivity)
  }
  func windowScene(_ scene: UIWindowScene, performActionFor shortcut: UIApplicationShortcutItem,
    completionHandler: @escaping (Bool) -> Void) {
    if let ordinaryDelegate {
      ordinaryDelegate.windowScene(scene, performActionFor: shortcut, completionHandler: completionHandler)
    } else { completionHandler(false) }
  }
}

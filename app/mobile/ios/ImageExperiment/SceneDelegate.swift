import Flutter
import UIKit

class SceneDelegate: FlutterSceneDelegate {
  override func scene(_ scene: UIScene, willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions) {
    super.scene(scene, willConnectTo: session, options: connectionOptions)
    guard let app = UIApplication.shared.delegate as? AppDelegate,
      let window = (scene as? UIWindowScene)?.windows.first else { return }
    app.ownedWindow = window
    app.exposePairingKey()
  }

  override func sceneDidDisconnect(_ scene: UIScene) {
    (UIApplication.shared.delegate as? AppDelegate)?.bootstrap?.revoke()
    super.sceneDidDisconnect(scene)
  }
}

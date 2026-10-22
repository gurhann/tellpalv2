import Foundation

/// Host testi yalnız idle timer API yüzeyini sağlar.
public final class UIApplication {
    public static let shared = UIApplication()
    public var isIdleTimerDisabled = false
}

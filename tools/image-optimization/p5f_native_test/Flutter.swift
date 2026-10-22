import Foundation

public typealias FlutterResult = (Any?) -> Void

public final class FlutterMethodCall {
    public let method: String
    public let arguments: Any?
    public init(methodName: String, arguments: Any?) {
        method = methodName
        self.arguments = arguments
    }
}

public final class FlutterError {
    public let code: String
    public init(code: String, message: String?, details: Any?) { self.code = code }
}

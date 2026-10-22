import Foundation

/// Hazırlıkta ağ/uygulama geçişleri serbesttir; başlayan ölçümün kesintisi kalıcıdır.
struct P5fLifetime {
    private(set) var begun = false
    private(set) var sealed = false
    var invalidReason: String?

    mutating func begin(permitted: Bool, nominal: Bool, lowPower: Bool) throws {
        guard !begun, !sealed, invalidReason == nil, permitted, nominal, !lowPower else {
            throw CocoaError(.userCancelled)
        }
        begun = true
    }

    mutating func invalidate(_ reason: String) -> Bool {
        guard begun, !sealed else { return false }
        invalidReason = invalidReason ?? reason
        return true
    }

    mutating func seal() throws {
        guard begun, !sealed, invalidReason == nil else { throw CocoaError(.userCancelled) }
        sealed = true
    }
}

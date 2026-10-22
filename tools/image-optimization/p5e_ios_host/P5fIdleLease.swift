import Foundation

/// Yalnız uygulamanın idle timer değerini geçici değiştirir; önceki değeri korur.
final class P5fIdleLease {
    private let read: () -> Bool
    private let write: (Bool) -> Void
    private var previous: Bool?

    init(read: @escaping () -> Bool, write: @escaping (Bool) -> Void) {
        self.read = read
        self.write = write
    }

    func acquire() throws {
        guard Thread.isMainThread, previous == nil else { throw CocoaError(.userCancelled) }
        previous = read()
        write(true)
    }

    func release() {
        precondition(Thread.isMainThread)
        guard let value = previous else { return }
        previous = nil
        write(value)
    }
}

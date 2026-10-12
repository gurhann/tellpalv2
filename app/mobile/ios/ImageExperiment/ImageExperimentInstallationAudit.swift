import CryptoKit
import Darwin
import Foundation

enum ImageExperimentAuditRefusal: Error { case refused }

struct ImageExperimentAuditRequest {
  static let environmentKey = "IMAGE_EXPERIMENT_INSTALLATION_AUDIT"
  static let scope = "image-optimization-installation-audit-r1"
  static let appIds = ["com.tellpal.tellpalMobilePoc.imageexperiment.profile",
    "com.tellpal.tellpalMobilePoc.imageexperiment.release"]
  let appId: String
  let nonce: String
  let deadline: Int64
  let monotonicDeadline: UInt64

  init(encoded: String?, brokerMetadataPresent: Bool, applicationId: String? = nil,
    now: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) throws {
    guard !brokerMetadataPresent, let encoded, encoded.utf8.count <= 8_192,
      let data = Data(base64Encoded: encoded), data.count <= 4_096,
      data.base64EncodedString() == encoded else { throw ImageExperimentAuditRefusal.refused }
    let fields = try ImageExperimentAuditJSON.parse(data)
    guard Set(fields.keys) == ["schemaVersion", "scope", "expectedAppId", "nonce", "deadline"],
      fields["schemaVersion"]?.integer == 1, fields["scope"]?.string == Self.scope,
      let appId = fields["expectedAppId"]?.string, Self.appIds.contains(appId),
      applicationId == nil || applicationId == appId,
      let nonce = fields["nonce"]?.string, Self.isHash(nonce),
      let deadline = fields["deadline"]?.integer
    else { throw ImageExperimentAuditRefusal.refused }
    let delta = deadline.subtractingReportingOverflow(now)
    guard !delta.overflow, delta.partialValue > 0, delta.partialValue <= 30_000
    else { throw ImageExperimentAuditRefusal.refused }
    let monotonic = DispatchTime.now().uptimeNanoseconds.addingReportingOverflow(UInt64(delta.partialValue) * 1_000_000)
    guard !monotonic.overflow else { throw ImageExperimentAuditRefusal.refused }
    self.appId = appId
    self.nonce = nonce
    self.deadline = deadline
    self.monotonicDeadline = monotonic.partialValue
  }

  var runnerId: String { appId + ".uitests.xctrunner" }

  func checkDeadline() throws {
    guard Int64(Date().timeIntervalSince1970 * 1000) < deadline,
      DispatchTime.now().uptimeNanoseconds < monotonicDeadline
    else { throw ImageExperimentAuditRefusal.refused }
  }

  var remainingSeconds: TimeInterval {
    let now = DispatchTime.now().uptimeNanoseconds
    guard now < monotonicDeadline else { return 0 }
    return min(30, TimeInterval(monotonicDeadline - now) / 1_000_000_000)
  }

  static func isHash(_ value: String) -> Bool {
    value.utf8.count == 64 && value.utf8.allSatisfy { (48...57).contains($0) || (97...102).contains($0) }
  }
}

// The audit wire object contains only ASCII strings and nonnegative integers. Reject
// escaped keys, duplicate keys, nested values, booleans and trailing data before decoding.
enum ImageExperimentAuditJSON {
  enum Value {
    case string(String), integer(Int64)
    var string: String? { if case .string(let value) = self { return value }; return nil }
    var integer: Int64? { if case .integer(let value) = self { return value }; return nil }
  }

  static func parse(_ data: Data) throws -> [String: Value] {
    guard !data.isEmpty, data.count <= 4_096 else { throw ImageExperimentAuditRefusal.refused }
    let bytes = [UInt8](data)
    var index = 0
    func whitespace() {
      while index < bytes.count && [UInt8(9), 10, 13, 32].contains(bytes[index]) { index += 1 }
    }
    func consume(_ byte: UInt8) throws {
      whitespace()
      guard index < bytes.count, bytes[index] == byte else { throw ImageExperimentAuditRefusal.refused }
      index += 1
    }
    func string() throws -> String {
      try consume(34)
      let start = index
      while index < bytes.count && bytes[index] != 34 {
        guard (32...126).contains(bytes[index]), bytes[index] != 92 else { throw ImageExperimentAuditRefusal.refused }
        index += 1
      }
      guard index < bytes.count else { throw ImageExperimentAuditRefusal.refused }
      let value = String(decoding: bytes[start..<index], as: UTF8.self)
      index += 1
      return value
    }
    try consume(123)
    var values: [String: Value] = [:]
    whitespace()
    guard index < bytes.count, bytes[index] != 125 else { throw ImageExperimentAuditRefusal.refused }
    while true {
      let key = try string()
      guard values[key] == nil else { throw ImageExperimentAuditRefusal.refused }
      try consume(58)
      whitespace()
      guard index < bytes.count else { throw ImageExperimentAuditRefusal.refused }
      if bytes[index] == 34 {
        values[key] = .string(try string())
      } else {
        let start = index
        while index < bytes.count && (48...57).contains(bytes[index]) { index += 1 }
        guard index > start, index - start == 1 || bytes[start] != 48,
          let value = Int64(String(decoding: bytes[start..<index], as: UTF8.self))
        else { throw ImageExperimentAuditRefusal.refused }
        values[key] = .integer(value)
      }
      whitespace()
      guard index < bytes.count else { throw ImageExperimentAuditRefusal.refused }
      if bytes[index] == 125 { index += 1; break }
      try consume(44)
    }
    whitespace()
    guard index == bytes.count else { throw ImageExperimentAuditRefusal.refused }
    return values
  }

  static func encode(_ fields: [String: Any]) throws -> String {
    let bytes = try JSONSerialization.data(withJSONObject: fields, options: [.sortedKeys])
    guard bytes.count <= 4_096 else { throw ImageExperimentAuditRefusal.refused }
    return bytes.base64EncodedString()
  }
}

struct ImageExperimentBundleDigest {
  let sha256: String
  let fileCount: Int64
  let byteCount: Int64
}

enum ImageExperimentAuditHashStage: String, CaseIterable {
  case rootStat = "HASH_ROOT_STAT"
  case rootOpen = "HASH_ROOT_OPEN"
  case rootIdentity = "HASH_ROOT_IDENTITY"
  case rootFstat = "HASH_ROOT_FSTAT"
  case rootDevice = "HASH_ROOT_DEVICE"
  case rootInode = "HASH_ROOT_INODE"
  case rootMode = "HASH_ROOT_MODE"
  case rootSize = "HASH_ROOT_SIZE"
  case rootMtimeSeconds = "HASH_ROOT_MTIME_SECONDS"
  case rootMtimeNanoseconds = "HASH_ROOT_MTIME_NANOSECONDS"
  case rootCtimeSeconds = "HASH_ROOT_CTIME_SECONDS"
  case rootCtimeNanoseconds = "HASH_ROOT_CTIME_NANOSECONDS"
  case directoryLimits = "HASH_DIRECTORY_LIMITS"
  case directoryStat = "HASH_DIRECTORY_STAT"
  case directoryDuplicate = "HASH_DIRECTORY_DUPLICATE"
  case directoryOpen = "HASH_DIRECTORY_OPEN"
  case directoryRead = "HASH_DIRECTORY_READ"
  case entryName = "HASH_ENTRY_NAME"
  case entryLimits = "HASH_ENTRY_LIMITS"
  case entryStat = "HASH_ENTRY_STAT"
  case entryType = "HASH_ENTRY_TYPE"
  case directoryChildOpen = "HASH_DIRECTORY_CHILD_OPEN"
  case directoryIdentity = "HASH_DIRECTORY_IDENTITY"
  case fileLimits = "HASH_FILE_LIMITS"
  case fileOpen = "HASH_FILE_OPEN"
  case fileIdentity = "HASH_FILE_IDENTITY"
  case fileRead = "HASH_FILE_READ"
  case fileLength = "HASH_FILE_LENGTH"
  case fileFinalIdentity = "HASH_FILE_FINAL_IDENTITY"
  case directoryFinalIdentity = "HASH_DIRECTORY_FINAL_IDENTITY"
  case reopenParent = "HASH_REOPEN_PARENT"
  case reopenPath = "HASH_REOPEN_PATH"
  case reopenFile = "HASH_REOPEN_FILE"
  case reopenIdentity = "HASH_REOPEN_IDENTITY"
  case finalRoot = "HASH_FINAL_ROOT"
  case deadline = "HASH_DEADLINE"
}

enum ImageExperimentInstallationAudit {
  static let maximumFiles: Int64 = 65_536
  static let maximumFileBytes: Int64 = 256 * 1024 * 1024
  static let maximumTotalBytes: Int64 = 512 * 1024 * 1024

  static func hashBundle(_ url: URL, checkDeadline: () throws -> Void,
    observeStage: (ImageExperimentAuditHashStage) -> Void = { _ in },
    observeEntryViews: (ImageExperimentAuditEntryViewResult) -> Void = { _ in }) throws -> ImageExperimentBundleDigest {
    var initial = stat()
    observeStage(.rootStat)
    guard url.isFileURL, lstat(url.path, &initial) == 0,
      initial.st_mode & S_IFMT == S_IFDIR else { throw ImageExperimentAuditRefusal.refused }
    observeStage(.rootOpen)
    let root = open(url.path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC)
    guard root >= 0 else { throw ImageExperimentAuditRefusal.refused }
    defer { close(root) }
    var opened = stat()
    observeStage(.rootIdentity)
    observeStage(.rootFstat)
    // Bind the two metadata views by identity, then retain each complete signature
    // against its own API baseline through the end of the scan.
    guard fstat(root, &opened) == 0, sameRootIdentity(initial, opened, observeRootStage: observeStage)
    else { throw ImageExperimentAuditRefusal.refused }
    var rows: [(path: [UInt8], digest: Data, signature: stat)] = []
    var directories: [Data: (path: stat, descriptor: stat)] = [Data(): (initial, opened)]
    var byteCount: Int64 = 0
    var entryCount: Int64 = 0

    func visit(_ descriptor: Int32, prefix: [UInt8], depth: Int,
      entry: (parent: Int32, name: String, path: stat, descriptor: stat)? = nil) throws {
      observeStage(.deadline)
      try checkDeadline()
      observeStage(.directoryLimits)
      guard depth <= 128 else { throw ImageExperimentAuditRefusal.refused }
      var before = stat()
      observeStage(.directoryStat)
      guard fstat(descriptor, &before) == 0 else { throw ImageExperimentAuditRefusal.refused }
      observeStage(.directoryDuplicate)
      let duplicate = dup(descriptor)
      guard duplicate >= 0 else { throw ImageExperimentAuditRefusal.refused }
      observeStage(.directoryOpen)
      guard let directory = fdopendir(duplicate) else { close(duplicate); throw ImageExperimentAuditRefusal.refused }
      defer { closedir(directory) }
      while true {
        observeStage(.deadline)
        try checkDeadline()
        observeStage(.directoryRead)
        errno = 0
        guard let entry = readdir(directory) else {
          guard errno == 0 else { throw ImageExperimentAuditRefusal.refused }
          break
        }
        let nameBytes: [UInt8] = withUnsafePointer(to: &entry.pointee.d_name) { pointer in
          pointer.withMemoryRebound(to: UInt8.self, capacity: Int(entry.pointee.d_namlen)) {
            Array(UnsafeBufferPointer(start: $0, count: Int(entry.pointee.d_namlen)))
          }
        }
        if nameBytes == [46] || nameBytes == [46, 46] { continue }
        observeStage(.entryName)
        guard !nameBytes.isEmpty, !nameBytes.contains(47), !nameBytes.contains(0),
          let name = String(bytes: nameBytes, encoding: .utf8) else { throw ImageExperimentAuditRefusal.refused }
        entryCount += 1
        observeStage(.entryLimits)
        guard entryCount <= maximumFiles else { throw ImageExperimentAuditRefusal.refused }
        let path = prefix + nameBytes
        guard path.count <= 4_096 else { throw ImageExperimentAuditRefusal.refused }
        var metadata = stat()
        observeStage(.entryStat)
        guard fstatat(descriptor, name, &metadata, AT_SYMLINK_NOFOLLOW) == 0
        else { throw ImageExperimentAuditRefusal.refused }
        observeStage(.entryType)
        switch metadata.st_mode & S_IFMT {
        case S_IFDIR:
          observeStage(.directoryChildOpen)
          let child = openat(descriptor, name, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC)
          guard child >= 0 else { throw ImageExperimentAuditRefusal.refused }
          defer { close(child) }
          var actual = stat()
          observeStage(.directoryIdentity)
          guard fstat(child, &actual) == 0, sameRootIdentity(metadata, actual) else {
            observeEntryViews(ImageExperimentAuditEntryViews.probe(parent: descriptor, name: name,
              child: child, checkDeadline: checkDeadline))
            throw ImageExperimentAuditRefusal.refused
          }
          directories[Data(path)] = (metadata, actual)
          try visit(child, prefix: path + [47], depth: depth + 1,
            entry: (descriptor, name, metadata, actual))
        case S_IFREG:
          let size = Int64(metadata.st_size)
          observeStage(.fileLimits)
          guard size >= 0, size <= maximumFileBytes, byteCount <= maximumTotalBytes - size,
            rows.count < maximumFiles else { throw ImageExperimentAuditRefusal.refused }
          // Nonblocking open also refuses a regular-file-to-FIFO race without waiting for a writer.
          observeStage(.fileOpen)
          let file = openat(descriptor, name, O_RDONLY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK)
          guard file >= 0 else { throw ImageExperimentAuditRefusal.refused }
          defer { close(file) }
          var actual = stat()
          observeStage(.fileIdentity)
          guard fstat(file, &actual) == 0, unchanged(metadata, actual) else { throw ImageExperimentAuditRefusal.refused }
          var digest = SHA256()
          var readBytes: Int64 = 0
          var buffer = [UInt8](repeating: 0, count: 1024 * 1024)
          while true {
            observeStage(.deadline)
            try checkDeadline()
            observeStage(.fileRead)
            let count = read(file, &buffer, buffer.count)
            guard count >= 0 else { throw ImageExperimentAuditRefusal.refused }
            if count == 0 { break }
            observeStage(.fileLength)
            guard readBytes <= size - Int64(count) else { throw ImageExperimentAuditRefusal.refused }
            readBytes += Int64(count)
            digest.update(data: Data(buffer.prefix(count)))
          }
          var after = stat()
          observeStage(.fileFinalIdentity)
          guard readBytes == size, fstat(file, &after) == 0, unchanged(actual, after)
          else { throw ImageExperimentAuditRefusal.refused }
          byteCount += readBytes
          rows.append((path, Data(digest.finalize()), after))
        default: throw ImageExperimentAuditRefusal.refused
        }
      }
      var after = stat()
      observeStage(.directoryFinalIdentity)
      guard fstat(descriptor, &after) == 0, unchanged(before, after) else { throw ImageExperimentAuditRefusal.refused }
      if let entry {
        var finalPath = stat()
        guard unchanged(entry.descriptor, after),
          fstatat(entry.parent, entry.name, &finalPath, AT_SYMLINK_NOFOLLOW) == 0,
          unchanged(entry.path, finalPath), sameRootIdentity(finalPath, after)
        else { throw ImageExperimentAuditRefusal.refused }
      }
    }

    func revalidate(_ path: [UInt8], signature: stat, directory: Bool) throws {
      observeStage(.deadline)
      try checkDeadline()
      observeStage(.reopenParent)
      var parent = dup(root)
      guard parent >= 0 else { throw ImageExperimentAuditRefusal.refused }
      defer { close(parent) }
      let components = path.split(separator: 47)
      observeStage(.reopenPath)
      guard !components.isEmpty, components.count <= 129 else { throw ImageExperimentAuditRefusal.refused }
      var prefix: [UInt8] = []
      for (index, component) in components.enumerated() {
        observeStage(.deadline)
        try checkDeadline()
        observeStage(.reopenPath)
        guard let name = String(bytes: component, encoding: .utf8) else { throw ImageExperimentAuditRefusal.refused }
        let isDirectory = index < components.count - 1 || directory
        observeStage(.reopenFile)
        let descriptor = openat(parent, name, O_RDONLY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK | (isDirectory ? O_DIRECTORY : 0))
        guard descriptor >= 0 else { throw ImageExperimentAuditRefusal.refused }
        var actual = stat()
        prefix += component
        observeStage(.reopenIdentity)
        if isDirectory {
          var actualPath = stat()
          guard let expected = directories[Data(prefix)], fstat(descriptor, &actual) == 0,
            unchanged(expected.descriptor, actual), fstatat(parent, name, &actualPath, AT_SYMLINK_NOFOLLOW) == 0,
            unchanged(expected.path, actualPath), sameRootIdentity(actualPath, actual)
          else { close(descriptor); throw ImageExperimentAuditRefusal.refused }
        } else {
          guard fstat(descriptor, &actual) == 0, unchanged(signature, actual)
          else { close(descriptor); throw ImageExperimentAuditRefusal.refused }
        }
        close(parent)
        parent = descriptor
        prefix.append(47)
      }
    }

    try visit(root, prefix: [], depth: 0)
    var digest = SHA256()
    for row in rows.sorted(by: { $0.path.lexicographicallyPrecedes($1.path) }) {
      observeStage(.deadline)
      try checkDeadline()
      var length = UInt32(row.path.count).bigEndian
      withUnsafeBytes(of: &length) { digest.update(data: Data($0)) }
      digest.update(data: Data(row.path))
      digest.update(data: row.digest)
    }
    // An early file can change while later files are read. Reopen every recorded path
    // through the retained root descriptor and compare its original identity and timestamps.
    for row in rows { try revalidate(row.path, signature: row.signature, directory: false) }
    for (path, signature) in directories where !path.isEmpty {
      try revalidate([UInt8](path), signature: signature.descriptor, directory: true)
    }
    var finalRoot = stat()
    var finalPath = stat()
    observeStage(.finalRoot)
    guard fstat(root, &finalRoot) == 0, unchanged(opened, finalRoot),
      lstat(url.path, &finalPath) == 0, unchanged(initial, finalPath), sameRootIdentity(finalPath, finalRoot)
    else { throw ImageExperimentAuditRefusal.refused }
    observeStage(.deadline)
    try checkDeadline()
    return ImageExperimentBundleDigest(sha256: digest.finalize().map { String(format: "%02x", $0) }.joined(),
      fileCount: Int64(rows.count), byteCount: byteCount)
  }

  private static func unchanged(_ first: stat, _ second: stat,
    observeRootStage: (ImageExperimentAuditHashStage) -> Void = { _ in }) -> Bool {
    observeRootStage(.rootDevice)
    guard first.st_dev == second.st_dev else { return false }
    observeRootStage(.rootInode)
    guard first.st_ino == second.st_ino else { return false }
    observeRootStage(.rootMode)
    guard first.st_mode == second.st_mode else { return false }
    observeRootStage(.rootSize)
    guard first.st_size == second.st_size else { return false }
    observeRootStage(.rootMtimeSeconds)
    guard first.st_mtimespec.tv_sec == second.st_mtimespec.tv_sec else { return false }
    observeRootStage(.rootMtimeNanoseconds)
    guard first.st_mtimespec.tv_nsec == second.st_mtimespec.tv_nsec else { return false }
    observeRootStage(.rootCtimeSeconds)
    guard first.st_ctimespec.tv_sec == second.st_ctimespec.tv_sec else { return false }
    observeRootStage(.rootCtimeNanoseconds)
    return first.st_ctimespec.tv_nsec == second.st_ctimespec.tv_nsec
  }

  private static func sameRootIdentity(_ first: stat, _ second: stat,
    observeRootStage: (ImageExperimentAuditHashStage) -> Void = { _ in }) -> Bool {
    observeRootStage(.rootDevice)
    guard first.st_dev == second.st_dev else { return false }
    observeRootStage(.rootInode)
    guard first.st_ino == second.st_ino else { return false }
    observeRootStage(.rootMode)
    guard first.st_mode == second.st_mode else { return false }
    observeRootStage(.rootSize)
    return first.st_size == second.st_size
  }

  static func appReport(request: ImageExperimentAuditRequest, applicationId: String?, digest: ImageExperimentBundleDigest) throws -> String {
    try request.checkDeadline()
    guard let applicationId, applicationId == request.appId, digest.fileCount > 0, digest.fileCount <= maximumFiles,
      digest.byteCount > 0, digest.byteCount <= maximumTotalBytes, ImageExperimentAuditRequest.isHash(digest.sha256)
    else { throw ImageExperimentAuditRefusal.refused }
    return try ImageExperimentAuditJSON.encode(["schemaVersion": 1, "scope": ImageExperimentAuditRequest.scope,
      "nonce": request.nonce, "appId": applicationId, "appSha256": digest.sha256,
      "appFileCount": digest.fileCount, "appByteCount": digest.byteCount])
  }

  static func report(request: ImageExperimentAuditRequest, encodedAppReport: String,
    runnerId: String?, digest: ImageExperimentBundleDigest) throws -> String {
    try request.checkDeadline()
    guard let runnerId, runnerId == request.runnerId, digest.fileCount > 0, digest.fileCount <= maximumFiles,
      digest.byteCount > 0, digest.byteCount <= maximumTotalBytes, ImageExperimentAuditRequest.isHash(digest.sha256),
      encodedAppReport.utf8.count <= 8_192,
      let bytes = Data(base64Encoded: encodedAppReport), bytes.base64EncodedString() == encodedAppReport
    else { throw ImageExperimentAuditRefusal.refused }
    let fields = try ImageExperimentAuditJSON.parse(bytes)
    guard Set(fields.keys) == ["schemaVersion", "scope", "nonce", "appId", "appSha256", "appFileCount", "appByteCount"],
      fields["schemaVersion"]?.integer == 1, fields["scope"]?.string == ImageExperimentAuditRequest.scope,
      fields["nonce"]?.string == request.nonce, let appId = fields["appId"]?.string, appId == request.appId,
      let hash = fields["appSha256"]?.string, ImageExperimentAuditRequest.isHash(hash),
      let files = fields["appFileCount"]?.integer, files > 0, files <= maximumFiles,
      let bytes = fields["appByteCount"]?.integer, bytes > 0, bytes <= maximumTotalBytes
    else { throw ImageExperimentAuditRefusal.refused }
    return try ImageExperimentAuditJSON.encode(["schemaVersion": 1, "scope": ImageExperimentAuditRequest.scope,
      "nonce": request.nonce, "appId": appId, "runnerId": runnerId,
      "appSha256": hash, "runnerSha256": digest.sha256, "appFileCount": files,
      "appByteCount": bytes, "runnerFileCount": digest.fileCount, "runnerByteCount": digest.byteCount])
  }
}

enum ImageExperimentAuditEntryViewResult: String, CaseIterable {
  case same = "SAME"
  case stable = "STABLE"
  case mismatch = "MISMATCH"
  case unavailable = "UNAVAILABLE"

  var line: String { "IMAGE_EXPERIMENT_INSTALLATION_AUDIT_ENTRY_VIEWS " + rawValue }
}

// Borrow the refused entry's descriptors without opening, closing or resuming the scan.
// Each API must retain its complete signature, and both views must name one directory.
enum ImageExperimentAuditEntryViews {
  static func probe(parent: Int32, name: String, child: Int32, checkDeadline: () throws -> Void,
    monotonicNow: () -> UInt64 = { DispatchTime.now().uptimeNanoseconds }) -> ImageExperimentAuditEntryViewResult {
    let limit = monotonicNow().addingReportingOverflow(1_000_000_000)
    guard !limit.overflow else { return .unavailable }
    func check() throws {
      try checkDeadline()
      guard monotonicNow() < limit.partialValue else { throw ImageExperimentAuditRefusal.refused }
    }
    do {
      try check()
      guard parent >= 0, child >= 0, !name.isEmpty, name.utf8.count <= 255,
        name != ".", name != "..", !name.utf8.contains(47), !name.utf8.contains(0)
      else { throw ImageExperimentAuditRefusal.refused }
      var observations: [(stat, stat)] = []
      for _ in 0..<3 {
        try check()
        var path = stat()
        guard fstatat(parent, name, &path, AT_SYMLINK_NOFOLLOW) == 0, path.st_mode & S_IFMT == S_IFDIR
        else { throw ImageExperimentAuditRefusal.refused }
        try check()
        var opened = stat()
        guard fstat(child, &opened) == 0, opened.st_mode & S_IFMT == S_IFDIR
        else { throw ImageExperimentAuditRefusal.refused }
        observations.append((path, opened))
      }
      try check()
      return classify(observations)
    } catch { return .unavailable }
  }

  static func classify(_ observations: [(stat, stat)]) -> ImageExperimentAuditEntryViewResult {
    // The root diagnostic already defines the same eight-field stability and
    // four-field cross-API identity predicates; reuse them only for diagnosis.
    switch ImageExperimentAuditRootViews.classify(observations) {
    case .same: return .same
    case .stable: return .stable
    case .mismatch: return .mismatch
    case .unavailable: return .unavailable
    }
  }
}

enum ImageExperimentAuditRootViewResult: String, CaseIterable {
  case same = "SAME"
  case stable = "STABLE"
  case mismatch = "MISMATCH"
  case unavailable = "UNAVAILABLE"

  var line: String { "IMAGE_EXPERIMENT_INSTALLATION_AUDIT_ROOT_VIEWS " + rawValue }
}

// A refused runner may compare repeated API views for diagnosis. This never resumes
// hashing, changes the audit outcome, or exposes file metadata.
enum ImageExperimentAuditRootViews {
  static func isRootIdentityStage(_ stage: ImageExperimentAuditHashStage) -> Bool {
    switch stage {
    case .rootIdentity, .rootFstat, .rootDevice, .rootInode, .rootMode, .rootSize,
      .rootMtimeSeconds, .rootMtimeNanoseconds, .rootCtimeSeconds, .rootCtimeNanoseconds:
      return true
    default: return false
    }
  }

  static func probe(_ url: URL, checkDeadline: () throws -> Void,
    monotonicNow: () -> UInt64 = { DispatchTime.now().uptimeNanoseconds }) -> ImageExperimentAuditRootViewResult {
    let limit = monotonicNow().addingReportingOverflow(1_000_000_000)
    guard !limit.overflow else { return .unavailable }
    func check() throws {
      try checkDeadline()
      guard monotonicNow() < limit.partialValue else { throw ImageExperimentAuditRefusal.refused }
    }
    do {
      try check()
      guard url.isFileURL else { throw ImageExperimentAuditRefusal.refused }
      var path = stat()
      guard lstat(url.path, &path) == 0, path.st_mode & S_IFMT == S_IFDIR
      else { throw ImageExperimentAuditRefusal.refused }
      try check()
      let descriptor = open(url.path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK)
      guard descriptor >= 0 else { throw ImageExperimentAuditRefusal.refused }
      defer { close(descriptor) }
      try check()
      var opened = stat()
      guard fstat(descriptor, &opened) == 0, opened.st_mode & S_IFMT == S_IFDIR
      else { throw ImageExperimentAuditRefusal.refused }
      var observations = [(path, opened)]
      for _ in 0..<2 {
        try check()
        var nextPath = stat()
        guard lstat(url.path, &nextPath) == 0, nextPath.st_mode & S_IFMT == S_IFDIR
        else { throw ImageExperimentAuditRefusal.refused }
        try check()
        var nextOpened = stat()
        guard fstat(descriptor, &nextOpened) == 0, nextOpened.st_mode & S_IFMT == S_IFDIR
        else { throw ImageExperimentAuditRefusal.refused }
        observations.append((nextPath, nextOpened))
      }
      try check()
      return classify(observations)
    } catch { return .unavailable }
  }

  static func classify(_ observations: [(stat, stat)]) -> ImageExperimentAuditRootViewResult {
    guard observations.count == 3 else { return .unavailable }
    let (path, descriptor) = observations[0]
    guard observations.allSatisfy({ unchanged(path, $0.0) && unchanged(descriptor, $0.1) &&
      sameIdentity($0.0, $0.1) }) else { return .mismatch }
    return observations.allSatisfy({ unchanged($0.0, $0.1) }) ? .same : .stable
  }

  private static func sameIdentity(_ first: stat, _ second: stat) -> Bool {
    first.st_dev == second.st_dev && first.st_ino == second.st_ino &&
      first.st_mode == second.st_mode && first.st_size == second.st_size
  }

  private static func unchanged(_ first: stat, _ second: stat) -> Bool {
    sameIdentity(first, second) && first.st_mtimespec.tv_sec == second.st_mtimespec.tv_sec &&
      first.st_mtimespec.tv_nsec == second.st_mtimespec.tv_nsec &&
      first.st_ctimespec.tv_sec == second.st_ctimespec.tv_sec &&
      first.st_ctimespec.tv_nsec == second.st_ctimespec.tv_nsec
  }
}

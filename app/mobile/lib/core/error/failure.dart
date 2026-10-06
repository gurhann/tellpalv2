enum FailureKind { network, timeout, unauthorized, forbidden, invalidResponse }

class Failure {
  const Failure(this.kind);
  final FailureKind kind;
}

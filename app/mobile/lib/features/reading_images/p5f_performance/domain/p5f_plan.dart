/// Altı format sırası iki tur uygulanır; her turda görsel yönü değişir.
class P5fTrial {
  const P5fTrial(this.ordinal, this.round, this.index, this.warmup);
  final int ordinal, round, index;
  final bool warmup;
  Map<String, Object?> toJson() => {
    'ordinal': ordinal,
    'round': round,
    'index': index,
    'warmup': warmup,
  };
}

const p5fOrders = [
  [0, 1, 2],
  [0, 2, 1],
  [1, 0, 2],
  [1, 2, 0],
  [2, 0, 1],
  [2, 1, 0],
];
List<P5fTrial> p5fPlan() {
  final result = <P5fTrial>[];
  for (var index = 0; index < 12; index++) {
    result.add(P5fTrial(result.length, -1, index, true));
  }
  for (var round = 0; round < 12; round++) {
    final images = round.isEven ? [0, 1, 2, 3] : [3, 2, 1, 0];
    for (final image in images) {
      for (final arm in p5fOrders[round % 6]) {
        result.add(P5fTrial(result.length, round, image * 3 + arm, false));
      }
    }
  }
  return List.unmodifiable(result);
}

import 'p5d_pins.dart';

/// Dört kaynağın yazı alanı; tripletin bütün formatları aynı koordinatı kullanır.
const p5dTextCrops = <int, ({int x, int y})>{
  1950: (x: 400, y: 760),
  1952: (x: 440, y: 2160),
  8303: (x: 240, y: 1410),
  8306: (x: 240, y: 240),
};

({int x, int y}) p5dTextCrop(int index) =>
    p5dTextCrops[p5dPins[index]['assetId']]!;

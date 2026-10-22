import 'package:json_annotation/json_annotation.dart';
import '../../domain/entity/reading_image_comparison.dart';
import 'reading_image_descriptor_parser.dart';
part 'reading_image_comparison_model.g.dart';

@JsonSerializable(createToJson: false)
class ReadingImageComparisonModel extends ReadingImageComparison {
  ReadingImageComparisonModel({
    required this.identity,
    required this.descriptor,
    required int leaseMillis,
  }) : super(
         parseReadingManifest(identity, descriptor),
         parseReadingContext(identity, descriptor),
         identity['liveGeneration'] as int,
         leaseMillis,
       );
  final Map<String, dynamic> identity, descriptor;
  factory ReadingImageComparisonModel.fromJson(Map<String, dynamic> json) =>
      _$ReadingImageComparisonModelFromJson(json);
}

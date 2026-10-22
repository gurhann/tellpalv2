import 'package:json_annotation/json_annotation.dart';
import '../domain/reading_image_admission.dart';
import '../../data/model/reading_image_descriptor_parser.dart';
part 'reading_image_admission_model.g.dart';

@JsonSerializable(createToJson: false)
class ReadingImageAdmissionModel extends ReadingImageAdmission {
  ReadingImageAdmissionModel({
    required super.accountScope,
    required this.delivery,
  }) : super(
         manifest: parseReadingManifest(
           delivery['identity'] as Map<String, dynamic>,
           delivery['descriptor'] as Map<String, dynamic>,
         ),
         context: parseReadingContext(
           delivery['identity'] as Map<String, dynamic>,
           delivery['descriptor'] as Map<String, dynamic>,
         ),
         liveGeneration:
             (delivery['identity'] as Map<String, dynamic>)['liveGeneration']
                 as int,
         leaseMillis: delivery['leaseMillis'] as int,
       );
  final Map<String, dynamic> delivery;
  factory ReadingImageAdmissionModel.fromJson(Map<String, dynamic> json) =>
      _$ReadingImageAdmissionModelFromJson(json);
}

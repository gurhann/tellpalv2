import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'local_lossless_qa_injection.config.dart';

@InjectableInit(
  initializerName: 'initLocalLosslessQa',
  generateForDir: ['lib/app/local_lossless_qa'],
)
GetIt configureLocalLosslessQa(GetIt container) =>
    container.initLocalLosslessQa(environment: 'localLosslessQa');

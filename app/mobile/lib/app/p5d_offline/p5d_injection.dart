import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'p5d_injection.config.dart';

@InjectableInit(
  initializerName: 'initP5d',
  generateForDir: ['lib/p5d_offline_bindings'],
)
GetIt configureP5d(GetIt container) =>
    container.initP5d(environment: 'p5dOffline');

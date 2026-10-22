import 'package:get_it/get_it.dart';
import 'package:injectable/injectable.dart';
import 'p5e_injection.config.dart';

@InjectableInit(
  initializerName: 'initP5e',
  generateForDir: ['lib/p5e_offline_bindings'],
)
GetIt configureP5e(GetIt container) =>
    container.initP5e(environment: 'p5eOffline');

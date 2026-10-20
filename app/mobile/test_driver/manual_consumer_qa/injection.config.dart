// GENERATED CODE - DO NOT MODIFY BY HAND
// dart format width=80

// **************************************************************************
// InjectableConfigGenerator
// **************************************************************************

// ignore_for_file: type=lint
// coverage:ignore-file

// ignore_for_file: no_leading_underscores_for_library_prefixes

import 'package:get_it/get_it.dart' as _i174;
import 'package:injectable/injectable.dart' as _i526;

import '../reading_consumer_qa_probe.dart' as _i585;
import 'coordinator.dart' as _i900;
import 'private_protocol.dart' as _i3;

extension GetItInjectableX on _i174.GetIt {
  // initializes the registration of main-scope dependencies inside of GetIt
  _i174.GetIt init({
    String? environment,
    _i526.EnvironmentFilter? environmentFilter,
  }) {
    final gh = _i526.GetItHelper(this, environment, environmentFilter);
    gh.factoryParam<
      _i900.ManualConsumerQaCoordinator,
      _i3.ManualQaPrivateProtocol,
      _i585.ReadingConsumerQaProbe?
    >(
      (protocol, suppliedProbe) =>
          _i900.ManualConsumerQaCoordinator(protocol, suppliedProbe),
    );
    return this;
  }
}

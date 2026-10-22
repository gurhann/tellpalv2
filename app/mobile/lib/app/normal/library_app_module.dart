import 'package:injectable/injectable.dart';
import '../router.dart';

/// Normal root yalnız kendi router kaydını toplar; ayrı QA modülleri bu kapsamda değildir.
@module
abstract class LibraryAppModule {
  @lazySingleton
  LibraryRouter get router => LibraryRouter();
}

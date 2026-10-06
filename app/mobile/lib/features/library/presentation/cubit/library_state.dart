import 'package:freezed_annotation/freezed_annotation.dart';
import '../../domain/entity/library_catalog.dart';
import '../../../../core/error/failure.dart';
part 'library_state.freezed.dart';

enum LibraryStatus { loading, loaded, empty, error }

@freezed
abstract class LibraryState with _$LibraryState {
  const factory LibraryState({
    required LibraryContext context,
    @Default(0) int attempt,
    @Default(LibraryStatus.loading) LibraryStatus status,
    LibraryCatalog? catalog,
    Failure? failure,
  }) = _LibraryState;
}

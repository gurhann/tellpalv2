import 'dart:async';
import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:injectable/injectable.dart';
import '../../../../app/library_configuration.dart';
import '../../domain/entity/library_catalog.dart';
part 'artwork_cubit.freezed.dart';

class ArtworkIdentity extends Equatable {
  const ArtworkIdentity(
    this.context,
    this.catalogAttempt,
    this.contentId,
    this.assetId,
  );
  final LibraryContext context;
  final int catalogAttempt;
  final int contentId;
  final int? assetId;
  @override
  List<Object?> get props => [context, catalogAttempt, contentId, assetId];
}

enum ArtworkStatus { loading, loaded, error }

@freezed
abstract class ArtworkState with _$ArtworkState {
  const factory ArtworkState({
    ArtworkIdentity? identity,
    @Default(0) int attempt,
    @Default(ArtworkStatus.loading) ArtworkStatus status,
  }) = _ArtworkState;
}

@injectable
class ArtworkCubit extends Cubit<ArtworkState> {
  ArtworkCubit(this._configuration) : super(const ArtworkState());
  final LibraryConfiguration _configuration;
  Timer? _timer;
  int _attempt = 0;
  int start(ArtworkIdentity identity, {required bool available}) {
    _timer?.cancel();
    final attempt = ++_attempt;
    emit(
      ArtworkState(
        identity: identity,
        attempt: attempt,
        status: available ? ArtworkStatus.loading : ArtworkStatus.error,
      ),
    );
    if (available) {
      _timer = Timer(_configuration.artworkWait, () => fail(identity, attempt));
    }
    return attempt;
  }

  void success(ArtworkIdentity identity, int attempt) {
    if (!_current(identity, attempt) || state.status != ArtworkStatus.loading) {
      return;
    }
    _timer?.cancel();
    emit(state.copyWith(status: ArtworkStatus.loaded));
  }

  bool fail(ArtworkIdentity identity, int attempt) {
    if (!_current(identity, attempt) || state.status != ArtworkStatus.loading) {
      return false;
    }
    _timer?.cancel();
    _attempt++;
    emit(state.copyWith(status: ArtworkStatus.error));
    return true;
  }

  bool _current(ArtworkIdentity identity, int attempt) =>
      !isClosed && identity == state.identity && attempt == _attempt;
  @override
  Future<void> close() {
    _attempt++;
    _timer?.cancel();
    return super.close();
  }
}

// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'story_detail_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$StoryDetailState {

 StoryDetailIdentity? get identity; int get attempt; int get metadataAttempt; StoryDetailStatus get status; StoryDetail? get detail; Failure? get failure; bool get narration; bool get visible; StoryHeroStatus get heroStatus; int get heroAttempt; bool get recoveringArtwork;
/// Create a copy of StoryDetailState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$StoryDetailStateCopyWith<StoryDetailState> get copyWith => _$StoryDetailStateCopyWithImpl<StoryDetailState>(this as StoryDetailState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is StoryDetailState&&(identical(other.identity, identity) || other.identity == identity)&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.metadataAttempt, metadataAttempt) || other.metadataAttempt == metadataAttempt)&&(identical(other.status, status) || other.status == status)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.failure, failure) || other.failure == failure)&&(identical(other.narration, narration) || other.narration == narration)&&(identical(other.visible, visible) || other.visible == visible)&&(identical(other.heroStatus, heroStatus) || other.heroStatus == heroStatus)&&(identical(other.heroAttempt, heroAttempt) || other.heroAttempt == heroAttempt)&&(identical(other.recoveringArtwork, recoveringArtwork) || other.recoveringArtwork == recoveringArtwork));
}


@override
int get hashCode => Object.hash(runtimeType,identity,attempt,metadataAttempt,status,detail,failure,narration,visible,heroStatus,heroAttempt,recoveringArtwork);

@override
String toString() {
  return 'StoryDetailState(identity: $identity, attempt: $attempt, metadataAttempt: $metadataAttempt, status: $status, detail: $detail, failure: $failure, narration: $narration, visible: $visible, heroStatus: $heroStatus, heroAttempt: $heroAttempt, recoveringArtwork: $recoveringArtwork)';
}


}

/// @nodoc
abstract mixin class $StoryDetailStateCopyWith<$Res>  {
  factory $StoryDetailStateCopyWith(StoryDetailState value, $Res Function(StoryDetailState) _then) = _$StoryDetailStateCopyWithImpl;
@useResult
$Res call({
 StoryDetailIdentity? identity, int attempt, int metadataAttempt, StoryDetailStatus status, StoryDetail? detail, Failure? failure, bool narration, bool visible, StoryHeroStatus heroStatus, int heroAttempt, bool recoveringArtwork
});




}
/// @nodoc
class _$StoryDetailStateCopyWithImpl<$Res>
    implements $StoryDetailStateCopyWith<$Res> {
  _$StoryDetailStateCopyWithImpl(this._self, this._then);

  final StoryDetailState _self;
  final $Res Function(StoryDetailState) _then;

/// Create a copy of StoryDetailState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? identity = freezed,Object? attempt = null,Object? metadataAttempt = null,Object? status = null,Object? detail = freezed,Object? failure = freezed,Object? narration = null,Object? visible = null,Object? heroStatus = null,Object? heroAttempt = null,Object? recoveringArtwork = null,}) {
  return _then(_self.copyWith(
identity: freezed == identity ? _self.identity : identity // ignore: cast_nullable_to_non_nullable
as StoryDetailIdentity?,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,metadataAttempt: null == metadataAttempt ? _self.metadataAttempt : metadataAttempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as StoryDetailStatus,detail: freezed == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as StoryDetail?,failure: freezed == failure ? _self.failure : failure // ignore: cast_nullable_to_non_nullable
as Failure?,narration: null == narration ? _self.narration : narration // ignore: cast_nullable_to_non_nullable
as bool,visible: null == visible ? _self.visible : visible // ignore: cast_nullable_to_non_nullable
as bool,heroStatus: null == heroStatus ? _self.heroStatus : heroStatus // ignore: cast_nullable_to_non_nullable
as StoryHeroStatus,heroAttempt: null == heroAttempt ? _self.heroAttempt : heroAttempt // ignore: cast_nullable_to_non_nullable
as int,recoveringArtwork: null == recoveringArtwork ? _self.recoveringArtwork : recoveringArtwork // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [StoryDetailState].
extension StoryDetailStatePatterns on StoryDetailState {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _StoryDetailState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _StoryDetailState() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _StoryDetailState value)  $default,){
final _that = this;
switch (_that) {
case _StoryDetailState():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _StoryDetailState value)?  $default,){
final _that = this;
switch (_that) {
case _StoryDetailState() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( StoryDetailIdentity? identity,  int attempt,  int metadataAttempt,  StoryDetailStatus status,  StoryDetail? detail,  Failure? failure,  bool narration,  bool visible,  StoryHeroStatus heroStatus,  int heroAttempt,  bool recoveringArtwork)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _StoryDetailState() when $default != null:
return $default(_that.identity,_that.attempt,_that.metadataAttempt,_that.status,_that.detail,_that.failure,_that.narration,_that.visible,_that.heroStatus,_that.heroAttempt,_that.recoveringArtwork);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( StoryDetailIdentity? identity,  int attempt,  int metadataAttempt,  StoryDetailStatus status,  StoryDetail? detail,  Failure? failure,  bool narration,  bool visible,  StoryHeroStatus heroStatus,  int heroAttempt,  bool recoveringArtwork)  $default,) {final _that = this;
switch (_that) {
case _StoryDetailState():
return $default(_that.identity,_that.attempt,_that.metadataAttempt,_that.status,_that.detail,_that.failure,_that.narration,_that.visible,_that.heroStatus,_that.heroAttempt,_that.recoveringArtwork);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( StoryDetailIdentity? identity,  int attempt,  int metadataAttempt,  StoryDetailStatus status,  StoryDetail? detail,  Failure? failure,  bool narration,  bool visible,  StoryHeroStatus heroStatus,  int heroAttempt,  bool recoveringArtwork)?  $default,) {final _that = this;
switch (_that) {
case _StoryDetailState() when $default != null:
return $default(_that.identity,_that.attempt,_that.metadataAttempt,_that.status,_that.detail,_that.failure,_that.narration,_that.visible,_that.heroStatus,_that.heroAttempt,_that.recoveringArtwork);case _:
  return null;

}
}

}

/// @nodoc


class _StoryDetailState implements StoryDetailState {
  const _StoryDetailState({this.identity, this.attempt = 0, this.metadataAttempt = 0, this.status = StoryDetailStatus.loading, this.detail, this.failure, this.narration = true, this.visible = true, this.heroStatus = StoryHeroStatus.loading, this.heroAttempt = 0, this.recoveringArtwork = false});


@override final  StoryDetailIdentity? identity;
@override@JsonKey() final  int attempt;
@override@JsonKey() final  int metadataAttempt;
@override@JsonKey() final  StoryDetailStatus status;
@override final  StoryDetail? detail;
@override final  Failure? failure;
@override@JsonKey() final  bool narration;
@override@JsonKey() final  bool visible;
@override@JsonKey() final  StoryHeroStatus heroStatus;
@override@JsonKey() final  int heroAttempt;
@override@JsonKey() final  bool recoveringArtwork;

/// Create a copy of StoryDetailState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$StoryDetailStateCopyWith<_StoryDetailState> get copyWith => __$StoryDetailStateCopyWithImpl<_StoryDetailState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _StoryDetailState&&(identical(other.identity, identity) || other.identity == identity)&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.metadataAttempt, metadataAttempt) || other.metadataAttempt == metadataAttempt)&&(identical(other.status, status) || other.status == status)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.failure, failure) || other.failure == failure)&&(identical(other.narration, narration) || other.narration == narration)&&(identical(other.visible, visible) || other.visible == visible)&&(identical(other.heroStatus, heroStatus) || other.heroStatus == heroStatus)&&(identical(other.heroAttempt, heroAttempt) || other.heroAttempt == heroAttempt)&&(identical(other.recoveringArtwork, recoveringArtwork) || other.recoveringArtwork == recoveringArtwork));
}


@override
int get hashCode => Object.hash(runtimeType,identity,attempt,metadataAttempt,status,detail,failure,narration,visible,heroStatus,heroAttempt,recoveringArtwork);

@override
String toString() {
  return 'StoryDetailState(identity: $identity, attempt: $attempt, metadataAttempt: $metadataAttempt, status: $status, detail: $detail, failure: $failure, narration: $narration, visible: $visible, heroStatus: $heroStatus, heroAttempt: $heroAttempt, recoveringArtwork: $recoveringArtwork)';
}


}

/// @nodoc
abstract mixin class _$StoryDetailStateCopyWith<$Res> implements $StoryDetailStateCopyWith<$Res> {
  factory _$StoryDetailStateCopyWith(_StoryDetailState value, $Res Function(_StoryDetailState) _then) = __$StoryDetailStateCopyWithImpl;
@override @useResult
$Res call({
 StoryDetailIdentity? identity, int attempt, int metadataAttempt, StoryDetailStatus status, StoryDetail? detail, Failure? failure, bool narration, bool visible, StoryHeroStatus heroStatus, int heroAttempt, bool recoveringArtwork
});




}
/// @nodoc
class __$StoryDetailStateCopyWithImpl<$Res>
    implements _$StoryDetailStateCopyWith<$Res> {
  __$StoryDetailStateCopyWithImpl(this._self, this._then);

  final _StoryDetailState _self;
  final $Res Function(_StoryDetailState) _then;

/// Create a copy of StoryDetailState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? identity = freezed,Object? attempt = null,Object? metadataAttempt = null,Object? status = null,Object? detail = freezed,Object? failure = freezed,Object? narration = null,Object? visible = null,Object? heroStatus = null,Object? heroAttempt = null,Object? recoveringArtwork = null,}) {
  return _then(_StoryDetailState(
identity: freezed == identity ? _self.identity : identity // ignore: cast_nullable_to_non_nullable
as StoryDetailIdentity?,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,metadataAttempt: null == metadataAttempt ? _self.metadataAttempt : metadataAttempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as StoryDetailStatus,detail: freezed == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as StoryDetail?,failure: freezed == failure ? _self.failure : failure // ignore: cast_nullable_to_non_nullable
as Failure?,narration: null == narration ? _self.narration : narration // ignore: cast_nullable_to_non_nullable
as bool,visible: null == visible ? _self.visible : visible // ignore: cast_nullable_to_non_nullable
as bool,heroStatus: null == heroStatus ? _self.heroStatus : heroStatus // ignore: cast_nullable_to_non_nullable
as StoryHeroStatus,heroAttempt: null == heroAttempt ? _self.heroAttempt : heroAttempt // ignore: cast_nullable_to_non_nullable
as int,recoveringArtwork: null == recoveringArtwork ? _self.recoveringArtwork : recoveringArtwork // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on

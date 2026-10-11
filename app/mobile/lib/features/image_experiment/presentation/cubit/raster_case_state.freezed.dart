// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'raster_case_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$RasterCaseState {

 RasterCaseStatus get status; RasterCase? get fixture; int get attempt;
/// Create a copy of RasterCaseState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RasterCaseStateCopyWith<RasterCaseState> get copyWith => _$RasterCaseStateCopyWithImpl<RasterCaseState>(this as RasterCaseState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RasterCaseState&&(identical(other.status, status) || other.status == status)&&(identical(other.fixture, fixture) || other.fixture == fixture)&&(identical(other.attempt, attempt) || other.attempt == attempt));
}


@override
int get hashCode => Object.hash(runtimeType,status,fixture,attempt);

@override
String toString() {
  return 'RasterCaseState(status: $status, fixture: $fixture, attempt: $attempt)';
}


}

/// @nodoc
abstract mixin class $RasterCaseStateCopyWith<$Res>  {
  factory $RasterCaseStateCopyWith(RasterCaseState value, $Res Function(RasterCaseState) _then) = _$RasterCaseStateCopyWithImpl;
@useResult
$Res call({
 RasterCaseStatus status, RasterCase? fixture, int attempt
});




}
/// @nodoc
class _$RasterCaseStateCopyWithImpl<$Res>
    implements $RasterCaseStateCopyWith<$Res> {
  _$RasterCaseStateCopyWithImpl(this._self, this._then);

  final RasterCaseState _self;
  final $Res Function(RasterCaseState) _then;

/// Create a copy of RasterCaseState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? fixture = freezed,Object? attempt = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as RasterCaseStatus,fixture: freezed == fixture ? _self.fixture : fixture // ignore: cast_nullable_to_non_nullable
as RasterCase?,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [RasterCaseState].
extension RasterCaseStatePatterns on RasterCaseState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RasterCaseState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RasterCaseState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RasterCaseState value)  $default,){
final _that = this;
switch (_that) {
case _RasterCaseState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RasterCaseState value)?  $default,){
final _that = this;
switch (_that) {
case _RasterCaseState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( RasterCaseStatus status,  RasterCase? fixture,  int attempt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RasterCaseState() when $default != null:
return $default(_that.status,_that.fixture,_that.attempt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( RasterCaseStatus status,  RasterCase? fixture,  int attempt)  $default,) {final _that = this;
switch (_that) {
case _RasterCaseState():
return $default(_that.status,_that.fixture,_that.attempt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( RasterCaseStatus status,  RasterCase? fixture,  int attempt)?  $default,) {final _that = this;
switch (_that) {
case _RasterCaseState() when $default != null:
return $default(_that.status,_that.fixture,_that.attempt);case _:
  return null;

}
}

}

/// @nodoc


class _RasterCaseState implements RasterCaseState {
  const _RasterCaseState({this.status = RasterCaseStatus.initial, this.fixture, this.attempt = 0});


@override@JsonKey() final  RasterCaseStatus status;
@override final  RasterCase? fixture;
@override@JsonKey() final  int attempt;

/// Create a copy of RasterCaseState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RasterCaseStateCopyWith<_RasterCaseState> get copyWith => __$RasterCaseStateCopyWithImpl<_RasterCaseState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RasterCaseState&&(identical(other.status, status) || other.status == status)&&(identical(other.fixture, fixture) || other.fixture == fixture)&&(identical(other.attempt, attempt) || other.attempt == attempt));
}


@override
int get hashCode => Object.hash(runtimeType,status,fixture,attempt);

@override
String toString() {
  return 'RasterCaseState(status: $status, fixture: $fixture, attempt: $attempt)';
}


}

/// @nodoc
abstract mixin class _$RasterCaseStateCopyWith<$Res> implements $RasterCaseStateCopyWith<$Res> {
  factory _$RasterCaseStateCopyWith(_RasterCaseState value, $Res Function(_RasterCaseState) _then) = __$RasterCaseStateCopyWithImpl;
@override @useResult
$Res call({
 RasterCaseStatus status, RasterCase? fixture, int attempt
});




}
/// @nodoc
class __$RasterCaseStateCopyWithImpl<$Res>
    implements _$RasterCaseStateCopyWith<$Res> {
  __$RasterCaseStateCopyWithImpl(this._self, this._then);

  final _RasterCaseState _self;
  final $Res Function(_RasterCaseState) _then;

/// Create a copy of RasterCaseState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? fixture = freezed,Object? attempt = null,}) {
  return _then(_RasterCaseState(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as RasterCaseStatus,fixture: freezed == fixture ? _self.fixture : fixture // ignore: cast_nullable_to_non_nullable
as RasterCase?,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on

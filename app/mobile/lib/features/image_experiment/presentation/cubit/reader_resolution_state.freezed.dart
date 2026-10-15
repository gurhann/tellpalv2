// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'reader_resolution_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ReaderResolutionState {

 ReaderResolutionStatus get status;
/// Create a copy of ReaderResolutionState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ReaderResolutionStateCopyWith<ReaderResolutionState> get copyWith => _$ReaderResolutionStateCopyWithImpl<ReaderResolutionState>(this as ReaderResolutionState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ReaderResolutionState&&(identical(other.status, status) || other.status == status));
}


@override
int get hashCode => Object.hash(runtimeType,status);

@override
String toString() {
  return 'ReaderResolutionState(status: $status)';
}


}

/// @nodoc
abstract mixin class $ReaderResolutionStateCopyWith<$Res>  {
  factory $ReaderResolutionStateCopyWith(ReaderResolutionState value, $Res Function(ReaderResolutionState) _then) = _$ReaderResolutionStateCopyWithImpl;
@useResult
$Res call({
 ReaderResolutionStatus status
});




}
/// @nodoc
class _$ReaderResolutionStateCopyWithImpl<$Res>
    implements $ReaderResolutionStateCopyWith<$Res> {
  _$ReaderResolutionStateCopyWithImpl(this._self, this._then);

  final ReaderResolutionState _self;
  final $Res Function(ReaderResolutionState) _then;

/// Create a copy of ReaderResolutionState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ReaderResolutionStatus,
  ));
}

}


/// Adds pattern-matching-related methods to [ReaderResolutionState].
extension ReaderResolutionStatePatterns on ReaderResolutionState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ReaderResolutionState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ReaderResolutionState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ReaderResolutionState value)  $default,){
final _that = this;
switch (_that) {
case _ReaderResolutionState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ReaderResolutionState value)?  $default,){
final _that = this;
switch (_that) {
case _ReaderResolutionState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ReaderResolutionStatus status)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ReaderResolutionState() when $default != null:
return $default(_that.status);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ReaderResolutionStatus status)  $default,) {final _that = this;
switch (_that) {
case _ReaderResolutionState():
return $default(_that.status);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ReaderResolutionStatus status)?  $default,) {final _that = this;
switch (_that) {
case _ReaderResolutionState() when $default != null:
return $default(_that.status);case _:
  return null;

}
}

}

/// @nodoc


class _ReaderResolutionState implements ReaderResolutionState {
  const _ReaderResolutionState({this.status = ReaderResolutionStatus.initial});


@override@JsonKey() final  ReaderResolutionStatus status;

/// Create a copy of ReaderResolutionState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ReaderResolutionStateCopyWith<_ReaderResolutionState> get copyWith => __$ReaderResolutionStateCopyWithImpl<_ReaderResolutionState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ReaderResolutionState&&(identical(other.status, status) || other.status == status));
}


@override
int get hashCode => Object.hash(runtimeType,status);

@override
String toString() {
  return 'ReaderResolutionState(status: $status)';
}


}

/// @nodoc
abstract mixin class _$ReaderResolutionStateCopyWith<$Res> implements $ReaderResolutionStateCopyWith<$Res> {
  factory _$ReaderResolutionStateCopyWith(_ReaderResolutionState value, $Res Function(_ReaderResolutionState) _then) = __$ReaderResolutionStateCopyWithImpl;
@override @useResult
$Res call({
 ReaderResolutionStatus status
});




}
/// @nodoc
class __$ReaderResolutionStateCopyWithImpl<$Res>
    implements _$ReaderResolutionStateCopyWith<$Res> {
  __$ReaderResolutionStateCopyWithImpl(this._self, this._then);

  final _ReaderResolutionState _self;
  final $Res Function(_ReaderResolutionState) _then;

/// Create a copy of ReaderResolutionState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,}) {
  return _then(_ReaderResolutionState(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ReaderResolutionStatus,
  ));
}


}

// dart format on

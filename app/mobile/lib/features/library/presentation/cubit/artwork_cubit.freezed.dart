// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'artwork_cubit.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ArtworkState {

 ArtworkIdentity? get identity; int get attempt; ArtworkStatus get status;
/// Create a copy of ArtworkState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ArtworkStateCopyWith<ArtworkState> get copyWith => _$ArtworkStateCopyWithImpl<ArtworkState>(this as ArtworkState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ArtworkState&&(identical(other.identity, identity) || other.identity == identity)&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.status, status) || other.status == status));
}


@override
int get hashCode => Object.hash(runtimeType,identity,attempt,status);

@override
String toString() {
  return 'ArtworkState(identity: $identity, attempt: $attempt, status: $status)';
}


}

/// @nodoc
abstract mixin class $ArtworkStateCopyWith<$Res>  {
  factory $ArtworkStateCopyWith(ArtworkState value, $Res Function(ArtworkState) _then) = _$ArtworkStateCopyWithImpl;
@useResult
$Res call({
 ArtworkIdentity? identity, int attempt, ArtworkStatus status
});




}
/// @nodoc
class _$ArtworkStateCopyWithImpl<$Res>
    implements $ArtworkStateCopyWith<$Res> {
  _$ArtworkStateCopyWithImpl(this._self, this._then);

  final ArtworkState _self;
  final $Res Function(ArtworkState) _then;

/// Create a copy of ArtworkState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? identity = freezed,Object? attempt = null,Object? status = null,}) {
  return _then(_self.copyWith(
identity: freezed == identity ? _self.identity : identity // ignore: cast_nullable_to_non_nullable
as ArtworkIdentity?,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ArtworkStatus,
  ));
}

}


/// Adds pattern-matching-related methods to [ArtworkState].
extension ArtworkStatePatterns on ArtworkState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ArtworkState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ArtworkState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ArtworkState value)  $default,){
final _that = this;
switch (_that) {
case _ArtworkState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ArtworkState value)?  $default,){
final _that = this;
switch (_that) {
case _ArtworkState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ArtworkIdentity? identity,  int attempt,  ArtworkStatus status)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ArtworkState() when $default != null:
return $default(_that.identity,_that.attempt,_that.status);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ArtworkIdentity? identity,  int attempt,  ArtworkStatus status)  $default,) {final _that = this;
switch (_that) {
case _ArtworkState():
return $default(_that.identity,_that.attempt,_that.status);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ArtworkIdentity? identity,  int attempt,  ArtworkStatus status)?  $default,) {final _that = this;
switch (_that) {
case _ArtworkState() when $default != null:
return $default(_that.identity,_that.attempt,_that.status);case _:
  return null;

}
}

}

/// @nodoc


class _ArtworkState implements ArtworkState {
  const _ArtworkState({this.identity, this.attempt = 0, this.status = ArtworkStatus.loading});
  

@override final  ArtworkIdentity? identity;
@override@JsonKey() final  int attempt;
@override@JsonKey() final  ArtworkStatus status;

/// Create a copy of ArtworkState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ArtworkStateCopyWith<_ArtworkState> get copyWith => __$ArtworkStateCopyWithImpl<_ArtworkState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ArtworkState&&(identical(other.identity, identity) || other.identity == identity)&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.status, status) || other.status == status));
}


@override
int get hashCode => Object.hash(runtimeType,identity,attempt,status);

@override
String toString() {
  return 'ArtworkState(identity: $identity, attempt: $attempt, status: $status)';
}


}

/// @nodoc
abstract mixin class _$ArtworkStateCopyWith<$Res> implements $ArtworkStateCopyWith<$Res> {
  factory _$ArtworkStateCopyWith(_ArtworkState value, $Res Function(_ArtworkState) _then) = __$ArtworkStateCopyWithImpl;
@override @useResult
$Res call({
 ArtworkIdentity? identity, int attempt, ArtworkStatus status
});




}
/// @nodoc
class __$ArtworkStateCopyWithImpl<$Res>
    implements _$ArtworkStateCopyWith<$Res> {
  __$ArtworkStateCopyWithImpl(this._self, this._then);

  final _ArtworkState _self;
  final $Res Function(_ArtworkState) _then;

/// Create a copy of ArtworkState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? identity = freezed,Object? attempt = null,Object? status = null,}) {
  return _then(_ArtworkState(
identity: freezed == identity ? _self.identity : identity // ignore: cast_nullable_to_non_nullable
as ArtworkIdentity?,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ArtworkStatus,
  ));
}


}

// dart format on

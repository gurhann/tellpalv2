// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'coordinator.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ManualConsumerQaState {

 String get stage; int get revision;
/// Create a copy of ManualConsumerQaState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ManualConsumerQaStateCopyWith<ManualConsumerQaState> get copyWith => _$ManualConsumerQaStateCopyWithImpl<ManualConsumerQaState>(this as ManualConsumerQaState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ManualConsumerQaState&&(identical(other.stage, stage) || other.stage == stage)&&(identical(other.revision, revision) || other.revision == revision));
}


@override
int get hashCode => Object.hash(runtimeType,stage,revision);

@override
String toString() {
  return 'ManualConsumerQaState(stage: $stage, revision: $revision)';
}


}

/// @nodoc
abstract mixin class $ManualConsumerQaStateCopyWith<$Res>  {
  factory $ManualConsumerQaStateCopyWith(ManualConsumerQaState value, $Res Function(ManualConsumerQaState) _then) = _$ManualConsumerQaStateCopyWithImpl;
@useResult
$Res call({
 String stage, int revision
});




}
/// @nodoc
class _$ManualConsumerQaStateCopyWithImpl<$Res>
    implements $ManualConsumerQaStateCopyWith<$Res> {
  _$ManualConsumerQaStateCopyWithImpl(this._self, this._then);

  final ManualConsumerQaState _self;
  final $Res Function(ManualConsumerQaState) _then;

/// Create a copy of ManualConsumerQaState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? stage = null,Object? revision = null,}) {
  return _then(_self.copyWith(
stage: null == stage ? _self.stage : stage // ignore: cast_nullable_to_non_nullable
as String,revision: null == revision ? _self.revision : revision // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [ManualConsumerQaState].
extension ManualConsumerQaStatePatterns on ManualConsumerQaState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ManualConsumerQaState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ManualConsumerQaState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ManualConsumerQaState value)  $default,){
final _that = this;
switch (_that) {
case _ManualConsumerQaState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ManualConsumerQaState value)?  $default,){
final _that = this;
switch (_that) {
case _ManualConsumerQaState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String stage,  int revision)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ManualConsumerQaState() when $default != null:
return $default(_that.stage,_that.revision);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String stage,  int revision)  $default,) {final _that = this;
switch (_that) {
case _ManualConsumerQaState():
return $default(_that.stage,_that.revision);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String stage,  int revision)?  $default,) {final _that = this;
switch (_that) {
case _ManualConsumerQaState() when $default != null:
return $default(_that.stage,_that.revision);case _:
  return null;

}
}

}

/// @nodoc


class _ManualConsumerQaState implements ManualConsumerQaState {
  const _ManualConsumerQaState({this.stage = 'baseline', this.revision = 0});
  

@override@JsonKey() final  String stage;
@override@JsonKey() final  int revision;

/// Create a copy of ManualConsumerQaState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ManualConsumerQaStateCopyWith<_ManualConsumerQaState> get copyWith => __$ManualConsumerQaStateCopyWithImpl<_ManualConsumerQaState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ManualConsumerQaState&&(identical(other.stage, stage) || other.stage == stage)&&(identical(other.revision, revision) || other.revision == revision));
}


@override
int get hashCode => Object.hash(runtimeType,stage,revision);

@override
String toString() {
  return 'ManualConsumerQaState(stage: $stage, revision: $revision)';
}


}

/// @nodoc
abstract mixin class _$ManualConsumerQaStateCopyWith<$Res> implements $ManualConsumerQaStateCopyWith<$Res> {
  factory _$ManualConsumerQaStateCopyWith(_ManualConsumerQaState value, $Res Function(_ManualConsumerQaState) _then) = __$ManualConsumerQaStateCopyWithImpl;
@override @useResult
$Res call({
 String stage, int revision
});




}
/// @nodoc
class __$ManualConsumerQaStateCopyWithImpl<$Res>
    implements _$ManualConsumerQaStateCopyWith<$Res> {
  __$ManualConsumerQaStateCopyWithImpl(this._self, this._then);

  final _ManualConsumerQaState _self;
  final $Res Function(_ManualConsumerQaState) _then;

/// Create a copy of ManualConsumerQaState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? stage = null,Object? revision = null,}) {
  return _then(_ManualConsumerQaState(
stage: null == stage ? _self.stage : stage // ignore: cast_nullable_to_non_nullable
as String,revision: null == revision ? _self.revision : revision // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on

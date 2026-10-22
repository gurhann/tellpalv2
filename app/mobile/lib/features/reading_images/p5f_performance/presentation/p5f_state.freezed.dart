// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'p5f_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$P5fState {

 String get status; int get completed; int get index; int get epoch; ReadingComparisonRaster? get raster; String? get error;
/// Create a copy of P5fState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$P5fStateCopyWith<P5fState> get copyWith => _$P5fStateCopyWithImpl<P5fState>(this as P5fState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is P5fState&&(identical(other.status, status) || other.status == status)&&(identical(other.completed, completed) || other.completed == completed)&&(identical(other.index, index) || other.index == index)&&(identical(other.epoch, epoch) || other.epoch == epoch)&&(identical(other.raster, raster) || other.raster == raster)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,status,completed,index,epoch,raster,error);

@override
String toString() {
  return 'P5fState(status: $status, completed: $completed, index: $index, epoch: $epoch, raster: $raster, error: $error)';
}


}

/// @nodoc
abstract mixin class $P5fStateCopyWith<$Res>  {
  factory $P5fStateCopyWith(P5fState value, $Res Function(P5fState) _then) = _$P5fStateCopyWithImpl;
@useResult
$Res call({
 String status, int completed, int index, int epoch, ReadingComparisonRaster? raster, String? error
});




}
/// @nodoc
class _$P5fStateCopyWithImpl<$Res>
    implements $P5fStateCopyWith<$Res> {
  _$P5fStateCopyWithImpl(this._self, this._then);

  final P5fState _self;
  final $Res Function(P5fState) _then;

/// Create a copy of P5fState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? completed = null,Object? index = null,Object? epoch = null,Object? raster = freezed,Object? error = freezed,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,completed: null == completed ? _self.completed : completed // ignore: cast_nullable_to_non_nullable
as int,index: null == index ? _self.index : index // ignore: cast_nullable_to_non_nullable
as int,epoch: null == epoch ? _self.epoch : epoch // ignore: cast_nullable_to_non_nullable
as int,raster: freezed == raster ? _self.raster : raster // ignore: cast_nullable_to_non_nullable
as ReadingComparisonRaster?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [P5fState].
extension P5fStatePatterns on P5fState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _P5fState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _P5fState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _P5fState value)  $default,){
final _that = this;
switch (_that) {
case _P5fState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _P5fState value)?  $default,){
final _that = this;
switch (_that) {
case _P5fState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String status,  int completed,  int index,  int epoch,  ReadingComparisonRaster? raster,  String? error)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _P5fState() when $default != null:
return $default(_that.status,_that.completed,_that.index,_that.epoch,_that.raster,_that.error);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String status,  int completed,  int index,  int epoch,  ReadingComparisonRaster? raster,  String? error)  $default,) {final _that = this;
switch (_that) {
case _P5fState():
return $default(_that.status,_that.completed,_that.index,_that.epoch,_that.raster,_that.error);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String status,  int completed,  int index,  int epoch,  ReadingComparisonRaster? raster,  String? error)?  $default,) {final _that = this;
switch (_that) {
case _P5fState() when $default != null:
return $default(_that.status,_that.completed,_that.index,_that.epoch,_that.raster,_that.error);case _:
  return null;

}
}

}

/// @nodoc


class _P5fState implements P5fState {
  const _P5fState({this.status = 'initial', this.completed = 0, this.index = 0, this.epoch = 0, this.raster, this.error});
  

@override@JsonKey() final  String status;
@override@JsonKey() final  int completed;
@override@JsonKey() final  int index;
@override@JsonKey() final  int epoch;
@override final  ReadingComparisonRaster? raster;
@override final  String? error;

/// Create a copy of P5fState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$P5fStateCopyWith<_P5fState> get copyWith => __$P5fStateCopyWithImpl<_P5fState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _P5fState&&(identical(other.status, status) || other.status == status)&&(identical(other.completed, completed) || other.completed == completed)&&(identical(other.index, index) || other.index == index)&&(identical(other.epoch, epoch) || other.epoch == epoch)&&(identical(other.raster, raster) || other.raster == raster)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,status,completed,index,epoch,raster,error);

@override
String toString() {
  return 'P5fState(status: $status, completed: $completed, index: $index, epoch: $epoch, raster: $raster, error: $error)';
}


}

/// @nodoc
abstract mixin class _$P5fStateCopyWith<$Res> implements $P5fStateCopyWith<$Res> {
  factory _$P5fStateCopyWith(_P5fState value, $Res Function(_P5fState) _then) = __$P5fStateCopyWithImpl;
@override @useResult
$Res call({
 String status, int completed, int index, int epoch, ReadingComparisonRaster? raster, String? error
});




}
/// @nodoc
class __$P5fStateCopyWithImpl<$Res>
    implements _$P5fStateCopyWith<$Res> {
  __$P5fStateCopyWithImpl(this._self, this._then);

  final _P5fState _self;
  final $Res Function(_P5fState) _then;

/// Create a copy of P5fState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? completed = null,Object? index = null,Object? epoch = null,Object? raster = freezed,Object? error = freezed,}) {
  return _then(_P5fState(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,completed: null == completed ? _self.completed : completed // ignore: cast_nullable_to_non_nullable
as int,index: null == index ? _self.index : index // ignore: cast_nullable_to_non_nullable
as int,epoch: null == epoch ? _self.epoch : epoch // ignore: cast_nullable_to_non_nullable
as int,raster: freezed == raster ? _self.raster : raster // ignore: cast_nullable_to_non_nullable
as ReadingComparisonRaster?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on

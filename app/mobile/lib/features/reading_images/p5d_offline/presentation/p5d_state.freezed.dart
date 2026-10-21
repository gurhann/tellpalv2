// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'p5d_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$P5dState {

 String get status; int get epoch; int get index; bool get crop; ReadingComparisonRaster? get raster; String? get error;
/// Create a copy of P5dState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$P5dStateCopyWith<P5dState> get copyWith => _$P5dStateCopyWithImpl<P5dState>(this as P5dState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is P5dState&&(identical(other.status, status) || other.status == status)&&(identical(other.epoch, epoch) || other.epoch == epoch)&&(identical(other.index, index) || other.index == index)&&(identical(other.crop, crop) || other.crop == crop)&&(identical(other.raster, raster) || other.raster == raster)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,status,epoch,index,crop,raster,error);

@override
String toString() {
  return 'P5dState(status: $status, epoch: $epoch, index: $index, crop: $crop, raster: $raster, error: $error)';
}


}

/// @nodoc
abstract mixin class $P5dStateCopyWith<$Res>  {
  factory $P5dStateCopyWith(P5dState value, $Res Function(P5dState) _then) = _$P5dStateCopyWithImpl;
@useResult
$Res call({
 String status, int epoch, int index, bool crop, ReadingComparisonRaster? raster, String? error
});




}
/// @nodoc
class _$P5dStateCopyWithImpl<$Res>
    implements $P5dStateCopyWith<$Res> {
  _$P5dStateCopyWithImpl(this._self, this._then);

  final P5dState _self;
  final $Res Function(P5dState) _then;

/// Create a copy of P5dState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? epoch = null,Object? index = null,Object? crop = null,Object? raster = freezed,Object? error = freezed,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,epoch: null == epoch ? _self.epoch : epoch // ignore: cast_nullable_to_non_nullable
as int,index: null == index ? _self.index : index // ignore: cast_nullable_to_non_nullable
as int,crop: null == crop ? _self.crop : crop // ignore: cast_nullable_to_non_nullable
as bool,raster: freezed == raster ? _self.raster : raster // ignore: cast_nullable_to_non_nullable
as ReadingComparisonRaster?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [P5dState].
extension P5dStatePatterns on P5dState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _P5dState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _P5dState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _P5dState value)  $default,){
final _that = this;
switch (_that) {
case _P5dState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _P5dState value)?  $default,){
final _that = this;
switch (_that) {
case _P5dState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String status,  int epoch,  int index,  bool crop,  ReadingComparisonRaster? raster,  String? error)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _P5dState() when $default != null:
return $default(_that.status,_that.epoch,_that.index,_that.crop,_that.raster,_that.error);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String status,  int epoch,  int index,  bool crop,  ReadingComparisonRaster? raster,  String? error)  $default,) {final _that = this;
switch (_that) {
case _P5dState():
return $default(_that.status,_that.epoch,_that.index,_that.crop,_that.raster,_that.error);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String status,  int epoch,  int index,  bool crop,  ReadingComparisonRaster? raster,  String? error)?  $default,) {final _that = this;
switch (_that) {
case _P5dState() when $default != null:
return $default(_that.status,_that.epoch,_that.index,_that.crop,_that.raster,_that.error);case _:
  return null;

}
}

}

/// @nodoc


class _P5dState implements P5dState {
  const _P5dState({this.status = 'initial', this.epoch = 0, this.index = 0, this.crop = false, this.raster, this.error});


@override@JsonKey() final  String status;
@override@JsonKey() final  int epoch;
@override@JsonKey() final  int index;
@override@JsonKey() final  bool crop;
@override final  ReadingComparisonRaster? raster;
@override final  String? error;

/// Create a copy of P5dState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$P5dStateCopyWith<_P5dState> get copyWith => __$P5dStateCopyWithImpl<_P5dState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _P5dState&&(identical(other.status, status) || other.status == status)&&(identical(other.epoch, epoch) || other.epoch == epoch)&&(identical(other.index, index) || other.index == index)&&(identical(other.crop, crop) || other.crop == crop)&&(identical(other.raster, raster) || other.raster == raster)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,status,epoch,index,crop,raster,error);

@override
String toString() {
  return 'P5dState(status: $status, epoch: $epoch, index: $index, crop: $crop, raster: $raster, error: $error)';
}


}

/// @nodoc
abstract mixin class _$P5dStateCopyWith<$Res> implements $P5dStateCopyWith<$Res> {
  factory _$P5dStateCopyWith(_P5dState value, $Res Function(_P5dState) _then) = __$P5dStateCopyWithImpl;
@override @useResult
$Res call({
 String status, int epoch, int index, bool crop, ReadingComparisonRaster? raster, String? error
});




}
/// @nodoc
class __$P5dStateCopyWithImpl<$Res>
    implements _$P5dStateCopyWith<$Res> {
  __$P5dStateCopyWithImpl(this._self, this._then);

  final _P5dState _self;
  final $Res Function(_P5dState) _then;

/// Create a copy of P5dState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? epoch = null,Object? index = null,Object? crop = null,Object? raster = freezed,Object? error = freezed,}) {
  return _then(_P5dState(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,epoch: null == epoch ? _self.epoch : epoch // ignore: cast_nullable_to_non_nullable
as int,index: null == index ? _self.index : index // ignore: cast_nullable_to_non_nullable
as int,crop: null == crop ? _self.crop : crop // ignore: cast_nullable_to_non_nullable
as bool,raster: freezed == raster ? _self.raster : raster // ignore: cast_nullable_to_non_nullable
as ReadingComparisonRaster?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on

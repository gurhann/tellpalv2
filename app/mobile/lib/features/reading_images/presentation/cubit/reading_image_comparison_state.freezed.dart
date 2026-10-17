// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'reading_image_comparison_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ReadingImageComparisonState {

 String get status; String? get error; ReadingImageComparison? get comparison; ReadingComparisonRaster? get raster; ReadingImageGeometry? get geometry; int get entryIndex; bool get canonicalSource; int get epoch;
/// Create a copy of ReadingImageComparisonState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ReadingImageComparisonStateCopyWith<ReadingImageComparisonState> get copyWith => _$ReadingImageComparisonStateCopyWithImpl<ReadingImageComparisonState>(this as ReadingImageComparisonState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ReadingImageComparisonState&&(identical(other.status, status) || other.status == status)&&(identical(other.error, error) || other.error == error)&&(identical(other.comparison, comparison) || other.comparison == comparison)&&(identical(other.raster, raster) || other.raster == raster)&&(identical(other.geometry, geometry) || other.geometry == geometry)&&(identical(other.entryIndex, entryIndex) || other.entryIndex == entryIndex)&&(identical(other.canonicalSource, canonicalSource) || other.canonicalSource == canonicalSource)&&(identical(other.epoch, epoch) || other.epoch == epoch));
}


@override
int get hashCode => Object.hash(runtimeType,status,error,comparison,raster,geometry,entryIndex,canonicalSource,epoch);

@override
String toString() {
  return 'ReadingImageComparisonState(status: $status, error: $error, comparison: $comparison, raster: $raster, geometry: $geometry, entryIndex: $entryIndex, canonicalSource: $canonicalSource, epoch: $epoch)';
}


}

/// @nodoc
abstract mixin class $ReadingImageComparisonStateCopyWith<$Res>  {
  factory $ReadingImageComparisonStateCopyWith(ReadingImageComparisonState value, $Res Function(ReadingImageComparisonState) _then) = _$ReadingImageComparisonStateCopyWithImpl;
@useResult
$Res call({
 String status, String? error, ReadingImageComparison? comparison, ReadingComparisonRaster? raster, ReadingImageGeometry? geometry, int entryIndex, bool canonicalSource, int epoch
});




}
/// @nodoc
class _$ReadingImageComparisonStateCopyWithImpl<$Res>
    implements $ReadingImageComparisonStateCopyWith<$Res> {
  _$ReadingImageComparisonStateCopyWithImpl(this._self, this._then);

  final ReadingImageComparisonState _self;
  final $Res Function(ReadingImageComparisonState) _then;

/// Create a copy of ReadingImageComparisonState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? error = freezed,Object? comparison = freezed,Object? raster = freezed,Object? geometry = freezed,Object? entryIndex = null,Object? canonicalSource = null,Object? epoch = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,comparison: freezed == comparison ? _self.comparison : comparison // ignore: cast_nullable_to_non_nullable
as ReadingImageComparison?,raster: freezed == raster ? _self.raster : raster // ignore: cast_nullable_to_non_nullable
as ReadingComparisonRaster?,geometry: freezed == geometry ? _self.geometry : geometry // ignore: cast_nullable_to_non_nullable
as ReadingImageGeometry?,entryIndex: null == entryIndex ? _self.entryIndex : entryIndex // ignore: cast_nullable_to_non_nullable
as int,canonicalSource: null == canonicalSource ? _self.canonicalSource : canonicalSource // ignore: cast_nullable_to_non_nullable
as bool,epoch: null == epoch ? _self.epoch : epoch // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [ReadingImageComparisonState].
extension ReadingImageComparisonStatePatterns on ReadingImageComparisonState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ReadingImageComparisonState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ReadingImageComparisonState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ReadingImageComparisonState value)  $default,){
final _that = this;
switch (_that) {
case _ReadingImageComparisonState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ReadingImageComparisonState value)?  $default,){
final _that = this;
switch (_that) {
case _ReadingImageComparisonState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String status,  String? error,  ReadingImageComparison? comparison,  ReadingComparisonRaster? raster,  ReadingImageGeometry? geometry,  int entryIndex,  bool canonicalSource,  int epoch)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ReadingImageComparisonState() when $default != null:
return $default(_that.status,_that.error,_that.comparison,_that.raster,_that.geometry,_that.entryIndex,_that.canonicalSource,_that.epoch);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String status,  String? error,  ReadingImageComparison? comparison,  ReadingComparisonRaster? raster,  ReadingImageGeometry? geometry,  int entryIndex,  bool canonicalSource,  int epoch)  $default,) {final _that = this;
switch (_that) {
case _ReadingImageComparisonState():
return $default(_that.status,_that.error,_that.comparison,_that.raster,_that.geometry,_that.entryIndex,_that.canonicalSource,_that.epoch);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String status,  String? error,  ReadingImageComparison? comparison,  ReadingComparisonRaster? raster,  ReadingImageGeometry? geometry,  int entryIndex,  bool canonicalSource,  int epoch)?  $default,) {final _that = this;
switch (_that) {
case _ReadingImageComparisonState() when $default != null:
return $default(_that.status,_that.error,_that.comparison,_that.raster,_that.geometry,_that.entryIndex,_that.canonicalSource,_that.epoch);case _:
  return null;

}
}

}

/// @nodoc


class _ReadingImageComparisonState implements ReadingImageComparisonState {
  const _ReadingImageComparisonState({this.status = 'initial', this.error, this.comparison, this.raster, this.geometry, this.entryIndex = 0, this.canonicalSource = false, this.epoch = 0});
  

@override@JsonKey() final  String status;
@override final  String? error;
@override final  ReadingImageComparison? comparison;
@override final  ReadingComparisonRaster? raster;
@override final  ReadingImageGeometry? geometry;
@override@JsonKey() final  int entryIndex;
@override@JsonKey() final  bool canonicalSource;
@override@JsonKey() final  int epoch;

/// Create a copy of ReadingImageComparisonState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ReadingImageComparisonStateCopyWith<_ReadingImageComparisonState> get copyWith => __$ReadingImageComparisonStateCopyWithImpl<_ReadingImageComparisonState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ReadingImageComparisonState&&(identical(other.status, status) || other.status == status)&&(identical(other.error, error) || other.error == error)&&(identical(other.comparison, comparison) || other.comparison == comparison)&&(identical(other.raster, raster) || other.raster == raster)&&(identical(other.geometry, geometry) || other.geometry == geometry)&&(identical(other.entryIndex, entryIndex) || other.entryIndex == entryIndex)&&(identical(other.canonicalSource, canonicalSource) || other.canonicalSource == canonicalSource)&&(identical(other.epoch, epoch) || other.epoch == epoch));
}


@override
int get hashCode => Object.hash(runtimeType,status,error,comparison,raster,geometry,entryIndex,canonicalSource,epoch);

@override
String toString() {
  return 'ReadingImageComparisonState(status: $status, error: $error, comparison: $comparison, raster: $raster, geometry: $geometry, entryIndex: $entryIndex, canonicalSource: $canonicalSource, epoch: $epoch)';
}


}

/// @nodoc
abstract mixin class _$ReadingImageComparisonStateCopyWith<$Res> implements $ReadingImageComparisonStateCopyWith<$Res> {
  factory _$ReadingImageComparisonStateCopyWith(_ReadingImageComparisonState value, $Res Function(_ReadingImageComparisonState) _then) = __$ReadingImageComparisonStateCopyWithImpl;
@override @useResult
$Res call({
 String status, String? error, ReadingImageComparison? comparison, ReadingComparisonRaster? raster, ReadingImageGeometry? geometry, int entryIndex, bool canonicalSource, int epoch
});




}
/// @nodoc
class __$ReadingImageComparisonStateCopyWithImpl<$Res>
    implements _$ReadingImageComparisonStateCopyWith<$Res> {
  __$ReadingImageComparisonStateCopyWithImpl(this._self, this._then);

  final _ReadingImageComparisonState _self;
  final $Res Function(_ReadingImageComparisonState) _then;

/// Create a copy of ReadingImageComparisonState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? error = freezed,Object? comparison = freezed,Object? raster = freezed,Object? geometry = freezed,Object? entryIndex = null,Object? canonicalSource = null,Object? epoch = null,}) {
  return _then(_ReadingImageComparisonState(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,comparison: freezed == comparison ? _self.comparison : comparison // ignore: cast_nullable_to_non_nullable
as ReadingImageComparison?,raster: freezed == raster ? _self.raster : raster // ignore: cast_nullable_to_non_nullable
as ReadingComparisonRaster?,geometry: freezed == geometry ? _self.geometry : geometry // ignore: cast_nullable_to_non_nullable
as ReadingImageGeometry?,entryIndex: null == entryIndex ? _self.entryIndex : entryIndex // ignore: cast_nullable_to_non_nullable
as int,canonicalSource: null == canonicalSource ? _self.canonicalSource : canonicalSource // ignore: cast_nullable_to_non_nullable
as bool,epoch: null == epoch ? _self.epoch : epoch // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on

// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'reading_image_consumer_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ReadingImageConsumerState {

 int get attempt; ReadingImageConsumerStatus get status; AdmittedReadingImage? get admission; ReadingImageSelection? get selection; String? get immutableKey; String? get transferKey; String? get url;
/// Create a copy of ReadingImageConsumerState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ReadingImageConsumerStateCopyWith<ReadingImageConsumerState> get copyWith => _$ReadingImageConsumerStateCopyWithImpl<ReadingImageConsumerState>(this as ReadingImageConsumerState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ReadingImageConsumerState&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.status, status) || other.status == status)&&(identical(other.admission, admission) || other.admission == admission)&&(identical(other.selection, selection) || other.selection == selection)&&(identical(other.immutableKey, immutableKey) || other.immutableKey == immutableKey)&&(identical(other.transferKey, transferKey) || other.transferKey == transferKey)&&(identical(other.url, url) || other.url == url));
}


@override
int get hashCode => Object.hash(runtimeType,attempt,status,admission,selection,immutableKey,transferKey,url);

@override
String toString() {
  return 'ReadingImageConsumerState(attempt: $attempt, status: $status, admission: $admission, selection: $selection, immutableKey: $immutableKey, transferKey: $transferKey, url: $url)';
}


}

/// @nodoc
abstract mixin class $ReadingImageConsumerStateCopyWith<$Res>  {
  factory $ReadingImageConsumerStateCopyWith(ReadingImageConsumerState value, $Res Function(ReadingImageConsumerState) _then) = _$ReadingImageConsumerStateCopyWithImpl;
@useResult
$Res call({
 int attempt, ReadingImageConsumerStatus status, AdmittedReadingImage? admission, ReadingImageSelection? selection, String? immutableKey, String? transferKey, String? url
});




}
/// @nodoc
class _$ReadingImageConsumerStateCopyWithImpl<$Res>
    implements $ReadingImageConsumerStateCopyWith<$Res> {
  _$ReadingImageConsumerStateCopyWithImpl(this._self, this._then);

  final ReadingImageConsumerState _self;
  final $Res Function(ReadingImageConsumerState) _then;

/// Create a copy of ReadingImageConsumerState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? attempt = null,Object? status = null,Object? admission = freezed,Object? selection = freezed,Object? immutableKey = freezed,Object? transferKey = freezed,Object? url = freezed,}) {
  return _then(_self.copyWith(
attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ReadingImageConsumerStatus,admission: freezed == admission ? _self.admission : admission // ignore: cast_nullable_to_non_nullable
as AdmittedReadingImage?,selection: freezed == selection ? _self.selection : selection // ignore: cast_nullable_to_non_nullable
as ReadingImageSelection?,immutableKey: freezed == immutableKey ? _self.immutableKey : immutableKey // ignore: cast_nullable_to_non_nullable
as String?,transferKey: freezed == transferKey ? _self.transferKey : transferKey // ignore: cast_nullable_to_non_nullable
as String?,url: freezed == url ? _self.url : url // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [ReadingImageConsumerState].
extension ReadingImageConsumerStatePatterns on ReadingImageConsumerState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ReadingImageConsumerState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ReadingImageConsumerState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ReadingImageConsumerState value)  $default,){
final _that = this;
switch (_that) {
case _ReadingImageConsumerState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ReadingImageConsumerState value)?  $default,){
final _that = this;
switch (_that) {
case _ReadingImageConsumerState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int attempt,  ReadingImageConsumerStatus status,  AdmittedReadingImage? admission,  ReadingImageSelection? selection,  String? immutableKey,  String? transferKey,  String? url)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ReadingImageConsumerState() when $default != null:
return $default(_that.attempt,_that.status,_that.admission,_that.selection,_that.immutableKey,_that.transferKey,_that.url);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int attempt,  ReadingImageConsumerStatus status,  AdmittedReadingImage? admission,  ReadingImageSelection? selection,  String? immutableKey,  String? transferKey,  String? url)  $default,) {final _that = this;
switch (_that) {
case _ReadingImageConsumerState():
return $default(_that.attempt,_that.status,_that.admission,_that.selection,_that.immutableKey,_that.transferKey,_that.url);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int attempt,  ReadingImageConsumerStatus status,  AdmittedReadingImage? admission,  ReadingImageSelection? selection,  String? immutableKey,  String? transferKey,  String? url)?  $default,) {final _that = this;
switch (_that) {
case _ReadingImageConsumerState() when $default != null:
return $default(_that.attempt,_that.status,_that.admission,_that.selection,_that.immutableKey,_that.transferKey,_that.url);case _:
  return null;

}
}

}

/// @nodoc


class _ReadingImageConsumerState implements ReadingImageConsumerState {
  const _ReadingImageConsumerState({this.attempt = 0, this.status = ReadingImageConsumerStatus.loading, this.admission, this.selection, this.immutableKey, this.transferKey, this.url});


@override@JsonKey() final  int attempt;
@override@JsonKey() final  ReadingImageConsumerStatus status;
@override final  AdmittedReadingImage? admission;
@override final  ReadingImageSelection? selection;
@override final  String? immutableKey;
@override final  String? transferKey;
@override final  String? url;

/// Create a copy of ReadingImageConsumerState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ReadingImageConsumerStateCopyWith<_ReadingImageConsumerState> get copyWith => __$ReadingImageConsumerStateCopyWithImpl<_ReadingImageConsumerState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ReadingImageConsumerState&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.status, status) || other.status == status)&&(identical(other.admission, admission) || other.admission == admission)&&(identical(other.selection, selection) || other.selection == selection)&&(identical(other.immutableKey, immutableKey) || other.immutableKey == immutableKey)&&(identical(other.transferKey, transferKey) || other.transferKey == transferKey)&&(identical(other.url, url) || other.url == url));
}


@override
int get hashCode => Object.hash(runtimeType,attempt,status,admission,selection,immutableKey,transferKey,url);

@override
String toString() {
  return 'ReadingImageConsumerState(attempt: $attempt, status: $status, admission: $admission, selection: $selection, immutableKey: $immutableKey, transferKey: $transferKey, url: $url)';
}


}

/// @nodoc
abstract mixin class _$ReadingImageConsumerStateCopyWith<$Res> implements $ReadingImageConsumerStateCopyWith<$Res> {
  factory _$ReadingImageConsumerStateCopyWith(_ReadingImageConsumerState value, $Res Function(_ReadingImageConsumerState) _then) = __$ReadingImageConsumerStateCopyWithImpl;
@override @useResult
$Res call({
 int attempt, ReadingImageConsumerStatus status, AdmittedReadingImage? admission, ReadingImageSelection? selection, String? immutableKey, String? transferKey, String? url
});




}
/// @nodoc
class __$ReadingImageConsumerStateCopyWithImpl<$Res>
    implements _$ReadingImageConsumerStateCopyWith<$Res> {
  __$ReadingImageConsumerStateCopyWithImpl(this._self, this._then);

  final _ReadingImageConsumerState _self;
  final $Res Function(_ReadingImageConsumerState) _then;

/// Create a copy of ReadingImageConsumerState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? attempt = null,Object? status = null,Object? admission = freezed,Object? selection = freezed,Object? immutableKey = freezed,Object? transferKey = freezed,Object? url = freezed,}) {
  return _then(_ReadingImageConsumerState(
attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ReadingImageConsumerStatus,admission: freezed == admission ? _self.admission : admission // ignore: cast_nullable_to_non_nullable
as AdmittedReadingImage?,selection: freezed == selection ? _self.selection : selection // ignore: cast_nullable_to_non_nullable
as ReadingImageSelection?,immutableKey: freezed == immutableKey ? _self.immutableKey : immutableKey // ignore: cast_nullable_to_non_nullable
as String?,transferKey: freezed == transferKey ? _self.transferKey : transferKey // ignore: cast_nullable_to_non_nullable
as String?,url: freezed == url ? _self.url : url // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on

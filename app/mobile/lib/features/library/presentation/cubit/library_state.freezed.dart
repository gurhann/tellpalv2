// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'library_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$LibraryState {

 LibraryContext get context; int get attempt; LibraryStatus get status; LibraryCatalog? get catalog; Failure? get failure;
/// Create a copy of LibraryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$LibraryStateCopyWith<LibraryState> get copyWith => _$LibraryStateCopyWithImpl<LibraryState>(this as LibraryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is LibraryState&&(identical(other.context, context) || other.context == context)&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.status, status) || other.status == status)&&(identical(other.catalog, catalog) || other.catalog == catalog)&&(identical(other.failure, failure) || other.failure == failure));
}


@override
int get hashCode => Object.hash(runtimeType,context,attempt,status,catalog,failure);

@override
String toString() {
  return 'LibraryState(context: $context, attempt: $attempt, status: $status, catalog: $catalog, failure: $failure)';
}


}

/// @nodoc
abstract mixin class $LibraryStateCopyWith<$Res>  {
  factory $LibraryStateCopyWith(LibraryState value, $Res Function(LibraryState) _then) = _$LibraryStateCopyWithImpl;
@useResult
$Res call({
 LibraryContext context, int attempt, LibraryStatus status, LibraryCatalog? catalog, Failure? failure
});




}
/// @nodoc
class _$LibraryStateCopyWithImpl<$Res>
    implements $LibraryStateCopyWith<$Res> {
  _$LibraryStateCopyWithImpl(this._self, this._then);

  final LibraryState _self;
  final $Res Function(LibraryState) _then;

/// Create a copy of LibraryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? context = null,Object? attempt = null,Object? status = null,Object? catalog = freezed,Object? failure = freezed,}) {
  return _then(_self.copyWith(
context: null == context ? _self.context : context // ignore: cast_nullable_to_non_nullable
as LibraryContext,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as LibraryStatus,catalog: freezed == catalog ? _self.catalog : catalog // ignore: cast_nullable_to_non_nullable
as LibraryCatalog?,failure: freezed == failure ? _self.failure : failure // ignore: cast_nullable_to_non_nullable
as Failure?,
  ));
}

}


/// Adds pattern-matching-related methods to [LibraryState].
extension LibraryStatePatterns on LibraryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _LibraryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _LibraryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _LibraryState value)  $default,){
final _that = this;
switch (_that) {
case _LibraryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _LibraryState value)?  $default,){
final _that = this;
switch (_that) {
case _LibraryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LibraryContext context,  int attempt,  LibraryStatus status,  LibraryCatalog? catalog,  Failure? failure)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _LibraryState() when $default != null:
return $default(_that.context,_that.attempt,_that.status,_that.catalog,_that.failure);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LibraryContext context,  int attempt,  LibraryStatus status,  LibraryCatalog? catalog,  Failure? failure)  $default,) {final _that = this;
switch (_that) {
case _LibraryState():
return $default(_that.context,_that.attempt,_that.status,_that.catalog,_that.failure);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LibraryContext context,  int attempt,  LibraryStatus status,  LibraryCatalog? catalog,  Failure? failure)?  $default,) {final _that = this;
switch (_that) {
case _LibraryState() when $default != null:
return $default(_that.context,_that.attempt,_that.status,_that.catalog,_that.failure);case _:
  return null;

}
}

}

/// @nodoc


class _LibraryState implements LibraryState {
  const _LibraryState({required this.context, this.attempt = 0, this.status = LibraryStatus.loading, this.catalog, this.failure});
  

@override final  LibraryContext context;
@override@JsonKey() final  int attempt;
@override@JsonKey() final  LibraryStatus status;
@override final  LibraryCatalog? catalog;
@override final  Failure? failure;

/// Create a copy of LibraryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$LibraryStateCopyWith<_LibraryState> get copyWith => __$LibraryStateCopyWithImpl<_LibraryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _LibraryState&&(identical(other.context, context) || other.context == context)&&(identical(other.attempt, attempt) || other.attempt == attempt)&&(identical(other.status, status) || other.status == status)&&(identical(other.catalog, catalog) || other.catalog == catalog)&&(identical(other.failure, failure) || other.failure == failure));
}


@override
int get hashCode => Object.hash(runtimeType,context,attempt,status,catalog,failure);

@override
String toString() {
  return 'LibraryState(context: $context, attempt: $attempt, status: $status, catalog: $catalog, failure: $failure)';
}


}

/// @nodoc
abstract mixin class _$LibraryStateCopyWith<$Res> implements $LibraryStateCopyWith<$Res> {
  factory _$LibraryStateCopyWith(_LibraryState value, $Res Function(_LibraryState) _then) = __$LibraryStateCopyWithImpl;
@override @useResult
$Res call({
 LibraryContext context, int attempt, LibraryStatus status, LibraryCatalog? catalog, Failure? failure
});




}
/// @nodoc
class __$LibraryStateCopyWithImpl<$Res>
    implements _$LibraryStateCopyWith<$Res> {
  __$LibraryStateCopyWithImpl(this._self, this._then);

  final _LibraryState _self;
  final $Res Function(_LibraryState) _then;

/// Create a copy of LibraryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? context = null,Object? attempt = null,Object? status = null,Object? catalog = freezed,Object? failure = freezed,}) {
  return _then(_LibraryState(
context: null == context ? _self.context : context // ignore: cast_nullable_to_non_nullable
as LibraryContext,attempt: null == attempt ? _self.attempt : attempt // ignore: cast_nullable_to_non_nullable
as int,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as LibraryStatus,catalog: freezed == catalog ? _self.catalog : catalog // ignore: cast_nullable_to_non_nullable
as LibraryCatalog?,failure: freezed == failure ? _self.failure : failure // ignore: cast_nullable_to_non_nullable
as Failure?,
  ));
}


}

// dart format on

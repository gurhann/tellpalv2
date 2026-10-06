// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'probe.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ProbeState {





@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ProbeState);
}


@override
int get hashCode => runtimeType.hashCode;

@override
String toString() {
  return 'ProbeState()';
}


}

/// @nodoc
class $ProbeStateCopyWith<$Res>  {
$ProbeStateCopyWith(ProbeState _, $Res Function(ProbeState) __);
}


/// Adds pattern-matching-related methods to [ProbeState].
extension ProbeStatePatterns on ProbeState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( ProbeInitial value)?  initial,TResult Function( ProbeLoaded value)?  loaded,required TResult orElse(),}){
final _that = this;
switch (_that) {
case ProbeInitial() when initial != null:
return initial(_that);case ProbeLoaded() when loaded != null:
return loaded(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( ProbeInitial value)  initial,required TResult Function( ProbeLoaded value)  loaded,}){
final _that = this;
switch (_that) {
case ProbeInitial():
return initial(_that);case ProbeLoaded():
return loaded(_that);}
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( ProbeInitial value)?  initial,TResult? Function( ProbeLoaded value)?  loaded,}){
final _that = this;
switch (_that) {
case ProbeInitial() when initial != null:
return initial(_that);case ProbeLoaded() when loaded != null:
return loaded(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function()?  initial,TResult Function( ProbeEntity item)?  loaded,required TResult orElse(),}) {final _that = this;
switch (_that) {
case ProbeInitial() when initial != null:
return initial();case ProbeLoaded() when loaded != null:
return loaded(_that.item);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function()  initial,required TResult Function( ProbeEntity item)  loaded,}) {final _that = this;
switch (_that) {
case ProbeInitial():
return initial();case ProbeLoaded():
return loaded(_that.item);}
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function()?  initial,TResult? Function( ProbeEntity item)?  loaded,}) {final _that = this;
switch (_that) {
case ProbeInitial() when initial != null:
return initial();case ProbeLoaded() when loaded != null:
return loaded(_that.item);case _:
  return null;

}
}

}

/// @nodoc


class ProbeInitial implements ProbeState {
  const ProbeInitial();
  






@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ProbeInitial);
}


@override
int get hashCode => runtimeType.hashCode;

@override
String toString() {
  return 'ProbeState.initial()';
}


}




/// @nodoc


class ProbeLoaded implements ProbeState {
  const ProbeLoaded(this.item);
  

 final  ProbeEntity item;

/// Create a copy of ProbeState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ProbeLoadedCopyWith<ProbeLoaded> get copyWith => _$ProbeLoadedCopyWithImpl<ProbeLoaded>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ProbeLoaded&&(identical(other.item, item) || other.item == item));
}


@override
int get hashCode => Object.hash(runtimeType,item);

@override
String toString() {
  return 'ProbeState.loaded(item: $item)';
}


}

/// @nodoc
abstract mixin class $ProbeLoadedCopyWith<$Res> implements $ProbeStateCopyWith<$Res> {
  factory $ProbeLoadedCopyWith(ProbeLoaded value, $Res Function(ProbeLoaded) _then) = _$ProbeLoadedCopyWithImpl;
@useResult
$Res call({
 ProbeEntity item
});




}
/// @nodoc
class _$ProbeLoadedCopyWithImpl<$Res>
    implements $ProbeLoadedCopyWith<$Res> {
  _$ProbeLoadedCopyWithImpl(this._self, this._then);

  final ProbeLoaded _self;
  final $Res Function(ProbeLoaded) _then;

/// Create a copy of ProbeState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') $Res call({Object? item = null,}) {
  return _then(ProbeLoaded(
null == item ? _self.item : item // ignore: cast_nullable_to_non_nullable
as ProbeEntity,
  ));
}


}

// dart format on

// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'geofence_definition.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

GeofenceDefinition _$GeofenceDefinitionFromJson(Map<String, dynamic> json) {
  return GeofenceDefinition(
    id: json['id'] as String,
    lat: (json['lat'] as num)?.toDouble(),
    lng: (json['lng'] as num)?.toDouble(),
    radius: (json['radius'] as num)?.toDouble(),
    httpMethod: json['httpMethod'] as String,
    url: json['url'] as String,
  );
}

Map<String, dynamic> _$GeofenceDefinitionToJson(GeofenceDefinition instance) =>
    <String, dynamic>{
      'id': instance.id,
      'lat': instance.lat,
      'lng': instance.lng,
      'radius': instance.radius,
      'httpMethod': instance.httpMethod,
      'url': instance.url,
    };

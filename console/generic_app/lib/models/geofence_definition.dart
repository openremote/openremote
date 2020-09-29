

import 'package:generic_app/models/base_model.dart';
import 'package:json_annotation/json_annotation.dart';

part 'geofence_definition.g.dart';

@JsonSerializable()
class GeofenceDefinition implements BaseModel{
  final String id;
  final double lat;
  final double lng;
  final double radius;
  final String httpMethod;
  final String url;

  GeofenceDefinition({this.id, this.lat, this.lng, this.radius, this.httpMethod, this.url});

  static GeofenceDefinition fromJson(Map<String, dynamic> json) => _$GeofenceDefinitionFromJson(json);
  @override
  Map<String, dynamic> toJson() => _$GeofenceDefinitionToJson(this);

  @override
  String toString() =>
      "GeofenceDefinition(id='$id', lat=$lat, lng=$lng, radius=$radius, httpMethod='$httpMethod', url='$url')";
}

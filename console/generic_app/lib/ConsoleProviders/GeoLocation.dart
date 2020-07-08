
import 'package:generic_app/models/GeofenceDefinition.dart';

class GeoLocation {
  final GeofenceDefinition geofenceDefinition;
  final Map<String, dynamic> data;

  GeoLocation(this.geofenceDefinition, {this.data});
}
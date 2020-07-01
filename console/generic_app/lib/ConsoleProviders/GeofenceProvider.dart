import 'dart:collection';
import 'dart:convert';
import 'dart:html';

import 'package:flutter/material.dart';
import 'package:flutter_geofence/Geolocation.dart';
import 'package:flutter_geofence/geofence.dart';
import 'package:generic_app/ConsoleProviders/GeoLocation.dart';
import 'package:generic_app/models/GeofenceDefinition.dart';
import 'package:generic_app/network/ApiManager.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

typedef GeofenceCallback = Function(Map<String, dynamic>);

class GeofenceProvider {
  static const String baseUrlKey = "baseUrl";
  static const String consoleIdKey = "consoleId";
  static const String geofencesKey = "geofences";
  static const String geoDisabledKey = "geoDisabled";

  final String _version = "ORConsole";
  final String _geofenceFetchEndpoint = "rules/geofences/";
  final SharedPreferences _sharedPreferences;
  final ApiManager _apiManager;

  GeofenceCallback _enableCallback;
  GeofenceCallback _getLocationCallback;

  String _baseURL;
  String _consoleId;
  List<GeofenceDefinition> geofences;

  GeoLocation _enteredLocation;
  GeoLocation _exitedLocation;
  bool _sendQueued;

  GeofenceProvider._internal(this._baseURL, this._consoleId, this.geofences,
      this._sharedPreferences, this._apiManager);

  factory GeofenceProvider(SharedPreferences sharedPreferences) {
    String geofenceString = sharedPreferences.getString(geofencesKey);
    List<GeofenceDefinition> geofences;
    if (geofenceString != null) {
      geofences = List<GeofenceDefinition>.from(json.decode(geofenceString));
    } else {
      geofences = new List<GeofenceDefinition>();
    }

    return GeofenceProvider._internal(
        sharedPreferences.getString(baseUrlKey),
        sharedPreferences.getString(consoleIdKey),
        geofences,
        sharedPreferences,
        ApiManager(sharedPreferences.getString(baseUrlKey)));
  }

  static Future<GeofenceProvider> getInstance() async {
    return GeofenceProvider(await SharedPreferences.getInstance());
  }

  initialize() async {
    return {
      "action": "PROVIDER_INIT",
      "provider": "geofence",
      "version": _version,
      "requiresPermission": true,
      "hasPermission": await Permission.locationAlways.isGranted,
      "success": true,
      "enabled": false,
      "disabled": _sharedPreferences.containsKey(geoDisabledKey)
    };
  }

  enable(String baseUrl, String consoleId, GeofenceCallback callback) async {
    this._baseURL = baseUrl;
    this._consoleId = consoleId;
    this._apiManager.baseUrl = baseUrl;
    await this._sharedPreferences.setString(baseUrlKey, baseUrl);
    await this._sharedPreferences.setString(consoleIdKey, consoleId);
    await this._sharedPreferences.setBool(geoDisabledKey, false);
    await this._sharedPreferences.setString(baseUrlKey, baseUrl);
    this._enableCallback = callback;

    if (await Permission.locationAlways.isGranted) {
      Geofence.initialize();
      Geofence.startListening(GeolocationEvent.entry, (geolocation) {
        _queueSendLocation(geofences.firstWhere((element) => element.id == geolocation.id), {"type": "Point", "coordinates": [geolocation.longitude, geolocation.latitude]});
      });
      Geofence.startListening(GeolocationEvent.exit, (geolocation) {
        _queueSendLocation(geofences.firstWhere((element) => element.id == geolocation.id), null);
      });
      _enableCallback?.call({
        "action": "PROVIDER_ENABLE",
        "provider": "geofence",
        "hasPermission": await Permission.locationAlways.isGranted,
        "success": true
      });
    } else {
      if (await Permission.locationAlways.isUndetermined) {
        Permission.locationAlways.request().then((value) {
          _enableCallback?.call({
            "action": "PROVIDER_ENABLE",
            "provider": "geofence",
            "hasPermission": value.isGranted,
            "success": true
          });
          if (value.isGranted) {
            Geofence.initialize();
          }
        });
      } else {
        _enableCallback?.call({
          "action": "PROVIDER_ENABLE",
          "provider": "geofence",
          "hasPermission": false,
          "success": true
        });
      }
    }
  }

  disable() async {
    for (String geofenceId in geofences.map((e) => e.id)) {
      Geofence.removeGeolocation(
          new Geolocation(id: geofenceId), GeolocationEvent.exit);
      Geofence.removeGeolocation(
          new Geolocation(id: geofenceId), GeolocationEvent.entry);
    }

    await _sharedPreferences.setBool(geoDisabledKey, true);
    await _sharedPreferences.remove(baseUrlKey);
    await _sharedPreferences.remove(consoleIdKey);
    return {
      "action": "PROVIDER_DISABLE",
      "provider": "geofence",
    };
  }

  addGeofence(GeofenceDefinition geofenceDefinition) {
    Geolocation geolocation = new Geolocation(
        id: geofenceDefinition.id,
        latitude: geofenceDefinition.lat,
        longitude: geofenceDefinition.lng,
        radius: geofenceDefinition.radius);
    Geofence.addGeolocation(geolocation, GeolocationEvent.entry);
    Geofence.addGeolocation(geolocation, GeolocationEvent.exit);
  }

  removeGeofence(String id) {
    Geofence.removeGeolocation(new Geolocation(id: id), GeolocationEvent.exit);
    Geofence.removeGeolocation(new Geolocation(id: id), GeolocationEvent.entry);
  }

  refreshGeofences() {
    _apiManager.getAll<GeofenceDefinition>(["rules", "geofences", _consoleId],
        GeofenceDefinition.fromJson).then((geofenceDefinitions) {
      print("Fetched geofences=${geofenceDefinitions.length}");

      List<GeofenceDefinition> remainingGeofences =
          new List<GeofenceDefinition>();
      geofences.forEach((oldGeofence) {
        if (!geofenceDefinitions
            .any((definition) => definition.id == oldGeofence.id)) {
          removeGeofence(oldGeofence.id);
        } else {
          remainingGeofences.add(oldGeofence);
        }
      });

      geofenceDefinitions.forEach((definition) {
        if (!geofences.any((element) => element.id == definition.id)) {
          addGeofence(definition);
        }
      });

      _sharedPreferences
          .setString(geofencesKey,
              json.encode(geofenceDefinitions.map((e) => e.toJson())))
          .then((value) => geofences = geofenceDefinitions);
    });
  }

  _queueSendLocation(GeofenceDefinition geofenceDefinition,
      Map<String, dynamic> locationData) {
    if (locationData == null) {
      _exitedLocation = GeoLocation(geofenceDefinition);

      // If exit is for same geofence as queued enter then remove enter to avoid incorrectly setting location
      if (_enteredLocation?.geofenceDefinition?.id == geofenceDefinition.id) {
        _exitedLocation = null;
      }
    } else {
      _enteredLocation = GeoLocation(geofenceDefinition, data: locationData);
    }

    if (!_sendQueued) {
      _sendQueued = true;
      print("Schedule send location");
      Future.delayed(Duration(seconds: 2)).then((value) {
        _doSendLocation();
      });
    }
  }

  _doSendLocation() {
    print("Do send location");
    bool success = false;

    if (_exitedLocation != null) {
      if (_sendLocation(
          _exitedLocation.geofenceDefinition, _exitedLocation.data)) {
        _exitedLocation = null;
        success = true;
      }
    } else if (_enteredLocation != null) {
      if (_sendLocation(
          _enteredLocation.geofenceDefinition, _enteredLocation.data)) {
        _enteredLocation = null;
        success = true;
      }
    }

    if (_exitedLocation != null || _enteredLocation != null) {
      if (!success) {
        print("Send failed so re-scheduling");
      } else {
        print("More locations to send so scheduling another run");
      }
      // Schedule another send
      Future.delayed(Duration(seconds: success ? 5 : 10)).then((value) {
        _doSendLocation();
      });
    } else {
      _sendQueued = false;
    }
  }

  _sendLocation(GeofenceDefinition geofenceDefinition,
      Map<String, dynamic> locationData) {
    return _apiManager
        .post(overrideUrl: geofenceDefinition.url, rawModel: locationData != null ? jsonEncode(locationData) : null.toString())
        .then((value) => true)
        .catchError((error) => false);
  }

  getLocation(GeofenceCallback callback, BuildContext context) async {
    _getLocationCallback = callback;

    if (await Permission.locationAlways.isGranted) {
      Coordinate coordinate = await Geofence.getCurrentLocation();
      _getLocationCallback({
        "action": "GET_LOCATION",
        "provider": "geofence",
        "data": {
          "latitude": coordinate.latitude,
          "longitude": coordinate.longitude
        }
      });
    } else {
      if (await Permission.locationAlways.isUndetermined) {
        Permission.locationAlways.request().then((value) {
          if (value.isGranted) {
            Geofence.getCurrentLocation().then((coordinate) {
              _getLocationCallback({
                "action": "GET_LOCATION",
                "provider": "geofence",
                "data": {
                  "latitude": coordinate.latitude,
                  "longitude": coordinate.longitude
                }
              });
            });
          }
        });
      } else {
        if (await Permission.locationAlways.isDenied) {
          showDialog(
              context: context,
              builder: (BuildContext context) {
                return AlertDialog(
                  title: new Text("Location permission denied"),
                  content: new Text(
                      "In order to get the location it's necessary to give permissions to the app. Do you want to open the settings?"),
                  actions: <Widget>[
                    new FlatButton(
                      child: new Text("Yes"),
                      onPressed: () {
                        openAppSettings();
                      },
                    ),
                    new FlatButton(
                      child: new Text("No"),
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                    ),
                  ],
                );
              });
        }
      }
    }
  }
}

import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:generic_app/ConsoleProviders/geo_location.dart';
import 'package:generic_app/models/geofence_definition.dart';
import 'package:generic_app/network/api_manager.dart';
import 'package:geofencing/geofencing.dart';
import 'package:location/location.dart' as lm;
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'provider_constants.dart';

class GeofenceProvider {
  static GeofenceProvider _instance;

  static const String baseUrlKey = "baseUrl";
  static const String consoleIdKey = "consoleId";
  static const String geofencesKey = "geofences";
  static const String geoDisabledKey = "geoDisabled";

  final String _version = "ORConsole";
  final String _geofenceFetchEndpoint = "rules/geofences/";
  final SharedPreferences _sharedPreferences;
  final ApiManager _apiManager;

  ProviderCallback _enableCallback;
  ProviderCallback _getLocationCallback;

  String _baseURL;
  String _consoleId;
  List<GeofenceDefinition> geofences;

  GeoLocation _enteredLocation;
  GeoLocation _exitedLocation;
  bool _sendQueued = false;
  final List<GeofenceEvent> triggers = <GeofenceEvent>[
    GeofenceEvent.enter,
    GeofenceEvent.exit
  ];

  static Future geofenceTriggerCallback(
      List<String> ids, Location location, GeofenceEvent event) async {
    final GeofenceProvider instance = await GeofenceProvider.getInstance();
    if (event == GeofenceEvent.enter) {
      ids.forEach((id) {
        final GeofenceDefinition geofenceDefinition =
            instance.geofences.firstWhere((element) => element.id == id, orElse: () => null);
        if (geofenceDefinition != null) {
          instance._queueSendLocation(geofenceDefinition, {
            "type": "Point",
            "coordinates": [location.longitude, location.latitude]
          });
        }
      });
    }
    if (event == GeofenceEvent.exit) {
      ids.forEach((id) {
        final GeofenceDefinition geofenceDefinition =
            instance.geofences.firstWhere((element) => element.id == id, orElse: () => null);
        if (geofenceDefinition != null) {
          instance._queueSendLocation(geofenceDefinition, null);
        }
      });
    }
  }

  lm.Location _locationManager;

  GeofenceProvider._internal(this._baseURL, this._consoleId, this.geofences,
      this._sharedPreferences, this._apiManager, this._locationManager);

  factory GeofenceProvider(SharedPreferences sharedPreferences) {
    final String geofenceString = sharedPreferences.getString(geofencesKey);
    List<GeofenceDefinition> geofences;
    if (geofenceString != null) {
      geofences = List<GeofenceDefinition>.from(json
          .decode(geofenceString)
          .map((item) => GeofenceDefinition.fromJson(item)));
    } else {
      geofences = <GeofenceDefinition>[];
    }

    return GeofenceProvider._internal(
        sharedPreferences.getString(baseUrlKey),
        sharedPreferences.getString(consoleIdKey),
        geofences,
        sharedPreferences,
        ApiManager(sharedPreferences.getString(baseUrlKey), baseHeaders: {"Content-Type": "application/json", "Accept": "application/json"}),
        lm.Location());
  }

  static Future<GeofenceProvider> getInstance() async {
    return _instance ??= GeofenceProvider(await SharedPreferences.getInstance());
  }

  Future<Map<String, dynamic>> initialize() async {
    return {
      "action": "PROVIDER_INIT",
      "provider": "geofence",
      "version": _version,
      "requiresPermission": true,
      "hasPermission": await Permission.locationAlways.isGranted,
      "success": true,
      "enabled": false,
      "disabled": _sharedPreferences.containsKey(geoDisabledKey) &&
          _sharedPreferences.getBool(geoDisabledKey)
    };
  }

  Future enable(String baseUrl, String consoleId, ProviderCallback callback) async {
    _baseURL = baseUrl;
    _consoleId = consoleId;
    _apiManager.baseUrl = baseUrl;
    await _sharedPreferences.setString(baseUrlKey, baseUrl);
    await _sharedPreferences.setString(consoleIdKey, consoleId);
    await _sharedPreferences.setBool(geoDisabledKey, false);
    await _sharedPreferences.setString(baseUrlKey, baseUrl);
    _enableCallback = callback;

    if (await Permission.locationAlways.isGranted) {
      _startGeofenceProvider();
      _enableCallback?.call({
        "action": "PROVIDER_ENABLE",
        "provider": "geofence",
        "hasPermission": await Permission.locationAlways.isGranted,
        "success": true
      });
      Future.delayed(const Duration(seconds: 5)).then((value) => refreshGeofences());
    } else {
      if (await Permission.locationAlways.isUndetermined) {
        Permission.locationAlways.request().then((value) {
          if (value.isGranted) {
            _startGeofenceProvider();
            Future.delayed(const Duration(seconds: 5))
                .then((value) => refreshGeofences());
          }
          _enableCallback?.call({
            "action": "PROVIDER_ENABLE",
            "provider": "geofence",
            "hasPermission": value.isGranted,
            "success": true
          });
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

  Future<Map<String, dynamic>> disable() async {
    for (final String geofenceId in geofences.map((e) => e.id)) {
      await GeofencingManager.removeGeofenceById(geofenceId);
    }

    await _sharedPreferences.setBool(geoDisabledKey, true);
    await _sharedPreferences.remove(baseUrlKey);
    return {
      "action": "PROVIDER_DISABLE",
      "provider": "geofence",
    };
  }

  Future addGeofence(GeofenceDefinition geofenceDefinition) async {
    await GeofencingManager.registerGeofence(
        GeofenceRegion(
            geofenceDefinition.id,
            geofenceDefinition.lat,
            geofenceDefinition.lng,
            geofenceDefinition.radius,
            triggers), geofenceTriggerCallback);
  }

  Future _startGeofenceProvider() async {
    await GeofencingManager.initialize();
    geofences.forEach((geofence) async {
      await addGeofence(geofence);
    });
  }

  Future removeGeofence(String id) async {
    await GeofencingManager.removeGeofenceById(id);
  }

  Future refreshGeofences() async {
    _apiManager.getAll<GeofenceDefinition>(["rules", "geofences", _consoleId],
        GeofenceDefinition.fromJson).then((geofenceDefinitions) async {
      print("Fetched geofences=${geofenceDefinitions.length}");

      final List<GeofenceDefinition> remainingGeofences = List.from(geofenceDefinitions);
      geofences.forEach((oldGeofence) async {
        if (!geofenceDefinitions
            .any((definition) => definition.id == oldGeofence.id)) {
          await removeGeofence(oldGeofence.id);
        } else {
          remainingGeofences.removeWhere((element) => element.id == oldGeofence.id);
        }
      });

      remainingGeofences.forEach((definition) async {
        if (!geofences.any((element) => element.id == definition.id)) {
          await addGeofence(definition);
        }
      });

      await _sharedPreferences
          .setString(geofencesKey,
          json.encode(geofenceDefinitions.map((e) => e.toJson()).toList()))
          .then((value) => geofences = geofenceDefinitions);
    });
  }

  void _queueSendLocation(GeofenceDefinition geofenceDefinition,
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
      Future.delayed(const Duration(seconds: 2)).then((value) {
        _doSendLocation();
      });
    }
  }

  Future _doSendLocation() async {
    print("Do send location");
    bool success = false;

    if (_exitedLocation != null) {
      if (await _sendLocation(
          _exitedLocation.geofenceDefinition, _exitedLocation.data)) {
        _exitedLocation = null;
        success = true;
      }
    } else if (_enteredLocation != null) {
      if (await _sendLocation(
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

  Future<bool> _sendLocation(GeofenceDefinition geofenceDefinition,
      Map<String, dynamic> locationData) {
    return _apiManager
        .put(
        overrideUrl:
        "${_baseURL}${geofenceDefinition.url}",
        rawModel: locationData != null
            ? jsonEncode(locationData)
            : null.toString())
        .then((value) => true)
        .catchError((error) {
      print(error);
      return false;
    });
  }

  Future getLocation(ProviderCallback callback, BuildContext context) async {
    _getLocationCallback = callback;

    if (await Permission.locationAlways.isGranted) {
      lm.LocationData locationData = await _locationManager.getLocation();
      _getLocationCallback({
        "action": "GET_LOCATION",
        "provider": "geofence",
        "data": {
          "latitude": locationData.latitude,
          "longitude": locationData.longitude
        }
      });
    } else {
      if (await Permission.locationAlways.isUndetermined) {
        Permission.locationAlways.request().then((value) {
          if (value.isGranted) {
            _locationManager.getLocation().then((locationData) {
              _getLocationCallback({
                "action": "GET_LOCATION",
                "provider": "geofence",
                "data": {
                  "latitude": locationData.latitude,
                  "longitude": locationData.longitude
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
                  title: const Text("Location permission denied"),
                  content: const Text(
                      "In order to get the location it's necessary to give permissions to the app. Do you want to open the settings?"),
                  actions: <Widget>[
                    FlatButton(
                      onPressed: () {
                        openAppSettings();
                      },
                      child: const Text("Yes"),
                    ),
                    FlatButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                      child: const Text("No"),
                    ),
                  ],
                );
              });
        }
      }
    }
  }
}

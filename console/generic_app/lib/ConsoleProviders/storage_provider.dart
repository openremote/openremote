import 'dart:core';

import 'package:shared_preferences/shared_preferences.dart';

class StorageProvider {
  final String _version = "1.0.0";
  final SharedPreferences _sharedPreferences;

  StorageProvider._internal(this._sharedPreferences);

  static Future<StorageProvider> getInstance() async {
    return StorageProvider._internal(await SharedPreferences.getInstance());
  }

  Map<String, dynamic> initialize() {
    return {
      "action": "PROVIDER_INIT",
      "provider": "storage",
      "version": _version,
      "requiresPermission": false,
      "hasPermission": true,
      "success": true,
      "enabled": true
    };
  }

  Map<String, dynamic> enable() {
    return {
      "action": "PROVIDER_ENABLE",
      "provider": "storage",
      "hasPermission": true,
      "success": true
    };
  }

  Map<String, dynamic> disable() {
    return {
      "action": "PROVIDER_DISABLE",
      "provider": "storage",
    };
  }

  void store(String key, String data) {
    if (data == null) {
      _sharedPreferences.remove(key);
    } else {
      _sharedPreferences.setString(key, data);
    }
  }

  Map<String, dynamic> retrieve(String key) {
    return {
      "action": "RETRIEVE",
      "provider": "storage",
      "key": key,
      "value": _sharedPreferences.getString(key)
    };
  }
}

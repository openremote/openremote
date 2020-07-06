import 'dart:core';

import 'package:shared_preferences/shared_preferences.dart';

class StorageProvider {
  final String _version = "1.0.0";
  final SharedPreferences _sharedPreferences;

  StorageProvider._internal(this._sharedPreferences);

  static Future<StorageProvider> getInstance() async {
    return StorageProvider._internal(await SharedPreferences.getInstance());
  }

  initialize() {
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

  enable() {
    return {
      "action": "PROVIDER_ENABLE",
      "provider": "storage",
      "hasPermission": true,
      "success": true
    };
  }

  disable() {
    return {
      "action": "PROVIDER_DISABLE",
      "provider": "storage",
    };
  }

  store(String key, String data) {
    if (data == null) {
      _sharedPreferences.remove(key);
    } else {
      _sharedPreferences.setString(key, data);
    }
  }

  retrieve(String key) {
    return {
      "action": "RETRIEVE",
      "provider": "storage",
      "key": key,
      "value": _sharedPreferences.getString(key)
    };
  }
}

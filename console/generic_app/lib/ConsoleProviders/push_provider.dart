import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'provider_constants.dart';

class PushProvider {
  static const String consoleIdKey = "consoleId";
  static const String pushDisabledKey = "pushDisabled";

  final String _version = "fcm";
  final SharedPreferences _sharedPreferences;
  final FirebaseMessaging _fcm;

  String _consoleId;

  PushProvider._internal(this._sharedPreferences, this._fcm);

  static Future<PushProvider> getInstance() async {
    return PushProvider._internal(
        await SharedPreferences.getInstance(), FirebaseMessaging());
  }

  Future<Map<String, dynamic>> initialize() async {
    return {
      "action": "PROVIDER_INIT",
      "provider": "push",
      "version": _version,
      "requiresPermission": true,
      "hasPermission": await Permission.notification.isGranted,
      "success": true,
      "enabled": false,
      "disabled": _sharedPreferences.containsKey(pushDisabledKey) &&
          _sharedPreferences.getBool(pushDisabledKey)
    };
  }

  Future enable(String consoleId, ProviderCallback callback) async {
    _consoleId = consoleId;
    await _sharedPreferences.setString(consoleIdKey, consoleId);
    await _sharedPreferences.setBool(pushDisabledKey, false);

    if (await Permission.notification.isGranted) {
      _fcm.getToken().then((token) {
        callback?.call({
          "action": "PROVIDER_ENABLE",
          "provider": "push",
          "hasPermission": true,
          "success": true,
          "data": {"token": token}
        });
      });
    } else {
      if (await Permission.notification.isUndetermined) {
        Permission.notification.request().then((value) {
          if (value.isGranted) {
            _fcm.getToken().then((token) {
              callback?.call({
                "action": "PROVIDER_ENABLE",
                "provider": "push",
                "hasPermission": value.isGranted,
                "success": true,
                "data": {"token": token}
              });
            });
          } else {
            callback?.call({
              "action": "PROVIDER_ENABLE",
              "provider": "push",
              "hasPermission": value.isGranted,
              "success": true
            });
          }
        });
      } else {
        callback?.call({
          "action": "PROVIDER_ENABLE",
          "provider": "push",
          "hasPermission": false,
          "success": true
        });
      }
    }
  }

  Future<Map<String, dynamic>> disable() async {
    await _sharedPreferences.setBool(pushDisabledKey, true);
    return {
      "action": "PROVIDER_DISABLE",
      "provider": "push",
    };
  }
}

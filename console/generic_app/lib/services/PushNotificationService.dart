import 'dart:io';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:generic_app/ConsoleProviders/GeofenceProvider.dart';
import 'package:generic_app/ConsoleProviders/StorageProvider.dart';
import 'package:generic_app/models/AlertAction.dart';
import 'package:generic_app/models/AlertButton.dart';

Future<dynamic> myBackgroundMessageHandler(Map<String, dynamic> remoteMessage) async {
  await handleNotification(remoteMessage, false, false);
}

Future handleNotification(Map<String, dynamic> remoteMessage, bool onResume, bool onLaunch) async {
  Map<String, dynamic> messageData = remoteMessage["data"] ??
      remoteMessage;
  if (messageData != null) {
    bool isSilent = !messageData.containsKey("or-title");

    if (isSilent) {
      String action = messageData["action"];

      switch (action) {
        case "GEOFENCE_REFRESH":
          GeofenceProvider geofenceProvider = await GeofenceProvider
              .getInstance();
          geofenceProvider.refreshGeofences();
          break;
        case "STORE":
          StorageProvider storageProvider = await StorageProvider
              .getInstance();
          storageProvider.store(messageData["key"], messageData["value"]);
          break;
      }
    } else {
      String title = messageData["or-title"];
      String body = messageData["or-body"];

      List<AlertButton> buttons;
      AlertAction action;

      // Check for action (to be executed when notification is clicked)
      if (messageData.containsKey("action")) {
        Map<String, dynamic> actionJson = messageData["action"];

        if (actionJson != null && actionJson.isNotEmpty) {
          action = AlertAction.fromJson(actionJson);

          if(onResume || onLaunch) {
            if(action.openInBrowser) {

            }
          }
        }
      }

      // Check for buttons
      if (messageData.containsKey("buttons")) {
        List<Map<String, dynamic>> buttonsJson = messageData["buttons"];

        if (buttonsJson != null && buttonsJson.isNotEmpty) {
          buttons = buttonsJson.map((e) => AlertButton.fromJson(e));
        }
      }
    }
  }
}

class PushNotificationService {
  final FirebaseMessaging _fcm = FirebaseMessaging();

  Future initialise() async {
    _fcm.configure(
      onMessage: (Map<String, dynamic> remoteMessage) async {
        print("onMessage: $remoteMessage");
        await handleNotification(remoteMessage, false, false);
      },
      onLaunch: (Map<String, dynamic> remoteMessage) async {
        print("onLaunch: $remoteMessage");
        await handleNotification(remoteMessage, false, true);
      },
      onResume: (Map<String, dynamic> remoteMessage) async {
        print("onResume: $remoteMessage");
        await handleNotification(remoteMessage, true, false);
      },
      onBackgroundMessage: Platform.isIOS ? null : myBackgroundMessageHandler,
    );
  }
}





import 'dart:convert';
import 'dart:io';

import 'package:awesome_dialog/awesome_dialog.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/widgets.dart';
import 'package:generic_app/ConsoleProviders/GeofenceProvider.dart';
import 'package:generic_app/ConsoleProviders/StorageProvider.dart';
import 'package:generic_app/events/OpenWebPageEvent.dart';
import 'package:generic_app/models/AlertAction.dart';
import 'package:generic_app/models/AlertButton.dart';

import 'EventBusService.dart';

Future<dynamic> myBackgroundMessageHandler(Map<String, dynamic> remoteMessage) async {
  await PushNotificationService.handleNotification(remoteMessage, false, false);
}

class PushNotificationService {
  static FirebaseMessaging _fcm;

  static BuildContext _context;

  static init({@required BuildContext context}) {
    _fcm = FirebaseMessaging();
    _context = context;
  }

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

  static Future handleNotification(Map<String, dynamic> remoteMessage, bool onResume, bool onLaunch) async {
    Map<dynamic, dynamic> messageData = remoteMessage["data"] ??
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
          Map<dynamic, dynamic> actionJson = messageData["action"];

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
          String buttonsJson = messageData["buttons"];

          if (buttonsJson != null && buttonsJson.isNotEmpty) {
            buttons = (jsonDecode(buttonsJson) as List).map((e) => AlertButton.fromJson(e));
          }
        }

        AwesomeDialog(
          context: _context,
          dialogType: DialogType.INFO,
            animType: AnimType.BOTTOMSLIDE,
          title: title,
          desc: body,
          btnOkText: buttons != null && buttons.isNotEmpty ? buttons[0].title : null,
          btnCancelText: buttons != null && buttons.isNotEmpty ? buttons[1].title: null,
          btnOkOnPress: () {
            if (buttons != null && buttons.isNotEmpty) {
              AlertButton button = buttons[0];
              if (button.action.url != null && button.action.url.isNotEmpty) {
                if (button.action.openInBrowser) {

                } else {
                  EventBusService
                      .getInstance()
                      .eventBus
                      .fire(new OpenWebPageEvent(button.action.url));
                }
              }
            }
          },
            btnCancelOnPress: () {}
        );
      }
    }
  }
}





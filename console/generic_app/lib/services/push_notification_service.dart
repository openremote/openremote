import 'dart:convert';
import 'dart:io';

import 'package:awesome_dialog/awesome_dialog.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:generic_app/ConsoleProviders/geofence_provider.dart';
import 'package:generic_app/ConsoleProviders/storage_provider.dart';
import 'package:generic_app/config/current_console_app_config.dart';
import 'package:generic_app/events/open_web_page_event.dart';
import 'package:generic_app/models/alert_action.dart';
import 'package:generic_app/models/alert_button.dart';
import 'package:url_launcher/url_launcher.dart';

import 'event_bus_service.dart';

Future<dynamic> backgroundMessageHandler(Map<String, dynamic> remoteMessage) async {
  print("background: $remoteMessage");
  Map<dynamic, dynamic> messageData = remoteMessage["data"] ?? remoteMessage;

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
        final StorageProvider storageProvider = await StorageProvider
            .getInstance();
        storageProvider.store(messageData["key"], messageData["value"]);
        break;
    }
  }
}

class PushNotificationService {
  static FirebaseMessaging _fcm;

  static BuildContext _context;

  static init({@required BuildContext context}) {
    _context = context;

    _fcm = FirebaseMessaging();
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
      onBackgroundMessage: Platform.isIOS ? null : backgroundMessageHandler,
    );
  }

  static Future handleNotification(
      Map<String, dynamic> remoteMessage, bool onResume, bool onLaunch) async {
    Map<dynamic, dynamic> messageData = remoteMessage["data"] ?? remoteMessage;
    if (messageData != null) {
      bool isSilent = (Platform.isAndroid && !messageData.containsKey("or-title"))
          || (Platform.isIOS && messageData["aps"]["alert"] == null);

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
        String title = Platform.isAndroid ? messageData["or-title"] : messageData["aps"]["alert"]["title"];
        String body = Platform.isAndroid ? messageData["or-body"] : messageData["aps"]["alert"]["body"];

        List<AlertButton> buttons;
        AlertButton confirmButton;
        AlertButton declineButton;
        AlertAction action;

        // Check for action (to be executed when notification is clicked)
        if (messageData.containsKey("action")) {
          String actionJson = messageData["action"];

          if (actionJson != null && actionJson.isNotEmpty) {
            action = AlertAction.fromJson(jsonDecode(actionJson));
            if (onResume || onLaunch) {
              performAlertAction(action);
            }
          }
        }

        // Check for buttons
        if (messageData.containsKey("buttons")) {
          String buttonsJson = messageData["buttons"];
          if (buttonsJson != null && buttonsJson.isNotEmpty) {
            buttons = List.from(jsonDecode(buttonsJson)).map((e) => AlertButton.fromJson(e)).toList();
            confirmButton = buttons.firstWhere((element) => element.action != null, orElse: () => null);
            declineButton = buttons.firstWhere((element) => element.action == null, orElse: () => null);
          }
        }

        if(!onResume && !onLaunch) {
          AwesomeDialog(
              context: _context,
              dialogType: DialogType.INFO,
              animType: AnimType.BOTTOMSLIDE,
              title: title,
              desc: body,
              btnOkText: confirmButton?.title ?? "Ok",
              btnOkColor: CurrentConsoleAppConfig.instance.primaryColor,
              btnOkOnPress: confirmButton != null ? () {
                if (confirmButton != null && confirmButton.action != null) {
                  performAlertAction(confirmButton.action);
                }
                else if (action != null) {
                  performAlertAction(action);
                }
              } : null,
              btnCancelText: declineButton?.title,
              btnCancelColor: Colors.black54,
              btnCancelOnPress: declineButton != null ?  () {
              } : null
          ).show();
        }
      }
    }
  }

  static void performAlertAction(AlertAction action) async {
    if (action.url != null && action.url.isNotEmpty) {
      if (action.openInBrowser) {
        if (await canLaunch(action.url)) {
          await launch(action.url);
        }
      } else {
        EventBusService
            .getInstance()
            .eventBus
            .fire(new OpenWebPageEvent(action.url));
      }
    }
  }
}





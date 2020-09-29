import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:generic_app/ConsoleProviders/geofence_provider.dart';
import 'package:generic_app/ConsoleProviders/push_provider.dart';
import 'package:generic_app/ConsoleProviders/storage_provider.dart';
import 'package:generic_app/config/current_console_app_config.dart';
import 'package:generic_app/events/open_web_page_event.dart';
import 'package:generic_app/models/link_config.dart';
import 'package:generic_app/services/event_bus_service.dart';
import 'package:generic_app/services/push_notification_service.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebViewPage extends StatefulWidget {
  const WebViewPage({Key key, this.title, this.initialUrl}) : super(key: key);

  final String title;
  final String initialUrl;

  @override
  _WebViewPageState createState() => _WebViewPageState();
}

class _WebViewPageState extends State<WebViewPage> {
  WebViewController _controller;
  GeofenceProvider _geofenceProvider;
  PushProvider _pushProvider;
  StorageProvider _storageProvider;
  WebView _webView;

  @override
  void initState() {
    EventBusService
        .getInstance()
        .eventBus
        .on<OpenWebPageEvent>()
        .listen((event) {
      _controller.loadUrl(event.url);
    });
    PushNotificationService.init(context: context);

    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    _webView = WebView(
      debuggingEnabled: true,
      onPageStarted: (url) {
        print("started: $url");
      },
      initialUrl: Uri.encodeFull(widget.initialUrl),
      javascriptMode: JavascriptMode.unrestricted,
      onWebViewCreated: (WebViewController webViewController) {
        _controller = webViewController;
      },
      javascriptChannels: <JavascriptChannel>{
        _postMessageJavascriptChannel(context),
      },
      navigationDelegate: (navigationRequest) async {
        if (navigationRequest.url.startsWith("webbrowser")) {
          final String navUrl =
              navigationRequest.url.replaceAll("webbrowser", "https");
          if (await canLaunch(navUrl)) {
            await launch(navUrl);
          }
          return NavigationDecision.prevent;
        }
        return NavigationDecision.navigate;
      },
    );

    final stack = Stack(children: <Widget>[_webView]);

    if (CurrentConsoleAppConfig.instance.menuEnabled) {
      switch (CurrentConsoleAppConfig.instance.menuPosition) {
        case "BOTTOM_RIGHT":
          stack.children
              .add(Positioned(bottom: 25, right: 25, child: _menuPopup(-50, -80)));
          break;
        case "TOP_LEFT":
          stack.children
              .add(Positioned(top: 25, left: 25, child: _menuPopup(0, 80)));
          break;
        case "TOP_RIGHT":
          stack.children
              .add(Positioned(top: 25, right: 25, child: _menuPopup(-50, 80)));
          break;
        case "BOTTOM_LEFT":
        default:
          stack.children
              .add(Positioned(bottom: 25, left: 25, child: _menuPopup(0, -80)));
          break;
      }
    }

    return WillPopScope( onWillPop: _onBackPressed, child: Scaffold(body: SafeArea(child: stack)));
  }

  void _notifyClient(Map<String, dynamic> data) {
    final String jsonString = json.encode(data);
    _controller.evaluateJavascript(
        "OpenRemoteConsole._handleProviderResponse(JSON.stringify($jsonString))");
  }

  JavascriptChannel _postMessageJavascriptChannel(BuildContext context) {
    return JavascriptChannel(
        name: 'MobileInterface',
        onMessageReceived: (JavascriptMessage message) {
          final Map<String, dynamic> messageContent = json.decode(message.message);
          final String messageType = messageContent["type"];
          final Map<String, dynamic> data = messageContent["data"];
          switch (messageType) {
            case "error":
              print("Received WebApp message, error: " + data["error"]);
              Scaffold.of(context).showSnackBar(
                SnackBar(content: Text(data["error"])),
              );
              break;
            case "provider":
              final String action = data["action"];
              if (action != null) {
                final String provider = data["provider"];
                if (provider == "geofence") {
                  _handleGeofenceProviderMessage(data);
                } else if (provider == "push") {
                  _handlePushProviderMessage(data);
                } else if (provider == "storage") {
                  _handleStorageProviderMessage(data);
                }
              }
              break;
            default:
          }
        });
  }

  Future _handleGeofenceProviderMessage(Map<String, dynamic> data) async {
    final String action = data["action"];

    _geofenceProvider ??= await GeofenceProvider.getInstance();

    if (action == "PROVIDER_INIT") {
      _notifyClient(await _geofenceProvider.initialize());
    } else if (action == "PROVIDER_ENABLE") {
      final String consoleId = data["consoleId"];

      if (consoleId != null) {
        await _geofenceProvider.enable(
            CurrentConsoleAppConfig.instance.baseUrl, consoleId, (enableData) {
          _notifyClient(enableData);
        });
      }
    } else if (action == "PROVIDER_DISABLE") {
      _notifyClient(await _geofenceProvider.disable());
    } else if (action == "GEOFENCE_REFRESH") {
      _geofenceProvider.refreshGeofences();
    } else if (action == "GET_LOCATION") {
      await _geofenceProvider.getLocation(
          (locationData) => _notifyClient(locationData), context);
    }
  }

  Future _handlePushProviderMessage(Map<String, dynamic> data) async {
    final String action = data["action"];

    _pushProvider ??= await PushProvider.getInstance();
    if (action == "PROVIDER_INIT") {
      _notifyClient(await _pushProvider.initialize());
    } else if (action == "PROVIDER_ENABLE") {
      final String consoleId = data["consoleId"];

      if (consoleId != null) {
        await _pushProvider.enable(
            consoleId, (enableData) => _notifyClient(enableData));
      }
    } else if (action == "PROVIDER_DISABLE") {
      _notifyClient(await _pushProvider.disable());
    }
  }

  Future _handleStorageProviderMessage(Map<String, dynamic> data) async {
    final String action = data["action"];

    _storageProvider ??= await StorageProvider.getInstance();

    if (action == "PROVIDER_INIT") {
      _notifyClient(_storageProvider.initialize());
    } else if (action == "PROVIDER_ENABLE") {
      _notifyClient(_storageProvider.enable());
    } else if (action == "STORE") {
      final String key = data["key"];
      final String valueJson = data["value"];
      _storageProvider.store(key, valueJson);
    } else if (action == "RETRIEVE") {
      final String key = data["key"];
      _notifyClient(_storageProvider.retrieve(key));
    }
  }

  Widget _menuPopup(double offsetX, double offsetY) => PopupMenuButton<int>(
        itemBuilder: (context) {
          final List<PopupMenuItem<int>> items = <PopupMenuItem<int>>[];

          if (CurrentConsoleAppConfig.instance.links == null) return items;

          int index = 0;
          for (final LinkConfig linkConfig in CurrentConsoleAppConfig.instance.links) {
            items.add(PopupMenuItem(
              value: index++,
              child: Text(
                linkConfig.displayText,
                style: const TextStyle(color: Colors.black87),
              ),
            ));
          }
          return items;
        },
        offset: Offset(offsetX, offsetY * CurrentConsoleAppConfig.instance.links?.length ?? 1),
        elevation: 2,
        onSelected: (selectedValue) {
          final LinkConfig linkConfig =
              CurrentConsoleAppConfig.instance.links[selectedValue];
          _controller.loadUrl(Uri.encodeFull(linkConfig.pageLink));
        },
        child: Container(
          height: 50,
          width: 50,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: CurrentConsoleAppConfig.instance.primaryColor,
            boxShadow: [
              BoxShadow(
                color: Colors.grey.withOpacity(0.5),
                spreadRadius: 2,
                blurRadius: 2,
              ),
            ]
          ),
          child: const Icon(Icons.menu),
        ),
      );

  Future<bool> _onBackPressed() async {
    if (await _controller.canGoBack()) {
      _controller.goBack();
      return Future.value(false);
    } else {
      return Future.value(true);
    }
  }
}

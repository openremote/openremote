import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:generic_app/ConsoleProviders/GeofenceProvider.dart';
import 'package:generic_app/ConsoleProviders/PushProvider.dart';
import 'package:generic_app/ConsoleProviders/StorageProvider.dart';
import 'package:generic_app/config/CurrentConsoleAppConfig.dart';
import 'package:generic_app/models/LinkConfig.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebViewPage extends StatefulWidget {
  WebViewPage({Key key, this.title, this.initialUrl}) : super(key: key);

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
      javascriptChannels: <JavascriptChannel>[
        _postMessageJavascriptChannel(context),
      ].toSet(),
      navigationDelegate: (navigationRequest) {
        return NavigationDecision.navigate;
      },
    );

    var stack = Stack(children: <Widget>[_webView]);

    if (CurrentConsoleAppConfig.instance.menuEnabled) {
      switch (CurrentConsoleAppConfig.instance.menuPosition) {
        case "BOTTOM_RIGHT":
          stack.children
              .add(Positioned(bottom: 25, right: 25, child: _menuPopup(-50, -100)));
          break;
        case "TOP_LEFT":
          stack.children
              .add(Positioned(top: 25, left: 25, child: _menuPopup(0, 100)));
          break;
        case "TOP_RIGHT":
          stack.children
              .add(Positioned(top: 25, right: 25, child: _menuPopup(-50, 100)));
          break;
        case "BOTTOM_LEFT":
        default:
          stack.children
              .add(Positioned(bottom: 25, left: 25, child: _menuPopup(0, -100)));
          break;
      }
    }

    return WillPopScope( onWillPop: _onBackPressed, child: Scaffold(body: stack));
  }

  _notifyClient(Map<String, dynamic> data) {
    final String jsonString = json.encode(data);
    _controller.evaluateJavascript(
        "OpenRemoteConsole._handleProviderResponse(JSON.stringify($jsonString))");
  }

  JavascriptChannel _postMessageJavascriptChannel(BuildContext context) {
    return JavascriptChannel(
        name: 'MobileInterface',
        onMessageReceived: (JavascriptMessage message) {
          Map<String, dynamic> messageContent = json.decode(message.message);
          String messageType = messageContent["type"];
          Map<String, dynamic> data = messageContent["data"];
          switch (messageType) {
            case "error":
              print("Received WebApp message, error: " + data["error"]);
              Scaffold.of(context).showSnackBar(
                SnackBar(content: Text(data["error"])),
              );
              break;
            case "provider":
              String action = data["action"];
              if (action != null) {
                String provider = data["provider"];
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

  _handleGeofenceProviderMessage(Map<String, dynamic> data) async {
    String action = data["action"];

    if (_geofenceProvider == null) {
      _geofenceProvider = await GeofenceProvider.getInstance();
    }

    if (action == "PROVIDER_INIT") {
      _notifyClient(await _geofenceProvider.initialize());
    } else if (action == "PROVIDER_ENABLE") {
      String consoleId = data["consoleId"];

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

  _handlePushProviderMessage(Map<String, dynamic> data) async {
    String action = data["action"];

    if (_pushProvider == null) {
      _pushProvider = await PushProvider.getInstance();
    }
    if (action == "PROVIDER_INIT") {
      _notifyClient(await _pushProvider.initialize());
    } else if (action == "PROVIDER_ENABLE") {
      String consoleId = data["consoleId"];

      if (consoleId != null) {
        await _pushProvider.enable(
            consoleId, (enableData) => _notifyClient(enableData));
      }
    } else if (action == "PROVIDER_DISABLE") {
      _notifyClient(await _pushProvider.disable());
    }
  }

  _handleStorageProviderMessage(Map<String, dynamic> data) async {
    String action = data["action"];

    if (_storageProvider == null) {
      _storageProvider = await StorageProvider.getInstance();
    }

    if (action == "PROVIDER_INIT") {
      _notifyClient(_storageProvider.initialize());
    } else if (action == "PROVIDER_ENABLE") {
      _notifyClient(_storageProvider.enable());
    } else if (action == "STORE") {
      String key = data["key"];
      String valueJson = data["value"];
      _storageProvider.store(key, valueJson);
    } else if (action == "RETRIEVE") {
      String key = data["key"];
      _notifyClient(_storageProvider.retrieve(key));
    }
  }

  Widget _menuPopup(double offsetX, double offsetY) => PopupMenuButton<int>(
        itemBuilder: (context) {
          List<PopupMenuItem<int>> items = new List<PopupMenuItem<int>>();

          if (CurrentConsoleAppConfig.instance.links == null) return items;

          int index = 0;
          for (LinkConfig linkConfig
              in CurrentConsoleAppConfig.instance.links) {
            items.add(PopupMenuItem(
              value: index++,
              child: Text(
                linkConfig.displayText,
                style: TextStyle(color: Colors.black87),
              ),
            ));
          }
          return items;
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
          child: Icon(Icons.menu),
        ),
        offset: Offset(offsetX, offsetY),
        elevation: 2,
        onSelected: (selectedValue) {
          LinkConfig linkConfig =
              CurrentConsoleAppConfig.instance.links[selectedValue];
          _controller.loadUrl(Uri.encodeFull(linkConfig.pageLink));
        },
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

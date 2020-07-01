import 'dart:async';

import 'package:flutter/material.dart';
import 'package:generic_app/ConsoleProviders/GeofenceProvider.dart';
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
  final Completer<WebViewController> _controller =
  Completer<WebViewController>();

  GeofenceProvider _geofenceProvider;

  @override
  void initState() {
    GeofenceProvider.getInstance().then((value) => _geofenceProvider = value);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    var stack = Stack(children: <Widget>[
      WebView(
        onPageStarted: (url) {
          print("started: $url");
        },
        initialUrl: Uri.encodeFull(widget.initialUrl),
        javascriptMode: JavascriptMode.unrestricted,
        onWebViewCreated: (WebViewController webViewController) {
          _controller.complete(webViewController);
        },
        javascriptChannels: <JavascriptChannel>[
          _toasterJavascriptChannel(context),
        ].toSet(),
        navigationDelegate: (navigationRequest) {
          return NavigationDecision.navigate;
        },
      )
    ]);

    if (CurrentConsoleAppConfig.instance.menuEnabled) {
      switch (CurrentConsoleAppConfig.instance.menuPosition) {
        case "BOTTOM_RIGHT":
          stack.children.add(Positioned(bottom: 25, right: 25, child: _menuPopup()));
          break;
        case "TOP_LEFT":
          stack.children.add(Positioned(top: 25, left: 25, child: _menuPopup()));
          break;
        case "TOP_RIGHT":
          stack.children.add(Positioned(top: 25, right: 25, child: _menuPopup()));
          break;
        case "BOTTOM_LEFT":
        default:
          stack.children.add(Positioned(bottom: 25, left: 25, child: _menuPopup()));
          break;
      }
    }

    return Scaffold(body: SafeArea(child: stack));
  }

  JavascriptChannel _toasterJavascriptChannel(BuildContext context) {
    return JavascriptChannel(
        name: 'Toaster',
        onMessageReceived: (JavascriptMessage message) {
          Scaffold.of(context).showSnackBar(
            SnackBar(content: Text(message.message)),
          );
        });
  }

  Widget _menuPopup() =>
      PopupMenuButton<int>(
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
          ),
          child: Icon(Icons.menu),
        ),
        offset: Offset(0, -100),
        elevation: 2,
        onSelected: (selectedValue) {
          LinkConfig linkConfig =
          CurrentConsoleAppConfig.instance.links[selectedValue];
          print(linkConfig);
        },
      );
}

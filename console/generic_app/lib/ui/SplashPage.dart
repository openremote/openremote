import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:generic_app/config/CurrentConsoleAppConfig.dart';
import 'package:generic_app/models/ConsoleAppConfig.dart';
import 'package:generic_app/network/ApiManager.dart';
import 'package:generic_app/ui/ProjectPage.dart';
import 'package:quick_actions/quick_actions.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../main.dart';
import 'WebViewPage.dart';

class SplashPage extends StatefulWidget {
  SplashPage({Key key}) : super(key: key);

  @override
  _SplashPageState createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage> {
  SharedPreferences _sharedPreferences;
  String _projectName;
  String _realmName;

  @override
  void initState() {
    final QuickActions quickActions = QuickActions();
    quickActions.setShortcutItems(<ShortcutItem>[
      const ShortcutItem(
          type: 'starting_page',
          localizedTitle: 'Change project',
          icon: 'ic_settings'),
    ]);
    quickActions.initialize((String shortcutType) async {
      if (shortcutType == 'starting_page') {
        _sharedPreferences = await SharedPreferences.getInstance();
        await _sharedPreferences.clear();
        ORApp.navigatorKey.currentState.pushReplacement(MaterialPageRoute(builder: (context) => ProjectPage()));
      }
    });

    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return new FutureBuilder(
        future: Future.delayed(Duration(seconds: 3), _getProjectData),
        builder: (BuildContext context, AsyncSnapshot snapshot) {
          if (snapshot.hasData) {
            Future.delayed(Duration.zero, () {
              if (snapshot.data) {
                Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(
                        builder: (context) => WebViewPage(
                            initialUrl: CurrentConsoleAppConfig.instance.initialUrl)));
              } else {
                Navigator.pushReplacement(context,
                    MaterialPageRoute(builder: (context) => ProjectPage()));
              }
            });
          }
          return _splashIcon();
        });
  }

  Widget _splashIcon() {
    return Scaffold(
        body: Align(
            alignment: Alignment.center,
            child: Container(
              padding: EdgeInsets.all(100),
              child: Image.asset('assets/images/or_splash.png',
                  fit: BoxFit.contain),
            )));
  }

  Future<bool> _getProjectData() async {
    _sharedPreferences = await SharedPreferences.getInstance();
    _projectName = _sharedPreferences.getString("project");
    _realmName = _sharedPreferences.getString("realm");

    bool hasData = _projectName != null && _realmName != null;
    if (hasData) {
      hasData = await _getConsoleAppConfig();
    }

    return hasData;
  }

  Future<bool> _getConsoleAppConfig() async {
    var apiManager =
        new ApiManager("http://192.168.100.9:8080/api/$_realmName");
    return apiManager
        .get(["app", "config"], ConsoleAppConfig.fromJson).then((value) {
      CurrentConsoleAppConfig.instance.updateConfig(value, _projectName);
      return true;
    }).catchError((onError) {
      print(onError);
      return false;
    });
  }
}

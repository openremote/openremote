import 'dart:async';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:generic_app/config/CurrentConsoleAppConfig.dart';
import 'package:generic_app/generated/l10n.dart';
import 'package:generic_app/models/ConsoleAppConfig.dart';
import 'package:generic_app/network/ApiManager.dart';
import 'package:generic_app/ui/ProjectPage.dart';
import 'package:quick_actions/quick_actions.dart';
import 'package:shared_preferences/shared_preferences.dart';

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

  String _shortCut;

  @override
  void initState() {
    final QuickActions quickActions = QuickActions();
    quickActions.setShortcutItems(<ShortcutItem>[
      const ShortcutItem(
          type: 'project_page', localizedTitle: 'startingPage', icon: 'home'),
    ]);
    quickActions.initialize((String shortcutType) {
        _shortCut = shortcutType;
    });


    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (_shortCut != null && _shortCut == 'startingPage') {
      Future.delayed(
          Duration(seconds: 3),
          () => Navigator.pushReplacement(
              context, MaterialPageRoute(builder: (context) => ProjectPage())));
      return _splashIcon();
    } else {
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
                              initialUrl:
                                  CurrentConsoleAppConfig.instance.url)));
                } else {
                  Navigator.pushReplacement(context,
                      MaterialPageRoute(builder: (context) => ProjectPage()));
                }
              });
            }
            return _splashIcon();
          });
    }
  }

  Widget _splashIcon() {
    return Scaffold(
        body: Align(
            alignment: Alignment.center,
            child: Container(
              padding: EdgeInsets.all(50),
              child: Image.asset('assets/images/or_splash.png',
                  fit: BoxFit.contain),
            )));
  }

  Future<bool> _getProjectData() async {
    _sharedPreferences = await SharedPreferences.getInstance();
    _projectName = _sharedPreferences.getString("project");
    _realmName = _sharedPreferences.getString("realm");

    bool hasData = _projectName != null && _realmName != null && _shortCut == null;
    if (hasData) {
      hasData = await _getConsoleAppConfig();
    }

    return hasData && _shortCut == null;
  }

  Future<bool> _getConsoleAppConfig() async {
    var apiManager =  new ApiManager("https://$_projectName.openremote.io/api/$_realmName");
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

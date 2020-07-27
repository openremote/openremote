import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:generic_app/config/CurrentConsoleAppConfig.dart';
import 'package:generic_app/models/ConsoleAppConfig.dart';
import 'package:generic_app/network/ApiManager.dart';
import 'package:generic_app/ui/ProjectPage.dart';
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

  @override
  Widget build(BuildContext context) {
    return new FutureBuilder(
        future: Future.delayed(Duration(seconds: 3), _getProjectData),
        builder: (BuildContext context, AsyncSnapshot snapshot) {
          if (snapshot.hasData) {
            Future.delayed(Duration.zero, () {
              if (snapshot.data) {
                Navigator.pushReplacement(context,
                    MaterialPageRoute(builder: (context) => WebViewPage(initialUrl: CurrentConsoleAppConfig.instance.url)));
              } else {
                Navigator.pushReplacement(context,
                    MaterialPageRoute(builder: (context) => ProjectPage()));
              }
            });
          }
          return Scaffold(
              body: Align(
                  alignment: Alignment.center,
                  child: Container(
                    padding: EdgeInsets.all(50),
                    child: Image.asset('assets/images/or_splash.png',
                        fit: BoxFit.contain),
                  )));
        });
  }

  Future<bool> _getProjectData() async {
    _sharedPreferences = await SharedPreferences.getInstance();
    _projectName = _sharedPreferences.getString("project");
    _realmName = _sharedPreferences.getString("realm");

    bool hasData = _projectName != null && _realmName != null;
    if (hasData) {
      await _getConsoleAppConfig();
    }

    return hasData;
  }

  Future _getConsoleAppConfig() async {
    var apiManager = new ApiManager("https://$_projectName.openremote.io/api/$_realmName");
    return apiManager
        .get(["app", "config"], ConsoleAppConfig.fromJson).then((value) {
      print(value);
      CurrentConsoleAppConfig.instance.updateConfig(value, _projectName);
    }).catchError((onError) {
      print(onError);
    });
  }
}

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
        future: _getProjectData(),
        builder: (BuildContext context, AsyncSnapshot snapshot) {
          Timer(Duration(seconds: 3), () {
            if (snapshot.hasData) {
              if (snapshot.data) {
                _getConsoleAppConfig();
              }
            } else {
              Navigator.pushReplacement(context,
                  CupertinoPageRoute(builder: (context) => ProjectPage()));
            }
          });
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

    return _projectName != null && _realmName != null;
  }

  void _getConsoleAppConfig() {
    var apiManager =
        new ApiManager("http://192.168.100.9:8080/api/$_realmName");
    apiManager.get(["app", "config"], ConsoleAppConfig.fromJson).then((value) {
      print(value);
      CurrentConsoleAppConfig.instance.updateConfig(value, _projectName);
      Navigator.pushReplacement(
          context,
          CupertinoPageRoute(
              builder: (context) => WebViewPage(
                    initialUrl: CurrentConsoleAppConfig.instance.url,
                  )));
    }).catchError((onError) {
      print(onError);
      Navigator.pushReplacement(
          context, CupertinoPageRoute(builder: (context) => ProjectPage()));
    });
  }
}

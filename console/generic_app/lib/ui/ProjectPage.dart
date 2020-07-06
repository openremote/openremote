import 'package:flutter/material.dart';
import 'package:generic_app/config/CurrentConsoleAppConfig.dart';
import 'package:generic_app/models/ConsoleAppConfig.dart';
import 'package:generic_app/network/ApiManager.dart';
import 'package:generic_app/ui/WebViewPage.dart';

class ProjectPage extends StatefulWidget {
  ProjectPage({Key key}) : super(key: key);

  @override
  _ProjectPageState createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage> {
  bool _isLoading;
  String _projectName;
  String _realmName;
  BuildContext _innerContext;

  @override
  void initState() {
    super.initState();
    _isLoading = false;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(body: Builder(builder: (BuildContext innerContext) {
      _innerContext = innerContext;
      return SafeArea(
          child: SingleChildScrollView(
              physics: ClampingScrollPhysics(),
              child: Align(
                  alignment: Alignment.center,
                  child: Column(children: <Widget>[
                    Container(
                        padding: EdgeInsets.only(top: 25, left: 20, right: 20),
                        height: MediaQuery.of(context).size.height / 2,
                        child: Column(children: <Widget>[
                          Expanded(
                              child: Image.asset(
                                  'assets/images/logo-mobile.png',
                                  fit: BoxFit.contain)),
                          Expanded(
                              child: Image.asset('assets/images/logo.png',
                                  fit: BoxFit.contain))
                        ])),
                    Container(
                      child: Column(
                        children: <Widget>[
                          Container(
                            child: TextField(
                                cursorColor: CurrentConsoleAppConfig
                                    .instance.primaryColor,
                                decoration: InputDecoration(
                                  labelText: 'Project',
                                  labelStyle: TextStyle(
                                      color: CurrentConsoleAppConfig
                                          .instance.primaryColor),
                                  focusedBorder: UnderlineInputBorder(
                                    borderSide: BorderSide(
                                        color: CurrentConsoleAppConfig
                                            .instance.primaryColor),
                                  ),
                                ),
                                textInputAction: TextInputAction.next,
                                onChanged: (inputText) {
                                  _projectName = inputText;
                                },
                                onSubmitted: (inputText) {
                                  FocusScope.of(context).nextFocus();
                                }),
                            padding: EdgeInsets.all(25),
                          ),
                          Container(
                            child: TextField(
                                cursorColor: CurrentConsoleAppConfig
                                    .instance.primaryColor,
                                decoration: InputDecoration(
                                    labelText: 'Realm',
                                    labelStyle: TextStyle(
                                        color: CurrentConsoleAppConfig
                                            .instance.primaryColor),
                                    focusedBorder: UnderlineInputBorder(
                                      borderSide: BorderSide(
                                          color: CurrentConsoleAppConfig
                                              .instance.primaryColor),
                                    )),
                                textInputAction: TextInputAction.done,
                                onChanged: (inputText) {
                                  _realmName = inputText;
                                },
                                onSubmitted: (inputText) {
                                  _getConsoleAppConfig();
                                  FocusScope.of(context).unfocus();
                                }),
                            padding: EdgeInsets.all(25),
                          ),
                          Container(
                            child: Align(
                              alignment: Alignment.centerRight,
                              child: RaisedButton(
                                  color: CurrentConsoleAppConfig
                                      .instance.primaryColor,
                                  focusColor: CurrentConsoleAppConfig
                                      .instance.secondaryColor,
                                  textColor: Colors.white,
                                  onPressed: () {
                                    _getConsoleAppConfig();
                                  },
                                  child: Text("Press")),
                            ),
                            padding: EdgeInsets.all(25),
                          )
                        ],
                      ),
                    )
                  ])
              )
          )
      );
    })
    );
  }

  void _getConsoleAppConfig() {
    var apiManager =
        new ApiManager("http://192.168.2.10:8080/api/${_realmName}");
//        new ApiManager("https://${_projectName}.openremote.io/${_realmName}");
    apiManager.get(["app", "config"], ConsoleAppConfig.fromJson).then((value) {
      print(value);
      CurrentConsoleAppConfig.instance.updateConfig(value, _projectName);
      Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => WebViewPage(
                    initialUrl: CurrentConsoleAppConfig.instance.url,
                  )));
    }).catchError((onError) => Scaffold.of(_innerContext).showSnackBar(
          SnackBar(content: Text("error")),
        ));
  }
}

import 'package:flutter/material.dart';
import 'package:generic_app/models/ConsoleAppConfig.dart';
import 'package:generic_app/network/ApiManager.dart';

class ProjectPage extends StatefulWidget {
  ProjectPage({Key key}) : super(key: key);

  @override
  _ProjectPageState createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage> {
  bool _isLoading;
  String _projectName;
  String _realmName;

  @override
  void initState() {
    super.initState();
    _isLoading = false;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: SafeArea(
            child: SingleChildScrollView(
                physics: ClampingScrollPhysics(),
                child: Align(
                    alignment: Alignment.center,
                    child: Column(children: <Widget>[
                      Container(
                          padding: EdgeInsets.only(top: 25),
                          height: MediaQuery.of(context).size.height / 2,
                          child: Column(children: <Widget>[
                            Expanded(
                                child: Image.asset(
                                    'assets/images/logo-mobile.png',
                                    fit: BoxFit.cover)),
                            Expanded(
                                child: Image.asset('assets/images/logo.png',
                                    fit: BoxFit.cover))
                          ])),
                      Container(
                        child: Column(
                          children: <Widget>[
                            Container(
                              child: TextField(
                                  decoration:
                                      InputDecoration(labelText: 'Project'),
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
                                  decoration:
                                      InputDecoration(labelText: 'Realm'),
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
                    ])))));
  }

  void _getConsoleAppConfig() {
    var apiManager = new ApiManager("http://localhost:8080/api/${_realmName}");
//        new ApiManager("https://${_projectName}.openremote.io/${_realmName}");
    apiManager.get(["app", "config"], ConsoleAppConfig.fromJson).then(
        (value) => print(value));
  }
}

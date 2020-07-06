import 'package:flutter/material.dart';
import 'package:generic_app/config/CurrentConsoleAppConfig.dart';
import 'package:generic_app/generated/l10n.dart';
import 'package:generic_app/models/ConsoleAppConfig.dart';
import 'package:generic_app/network/ApiManager.dart';
import 'package:generic_app/ui/WebViewPage.dart';

class ProjectPage extends StatefulWidget {
  ProjectPage({Key key}) : super(key: key);

  @override
  _ProjectPageState createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage> {
  String _projectName;
  String _realmName;
  BuildContext _innerContext;

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
                      child: Expanded(
                          child: Image.asset('assets/images/or_logo.png',
                              fit: BoxFit.contain)),
                    ),
                    Container(
                      color: Colors.white,
                      height: MediaQuery.of(context).size.height / 2,
                      child: Column(
                        children: <Widget>[
                          Align(
                              alignment: Alignment.centerLeft,
                              child: Container(
                                padding: EdgeInsets.only(
                                    top: 25, left: 25, right: 25),
                                child: Text(
                                  S.of(context).connectToYourApplication,
                                  style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 18,
                                      color: Colors.black54),
                                ),
                              )),
                          Container(
                            child: TextField(
                                cursorColor: CurrentConsoleAppConfig
                                    .instance.primaryColor,
                                decoration: InputDecoration(
                                  labelText: S.of(context).project,
                                  filled: true,
                                  fillColor: Colors.grey.shade100,
                                  focusedBorder: InputBorder.none,
                                  enabledBorder: InputBorder.none,
                                  errorBorder: InputBorder.none,
                                  disabledBorder: InputBorder.none,
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
                                  labelText: S.of(context).realm,
                                  filled: true,
                                  fillColor: Colors.grey.shade100,
                                  focusedBorder: InputBorder.none,
                                  enabledBorder: InputBorder.none,
                                  errorBorder: InputBorder.none,
                                  disabledBorder: InputBorder.none,
                                ),
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
                                  child: Text(S.of(context).connect)),
                            ),
                            padding: EdgeInsets.all(25),
                          )
                        ],
                      ),
                    )
                  ]))));
    }));
  }

  void _getConsoleAppConfig() {
    var apiManager =
        new ApiManager("https://$_projectName.openremote.io/$_realmName");
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
          SnackBar(
              content: Text(
                  "Error occurred getting app config. Check your input and try again")),
        ));
  }
}

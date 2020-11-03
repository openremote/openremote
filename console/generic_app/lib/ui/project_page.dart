import 'package:flutter/material.dart';
import 'package:generic_app/config/current_console_app_config.dart';
import 'package:generic_app/generated/l10n.dart';
import 'package:generic_app/models/console_app_config.dart';
import 'package:generic_app/network/api_manager.dart';
import 'package:generic_app/ui/web_view_page.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ProjectPage extends StatefulWidget {
  const ProjectPage({Key key}) : super(key: key);

  @override
  _ProjectPageState createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage> {
  SharedPreferences _sharedPreferences;
  String _projectName;
  String _realmName;
  BuildContext _innerContext;

  @override
  void initState() {
    SharedPreferences.getInstance().then((value) {
      _sharedPreferences = value;
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {

    return Scaffold(
        backgroundColor: Colors.white,
        body: Builder(builder: (BuildContext innerContext) {
          _innerContext = innerContext;
          return SafeArea(
              child: SingleChildScrollView(
                  physics: const ClampingScrollPhysics(),
                  child: Align(
                      child: Column(children: <Widget>[
                        Container(
                          color: Colors.white,
                          padding:
                              const EdgeInsets.only(top: 25, left: 50, right: 50),
                          height: MediaQuery.of(context).size.height * 0.4,
                          child: Image.asset('assets/images/or_logo.png',
                              fit: BoxFit.contain),
                        ),
                    Container(
                      color: Colors.white,
                      height: MediaQuery.of(context).size.height * 0.6,
                      child: Column(
                        children: <Widget>[
                          Align(
                              alignment: Alignment.centerLeft,
                              child: Container(
                                padding: const EdgeInsets.only(
                                    top: 25, left: 25, right: 25),
                                child: Text(
                                  S.of(context).connectToYourApplication,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 18,
                                      color: Colors.black54),
                                ),
                              )),
                          Container(
                            padding: const EdgeInsets.all(25),
                            child: TextField(
                                cursorColor: CurrentConsoleAppConfig
                                    .instance.primaryColor,
                                decoration: InputDecoration(
                                  border: const UnderlineInputBorder(
                                      borderRadius: BorderRadius.only(
                                          topLeft: Radius.circular(3),
                                          topRight: Radius.circular(3))),
                                  labelText: S
                                      .of(context)
                                      .project,
                                  filled: true,
                                  fillColor: Colors.grey.shade100,
                                ),
                                textInputAction: TextInputAction.next,
                                onChanged: (inputText) {
                                  _projectName = inputText.trim();
                                },
                                onSubmitted: (inputText) {
                                  FocusScope.of(context).nextFocus();
                                }),
                          ),
                          Container(
                            padding: const EdgeInsets.fromLTRB(25, 10, 25, 10),
                            child: TextField(
                                cursorColor: CurrentConsoleAppConfig
                                    .instance.primaryColor,
                                decoration: InputDecoration(
                                  border: const UnderlineInputBorder(
                                      borderRadius: BorderRadius.only(
                                          topLeft: Radius.circular(3),
                                          topRight: Radius.circular(3))),
                                  labelText: S
                                      .of(context)
                                      .realm,
                                  filled: true,
                                  fillColor: Colors.grey.shade100,
                                ),
                                textInputAction: TextInputAction.done,
                                onChanged: (inputText) {
                                  _realmName = inputText.trim();
                                },
                                onSubmitted: (inputText) {
                                  _getConsoleAppConfig();
                                  FocusScope.of(context).unfocus();
                                }),
                          ),
                          Container(
                            padding: const EdgeInsets.fromLTRB(25, 10, 25, 10),
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
                          )
                        ],
                      ),
                    )
                  ]))));
    }));
  }

  void _getConsoleAppConfig() {
    final apiManager =  ApiManager("https://$_projectName.openremote.io/api/$_realmName");
    apiManager.get(["app", "config"], ConsoleAppConfig.fromJson).then((value) {
      _sharedPreferences.setString("project", _projectName);
      _sharedPreferences.setString("realm", _realmName);
      CurrentConsoleAppConfig.instance.updateConfig(value, _projectName);
      Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => WebViewPage(
                    initialUrl: CurrentConsoleAppConfig.instance.initialUrl,
                  )));
    }).catchError((onError) {
      print(onError);
      Scaffold.of(_innerContext).showSnackBar(
          const SnackBar(
              content: Text(
                  "Error occurred getting app config. Check your input and try again")),
        );
    });
  }
}

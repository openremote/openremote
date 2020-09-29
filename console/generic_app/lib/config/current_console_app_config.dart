import 'dart:ui';

import 'package:generic_app/models/console_app_config.dart';
import 'package:generic_app/models/link_config.dart';
import 'package:generic_app/utils/HexColor.dart';

class CurrentConsoleAppConfig {
  static final CurrentConsoleAppConfig instance =
      CurrentConsoleAppConfig._privateConstructor();

  CurrentConsoleAppConfig._privateConstructor();

  void updateConfig(ConsoleAppConfig appConfig, String project) {
    _realm = appConfig.realm;
    _initialUrl = appConfig.initialUrl;
    _url = appConfig.url;
    _menuEnabled = appConfig.menuEnabled;
    _menuPosition = appConfig.menuPosition;
    _primaryColor = appConfig.primaryColor;
    _secondaryColor = appConfig.secondaryColor;
    _links = appConfig.links;
    _projectName = project;
  }

  String _realm;
  String _initialUrl;
  String _url;
  bool _menuEnabled;
  String _menuPosition;
  String _menuImage;
  String _primaryColor;
  String _secondaryColor;
  List<LinkConfig> _links;
  String _projectName;


  String get realm {
    return _realm;
  }

  String get initialUrl {
    return _initialUrl;
  }

  String get url {
    return _url;
    //consolePlatform
    //consoleName=realm
    //consoleVersion
  }

  bool get menuEnabled {
    return _menuEnabled;
  }

  String get menuPosition {
    return _menuPosition ?? "BOTTOM_LEFT";
  }

  String get menuImage {
    return _menuImage ?? "menu";
  }

  Color get primaryColor {
    return HexColor(_primaryColor ?? "#43A047");
  }

  Color get secondaryColor {
    return HexColor(_secondaryColor ?? "#C1D72E");
  }

  List<LinkConfig> get links {
    return _links;
  }

  String get baseUrl {
    return "https://$_projectName.openremote.io/api/$_realm";
  }
}

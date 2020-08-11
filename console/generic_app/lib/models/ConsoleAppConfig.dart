
import 'package:generic_app/models/LinkConfig.dart';
import 'package:json_annotation/json_annotation.dart';

import 'BaseModel.dart';

part 'ConsoleAppConfig.g.dart';

@JsonSerializable()
class ConsoleAppConfig implements BaseModel {

  final int id;
  final String realm;
  final String initialUrl;
  final String url;
  final bool menuEnabled;
  final String  menuPosition;
  final String menuImage;
  final String primaryColor;
  final String secondaryColor;
  final List<LinkConfig> links;

  ConsoleAppConfig({
     this.id,
     this.realm,
     this.initialUrl,
     this.url,
     this.menuEnabled,
     this.menuPosition,
     this.menuImage,
     this.primaryColor,
     this.secondaryColor,
     this.links
   });

  static ConsoleAppConfig fromJson(Map<String, dynamic> json) => _$ConsoleAppConfigFromJson(json);
  Map<String, dynamic> toJson() => _$ConsoleAppConfigToJson(this);
}

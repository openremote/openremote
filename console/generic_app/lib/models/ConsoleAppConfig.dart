
import 'package:json_annotation/json_annotation.dart';

import 'BaseModel.dart';

part 'ConsoleAppConfig.g.dart';

@JsonSerializable()
class ConsoleAppConfig implements BaseModel {

  final int id;
  final String realm  ;

  final String url;

  final bool menuEnabled;

  final String  menuPosition;

  final String menuImage;

  final String primaryColor;

  final String secondaryColor;

  final Map<String, dynamic> links;

  ConsoleAppConfig({
     this.id,
     this.realm,
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
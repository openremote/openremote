
import 'package:generic_app/models/link_config.dart';
import 'package:json_annotation/json_annotation.dart';

import 'base_model.dart';

part 'console_app_config.g.dart';

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
  @override
  Map<String, dynamic> toJson() => _$ConsoleAppConfigToJson(this);
}

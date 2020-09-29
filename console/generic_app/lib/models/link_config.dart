
import 'package:json_annotation/json_annotation.dart';

import 'base_model.dart';

part 'link_config.g.dart';

@JsonSerializable()
class LinkConfig implements BaseModel {

  final String displayText;
  final String pageLink;

  LinkConfig(this.displayText, this.pageLink);

  static LinkConfig fromJson(Map<String, dynamic> json) => _$LinkConfigFromJson(json);
  @override
  Map<String, dynamic> toJson() => _$LinkConfigToJson(this);
}

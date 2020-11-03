
import 'package:generic_app/models/base_model.dart';
import 'package:json_annotation/json_annotation.dart';

import 'alert_action.dart';

part 'alert_button.g.dart';

@JsonSerializable()
class AlertButton implements BaseModel {

  final String title;
  final AlertAction action;

  AlertButton({
    this.title,
    this.action,
  });

  static AlertButton fromJson(Map<String, dynamic> json) => _$AlertButtonFromJson(json);
  @override
  Map<String, dynamic> toJson() => _$AlertButtonToJson(this);
}

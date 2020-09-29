
import 'package:generic_app/models/BaseModel.dart';
import 'package:json_annotation/json_annotation.dart';

import 'AlertAction.dart';

part 'AlertButton.g.dart';

@JsonSerializable()
class AlertButton implements BaseModel {

  final String title;
  final AlertAction action;

  AlertButton({
    this.title,
    this.action,
  });

  static AlertButton fromJson(Map<String, dynamic> json) => _$AlertButtonFromJson(json);
  Map<String, dynamic> toJson() => _$AlertButtonToJson(this);
}

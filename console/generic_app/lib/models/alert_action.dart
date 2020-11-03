
import 'package:generic_app/models/base_model.dart';
import 'package:json_annotation/json_annotation.dart';

part 'alert_action.g.dart';

@JsonSerializable()
class AlertAction implements BaseModel {

  final String url;
  final String httpMethod;
  final Map<String, dynamic> data;
  final bool silent;
  final bool openInBrowser;

  AlertAction({
    this.url,
    this.httpMethod,
    this.data,
    this.silent,
    this.openInBrowser
  });

  static AlertAction fromJson(Map<String, dynamic> json) => _$AlertActionFromJson(json);
  @override
  Map<String, dynamic> toJson() => _$AlertActionToJson(this);
}

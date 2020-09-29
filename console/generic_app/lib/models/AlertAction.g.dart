// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'AlertAction.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AlertAction _$AlertActionFromJson(Map<String, dynamic> json) {
  return AlertAction(
    url: json['url'] as String,
    httpMethod: json['httpMethod'] as String,
    data: json['data'] as Map<String, dynamic>,
    silent: json['silent'] as bool,
    openInBrowser: json['openInBrowser'] as bool,
  );
}

Map<String, dynamic> _$AlertActionToJson(AlertAction instance) =>
    <String, dynamic>{
      'url': instance.url,
      'httpMethod': instance.httpMethod,
      'data': instance.data,
      'silent': instance.silent,
      'openInBrowser': instance.openInBrowser,
    };

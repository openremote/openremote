// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'LinkConfig.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LinkConfig _$LinkConfigFromJson(Map<String, dynamic> json) {
  return LinkConfig(
    json['displayText'] as String,
    json['pageLink'] as String,
    (json['queryParams'] as Map<String, dynamic>)?.map(
      (k, e) => MapEntry(k, e as String),
    ),
  );
}

Map<String, dynamic> _$LinkConfigToJson(LinkConfig instance) =>
    <String, dynamic>{
      'displayText': instance.displayText,
      'pageLink': instance.pageLink,
      'queryParams': instance.queryParams,
    };

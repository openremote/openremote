// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'alert_button.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AlertButton _$AlertButtonFromJson(Map<String, dynamic> json) {
  return AlertButton(
    title: json['title'] as String,
    action: json['action'] == null
        ? null
        : AlertAction.fromJson(json['action'] as Map<String, dynamic>),
  );
}

Map<String, dynamic> _$AlertButtonToJson(AlertButton instance) =>
    <String, dynamic>{
      'title': instance.title,
      'action': instance.action,
    };

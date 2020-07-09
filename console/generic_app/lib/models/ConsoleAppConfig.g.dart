// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ConsoleAppConfig.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ConsoleAppConfig _$ConsoleAppConfigFromJson(Map<String, dynamic> json) {
  return ConsoleAppConfig(
    id: json['id'] as int,
    realm: json['realm'] as String,
    url: json['url'] as String,
    menuEnabled: json['menuEnabled'] as bool,
    menuPosition: json['menuPosition'] as String,
    menuImage: json['menuImage'] as String,
    primaryColor: json['primaryColor'] as String,
    secondaryColor: json['secondaryColor'] as String,
    links: (json['links'] as List)
        ?.map((e) =>
            e == null ? null : LinkConfig.fromJson(e as Map<String, dynamic>))
        ?.toList(),
  );
}

Map<String, dynamic> _$ConsoleAppConfigToJson(ConsoleAppConfig instance) =>
    <String, dynamic>{
      'id': instance.id,
      'realm': instance.realm,
      'url': instance.url,
      'menuEnabled': instance.menuEnabled,
      'menuPosition': instance.menuPosition,
      'menuImage': instance.menuImage,
      'primaryColor': instance.primaryColor,
      'secondaryColor': instance.secondaryColor,
      'links': instance.links,
    };

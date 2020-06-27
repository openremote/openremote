import 'dart:collection';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/BaseModel.dart';

typedef ResponseParser<T extends BaseModel> = T Function(Map<String, dynamic>);

enum HttpMethod { get, post, put, patch, delete }

class ApiManager {
  String baseUrl;
  String accessToken;
  String responseBodyDataKey;
  Map<String, String> baseHeaders;

  ApiManager(this.baseUrl);

  /*********************************Private functions*******************************/

  Future<T> get<T>(List<String> pathComponents, ResponseParser responseParser,
      {Map<String, dynamic> queryParameters,
      Map<String, String> additionalHeaders}) async {
    StringBuffer urlBuffer = new StringBuffer(baseUrl);

    pathComponents
        .forEach((pathComponent) => urlBuffer.write("/" + pathComponent));

    if (queryParameters != null) {
      urlBuffer.write("?");
      List<MapEntry<String, dynamic>> params = queryParameters.entries.toList();
      for (var i = 0; i < params.length; i++) {
        if (i > 0) {
          urlBuffer.write("&");
        }
        urlBuffer.write("${params[i].key}=${params[i].value}");
      }
    }

    Map<String, String> headers = baseHeaders != null ? new HashMap.from(baseHeaders) : new HashMap();
    if (additionalHeaders != null) {
      headers.addAll(additionalHeaders);
    }

    T returnValue;

    await http.get(urlBuffer.toString(), headers: headers).then((response) {
      if (response.statusCode == 200) {
        try {
          Map<String, dynamic> json = jsonDecode(response.body);
          var jsonData = json;
          if (responseBodyDataKey != null && responseBodyDataKey != "") {
            jsonData = json[responseBodyDataKey];
          }
          if (jsonData is List) {
            throw new Exception("Expected json object");
          } else {
            returnValue = responseParser(jsonData) as T;
          }
        } catch (e) {
          print(e);
          rethrow;
        }
      } else {
        throw new Exception(
            "Response error: ${response.statusCode} - ${response.body}");
      }
    });

    return returnValue;
  }

  Future<List<T>> getAll<T>(
      List<String> pathComponents, ResponseParser responseParser,
      {Map<String, dynamic> queryParameters,
      Map<String, String> additionalHeaders}) async {
    StringBuffer urlBuffer = new StringBuffer(baseUrl);

    pathComponents
        .forEach((pathComponent) => urlBuffer.write("/" + pathComponent));

    if (queryParameters != null) {
      urlBuffer.write("?");
      List<MapEntry<String, dynamic>> params = queryParameters.entries.toList();
      for (var i = 0; i < params.length; i++) {
        if (i > 0) {
          urlBuffer.write("&");
        }
        urlBuffer.write("${params[i].key}=${params[i].value}");
      }
    }

    Map<String, String> headers = baseHeaders != null ? new HashMap.from(baseHeaders) : new HashMap();
    if (additionalHeaders != null) {
      headers.addAll(additionalHeaders);
    }

    List<T> returnValue;

    await http.get(urlBuffer.toString(), headers: headers).then((response) {
      if (response.statusCode == 200) {
        try {
          var json = jsonDecode(response.body);
          var jsonData = json;
          if (responseBodyDataKey != null && responseBodyDataKey != "") {
            jsonData = json[responseBodyDataKey];
          }
          if (jsonData is List) {
            returnValue =
                List<T>.from(jsonData.map((item) => responseParser(item)));
          } else {
            throw new Exception("Expected array in body");
          }
        } catch (e) {
          print(e);
          rethrow;
        }
      } else {
        throw new Exception(
            "Response error: ${response.statusCode} - ${response.body}");
      }
    });

    return returnValue;
  }
}

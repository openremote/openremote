import 'dart:collection';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/base_model.dart';

typedef ResponseParser<T extends BaseModel> = T Function(Map<String, dynamic>);

enum HttpMethod { get, post, put, patch, delete }

class ApiManager {
  String baseUrl;
  String accessToken;
  String responseBodyDataKey;
  Map<String, String> baseHeaders;

  ApiManager(this.baseUrl, {this.baseHeaders});

  Future<T> get<T>(List<String> pathComponents, ResponseParser responseParser,
      {Map<String, dynamic> queryParameters,
      Map<String, String> additionalHeaders}) async {

    return http.get(_prepareUrl(pathComponents, queryParameters: queryParameters), headers: _prepareHeaders(additionalHeaders)).then((response) {
      if (response.statusCode == 200) {
        try {
          final Map<String, dynamic> json = jsonDecode(response.body);
          var jsonData = json;
          if (responseBodyDataKey != null && responseBodyDataKey != "") {
            jsonData = json[responseBodyDataKey];
          }
          if (jsonData is List) {
            throw Exception("Expected json object");
          } else {
            return responseParser(jsonData) as T;
          }
        } catch (e) {
          print(e);
          rethrow;
        }
      } else {
        throw Exception(
            "Response error: ${response.statusCode} - ${response.body}");
      }
    });
  }

  Future<List<T>> getAll<T>(
      List<String> pathComponents, ResponseParser responseParser,
      {Map<String, dynamic> queryParameters,
      Map<String, String> additionalHeaders}) async {

    return http.get(_prepareUrl(pathComponents, queryParameters: queryParameters), headers: _prepareHeaders(additionalHeaders)).then((response) {
      if (response.statusCode == 200) {
        try {
          final json = jsonDecode(response.body);
          var jsonData = json;
          if (responseBodyDataKey != null && responseBodyDataKey != "") {
            jsonData = json[responseBodyDataKey];
          }
          if (jsonData is List) {
           return List<T>.from(jsonData.map((item) => responseParser(item)));
          } else {
            throw Exception("Expected array in body");
          }
        } catch (e) {
          print(e);
          rethrow;
        }
      } else {
        throw Exception(
            "Response error: ${response.statusCode} - ${response.body}");
      }
    });
  }



  Future<T> post<T extends BaseModel>({ResponseParser responseParser,
      List<String> pathComponents, T model, String rawModel,
        Map<String, String> additionalHeaders, String overrideUrl}) async {

    return http
        .post(overrideUrl ?? _prepareUrl(pathComponents), headers: _prepareHeaders(additionalHeaders), body: model?.toJson() ?? rawModel)
        .then((response) {
      if (response.statusCode == 201) {
        try {
          final json = jsonDecode(response.body);
          var jsonData = json;
          if (responseBodyDataKey != null && responseBodyDataKey != "") {
            jsonData = json[responseBodyDataKey];
          }
          if (jsonData is List) {
            throw Exception("Expected json object");
          } else {
            return responseParser(jsonData) as T;
          }
        } catch (e) {
          print(e);
          rethrow;
        }
      } else if (response.statusCode >= 200 && response.statusCode <= 299) {
        return null;
      } else {
        throw Exception(
            "Response error: ${response.statusCode} - ${response.body}");
      }
    });
  }

  Future<T> put<T extends BaseModel>({ResponseParser responseParser,
    List<String> pathComponents, T model, String rawModel,
    Map<String, String> additionalHeaders, String overrideUrl}) async {

    return http
        .put(overrideUrl ?? _prepareUrl(pathComponents), headers: _prepareHeaders(additionalHeaders), body: model?.toJson() ?? rawModel)
        .then((response) {
      if (response.statusCode == 201) {
        try {
          final json = jsonDecode(response.body);
          var jsonData = json;
          if (responseBodyDataKey != null && responseBodyDataKey != "") {
            jsonData = json[responseBodyDataKey];
          }
          if (jsonData is List) {
            throw Exception("Expected json object");
          } else {
            return responseParser(jsonData) as T;
          }
        } catch (e) {
          print(e);
          rethrow;
        }
      } else if (response.statusCode >= 200 && response.statusCode <= 299) {
        return null;
      } else {
        throw Exception(
            "Response error: ${response.statusCode} - ${response.body}");
      }
    });
  }

  String _prepareUrl(List<String> pathComponents, {Map<String, dynamic> queryParameters}) {
    final StringBuffer urlBuffer = StringBuffer(baseUrl);

    pathComponents
        .forEach((pathComponent) => urlBuffer.write("/$pathComponent"));

    if (queryParameters != null) {
      urlBuffer.write("?");
      final List<MapEntry<String, dynamic>> params = queryParameters.entries.toList();
      for (var i = 0; i < params.length; i++) {
        if (i > 0) {
          urlBuffer.write("&");
        }
        urlBuffer.write("${params[i].key}=${params[i].value}");
      }
    }
    return urlBuffer.toString();
  }

  Map<String, String> _prepareHeaders(Map<String, String> additionalHeaders) {
    final Map<String, String> headers =
    baseHeaders != null ? HashMap.from(baseHeaders) : HashMap();
    if (additionalHeaders != null) {
      headers.addAll(additionalHeaders);
    }
    return headers;
  }
}

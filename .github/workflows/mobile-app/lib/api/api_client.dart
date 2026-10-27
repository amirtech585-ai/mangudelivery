import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/session.dart';

class ApiException implements Exception {
  ApiException(this.statusCode, this.message);

  final int statusCode;
  final String message;

  @override
  String toString() => message;
}

/// All calls to the Spring Boot API. The app never talks to the database directly.
class ApiClient {
  ApiClient({required this.baseUrl});

  final String baseUrl;

  Future<dynamic> _send(String method, String path, {String? token, Object? body}) async {
    final uri = Uri.parse('$baseUrl$path');
    final headers = <String, String>{
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
    final encoded = body == null ? null : jsonEncode(body);

    http.Response response;
    try {
      response = method == 'GET'
          ? await http.get(uri, headers: headers).timeout(const Duration(seconds: 20))
          : await http.post(uri, headers: headers, body: encoded).timeout(const Duration(seconds: 20));
    } catch (_) {
      throw ApiException(0, 'Cannot reach the server. Check your connection and try again.');
    }

    dynamic data;
    if (response.bodyBytes.isNotEmpty) {
      try {
        data = jsonDecode(utf8.decode(response.bodyBytes));
      } catch (_) {
        data = null;
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return data;
    }

    final message = (data is Map && data['message'] is String && (data['message'] as String).isNotEmpty)
        ? data['message'] as String
        : 'Request failed (${response.statusCode})';
    throw ApiException(response.statusCode, message);
  }

  Future<List<dynamic>> _list(String token, String path) async =>
      await _send('GET', path, token: token) as List<dynamic>;

  Future<Map<String, dynamic>> _map(String method, String path, String token, [Object? body]) async =>
      await _send(method, path, token: token, body: body ?? <String, Object>{}) as Map<String, dynamic>;

  // ---- accounts ----

  Future<Session> login({required String email, required String password, required String role}) async {
    final data = await _send('POST', '/api/auth/login', body: {
      'email': email,
      'password': password,
      'role': role,
    });
    return Session.fromJson(data as Map<String, dynamic>);
  }

  Future<Session> register({
    required String role,
    required String email,
    required String password,
    required String fullName,
    String? phone,
    Map<String, String>? application,
  }) async {
    final data = await _send('POST', '/api/auth/register', body: {
      'role': role,
      'email': email,
      'password': password,
      'fullName': fullName,
      if (phone != null && phone.isNotEmpty) 'phone': phone,
      if (application != null) 'application': application,
    });
    return Session.fromJson(data as Map<String, dynamic>);
  }

  Future<void> logout(String token) async {
    await _send('POST', '/api/auth/logout', token: token);
  }

  Future<Map<String, dynamic>> me(String token) async {
    return await _send('GET', '/api/me', token: token) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> submitApplication(String token, Map<String, String> details) async {
    return await _send('POST', '/api/partner/application', token: token, body: details)
        as Map<String, dynamic>;
  }

  // ---- catalogue ----

  Future<List<dynamic>> buyerProducts(String token) => _list(token, '/api/buyer/products');

  Future<List<dynamic>> sellerProducts(String token) => _list(token, '/api/seller/products');

  Future<Map<String, dynamic>> createProduct(String token, Map<String, dynamic> product) =>
      _map('POST', '/api/seller/products', token, product);

  // ---- buyer orders ----

  Future<Map<String, dynamic>> placeOrder(String token, Map<String, dynamic> order) =>
      _map('POST', '/api/buyer/orders', token, order);

  Future<List<dynamic>> buyerOrders(String token) => _list(token, '/api/buyer/orders');

  Future<Map<String, dynamic>> cancelBuyerOrder(String token, int id, String reason) =>
      _map('POST', '/api/buyer/orders/$id/cancel', token, {'reason': reason});

  // ---- seller orders ----

  Future<List<dynamic>> sellerOrders(String token) => _list(token, '/api/seller/orders');

  /// action: confirm, ready, or reject (reject needs a reason).
  Future<Map<String, dynamic>> sellerAction(String token, int id, String action, {String? reason}) =>
      _map('POST', '/api/seller/orders/$id/$action', token, reason == null ? null : {'reason': reason});

  // ---- rider jobs ----

  Future<List<dynamic>> riderJobs(String token) => _list(token, '/api/rider/jobs');

  Future<List<dynamic>> riderOrders(String token) => _list(token, '/api/rider/orders');

  /// action: accept, pickup, on-the-way, release, deliver (needs pin), or fail.
  Future<Map<String, dynamic>> riderAction(String token, int id, String action, {Map<String, String>? body}) =>
      _map('POST', '/api/rider/jobs/$id/$action', token, body);
}

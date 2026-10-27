import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/session.dart';

/// Persists the current session in the platform's secure storage (Keychain / Keystore).
class SessionStore {
  SessionStore({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  static const _key = 'mangu_session';

  final FlutterSecureStorage _storage;

  Future<void> save(Session session) {
    return _storage.write(key: _key, value: jsonEncode(session.toJson()));
  }

  Future<Session?> load() async {
    final raw = await _storage.read(key: _key);
    if (raw == null) return null;
    return Session.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  }

  Future<void> clear() => _storage.delete(key: _key);
}

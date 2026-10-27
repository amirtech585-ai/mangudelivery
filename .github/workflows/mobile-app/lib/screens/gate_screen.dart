import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/session.dart';
import '../state/session_store.dart';
import 'buyer_home_screen.dart';
import 'rider_home_screen.dart';
import 'seller_home_screen.dart';
import 'status_screen.dart';
import 'welcome_screen.dart';

/// Root of the app. Decides what to show from the stored session and the
/// server's view of the account: welcome, role home, or a status screen.
class GateScreen extends StatefulWidget {
  const GateScreen({super.key, required this.api, required this.store});

  final ApiClient api;
  final SessionStore store;

  @override
  State<GateScreen> createState() => _GateScreenState();
}

class _GateScreenState extends State<GateScreen> {
  bool _loading = true;
  String? _error;
  Session? _session;
  Map<String, dynamic>? _me;

  @override
  void initState() {
    super.initState();
    _restore();
  }

  Future<void> _restore() async {
    final saved = await widget.store.load();
    if (!mounted) return;
    if (saved == null) {
      setState(() => _loading = false);
      return;
    }
    await _activate(saved);
  }

  /// Called after login or registration, and on restore.
  Future<void> _activate(Session session) async {
    await widget.store.save(session);
    if (!mounted) return;
    setState(() {
      _session = session;
      _me = null;
      _error = null;
      _loading = true;
    });
    await _loadMe();
  }

  Future<void> _loadMe() async {
    final session = _session;
    if (session == null) return;
    try {
      final me = await widget.api.me(session.token);
      if (!mounted) return;
      setState(() {
        _me = me;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      // 401/403: token expired or invalid. Force a fresh sign-in.
      if (e.statusCode == 401 || e.statusCode == 403) {
        await _logout();
        return;
      }
      setState(() {
        _error = e.message;
        _loading = false;
      });
    }
  }

  Future<void> _refresh() async {
    setState(() => _loading = true);
    await _loadMe();
  }

  /// Logs out this device. The user may then create or log into another role's account.
  Future<void> _logout() async {
    final session = _session;
    if (session != null) {
      try {
        await widget.api.logout(session.token);
      } catch (_) {
        // Ignore network errors on logout; the local session is cleared regardless.
      }
    }
    await widget.store.clear();
    if (!mounted) return;
    setState(() {
      _session = null;
      _me = null;
      _error = null;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    final session = _session;
    if (session == null) {
      return WelcomeScreen(api: widget.api, onSession: _activate);
    }

    if (_error != null) {
      return Scaffold(
        appBar: AppBar(title: const Text('ManguDelivery')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(_error!, textAlign: TextAlign.center),
                const SizedBox(height: 12),
                FilledButton(onPressed: _refresh, child: const Text('Try again')),
                TextButton(onPressed: _logout, child: const Text('Log out')),
              ],
            ),
          ),
        ),
      );
    }

    final me = _me;
    if (me == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    final status = me['status'] as String;
    final role = me['role'] as String;

    if (status != 'ACTIVE') {
      return StatusScreen(me: me, api: widget.api, token: session.token, onRefresh: _refresh, onLogout: _logout);
    }

    switch (role) {
      case 'BUYER':
        return BuyerHomeScreen(api: widget.api, token: session.token, onLogout: _logout);
      case 'SELLER':
        return SellerHomeScreen(api: widget.api, token: session.token, onLogout: _logout);
      case 'RIDER':
        return RiderHomeScreen(api: widget.api, token: session.token, onLogout: _logout);
      default:
        return StatusScreen(me: me, api: widget.api, token: session.token, onRefresh: _refresh, onLogout: _logout);
    }
  }
}

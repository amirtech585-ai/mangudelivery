import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/session.dart';
import 'login_screen.dart';
import 'register_screen.dart';

/// Shown when no account is signed in on this device.
class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key, required this.api, required this.onSession});

  final ApiClient api;
  final Future<void> Function(Session session) onSession;

  void _openRegister(BuildContext context, String role) {
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => RegisterScreen(api: api, role: role, onSession: onSession),
    ));
  }

  void _openLogin(BuildContext context) {
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => LoginScreen(api: api, onSession: onSession),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 32),
              Text('ManguDelivery', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 8),
              const Text('Shop, sell, or deliver in Masaka City.'),
              const SizedBox(height: 32),
              Text('Create an account', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              const Text(
                'Each account has one role. You can create accounts for other roles later, after logging out.',
                style: TextStyle(color: Colors.black54),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () => _openRegister(context, 'BUYER'),
                child: const Text('Buyer: shop for products'),
              ),
              const SizedBox(height: 8),
              OutlinedButton(
                onPressed: () => _openRegister(context, 'SELLER'),
                child: const Text('Seller: sell products (needs approval)'),
              ),
              const SizedBox(height: 8),
              OutlinedButton(
                onPressed: () => _openRegister(context, 'RIDER'),
                child: const Text('Rider: deliver orders (needs approval)'),
              ),
              const Spacer(),
              TextButton(
                onPressed: () => _openLogin(context),
                child: const Text('I already have an account. Log in'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

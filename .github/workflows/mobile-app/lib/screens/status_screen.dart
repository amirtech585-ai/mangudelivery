import 'package:flutter/material.dart';

import '../api/api_client.dart';
import 'resubmit_screen.dart';

/// Shown to sellers and riders who are not yet approved, and to suspended accounts.
class StatusScreen extends StatelessWidget {
  const StatusScreen({
    super.key,
    required this.me,
    required this.api,
    required this.token,
    required this.onRefresh,
    required this.onLogout,
  });

  final Map<String, dynamic> me;
  final ApiClient api;
  final String token;
  final Future<void> Function() onRefresh;
  final Future<void> Function() onLogout;

  @override
  Widget build(BuildContext context) {
    final status = me['status'] as String;
    final reason = me['rejectionReason'] as String?;

    final (title, message) = switch (status) {
      'PENDING_APPROVAL' => (
          'Application under review',
          'ManguDelivery is reviewing your application. You will be able to continue once it is approved.',
        ),
      'REJECTED' => (
          'Application not approved',
          reason ?? 'Your application was not approved. Contact ManguDelivery support for details.',
        ),
      'SUSPENDED' => (
          'Account suspended',
          'This account is suspended. Contact ManguDelivery support.',
        ),
      _ => ('Account unavailable', 'This account cannot be used right now.'),
    };

    return Scaffold(
      appBar: AppBar(title: const Text('ManguDelivery')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Icon(Icons.hourglass_top, size: 56, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 16),
            Text(title, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            if (status == 'REJECTED') ...[
              FilledButton(
                onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => ResubmitScreen(
                    api: api,
                    token: token,
                    role: me['role'] as String,
                    onSubmitted: onRefresh,
                  ),
                )),
                child: const Text('Correct and resubmit'),
              ),
              const SizedBox(height: 8),
            ],
            OutlinedButton(onPressed: onRefresh, child: const Text('Check status')),
            const SizedBox(height: 8),
            TextButton(
              onPressed: onLogout,
              child: const Text('Log out and use another account'),
            ),
          ],
        ),
      ),
    );
  }
}

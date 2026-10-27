import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'config.dart';
import 'screens/gate_screen.dart';
import 'state/session_store.dart';

void main() {
  runApp(const ManguDeliveryApp());
}

class ManguDeliveryApp extends StatelessWidget {
  const ManguDeliveryApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ManguDelivery',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF0B5D32),
        useMaterial3: true,
      ),
      home: GateScreen(
        api: ApiClient(baseUrl: AppConfig.apiBaseUrl),
        store: SessionStore(),
      ),
    );
  }
}

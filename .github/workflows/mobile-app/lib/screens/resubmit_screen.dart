import 'package:flutter/material.dart';

import '../api/api_client.dart';

/// Lets a rejected seller or rider correct the application and submit it again.
class ResubmitScreen extends StatefulWidget {
  const ResubmitScreen({
    super.key,
    required this.api,
    required this.token,
    required this.role,
    required this.onSubmitted,
  });

  final ApiClient api;
  final String token;
  final String role; // SELLER or RIDER
  final Future<void> Function() onSubmitted;

  @override
  State<ResubmitScreen> createState() => _ResubmitScreenState();
}

class _ResubmitScreenState extends State<ResubmitScreen> {
  final _formKey = GlobalKey<FormState>();
  final _ctrl = <String, TextEditingController>{};
  bool _busy = false;
  String? _error;

  bool get _isSeller => widget.role == 'SELLER';

  /// Field key, label, and number of lines for the role's application.
  List<(String, String, int)> get _fields => _isSeller
      ? const [
          ('businessName', 'Business name', 1),
          ('productCategories', 'Product categories (e.g. food, electronics)', 2),
          ('location', 'Business location in Masaka', 1),
        ]
      : const [
          ('nationalId', 'National ID number', 1),
          ('licenceNumber', 'Driving licence number', 1),
          ('vehicleType', 'Vehicle type (e.g. boda-boda)', 1),
          ('vehicleRegistration', 'Vehicle registration number', 1),
          ('location', 'Home base in Masaka', 1),
        ];

  TextEditingController _c(String key) => _ctrl.putIfAbsent(key, () => TextEditingController());

  @override
  void dispose() {
    for (final c in _ctrl.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    final details = <String, String>{
      for (final f in _fields) f.$1: _c(f.$1).text.trim(),
    };
    try {
      await widget.api.submitApplication(widget.token, details);
      if (!mounted) return;
      Navigator.of(context).pop();
      await widget.onSubmitted();
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Correct your application')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Update the details the administrator asked you to correct, then submit again.',
                style: TextStyle(color: Colors.black54),
              ),
              const SizedBox(height: 16),
              for (final f in _fields)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: TextFormField(
                    controller: _c(f.$1),
                    maxLines: f.$3,
                    decoration: InputDecoration(labelText: f.$2),
                    validator: (v) => (v == null || v.trim().isEmpty) ? 'Required' : null,
                  ),
                ),
              if (_error != null) ...[
                Text(_error!, style: const TextStyle(color: Colors.red)),
                const SizedBox(height: 12),
              ],
              FilledButton(
                onPressed: _busy ? null : _submit,
                child: _busy
                    ? const SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Submit again'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

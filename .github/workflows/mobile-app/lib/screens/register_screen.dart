import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/session.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({
    super.key,
    required this.api,
    required this.role,
    required this.onSession,
  });

  final ApiClient api;
  final String role; // BUYER, SELLER or RIDER
  final Future<void> Function(Session session) onSession;

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _ctrl = <String, TextEditingController>{};
  bool _busy = false;
  String? _error;

  static const _roleLabels = {'BUYER': 'Buyer', 'SELLER': 'Seller', 'RIDER': 'Rider'};

  bool get _isSeller => widget.role == 'SELLER';
  bool get _isRider => widget.role == 'RIDER';

  TextEditingController _c(String key) => _ctrl.putIfAbsent(key, () => TextEditingController());

  String _t(String key) => _c(key).text.trim();

  @override
  void dispose() {
    for (final c in _ctrl.values) {
      c.dispose();
    }
    super.dispose();
  }

  /// Seller or rider details. Null for buyers.
  Map<String, String>? _applicationPayload() {
    if (_isSeller) {
      return {
        'businessName': _t('businessName'),
        'productCategories': _t('productCategories'),
        'location': _t('location'),
      };
    }
    if (_isRider) {
      return {
        'nationalId': _t('nationalId'),
        'licenceNumber': _t('licenceNumber'),
        'vehicleType': _t('vehicleType'),
        'vehicleRegistration': _t('vehicleRegistration'),
        'location': _t('location'),
      };
    }
    return null;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final session = await widget.api.register(
        role: widget.role,
        email: _t('email'),
        password: _c('password').text,
        fullName: _t('fullName'),
        phone: _t('phone'),
        application: _applicationPayload(),
      );
      await widget.onSession(session);
      if (!mounted) return;
      Navigator.of(context).popUntil((route) => route.isFirst);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _field(
    String key,
    String label, {
    bool required = true,
    bool obscure = false,
    TextInputType? keyboard,
    int maxLines = 1,
    String? Function(String?)? validator,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextFormField(
        controller: _c(key),
        obscureText: obscure,
        keyboardType: keyboard,
        maxLines: maxLines,
        decoration: InputDecoration(labelText: label),
        validator: validator ??
            (v) => (required && (v == null || v.trim().isEmpty)) ? 'Required' : null,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final label = _roleLabels[widget.role] ?? widget.role;

    return Scaffold(
      appBar: AppBar(title: Text('Create $label account')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _field('fullName', 'Full name'),
              _field('email', 'Email', keyboard: TextInputType.emailAddress),
              _field('phone', 'Phone (optional)', required: false, keyboard: TextInputType.phone),
              _field(
                'password',
                'Password (at least 8 characters)',
                obscure: true,
                validator: (v) => (v == null || v.length < 8) ? 'At least 8 characters' : null,
              ),
              if (_isSeller) ...[
                const Divider(height: 32),
                const Text('Business application', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                _field('businessName', 'Business name'),
                _field('productCategories', 'Product categories (e.g. food, electronics)', maxLines: 2),
                _field('location', 'Business location in Masaka'),
              ],
              if (_isRider) ...[
                const Divider(height: 32),
                const Text('Rider application', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                _field('nationalId', 'National ID number'),
                _field('licenceNumber', 'Driving licence number'),
                _field('vehicleType', 'Vehicle type (e.g. boda-boda)'),
                _field('vehicleRegistration', 'Vehicle registration number'),
                _field('location', 'Home base in Masaka'),
              ],
              if (_isSeller || _isRider)
                const Padding(
                  padding: EdgeInsets.only(bottom: 12),
                  child: Text(
                    'Your application will be reviewed by ManguDelivery before you can start.',
                    style: TextStyle(color: Colors.black54),
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
                    : Text('Create $label account'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

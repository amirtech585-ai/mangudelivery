import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/order_status.dart';
import '../state/cart.dart';

/// Reviews the cart, takes the delivery address, and places a cash-on-delivery order.
/// Fees are calculated by the server, so the total shown here is the subtotal only.
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key, required this.api, required this.token, required this.cart});

  final ApiClient api;
  final String token;
  final Cart cart;

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _formKey = GlobalKey<FormState>();
  final _address = TextEditingController();
  final _note = TextEditingController();
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _address.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _placeOrder() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final order = await widget.api.placeOrder(widget.token, {
        'items': [for (final l in widget.cart.lines) {'productId': l.id, 'quantity': l.quantity}],
        'paymentMethod': 'CASH',
        'deliveryAddress': _address.text.trim(),
        if (_note.text.trim().isNotEmpty) 'deliveryNote': _note.text.trim(),
      });
      widget.cart.clear();
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (ctx) => AlertDialog(
          title: const Text('Order placed'),
          content: Text(
            'Your delivery PIN is ${order['deliveryPin']}.\n\n'
            'Give it to the rider only when the order arrives. '
            'Pay ${formatUgx((order['totalUgx'] as num))} in cash on delivery.\n\n'
            'You can see the PIN again under My orders.',
          ),
          actions: [FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('OK'))],
        ),
      );
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final lines = widget.cart.lines;
    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('From ${widget.cart.sellerName ?? ''}', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              for (final l in lines)
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(l.name),
                  subtitle: Text('${formatUgx(l.priceUgx)} each'),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.remove_circle_outline),
                        onPressed: () => setState(() => widget.cart.setQuantity(l.id, l.quantity - 1)),
                      ),
                      Text('${l.quantity}'),
                      IconButton(
                        icon: const Icon(Icons.add_circle_outline),
                        onPressed: l.quantity < l.stock
                            ? () => setState(() => widget.cart.setQuantity(l.id, l.quantity + 1))
                            : null,
                      ),
                    ],
                  ),
                ),
              const Divider(height: 32),
              Text('Subtotal ${formatUgx(widget.cart.subtotal)}', style: const TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 4),
              const Text(
                'Delivery and service fees are added when the order is placed.',
                style: TextStyle(color: Colors.black54),
              ),
              const SizedBox(height: 20),
              TextFormField(
                controller: _address,
                maxLength: 300,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Delivery address in Masaka'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Enter where the rider should deliver' : null,
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _note,
                maxLength: 300,
                decoration: const InputDecoration(labelText: 'Note for the rider (optional)'),
              ),
              const SizedBox(height: 8),
              const Text('Payment: cash on delivery'),
              if (_error != null) ...[
                const SizedBox(height: 12),
                Text(_error!, style: const TextStyle(color: Colors.red)),
              ],
              const SizedBox(height: 20),
              FilledButton(
                onPressed: (_busy || lines.isEmpty) ? null : _placeOrder,
                child: _busy
                    ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Place order'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

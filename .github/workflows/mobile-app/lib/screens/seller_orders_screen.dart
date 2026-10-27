import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../widgets/ask_reason.dart';
import '../widgets/order_tile.dart';

/// Incoming orders. Accept or reject while waiting; mark ready once accepted.
class SellerOrdersScreen extends StatefulWidget {
  const SellerOrdersScreen({super.key, required this.api, required this.token});

  final ApiClient api;
  final String token;

  @override
  State<SellerOrdersScreen> createState() => _SellerOrdersScreenState();
}

class _SellerOrdersScreenState extends State<SellerOrdersScreen> {
  late Future<List<dynamic>> _orders;

  @override
  void initState() {
    super.initState();
    _orders = widget.api.sellerOrders(widget.token);
  }

  void _reload() {
    setState(() => _orders = widget.api.sellerOrders(widget.token));
  }

  Future<void> _run(Future<void> Function() call) async {
    try {
      await call();
      _reload();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Orders'),
        actions: [IconButton(onPressed: _reload, icon: const Icon(Icons.refresh), tooltip: 'Refresh')],
      ),
      body: RefreshIndicator(
        onRefresh: () async => _reload(),
        child: FutureBuilder<List<dynamic>>(
          future: _orders,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return ListView(children: [Padding(
                padding: const EdgeInsets.all(24),
                child: Text(snapshot.error.toString(), textAlign: TextAlign.center),
              )]);
            }
            final items = snapshot.data ?? const [];
            if (items.isEmpty) {
              return ListView(children: const [Padding(
                padding: EdgeInsets.all(24),
                child: Text('No orders yet.', textAlign: TextAlign.center),
              )]);
            }
            return ListView(
              padding: const EdgeInsets.symmetric(vertical: 8),
              children: [
                for (final o in items)
                  OrderTile(
                    order: o as Map<String, dynamic>,
                    actions: _actionsFor(o),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }

  List<Widget> _actionsFor(Map<dynamic, dynamic> o) {
    final id = (o['id'] as num).toInt();
    switch (o['status']) {
      case 'PAID':
        return [
          FilledButton(
            onPressed: () => _run(() => widget.api.sellerAction(widget.token, id, 'confirm')),
            child: const Text('Accept order'),
          ),
          OutlinedButton(
            onPressed: () async {
              final reason = await askReason(context, title: 'Reject order', hint: 'Why can you not fulfil it?');
              if (reason == null) return;
              await _run(() => widget.api.sellerAction(widget.token, id, 'reject', reason: reason));
            },
            child: const Text('Reject'),
          ),
        ];
      case 'SELLER_CONFIRMED':
        return [
          FilledButton(
            onPressed: () => _run(() => widget.api.sellerAction(widget.token, id, 'ready')),
            child: const Text('Mark ready for pickup'),
          ),
        ];
      default:
        return const [];
    }
  }
}

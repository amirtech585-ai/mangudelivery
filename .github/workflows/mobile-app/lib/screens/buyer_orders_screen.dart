import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../widgets/ask_reason.dart';
import '../widgets/order_tile.dart';

const _buyerCancellable = {'PAID', 'SELLER_CONFIRMED', 'READY_FOR_PICKUP'};

class BuyerOrdersScreen extends StatefulWidget {
  const BuyerOrdersScreen({super.key, required this.api, required this.token});

  final ApiClient api;
  final String token;

  @override
  State<BuyerOrdersScreen> createState() => _BuyerOrdersScreenState();
}

class _BuyerOrdersScreenState extends State<BuyerOrdersScreen> {
  late Future<List<dynamic>> _orders;

  @override
  void initState() {
    super.initState();
    _orders = widget.api.buyerOrders(widget.token);
  }

  void _reload() {
    setState(() => _orders = widget.api.buyerOrders(widget.token));
  }

  Future<void> _cancel(int id) async {
    final reason = await askReason(context, title: 'Cancel order', hint: 'Why are you cancelling?');
    if (reason == null) return;
    try {
      await widget.api.cancelBuyerOrder(widget.token, id, reason);
      _reload();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My orders'),
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
                child: Text('You have no orders yet.', textAlign: TextAlign.center),
              )]);
            }
            return ListView(
              padding: const EdgeInsets.symmetric(vertical: 8),
              children: [
                for (final o in items)
                  OrderTile(
                    order: o as Map<String, dynamic>,
                    actions: [
                      if (_buyerCancellable.contains(o['status']))
                        OutlinedButton(
                          onPressed: () => _cancel((o['id'] as num).toInt()),
                          child: const Text('Cancel order'),
                        ),
                    ],
                  ),
              ],
            );
          },
        ),
      ),
    );
  }
}

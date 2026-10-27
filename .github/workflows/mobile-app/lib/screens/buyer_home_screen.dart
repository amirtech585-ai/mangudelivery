import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/order_status.dart';
import '../state/cart.dart';
import 'buyer_orders_screen.dart';
import 'checkout_screen.dart';

class BuyerHomeScreen extends StatefulWidget {
  const BuyerHomeScreen({
    super.key,
    required this.api,
    required this.token,
    required this.onLogout,
  });

  final ApiClient api;
  final String token;
  final Future<void> Function() onLogout;

  @override
  State<BuyerHomeScreen> createState() => _BuyerHomeScreenState();
}

class _BuyerHomeScreenState extends State<BuyerHomeScreen> {
  late Future<List<dynamic>> _products;
  final Cart _cart = Cart();

  @override
  void initState() {
    super.initState();
    _products = widget.api.buyerProducts(widget.token);
  }

  @override
  void dispose() {
    _cart.dispose();
    super.dispose();
  }

  void _reload() {
    setState(() {
      _products = widget.api.buyerProducts(widget.token);
    });
  }

  void _add(Map<String, dynamic> product) {
    final problem = _cart.add(product);
    if (problem != null && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(problem)));
    }
  }

  Future<void> _checkout() async {
    final placed = await Navigator.of(context).push<bool>(MaterialPageRoute(
      builder: (_) => CheckoutScreen(api: widget.api, token: widget.token, cart: _cart),
    ));
    if (placed == true) _reload();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: _cart,
      builder: (context, _) => Scaffold(
        appBar: AppBar(
          title: const Text('Shop'),
          actions: [
            IconButton(
              tooltip: 'My orders',
              icon: const Icon(Icons.receipt_long),
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => BuyerOrdersScreen(api: widget.api, token: widget.token),
              )),
            ),
            IconButton(onPressed: _reload, icon: const Icon(Icons.refresh), tooltip: 'Refresh'),
            IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout), tooltip: 'Log out'),
          ],
        ),
        floatingActionButton: _cart.count == 0
            ? null
            : FloatingActionButton.extended(
                onPressed: _checkout,
                icon: Badge(label: Text('${_cart.count}'), child: const Icon(Icons.shopping_cart)),
                label: Text('Checkout ${formatUgx(_cart.subtotal)}'),
              ),
        body: FutureBuilder<List<dynamic>>(
          future: _products,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return Center(child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(snapshot.error.toString(), textAlign: TextAlign.center),
              ));
            }
            final items = snapshot.data ?? const [];
            if (items.isEmpty) {
              return const Center(child: Text('No products available yet.'));
            }
            return ListView.separated(
              padding: const EdgeInsets.only(bottom: 96),
              itemCount: items.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, i) {
                final p = items[i] as Map<String, dynamic>;
                final stock = (p['stock'] as num).toInt();
                return ListTile(
                  title: Text(p['name'] as String),
                  subtitle: Text('${p['sellerName']} • ${p['category']} • $stock in stock'),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(formatUgx((p['priceUgx'] as num))),
                      IconButton(
                        tooltip: 'Add to cart',
                        icon: const Icon(Icons.add_circle_outline),
                        onPressed: stock > 0 ? () => _add(p) : null,
                      ),
                    ],
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}

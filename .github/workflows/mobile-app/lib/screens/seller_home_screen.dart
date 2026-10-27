import 'package:flutter/material.dart';

import '../api/api_client.dart';
import 'seller_orders_screen.dart';

class SellerHomeScreen extends StatefulWidget {
  const SellerHomeScreen({
    super.key,
    required this.api,
    required this.token,
    required this.onLogout,
  });

  final ApiClient api;
  final String token;
  final Future<void> Function() onLogout;

  @override
  State<SellerHomeScreen> createState() => _SellerHomeScreenState();
}

class _SellerHomeScreenState extends State<SellerHomeScreen> {
  late Future<List<dynamic>> _products;

  @override
  void initState() {
    super.initState();
    _products = widget.api.sellerProducts(widget.token);
  }

  void _reload() {
    setState(() {
      _products = widget.api.sellerProducts(widget.token);
    });
  }

  void _snack(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  Future<void> _addProduct() async {
    final name = TextEditingController();
    final category = TextEditingController();
    final price = TextEditingController();
    final stock = TextEditingController();

    final save = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('New product'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: name, decoration: const InputDecoration(labelText: 'Name')),
              TextField(controller: category, decoration: const InputDecoration(labelText: 'Category')),
              TextField(
                controller: price,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Price (UGX)'),
              ),
              TextField(
                controller: stock,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Stock'),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Save')),
        ],
      ),
    );

    if (save == true) {
      final priceValue = int.tryParse(price.text.trim());
      final stockValue = int.tryParse(stock.text.trim());
      if (name.text.trim().isEmpty || category.text.trim().isEmpty) {
        _snack('Enter a name and a category.');
      } else if (priceValue == null || priceValue < 1) {
        _snack('Enter a price of at least UGX 1.');
      } else if (stockValue == null || stockValue < 0) {
        _snack('Enter a stock quantity of 0 or more.');
      } else {
        try {
          await widget.api.createProduct(widget.token, {
            'name': name.text.trim(),
            'category': category.text.trim(),
            'priceUgx': priceValue,
            'stock': stockValue,
          });
          _reload();
        } on ApiException catch (e) {
          _snack(e.message);
        }
      }
    }

    name.dispose();
    category.dispose();
    price.dispose();
    stock.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My store'),
        actions: [
          IconButton(
            tooltip: 'Orders',
            icon: const Icon(Icons.receipt_long),
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => SellerOrdersScreen(api: widget.api, token: widget.token),
            )),
          ),
          IconButton(onPressed: _reload, icon: const Icon(Icons.refresh), tooltip: 'Refresh'),
          IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout), tooltip: 'Log out'),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _addProduct,
        icon: const Icon(Icons.add),
        label: const Text('Add product'),
      ),
      body: FutureBuilder<List<dynamic>>(
        future: _products,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text(snapshot.error.toString()));
          }
          final items = snapshot.data ?? const [];
          if (items.isEmpty) {
            return const Center(child: Text('No products yet. Tap "Add product" to start selling.'));
          }
          return ListView.separated(
            itemCount: items.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, i) {
              final p = items[i] as Map<String, dynamic>;
              return ListTile(
                title: Text(p['name'] as String),
                subtitle: Text('${p['category']} • stock ${p['stock']}'),
                trailing: Text('UGX ${p['priceUgx']}'),
              );
            },
          );
        },
      ),
    );
  }
}

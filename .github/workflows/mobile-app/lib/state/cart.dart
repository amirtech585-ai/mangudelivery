import 'package:flutter/foundation.dart';

class CartLine {
  CartLine(this.product, this.quantity);

  final Map<String, dynamic> product;
  int quantity;

  int get id => (product['id'] as num).toInt();
  int get priceUgx => (product['priceUgx'] as num).toInt();
  int get stock => (product['stock'] as num).toInt();
  String get name => product['name'] as String;
  String get sellerName => product['sellerName'] as String? ?? '';
}

/// The cart lives on the device until checkout. It is one seller at a time, matching the server rule.
class Cart extends ChangeNotifier {
  final Map<int, CartLine> _lines = {};

  List<CartLine> get lines => _lines.values.toList();
  int get count => _lines.values.fold(0, (sum, l) => sum + l.quantity);
  int get subtotal => _lines.values.fold(0, (sum, l) => sum + l.quantity * l.priceUgx);
  String? get sellerName => _lines.isEmpty ? null : _lines.values.first.sellerName;

  /// Adds one unit. Returns a message for the user if the item cannot be added, otherwise null.
  String? add(Map<String, dynamic> product) {
    final seller = product['sellerName'] as String? ?? '';
    if (_lines.isNotEmpty && seller != sellerName) {
      return 'Your cart has items from $sellerName. Checkout or clear it first.';
    }
    final id = (product['id'] as num).toInt();
    final stock = (product['stock'] as num).toInt();
    final existing = _lines[id];
    final current = existing?.quantity ?? 0;
    if (current + 1 > stock) {
      return 'Only $stock left of ${product['name']}.';
    }
    if (existing == null) {
      _lines[id] = CartLine(product, 1);
    } else {
      existing.quantity = current + 1;
    }
    notifyListeners();
    return null;
  }

  void setQuantity(int id, int quantity) {
    final line = _lines[id];
    if (line == null) return;
    if (quantity <= 0) {
      _lines.remove(id);
    } else {
      line.quantity = quantity > line.stock ? line.stock : quantity;
    }
    notifyListeners();
  }

  void clear() {
    _lines.clear();
    notifyListeners();
  }
}

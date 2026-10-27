import 'package:flutter/material.dart';

import '../models/order_status.dart';

/// Summary of one order. Actions appear at the bottom and depend on the screen.
class OrderTile extends StatelessWidget {
  const OrderTile({super.key, required this.order, this.actions = const []});

  final Map<String, dynamic> order;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final status = order['status'] as String;
    final items = (order['items'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
    final total = (order['totalUgx'] as num).toInt();
    final pin = order['deliveryPin'] as String?;
    final address = order['deliveryAddress'] as String?;
    final reason = order['reason'] as String?;
    final theme = Theme.of(context);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text('Order #${order['id']}', style: theme.textTheme.titleMedium)),
                Text(statusLabel(status), style: const TextStyle(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 6),
            for (final i in items) Text('${i['quantity']} x ${i['productName']}'),
            const SizedBox(height: 4),
            Text('Total ${formatUgx(total)} (cash on delivery)'),
            if (order['buyerName'] != null) Text('Buyer: ${order['buyerName']}'),
            if (order['riderName'] != null) Text('Rider: ${order['riderName']}'),
            if (address != null) Text('Deliver to: $address'),
            if (order['deliveryNote'] != null) Text('Note: ${order['deliveryNote']}'),
            if (pin != null) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: theme.colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  'Delivery PIN: $pin\nGive this to the rider only when your order arrives.',
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
              ),
            ],
            if (reason != null && reason.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text('Reason: $reason', style: const TextStyle(color: Colors.black54)),
            ],
            if (actions.isNotEmpty) ...[
              const SizedBox(height: 8),
              Wrap(spacing: 8, runSpacing: 8, children: actions),
            ],
          ],
        ),
      ),
    );
  }
}

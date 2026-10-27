/// Labels shown to users for each order status, and money formatting.
String statusLabel(String status) {
  switch (status) {
    case 'PAID':
      return 'Waiting for seller';
    case 'SELLER_CONFIRMED':
      return 'Seller accepted';
    case 'READY_FOR_PICKUP':
      return 'Ready for pickup';
    case 'RIDER_ASSIGNED':
      return 'Rider assigned';
    case 'PICKED_UP':
      return 'Picked up';
    case 'ON_THE_WAY':
      return 'On the way';
    case 'DELIVERED':
      return 'Delivered';
    case 'COMPLETED':
      return 'Completed';
    case 'SELLER_REJECTED':
      return 'Rejected by seller';
    case 'BUYER_CANCELLED':
      return 'Cancelled';
    case 'ADMIN_CANCELLED':
      return 'Cancelled by ManguDelivery';
    case 'DELIVERY_FAILED':
      return 'Delivery failed';
    default:
      return status;
  }
}

/// Formats whole shillings, for example 15000 becomes "UGX 15,000".
String formatUgx(num value) {
  final digits = value.toInt().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) buffer.write(',');
    buffer.write(digits[i]);
  }
  return 'UGX $buffer';
}

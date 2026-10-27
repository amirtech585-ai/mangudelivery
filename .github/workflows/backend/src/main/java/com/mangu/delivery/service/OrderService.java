package com.mangu.delivery.service;

import com.mangu.delivery.domain.*;
import com.mangu.delivery.dto.OrderDtos.CartLine;
import com.mangu.delivery.dto.OrderDtos.OrderItemView;
import com.mangu.delivery.dto.OrderDtos.OrderView;
import com.mangu.delivery.dto.OrderDtos.PlaceOrderRequest;
import com.mangu.delivery.repository.LedgerRepository;
import com.mangu.delivery.repository.OrderEventRepository;
import com.mangu.delivery.repository.OrderRepository;
import com.mangu.delivery.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * The order lifecycle (Documentation Sections 5, 8, 10 and 11). Every state change goes through
 * {@link #transition}, which enforces the allowed-transition table and writes the history.
 * Money amounts are whole UGX.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PAID, EnumSet.of(OrderStatus.SELLER_CONFIRMED, OrderStatus.SELLER_REJECTED,
                    OrderStatus.BUYER_CANCELLED, OrderStatus.ADMIN_CANCELLED),
            OrderStatus.SELLER_CONFIRMED, EnumSet.of(OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.BUYER_CANCELLED, OrderStatus.ADMIN_CANCELLED),
            OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.RIDER_ASSIGNED,
                    OrderStatus.BUYER_CANCELLED, OrderStatus.ADMIN_CANCELLED),
            OrderStatus.RIDER_ASSIGNED, EnumSet.of(OrderStatus.PICKED_UP,
                    OrderStatus.READY_FOR_PICKUP, OrderStatus.ADMIN_CANCELLED),
            OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.ON_THE_WAY),
            OrderStatus.ON_THE_WAY, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.COMPLETED),
            OrderStatus.DELIVERY_FAILED, EnumSet.of(OrderStatus.ADMIN_CANCELLED));

    /** States in which the seller's stock is returned to the listing. */
    private static final Set<OrderStatus> RESTOCK_ON = EnumSet.of(
            OrderStatus.SELLER_REJECTED, OrderStatus.BUYER_CANCELLED, OrderStatus.ADMIN_CANCELLED);

    /** States in which the buyer may see the delivery PIN. */
    private static final Set<OrderStatus> PIN_VISIBLE_TO_BUYER = EnumSet.of(
            OrderStatus.PAID, OrderStatus.SELLER_CONFIRMED, OrderStatus.READY_FOR_PICKUP,
            OrderStatus.RIDER_ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.ON_THE_WAY);

    private static final int MAX_PIN_ATTEMPTS = 5;

    // Platform settings (Documentation Section 9.3). These will move to the admin settings table.
    @Value("${app.pricing.delivery-fee-ugx:3000}")
    private long deliveryFeeUgx;

    @Value("${app.pricing.service-fee-ugx:1000}")
    private long serviceFeeUgx;

    @Value("${app.pricing.commission-percent:5}")
    private long commissionPercent;

    private final OrderRepository orders;
    private final ProductRepository products;
    private final OrderEventRepository events;
    private final LedgerRepository ledger;
    private final AuthService auth;
    private final SecureRandom random = new SecureRandom();

    private enum Audience { BUYER, SELLER, RIDER, ADMIN }

    // ---------- buyer ----------

    @Transactional
    public OrderView placeOrder(Long buyerId, PlaceOrderRequest req) {
        User buyer = auth.requireActive(buyerId);
        if (req.paymentMethod() != PaymentMethod.CASH) {
            throw badRequest("Only cash on delivery is available at the moment");
        }

        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (CartLine line : req.items()) {
            wanted.merge(line.productId(), line.quantity(), Integer::sum);
        }

        Order order = new Order();
        order.setBuyer(buyer);
        User seller = null;
        long subtotal = 0;
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product p = products.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> notFound("A product in your cart no longer exists"));
            if (!p.isActive() || p.getSeller().getStatus() != AccountStatus.ACTIVE) {
                throw conflict(p.getName() + " is no longer available");
            }
            if (seller == null) {
                seller = p.getSeller();
            } else if (!seller.getId().equals(p.getSeller().getId())) {
                throw badRequest("Checkout one seller at a time. Remove items from other sellers first.");
            }
            int qty = entry.getValue();
            if (qty > p.getStock()) {
                throw conflict("Only " + p.getStock() + " left of " + p.getName());
            }
            p.setStock(p.getStock() - qty);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setProductName(p.getName());
            item.setUnitPriceUgx(p.getPriceUgx());
            item.setQuantity(qty);
            item.setLineTotalUgx(p.getPriceUgx() * qty);
            order.getItems().add(item);
            subtotal += item.getLineTotalUgx();
        }

        order.setSeller(seller);
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setSubtotalUgx(subtotal);
        order.setCommissionUgx(subtotal * commissionPercent / 100);
        order.setDeliveryFeeUgx(deliveryFeeUgx);
        order.setServiceFeeUgx(serviceFeeUgx);
        order.setTotalUgx(subtotal + deliveryFeeUgx + serviceFeeUgx);
        order.setDeliveryAddress(req.deliveryAddress().trim());
        order.setDeliveryNote(req.deliveryNote() == null || req.deliveryNote().isBlank() ? null : req.deliveryNote().trim());
        order.setDeliveryPin(String.format("%04d", random.nextInt(10000)));
        orders.save(order);
        record(order, null, OrderStatus.PAID, buyerId, null);
        return view(order, Audience.BUYER);
    }

    @Transactional(readOnly = true)
    public List<OrderView> buyerOrders(Long buyerId) {
        auth.requireActive(buyerId);
        return orders.findByBuyerIdOrderByIdDesc(buyerId).stream().map(o -> view(o, Audience.BUYER)).toList();
    }

    @Transactional(readOnly = true)
    public OrderView buyerOrder(Long buyerId, Long orderId) {
        Order o = requireOrder(orderId);
        requireOwner(o.getBuyer().getId(), buyerId);
        return view(o, Audience.BUYER);
    }

    @Transactional
    public OrderView buyerCancel(Long buyerId, Long orderId, String reason) {
        auth.requireActive(buyerId);
        Order o = requireOrderForUpdate(orderId);
        requireOwner(o.getBuyer().getId(), buyerId);
        transition(o, OrderStatus.BUYER_CANCELLED, buyerId, reason);
        restock(o);
        return view(o, Audience.BUYER);
    }

    // ---------- seller ----------

    @Transactional(readOnly = true)
    public List<OrderView> sellerOrders(Long sellerId) {
        auth.requireActive(sellerId);
        return orders.findBySellerIdOrderByIdDesc(sellerId).stream().map(o -> view(o, Audience.SELLER)).toList();
    }

    @Transactional
    public OrderView sellerConfirm(Long sellerId, Long orderId) {
        Order o = sellerOrder(sellerId, orderId);
        transition(o, OrderStatus.SELLER_CONFIRMED, sellerId, null);
        return view(o, Audience.SELLER);
    }

    @Transactional
    public OrderView sellerReject(Long sellerId, Long orderId, String reason) {
        Order o = sellerOrder(sellerId, orderId);
        transition(o, OrderStatus.SELLER_REJECTED, sellerId, reason);
        restock(o);
        return view(o, Audience.SELLER);
    }

    @Transactional
    public OrderView sellerReady(Long sellerId, Long orderId) {
        Order o = sellerOrder(sellerId, orderId);
        transition(o, OrderStatus.READY_FOR_PICKUP, sellerId, null);
        return view(o, Audience.SELLER);
    }

    // ---------- rider ----------

    /** Orders ready for pickup that no rider has taken yet. Zones are applied once service areas exist. */
    @Transactional(readOnly = true)
    public List<OrderView> availableJobs(Long riderId) {
        auth.requireActive(riderId);
        return orders.findByStatusAndRiderIsNullOrderByIdAsc(OrderStatus.READY_FOR_PICKUP).stream()
                .map(o -> view(o, Audience.RIDER)).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderView> riderOrders(Long riderId) {
        auth.requireActive(riderId);
        return orders.findByRiderIdOrderByIdDesc(riderId).stream().map(o -> view(o, Audience.RIDER)).toList();
    }

    @Transactional
    public OrderView riderAccept(Long riderId, Long orderId) {
        User rider = auth.requireActive(riderId);
        Order o = requireOrderForUpdate(orderId);
        if (o.getRider() != null || o.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw conflict("This job has already been taken or is no longer available");
        }
        o.setRider(rider);
        transition(o, OrderStatus.RIDER_ASSIGNED, riderId, null);
        return view(o, Audience.RIDER);
    }

    /** The rider gives the job back before pickup. The order returns to the job list. */
    @Transactional
    public OrderView riderRelease(Long riderId, Long orderId, String reason) {
        Order o = riderOrder(riderId, orderId);
        transition(o, OrderStatus.READY_FOR_PICKUP, riderId, "Released by rider: " + reason);
        o.setRider(null);
        return view(o, Audience.RIDER);
    }

    @Transactional
    public OrderView riderPickup(Long riderId, Long orderId) {
        Order o = riderOrder(riderId, orderId);
        transition(o, OrderStatus.PICKED_UP, riderId, null);
        return view(o, Audience.RIDER);
    }

    @Transactional
    public OrderView riderOnTheWay(Long riderId, Long orderId) {
        Order o = riderOrder(riderId, orderId);
        transition(o, OrderStatus.ON_THE_WAY, riderId, null);
        return view(o, Audience.RIDER);
    }

    /**
     * Confirms delivery with the buyer's PIN. A wrong PIN is counted and saved even though the
     * request fails, so the attempt count survives the error response.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public OrderView riderDeliver(Long riderId, Long orderId, String pin) {
        auth.requireActive(riderId);
        Order o = riderOrder(riderId, orderId);
        if (o.getStatus() != OrderStatus.ON_THE_WAY) {
            throw conflict("The order must be on the way before it can be delivered (current: " + o.getStatus() + ")");
        }
        if (o.getPinAttempts() >= MAX_PIN_ATTEMPTS) {
            throw forbidden("This delivery is locked after too many wrong PINs. Contact ManguDelivery support.");
        }
        if (!o.getDeliveryPin().equals(pin)) {
            o.setPinAttempts(o.getPinAttempts() + 1);
            int left = MAX_PIN_ATTEMPTS - o.getPinAttempts();
            throw badRequest(left > 0
                    ? "Incorrect PIN. " + left + " attempts left."
                    : "Incorrect PIN. This delivery is now locked. Contact ManguDelivery support.");
        }
        transition(o, OrderStatus.DELIVERED, riderId, null);
        transition(o, OrderStatus.COMPLETED, riderId, null);
        o.setCashCollected(o.getPaymentMethod() == PaymentMethod.CASH);
        writeLedger(o);
        return view(o, Audience.RIDER);
    }

    @Transactional
    public OrderView riderFail(Long riderId, Long orderId, String reason) {
        Order o = riderOrder(riderId, orderId);
        transition(o, OrderStatus.DELIVERY_FAILED, riderId, reason);
        return view(o, Audience.RIDER);
    }

    // ---------- admin ----------

    @Transactional(readOnly = true)
    public List<OrderView> adminOrders() {
        return orders.findTop200ByOrderByIdDesc().stream().map(o -> view(o, Audience.ADMIN)).toList();
    }

    /** Administrator cancellation. Releases the rider and returns stock. Requires a reason. */
    @Transactional
    public OrderView adminCancel(Long adminId, Long orderId, String reason) {
        Order o = requireOrderForUpdate(orderId);
        transition(o, OrderStatus.ADMIN_CANCELLED, adminId, reason);
        o.setRider(null);
        restock(o);
        return view(o, Audience.ADMIN);
    }

    // ---------- helpers ----------

    private void transition(Order o, OrderStatus to, Long actorId, String reason) {
        OrderStatus from = o.getStatus();
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw conflict("This order is " + from + " and cannot be changed to " + to + " yet");
        }
        o.setStatus(to);
        o.setUpdatedAt(Instant.now());
        if (reason != null) {
            o.setReason(reason);
        }
        record(o, from, to, actorId, reason);
    }

    private void record(Order o, OrderStatus from, OrderStatus to, Long actorId, String reason) {
        OrderEvent e = new OrderEvent();
        e.setOrder(o);
        e.setFromStatus(from);
        e.setToStatus(to);
        e.setActorId(actorId);
        e.setReason(reason);
        events.save(e);
    }

    /** Returns the quantities of an order to stock. Called only when the order leaves the flow. */
    private void restock(Order o) {
        if (!RESTOCK_ON.contains(o.getStatus())) {
            return;
        }
        for (OrderItem item : o.getItems()) {
            Product p = products.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> notFound("Product not found"));
            p.setStock(p.getStock() + item.getQuantity());
        }
    }

    private void writeLedger(Order o) {
        long commission = o.getCommissionUgx();
        ledger(o, "SELLER", o.getSeller().getId(), "SELLER_EARNING", o.getSubtotalUgx() - commission);
        ledger(o, "RIDER", o.getRider().getId(), "RIDER_EARNING", o.getDeliveryFeeUgx());
        ledger(o, "PLATFORM", null, "COMMISSION", commission);
        ledger(o, "PLATFORM", null, "SERVICE_FEE", o.getServiceFeeUgx());
    }

    private void ledger(Order o, String party, Long userId, String type, long amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setOrder(o);
        entry.setParty(party);
        entry.setUserId(userId);
        entry.setEntryType(type);
        entry.setAmountUgx(amount);
        ledger.save(entry);
    }

    private Order requireOrder(Long orderId) {
        return orders.findById(orderId).orElseThrow(() -> notFound("Order not found"));
    }

    private Order requireOrderForUpdate(Long orderId) {
        return orders.findByIdForUpdate(orderId).orElseThrow(() -> notFound("Order not found"));
    }

    /** Sellers see only their own orders. Other orders look like they do not exist. */
    private Order sellerOrder(Long sellerId, Long orderId) {
        auth.requireActive(sellerId);
        Order o = requireOrderForUpdate(orderId);
        requireOwner(o.getSeller().getId(), sellerId);
        return o;
    }

    private Order riderOrder(Long riderId, Long orderId) {
        auth.requireActive(riderId);
        Order o = requireOrderForUpdate(orderId);
        if (o.getRider() == null || !o.getRider().getId().equals(riderId)) {
            throw notFound("Order not found");
        }
        return o;
    }

    private static void requireOwner(Long ownerId, Long callerId) {
        if (!ownerId.equals(callerId)) {
            throw notFound("Order not found");
        }
    }

    private OrderView view(Order o, Audience who) {
        boolean admin = who == Audience.ADMIN;
        boolean showPin = who == Audience.BUYER && PIN_VISIBLE_TO_BUYER.contains(o.getStatus());
        boolean showAddress = who == Audience.BUYER || who == Audience.RIDER || admin;
        boolean showSellerEarning = who == Audience.SELLER || admin;
        boolean showRiderEarning = who == Audience.RIDER || admin;

        List<OrderItemView> items = o.getItems().stream()
                .map(i -> new OrderItemView(i.getProductName(), i.getUnitPriceUgx(), i.getQuantity(), i.getLineTotalUgx()))
                .toList();

        return new OrderView(
                o.getId(),
                o.getStatus(),
                o.getPaymentMethod(),
                o.getBuyer().getFullName(),
                o.getSeller().getFullName(),
                o.getRider() == null ? null : o.getRider().getFullName(),
                items,
                o.getSubtotalUgx(),
                o.getDeliveryFeeUgx(),
                o.getServiceFeeUgx(),
                admin ? o.getCommissionUgx() : null,
                showSellerEarning ? o.getSubtotalUgx() - o.getCommissionUgx() : null,
                showRiderEarning ? o.getDeliveryFeeUgx() : null,
                o.getTotalUgx(),
                showAddress ? o.getDeliveryAddress() : null,
                showAddress ? o.getDeliveryNote() : null,
                showPin ? o.getDeliveryPin() : null,
                o.getReason(),
                o.getCreatedAt(),
                o.getUpdatedAt());
    }

    private static ResponseStatusException badRequest(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    private static ResponseStatusException notFound(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }

    private static ResponseStatusException conflict(String m) {
        return new ResponseStatusException(HttpStatus.CONFLICT, m);
    }

    private static ResponseStatusException forbidden(String m) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, m);
    }
}

package com.mangu.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "CustomerOrder")
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private User rider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private long subtotalUgx;

    @Column(nullable = false)
    private long deliveryFeeUgx;

    @Column(nullable = false)
    private long serviceFeeUgx;

    /** Platform commission on the subtotal, taken from the seller's earnings. */
    @Column(nullable = false)
    private long commissionUgx;

    @Column(nullable = false)
    private long totalUgx;

    @Column(nullable = false, length = 300)
    private String deliveryAddress;

    @Column(length = 300)
    private String deliveryNote;

    @Column(nullable = false, length = 4)
    private String deliveryPin;

    @Column(nullable = false)
    private int pinAttempts;

    /** Set when the rider confirms delivery of a cash order. */
    @Column(nullable = false)
    private boolean cashCollected;

    /** Reason for the latest rejection, cancellation, release, or failure. */
    @Column(length = 300)
    private String reason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}

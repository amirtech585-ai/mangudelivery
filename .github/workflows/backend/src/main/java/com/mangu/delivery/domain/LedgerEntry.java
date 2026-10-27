package com.mangu.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Financial record written when an order completes. The entries for one order add up to its total.
 * Party is SELLER, RIDER, or PLATFORM. Entries are never edited; corrections will be new entries.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String party;

    private Long userId;

    @Column(nullable = false)
    private String entryType;

    @Column(nullable = false)
    private long amountUgx;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}

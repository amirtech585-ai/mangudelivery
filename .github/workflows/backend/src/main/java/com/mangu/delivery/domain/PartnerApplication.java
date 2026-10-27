package com.mangu.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Seller business application or rider application, reviewed by the admin. */
@Entity
@Table(name = "partner_applications")
@Getter
@Setter
@NoArgsConstructor
public class PartnerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SELLER or RIDER. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Seller fields
    private String businessName;
    private String productCategories;

    // Rider fields
    private String nationalId;
    private String licenceNumber;
    private String vehicleType;
    private String vehicleRegistration;

    // Shared
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private String rejectionReason;

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    private Instant reviewedAt;
    private Long reviewedBy;
}

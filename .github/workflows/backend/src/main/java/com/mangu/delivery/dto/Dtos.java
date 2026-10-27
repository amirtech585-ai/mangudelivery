package com.mangu.delivery.dto;

import com.mangu.delivery.domain.AccountStatus;
import com.mangu.delivery.domain.ApplicationStatus;
import com.mangu.delivery.domain.Role;
import jakarta.validation.constraints.*;

import java.time.Instant;

/** Request and response shapes for the REST API. */
public final class Dtos {

    private Dtos() {
    }

    public record ApplicationDetails(
            String businessName,
            String productCategories,
            String nationalId,
            String licenceNumber,
            String vehicleType,
            String vehicleRegistration,
            String location) {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            @NotNull Role role,
            ApplicationDetails application) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotNull Role role) {
    }

    public record AuthResponse(String token, Long userId, Role role, AccountStatus status) {
    }

    public record MeResponse(
            Long userId,
            String email,
            String fullName,
            Role role,
            AccountStatus status,
            String applicationStatus,
            String rejectionReason) {
    }

    public record ApplicationView(
            Long id,
            Long userId,
            String fullName,
            String email,
            String phone,
            Role role,
            ApplicationStatus status,
            ApplicationDetails details,
            String rejectionReason,
            Instant submittedAt) {
    }

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 60) String category,
            @Min(1) long priceUgx,
            @Min(0) int stock) {
    }

    public record UserView(Long id, String email, String fullName, Role role, AccountStatus status, Instant createdAt) {
    }

    public record SuspendRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record AuditView(Long id, Long actorId, String action, String targetType, Long targetId,
                            String details, Instant createdAt) {
    }

    public record ProductView(
            Long id,
            String name,
            String description,
            String category,
            long priceUgx,
            int stock,
            boolean active,
            String sellerName) {
    }
}

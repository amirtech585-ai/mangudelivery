package com.mangu.delivery.dto;

import com.mangu.delivery.domain.OrderStatus;
import com.mangu.delivery.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CartLine(@NotNull Long productId, @Min(1) @Max(50) int quantity) {
    }

    public record PlaceOrderRequest(
            @NotEmpty @Valid List<CartLine> items,
            @NotNull PaymentMethod paymentMethod,
            @NotBlank @Size(max = 300) String deliveryAddress,
            @Size(max = 300) String deliveryNote) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 300) String reason) {
    }

    public record DeliverRequest(@NotBlank @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String pin) {
    }

    public record OrderItemView(String productName, long unitPriceUgx, int quantity, long lineTotalUgx) {
    }

    /**
     * One shape for every audience. Fields a viewer must not see are null:
     * the PIN (buyer only, until delivery), the address (seller never sees it),
     * and money fields (commission for admin; earnings for the party that earns them).
     */
    public record OrderView(
            Long id,
            OrderStatus status,
            PaymentMethod paymentMethod,
            String buyerName,
            String sellerName,
            String riderName,
            List<OrderItemView> items,
            long subtotalUgx,
            long deliveryFeeUgx,
            long serviceFeeUgx,
            Long commissionUgx,
            Long sellerEarningUgx,
            Long riderEarningUgx,
            long totalUgx,
            String deliveryAddress,
            String deliveryNote,
            String deliveryPin,
            String reason,
            Instant createdAt,
            Instant updatedAt) {
    }
}

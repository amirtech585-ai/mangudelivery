package com.mangu.delivery.web;

import com.mangu.delivery.dto.OrderDtos.OrderView;
import com.mangu.delivery.dto.OrderDtos.ReasonRequest;
import com.mangu.delivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final OrderService orders;

    @GetMapping
    public List<OrderView> mine(@AuthenticationPrincipal Long userId) {
        return orders.sellerOrders(userId);
    }

    @PostMapping("/{id}/confirm")
    public OrderView confirm(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orders.sellerConfirm(userId, id);
    }

    @PostMapping("/{id}/reject")
    public OrderView reject(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                            @Valid @RequestBody ReasonRequest req) {
        return orders.sellerReject(userId, id, req.reason().trim());
    }

    @PostMapping("/{id}/ready")
    public OrderView ready(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orders.sellerReady(userId, id);
    }
}

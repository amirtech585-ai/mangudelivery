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
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orders;

    @GetMapping
    public List<OrderView> all() {
        return orders.adminOrders();
    }

    @PostMapping("/{id}/cancel")
    public OrderView cancel(@AuthenticationPrincipal Long adminId, @PathVariable Long id,
                            @Valid @RequestBody ReasonRequest req) {
        return orders.adminCancel(adminId, id, req.reason().trim());
    }
}

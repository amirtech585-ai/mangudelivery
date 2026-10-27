package com.mangu.delivery.web;

import com.mangu.delivery.dto.OrderDtos.OrderView;
import com.mangu.delivery.dto.OrderDtos.PlaceOrderRequest;
import com.mangu.delivery.dto.OrderDtos.ReasonRequest;
import com.mangu.delivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buyer/orders")
@RequiredArgsConstructor
public class BuyerOrderController {

    private final OrderService orders;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@AuthenticationPrincipal Long userId, @Valid @RequestBody PlaceOrderRequest req) {
        return orders.placeOrder(userId, req);
    }

    @GetMapping
    public List<OrderView> mine(@AuthenticationPrincipal Long userId) {
        return orders.buyerOrders(userId);
    }

    @GetMapping("/{id}")
    public OrderView one(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orders.buyerOrder(userId, id);
    }

    @PostMapping("/{id}/cancel")
    public OrderView cancel(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                            @Valid @RequestBody ReasonRequest req) {
        return orders.buyerCancel(userId, id, req.reason().trim());
    }
}

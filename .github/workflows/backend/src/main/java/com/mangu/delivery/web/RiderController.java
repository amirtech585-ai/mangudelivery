package com.mangu.delivery.web;

import com.mangu.delivery.dto.OrderDtos.DeliverRequest;
import com.mangu.delivery.dto.OrderDtos.OrderView;
import com.mangu.delivery.dto.OrderDtos.ReasonRequest;
import com.mangu.delivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rider")
@RequiredArgsConstructor
public class RiderController {

    private final OrderService orders;

    /** Jobs the rider can accept now. */
    @GetMapping("/jobs")
    public List<OrderView> jobs(@AuthenticationPrincipal Long userId) {
        return orders.availableJobs(userId);
    }

    /** Every order this rider has accepted, including completed ones. */
    @GetMapping("/orders")
    public List<OrderView> mine(@AuthenticationPrincipal Long userId) {
        return orders.riderOrders(userId);
    }

    @PostMapping("/jobs/{id}/accept")
    public OrderView accept(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orders.riderAccept(userId, id);
    }

    @PostMapping("/jobs/{id}/release")
    public OrderView release(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                             @Valid @RequestBody ReasonRequest req) {
        return orders.riderRelease(userId, id, req.reason().trim());
    }

    @PostMapping("/jobs/{id}/pickup")
    public OrderView pickup(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orders.riderPickup(userId, id);
    }

    @PostMapping("/jobs/{id}/on-the-way")
    public OrderView onTheWay(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return orders.riderOnTheWay(userId, id);
    }

    @PostMapping("/jobs/{id}/deliver")
    public OrderView deliver(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                             @Valid @RequestBody DeliverRequest req) {
        return orders.riderDeliver(userId, id, req.pin());
    }

    @PostMapping("/jobs/{id}/fail")
    public OrderView fail(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                          @Valid @RequestBody ReasonRequest req) {
        return orders.riderFail(userId, id, req.reason().trim());
    }
}

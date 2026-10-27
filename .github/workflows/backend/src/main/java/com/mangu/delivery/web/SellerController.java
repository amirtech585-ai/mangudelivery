package com.mangu.delivery.web;

import com.mangu.delivery.dto.Dtos.ProductRequest;
import com.mangu.delivery.dto.Dtos.ProductView;
import com.mangu.delivery.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class SellerController {

    private final ProductService products;

    @GetMapping
    public List<ProductView> mine(@AuthenticationPrincipal Long userId) {
        return products.myListings(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@AuthenticationPrincipal Long userId, @Valid @RequestBody ProductRequest req) {
        return products.create(userId, req);
    }
}

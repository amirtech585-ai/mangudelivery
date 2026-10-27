package com.mangu.delivery.web;

import com.mangu.delivery.dto.Dtos.ProductView;
import com.mangu.delivery.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final ProductService products;

    @GetMapping("/products")
    public List<ProductView> products(@AuthenticationPrincipal Long userId) {
        return products.publicListings(userId);
    }
}

package com.mangu.delivery.service;

import com.mangu.delivery.domain.Product;
import com.mangu.delivery.domain.User;
import com.mangu.delivery.dto.Dtos.ProductRequest;
import com.mangu.delivery.dto.Dtos.ProductView;
import com.mangu.delivery.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;
    private final AuthService auth;

    /** Buyers see only active listings from approved sellers. */
    @Transactional(readOnly = true)
    public List<ProductView> publicListings(Long buyerId) {
        auth.requireActive(buyerId);
        return products.findPublicListings().stream().map(ProductService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> myListings(Long sellerId) {
        auth.requireActive(sellerId);
        return products.findBySellerIdOrderByIdDesc(sellerId).stream().map(ProductService::toView).toList();
    }

    /** Sellers cannot publish until approved (requireActive enforces it). */
    @Transactional
    public ProductView create(Long sellerId, ProductRequest req) {
        User seller = auth.requireActive(sellerId);
        Product p = new Product();
        p.setSeller(seller);
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setCategory(req.category().trim());
        p.setPriceUgx(req.priceUgx());
        p.setStock(req.stock());
        products.save(p);
        return toView(p);
    }

    private static ProductView toView(Product p) {
        return new ProductView(p.getId(), p.getName(), p.getDescription(), p.getCategory(),
                p.getPriceUgx(), p.getStock(), p.isActive(), p.getSeller().getFullName());
    }
}

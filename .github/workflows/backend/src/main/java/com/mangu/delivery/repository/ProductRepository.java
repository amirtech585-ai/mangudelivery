package com.mangu.delivery.repository;

import com.mangu.delivery.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerIdOrderByIdDesc(Long sellerId);

    /** Buyer listings: active products from sellers whose account is ACTIVE (approved). */
    @Query("""
           select p from Product p
           where p.active = true
             and p.seller.status = com.mangu.delivery.domain.AccountStatus.ACTIVE
           order by p.id desc
           """)
    List<Product> findPublicListings();

    /** Locks the row so two orders cannot take the last items in stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}

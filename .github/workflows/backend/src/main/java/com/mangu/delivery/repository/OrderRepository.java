package com.mangu.delivery.repository;

import com.mangu.delivery.domain.Order;
import com.mangu.delivery.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByIdDesc(Long buyerId);

    List<Order> findBySellerIdOrderByIdDesc(Long sellerId);

    List<Order> findByRiderIdOrderByIdDesc(Long riderId);

    List<Order> findTop200ByOrderByIdDesc();

    List<Order> findByStatusAndRiderIsNullOrderByIdAsc(OrderStatus status);

    /** Locks the order row so that two riders cannot accept the same job. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}

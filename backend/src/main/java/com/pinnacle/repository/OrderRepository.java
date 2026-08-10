package com.pinnacle.repository;

import com.pinnacle.entity.Order;
import com.pinnacle.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    List<Order> findByAccountIdAndStatus(UUID accountId, OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
}

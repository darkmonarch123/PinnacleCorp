package com.pinnacle.repository;

import com.pinnacle.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    List<Trade> findByAccountIdOrderByClosedAtDesc(UUID accountId);
}

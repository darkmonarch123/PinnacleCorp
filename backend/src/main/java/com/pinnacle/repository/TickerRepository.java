package com.pinnacle.repository;

import com.pinnacle.entity.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TickerRepository extends JpaRepository<Ticker, UUID> {
    Optional<Ticker> findBySymbol(String symbol);
    List<Ticker> findByActiveTrue();
}

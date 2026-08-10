package com.pinnacle.repository;

import com.pinnacle.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {
    List<PriceAlert> findByUserId(UUID userId);
    List<PriceAlert> findByActiveTrue();
}

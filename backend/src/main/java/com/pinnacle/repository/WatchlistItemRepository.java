package com.pinnacle.repository;

import com.pinnacle.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {
    List<WatchlistItem> findByUserIdOrderBySortOrderAsc(UUID userId);
    Optional<WatchlistItem> findByUserIdAndTickerId(UUID userId, UUID tickerId);
    void deleteByUserIdAndTickerId(UUID userId, UUID tickerId);
}

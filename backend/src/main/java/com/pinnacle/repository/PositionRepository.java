package com.pinnacle.repository;

import com.pinnacle.entity.Position;
import com.pinnacle.entity.enums.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByAccountIdAndStatus(UUID accountId, PositionStatus status);
    List<Position> findByStatus(PositionStatus status);
    List<Position> findByStatusIn(List<PositionStatus> statuses);
    List<Position> findByAccountIdAndStatusNotOrderByOpenedAtDesc(UUID accountId, PositionStatus status);
    List<Position> findByAccountIdOrderByOpenedAtDesc(UUID accountId);

    @Query("""
        SELECT p FROM Position p
        WHERE p.accountId = :accountId AND p.tickerId = :tickerId AND p.remainingQuantity > 0
        ORDER BY p.openedAt ASC
        """)
    List<Position> findOpenPositionsForNetting(@Param("accountId") UUID accountId, @Param("tickerId") UUID tickerId);
}

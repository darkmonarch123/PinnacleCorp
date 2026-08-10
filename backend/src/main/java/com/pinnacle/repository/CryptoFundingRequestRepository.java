package com.pinnacle.repository;

import com.pinnacle.entity.CryptoFundingRequest;
import com.pinnacle.entity.enums.FundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CryptoFundingRequestRepository extends JpaRepository<CryptoFundingRequest, UUID> {
    List<CryptoFundingRequest> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    List<CryptoFundingRequest> findByStatusOrderByCreatedAtAsc(FundingStatus status);
}

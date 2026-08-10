package com.pinnacle.repository;

import com.pinnacle.entity.BankTransferRequest;
import com.pinnacle.entity.enums.FundingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankTransferRequestRepository extends JpaRepository<BankTransferRequest, UUID> {
    List<BankTransferRequest> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    List<BankTransferRequest> findByStatusOrderByCreatedAtAsc(FundingStatus status);
}

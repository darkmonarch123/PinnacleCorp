package com.pinnacle.funding.controller;

import com.pinnacle.funding.dto.*;
import com.pinnacle.funding.service.FundingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/funding")
public class FundingController {

    private final FundingService fundingService;

    public FundingController(FundingService fundingService) {
        this.fundingService = fundingService;
    }

    // ---------------- Bank transfer (user-facing) ----------------

    @PostMapping("/bank-transfer")
    public ResponseEntity<BankTransferResponse> createBankTransfer(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateBankTransferRequest request
    ) {
        return ResponseEntity.ok(fundingService.createBankTransfer(userId, request));
    }

    @GetMapping("/bank-transfer")
    public ResponseEntity<List<BankTransferResponse>> listMyBankTransfers(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(fundingService.listMyBankTransfers(userId));
    }

    // ---------------- Crypto funding (user-facing) ----------------

    @PostMapping("/crypto")
    public ResponseEntity<CryptoFundingResponse> createCryptoFunding(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateCryptoFundingRequest request
    ) {
        return ResponseEntity.ok(fundingService.createCryptoFunding(userId, request));
    }

    @GetMapping("/crypto")
    public ResponseEntity<List<CryptoFundingResponse>> listMyCryptoFundings(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(fundingService.listMyCryptoFundings(userId));
    }

    // ---------------- Admin ----------------
    // No broader role system exists — gated purely on User.isAdmin(). Set that
    // flag directly in the database for whichever account should review
    // funding requests during the demo.

    @GetMapping("/admin/bank-transfer/pending")
    public ResponseEntity<List<BankTransferResponse>> listPendingBankTransfers(@AuthenticationPrincipal UUID adminUserId) {
        return ResponseEntity.ok(fundingService.listPendingBankTransfers(adminUserId));
    }

    @PostMapping("/admin/bank-transfer/{requestId}/confirm")
    public ResponseEntity<BankTransferResponse> confirmBankTransfer(
            @AuthenticationPrincipal UUID adminUserId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminDecisionRequest request
    ) {
        String note = request != null ? request.adminNote() : null;
        return ResponseEntity.ok(fundingService.confirmBankTransfer(adminUserId, requestId, note));
    }

    @PostMapping("/admin/bank-transfer/{requestId}/reject")
    public ResponseEntity<BankTransferResponse> rejectBankTransfer(
            @AuthenticationPrincipal UUID adminUserId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminDecisionRequest request
    ) {
        String note = request != null ? request.adminNote() : null;
        return ResponseEntity.ok(fundingService.rejectBankTransfer(adminUserId, requestId, note));
    }

    @GetMapping("/admin/crypto/pending")
    public ResponseEntity<List<CryptoFundingResponse>> listPendingCryptoFundings(@AuthenticationPrincipal UUID adminUserId) {
        return ResponseEntity.ok(fundingService.listPendingCryptoFundings(adminUserId));
    }

    @PostMapping("/admin/crypto/{requestId}/confirm")
    public ResponseEntity<CryptoFundingResponse> confirmCryptoFunding(
            @AuthenticationPrincipal UUID adminUserId,
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminDecisionRequest request
    ) {
        return ResponseEntity.ok(fundingService.confirmCryptoFunding(adminUserId, requestId, request.usdEquivalent(), request.adminNote()));
    }

    @PostMapping("/admin/crypto/{requestId}/reject")
    public ResponseEntity<CryptoFundingResponse> rejectCryptoFunding(
            @AuthenticationPrincipal UUID adminUserId,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminDecisionRequest request
    ) {
        String note = request != null ? request.adminNote() : null;
        return ResponseEntity.ok(fundingService.rejectCryptoFunding(adminUserId, requestId, note));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
    }
}

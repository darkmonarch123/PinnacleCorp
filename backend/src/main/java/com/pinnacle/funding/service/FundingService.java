package com.pinnacle.funding.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.BankTransferRequest;
import com.pinnacle.entity.CryptoFundingRequest;
import com.pinnacle.entity.User;
import com.pinnacle.entity.enums.FundingStatus;
import com.pinnacle.entity.enums.LedgerEntryType;
import com.pinnacle.funding.dto.*;
import com.pinnacle.oms.service.LedgerService;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.BankTransferRequestRepository;
import com.pinnacle.repository.CryptoFundingRequestRepository;
import com.pinnacle.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Simulated funding only — no real banking or blockchain integration exists.
 * Confirmation is gated on User.isAdmin() rather than any real verification.
 * Confirming always goes through LedgerService (DEPOSIT entries), never
 * touches Account.balance directly — same discipline as the rest of the app.
 *
 * Known simplification: ledger credits happen at face value in the request's
 * stated amount/currency, with no FX conversion against the account's own
 * currency. Fine for a demo; would need real conversion before this could
 * mean anything financially. See FINAL_HANDOFF.md.
 */
@Service
public class FundingService {

    private final BankTransferRequestRepository bankTransferRepository;
    private final CryptoFundingRequestRepository cryptoFundingRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;

    public FundingService(
            BankTransferRequestRepository bankTransferRepository,
            CryptoFundingRequestRepository cryptoFundingRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            LedgerService ledgerService
    ) {
        this.bankTransferRepository = bankTransferRepository;
        this.cryptoFundingRepository = cryptoFundingRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.ledgerService = ledgerService;
    }

    // ---------------- Bank transfer ----------------

    @Transactional
    public BankTransferResponse createBankTransfer(UUID userId, CreateBankTransferRequest request) {
        Account account = resolveAccount(userId);

        BankTransferRequest transfer = new BankTransferRequest();
        transfer.setAccountId(account.getId());
        transfer.setAmount(request.amount());
        transfer.setCurrency(request.currency().toUpperCase());
        transfer.setTransferReference(generateReference("BT"));
        bankTransferRepository.save(transfer);

        return BankTransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public List<BankTransferResponse> listMyBankTransfers(UUID userId) {
        Account account = resolveAccount(userId);
        return bankTransferRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(BankTransferResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BankTransferResponse> listPendingBankTransfers(UUID adminUserId) {
        requireAdmin(adminUserId);
        return bankTransferRepository.findByStatusOrderByCreatedAtAsc(FundingStatus.PENDING).stream()
                .map(BankTransferResponse::from).toList();
    }

    @Transactional
    public BankTransferResponse confirmBankTransfer(UUID adminUserId, UUID requestId, String adminNote) {
        requireAdmin(adminUserId);
        BankTransferRequest transfer = findPendingBankTransfer(requestId);

        Account account = accountRepository.findById(transfer.getAccountId())
                .orElseThrow(() -> new IllegalStateException("Account not found for funding request"));

        transfer.setStatus(FundingStatus.CONFIRMED);
        transfer.setAdminNote(adminNote);
        transfer.setConfirmedAt(Instant.now());
        bankTransferRepository.save(transfer);

        ledgerService.postEntry(
                account, LedgerEntryType.DEPOSIT, transfer.getAmount(), null,
                "Bank transfer confirmed: " + transfer.getTransferReference()
        );

        return BankTransferResponse.from(transfer);
    }

    @Transactional
    public BankTransferResponse rejectBankTransfer(UUID adminUserId, UUID requestId, String adminNote) {
        requireAdmin(adminUserId);
        BankTransferRequest transfer = findPendingBankTransfer(requestId);

        transfer.setStatus(FundingStatus.REJECTED);
        transfer.setAdminNote(adminNote);
        bankTransferRepository.save(transfer);

        return BankTransferResponse.from(transfer);
    }

    // ---------------- Crypto funding ----------------

    @Transactional
    public CryptoFundingResponse createCryptoFunding(UUID userId, CreateCryptoFundingRequest request) {
        Account account = resolveAccount(userId);

        CryptoFundingRequest funding = new CryptoFundingRequest();
        funding.setAccountId(account.getId());
        funding.setCryptoType(request.cryptoType().toUpperCase());
        funding.setNetwork(request.network());
        funding.setWalletAddress(request.walletAddress());
        funding.setTransactionHash(request.transactionHash());
        funding.setAmount(request.amount());
        cryptoFundingRepository.save(funding);

        return CryptoFundingResponse.from(funding);
    }

    @Transactional(readOnly = true)
    public List<CryptoFundingResponse> listMyCryptoFundings(UUID userId) {
        Account account = resolveAccount(userId);
        return cryptoFundingRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(CryptoFundingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CryptoFundingResponse> listPendingCryptoFundings(UUID adminUserId) {
        requireAdmin(adminUserId);
        return cryptoFundingRepository.findByStatusOrderByCreatedAtAsc(FundingStatus.PENDING).stream()
                .map(CryptoFundingResponse::from).toList();
    }

    @Transactional
    public CryptoFundingResponse confirmCryptoFunding(UUID adminUserId, UUID requestId, BigDecimal usdEquivalent, String adminNote) {
        requireAdmin(adminUserId);
        if (usdEquivalent == null || usdEquivalent.signum() <= 0) {
            throw new IllegalArgumentException("usdEquivalent is required to confirm a crypto funding request");
        }

        CryptoFundingRequest funding = findPendingCryptoFunding(requestId);
        Account account = accountRepository.findById(funding.getAccountId())
                .orElseThrow(() -> new IllegalStateException("Account not found for funding request"));

        funding.setStatus(FundingStatus.CONFIRMED);
        funding.setUsdEquivalent(usdEquivalent);
        funding.setAdminNote(adminNote);
        funding.setConfirmedAt(Instant.now());
        cryptoFundingRepository.save(funding);

        ledgerService.postEntry(
                account, LedgerEntryType.DEPOSIT, usdEquivalent, null,
                "Crypto funding confirmed: " + funding.getCryptoType() + " tx " + funding.getTransactionHash()
        );

        return CryptoFundingResponse.from(funding);
    }

    @Transactional
    public CryptoFundingResponse rejectCryptoFunding(UUID adminUserId, UUID requestId, String adminNote) {
        requireAdmin(adminUserId);
        CryptoFundingRequest funding = findPendingCryptoFunding(requestId);

        funding.setStatus(FundingStatus.REJECTED);
        funding.setAdminNote(adminNote);
        cryptoFundingRepository.save(funding);

        return CryptoFundingResponse.from(funding);
    }

    // ---------------- helpers ----------------

    private Account resolveAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));
    }

    private void requireAdmin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (!user.isAdmin()) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private BankTransferRequest findPendingBankTransfer(UUID requestId) {
        BankTransferRequest transfer = bankTransferRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer request not found"));
        if (transfer.getStatus() != FundingStatus.PENDING) {
            throw new IllegalStateException("Request is already " + transfer.getStatus());
        }
        return transfer;
    }

    private CryptoFundingRequest findPendingCryptoFunding(UUID requestId) {
        CryptoFundingRequest funding = cryptoFundingRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Crypto funding request not found"));
        if (funding.getStatus() != FundingStatus.PENDING) {
            throw new IllegalStateException("Request is already " + funding.getStatus());
        }
        return funding;
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

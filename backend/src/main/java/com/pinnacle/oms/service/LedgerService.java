package com.pinnacle.oms.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.LedgerEntry;
import com.pinnacle.entity.enums.LedgerEntryType;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The single write path for anything that changes an account's balance.
 * Account.balance / Account.buyingPower are caches for fast reads — this is
 * the only class allowed to mutate them, and it always does so by writing an
 * immutable ledger_entries row first.
 *
 * NOTE: this simplified model moves buyingPower in lockstep with balance
 * (no separate margin multiplier yet). That's a deliberate scope cut for
 * this pass, not an oversight — see README.
 */
@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository, AccountRepository accountRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * @param amount signed: positive credits the account, negative debits it
     */
    @Transactional
    public void postEntry(Account account, LedgerEntryType type, BigDecimal amount, UUID referenceOrderId, String description) {
        postEntry(account, type, amount, referenceOrderId, null, description);
    }

    /** Overload for entries tied to a closed trade (e.g. REALIZED_PNL), which carries both references. */
    @Transactional
    public void postEntry(Account account, LedgerEntryType type, BigDecimal amount, UUID referenceOrderId, UUID referenceTradeId, String description) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccountId(account.getId());
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setReferenceOrderId(referenceOrderId);
        entry.setReferenceTradeId(referenceTradeId);
        entry.setDescription(description);
        ledgerEntryRepository.save(entry);

        account.setBalance(account.getBalance().add(amount));
        account.setBuyingPower(account.getBuyingPower().add(amount));
        accountRepository.save(account);
    }
}

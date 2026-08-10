package com.pinnacle.oms.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.LedgerEntry;
import com.pinnacle.entity.enums.LedgerEntryType;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    private LedgerService ledgerService;
    private Account account;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerEntryRepository, accountRepository);

        account = new Account();
        account.setId(UUID.randomUUID());
        account.setBalance(new BigDecimal("10000.00"));
        account.setBuyingPower(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("a debit reduces both balance and buying power by the same amount")
    void debitReducesBalanceAndBuyingPower() {
        ledgerService.postEntry(account, LedgerEntryType.ORDER_FILL_DEBIT, new BigDecimal("-2000.00"), UUID.randomUUID(), "Fill");

        assertThat(account.getBalance()).isEqualByComparingTo("8000.00");
        assertThat(account.getBuyingPower()).isEqualByComparingTo("8000.00");
    }

    @Test
    @DisplayName("a credit increases both balance and buying power by the same amount")
    void creditIncreasesBalanceAndBuyingPower() {
        ledgerService.postEntry(account, LedgerEntryType.ORDER_FILL_CREDIT, new BigDecimal("1500.00"), UUID.randomUUID(), "Release capital");

        assertThat(account.getBalance()).isEqualByComparingTo("11500.00");
        assertThat(account.getBuyingPower()).isEqualByComparingTo("11500.00");
    }

    @Test
    @DisplayName("every posted entry is persisted with the correct signed amount and type")
    void persistsLedgerEntryWithCorrectFields() {
        UUID orderId = UUID.randomUUID();
        ledgerService.postEntry(account, LedgerEntryType.REALIZED_PNL, new BigDecimal("-50.00"), orderId, "Loss on close");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

        LedgerEntry saved = captor.getValue();
        assertThat(saved.getAccountId()).isEqualTo(account.getId());
        assertThat(saved.getEntryType()).isEqualTo(LedgerEntryType.REALIZED_PNL);
        assertThat(saved.getAmount()).isEqualByComparingTo("-50.00");
        assertThat(saved.getReferenceOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("the reference-trade overload attaches the trade ID to the persisted entry")
    void persistsReferenceTradeId() {
        UUID orderId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();

        ledgerService.postEntry(account, LedgerEntryType.REALIZED_PNL, new BigDecimal("100.00"), orderId, tradeId, "Win on close");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());

        assertThat(captor.getValue().getReferenceTradeId()).isEqualTo(tradeId);
    }

    @Test
    @DisplayName("the updated account is persisted after every posting")
    void savesAccountAfterPosting() {
        ledgerService.postEntry(account, LedgerEntryType.DEPOSIT, new BigDecimal("500.00"), null, "Deposit");

        verify(accountRepository).save(account);
    }
}

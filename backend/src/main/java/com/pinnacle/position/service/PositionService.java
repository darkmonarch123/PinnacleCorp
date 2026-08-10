package com.pinnacle.position.service;

import com.pinnacle.entity.*;
import com.pinnacle.entity.enums.LedgerEntryType;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.PositionStatus;
import com.pinnacle.oms.service.LedgerService;
import com.pinnacle.position.dto.PositionResponse;
import com.pinnacle.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The reconciliation layer order placement deliberately leaves alone: a fill
 * either nets against existing opposite-side positions (closing them,
 * generating a Trade with realized P&L) or, once fully netted, opens/adds to
 * a same-side position for whatever quantity is left over.
 *
 * Netting is FIFO across a ticker's open positions for a given account.
 */
@Service
public class PositionService {

    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final TickerRepository tickerRepository;
    private final TradeRepository tradeRepository;
    private final LedgerService ledgerService;

    public PositionService(
            PositionRepository positionRepository,
            AccountRepository accountRepository,
            TickerRepository tickerRepository,
            TradeRepository tradeRepository,
            LedgerService ledgerService
    ) {
        this.positionRepository = positionRepository;
        this.accountRepository = accountRepository;
        this.tickerRepository = tickerRepository;
        this.tradeRepository = tradeRepository;
        this.ledgerService = ledgerService;
    }

    /**
     * Called from OrderService when an order fills. Nets against any
     * existing opposite-side positions first (FIFO), then opens or adds to
     * a same-side position with whatever quantity is left over — this is
     * what lets a SELL close a long instead of always opening a new short.
     */
    @Transactional
    public void processFill(Account account, Ticker ticker, Order order, BigDecimal fillPrice) {
        List<Position> candidates = positionRepository.findOpenPositionsForNetting(account.getId(), ticker.getId());

        BigDecimal remaining = order.getQuantity();

        for (Position position : candidates) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (position.getSide() == order.getSide()) continue; // same side: nothing to net here, handled below

            BigDecimal closeQty = remaining.min(position.getRemainingQuantity());
            closePosition(account, ticker, position, closeQty, fillPrice, order.getId());
            remaining = remaining.subtract(closeQty);
        }

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) return;

        // Leftover quantity after netting: add to an existing same-side position, or open a new one.
        Position sameSide = candidates.stream()
                .filter(p -> p.getSide() == order.getSide() && p.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);

        if (sameSide != null) {
            increasePosition(sameSide, remaining, fillPrice);
        } else {
            openPosition(account, ticker, order, remaining, fillPrice);
        }

        BigDecimal notional = fillPrice.multiply(remaining);
        ledgerService.postEntry(
                account, LedgerEntryType.ORDER_FILL_DEBIT, notional.negate(), order.getId(),
                "Fill: " + order.getSide() + " " + remaining + " " + ticker.getSymbol() + " @ " + fillPrice
        );
    }

    private void openPosition(Account account, Ticker ticker, Order order, BigDecimal quantity, BigDecimal fillPrice) {
        Position position = new Position();
        position.setAccountId(account.getId());
        position.setTickerId(ticker.getId());
        position.setOriginOrderId(order.getId());
        position.setSide(order.getSide());
        position.setStatus(PositionStatus.OPEN);
        position.setQuantity(quantity);
        position.setRemainingQuantity(quantity);
        position.setEntryPrice(fillPrice);
        position.setStopLoss(order.getStopLoss());
        position.setTakeProfit(order.getTakeProfit());
        positionRepository.save(position);
    }

    /** Weighted-average the entry price in as size increases; marks the position MODIFIED. */
    private void increasePosition(Position position, BigDecimal addQuantity, BigDecimal fillPrice) {
        BigDecimal existingNotional = position.getEntryPrice().multiply(position.getRemainingQuantity());
        BigDecimal addedNotional = fillPrice.multiply(addQuantity);
        BigDecimal newRemaining = position.getRemainingQuantity().add(addQuantity);

        BigDecimal newAvgEntry = existingNotional.add(addedNotional)
                .divide(newRemaining, 4, java.math.RoundingMode.HALF_UP);

        position.setEntryPrice(newAvgEntry);
        position.setQuantity(position.getQuantity().add(addQuantity));
        position.setRemainingQuantity(newRemaining);
        position.setStatus(PositionStatus.MODIFIED);
        positionRepository.save(position);
    }

    /**
     * Closes (fully or partially) a position at exitPrice: computes realized
     * P&L, writes a Trade row, and releases the closed portion's reserved
     * capital plus/minus the P&L back to the account via the ledger.
     */
    @Transactional
    public void closePosition(Account account, Ticker ticker, Position position, BigDecimal closeQty, BigDecimal exitPrice, UUID triggeringOrderId) {
        position.setStatus(PositionStatus.PENDING_CLOSE);
        positionRepository.save(position);

        BigDecimal pnl = position.getSide() == OrderSide.BUY
                ? exitPrice.subtract(position.getEntryPrice()).multiply(closeQty)
                : position.getEntryPrice().subtract(exitPrice).multiply(closeQty);

        Trade trade = new Trade();
        trade.setAccountId(account.getId());
        trade.setPositionId(position.getId());
        trade.setTickerId(ticker.getId());
        trade.setSide(position.getSide());
        trade.setQuantity(closeQty);
        trade.setEntryPrice(position.getEntryPrice());
        trade.setExitPrice(exitPrice);
        trade.setRealizedPnl(pnl);
        trade.setOpenedAt(position.getOpenedAt());
        trade.setClosedAt(Instant.now());
        tradeRepository.save(trade);

        // Release the reserved capital for the closed portion, then post the P&L —
        // together these net to "receive exitPrice*closeQty" for a long, or the
        // mirrored amount for a short, consistent with how opening a position
        // reserved capital at entryPrice in the first place (see LedgerService).
        BigDecimal releasedCapital = position.getEntryPrice().multiply(closeQty);
        ledgerService.postEntry(
                account, LedgerEntryType.ORDER_FILL_CREDIT, releasedCapital, triggeringOrderId,
                "Release capital closing " + closeQty + " " + ticker.getSymbol()
        );
        ledgerService.postEntry(
                account, LedgerEntryType.REALIZED_PNL, pnl, triggeringOrderId, trade.getId(),
                "Realized P&L closing " + closeQty + " " + ticker.getSymbol()
        );

        position.setRemainingQuantity(position.getRemainingQuantity().subtract(closeQty));
        if (position.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            position.setStatus(PositionStatus.CLOSED);
            position.setClosedAt(Instant.now());
        } else {
            position.setStatus(PositionStatus.PARTIAL_CLOSE);
        }
        positionRepository.save(position);
    }

    @Transactional
    public PositionResponse closePositionManually(UUID userId, UUID positionId, BigDecimal requestedQuantity, BigDecimal currentPrice) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found"));

        if (!position.getAccountId().equals(account.getId())) {
            throw new IllegalArgumentException("Position not found");
        }
        if (position.getStatus() == PositionStatus.CLOSED) {
            throw new IllegalStateException("Position is already closed");
        }

        Ticker ticker = tickerRepository.findById(position.getTickerId())
                .orElseThrow(() -> new IllegalStateException("Ticker not found for position"));

        BigDecimal closeQty = requestedQuantity != null
                ? requestedQuantity.min(position.getRemainingQuantity())
                : position.getRemainingQuantity();

        closePosition(account, ticker, position, closeQty, currentPrice, null);

        return PositionResponse.from(position, ticker.getSymbol());
    }

    @Transactional
    public PositionResponse modifyPosition(UUID userId, UUID positionId, BigDecimal stopLoss, BigDecimal takeProfit,
                                            boolean clearStopLoss, boolean clearTakeProfit) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found"));

        if (!position.getAccountId().equals(account.getId())) {
            throw new IllegalArgumentException("Position not found");
        }
        if (position.getStatus() == PositionStatus.CLOSED) {
            throw new IllegalStateException("Cannot modify a closed position");
        }

        if (clearStopLoss) position.setStopLoss(null);
        else if (stopLoss != null) position.setStopLoss(stopLoss);

        if (clearTakeProfit) position.setTakeProfit(null);
        else if (takeProfit != null) position.setTakeProfit(takeProfit);

        if (position.getStatus() == PositionStatus.OPEN) {
            position.setStatus(PositionStatus.MODIFIED);
        }
        positionRepository.save(position);

        String symbol = tickerRepository.findById(position.getTickerId()).map(Ticker::getSymbol).orElse("UNKNOWN");
        return PositionResponse.from(position, symbol);
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> listPositions(UUID userId, boolean openOnly) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        List<Position> positions = openOnly
                ? positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED)
                : positionRepository.findByAccountIdOrderByOpenedAtDesc(account.getId());

        return positions.stream()
                .map(p -> PositionResponse.from(p, tickerRepository.findById(p.getTickerId()).map(Ticker::getSymbol).orElse("UNKNOWN")))
                .toList();
    }
}

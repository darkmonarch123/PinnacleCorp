package com.pinnacle.oms.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Order;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.OrderType;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.oms.dto.OrderResponse;
import com.pinnacle.oms.dto.PlaceOrderRequest;
import com.pinnacle.oms.service.RiskCheckService.RiskCheckResult;
import com.pinnacle.position.service.PositionService;
import com.pinnacle.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order lifecycle:
 *   NEW -> PENDING_RISK_CHECK -> ROUTED -> FILLED
 *                             \-> REJECTED
 *                    ROUTED  -> CANCELLED (user-initiated) / EXPIRED (background job)
 *
 * A FILLED order is handed off to PositionService, which nets it against
 * any existing opposite-side position (closing it, generating a Trade with
 * realized P&L) before opening or adding to a same-side position with
 * whatever quantity is left over. That reconciliation logic intentionally
 * lives in PositionService, not here — see its class doc.
 *
 * PARTIALLY_FILLED exists in the schema for a future partial-fill / order
 * book simulation; this pass only ever produces full fills.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final TickerRepository tickerRepository;
    private final RiskCheckService riskCheckService;
    private final PositionService positionService;
    private final PriceCacheService priceCacheService;

    public OrderService(
            OrderRepository orderRepository,
            AccountRepository accountRepository,
            TickerRepository tickerRepository,
            RiskCheckService riskCheckService,
            PositionService positionService,
            PriceCacheService priceCacheService
    ) {
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.tickerRepository = tickerRepository;
        this.riskCheckService = riskCheckService;
        this.positionService = positionService;
        this.priceCacheService = priceCacheService;
    }

    @Transactional
    public OrderResponse placeOrder(UUID userId, PlaceOrderRequest request) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        String symbol = request.symbol().toUpperCase();
        Ticker ticker = tickerRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + symbol));

        if (request.type() == OrderType.LIMIT && request.limitPrice() == null) {
            throw new IllegalArgumentException("limitPrice is required for LIMIT orders");
        }

        Order order = new Order();
        order.setAccountId(account.getId());
        order.setTickerId(ticker.getId());
        order.setSide(request.side());
        order.setType(request.type());
        order.setQuantity(request.quantity());
        order.setLimitPrice(request.limitPrice());
        order.setStopLoss(request.stopLoss());
        order.setTakeProfit(request.takeProfit());
        order.setStatus(OrderStatus.NEW);
        orderRepository.save(order);

        order.setStatus(OrderStatus.PENDING_RISK_CHECK);
        orderRepository.save(order);

        BigDecimal priceForRiskCheck = request.type() == OrderType.LIMIT
                ? request.limitPrice()
                : priceCacheService.getLatestPrice(symbol).orElse(null);

        if (priceForRiskCheck == null) {
            return reject(order, symbol, "No live price available for " + symbol + " yet — try again shortly");
        }

        RiskCheckResult risk = riskCheckService.checkOrder(account, ticker, request.quantity(), priceForRiskCheck);
        if (!risk.passed()) {
            return reject(order, symbol, risk.reason());
        }

        order.setStatus(OrderStatus.ROUTED);
        orderRepository.save(order);

        Optional<BigDecimal> currentPrice = priceCacheService.getLatestPrice(symbol);
        if (currentPrice.isPresent() && isMarketable(order, currentPrice.get())) {
            fill(order, account, ticker, currentPrice.get());
        }
        // else: stays ROUTED, picked up by OrderMatchingService's scheduled sweep

        return OrderResponse.from(order, symbol);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getAccountId().equals(account.getId())) {
            throw new IllegalArgumentException("Order not found");
        }

        if (order.getStatus() != OrderStatus.ROUTED) {
            throw new IllegalStateException("Only pending (ROUTED) orders can be cancelled — this order is " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        String symbol = tickerRepository.findById(order.getTickerId()).map(Ticker::getSymbol).orElse("UNKNOWN");
        return OrderResponse.from(order, symbol);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        return orderRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(o -> OrderResponse.from(o, resolveSymbol(o.getTickerId())))
                .toList();
    }

    /** Used by OrderMatchingService when a pending limit order becomes marketable. */
    @Transactional
    public void fillPendingOrder(Order order, BigDecimal fillPrice) {
        Account account = accountRepository.findById(order.getAccountId())
                .orElseThrow(() -> new IllegalStateException("Account not found for order " + order.getId()));
        Ticker ticker = tickerRepository.findById(order.getTickerId())
                .orElseThrow(() -> new IllegalStateException("Ticker not found for order " + order.getId()));

        fill(order, account, ticker, fillPrice);
    }

    private boolean isMarketable(Order order, BigDecimal currentPrice) {
        if (order.getType() == OrderType.MARKET) return true;

        return switch (order.getSide()) {
            case BUY -> currentPrice.compareTo(order.getLimitPrice()) <= 0;
            case SELL -> currentPrice.compareTo(order.getLimitPrice()) >= 0;
        };
    }

    private void fill(Order order, Account account, Ticker ticker, BigDecimal fillPrice) {
        order.setFilledQuantity(order.getQuantity());
        order.setStatus(OrderStatus.FILLED);
        orderRepository.save(order);

        // Netting against any existing opposite-side position, opening/adding
        // to a same-side one for the leftover, and all ledger postings for
        // the fill happen here — see PositionService for why this isn't
        // handled inline in OMS.
        positionService.processFill(account, ticker, order, fillPrice);
    }

    private OrderResponse reject(Order order, String symbol, String reason) {
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        orderRepository.save(order);
        return OrderResponse.from(order, symbol);
    }

    private String resolveSymbol(UUID tickerId) {
        return tickerRepository.findById(tickerId).map(Ticker::getSymbol).orElse("UNKNOWN");
    }
}

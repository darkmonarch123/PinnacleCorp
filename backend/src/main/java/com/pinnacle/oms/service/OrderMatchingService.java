package com.pinnacle.oms.service;

import com.pinnacle.entity.Order;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.OrderType;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.repository.OrderRepository;
import com.pinnacle.repository.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * A market order always fills synchronously in OrderService — if there's no
 * price it's rejected outright, so nothing market-typed ever sits ROUTED.
 * This sweep exists purely for pending LIMIT orders: on each pass it checks
 * every ROUTED order's ticker price and fills it the moment it crosses the
 * limit. Stale ROUTED orders (default: 24h) are expired rather than left
 * pending forever.
 */
@Service
public class OrderMatchingService {

    private static final Logger log = LoggerFactory.getLogger(OrderMatchingService.class);
    private static final long EXPIRY_HOURS = 24;

    private final OrderRepository orderRepository;
    private final TickerRepository tickerRepository;
    private final PriceCacheService priceCacheService;
    private final OrderService orderService;

    public OrderMatchingService(
            OrderRepository orderRepository,
            TickerRepository tickerRepository,
            PriceCacheService priceCacheService,
            OrderService orderService
    ) {
        this.orderRepository = orderRepository;
        this.tickerRepository = tickerRepository;
        this.priceCacheService = priceCacheService;
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 5000)
    public void matchAndExpire() {
        List<Order> routedOrders = orderRepository.findByStatus(OrderStatus.ROUTED);
        Instant expiryCutoff = Instant.now().minus(EXPIRY_HOURS, ChronoUnit.HOURS);

        for (Order order : routedOrders) {
            try {
                if (order.getCreatedAt().isBefore(expiryCutoff)) {
                    order.setStatus(OrderStatus.EXPIRED);
                    orderRepository.save(order);
                    continue;
                }

                if (order.getType() != OrderType.LIMIT) continue;

                Ticker ticker = tickerRepository.findById(order.getTickerId()).orElse(null);
                if (ticker == null) continue;

                Optional<BigDecimal> currentPrice = priceCacheService.getLatestPrice(ticker.getSymbol());
                if (currentPrice.isEmpty()) continue;

                boolean marketable = switch (order.getSide()) {
                    case BUY -> currentPrice.get().compareTo(order.getLimitPrice()) <= 0;
                    case SELL -> currentPrice.get().compareTo(order.getLimitPrice()) >= 0;
                };

                if (marketable) {
                    orderService.fillPendingOrder(order, currentPrice.get());
                }
            } catch (Exception e) {
                log.error("Failed processing pending order {}", order.getId(), e);
            }
        }
    }
}

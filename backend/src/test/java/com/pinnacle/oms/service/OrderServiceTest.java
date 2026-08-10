package com.pinnacle.oms.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Order;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.OrderType;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.oms.dto.OrderResponse;
import com.pinnacle.oms.dto.PlaceOrderRequest;
import com.pinnacle.oms.service.RiskCheckService.RiskCheckResult;
import com.pinnacle.position.service.PositionService;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.OrderRepository;
import com.pinnacle.repository.TickerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TickerRepository tickerRepository;
    @Mock private RiskCheckService riskCheckService;
    @Mock private PositionService positionService;
    @Mock private PriceCacheService priceCacheService;

    private OrderService orderService;
    private UUID userId;
    private Account account;
    private Ticker ticker;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, accountRepository, tickerRepository,
                riskCheckService, positionService, priceCacheService);

        userId = UUID.randomUUID();
        account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setBuyingPower(new BigDecimal("10000.00"));

        ticker = new Ticker();
        ticker.setId(UUID.randomUUID());
        ticker.setSymbol("AAPL");
        ticker.setActive(true);
        ticker.setMinOrderSize(BigDecimal.ONE);
        ticker.setMaxOrderSize(new BigDecimal("100000"));

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(tickerRepository.findBySymbol("AAPL")).thenReturn(Optional.of(ticker));
    }

    private PlaceOrderRequest marketBuy(BigDecimal quantity) {
        return new PlaceOrderRequest("AAPL", OrderSide.BUY, OrderType.MARKET, quantity, null, null, null);
    }

    @Test
    @DisplayName("a market order fills immediately when a live price is available and risk passes")
    void marketOrderFillsImmediately() {
        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(riskCheckService.checkOrder(eq(account), eq(ticker), any(), any()))
                .thenReturn(new RiskCheckResult(true, null));

        OrderResponse response = orderService.placeOrder(userId, marketBuy(new BigDecimal("10")));

        assertThat(response.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(response.filledQuantity()).isEqualByComparingTo("10");
        verify(positionService).processFill(eq(account), eq(ticker), any(Order.class), eq(new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("an order is rejected, not silently dropped, when no live price is available yet")
    void rejectsWhenNoPriceAvailable() {
        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.empty());

        OrderResponse response = orderService.placeOrder(userId, marketBuy(new BigDecimal("10")));

        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(response.rejectionReason()).contains("No live price available");
        verifyNoInteractions(positionService);
    }

    @Test
    @DisplayName("an order is rejected with the risk check's own reason when the risk check fails")
    void rejectsWhenRiskCheckFails() {
        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(riskCheckService.checkOrder(eq(account), eq(ticker), any(), any()))
                .thenReturn(new RiskCheckResult(false, "Insufficient buying power: need 2000.00, have 100.00"));

        OrderResponse response = orderService.placeOrder(userId, marketBuy(new BigDecimal("10")));

        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(response.rejectionReason()).contains("Insufficient buying power");
        verifyNoInteractions(positionService);
    }

    @Test
    @DisplayName("a LIMIT order without a limitPrice is rejected as a bad request, before any risk check runs")
    void limitOrderRequiresLimitPrice() {
        PlaceOrderRequest request = new PlaceOrderRequest("AAPL", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10"), null, null, null);

        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limitPrice is required");

        verifyNoInteractions(riskCheckService);
    }

    @Test
    @DisplayName("a non-marketable LIMIT order stays ROUTED (pending) rather than filling or rejecting")
    void nonMarketableLimitOrderStaysRouted() {
        // BUY limit at $190 while the market trades at $200 — not marketable yet.
        PlaceOrderRequest request = new PlaceOrderRequest("AAPL", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10"), new BigDecimal("190.00"), null, null);

        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(riskCheckService.checkOrder(eq(account), eq(ticker), any(), any()))
                .thenReturn(new RiskCheckResult(true, null));

        OrderResponse response = orderService.placeOrder(userId, request);

        assertThat(response.status()).isEqualTo(OrderStatus.ROUTED);
        verifyNoInteractions(positionService);
    }

    @Test
    @DisplayName("a marketable LIMIT order fills immediately")
    void marketableLimitOrderFillsImmediately() {
        // BUY limit at $210 while the market trades at $200 — already marketable.
        PlaceOrderRequest request = new PlaceOrderRequest("AAPL", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10"), new BigDecimal("210.00"), null, null);

        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(riskCheckService.checkOrder(eq(account), eq(ticker), any(), any()))
                .thenReturn(new RiskCheckResult(true, null));

        OrderResponse response = orderService.placeOrder(userId, request);

        assertThat(response.status()).isEqualTo(OrderStatus.FILLED);
        // Fills at the current market price, not the limit price.
        verify(positionService).processFill(eq(account), eq(ticker), any(Order.class), eq(new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("cancelling a ROUTED order succeeds and transitions it to CANCELLED")
    void cancelRoutedOrderSucceeds() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(account.getId());
        order.setTickerId(ticker.getId());
        order.setStatus(OrderStatus.ROUTED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(tickerRepository.findById(ticker.getId())).thenReturn(Optional.of(ticker));

        OrderResponse response = orderService.cancelOrder(userId, order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelling a FILLED order is rejected — only pending (ROUTED) orders can be cancelled")
    void cancelFilledOrderFails() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(account.getId());
        order.setStatus(OrderStatus.FILLED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending (ROUTED) orders can be cancelled");
    }

    @Test
    @DisplayName("cancelling another account's order is rejected as not found, not as a permission error")
    void cancelAnotherAccountsOrderFails() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(UUID.randomUUID()); // belongs to a different account
        order.setStatus(OrderStatus.ROUTED);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, order.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("fillPendingOrder (used by the background matcher) fills at the given price and hands off to PositionService")
    void fillPendingOrderFillsAtGivenPrice() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountId(account.getId());
        order.setTickerId(ticker.getId());
        order.setSide(OrderSide.BUY);
        order.setType(OrderType.LIMIT);
        order.setQuantity(new BigDecimal("5"));
        order.setStatus(OrderStatus.ROUTED);

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(tickerRepository.findById(ticker.getId())).thenReturn(Optional.of(ticker));

        orderService.fillPendingOrder(order, new BigDecimal("195.00"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo("5");
        verify(positionService).processFill(account, ticker, order, new BigDecimal("195.00"));
    }

    @Test
    @DisplayName("the order is persisted at each state transition, not just once at the end")
    void persistsEachStateTransition() {
        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(riskCheckService.checkOrder(eq(account), eq(ticker), any(), any()))
                .thenReturn(new RiskCheckResult(true, null));

        OrderResponse response = orderService.placeOrder(userId, marketBuy(new BigDecimal("10")));

        // NOTE: Order is a mutable entity and every save() call here passes the same
        // reference, so an ArgumentCaptor would only ever see its final state (FILLED)
        // regardless of which call is inspected — asserting on captured intermediate
        // values would be a false signal, not a real check. Call count plus the final
        // state is what's actually verifiable here: NEW -> PENDING_RISK_CHECK -> ROUTED
        // -> FILLED is four saves in placeOrder's happy path.
        verify(orderRepository, times(4)).save(any(Order.class));
        assertThat(response.status()).isEqualTo(OrderStatus.FILLED);
    }
}

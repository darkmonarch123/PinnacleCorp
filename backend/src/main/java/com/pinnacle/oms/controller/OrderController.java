package com.pinnacle.oms.controller;

import com.pinnacle.oms.dto.OrderResponse;
import com.pinnacle.oms.dto.PlaceOrderRequest;
import com.pinnacle.oms.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.placeOrder(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(orderService.listOrders(userId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }

    // Order placement's IllegalArgumentException/IllegalStateException cases are all
    // client-facing validation problems (unknown ticker, wrong order state to cancel,
    // missing account) — surfaced as 400 with a plain message rather than a 500.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}

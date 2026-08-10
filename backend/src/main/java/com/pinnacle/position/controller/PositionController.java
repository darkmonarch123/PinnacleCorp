package com.pinnacle.position.controller;

import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.position.dto.ClosePositionRequest;
import com.pinnacle.position.dto.ModifyPositionRequest;
import com.pinnacle.position.dto.PositionResponse;
import com.pinnacle.position.service.PositionService;
import com.pinnacle.repository.PositionRepository;
import com.pinnacle.repository.TickerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;
    private final PositionRepository positionRepository;
    private final TickerRepository tickerRepository;
    private final PriceCacheService priceCacheService;

    public PositionController(
            PositionService positionService,
            PositionRepository positionRepository,
            TickerRepository tickerRepository,
            PriceCacheService priceCacheService
    ) {
        this.positionService = positionService;
        this.positionRepository = positionRepository;
        this.tickerRepository = tickerRepository;
        this.priceCacheService = priceCacheService;
    }

    @GetMapping
    public ResponseEntity<List<PositionResponse>> listPositions(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "true") boolean openOnly
    ) {
        return ResponseEntity.ok(positionService.listPositions(userId, openOnly));
    }

    @PatchMapping("/{positionId}")
    public ResponseEntity<PositionResponse> modifyPosition(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID positionId,
            @RequestBody ModifyPositionRequest request
    ) {
        return ResponseEntity.ok(positionService.modifyPosition(
                userId, positionId, request.stopLoss(), request.takeProfit(),
                request.clearStopLoss(), request.clearTakeProfit()
        ));
    }

    @PostMapping("/{positionId}/close")
    public ResponseEntity<PositionResponse> closePosition(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID positionId,
            @RequestBody(required = false) ClosePositionRequest request
    ) {
        var position = positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found"));
        String symbol = tickerRepository.findById(position.getTickerId())
                .orElseThrow(() -> new IllegalStateException("Ticker not found"))
                .getSymbol();

        BigDecimal currentPrice = priceCacheService.getLatestPrice(symbol)
                .orElseThrow(() -> new IllegalStateException("No live price available for " + symbol + " right now"));

        BigDecimal requestedQuantity = request != null ? request.quantity() : null;
        return ResponseEntity.ok(positionService.closePositionManually(userId, positionId, requestedQuantity, currentPrice));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}

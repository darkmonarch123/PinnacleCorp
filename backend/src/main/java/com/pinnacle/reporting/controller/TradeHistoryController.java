package com.pinnacle.reporting.controller;

import com.pinnacle.reporting.dto.EquityCurvePoint;
import com.pinnacle.reporting.dto.TradeResponse;
import com.pinnacle.reporting.dto.TradeStatsResponse;
import com.pinnacle.reporting.service.TradeHistoryService;
import com.pinnacle.reporting.service.TradeHistoryService.ResultFilter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/trades")
public class TradeHistoryController {

    private final TradeHistoryService tradeHistoryService;

    public TradeHistoryController(TradeHistoryService tradeHistoryService) {
        this.tradeHistoryService = tradeHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<TradeResponse>> listTrades(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "ALL") ResultFilter result
    ) {
        return ResponseEntity.ok(tradeHistoryService.listTrades(userId, symbol, from, to, result));
    }

    @GetMapping("/stats")
    public ResponseEntity<TradeStatsResponse> getStats(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(tradeHistoryService.getStats(userId));
    }

    @GetMapping("/equity-curve")
    public ResponseEntity<List<EquityCurvePoint>> getEquityCurve(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(tradeHistoryService.getEquityCurve(userId));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@AuthenticationPrincipal UUID userId) {
        String csv = tradeHistoryService.exportCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pinnacle-trades.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

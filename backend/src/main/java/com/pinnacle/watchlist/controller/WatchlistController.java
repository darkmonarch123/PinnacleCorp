package com.pinnacle.watchlist.controller;

import com.pinnacle.watchlist.dto.WatchlistItemResponse;
import com.pinnacle.watchlist.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    public record AddSymbolRequest(String symbol) {}

    @GetMapping
    public ResponseEntity<List<WatchlistItemResponse>> list(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(watchlistService.list(userId));
    }

    @PostMapping
    public ResponseEntity<WatchlistItemResponse> add(
            @AuthenticationPrincipal UUID userId,
            @RequestBody AddSymbolRequest request
    ) {
        return ResponseEntity.ok(watchlistService.add(userId, request.symbol()));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UUID userId, @PathVariable String symbol) {
        watchlistService.remove(userId, symbol);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}

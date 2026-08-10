package com.pinnacle.alerts.controller;

import com.pinnacle.alerts.dto.CreateAlertRequest;
import com.pinnacle.alerts.dto.PriceAlertResponse;
import com.pinnacle.alerts.service.PriceAlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    public PriceAlertController(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    @GetMapping
    public ResponseEntity<List<PriceAlertResponse>> list(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(priceAlertService.listAlerts(userId));
    }

    @PostMapping
    public ResponseEntity<PriceAlertResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateAlertRequest request
    ) {
        return ResponseEntity.ok(priceAlertService.createAlert(userId, request));
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID alertId) {
        priceAlertService.deleteAlert(userId, alertId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}

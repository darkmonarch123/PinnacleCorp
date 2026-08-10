package com.pinnacle.marketdata.controller;

import com.pinnacle.marketdata.dto.CandleDto;
import com.pinnacle.marketdata.dto.TickerResponse;
import com.pinnacle.marketdata.repository.PriceOhlcRepository;
import com.pinnacle.repository.TickerRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final PriceOhlcRepository priceOhlcRepository;
    private final TickerRepository tickerRepository;

    public MarketDataController(PriceOhlcRepository priceOhlcRepository, TickerRepository tickerRepository) {
        this.priceOhlcRepository = priceOhlcRepository;
        this.tickerRepository = tickerRepository;
    }

    /** All active tradable tickers (US + African) — the frontend's instrument picker should use this, not a hardcoded list. */
    @GetMapping("/tickers")
    public ResponseEntity<List<TickerResponse>> getTickers() {
        return ResponseEntity.ok(tickerRepository.findByActiveTrue().stream().map(TickerResponse::from).toList());
    }

    /**
     * GET /api/market-data/candles?symbol=AAPL&timeframe=1D&from=...&to=...&limit=500
     * from/to default to the trailing 30 days if omitted.
     */
    @GetMapping("/candles")
    public ResponseEntity<List<CandleDto>> getCandles(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "500") int limit
    ) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

        List<CandleDto> candles = priceOhlcRepository.findCandles(
                symbol.toUpperCase(), timeframe, effectiveFrom, effectiveTo, Math.min(limit, 5000)
        );

        return ResponseEntity.ok(candles);
    }
}

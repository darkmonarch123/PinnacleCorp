package com.pinnacle.marketdata.service;

import com.pinnacle.marketdata.repository.PriceOhlcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Rolls raw price_ticks up into price_ohlc candles for each supported
 * timeframe. Runs on a simple fixed schedule per timeframe rather than a
 * Timescale continuous aggregate, so the logic is visible and swappable —
 * moving this to a continuous aggregate later is a drop-in replacement for
 * this class, not a schema change.
 */
@Service
public class CandleAggregationService {

    private static final Logger log = LoggerFactory.getLogger(CandleAggregationService.class);

    private final PriceOhlcRepository priceOhlcRepository;
    private final List<String> watchedSymbols;

    public CandleAggregationService(
            PriceOhlcRepository priceOhlcRepository,
            @Value("${pinnacle.market-data.symbols}") String symbolsCsv
    ) {
        this.priceOhlcRepository = priceOhlcRepository;
        this.watchedSymbols = Arrays.asList(symbolsCsv.split(","));
    }

    @Scheduled(fixedRate = 60_000)
    public void rollUp1MinuteCandles() {
        rollUp("1m", Duration.ofMinutes(1), currentBucketStart(ChronoUnit.MINUTES));
    }

    @Scheduled(fixedRate = 5 * 60_000)
    public void rollUp5MinuteCandles() {
        Instant now = Instant.now();
        long epochMinute = now.getEpochSecond() / 60;
        Instant bucketStart = Instant.ofEpochSecond((epochMinute - (epochMinute % 5)) * 60);
        rollUp("5m", Duration.ofMinutes(5), bucketStart);
    }

    @Scheduled(fixedRate = 60 * 60_000)
    public void rollUp1HourCandles() {
        rollUp("1h", Duration.ofHours(1), currentBucketStart(ChronoUnit.HOURS));
    }

    @Scheduled(fixedRate = 24 * 60 * 60_000)
    public void rollUp1DayCandles() {
        rollUp("1D", Duration.ofDays(1), currentBucketStart(ChronoUnit.DAYS));
    }

    // 1W buckets are intentionally left to a follow-up: they need a
    // week-start convention (Mon vs Sun) decided with the rest of the team
    // before being baked into the aggregation logic.

    private Instant currentBucketStart(ChronoUnit unit) {
        return Instant.now().truncatedTo(unit);
    }

    private void rollUp(String timeframe, Duration bucketSize, Instant bucketStart) {
        Instant bucketEnd = bucketStart.plus(bucketSize);

        for (String symbol : watchedSymbols) {
            try {
                List<BigDecimal[]> ticks = priceOhlcRepository.findTickPricesInRange(symbol, bucketStart, bucketEnd);
                if (ticks.isEmpty()) continue;

                BigDecimal open = ticks.get(0)[0];
                BigDecimal close = ticks.get(ticks.size() - 1)[0];
                BigDecimal high = ticks.stream().map(t -> t[0]).max(BigDecimal::compareTo).orElse(open);
                BigDecimal low = ticks.stream().map(t -> t[0]).min(BigDecimal::compareTo).orElse(open);
                BigDecimal volume = ticks.stream().map(t -> t[1]).reduce(BigDecimal.ZERO, BigDecimal::add);

                priceOhlcRepository.upsertCandle(symbol, timeframe, bucketStart, open, high, low, close, volume);
            } catch (Exception e) {
                log.error("Failed to roll up {} candle for {}", timeframe, symbol, e);
            }
        }
    }
}

package com.pinnacle.marketdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinnacle.marketdata.client.MarketDataProviderClient;
import com.pinnacle.marketdata.dto.PriceQuote;
import com.pinnacle.marketdata.dto.PriceTickMessage;
import com.pinnacle.marketdata.repository.PriceTickRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * The core ingestion loop:
 *   1. Poll the configured provider for the watched symbol list
 *   2. Cache the latest price per symbol in Redis (fast reads for REST endpoints)
 *   3. Persist each quote as a raw tick in TimescaleDB (price_ticks)
 *   4. Broadcast the tick to any subscribed WebSocket clients
 *
 * Poll interval and symbol list are both configurable via application.yml /
 * env vars so this can run against a free-tier rate limit without code changes.
 */
@Service
public class MarketDataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataIngestionService.class);
    private static final String REDIS_KEY_PREFIX = "price:latest:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(10);

    private final MarketDataProviderClient providerClient;
    private final PriceTickRepository priceTickRepository;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> watchedSymbols;

    public MarketDataIngestionService(
            MarketDataProviderClient providerClient,
            PriceTickRepository priceTickRepository,
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            @Value("${pinnacle.market-data.symbols}") String symbolsCsv
    ) {
        this.providerClient = providerClient;
        this.priceTickRepository = priceTickRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.watchedSymbols = Arrays.asList(symbolsCsv.split(","));
    }

    @Scheduled(fixedRateString = "${pinnacle.market-data.poll-interval-ms}")
    public void pollAndBroadcast() {
        List<PriceQuote> quotes = providerClient.fetchQuotes(watchedSymbols);

        if (quotes.isEmpty()) {
            return;
        }

        for (PriceQuote quote : quotes) {
            try {
                BigDecimal previousPrice = readCachedPrice(quote.symbol());
                cacheLatestPrice(quote);
                priceTickRepository.insert(quote);
                broadcastTick(quote, previousPrice);
            } catch (Exception e) {
                log.error("Failed processing quote for {}", quote.symbol(), e);
            }
        }
    }

    private BigDecimal readCachedPrice(String symbol) {
        String raw = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + symbol);
        if (raw == null) return null;
        try {
            return objectMapper.readTree(raw).get("price").decimalValue();
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheLatestPrice(PriceQuote quote) throws Exception {
        String json = objectMapper.writeValueAsString(quote);
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + quote.symbol(), json, REDIS_TTL);
    }

    private void broadcastTick(PriceQuote quote, BigDecimal previousPrice) {
        BigDecimal changePercent = (previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) != 0)
                ? quote.price().subtract(previousPrice)
                        .divide(previousPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        PriceTickMessage message = new PriceTickMessage(
                quote.symbol(), quote.price(), changePercent, quote.timestamp()
        );

        messagingTemplate.convertAndSend("/topic/prices/" + quote.symbol(), message);
    }
}

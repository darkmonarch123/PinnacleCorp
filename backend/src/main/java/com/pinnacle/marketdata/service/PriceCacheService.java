package com.pinnacle.marketdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PriceCacheService {

    private static final String REDIS_KEY_PREFIX = "price:latest:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PriceCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Empty if no tick for this symbol has arrived yet (or it's fallen out of the 10-minute TTL). */
    public Optional<BigDecimal> getLatestPrice(String symbol) {
        String raw = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + symbol);
        if (raw == null) return Optional.empty();

        try {
            return Optional.of(objectMapper.readTree(raw).get("price").decimalValue());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

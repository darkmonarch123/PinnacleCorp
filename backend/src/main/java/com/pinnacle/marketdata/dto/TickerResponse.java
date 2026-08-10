package com.pinnacle.marketdata.dto;

import com.pinnacle.entity.Ticker;

import java.util.UUID;

public record TickerResponse(UUID id, String symbol, String exchange, String sector) {
    public static TickerResponse from(Ticker t) {
        return new TickerResponse(t.getId(), t.getSymbol(), t.getExchange(), t.getSector());
    }
}

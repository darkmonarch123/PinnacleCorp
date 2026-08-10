package com.pinnacle.marketdata.client;

import com.pinnacle.marketdata.dto.PriceQuote;

import java.util.List;

/**
 * Abstraction over the upstream market data vendor (Twelve Data, Alpha
 * Vantage, Finnhub, ...). Swapping providers means adding a new
 * implementation and changing pinnacle.market-data.provider — nothing else
 * in the ingestion pipeline depends on the vendor's API shape.
 */
public interface MarketDataProviderClient {
    List<PriceQuote> fetchQuotes(List<String> symbols);
}

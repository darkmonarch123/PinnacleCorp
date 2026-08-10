package com.pinnacle.marketdata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinnacle.marketdata.dto.PriceQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Twelve Data's /quote endpoint supports batching multiple symbols in one
 * call (comma-separated), which keeps us well inside free-tier rate limits
 * even at a few seconds' poll interval.
 *
 * https://twelvedata.com/docs#quote
 */
@Component
public class TwelveDataClient implements MarketDataProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataClient.class);
    private static final String BASE_URL = "https://api.twelvedata.com";

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TwelveDataClient(@Value("${pinnacle.market-data.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    @Override
    public List<PriceQuote> fetchQuotes(List<String> symbols) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("MARKET_DATA_API_KEY not set — skipping poll. Set it in .env to enable live quotes.");
            return List.of();
        }

        String symbolParam = String.join(",", symbols);

        try {
            String raw = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbolParam)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseQuotes(raw, symbols);
        } catch (Exception e) {
            log.error("Failed to fetch quotes from Twelve Data for symbols {}", symbolParam, e);
            return List.of();
        }
    }

    /**
     * Twelve Data returns a single quote object when one symbol is requested,
     * or a map keyed by symbol when multiple are requested — normalize both
     * shapes here.
     */
    private List<PriceQuote> parseQuotes(String raw, List<String> requestedSymbols) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        List<PriceQuote> quotes = new ArrayList<>();

        if (root.has("symbol")) {
            quotes.add(toQuote(root));
            return quotes;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode node = entry.getValue();
            if (node.has("close") || node.has("price")) {
                quotes.add(toQuote(node.has("symbol") ? node : withSymbol(node, entry.getKey())));
            }
        }
        return quotes;
    }

    private JsonNode withSymbol(JsonNode node, String symbol) {
        return ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("symbol", symbol);
    }

    private PriceQuote toQuote(JsonNode node) {
        String symbol = node.get("symbol").asText();
        BigDecimal price = new BigDecimal(
                node.has("close") ? node.get("close").asText() : node.get("price").asText()
        );
        BigDecimal volume = node.has("volume") && !node.get("volume").isNull()
                ? new BigDecimal(node.get("volume").asText())
                : null;

        return new PriceQuote(symbol, price, volume, Instant.now());
    }
}

package com.pinnacle.watchlist.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WatchlistItemResponse(
    UUID id,
    String symbol,
    BigDecimal lastPrice // null if no live tick has arrived for this symbol yet
) {}

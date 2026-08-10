package com.pinnacle.watchlist.service;

import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.WatchlistItem;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.repository.TickerRepository;
import com.pinnacle.repository.WatchlistItemRepository;
import com.pinnacle.watchlist.dto.WatchlistItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final TickerRepository tickerRepository;
    private final PriceCacheService priceCacheService;

    public WatchlistService(
            WatchlistItemRepository watchlistItemRepository,
            TickerRepository tickerRepository,
            PriceCacheService priceCacheService
    ) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.tickerRepository = tickerRepository;
        this.priceCacheService = priceCacheService;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> list(UUID userId) {
        return watchlistItemRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(item -> {
                    Ticker ticker = tickerRepository.findById(item.getTickerId())
                            .orElseThrow(() -> new IllegalStateException("Ticker missing for watchlist item " + item.getId()));
                    return new WatchlistItemResponse(
                            item.getId(), ticker.getSymbol(), priceCacheService.getLatestPrice(ticker.getSymbol()).orElse(null)
                    );
                })
                .toList();
    }

    @Transactional
    public WatchlistItemResponse add(UUID userId, String symbol) {
        Ticker ticker = tickerRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + symbol));

        WatchlistItem existing = watchlistItemRepository.findByUserIdAndTickerId(userId, ticker.getId()).orElse(null);
        if (existing != null) {
            return new WatchlistItemResponse(existing.getId(), ticker.getSymbol(),
                    priceCacheService.getLatestPrice(ticker.getSymbol()).orElse(null));
        }

        int nextOrder = watchlistItemRepository.findByUserIdOrderBySortOrderAsc(userId).size();

        WatchlistItem item = new WatchlistItem();
        item.setUserId(userId);
        item.setTickerId(ticker.getId());
        item.setSortOrder(nextOrder);
        watchlistItemRepository.save(item);

        return new WatchlistItemResponse(item.getId(), ticker.getSymbol(),
                priceCacheService.getLatestPrice(ticker.getSymbol()).orElse(null));
    }

    @Transactional
    public void remove(UUID userId, String symbol) {
        Ticker ticker = tickerRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + symbol));
        watchlistItemRepository.deleteByUserIdAndTickerId(userId, ticker.getId());
    }
}

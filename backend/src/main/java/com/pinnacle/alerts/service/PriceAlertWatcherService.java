package com.pinnacle.alerts.service;

import com.pinnacle.entity.PriceAlert;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.enums.AlertCondition;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.repository.PriceAlertRepository;
import com.pinnacle.repository.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Every 5 seconds, checks every active price alert against its ticker's live
 * cached price. On a match, the alert is deactivated (one-shot — it won't
 * fire again until the user re-creates it) and a notification is created.
 */
@Service
public class PriceAlertWatcherService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertWatcherService.class);

    private final PriceAlertRepository priceAlertRepository;
    private final TickerRepository tickerRepository;
    private final PriceCacheService priceCacheService;
    private final NotificationService notificationService;

    public PriceAlertWatcherService(
            PriceAlertRepository priceAlertRepository,
            TickerRepository tickerRepository,
            PriceCacheService priceCacheService,
            NotificationService notificationService
    ) {
        this.priceAlertRepository = priceAlertRepository;
        this.tickerRepository = tickerRepository;
        this.priceCacheService = priceCacheService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void checkAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByActiveTrue();

        for (PriceAlert alert : activeAlerts) {
            try {
                evaluate(alert);
            } catch (Exception e) {
                log.error("Failed evaluating price alert {}", alert.getId(), e);
            }
        }
    }

    private void evaluate(PriceAlert alert) {
        Ticker ticker = tickerRepository.findById(alert.getTickerId()).orElse(null);
        if (ticker == null) return;

        Optional<BigDecimal> priceOpt = priceCacheService.getLatestPrice(ticker.getSymbol());
        if (priceOpt.isEmpty()) return;
        BigDecimal price = priceOpt.get();

        boolean triggered = alert.getCondition() == AlertCondition.ABOVE
                ? price.compareTo(alert.getTargetPrice()) >= 0
                : price.compareTo(alert.getTargetPrice()) <= 0;

        if (!triggered) return;

        alert.setActive(false);
        alert.setTriggeredAt(Instant.now());
        priceAlertRepository.save(alert);

        String direction = alert.getCondition() == AlertCondition.ABOVE ? "above" : "below";
        String message = ticker.getSymbol() + " crossed " + direction + " $" + alert.getTargetPrice() + " (now $" + price + ")";

        notificationService.notify(alert.getUserId(), message, alert.getId());
    }
}

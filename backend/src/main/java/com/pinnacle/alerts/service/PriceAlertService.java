package com.pinnacle.alerts.service;

import com.pinnacle.alerts.dto.CreateAlertRequest;
import com.pinnacle.alerts.dto.PriceAlertResponse;
import com.pinnacle.entity.PriceAlert;
import com.pinnacle.entity.Ticker;
import com.pinnacle.repository.PriceAlertRepository;
import com.pinnacle.repository.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final TickerRepository tickerRepository;

    public PriceAlertService(PriceAlertRepository priceAlertRepository, TickerRepository tickerRepository) {
        this.priceAlertRepository = priceAlertRepository;
        this.tickerRepository = tickerRepository;
    }

    @Transactional
    public PriceAlertResponse createAlert(UUID userId, CreateAlertRequest request) {
        Ticker ticker = tickerRepository.findBySymbol(request.symbol().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker: " + request.symbol()));

        PriceAlert alert = new PriceAlert();
        alert.setUserId(userId);
        alert.setTickerId(ticker.getId());
        alert.setTargetPrice(request.targetPrice());
        alert.setCondition(request.condition());
        priceAlertRepository.save(alert);

        return PriceAlertResponse.from(alert, ticker.getSymbol());
    }

    @Transactional(readOnly = true)
    public List<PriceAlertResponse> listAlerts(UUID userId) {
        return priceAlertRepository.findByUserId(userId).stream()
                .map(a -> PriceAlertResponse.from(a, resolveSymbol(a.getTickerId())))
                .toList();
    }

    @Transactional
    public void deleteAlert(UUID userId, UUID alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        if (!alert.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Alert not found");
        }
        priceAlertRepository.delete(alert);
    }

    private String resolveSymbol(UUID tickerId) {
        return tickerRepository.findById(tickerId).map(Ticker::getSymbol).orElse("UNKNOWN");
    }
}

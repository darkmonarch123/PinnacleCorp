package com.pinnacle.account.controller;

import com.pinnacle.account.dto.CurrencyRateResponse;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * currency_rates isn't a JPA entity (simple lookup table, JdbcTemplate is
 * enough) — same pattern as price_ticks/price_ohlc elsewhere in the app.
 * Fixed/mock rates for the demo, not live FX.
 */
@RestController
@RequestMapping("/api/currency-rates")
public class CurrencyRateController {

    private final NamedParameterJdbcTemplate jdbc;

    public CurrencyRateController(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<CurrencyRateResponse> list() {
        return jdbc.query(
                "SELECT currency, usd_rate FROM currency_rates ORDER BY currency",
                (rs, rowNum) -> new CurrencyRateResponse(rs.getString("currency"), rs.getBigDecimal("usd_rate"))
        );
    }
}

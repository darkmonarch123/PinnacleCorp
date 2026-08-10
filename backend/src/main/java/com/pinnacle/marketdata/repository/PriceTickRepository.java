package com.pinnacle.marketdata.repository;

import com.pinnacle.marketdata.dto.PriceQuote;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

/**
 * price_ticks is a TimescaleDB hypertable with a composite (time, symbol) key
 * and no single-column primary key, so it's accessed via JdbcTemplate rather
 * than a JPA entity/repository.
 */
@Repository
public class PriceTickRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PriceTickRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(PriceQuote quote) {
        String sql = """
            INSERT INTO price_ticks (time, symbol, price, volume)
            VALUES (:time, :symbol, :price, :volume)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("time", Timestamp.from(quote.timestamp()))
                .addValue("symbol", quote.symbol())
                .addValue("price", quote.price())
                .addValue("volume", quote.volume());

        jdbc.update(sql, params);
    }
}

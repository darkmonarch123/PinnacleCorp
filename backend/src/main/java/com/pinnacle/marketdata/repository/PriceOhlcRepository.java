package com.pinnacle.marketdata.repository;

import com.pinnacle.marketdata.dto.CandleDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class PriceOhlcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PriceOhlcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertCandle(String symbol, String timeframe, Instant bucketStart,
                              BigDecimal open, BigDecimal high, BigDecimal low,
                              BigDecimal close, BigDecimal volume) {
        // A given (symbol, timeframe, time) bucket is rewritten as new ticks
        // arrive within it, so this is an upsert keyed on that triple.
        String sql = """
            INSERT INTO price_ohlc (time, symbol, timeframe, open, high, low, close, volume)
            VALUES (:time, :symbol, CAST(:timeframe AS timeframe), :open, :high, :low, :close, :volume)
            ON CONFLICT (symbol, timeframe, time) DO UPDATE SET
                high = GREATEST(price_ohlc.high, EXCLUDED.high),
                low = LEAST(price_ohlc.low, EXCLUDED.low),
                close = EXCLUDED.close,
                volume = COALESCE(price_ohlc.volume, 0) + COALESCE(EXCLUDED.volume, 0)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("time", Timestamp.from(bucketStart))
                .addValue("symbol", symbol)
                .addValue("timeframe", timeframe)
                .addValue("open", open)
                .addValue("high", high)
                .addValue("low", low)
                .addValue("close", close)
                .addValue("volume", volume);

        jdbc.update(sql, params);
    }

    public List<CandleDto> findCandles(String symbol, String timeframe, Instant from, Instant to, int limit) {
        String sql = """
            SELECT time, open, high, low, close, volume
            FROM price_ohlc
            WHERE symbol = :symbol
              AND timeframe = CAST(:timeframe AS timeframe)
              AND time BETWEEN :from AND :to
            ORDER BY time ASC
            LIMIT :limit
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("symbol", symbol)
                .addValue("timeframe", timeframe)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to))
                .addValue("limit", limit);

        return jdbc.query(sql, params, (rs, rowNum) -> new CandleDto(
                rs.getTimestamp("time").toInstant(),
                rs.getBigDecimal("open"),
                rs.getBigDecimal("high"),
                rs.getBigDecimal("low"),
                rs.getBigDecimal("close"),
                rs.getBigDecimal("volume")
        ));
    }

    /** Raw ticks that fall within [bucketStart, bucketEnd) for a given symbol, used by the rollup job. */
    public List<BigDecimal[]> findTickPricesInRange(String symbol, Instant bucketStart, Instant bucketEnd) {
        String sql = """
            SELECT price, COALESCE(volume, 0) AS volume
            FROM price_ticks
            WHERE symbol = :symbol AND time >= :start AND time < :end
            ORDER BY time ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("symbol", symbol)
                .addValue("start", Timestamp.from(bucketStart))
                .addValue("end", Timestamp.from(bucketEnd));

        return jdbc.query(sql, params, (rs, rowNum) -> new BigDecimal[]{
                rs.getBigDecimal("price"), rs.getBigDecimal("volume")
        });
    }
}

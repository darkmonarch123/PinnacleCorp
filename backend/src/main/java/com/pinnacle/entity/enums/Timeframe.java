package com.pinnacle.entity.enums;

public enum Timeframe {
    ONE_MINUTE("1m"),
    FIVE_MINUTE("5m"),
    ONE_HOUR("1h"),
    ONE_DAY("1D"),
    ONE_WEEK("1W");

    private final String code;

    Timeframe(String code) { this.code = code; }

    public String getCode() { return code; }
}

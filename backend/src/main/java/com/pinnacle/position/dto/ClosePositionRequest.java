package com.pinnacle.position.dto;

import java.math.BigDecimal;

/** quantity is optional — omit it (or send null) to close the full remaining quantity. */
public record ClosePositionRequest(BigDecimal quantity) {}

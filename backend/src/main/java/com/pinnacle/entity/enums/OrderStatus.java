package com.pinnacle.entity.enums;

/**
 * Order lifecycle state machine:
 * NEW -> PENDING_RISK_CHECK -> ROUTED -> PARTIALLY_FILLED -> FILLED
 * with branches to CANCELLED, EXPIRED, REJECTED from any pre-terminal state.
 */
public enum OrderStatus {
    NEW,
    PENDING_RISK_CHECK,
    ROUTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    EXPIRED,
    REJECTED
}

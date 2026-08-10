package com.pinnacle.entity.enums;

/**
 * Position lifecycle: OPEN -> MODIFIED / PARTIAL_CLOSE -> PENDING_CLOSE -> CLOSED
 */
public enum PositionStatus {
    OPEN,
    MODIFIED,
    PARTIAL_CLOSE,
    PENDING_CLOSE,
    CLOSED
}

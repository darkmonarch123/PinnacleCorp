package com.pinnacle.funding.dto;

import java.math.BigDecimal;

/**
 * usdEquivalent is only used (and required) when confirming a crypto
 * request — the admin states what the claimed crypto amount is worth in USD
 * for the ledger credit, since this app doesn't do live crypto pricing.
 */
public record AdminDecisionRequest(String adminNote, BigDecimal usdEquivalent) {}

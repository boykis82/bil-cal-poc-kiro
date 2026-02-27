package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;

/**
 * 선납내역 모델 (placeholder).
 */
public record PrepaidRecord(
        String prepaidId,
        BigDecimal amount) {
}

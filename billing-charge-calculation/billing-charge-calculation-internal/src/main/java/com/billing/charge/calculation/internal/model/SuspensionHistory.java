package com.billing.charge.calculation.internal.model;

import java.time.LocalDate;

/**
 * 정지이력 모델 (placeholder).
 */
public record SuspensionHistory(
        String suspensionId,
        LocalDate startDate,
        LocalDate endDate,
        String reason) {
}

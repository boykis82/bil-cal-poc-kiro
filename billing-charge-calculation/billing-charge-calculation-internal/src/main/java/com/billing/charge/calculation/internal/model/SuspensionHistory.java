package com.billing.charge.calculation.internal.model;

import java.time.LocalDate;

/**
 * 정지이력 모델 (placeholder).
 */
public record SuspensionHistory(
        /** 계약ID (데이터 로더에서 계약ID 기준 그룹핑에 사용) */
        String contractId,
        String suspensionId,
        LocalDate startDate,
        LocalDate endDate,
        String reason) {
}

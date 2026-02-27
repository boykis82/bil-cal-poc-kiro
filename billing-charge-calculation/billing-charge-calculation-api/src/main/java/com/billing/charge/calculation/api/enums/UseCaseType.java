package com.billing.charge.calculation.api.enums;

/**
 * 요금 계산 유스케이스 유형.
 */
public enum UseCaseType {
    REGULAR_BILLING, // 정기청구
    REALTIME_QUERY, // 실시간 요금 조회
    ESTIMATE_QUERY, // 예상 요금 조회
    QUOTATION_QUERY // 견적 요금 조회
}

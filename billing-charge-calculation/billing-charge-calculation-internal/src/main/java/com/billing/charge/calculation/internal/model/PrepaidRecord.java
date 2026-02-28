package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;

/**
 * 선납내역 모델.
 * 선납반제 처리에 필요한 선납내역 정보를 담는다.
 */
public record PrepaidRecord(
        /** 계약ID (데이터 로더에서 계약ID 기준 그룹핑에 사용) */
        String contractId,
        String prepaidId,
        BigDecimal amount) {
}

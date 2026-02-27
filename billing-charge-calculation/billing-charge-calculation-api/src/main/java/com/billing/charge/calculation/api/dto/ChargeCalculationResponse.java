package com.billing.charge.calculation.api.dto;

import java.util.List;

/**
 * 요금 계산 응답 DTO.
 * 계약별 요금 계산 결과 리스트를 포함.
 */
public record ChargeCalculationResponse(
        List<ContractChargeResult> results) {
    public static ChargeCalculationResponse of(List<ContractChargeResult> results) {
        return new ChargeCalculationResponse(results);
    }
}

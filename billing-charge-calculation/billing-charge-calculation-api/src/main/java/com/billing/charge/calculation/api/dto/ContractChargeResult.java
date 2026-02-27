package com.billing.charge.calculation.api.dto;

import com.billing.charge.calculation.api.model.FlatChargeResult;

import java.util.List;

/**
 * 계약별 요금 계산 결과 DTO.
 */
public record ContractChargeResult(
        String contractId,
        List<FlatChargeResult> chargeResults) {
}

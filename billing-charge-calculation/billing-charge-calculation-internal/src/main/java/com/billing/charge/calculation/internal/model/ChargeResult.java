package com.billing.charge.calculation.internal.model;

import com.billing.charge.calculation.api.model.FlatChargeResult;

import java.util.List;

/**
 * 요금 계산 최종 결과 모델.
 */
public record ChargeResult(
        String contractId,
        List<FlatChargeResult> chargeResults) {
}

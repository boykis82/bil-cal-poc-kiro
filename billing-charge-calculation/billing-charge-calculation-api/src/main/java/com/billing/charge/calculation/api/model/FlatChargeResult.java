package com.billing.charge.calculation.api.model;

import com.billing.charge.calculation.api.enums.ChargeItemType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 기간 미존재 요금 계산 결과.
 * 일회성, 통화료, 할인2, 부가세 등 기간 정보가 불필요한 요금 항목의 결과.
 */
public record FlatChargeResult(
        String chargeItemCode,
        String chargeItemName,
        ChargeItemType chargeItemType,
        BigDecimal amount,
        String currencyCode,
        Map<String, Object> metadata) {
    public static FlatChargeResult of(String code, BigDecimal amount) {
        return new FlatChargeResult(code, null, null, amount, "KRW", Map.of());
    }
}

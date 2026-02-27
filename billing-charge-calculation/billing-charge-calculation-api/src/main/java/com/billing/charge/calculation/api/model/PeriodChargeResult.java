package com.billing.charge.calculation.api.model;

import com.billing.charge.calculation.api.enums.ChargeItemType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 기간 존재 요금 계산 결과.
 * 월정액, 할인1 등 기간 정보가 필요한 요금 항목의 결과.
 */
public record PeriodChargeResult(
        String chargeItemCode,
        String chargeItemName,
        ChargeItemType chargeItemType,
        BigDecimal amount,
        LocalDate periodFrom,
        LocalDate periodTo,
        String currencyCode,
        Map<String, Object> metadata) {
    public static PeriodChargeResult of(String code, ChargeItemType type,
            BigDecimal amount, LocalDate from, LocalDate to) {
        return new PeriodChargeResult(code, null, type, amount, from, to, "KRW", Map.of());
    }
}

package com.billing.charge.calculation.internal.util;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 13: Period→Flat 압축 금액 보존
 *
 * 임의의 PeriodChargeResult 목록에 대해, PeriodToFlatCompactor.compact() 수행 후
 * 항목코드별 합산 금액은 원본 PeriodChargeResult의 항목코드별 합산 금액과 일치해야 한다.
 *
 * Validates: Requirements 8.3
 */
@Tag("Feature: billing-charge-calculation, Property 13: Period→Flat 압축 금액 보존")
class PeriodToFlatCompactorPropertyTest {

    @Property(tries = 100)
    void compactionShouldPreserveTotalAmountPerItem(
            @ForAll("periodChargeResults") List<PeriodChargeResult> periodResults) {

        // 원본 항목코드별 합산
        Map<String, BigDecimal> originalSums = periodResults.stream()
                .collect(Collectors.groupingBy(
                        PeriodChargeResult::chargeItemCode,
                        Collectors.reducing(BigDecimal.ZERO, PeriodChargeResult::amount, BigDecimal::add)));

        // 압축 수행
        List<FlatChargeResult> compacted = PeriodToFlatCompactor.compact(periodResults);

        // 압축 후 항목코드별 합산
        Map<String, BigDecimal> compactedSums = compacted.stream()
                .collect(Collectors.toMap(
                        FlatChargeResult::chargeItemCode,
                        FlatChargeResult::amount));

        // 금액 보존 검증
        assertThat(compactedSums).isEqualTo(originalSums);
    }

    @Provide
    Arbitrary<List<PeriodChargeResult>> periodChargeResults() {
        Arbitrary<String> itemCodes = Arbitraries.of("MONTHLY_001", "MONTHLY_002", "DISCOUNT_001");
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(2);
        Arbitrary<LocalDate> dates = Combinators.combine(
                Arbitraries.integers().between(2020, 2026),
                Arbitraries.integers().between(1, 12),
                Arbitraries.integers().between(1, 28)).as(LocalDate::of);

        return Combinators.combine(itemCodes, amounts, dates,
                Arbitraries.integers().between(1, 30))
                .as((code, amount, start, days) -> PeriodChargeResult.of(code, ChargeItemType.MONTHLY_FEE, amount,
                        start, start.plusDays(days)))
                .list().ofMinSize(1).ofMaxSize(20);
    }
}

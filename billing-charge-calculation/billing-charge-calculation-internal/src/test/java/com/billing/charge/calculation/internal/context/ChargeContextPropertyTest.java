package com.billing.charge.calculation.internal.context;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.model.ChargeInput;
import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 4: ChargeContext 결과 누적 및 참조
 *
 * 임의의 Step 시퀀스에서, 이전 Step이 ChargeContext에 추가한 PeriodChargeResult 또는
 * FlatChargeResult는 후속 Step에서 항상 조회 가능해야 하며,
 * 추가된 결과가 유실되거나 변조되지 않아야 한다.
 *
 * Validates: Requirements 3.4, 15.3
 */
@Tag("Feature: billing-charge-calculation, Property 4: ChargeContext 결과 누적 및 참조")
class ChargeContextPropertyTest {

    @Property(tries = 100)
    void addedPeriodResultsMustBeQueryableAndUnmodified(
            @ForAll("periodChargeResults") List<PeriodChargeResult> results) {

        ChargeContext context = createContext();

        // 시뮬레이션: 여러 Step이 순차적으로 결과를 추가
        for (PeriodChargeResult result : results) {
            context.addPeriodResult(result);
        }

        // 모든 추가된 결과가 조회 가능해야 함
        List<PeriodChargeResult> retrieved = context.getPeriodResults();
        assertThat(retrieved).hasSize(results.size());

        // 추가된 결과가 변조되지 않아야 함 (순서 및 내용 동일)
        for (int i = 0; i < results.size(); i++) {
            assertThat(retrieved.get(i)).isEqualTo(results.get(i));
        }
    }

    @Property(tries = 100)
    void addedFlatResultsMustBeQueryableAndUnmodified(
            @ForAll("flatChargeResults") List<FlatChargeResult> results) {

        ChargeContext context = createContext();

        for (FlatChargeResult result : results) {
            context.addFlatResult(result);
        }

        List<FlatChargeResult> retrieved = context.getFlatResults();
        assertThat(retrieved).hasSize(results.size());

        for (int i = 0; i < results.size(); i++) {
            assertThat(retrieved.get(i)).isEqualTo(results.get(i));
        }
    }

    @Property(tries = 100)
    void resultsByTypeShouldReturnOnlyMatchingType(
            @ForAll("periodChargeResults") List<PeriodChargeResult> results) {

        ChargeContext context = createContext();
        for (PeriodChargeResult result : results) {
            context.addPeriodResult(result);
        }

        // 각 ChargeItemType에 대해 필터링 결과 검증
        for (ChargeItemType type : ChargeItemType.values()) {
            List<PeriodChargeResult> filtered = context.getPeriodResultsByType(type);
            long expectedCount = results.stream()
                    .filter(r -> r.chargeItemType() == type)
                    .count();
            assertThat(filtered).hasSize((int) expectedCount);
            assertThat(filtered).allMatch(r -> r.chargeItemType() == type);
        }
    }

    @Property(tries = 100)
    void subsequentAdditionsDoNotAffectPreviousResults(
            @ForAll("periodChargeResults") @Size(min = 1, max = 10) List<PeriodChargeResult> firstBatch,
            @ForAll("flatChargeResults") @Size(min = 1, max = 10) List<FlatChargeResult> secondBatch) {

        ChargeContext context = createContext();

        // Step 1: period 결과 추가
        for (PeriodChargeResult result : firstBatch) {
            context.addPeriodResult(result);
        }

        // Step 1 결과 스냅샷
        List<PeriodChargeResult> afterStep1 = List.copyOf(context.getPeriodResults());

        // Step 2: flat 결과 추가 (이전 period 결과에 영향 없어야 함)
        for (FlatChargeResult result : secondBatch) {
            context.addFlatResult(result);
        }

        // Step 1에서 추가한 period 결과가 그대로 유지되어야 함
        assertThat(context.getPeriodResults()).isEqualTo(afterStep1);
        // Step 2에서 추가한 flat 결과도 조회 가능해야 함
        assertThat(context.getFlatResults()).hasSize(secondBatch.size());
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<PeriodChargeResult>> periodChargeResults() {
        return periodChargeResult().list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<FlatChargeResult>> flatChargeResults() {
        return flatChargeResult().list().ofMinSize(0).ofMaxSize(20);
    }

    private Arbitrary<PeriodChargeResult> periodChargeResult() {
        Arbitrary<String> codes = Arbitraries.of("MONTHLY_001", "MONTHLY_002", "DISCOUNT_001", "DISCOUNT_002");
        Arbitrary<ChargeItemType> types = Arbitraries.of(ChargeItemType.values());
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(2);
        Arbitrary<LocalDate> dates = Arbitraries.of(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 6, 1));

        return Combinators.combine(codes, types, amounts, dates)
                .as((code, type, amount, start) -> PeriodChargeResult.of(code, type, amount, start,
                        start.plusDays(29)));
    }

    private Arbitrary<FlatChargeResult> flatChargeResult() {
        Arbitrary<String> codes = Arbitraries.of("ONETIME_001", "USAGE_001", "VAT_001", "LATE_001");
        Arbitrary<ChargeItemType> types = Arbitraries.of(ChargeItemType.values());
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("50000"))
                .ofScale(2);

        return Combinators.combine(codes, types, amounts)
                .as((code, type, amount) -> new FlatChargeResult(code, null, type, amount, "KRW", Map.of()));
    }

    private ChargeContext createContext() {
        ContractInfo contractInfo = new ContractInfo(
                "CONTRACT-001", "SUB-001", "PROD-001",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        ChargeInput chargeInput = ChargeInput.builder().build();
        return ChargeContext.of("TENANT-001", contractInfo, chargeInput);
    }
}

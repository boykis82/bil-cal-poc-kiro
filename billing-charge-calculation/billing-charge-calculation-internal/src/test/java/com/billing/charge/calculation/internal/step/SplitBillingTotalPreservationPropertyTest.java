package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 14: 분리과금 총액 보존
 *
 * 임의의 요금 계산 결과에 대해, 분리과금 처리 후 분리된 항목들의 총합은
 * 분리과금 처리 전 총액과 일치해야 한다.
 * SplitBillingStep은 음수(차감) + 양수(부과) 쌍을 생성하므로 순 변동은 0이어야 한다.
 *
 * **Validates: Requirements 14.1**
 */
class SplitBillingTotalPreservationPropertyTest {

    private final SplitBillingStep splitBillingStep = new SplitBillingStep();

    // Feature: billing-charge-calculation, Property 14: 분리과금 총액 보존
    @Property(tries = 100)
    void splitBillingShouldPreserveTotalAmount(
            @ForAll("splitBillingContextsWithCharges") ChargeContext context) {

        // 분리과금 처리 전 전체 FlatChargeResult 합계
        BigDecimal totalBefore = context.getFlatResults().stream()
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        splitBillingStep.process(context);

        // 분리과금 처리 후 전체 FlatChargeResult 합계
        BigDecimal totalAfter = context.getFlatResults().stream()
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총액이 보존되어야 한다 (분리과금 항목의 순 변동 = 0)
        assertThat(totalAfter)
                .as("분리과금 처리 후 총액(%s)이 처리 전 총액(%s)과 일치해야 한다",
                        totalAfter, totalBefore)
                .isEqualByComparingTo(totalBefore);
    }

    // Feature: billing-charge-calculation, Property 14: 분리과금 항목 쌍 검증
    @Property(tries = 100)
    void splitBillingItemsShouldSumToZero(
            @ForAll("splitBillingContextsWithCharges") ChargeContext context) {

        int flatCountBefore = context.getFlatResults().size();

        splitBillingStep.process(context);

        // 새로 추가된 SPLIT_BILLING 항목만 추출
        List<FlatChargeResult> splitResults = context.getFlatResults().stream()
                .skip(flatCountBefore)
                .filter(r -> r.chargeItemType() == ChargeItemType.SPLIT_BILLING)
                .toList();

        if (!splitResults.isEmpty()) {
            // 분리과금 항목들의 합은 0이어야 한다 (차감 + 부과 = 0)
            BigDecimal splitSum = splitResults.stream()
                    .map(FlatChargeResult::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(splitSum)
                    .as("분리과금 항목들의 합(%s)은 0이어야 한다", splitSum)
                    .isEqualByComparingTo(BigDecimal.ZERO);

            // 정확히 2개의 항목이 생성되어야 한다 (차감 1개 + 부과 1개)
            assertThat(splitResults).hasSize(2);
        }
    }

    @Provide
    Arbitrary<ChargeContext> splitBillingContextsWithCharges() {
        return Combinators.combine(
                Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf),
                Arbitraries.integers().between(1, 99).map(BigDecimal::valueOf),
                Arbitraries.integers().between(0, 50000).map(BigDecimal::valueOf))
                .as((monthlyFee, splitRatio, usageFee) -> {
                    LocalDate now = LocalDate.of(2024, 6, 15);

                    SubscriptionInfo sub = new SubscriptionInfo(
                            "SUB001", "PROD001", "SUBSCRIBER001",
                            BigDecimal.ZERO, now.minusMonths(1), now, "N", null,
                            List.of(), List.of(), List.of(), splitRatio, "TARGET_001");

                    ChargeContext ctx = ChargeContext.of(
                            "TENANT_01",
                            new ContractInfo("C001", "SUB001", "PROD001", now.minusMonths(1), now),
                            ChargeInput.builder().subscriptionInfo(sub).build());

                    // 다양한 원금성 항목 사전 추가
                    ctx.addFlatResult(new FlatChargeResult(
                            "PROD001", "월정액", ChargeItemType.MONTHLY_FEE,
                            monthlyFee, "KRW", Map.of()));

                    if (usageFee.compareTo(BigDecimal.ZERO) > 0) {
                        ctx.addFlatResult(new FlatChargeResult(
                                "USAGE001", "통화료", ChargeItemType.USAGE_FEE,
                                usageFee, "KRW", Map.of()));
                    }

                    return ctx;
                });
    }
}

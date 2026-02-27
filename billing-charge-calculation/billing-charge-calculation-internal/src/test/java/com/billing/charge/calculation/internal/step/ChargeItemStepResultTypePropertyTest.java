package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 11: 요금 항목 결과 유형 일관성 (원금성)
 *
 * MonthlyFeeStep은 PeriodChargeResult만 생성해야 하고,
 * OneTimeFeeStep과 UsageFeeStep은 FlatChargeResult만 생성해야 한다.
 *
 * **Validates: Requirements 5.2, 6.2, 7.2**
 */
class ChargeItemStepResultTypePropertyTest {

    private final MonthlyFeeStep monthlyFeeStep = new MonthlyFeeStep();
    private final OneTimeFeeStep oneTimeFeeStep = new OneTimeFeeStep();
    private final UsageFeeStep usageFeeStep = new UsageFeeStep();

    // Feature: billing-charge-calculation, Property 11: MonthlyFeeStep →
    // PeriodChargeResult only
    @Property(tries = 100)
    void monthlyFeeStepShouldOnlyProducePeriodResults(
            @ForAll("monthlyFeeContexts") ChargeContext context) {

        int flatBefore = context.getFlatResults().size();

        monthlyFeeStep.process(context);

        // MonthlyFeeStep은 FlatChargeResult를 추가하지 않아야 한다
        assertThat(context.getFlatResults()).hasSize(flatBefore);

        // 생성된 PeriodChargeResult는 모두 MONTHLY_FEE 타입이어야 한다
        for (PeriodChargeResult result : context.getPeriodResults()) {
            assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.MONTHLY_FEE);
        }
    }

    // Feature: billing-charge-calculation, Property 11: OneTimeFeeStep →
    // FlatChargeResult only
    @Property(tries = 100)
    void oneTimeFeeStepShouldOnlyProduceFlatResults(
            @ForAll("oneTimeFeeContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        oneTimeFeeStep.process(context);

        // OneTimeFeeStep은 PeriodChargeResult를 추가하지 않아야 한다
        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        // 생성된 FlatChargeResult는 모두 ONE_TIME_FEE 타입이어야 한다
        for (FlatChargeResult result : context.getFlatResults()) {
            assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.ONE_TIME_FEE);
        }
    }

    // Feature: billing-charge-calculation, Property 11: UsageFeeStep →
    // FlatChargeResult only
    @Property(tries = 100)
    void usageFeeStepShouldOnlyProduceFlatResults(
            @ForAll("usageFeeContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        usageFeeStep.process(context);

        // UsageFeeStep은 PeriodChargeResult를 추가하지 않아야 한다
        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        // 생성된 FlatChargeResult는 모두 USAGE_FEE 타입이어야 한다
        for (FlatChargeResult result : context.getFlatResults()) {
            assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.USAGE_FEE);
        }
    }

    @Provide
    Arbitrary<ChargeContext> monthlyFeeContexts() {
        return Combinators.combine(
                Arbitraries.integers().between(2020, 2025),
                Arbitraries.integers().between(1, 12),
                Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf)).as((year, month, rate) -> {
                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end = start.plusMonths(1).minusDays(1);

                    SubscriptionInfo sub = new SubscriptionInfo(
                            "SUB001", "PROD001", "SUBSCRIBER001",
                            rate, start, end, "N", null, List.of(), List.of(), List.of(), null, null);

                    return ChargeContext.of(
                            "TENANT_01",
                            new ContractInfo("C001", "SUB001", "PROD001", start, end),
                            ChargeInput.builder().subscriptionInfo(sub).suspensionHistories(List.of()).build());
                });
    }

    @Provide
    Arbitrary<ChargeContext> oneTimeFeeContexts() {
        Arbitrary<List<SubscriptionInfo.OneTimeFeeItem>> feeItems = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.integers().between(100, 50000).map(BigDecimal::valueOf))
                .as((code, amount) -> new SubscriptionInfo.OneTimeFeeItem(code, "일회성_" + code, amount))
                .list().ofMinSize(1).ofMaxSize(5);

        return feeItems.map(items -> {
            LocalDate now = LocalDate.of(2024, 1, 1);
            SubscriptionInfo sub = new SubscriptionInfo(
                    "SUB001", "PROD001", "SUBSCRIBER001",
                    BigDecimal.ZERO, now, now.plusMonths(1), "N", null, items, List.of(), List.of(), null, null);
            return ChargeContext.of(
                    "TENANT_01",
                    new ContractInfo("C001", "SUB001", "PROD001", now, now.plusMonths(1)),
                    ChargeInput.builder().subscriptionInfo(sub).suspensionHistories(List.of()).build());
        });
    }

    @Provide
    Arbitrary<ChargeContext> usageFeeContexts() {
        Arbitrary<List<SubscriptionInfo.UsageFeeItem>> usageItems = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(10),
                Arbitraries.integers().between(1, 1000).map(BigDecimal::valueOf),
                Arbitraries.integers().between(1, 500).map(BigDecimal::valueOf))
                .as((code, price, qty) -> new SubscriptionInfo.UsageFeeItem(code, "통화료_" + code, price, qty))
                .list().ofMinSize(1).ofMaxSize(5);

        return usageItems.map(items -> {
            LocalDate now = LocalDate.of(2024, 1, 1);
            SubscriptionInfo sub = new SubscriptionInfo(
                    "SUB001", "PROD001", "SUBSCRIBER001",
                    BigDecimal.ZERO, now, now.plusMonths(1), "N", null, List.of(), items, List.of(), null, null);
            return ChargeContext.of(
                    "TENANT_01",
                    new ContractInfo("C001", "SUB001", "PROD001", now, now.plusMonths(1)),
                    ChargeInput.builder().subscriptionInfo(sub).suspensionHistories(List.of()).build());
        });
    }
}

package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.BillingInfo;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.PrepaidRecord;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 11: 요금 항목 결과 유형 일관성 (후처리)
 *
 * LateFeeStep, AutoPayDiscountStep, VatStep, PrepaidOffsetStep,
 * SplitBillingStep은
 * 모두 FlatChargeResult만 생성해야 한다 (PeriodChargeResult를 추가하지 않아야 한다).
 *
 * **Validates: Requirements 10.2, 11.2, 12.2, 13.2, 14.2**
 */
class PostProcessStepResultTypePropertyTest {

    private final LateFeeStep lateFeeStep = new LateFeeStep();
    private final AutoPayDiscountStep autoPayDiscountStep = new AutoPayDiscountStep();
    private final VatStep vatStep = new VatStep();
    private final PrepaidOffsetStep prepaidOffsetStep = new PrepaidOffsetStep();
    private final SplitBillingStep splitBillingStep = new SplitBillingStep();

    // Feature: billing-charge-calculation, Property 11: LateFeeStep →
    // FlatChargeResult only
    @Property(tries = 100)
    void lateFeeStepShouldOnlyProduceFlatResults(
            @ForAll("lateFeeContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        lateFeeStep.process(context);

        // LateFeeStep은 PeriodChargeResult를 추가하지 않아야 한다
        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        // 새로 생성된 FlatChargeResult는 모두 LATE_FEE 타입이어야 한다
        for (FlatChargeResult result : context.getFlatResults()) {
            if (result.chargeItemType() == ChargeItemType.LATE_FEE) {
                assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.LATE_FEE);
            }
        }
    }

    // Feature: billing-charge-calculation, Property 11: AutoPayDiscountStep →
    // FlatChargeResult only
    @Property(tries = 100)
    void autoPayDiscountStepShouldOnlyProduceFlatResults(
            @ForAll("autoPayContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        autoPayDiscountStep.process(context);

        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        for (FlatChargeResult result : context.getFlatResults()) {
            if (result.chargeItemType() == ChargeItemType.AUTO_PAY_DISCOUNT) {
                assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.AUTO_PAY_DISCOUNT);
            }
        }
    }

    // Feature: billing-charge-calculation, Property 11: VatStep → FlatChargeResult
    // only
    @Property(tries = 100)
    void vatStepShouldOnlyProduceFlatResults(
            @ForAll("vatContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        vatStep.process(context);

        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        for (FlatChargeResult result : context.getFlatResults()) {
            if (result.chargeItemType() == ChargeItemType.VAT) {
                assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.VAT);
            }
        }
    }

    // Feature: billing-charge-calculation, Property 11: PrepaidOffsetStep →
    // FlatChargeResult only
    @Property(tries = 100)
    void prepaidOffsetStepShouldOnlyProduceFlatResults(
            @ForAll("prepaidContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        prepaidOffsetStep.process(context);

        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        for (FlatChargeResult result : context.getFlatResults()) {
            if (result.chargeItemType() == ChargeItemType.PREPAID_OFFSET) {
                assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.PREPAID_OFFSET);
            }
        }
    }

    // Feature: billing-charge-calculation, Property 11: SplitBillingStep →
    // FlatChargeResult only
    @Property(tries = 100)
    void splitBillingStepShouldOnlyProduceFlatResults(
            @ForAll("splitBillingContexts") ChargeContext context) {

        int periodBefore = context.getPeriodResults().size();

        splitBillingStep.process(context);

        assertThat(context.getPeriodResults()).hasSize(periodBefore);

        for (FlatChargeResult result : context.getFlatResults()) {
            if (result.chargeItemType() == ChargeItemType.SPLIT_BILLING) {
                assertThat(result.chargeItemType()).isEqualTo(ChargeItemType.SPLIT_BILLING);
            }
        }
    }

    // --- Providers ---

    @Provide
    Arbitrary<ChargeContext> lateFeeContexts() {
        return Combinators.combine(
                Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf),
                Arbitraries.integers().between(1, 90)).as((unpaid, overdueDays) -> {
                    LocalDate now = LocalDate.of(2024, 6, 15);
                    LocalDate dueDate = now.minusDays(overdueDays);

                    BillingInfo billing = new BillingInfo(
                            "BILL001", "C001", BigDecimal.valueOf(50000),
                            unpaid, dueDate, now, false, null);

                    SubscriptionInfo sub = new SubscriptionInfo(
                            "SUB001", "PROD001", "SUBSCRIBER001",
                            BigDecimal.ZERO, now.minusMonths(1), now, "N", null,
                            List.of(), List.of(), List.of(), null, null);

                    return ChargeContext.of(
                            "TENANT_01",
                            new ContractInfo("C001", "SUB001", "PROD001", now.minusMonths(1), now),
                            ChargeInput.builder()
                                    .subscriptionInfo(sub)
                                    .billingInfo(billing)
                                    .build());
                });
    }

    @Provide
    Arbitrary<ChargeContext> autoPayContexts() {
        return Combinators.combine(
                Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf),
                Arbitraries.integers().between(1, 10).map(BigDecimal::valueOf)).as((chargeAmount, discountRate) -> {
                    LocalDate now = LocalDate.of(2024, 6, 15);

                    BillingInfo billing = new BillingInfo(
                            "BILL001", "C001", null, null, null, null,
                            true, discountRate);

                    SubscriptionInfo sub = new SubscriptionInfo(
                            "SUB001", "PROD001", "SUBSCRIBER001",
                            BigDecimal.ZERO, now.minusMonths(1), now, "N", null,
                            List.of(), List.of(), List.of(), null, null);

                    ChargeContext ctx = ChargeContext.of(
                            "TENANT_01",
                            new ContractInfo("C001", "SUB001", "PROD001", now.minusMonths(1), now),
                            ChargeInput.builder()
                                    .subscriptionInfo(sub)
                                    .billingInfo(billing)
                                    .build());

                    // 사전 요금 결과 추가
                    ctx.addFlatResult(new FlatChargeResult(
                            "PROD001", "월정액", ChargeItemType.MONTHLY_FEE,
                            chargeAmount, "KRW", Map.of()));

                    return ctx;
                });
    }

    @Provide
    Arbitrary<ChargeContext> vatContexts() {
        return Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf)
                .map(chargeAmount -> {
                    LocalDate now = LocalDate.of(2024, 6, 15);

                    SubscriptionInfo sub = new SubscriptionInfo(
                            "SUB001", "PROD001", "SUBSCRIBER001",
                            BigDecimal.ZERO, now.minusMonths(1), now, "N", null,
                            List.of(), List.of(), List.of(), null, null);

                    ChargeContext ctx = ChargeContext.of(
                            "TENANT_01",
                            new ContractInfo("C001", "SUB001", "PROD001", now.minusMonths(1), now),
                            ChargeInput.builder().subscriptionInfo(sub).build());

                    ctx.addFlatResult(new FlatChargeResult(
                            "PROD001", "월정액", ChargeItemType.MONTHLY_FEE,
                            chargeAmount, "KRW", Map.of()));

                    return ctx;
                });
    }

    @Provide
    Arbitrary<ChargeContext> prepaidContexts() {
        return Arbitraries.integers().between(1, 5)
                .flatMap(count -> Arbitraries.integers().between(100, 10000).map(BigDecimal::valueOf)
                        .list().ofSize(count)
                        .map(amounts -> {
                            LocalDate now = LocalDate.of(2024, 6, 15);

                            List<PrepaidRecord> records = amounts.stream()
                                    .map(a -> new PrepaidRecord("PRE_" + a, a))
                                    .toList();

                            SubscriptionInfo sub = new SubscriptionInfo(
                                    "SUB001", "PROD001", "SUBSCRIBER001",
                                    BigDecimal.ZERO, now.minusMonths(1), now, "N", null,
                                    List.of(), List.of(), List.of(), null, null);

                            return ChargeContext.of(
                                    "TENANT_01",
                                    new ContractInfo("C001", "SUB001", "PROD001", now.minusMonths(1), now),
                                    ChargeInput.builder()
                                            .subscriptionInfo(sub)
                                            .prepaidRecords(records)
                                            .build());
                        }));
    }

    @Provide
    Arbitrary<ChargeContext> splitBillingContexts() {
        return Combinators.combine(
                Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf),
                Arbitraries.integers().between(1, 99).map(BigDecimal::valueOf)).as((chargeAmount, splitRatio) -> {
                    LocalDate now = LocalDate.of(2024, 6, 15);

                    SubscriptionInfo sub = new SubscriptionInfo(
                            "SUB001", "PROD001", "SUBSCRIBER001",
                            BigDecimal.ZERO, now.minusMonths(1), now, "N", null,
                            List.of(), List.of(), List.of(), splitRatio, "TARGET_001");

                    ChargeContext ctx = ChargeContext.of(
                            "TENANT_01",
                            new ContractInfo("C001", "SUB001", "PROD001", now.minusMonths(1), now),
                            ChargeInput.builder().subscriptionInfo(sub).build());

                    ctx.addFlatResult(new FlatChargeResult(
                            "PROD001", "월정액", ChargeItemType.MONTHLY_FEE,
                            chargeAmount, "KRW", Map.of()));

                    return ctx;
                });
    }
}

package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.DiscountSubscription;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 12: 할인 금액 상한 불변식
 *
 * 임의의 할인 계산(할인1, 할인2)에 대해, 할인 금액의 절대값은
 * 대상 원금성 항목의 금액을 초과하지 않아야 한다.
 *
 * **Validates: Requirements 8.1, 9.1**
 */
class DiscountAmountUpperBoundPropertyTest {

        private final PeriodDiscountStep periodDiscountStep = new PeriodDiscountStep();
        private final FlatDiscountStep flatDiscountStep = new FlatDiscountStep();

        // Feature: billing-charge-calculation, Property 12: 할인1(PeriodDiscount) 할인 금액
        // 상한
        @Property(tries = 100)
        @Tag("Feature: billing-charge-calculation, Property 12: 할인 금액 상한 불변식")
        void periodDiscountAmountShouldNotExceedPrincipal(
                        @ForAll("periodDiscountContexts") ChargeContext context) {

                // 원금성 항목의 총 금액 기록
                List<PeriodChargeResult> principalsBefore = context.getPeriodResultsByType(ChargeItemType.MONTHLY_FEE);
                BigDecimal totalPrincipal = principalsBefore.stream()
                                .map(PeriodChargeResult::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                periodDiscountStep.process(context);

                // 할인 결과 조회
                List<PeriodChargeResult> discountResults = context
                                .getPeriodResultsByType(ChargeItemType.PERIOD_DISCOUNT);

                for (PeriodChargeResult discount : discountResults) {
                        // 할인 금액은 음수여야 한다
                        assertThat(discount.amount()).isLessThanOrEqualTo(BigDecimal.ZERO);

                        // 해당 할인의 대상 원금 구간 찾기
                        PeriodChargeResult matchingPrincipal = principalsBefore.stream()
                                        .filter(p -> p.periodFrom().equals(discount.periodFrom())
                                                        && p.periodTo().equals(discount.periodTo()))
                                        .findFirst()
                                        .orElse(null);

                        if (matchingPrincipal != null) {
                                // 할인 금액의 절대값은 원금을 초과하지 않아야 한다
                                assertThat(discount.amount().abs())
                                                .as("할인 금액 절대값(%s)이 원금(%s)을 초과하면 안됨",
                                                                discount.amount().abs(), matchingPrincipal.amount())
                                                .isLessThanOrEqualTo(matchingPrincipal.amount().abs());
                        }
                }

                // 전체 할인 총액의 절대값도 전체 원금 총액을 초과하지 않아야 한다
                BigDecimal totalDiscount = discountResults.stream()
                                .map(PeriodChargeResult::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (!discountResults.isEmpty()) {
                        assertThat(totalDiscount.abs())
                                        .as("전체 할인 총액 절대값(%s)이 전체 원금 총액(%s)을 초과하면 안됨",
                                                        totalDiscount.abs(), totalPrincipal)
                                        .isLessThanOrEqualTo(totalPrincipal.abs());
                }
        }

        // Feature: billing-charge-calculation, Property 12: 할인2(FlatDiscount) 할인 금액 상한
        @Property(tries = 100)
        @Tag("Feature: billing-charge-calculation, Property 12: 할인 금액 상한 불변식")
        void flatDiscountAmountShouldNotExceedPrincipal(
                        @ForAll("flatDiscountContexts") ChargeContext context) {

                // 원금성 Flat 항목의 금액 기록
                List<FlatChargeResult> principalsBefore = context.getFlatResults().stream()
                                .filter(f -> f.chargeItemType() != ChargeItemType.FLAT_DISCOUNT)
                                .toList();

                flatDiscountStep.process(context);

                // 할인 결과 조회
                List<FlatChargeResult> discountResults = context.getFlatResultsByType(ChargeItemType.FLAT_DISCOUNT);

                for (FlatChargeResult discount : discountResults) {
                        // 할인 금액은 음수여야 한다
                        assertThat(discount.amount()).isLessThanOrEqualTo(BigDecimal.ZERO);
                }

                // 각 할인 항목의 절대값이 대상 원금 항목별 금액을 초과하지 않는지 검증
                // 할인은 원금 항목 × 할인 항목 조합으로 생성되므로, 개별 할인의 절대값이 원금을 초과하면 안됨
                BigDecimal maxPrincipal = principalsBefore.stream()
                                .map(f -> f.amount().abs())
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);

                for (FlatChargeResult discount : discountResults) {
                        assertThat(discount.amount().abs())
                                        .as("개별 할인 금액 절대값(%s)이 최대 원금(%s)을 초과하면 안됨",
                                                        discount.amount().abs(), maxPrincipal)
                                        .isLessThanOrEqualTo(maxPrincipal);
                }
        }

        @Provide
        Arbitrary<ChargeContext> periodDiscountContexts() {
                return Combinators.combine(
                                Arbitraries.integers().between(1000, 100000).map(BigDecimal::valueOf),
                                Arbitraries.integers().between(1, 100).map(BigDecimal::valueOf),
                                Arbitraries.integers().between(2020, 2025),
                                Arbitraries.integers().between(1, 12)).as((monthlyRate, discountRate, year, month) -> {
                                        LocalDate start = LocalDate.of(year, month, 1);
                                        LocalDate end = start.plusMonths(1).minusDays(1);

                                        DiscountSubscription discountSub = DiscountSubscription.builder()
                                                        .contractId("C001")
                                                        .discountId("DI001")
                                                        .discountCode("DC001")
                                                        .discountName("할인_DC001")
                                                        .discountRate(discountRate)
                                                        .startDate(start)
                                                        .endDate(end)
                                                        .build();

                                        SubscriptionInfo sub = new SubscriptionInfo(
                                                        "C001", "SUB001", "PROD001", "SUBSCRIBER001",
                                                        monthlyRate, start, end, "N", null,
                                                        List.of(), List.of(), List.of(), null, null);

                                        ChargeContext ctx = ChargeContext.of(
                                                        "TENANT_01",
                                                        new ContractInfo("C001", "SUB001", "PROD001", start, end),
                                                        ChargeInput.builder()
                                                                        .subscriptionInfo(sub)
                                                                        .suspensionHistories(List.of())
                                                                        .discountSubscriptions(List.of(discountSub))
                                                                        .build());

                                        // 원금성 PeriodChargeResult 사전 추가 (MonthlyFeeStep이 이미 실행된 상태 시뮬레이션)
                                        ctx.addPeriodResult(PeriodChargeResult.of("PROD001", ChargeItemType.MONTHLY_FEE,
                                                        monthlyRate, start, end));

                                        return ctx;
                                });
        }

        @Provide
        Arbitrary<ChargeContext> flatDiscountContexts() {
                return Combinators.combine(
                                Arbitraries.integers().between(100, 50000).map(BigDecimal::valueOf),
                                Arbitraries.integers().between(1, 100).map(BigDecimal::valueOf))
                                .as((amount, discountRate) -> {
                                        LocalDate now = LocalDate.of(2024, 1, 1);

                                        DiscountSubscription discountSub = DiscountSubscription.builder()
                                                        .contractId("C001")
                                                        .discountId("DI002")
                                                        .discountCode("DC002")
                                                        .discountName("할인_DC002")
                                                        .discountRate(discountRate)
                                                        .startDate(now)
                                                        .endDate(now.plusMonths(1))
                                                        .build();

                                        SubscriptionInfo sub = new SubscriptionInfo(
                                                        "C001", "SUB001", "PROD001", "SUBSCRIBER001",
                                                        BigDecimal.ZERO, now, now.plusMonths(1), "N", null,
                                                        List.of(), List.of(), List.of(), null, null);

                                        ChargeContext ctx = ChargeContext.of(
                                                        "TENANT_01",
                                                        new ContractInfo("C001", "SUB001", "PROD001", now,
                                                                        now.plusMonths(1)),
                                                        ChargeInput.builder()
                                                                        .subscriptionInfo(sub)
                                                                        .suspensionHistories(List.of())
                                                                        .discountSubscriptions(List.of(discountSub))
                                                                        .build());

                                        // 원금성 FlatChargeResult 사전 추가 (압축 후 상태 시뮬레이션)
                                        ctx.addFlatResult(new FlatChargeResult(
                                                        "PROD001", "월정액_PROD001", ChargeItemType.ONE_TIME_FEE,
                                                        amount, "KRW", java.util.Map.of()));

                                        return ctx;
                                });
        }
}

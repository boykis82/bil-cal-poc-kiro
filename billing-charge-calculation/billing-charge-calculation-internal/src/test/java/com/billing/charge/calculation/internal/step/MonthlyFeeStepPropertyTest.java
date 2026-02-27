package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 10: 월정액 계산 결과 기간 유효성
 *
 * 임의의 월정액 계산 입력(가입정보, 정지이력, 기준정보)에 대해,
 * 계산 결과인 PeriodChargeResult의 periodFrom은 항상 periodTo 이하이어야 하며,
 * 금액은 0 이상이고 해당 구간의 기준 요금을 초과하지 않아야 한다.
 *
 * **Validates: Requirements 5.1, 5.2**
 */
class MonthlyFeeStepPropertyTest {

        private final MonthlyFeeStep monthlyFeeStep = new MonthlyFeeStep();

        // Feature: billing-charge-calculation, Property 10: 월정액 계산 결과 기간 유효성
        @Property(tries = 100)
        void monthlyFeeResultPeriodShouldBeValid(
                        @ForAll("validMonthlyFeeInputs") MonthlyFeeTestInput input) {

                ChargeContext context = ChargeContext.of(
                                "TENANT_01",
                                new ContractInfo("C001", "SUB001", input.productId(),
                                                input.startDate(), input.endDate()),
                                ChargeInput.builder()
                                                .subscriptionInfo(input.subscriptionInfo())
                                                .suspensionHistories(List.of())
                                                .build());

                monthlyFeeStep.process(context);

                List<PeriodChargeResult> results = context.getPeriodResults();

                for (PeriodChargeResult result : results) {
                        // periodFrom <= periodTo
                        assertThat(result.periodFrom())
                                        .as("periodFrom은 periodTo 이하이어야 한다")
                                        .isBeforeOrEqualTo(result.periodTo());

                        // amount >= 0
                        assertThat(result.amount())
                                        .as("금액은 0 이상이어야 한다")
                                        .isGreaterThanOrEqualTo(BigDecimal.ZERO);

                        // amount <= monthlyRate (일할 계산이므로 월정액을 초과할 수 없음)
                        assertThat(result.amount())
                                        .as("금액은 월정액을 초과하지 않아야 한다")
                                        .isLessThanOrEqualTo(input.monthlyRate());
                }
        }

        @Provide
        Arbitrary<MonthlyFeeTestInput> validMonthlyFeeInputs() {
                // 2020~2025 범위의 날짜 생성
                Arbitrary<LocalDate> baseDates = Arbitraries.integers()
                                .between(2020, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                                .map(month -> LocalDate.of(year, month, 1)));

                Arbitrary<BigDecimal> rates = Arbitraries.integers()
                                .between(1000, 100000)
                                .map(BigDecimal::valueOf);

                return Combinators.combine(baseDates, rates,
                                Arbitraries.integers().between(0, 27))
                                .as((baseDate, rate, extraDays) -> {
                                        LocalDate startDate = baseDate;
                                        YearMonth ym = YearMonth.from(startDate);
                                        // endDate는 같은 달 내에서 startDate 이후
                                        LocalDate endDate = startDate.plusDays(
                                                        Math.min(extraDays, ym.lengthOfMonth()
                                                                        - startDate.getDayOfMonth()));
                                        if (endDate.isBefore(startDate)) {
                                                endDate = startDate;
                                        }

                                        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(
                                                        "SUB001", "PROD001", "SUBSCRIBER001",
                                                        rate, startDate, endDate,
                                                        "N", null, List.of(), List.of(), List.of(), null, null);

                                        return new MonthlyFeeTestInput("PROD001", startDate, endDate, rate,
                                                        subscriptionInfo);
                                });
        }

        record MonthlyFeeTestInput(
                        String productId,
                        LocalDate startDate,
                        LocalDate endDate,
                        BigDecimal monthlyRate,
                        SubscriptionInfo subscriptionInfo) {
        }
}

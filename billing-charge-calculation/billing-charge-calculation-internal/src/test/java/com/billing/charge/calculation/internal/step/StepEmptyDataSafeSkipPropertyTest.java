package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Feature: subscription-data-load-refactor, Property 4: Step 데이터 부재 시 안전한 생략
 *
 * 빈 또는 불완전한 ChargeInput을 가진 ChargeContext에 대해 각 Step 실행 시
 * 예외가 발생하지 않고, 결과 리스트(periodResults, flatResults)에 새 항목이 추가되지 않음을 검증한다.
 *
 * **Validates: 요구사항 12.3**
 */
class StepEmptyDataSafeSkipPropertyTest {

    private MonthlyFeeStep monthlyFeeStep;
    private OneTimeFeeStep oneTimeFeeStep;
    private UsageFeeStep usageFeeStep;
    private FlatDiscountStep flatDiscountStep;
    private PeriodDiscountStep periodDiscountStep;
    private LateFeeStep lateFeeStep;
    private AutoPayDiscountStep autoPayDiscountStep;
    private VatStep vatStep;
    private PrepaidOffsetStep prepaidOffsetStep;
    private SplitBillingStep splitBillingStep;

    @BeforeTry
    void setUp() {
        monthlyFeeStep = new MonthlyFeeStep();
        oneTimeFeeStep = new OneTimeFeeStep();
        usageFeeStep = new UsageFeeStep();
        flatDiscountStep = new FlatDiscountStep();
        periodDiscountStep = new PeriodDiscountStep();
        lateFeeStep = new LateFeeStep();
        autoPayDiscountStep = new AutoPayDiscountStep();
        vatStep = new VatStep();
        prepaidOffsetStep = new PrepaidOffsetStep();
        splitBillingStep = new SplitBillingStep();
    }


    // --- Property 4: 각 Step에 빈/불완전 ChargeInput 전달 시 안전한 생략 ---

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void monthlyFeeStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(monthlyFeeStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void oneTimeFeeStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(oneTimeFeeStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void usageFeeStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(usageFeeStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void flatDiscountStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(flatDiscountStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void periodDiscountStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(periodDiscountStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void lateFeeStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(lateFeeStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void autoPayDiscountStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(autoPayDiscountStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void vatStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(vatStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void prepaidOffsetStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(prepaidOffsetStep, context);
    }

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 4: Step 데이터 부재 시 안전한 생략")
    void splitBillingStepShouldSafelySkipWithEmptyData(
            @ForAll("emptyOrIncompleteContexts") ChargeContext context) {
        assertStepSafelySkips(splitBillingStep, context);
    }

    // --- Helper ---

    private void assertStepSafelySkips(ChargeItemStep step, ChargeContext context) {
        int periodBefore = context.getPeriodResults().size();
        int flatBefore = context.getFlatResults().size();

        assertThatNoException().isThrownBy(() -> step.process(context));

        assertThat(context.getPeriodResults()).hasSize(periodBefore);
        assertThat(context.getFlatResults()).hasSize(flatBefore);
    }

    // --- Arbitrary: 빈 또는 불완전한 ChargeInput을 가진 ChargeContext ---

    @Provide
    Arbitrary<ChargeContext> emptyOrIncompleteContexts() {
        Arbitrary<ChargeInput> emptyInputs = Arbitraries.of(
                // 완전히 빈 ChargeInput (모든 필드 null, map은 빈 HashMap)
                ChargeInput.builder().build(),
                // null map을 가진 ChargeInput
                ChargeInput.builder()
                        .oneTimeChargeDataMap(null)
                        .usageChargeDataMap(null)
                        .build(),
                // 빈 리스트만 있는 ChargeInput
                ChargeInput.builder()
                        .suspensionHistories(List.of())
                        .prepaidRecords(List.of())
                        .discountSubscriptions(List.of())
                        .oneTimeChargeDataMap(new HashMap<>())
                        .usageChargeDataMap(new HashMap<>())
                        .build()
        );

        Arbitrary<String> tenantIds = Arbitraries.of("TENANT_01", "TENANT_02", "TENANT_03");

        Arbitrary<ContractInfo> contractInfos = Arbitraries.of(
                new ContractInfo("C001", "SUB001", "PROD001",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)),
                new ContractInfo("C002", "SUB002", "PROD002",
                        LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30))
        );

        return Combinators.combine(tenantIds, contractInfos, emptyInputs)
                .as(ChargeContext::of);
    }
}

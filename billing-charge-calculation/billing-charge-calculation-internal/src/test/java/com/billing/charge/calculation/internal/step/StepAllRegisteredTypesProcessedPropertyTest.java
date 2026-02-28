package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: subscription-data-load-refactor, Property 6: Step의 모든 등록 유형 데이터 처리
 *
 * 임의의 N개 유형의 OneTimeChargeDomain/UsageChargeDomain 데이터가 ChargeInput에 존재할 때
 * 해당 Step(OneTimeFeeStep/UsageFeeStep) 실행 시 모든 N개 유형에 대해 계산 결과가 생성됨을 검증한다.
 *
 * **Validates: 요구사항 4.4, 5.4, 12.4, 12.5**
 */
class StepAllRegisteredTypesProcessedPropertyTest {

    private final OneTimeFeeStep oneTimeFeeStep = new OneTimeFeeStep();
    private final UsageFeeStep usageFeeStep = new UsageFeeStep();

    // --- Property 6: OneTimeFeeStep이 모든 등록 유형 데이터를 처리 ---

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 6: Step의 모든 등록 유형 데이터 처리")
    void oneTimeFeeStepShouldProduceResultsForAllRegisteredTypes(
            @ForAll("oneTimeChargeContexts") ContextWithExpectedTypes<OneTimeChargeDomain> input) {

        ChargeContext context = input.context();
        Set<String> expectedTypeNames = input.expectedTypeNames();

        oneTimeFeeStep.process(context);

        // 생성된 FlatChargeResult의 chargeItemCode에서 유형명 추출
        Set<String> producedTypeNames = context.getFlatResults().stream()
                .filter(r -> r.chargeItemType() == ChargeItemType.ONE_TIME_FEE)
                .map(FlatChargeResult::chargeItemCode)
                .collect(Collectors.toSet());

        assertThat(producedTypeNames)
                .as("OneTimeFeeStep이 모든 등록 유형(%s)에 대해 결과를 생성해야 함", expectedTypeNames)
                .containsAll(expectedTypeNames);
    }

    // --- Property 6: UsageFeeStep이 모든 등록 유형 데이터를 처리 ---

    @Property(tries = 100)
    @Tag("Feature: subscription-data-load-refactor")
    @Tag("Property 6: Step의 모든 등록 유형 데이터 처리")
    void usageFeeStepShouldProduceResultsForAllRegisteredTypes(
            @ForAll("usageChargeContexts") ContextWithExpectedTypes<UsageChargeDomain> input) {

        ChargeContext context = input.context();
        Set<String> expectedTypeNames = input.expectedTypeNames();

        usageFeeStep.process(context);

        // 생성된 FlatChargeResult의 chargeItemCode에서 유형명 추출
        Set<String> producedTypeNames = context.getFlatResults().stream()
                .filter(r -> r.chargeItemType() == ChargeItemType.USAGE_FEE)
                .map(FlatChargeResult::chargeItemCode)
                .collect(Collectors.toSet());

        assertThat(producedTypeNames)
                .as("UsageFeeStep이 모든 등록 유형(%s)에 대해 결과를 생성해야 함", expectedTypeNames)
                .containsAll(expectedTypeNames);
    }

    // --- Helper record ---

    record ContextWithExpectedTypes<T>(ChargeContext context, Set<String> expectedTypeNames) {}

    // --- Arbitrary: OneTimeChargeDomain N개 유형 데이터를 가진 ChargeContext ---

    @Provide
    Arbitrary<ContextWithExpectedTypes<OneTimeChargeDomain>> oneTimeChargeContexts() {
        // 사용 가능한 OneTimeChargeDomain 유형 중 1~2개를 임의 선택
        Arbitrary<Set<String>> typeSubsets = Arbitraries.subsetOf("InstallmentHistory", "PenaltyFee")
                .filter(s -> !s.isEmpty());

        Arbitrary<BigDecimal> amounts = Arbitraries.integers().between(100, 100000)
                .map(BigDecimal::valueOf);

        return Combinators.combine(typeSubsets, amounts).as((types, amount) -> {
            Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>> dataMap = new HashMap<>();
            Set<String> expectedTypeNames = new HashSet<>();

            for (String typeName : types) {
                switch (typeName) {
                    case "InstallmentHistory" -> {
                        dataMap.put(InstallmentHistory.class, List.of(
                                InstallmentHistory.builder()
                                        .contractId("C001")
                                        .installmentId("INST001")
                                        .installmentAmount(amount)
                                        .currentInstallment(1)
                                        .totalInstallments(12)
                                        .build()));
                        expectedTypeNames.add("InstallmentHistory");
                    }
                    case "PenaltyFee" -> {
                        dataMap.put(PenaltyFee.class, List.of(
                                PenaltyFee.builder()
                                        .contractId("C001")
                                        .penaltyId("PEN001")
                                        .penaltyAmount(amount)
                                        .penaltyReason("위약금")
                                        .build()));
                        expectedTypeNames.add("PenaltyFee");
                    }
                }
            }

            ChargeInput chargeInput = ChargeInput.builder()
                    .oneTimeChargeDataMap(dataMap)
                    .build();

            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);
            ContractInfo contractInfo = new ContractInfo("C001", "SUB001", "PROD001", start, end);
            ChargeContext context = ChargeContext.of("TENANT_01", contractInfo, chargeInput);

            return new ContextWithExpectedTypes<>(context, expectedTypeNames);
        });
    }

    // --- Arbitrary: UsageChargeDomain N개 유형 데이터를 가진 ChargeContext ---

    @Provide
    Arbitrary<ContextWithExpectedTypes<UsageChargeDomain>> usageChargeContexts() {
        // 사용 가능한 UsageChargeDomain 유형 중 1~2개를 임의 선택
        Arbitrary<Set<String>> typeSubsets = Arbitraries.subsetOf("VoiceUsage", "DataUsage")
                .filter(s -> !s.isEmpty());

        Arbitrary<BigDecimal> values = Arbitraries.integers().between(1, 10000)
                .map(BigDecimal::valueOf);

        Arbitrary<BigDecimal> unitPrices = Arbitraries.integers().between(1, 100)
                .map(BigDecimal::valueOf);

        return Combinators.combine(typeSubsets, values, unitPrices).as((types, value, unitPrice) -> {
            Map<Class<? extends UsageChargeDomain>, List<? extends UsageChargeDomain>> dataMap = new HashMap<>();
            Set<String> expectedTypeNames = new HashSet<>();

            for (String typeName : types) {
                switch (typeName) {
                    case "VoiceUsage" -> {
                        dataMap.put(VoiceUsage.class, List.of(
                                VoiceUsage.builder()
                                        .contractId("C001")
                                        .usageId("VU001")
                                        .duration(value)
                                        .unitPrice(unitPrice)
                                        .build()));
                        expectedTypeNames.add("VoiceUsage");
                    }
                    case "DataUsage" -> {
                        dataMap.put(DataUsage.class, List.of(
                                DataUsage.builder()
                                        .contractId("C001")
                                        .usageId("DU001")
                                        .dataVolume(value)
                                        .unitPrice(unitPrice)
                                        .build()));
                        expectedTypeNames.add("DataUsage");
                    }
                }
            }

            ChargeInput chargeInput = ChargeInput.builder()
                    .usageChargeDataMap(dataMap)
                    .build();

            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);
            ContractInfo contractInfo = new ContractInfo("C001", "SUB001", "PROD001", start, end);
            ChargeContext context = ChargeContext.of("TENANT_01", contractInfo, chargeInput);

            return new ContextWithExpectedTypes<>(context, expectedTypeNames);
        });
    }
}

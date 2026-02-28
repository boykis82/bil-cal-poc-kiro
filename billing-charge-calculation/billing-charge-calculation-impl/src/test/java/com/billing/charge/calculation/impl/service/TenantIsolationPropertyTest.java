package com.billing.charge.calculation.impl.service;

import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.dto.ChargeCalculationResponse;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.impl.dataloader.DataLoadOrchestrator;
import com.billing.charge.calculation.impl.pipeline.Pipeline;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import com.billing.charge.calculation.internal.step.ChargeItemStep;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 16: 테넌트별 요금 계산 정책 격리
 *
 * 임의의 서로 다른 두 테넌트에 대해, 동일한 상품유형과 유스케이스로 요금 계산을 수행했을 때,
 * 각 테넌트의 Pipeline 구성과 기준정보가 독립적으로 적용되어야 한다.
 *
 * **Validates: Requirements 17.1**
 */
@Tag("Feature: billing-charge-calculation, Property 16: 테넌트별 요금 계산 정책 격리")
class TenantIsolationPropertyTest {

    /**
     * Property 16: 서로 다른 두 테넌트가 동일한 productType, useCaseType으로 요금 계산 시,
     * 각 테넌트에 독립적인 Pipeline이 구성되고 결과가 서로 간섭하지 않아야 한다.
     */
    @Property(tries = 100)
    void differentTenantsShouldGetIndependentPipelineConfigurations(
            @ForAll("tenantPairs") String[] tenantPair,
            @ForAll("productTypes") ProductType productType,
            @ForAll("useCaseTypes") UseCaseType useCaseType) {

        String tenantA = tenantPair[0];
        String tenantB = tenantPair[1];

        // 테넌트별 Pipeline 구성 추적
        Map<String, Pipeline> configuredPipelines = new ConcurrentHashMap<>();
        Map<String, String> configuredTenantIds = new ConcurrentHashMap<>();

        // 테넌트 A에는 고정 금액 1000, 테넌트 B에는 고정 금액 2000을 부여하는 구성
        // 이렇게 하면 두 테넌트의 결과가 항상 다름을 보장
        PipelineConfigurator configurator = (tid, pt, uct) -> {
            configuredTenantIds.put(tid, tid);
            BigDecimal tenantAmount = tid.equals(tenantA)
                    ? new BigDecimal("1000")
                    : new BigDecimal("2000");
            List<ChargeItemStep> steps = List.of(
                    new TenantStep(tid + "_STEP", 1, tenantAmount));
            Pipeline pipeline = new Pipeline("PIPELINE_" + tid, steps);
            configuredPipelines.put(tid, pipeline);
            return pipeline;
        };

        // NoOp Strategy (모든 UseCaseType 지원)
        List<DataAccessStrategy> strategies = Arrays.stream(UseCaseType.values())
                .map(NoOpStrategy::new)
                .map(s -> (DataAccessStrategy) s)
                .toList();
        DataAccessStrategyResolver resolver = new DataAccessStrategyResolver(strategies);
        PipelineEngine engine = new PipelineEngine();

        ChargeCalculationServiceImpl service = new ChargeCalculationServiceImpl(
                configurator, engine, resolver, new DataLoadOrchestrator(List.of(), List.of()));

        // 테넌트 A 요청
        ChargeCalculationRequest requestA = ChargeCalculationRequest.builder()
                .tenantId(tenantA)
                .useCaseType(useCaseType)
                .productType(productType)
                .contracts(List.of(new ContractInfo(
                        "CONTRACT-A", "SUB-A", "PROD-A",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))))
                .build();

        // 테넌트 B 요청 (동일한 productType, useCaseType)
        ChargeCalculationRequest requestB = ChargeCalculationRequest.builder()
                .tenantId(tenantB)
                .useCaseType(useCaseType)
                .productType(productType)
                .contracts(List.of(new ContractInfo(
                        "CONTRACT-B", "SUB-B", "PROD-B",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))))
                .build();

        // 각 테넌트별 요금 계산 수행
        ChargeCalculationResponse responseA = service.calculate(requestA);
        ChargeCalculationResponse responseB = service.calculate(requestB);

        // 검증 1: PipelineConfigurator가 각 테넌트의 tenantId로 호출되었는지 확인
        assertThat(configuredTenantIds).containsKey(tenantA);
        assertThat(configuredTenantIds).containsKey(tenantB);
        assertThat(configuredTenantIds.get(tenantA)).isEqualTo(tenantA);
        assertThat(configuredTenantIds.get(tenantB)).isEqualTo(tenantB);

        // 검증 2: 서로 다른 Pipeline이 구성되었는지 확인
        Pipeline pipelineA = configuredPipelines.get(tenantA);
        Pipeline pipelineB = configuredPipelines.get(tenantB);
        assertThat(pipelineA.getPipelineId()).isNotEqualTo(pipelineB.getPipelineId());

        // 검증 3: 각 테넌트의 결과가 독립적 (서로 다른 금액)
        assertThat(responseA.results()).hasSize(1);
        assertThat(responseB.results()).hasSize(1);

        BigDecimal totalA = responseA.results().getFirst().chargeResults().stream()
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalB = responseB.results().getFirst().chargeResults().stream()
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 테넌트별 독립적인 정책이 적용되어 결과 금액이 달라야 함
        assertThat(totalA).isNotEqualTo(totalB);
        assertThat(totalA).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(totalB).isEqualByComparingTo(new BigDecimal("2000"));
    }

    // --- Generators ---

    @Provide
    Arbitrary<String[]> tenantPairs() {
        Arbitrary<String> tenantIds = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10)
                .map(s -> "TENANT-" + s);

        return tenantIds.tuple2()
                .filter(t -> !t.get1().equals(t.get2()))
                .map(t -> new String[] { t.get1(), t.get2() });
    }

    @Provide
    Arbitrary<ProductType> productTypes() {
        return Arbitraries.of(ProductType.values());
    }

    @Provide
    Arbitrary<UseCaseType> useCaseTypes() {
        return Arbitraries.of(UseCaseType.values());
    }

    // --- Test Doubles ---

    /**
     * 테넌트별 고유 금액을 ChargeContext에 추가하는 Step.
     */
    private record TenantStep(String stepId, int order,
            BigDecimal amount) implements ChargeItemStep {

        @Override
        public String getStepId() {
            return stepId;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public void process(ChargeContext context) {
            context.addFlatResult(new FlatChargeResult(
                    stepId, stepId, ChargeItemType.ONE_TIME_FEE,
                    amount, "KRW", Map.of()));
        }

        @Override
        public boolean requiresStatusUpdate() {
            return false;
        }
    }

    private static class NoOpStrategy implements DataAccessStrategy {
        private final UseCaseType useCaseType;

        NoOpStrategy(UseCaseType useCaseType) {
            this.useCaseType = useCaseType;
        }

        @Override
        public UseCaseType supportedUseCaseType() {
            return useCaseType;
        }

        @Override
        public ChargeInput readChargeInput(ContractInfo contractInfo) {
            return ChargeInput.builder().build();
        }

        @Override
        public void writeChargeResult(ChargeResult result) {
        }

        @Override
        public void updateProcessingStatus(String chargeItemId, ProcessingStatus status) {
        }

        @Override
        public ContractBaseLoader getContractBaseLoader() {
            throw new UnsupportedOperationException("Not used in test");
        }

        @Override
        public List<ChargeItemDataLoader> getChargeItemDataLoaders() {
            throw new UnsupportedOperationException("Not used in test");
        }
    }
}

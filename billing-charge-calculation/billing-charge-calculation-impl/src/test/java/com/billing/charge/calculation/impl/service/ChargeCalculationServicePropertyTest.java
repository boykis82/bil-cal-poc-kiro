package com.billing.charge.calculation.impl.service;

import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.dto.ChargeCalculationResponse;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.impl.pipeline.Pipeline;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 2: 계약정보 건수와 결과 건수 일치
 *
 * 임의의 비어있지 않은 계약정보 리스트에 대해, ChargeCalculationService.calculate()의
 * 응답에 포함된 결과 건수는 입력 계약정보 건수와 항상 일치해야 한다.
 *
 * **Validates: Requirements 1.3**
 */
@Tag("Feature: billing-charge-calculation, Property 2: 계약정보 건수와 결과 건수 일치")
class ChargeCalculationServicePropertyTest {

    private final ChargeCalculationServiceImpl service;

    ChargeCalculationServicePropertyTest() {
        // Stub Strategy: 모든 UseCaseType 지원
        List<DataAccessStrategy> strategies = Arrays.stream(UseCaseType.values())
                .map(NoOpStrategy::new)
                .map(s -> (DataAccessStrategy) s)
                .toList();
        DataAccessStrategyResolver resolver = new DataAccessStrategyResolver(strategies);

        // Stub PipelineConfigurator: 빈 Step 목록의 Pipeline 반환
        PipelineConfigurator configurator = (tenantId, productType, useCaseType) -> new Pipeline("TEST_PIPELINE",
                List.of());

        PipelineEngine engine = new PipelineEngine();

        this.service = new ChargeCalculationServiceImpl(configurator, engine, resolver);
    }

    /**
     * Property 2: 임의의 비어있지 않은 계약정보 리스트에 대해,
     * 응답 결과 건수는 입력 계약정보 건수와 항상 일치해야 한다.
     */
    @Property(tries = 100)
    void resultCountShouldMatchContractCount(
            @ForAll("contractLists") List<ContractInfo> contracts,
            @ForAll("useCaseTypes") UseCaseType useCaseType,
            @ForAll("productTypes") ProductType productType) {

        ChargeCalculationRequest request = ChargeCalculationRequest.builder()
                .tenantId("TENANT-001")
                .useCaseType(useCaseType)
                .productType(productType)
                .contracts(contracts)
                .build();

        ChargeCalculationResponse response = service.calculate(request);

        assertThat(response.results()).hasSize(contracts.size());
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<ContractInfo>> contractLists() {
        return Arbitraries.integers().between(1, 50)
                .map(count -> IntStream.rangeClosed(1, count)
                        .mapToObj(i -> new ContractInfo(
                                "CONTRACT-" + i,
                                "SUB-" + i,
                                "PROD-" + i,
                                LocalDate.of(2025, 1, 1),
                                LocalDate.of(2025, 1, 31)))
                        .toList());
    }

    @Provide
    Arbitrary<UseCaseType> useCaseTypes() {
        return Arbitraries.of(UseCaseType.values());
    }

    @Provide
    Arbitrary<ProductType> productTypes() {
        return Arbitraries.of(ProductType.values());
    }

    // --- Test Doubles ---

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
    }
}

package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.configurator.DbPipelineConfigurator;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.mapper.PipelineConfigMapper;
import com.billing.charge.calculation.internal.mapper.dto.PipelineConfigDto;
import com.billing.charge.calculation.internal.mapper.dto.PipelineStepConfigDto;
import com.billing.charge.calculation.internal.step.ChargeItemStep;
import net.jqwik.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 5: Pipeline 구성의 테넌트/상품유형/유스케이스
 * 결정성
 *
 * 임의의 (tenantId, productType, useCaseType) 조합에 대해,
 * PipelineConfigurator가 반환하는 Pipeline의 Step 목록은
 * 해당 조합의 DB 설정(PIPELINE_CONFIG, PIPELINE_STEP_CONFIG)과 항상 일치해야 한다.
 *
 * Validates: Requirements 3.2, 17.2
 */
@Tag("Feature: billing-charge-calculation, Property 5: Pipeline 구성의 테넌트/상품유형/유스케이스 결정성")
class PipelineConfiguratorPropertyTest {

    private static final List<String> ALL_STEP_IDS = List.of(
            "MONTHLY_FEE", "ONE_TIME_FEE", "USAGE_FEE",
            "PERIOD_DISCOUNT", "COMPACTION", "FLAT_DISCOUNT",
            "LATE_FEE", "AUTO_PAY_DISCOUNT", "VAT",
            "PREPAID_OFFSET", "SPLIT_BILLING");

    /**
     * Property 5: 동일한 (tenantId, productType, useCaseType) 조합에 대해
     * 항상 동일한 Pipeline Step 목록이 반환되어야 한다 (결정성).
     * Pipeline의 Step 목록은 DB 설정의 STEP_ORDER 순서와 일치해야 한다.
     */
    @Property(tries = 100)
    void sameInputShouldAlwaysProduceSamePipelineOutput(
            @ForAll("tenantIds") String tenantId,
            @ForAll ProductType productType,
            @ForAll UseCaseType useCaseType,
            @ForAll("stepSubsets") List<String> configuredStepIds) {

        // Stub mapper: 주어진 조합에 대해 결정적 결과 반환
        StubPipelineConfigMapper mapper = new StubPipelineConfigMapper(
                tenantId, productType.name(), useCaseType.name(), configuredStepIds);

        // ChargeItemStep Bean 등록 — order는 configuredStepIds 내 위치 기반
        // Pipeline은 step.getOrder()로 정렬하므로, DB의 STEP_ORDER와 일치시켜야 함
        Map<String, Integer> stepOrderMap = new HashMap<>();
        for (int i = 0; i < configuredStepIds.size(); i++) {
            stepOrderMap.put(configuredStepIds.get(i), (i + 1) * 10);
        }

        List<ChargeItemStep> allSteps = ALL_STEP_IDS.stream()
                .map(id -> (ChargeItemStep) new StubChargeItemStep(id, stepOrderMap.getOrDefault(id, 999)))
                .toList();

        DbPipelineConfigurator dbConfigurator = new DbPipelineConfigurator(mapper, allSteps);
        DefaultPipelineConfigurator configurator = new DefaultPipelineConfigurator(dbConfigurator);

        // 동일 입력으로 두 번 호출
        Pipeline pipeline1 = configurator.configure(tenantId, productType, useCaseType);
        Pipeline pipeline2 = configurator.configure(tenantId, productType, useCaseType);

        // 결과가 동일해야 함 (결정성)
        List<String> stepIds1 = pipeline1.getSteps().stream()
                .map(ChargeItemStep::getStepId).toList();
        List<String> stepIds2 = pipeline2.getSteps().stream()
                .map(ChargeItemStep::getStepId).toList();

        assertThat(stepIds1).isEqualTo(stepIds2);

        // Pipeline의 Step 목록이 DB 설정의 stepId들과 일치해야 함 (순서 포함)
        assertThat(stepIds1).containsExactlyElementsOf(configuredStepIds);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> tenantIds() {
        return Arbitraries.of("TENANT_A", "TENANT_B", "TENANT_C", "TENANT_D");
    }

    @Provide
    Arbitrary<List<String>> stepSubsets() {
        return Arbitraries.of(ALL_STEP_IDS)
                .list().ofMinSize(1).ofMaxSize(ALL_STEP_IDS.size())
                .uniqueElements();
    }

    // --- Test Doubles ---

    /**
     * 결정적 결과를 반환하는 Stub PipelineConfigMapper.
     */
    private static class StubPipelineConfigMapper implements PipelineConfigMapper {

        private final String tenantId;
        private final String productType;
        private final String useCaseType;
        private final List<String> stepIds;

        StubPipelineConfigMapper(String tenantId, String productType,
                String useCaseType, List<String> stepIds) {
            this.tenantId = tenantId;
            this.productType = productType;
            this.useCaseType = useCaseType;
            this.stepIds = stepIds;
        }

        @Override
        public PipelineConfigDto findByTenantAndProductAndUseCase(
                String tenantId, String productType, String useCaseType) {
            if (this.tenantId.equals(tenantId)
                    && this.productType.equals(productType)
                    && this.useCaseType.equals(useCaseType)) {
                PipelineConfigDto dto = new PipelineConfigDto();
                dto.setPipelineConfigId("CONFIG-001");
                dto.setTenantId(tenantId);
                dto.setProductType(productType);
                dto.setUseCaseType(useCaseType);
                dto.setDescription("Test pipeline");
                return dto;
            }
            return null;
        }

        @Override
        public List<PipelineStepConfigDto> findStepsByPipelineConfigId(String pipelineConfigId) {
            return IntStream.range(0, stepIds.size())
                    .mapToObj(i -> {
                        PipelineStepConfigDto dto = new PipelineStepConfigDto();
                        dto.setPipelineConfigId(pipelineConfigId);
                        dto.setStepId(stepIds.get(i));
                        dto.setStepOrder((i + 1) * 10);
                        return dto;
                    })
                    .toList();
        }
    }

    /**
     * Stub ChargeItemStep: stepId와 order를 외부에서 지정.
     */
    private static class StubChargeItemStep implements ChargeItemStep {

        private final String stepId;
        private final int order;

        StubChargeItemStep(String stepId, int order) {
            this.stepId = stepId;
            this.order = order;
        }

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
            // no-op
        }

        @Override
        public boolean requiresStatusUpdate() {
            return false;
        }
    }
}

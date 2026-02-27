package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import com.billing.charge.calculation.internal.step.ChargeItemStep;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 15: 처리 상태 기록 조건부 실행
 *
 * 임의의 Pipeline 실행에서, requiresStatusUpdate()가 true인 Step이 정상 완료되면
 * 해당 Step의 처리 상태가 COMPLETED로 DB에 기록되어야 하고,
 * false인 Step은 상태 기록이 발생하지 않아야 한다.
 *
 * Validates: Requirements 16.1
 */
@Tag("Feature: billing-charge-calculation, Property 15: 처리 상태 기록 조건부 실행")
class PipelineEngineStatusUpdatePropertyTest {

    private final PipelineEngine engine = new PipelineEngine();

    @Property(tries = 100)
    void statusUpdateShouldOnlyBeCalledForStepsRequiringIt(
            @ForAll("stepConfigs") List<StepConfig> configs) {

        List<String> statusUpdatedStepIds = new ArrayList<>();

        // Step 목록 생성: 각 Step의 requiresStatusUpdate 값이 임의로 결정됨
        List<ChargeItemStep> steps = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            StepConfig config = configs.get(i);
            steps.add(new SimpleStep("STEP_" + i, i + 1, config.requiresStatusUpdate()));
        }

        // updateProcessingStatus 호출을 기록하는 Strategy
        DataAccessStrategy recordingStrategy = new StatusRecordingStrategy(statusUpdatedStepIds);

        Pipeline pipeline = new Pipeline("TEST_PIPELINE", steps);
        ChargeContext context = createContext();

        engine.execute(pipeline, context, recordingStrategy);

        // requiresStatusUpdate=true인 Step만 상태 갱신이 호출되어야 함
        List<String> expectedUpdatedStepIds = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).requiresStatusUpdate()) {
                expectedUpdatedStepIds.add("STEP_" + i);
            }
        }

        assertThat(statusUpdatedStepIds).isEqualTo(expectedUpdatedStepIds);
    }

    // --- Generators ---

    record StepConfig(boolean requiresStatusUpdate) {
    }

    @Provide
    Arbitrary<List<StepConfig>> stepConfigs() {
        return Arbitraries.of(true, false)
                .map(StepConfig::new)
                .list().ofMinSize(1).ofMaxSize(12);
    }

    // --- Test Doubles ---

    private static class SimpleStep implements ChargeItemStep {
        private final String stepId;
        private final int order;
        private final boolean requiresStatusUpdate;

        SimpleStep(String stepId, int order, boolean requiresStatusUpdate) {
            this.stepId = stepId;
            this.order = order;
            this.requiresStatusUpdate = requiresStatusUpdate;
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
            /* no-op */ }

        @Override
        public boolean requiresStatusUpdate() {
            return requiresStatusUpdate;
        }
    }

    private static class StatusRecordingStrategy implements DataAccessStrategy {
        private final List<String> statusUpdatedStepIds;

        StatusRecordingStrategy(List<String> statusUpdatedStepIds) {
            this.statusUpdatedStepIds = statusUpdatedStepIds;
        }

        @Override
        public UseCaseType supportedUseCaseType() {
            return UseCaseType.REGULAR_BILLING;
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
            assertThat(status).isEqualTo(ProcessingStatus.COMPLETED);
            statusUpdatedStepIds.add(chargeItemId);
        }
    }

    private ChargeContext createContext() {
        ContractInfo contractInfo = new ContractInfo(
                "CONTRACT-001", "SUB-001", "PROD-001",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        ChargeInput chargeInput = ChargeInput.builder().build();
        return ChargeContext.of("TENANT-001", contractInfo, chargeInput);
    }
}

package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.step.ChargeItemStep;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import com.billing.charge.calculation.internal.model.ChargeResult;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 3: Pipeline Step 실행 순서 보장
 *
 * 임의의 Pipeline 구성에 대해, PipelineEngine이 Step을 실행하는 순서는
 * Pipeline에 구성된 Step의 order 순서와 항상 일치해야 한다.
 *
 * Validates: Requirements 3.3
 */
@Tag("Feature: billing-charge-calculation, Property 3: Pipeline Step 실행 순서 보장")
class PipelineEnginePropertyTest {

    private final PipelineEngine engine = new PipelineEngine();

    @Property(tries = 100)
    void stepsShouldBeExecutedInOrderDefinedByGetOrder(
            @ForAll("shuffledStepOrders") List<Integer> orders) {

        List<String> executionLog = new ArrayList<>();

        // 임의의 order 값을 가진 Step 목록 생성 (셔플된 상태)
        List<ChargeItemStep> steps = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            String stepId = "STEP_" + i;
            int order = orders.get(i);
            steps.add(new RecordingStep(stepId, order, executionLog));
        }

        Pipeline pipeline = new Pipeline("TEST_PIPELINE", steps);
        ChargeContext context = createContext();
        DataAccessStrategy strategy = new NoOpStrategy();

        engine.execute(pipeline, context, strategy);

        // 실행된 Step 수가 입력 Step 수와 일치해야 함
        assertThat(executionLog).hasSize(orders.size());

        // 실행 순서가 order 오름차순이어야 함
        List<Integer> sortedOrders = orders.stream().sorted().toList();
        for (int i = 0; i < executionLog.size(); i++) {
            // executionLog에 기록된 stepId에서 원래 인덱스를 추출하여 order 확인
            String executedStepId = executionLog.get(i);
            int originalIndex = Integer.parseInt(executedStepId.replace("STEP_", ""));
            assertThat(orders.get(originalIndex)).isEqualTo(sortedOrders.get(i));
        }
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<Integer>> shuffledStepOrders() {
        return Arbitraries.integers().between(1, 100)
                .list().ofMinSize(1).ofMaxSize(15)
                .uniqueElements()
                .map(list -> {
                    List<Integer> shuffled = new ArrayList<>(list);
                    Collections.shuffle(shuffled);
                    return shuffled;
                });
    }

    // --- Test Doubles ---

    /**
     * 실행 순서를 기록하는 Step 구현체.
     */
    private static class RecordingStep implements ChargeItemStep {
        private final String stepId;
        private final int order;
        private final List<String> executionLog;

        RecordingStep(String stepId, int order, List<String> executionLog) {
            this.stepId = stepId;
            this.order = order;
            this.executionLog = executionLog;
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
            executionLog.add(stepId);
        }

        @Override
        public boolean requiresStatusUpdate() {
            return false;
        }
    }

    /**
     * No-op DataAccessStrategy for testing.
     */
    private static class NoOpStrategy implements DataAccessStrategy {
        @Override
        public UseCaseType supportedUseCaseType() {
            return UseCaseType.REALTIME_QUERY;
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

    private ChargeContext createContext() {
        ContractInfo contractInfo = new ContractInfo(
                "CONTRACT-001", "SUB-001", "PROD-001",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        ChargeInput chargeInput = ChargeInput.builder().build();
        return ChargeContext.of("TENANT-001", contractInfo, chargeInput);
    }
}

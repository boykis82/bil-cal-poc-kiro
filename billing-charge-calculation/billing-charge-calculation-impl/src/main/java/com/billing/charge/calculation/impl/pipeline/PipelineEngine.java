package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.exception.ProcessingStatusUpdateException;
import com.billing.charge.calculation.api.exception.StepExecutionException;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.step.ChargeItemStep;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import org.springframework.stereotype.Component;

/**
 * Pipeline을 실행하는 엔진.
 * Step 목록을 order 순서대로 실행하고, 처리 상태 갱신을 관리한다.
 */
@Component
public class PipelineEngine {

    /**
     * Pipeline을 실행한다.
     *
     * @param pipeline 실행할 Pipeline (Step 목록 포함)
     * @param context  요금 계산 컨텍스트
     * @param strategy 데이터 접근 전략
     * @throws StepExecutionException          Step 실행 중 오류 발생 시
     * @throws ProcessingStatusUpdateException 처리 상태 DB 갱신 중 오류 발생 시
     */
    public void execute(Pipeline pipeline, ChargeContext context, DataAccessStrategy strategy) {
        String contractId = context.getContractInfo().contractId();

        for (ChargeItemStep step : pipeline.getSteps()) {
            try {
                step.process(context);
            } catch (Exception e) {
                throw new StepExecutionException(step.getStepId(), contractId, e);
            }

            if (step.requiresStatusUpdate()) {
                try {
                    strategy.updateProcessingStatus(step.getStepId(), ProcessingStatus.COMPLETED);
                } catch (Exception e) {
                    throw new ProcessingStatusUpdateException(step.getStepId(), contractId);
                }
            }
        }
    }
}

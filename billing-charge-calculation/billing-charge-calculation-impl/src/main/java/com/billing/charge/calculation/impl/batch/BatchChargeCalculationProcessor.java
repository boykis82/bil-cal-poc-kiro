package com.billing.charge.calculation.impl.batch;

import com.billing.charge.calculation.api.dto.ContractChargeResult;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.exception.ProcessingStatusUpdateException;
import com.billing.charge.calculation.api.exception.StepExecutionException;
import com.billing.charge.calculation.impl.pipeline.Pipeline;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 정기청구 배치 연동 프로세서.
 * Spring Batch chunk 단위로 전달된 복수 건의 계약정보를 일괄 처리한다.
 * 개별 계약 실패 시 해당 건만 실패 처리하고 나머지 계약은 계속 진행한다.
 *
 * Requirements: 1.4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchChargeCalculationProcessor {

    private final PipelineConfigurator pipelineConfigurator;
    private final PipelineEngine pipelineEngine;
    private final DataAccessStrategyResolver strategyResolver;

    /**
     * chunk 단위 복수 건 계약정보를 처리한다.
     *
     * @param tenantId    테넌트 ID
     * @param productType 상품 유형
     * @param useCaseType 요금 계산 유스케이스 구분
     * @param contracts   chunk 단위 계약정보 리스트
     * @return 계약별 요금 계산 결과 리스트 (실패 건은 빈 결과 포함)
     */
    public List<ContractChargeResult> processChunk(String tenantId, ProductType productType,
            UseCaseType useCaseType, List<ContractInfo> contracts) {
        DataAccessStrategy strategy = strategyResolver.resolve(useCaseType);
        Pipeline pipeline = pipelineConfigurator.configure(tenantId, productType, useCaseType);

        List<ContractChargeResult> results = new ArrayList<>();
        for (ContractInfo contract : contracts) {
            try {
                ChargeInput input = strategy.readChargeInput(contract);
                ChargeContext context = ChargeContext.of(tenantId, contract, input);
                pipelineEngine.execute(pipeline, context, strategy);
                strategy.writeChargeResult(context.toChargeResult());
                results.add(context.toContractChargeResult());
            } catch (StepExecutionException e) {
                log.error("요금 계산 실패: contractId={}, stepId={}", contract.contractId(), e.getStepId(), e);
                results.add(new ContractChargeResult(contract.contractId(), List.of()));
            } catch (ProcessingStatusUpdateException e) {
                log.error("처리 상태 갱신 실패: contractId={}, stepId={}", contract.contractId(), e.getStepId(), e);
                results.add(new ContractChargeResult(contract.contractId(), List.of()));
            }
        }
        return results;
    }
}

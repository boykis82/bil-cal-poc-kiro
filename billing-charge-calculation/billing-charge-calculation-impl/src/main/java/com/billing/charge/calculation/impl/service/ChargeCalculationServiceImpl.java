package com.billing.charge.calculation.impl.service;

import com.billing.charge.calculation.api.ChargeCalculationService;
import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.dto.ChargeCalculationResponse;
import com.billing.charge.calculation.api.dto.ContractChargeResult;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.exception.InvalidRequestException;
import com.billing.charge.calculation.impl.dataloader.DataLoadOrchestrator;
import com.billing.charge.calculation.impl.pipeline.Pipeline;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 요금 계산 단일 진입점 서비스 구현체.
 * 유효성 검증 → Strategy 결정 → Pipeline 구성 → 일괄 데이터 로딩 → 계약정보 건별 처리 흐름을 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeCalculationServiceImpl implements ChargeCalculationService {

    private final PipelineConfigurator pipelineConfigurator;
    private final PipelineEngine pipelineEngine;
    private final DataAccessStrategyResolver strategyResolver;
    private final DataLoadOrchestrator dataLoadOrchestrator;

    @Override
    public ChargeCalculationResponse calculate(ChargeCalculationRequest request) {
        // 1. 유효성 검증
        validate(request);

        // 2. DataAccessStrategy 결정
        DataAccessStrategy strategy = strategyResolver.resolve(request.getUseCaseType());

        // 3. Pipeline 구성
        Pipeline pipeline = pipelineConfigurator.configure(
                request.getTenantId(),
                request.getProductType(),
                request.getUseCaseType());

        // 4. Strategy에서 로더 획득 후 DataLoadOrchestrator를 통한 일괄 데이터 로딩
        List<String> contractIds = request.getContracts().stream()
                .map(ContractInfo::contractId)
                .toList();

        Map<String, ChargeInput> chargeInputMap = dataLoadOrchestrator.loadAll(
                strategy.getContractBaseLoader(),
                strategy.getChargeItemDataLoaders(),
                contractIds);

        // 5. 계약정보 건별 처리
        List<ContractChargeResult> results = new ArrayList<>();
        for (ContractInfo contract : request.getContracts()) {
            // 5-1. 일괄 로딩된 ChargeInput 조회 (없으면 빈 ChargeInput 사용)
            ChargeInput input = chargeInputMap.getOrDefault(
                    contract.contractId(), ChargeInput.builder().build());

            // 5-2. ChargeContext 생성
            ChargeContext context = ChargeContext.of(request.getTenantId(), contract, input);

            // 5-3. Pipeline 실행
            pipelineEngine.execute(pipeline, context, strategy);

            // 5-4. 결과 저장 (전략에 따라 no-op 가능)
            strategy.writeChargeResult(context.toChargeResult());

            results.add(context.toContractChargeResult());
        }

        return ChargeCalculationResponse.of(results);
    }

    private void validate(ChargeCalculationRequest request) {
        if (request.getUseCaseType() == null) {
            throw new InvalidRequestException("useCaseType", "유스케이스 구분 값이 누락되었습니다.");
        }
        if (request.getContracts() == null || request.getContracts().isEmpty()) {
            throw new InvalidRequestException("contracts", "계약정보 리스트가 비어 있습니다.");
        }
    }
}

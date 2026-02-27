package com.billing.charge.calculation.internal.configurator;

import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.exception.PipelineConfigNotFoundException;
import com.billing.charge.calculation.internal.mapper.PipelineConfigMapper;
import com.billing.charge.calculation.internal.mapper.dto.PipelineConfigDto;
import com.billing.charge.calculation.internal.mapper.dto.PipelineStepConfigDto;
import com.billing.charge.calculation.internal.step.ChargeItemStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DB 기반 Pipeline 구성기.
 * PIPELINE_CONFIG, PIPELINE_STEP_CONFIG 테이블에서 구성 정보를 읽어
 * tenantId + productType + useCaseType 조합에 맞는 Step 목록을 구성한다.
 *
 * Spring Bean으로 등록된 ChargeItemStep들을 stepId로 매핑하여 사용한다.
 */
@Slf4j
@Component
public class DbPipelineConfigurator {

    private final PipelineConfigMapper pipelineConfigMapper;
    private final Map<String, ChargeItemStep> stepRegistry;

    public DbPipelineConfigurator(PipelineConfigMapper pipelineConfigMapper,
            List<ChargeItemStep> steps) {
        this.pipelineConfigMapper = pipelineConfigMapper;
        this.stepRegistry = steps.stream()
                .collect(Collectors.toMap(ChargeItemStep::getStepId, Function.identity()));
    }

    /**
     * 주어진 조건에 맞는 ChargeItemStep 목록을 구성하여 반환한다.
     *
     * @param tenantId    테넌트 ID
     * @param productType 상품 유형
     * @param useCaseType 요금 계산 유스케이스 구분
     * @return pipelineConfigId와 구성된 Step 목록
     */
    public PipelineConfiguration configure(String tenantId, ProductType productType, UseCaseType useCaseType) {
        // 1. PIPELINE_CONFIG 조회
        PipelineConfigDto config = pipelineConfigMapper.findByTenantAndProductAndUseCase(
                tenantId, productType.name(), useCaseType.name());

        if (config == null) {
            throw new PipelineConfigNotFoundException(tenantId, productType, useCaseType);
        }

        // 2. PIPELINE_STEP_CONFIG 조회 (STEP_ORDER 순)
        List<PipelineStepConfigDto> stepConfigs = pipelineConfigMapper
                .findStepsByPipelineConfigId(config.getPipelineConfigId());

        // 3. stepId → ChargeItemStep Bean 매핑
        List<ChargeItemStep> resolvedSteps = stepConfigs.stream()
                .map(stepConfig -> {
                    ChargeItemStep step = stepRegistry.get(stepConfig.getStepId());
                    if (step == null) {
                        log.warn("등록된 ChargeItemStep Bean을 찾을 수 없음: stepId={}", stepConfig.getStepId());
                    }
                    return step;
                })
                .filter(step -> step != null)
                .toList();

        return new PipelineConfiguration(config.getPipelineConfigId(), resolvedSteps);
    }

    /**
     * Pipeline 구성 결과를 담는 record.
     * impl 모듈에서 Pipeline 객체로 변환하여 사용한다.
     */
    public record PipelineConfiguration(String pipelineConfigId, List<ChargeItemStep> steps) {
    }
}

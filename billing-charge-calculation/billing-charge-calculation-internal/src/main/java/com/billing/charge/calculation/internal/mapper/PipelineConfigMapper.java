package com.billing.charge.calculation.internal.mapper;

import com.billing.charge.calculation.internal.mapper.dto.PipelineConfigDto;
import com.billing.charge.calculation.internal.mapper.dto.PipelineStepConfigDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PIPELINE_CONFIG, PIPELINE_STEP_CONFIG 테이블 조회 MyBatis Mapper.
 * XML 매핑 파일은 Task 20에서 생성 예정.
 */
@Mapper
public interface PipelineConfigMapper {

    /**
     * 테넌트ID, 상품유형, 유스케이스 조합으로 활성화된 Pipeline 구성을 조회한다.
     *
     * @param tenantId    테넌트 ID
     * @param productType 상품 유형
     * @param useCaseType 유스케이스 유형
     * @return Pipeline 구성 DTO (없으면 null)
     */
    PipelineConfigDto findByTenantAndProductAndUseCase(
            @Param("tenantId") String tenantId,
            @Param("productType") String productType,
            @Param("useCaseType") String useCaseType);

    /**
     * Pipeline 구성 ID로 활성화된 Step 구성 목록을 STEP_ORDER 순으로 조회한다.
     *
     * @param pipelineConfigId Pipeline 구성 ID
     * @return Step 구성 DTO 목록 (STEP_ORDER 오름차순)
     */
    List<PipelineStepConfigDto> findStepsByPipelineConfigId(
            @Param("pipelineConfigId") String pipelineConfigId);
}

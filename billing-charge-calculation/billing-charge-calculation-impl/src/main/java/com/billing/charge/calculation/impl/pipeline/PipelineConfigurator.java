package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;

/**
 * 테넌트ID, 상품유형, 유스케이스에 따라 Pipeline을 구성한다.
 * Pipeline 구성 정보는 DB(PIPELINE_CONFIG, PIPELINE_STEP_CONFIG)에서 관리한다.
 */
public interface PipelineConfigurator {

    /**
     * 주어진 조건에 맞는 Pipeline을 구성하여 반환한다.
     *
     * @param tenantId    테넌트 ID
     * @param productType 상품 유형
     * @param useCaseType 요금 계산 유스케이스 구분
     * @return 구성된 Pipeline (Step 목록 포함)
     */
    Pipeline configure(String tenantId, ProductType productType, UseCaseType useCaseType);
}

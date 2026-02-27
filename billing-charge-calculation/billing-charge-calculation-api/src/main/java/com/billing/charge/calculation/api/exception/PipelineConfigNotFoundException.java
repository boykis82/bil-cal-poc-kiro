package com.billing.charge.calculation.api.exception;

import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import lombok.Getter;

/**
 * Pipeline 구성 미존재 예외.
 * 해당 테넌트/상품유형/유스케이스 조합의 Pipeline 설정이 없는 경우 발생한다.
 */
@Getter
public class PipelineConfigNotFoundException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_PIPELINE_CONFIG_NOT_FOUND";

    private final String tenantId;
    private final ProductType productType;
    private final UseCaseType useCaseType;

    public PipelineConfigNotFoundException(String tenantId, ProductType productType, UseCaseType useCaseType) {
        super(ERROR_CODE, String.format("Pipeline 구성을 찾을 수 없음: tenantId=%s, productType=%s, useCaseType=%s",
                tenantId, productType, useCaseType));
        this.tenantId = tenantId;
        this.productType = productType;
        this.useCaseType = useCaseType;
    }
}

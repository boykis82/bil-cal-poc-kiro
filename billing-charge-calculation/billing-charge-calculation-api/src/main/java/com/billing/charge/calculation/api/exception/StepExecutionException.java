package com.billing.charge.calculation.api.exception;

import lombok.Getter;

/**
 * Step 실행 오류 예외.
 * 개별 Step 내 비즈니스 로직 오류 발생 시 사용한다.
 */
@Getter
public class StepExecutionException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_STEP_EXECUTION_ERROR";

    private final String stepId;
    private final String contractId;

    public StepExecutionException(String stepId, String contractId, Throwable cause) {
        super(ERROR_CODE, String.format("Step 실행 오류: stepId=%s, contractId=%s", stepId, contractId), cause);
        this.stepId = stepId;
        this.contractId = contractId;
    }
}

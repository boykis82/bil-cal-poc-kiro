package com.billing.charge.calculation.api.exception;

import lombok.Getter;

/**
 * 처리 상태 DB 갱신 오류 예외.
 * DB 갱신 실패 시 해당 계약의 요금 계산을 실패 처리한다.
 */
@Getter
public class ProcessingStatusUpdateException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_PROCESSING_STATUS_UPDATE_ERROR";

    private final String stepId;
    private final String contractId;

    public ProcessingStatusUpdateException(String stepId, String contractId) {
        super(ERROR_CODE, String.format("처리 상태 갱신 실패: stepId=%s, contractId=%s", stepId, contractId));
        this.stepId = stepId;
        this.contractId = contractId;
    }
}

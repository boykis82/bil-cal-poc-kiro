package com.billing.charge.calculation.api.exception;

import lombok.Getter;

/**
 * 유효성 검증 오류 예외.
 * 유스케이스 구분 누락, 빈 계약정보 리스트 등의 경우 발생한다.
 */
@Getter
public class InvalidRequestException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_INVALID_REQUEST";

    private final String fieldName;
    private final String reason;

    public InvalidRequestException(String fieldName, String reason) {
        super(ERROR_CODE, String.format("유효하지 않은 요청: %s - %s", fieldName, reason));
        this.fieldName = fieldName;
        this.reason = reason;
    }
}

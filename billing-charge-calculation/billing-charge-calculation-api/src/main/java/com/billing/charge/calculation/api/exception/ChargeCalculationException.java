package com.billing.charge.calculation.api.exception;

import lombok.Getter;

/**
 * 요금 계산 모듈의 추상 기반 예외 클래스.
 * 모든 요금 계산 관련 예외는 이 클래스를 상속한다.
 */
@Getter
public abstract class ChargeCalculationException extends RuntimeException {

    private final String errorCode;

    protected ChargeCalculationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ChargeCalculationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

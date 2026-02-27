package com.billing.charge.calculation.api.exception;

import com.billing.charge.calculation.api.enums.UseCaseType;
import lombok.Getter;

/**
 * 지원하지 않는 유스케이스 유형 예외.
 * 등록되지 않은 유스케이스 유형으로 요금 계산을 요청한 경우 발생한다.
 */
@Getter
public class UnsupportedUseCaseException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_UNSUPPORTED_USE_CASE";

    private final UseCaseType useCaseType;

    public UnsupportedUseCaseException(UseCaseType useCaseType) {
        super(ERROR_CODE, String.format("지원하지 않는 유스케이스 유형: %s", useCaseType));
        this.useCaseType = useCaseType;
    }
}

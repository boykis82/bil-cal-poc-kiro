package com.billing.charge.calculation.api.exception;

import lombok.Getter;

/**
 * 데이터 로딩 단계에서 발생하는 오류를 구분하기 위한 전용 예외.
 * ContractBaseLoader, ChargeItemDataLoader, OneTimeChargeDataLoader, UsageChargeDataLoader
 * 실행 중 발생하는 오류를 래핑한다.
 */
@Getter
public class DataLoadException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_DATA_LOAD_ERROR";

    public DataLoadException(String message) {
        super(ERROR_CODE, message);
    }

    public DataLoadException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}

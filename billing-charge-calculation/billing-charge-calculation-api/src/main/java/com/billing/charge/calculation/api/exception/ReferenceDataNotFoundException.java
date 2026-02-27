package com.billing.charge.calculation.api.exception;

import lombok.Getter;

/**
 * 기준정보 조회 실패 예외.
 * 캐시 미스 후 DB 조회도 실패한 경우 발생한다.
 */
@Getter
public class ReferenceDataNotFoundException extends ChargeCalculationException {

    private static final String ERROR_CODE = "CHARGE_REFERENCE_DATA_NOT_FOUND";

    private final String key;

    public ReferenceDataNotFoundException(String key) {
        super(ERROR_CODE, String.format("기준정보를 찾을 수 없음: key=%s", key));
        this.key = key;
    }
}

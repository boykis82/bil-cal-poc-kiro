package com.billing.charge.calculation.impl.cache;

/**
 * 기준정보 캐시 키.
 * 기준정보 유형과 키 값의 조합으로 캐시 항목을 식별한다.
 *
 * @param type     기준정보 유형
 * @param keyValue 기준정보 식별 키 값
 */
public record ReferenceDataKey(
        ReferenceDataType type,
        String keyValue) {
}

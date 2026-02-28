package com.billing.charge.calculation.internal.model;

/**
 * 일회성 요금 도메인 마커 인터페이스.
 * 다양한 유형의 일회성 요금(할부이력, 위약금, 가입비 등) 데이터를 추상화한다.
 * 제네릭 데이터 로더(OneTimeChargeDataLoader)에서 타입 바운드로 사용된다.
 */
public interface OneTimeChargeDomain {

    /**
     * 이 도메인 데이터가 속한 계약ID를 반환한다.
     *
     * @return 계약ID
     */
    String getContractId();
}

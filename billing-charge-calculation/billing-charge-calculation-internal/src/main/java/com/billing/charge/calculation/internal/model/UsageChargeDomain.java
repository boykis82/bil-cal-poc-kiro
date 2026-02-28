package com.billing.charge.calculation.internal.model;

/**
 * 통화료/종량료 도메인 마커 인터페이스.
 * 다양한 유형의 통화료와 종량료(음성통화, 데이터 사용량 등) 데이터를 추상화한다.
 * 제네릭 데이터 로더(UsageChargeDataLoader)에서 타입 바운드로 사용된다.
 */
public interface UsageChargeDomain {

    /**
     * 이 도메인 데이터가 속한 계약ID를 반환한다.
     *
     * @return 계약ID
     */
    String getContractId();
}

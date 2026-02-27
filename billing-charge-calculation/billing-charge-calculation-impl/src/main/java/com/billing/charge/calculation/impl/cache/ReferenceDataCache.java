package com.billing.charge.calculation.impl.cache;

import java.util.Collection;

/**
 * 기준정보 인메모리 캐시 인터페이스.
 * 요금 계산 로직은 캐시 사용 여부를 인지하지 않고 투명하게 기준정보를 조회한다.
 */
public interface ReferenceDataCache {

    /**
     * 기준정보를 조회한다.
     * 캐시 히트 시 메모리에서, 미스 시 DB에서 조회 후 캐싱한다.
     *
     * @param tenantId 테넌트 ID
     * @param key      기준정보 캐시 키
     * @param type     반환 타입 클래스
     * @param <T>      반환 타입
     * @return 기준정보 데이터
     */
    <T> T getReferenceData(String tenantId, ReferenceDataKey key, Class<T> type);

    /**
     * 정기청구 배치 시작 시 필요한 기준정보를 사전 적재한다.
     *
     * @param tenantId 테넌트 ID
     * @param keys     사전 적재할 기준정보 키 목록
     */
    void preload(String tenantId, Collection<ReferenceDataKey> keys);

    /**
     * 특정 기준정보의 캐시를 무효화한다.
     * OLTP 업무 중 기준정보 변경 시 사용한다.
     *
     * @param tenantId 테넌트 ID
     * @param key      무효화할 기준정보 캐시 키
     */
    void invalidate(String tenantId, ReferenceDataKey key);

    /**
     * 테넌트의 전체 캐시를 무효화한다.
     *
     * @param tenantId 테넌트 ID
     */
    void invalidateAll(String tenantId);
}

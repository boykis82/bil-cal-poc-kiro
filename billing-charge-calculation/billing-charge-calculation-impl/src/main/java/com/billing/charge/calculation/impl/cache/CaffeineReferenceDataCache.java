package com.billing.charge.calculation.impl.cache;

import com.billing.charge.calculation.internal.mapper.ReferenceDataMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caffeine 기반 기준정보 캐시 구현체.
 * 테넌트별 캐시 분리, 사전 적재(preload), 무효화를 지원한다.
 * 캐시 미스 시 DB fallback으로 ReferenceDataMapper를 통해 조회 후 캐싱한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaffeineReferenceDataCache implements ReferenceDataCache {

    /** 테넌트별 캐시 인스턴스. key = tenantId */
    private final ConcurrentHashMap<String, Cache<ReferenceDataKey, Object>> tenantCaches = new ConcurrentHashMap<>();

    private final ReferenceDataMapper referenceDataMapper;

    @Override
    public <T> T getReferenceData(String tenantId, ReferenceDataKey key, Class<T> type) {
        Cache<ReferenceDataKey, Object> cache = getOrCreateCache(tenantId);
        Object value = cache.get(key,
                k -> referenceDataMapper.selectReferenceData(tenantId, k.type().name(), k.keyValue()));
        return type.cast(value);
    }

    @Override
    public void preload(String tenantId, Collection<ReferenceDataKey> keys) {
        Cache<ReferenceDataKey, Object> cache = getOrCreateCache(tenantId);

        List<Map<String, String>> mapKeys = keys.stream()
                .map(k -> Map.of("type", k.type().name(), "keyValue", k.keyValue()))
                .toList();

        List<Map<String, Object>> rows = referenceDataMapper.selectBulkReferenceData(tenantId, mapKeys);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String typeName = (String) row.get("type");
                String keyValue = (String) row.get("keyValue");
                Object data = row.get("data");
                if (typeName != null && keyValue != null && data != null) {
                    ReferenceDataKey refKey = new ReferenceDataKey(
                            ReferenceDataType.valueOf(typeName), keyValue);
                    cache.put(refKey, data);
                }
            }
        }
        log.info("기준정보 사전 적재 완료: tenantId={}, keys={}", tenantId, keys.size());
    }

    @Override
    public void invalidate(String tenantId, ReferenceDataKey key) {
        Cache<ReferenceDataKey, Object> cache = tenantCaches.get(tenantId);
        if (cache != null) {
            cache.invalidate(key);
            log.debug("캐시 무효화: tenantId={}, key={}", tenantId, key);
        }
    }

    @Override
    public void invalidateAll(String tenantId) {
        Cache<ReferenceDataKey, Object> cache = tenantCaches.get(tenantId);
        if (cache != null) {
            cache.invalidateAll();
            log.info("테넌트 전체 캐시 무효화: tenantId={}", tenantId);
        }
    }

    private Cache<ReferenceDataKey, Object> getOrCreateCache(String tenantId) {
        return tenantCaches.computeIfAbsent(tenantId, id -> Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofHours(1))
                .build());
    }
}

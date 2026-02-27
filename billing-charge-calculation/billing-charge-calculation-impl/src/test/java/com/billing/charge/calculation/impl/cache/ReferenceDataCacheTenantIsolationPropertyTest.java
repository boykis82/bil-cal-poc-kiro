package com.billing.charge.calculation.impl.cache;

import com.billing.charge.calculation.internal.mapper.ReferenceDataMapper;
import net.jqwik.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 8: 테넌트별 캐시 격리
 *
 * 임의의 두 테넌트 A, B에 대해, 테넌트 A의 캐시를 무효화(invalidate 또는 invalidateAll)해도
 * 테넌트 B의 캐시 데이터는 영향을 받지 않아야 한다.
 *
 * **Validates: Requirements 17.3**
 */
@Tag("Feature: billing-charge-calculation, Property 8: 테넌트별 캐시 격리")
class ReferenceDataCacheTenantIsolationPropertyTest {

    @Property(tries = 100)
    void invalidatingTenantAShouldNotAffectTenantB(
            @ForAll("tenantIdPairs") TenantIdPair tenantPair,
            @ForAll("referenceDataKeys") ReferenceDataKey key,
            @ForAll("referenceValues") String valueA,
            @ForAll("referenceValues") String valueB) {

        Assume.that(!tenantPair.tenantA().equals(tenantPair.tenantB()));

        // 테넌트별로 다른 값을 반환하는 mapper
        TenantAwareStubMapper mapper = new TenantAwareStubMapper();
        mapper.setData(tenantPair.tenantA(), key, valueA);
        mapper.setData(tenantPair.tenantB(), key, valueB);

        CaffeineReferenceDataCache cache = new CaffeineReferenceDataCache(mapper);

        // 두 테넌트 모두 캐시에 적재
        String resultA = cache.getReferenceData(tenantPair.tenantA(), key, String.class);
        String resultB = cache.getReferenceData(tenantPair.tenantB(), key, String.class);
        assertThat(resultA).isEqualTo(valueA);
        assertThat(resultB).isEqualTo(valueB);

        // 테넌트 A의 캐시만 무효화
        cache.invalidate(tenantPair.tenantA(), key);

        // 테넌트 B의 캐시는 영향 없이 동일한 값 반환
        String resultBAfterInvalidate = cache.getReferenceData(tenantPair.tenantB(), key, String.class);
        assertThat(resultBAfterInvalidate).isEqualTo(valueB);
    }

    @Property(tries = 100)
    void invalidateAllTenantAShouldNotAffectTenantB(
            @ForAll("tenantIdPairs") TenantIdPair tenantPair,
            @ForAll("referenceDataKeys") ReferenceDataKey key,
            @ForAll("referenceValues") String valueA,
            @ForAll("referenceValues") String valueB) {

        Assume.that(!tenantPair.tenantA().equals(tenantPair.tenantB()));

        TenantAwareStubMapper mapper = new TenantAwareStubMapper();
        mapper.setData(tenantPair.tenantA(), key, valueA);
        mapper.setData(tenantPair.tenantB(), key, valueB);

        CaffeineReferenceDataCache cache = new CaffeineReferenceDataCache(mapper);

        // 두 테넌트 모두 캐시에 적재
        cache.getReferenceData(tenantPair.tenantA(), key, String.class);
        String resultB = cache.getReferenceData(tenantPair.tenantB(), key, String.class);
        assertThat(resultB).isEqualTo(valueB);

        // 테넌트 A 전체 캐시 무효화
        cache.invalidateAll(tenantPair.tenantA());

        // 테넌트 B의 캐시는 영향 없음
        String resultBAfterInvalidateAll = cache.getReferenceData(tenantPair.tenantB(), key, String.class);
        assertThat(resultBAfterInvalidateAll).isEqualTo(valueB);
    }

    // --- Generators ---

    record TenantIdPair(String tenantA, String tenantB) {
    }

    @Provide
    Arbitrary<TenantIdPair> tenantIdPairs() {
        Arbitrary<String> tenantIds = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8)
                .map(s -> "T_" + s);
        return Combinators.combine(tenantIds, tenantIds)
                .filter((a, b) -> !a.equals(b))
                .as(TenantIdPair::new);
    }

    @Provide
    Arbitrary<ReferenceDataKey> referenceDataKeys() {
        Arbitrary<ReferenceDataType> types = Arbitraries.of(ReferenceDataType.values());
        Arbitrary<String> keyValues = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(s -> "KEY_" + s);
        return Combinators.combine(types, keyValues).as(ReferenceDataKey::new);
    }

    @Provide
    Arbitrary<String> referenceValues() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
                .map(s -> "VALUE_" + s);
    }

    // --- Test Double ---

    /**
     * 테넌트별로 다른 데이터를 반환하는 Mapper stub.
     */
    private static class TenantAwareStubMapper implements ReferenceDataMapper {

        private final ConcurrentHashMap<String, Object> dataStore = new ConcurrentHashMap<>();

        void setData(String tenantId, ReferenceDataKey key, Object value) {
            dataStore.put(compositeKey(tenantId, key), value);
        }

        private String compositeKey(String tenantId, ReferenceDataKey key) {
            return tenantId + "|" + key.type().name() + "|" + key.keyValue();
        }

        @Override
        public Object selectReferenceData(String tenantId, String type, String keyValue) {
            return dataStore.get(tenantId + "|" + type + "|" + keyValue);
        }

        @Override
        public List<Map<String, Object>> selectBulkReferenceData(
                String tenantId, List<Map<String, String>> keys) {
            return List.of();
        }
    }
}

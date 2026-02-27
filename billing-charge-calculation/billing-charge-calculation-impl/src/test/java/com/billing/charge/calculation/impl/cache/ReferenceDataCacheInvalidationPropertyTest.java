package com.billing.charge.calculation.impl.cache;

import com.billing.charge.calculation.internal.mapper.ReferenceDataMapper;
import net.jqwik.api.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 7: 캐시 무효화 후 최신 데이터 반환
 *
 * 임의의 캐시된 기준정보에 대해, invalidate() 호출 후 다시 조회하면
 * DB의 최신 데이터가 반환되어야 한다. (캐시 무효화 → 재조회 = DB 최신값)
 *
 * **Validates: Requirements 4.3, 4.4**
 */
@Tag("Feature: billing-charge-calculation, Property 7: 캐시 무효화 후 최신 데이터 반환")
class ReferenceDataCacheInvalidationPropertyTest {

    @Property(tries = 100)
    void afterInvalidateShouldReturnFreshDataFromDb(
            @ForAll("tenantIds") String tenantId,
            @ForAll("referenceDataKeys") ReferenceDataKey key,
            @ForAll("referenceValues") String originalValue,
            @ForAll("referenceValues") String updatedValue) {

        Assume.that(!originalValue.equals(updatedValue));

        // AtomicReference로 DB 데이터 변경을 시뮬레이션
        AtomicReference<String> dbValue = new AtomicReference<>(originalValue);
        MutableStubMapper mapper = new MutableStubMapper(dbValue);
        CaffeineReferenceDataCache cache = new CaffeineReferenceDataCache(mapper);

        // 최초 조회 → 캐시에 originalValue 적재
        String firstResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(firstResult).isEqualTo(originalValue);

        // DB 데이터 변경 시뮬레이션
        dbValue.set(updatedValue);

        // 캐시 무효화 전 조회 → 여전히 캐시된 originalValue
        String cachedResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(cachedResult).isEqualTo(originalValue);

        // 캐시 무효화
        cache.invalidate(tenantId, key);

        // 무효화 후 조회 → DB에서 최신 updatedValue 반환
        String freshResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(freshResult).isEqualTo(updatedValue);
    }

    @Property(tries = 100)
    void afterInvalidateAllShouldReturnFreshDataFromDb(
            @ForAll("tenantIds") String tenantId,
            @ForAll("referenceDataKeys") ReferenceDataKey key,
            @ForAll("referenceValues") String originalValue,
            @ForAll("referenceValues") String updatedValue) {

        Assume.that(!originalValue.equals(updatedValue));

        AtomicReference<String> dbValue = new AtomicReference<>(originalValue);
        MutableStubMapper mapper = new MutableStubMapper(dbValue);
        CaffeineReferenceDataCache cache = new CaffeineReferenceDataCache(mapper);

        // 최초 조회 → 캐시에 적재
        String firstResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(firstResult).isEqualTo(originalValue);

        // DB 데이터 변경
        dbValue.set(updatedValue);

        // invalidateAll 호출
        cache.invalidateAll(tenantId);

        // 재조회 → DB 최신값
        String freshResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(freshResult).isEqualTo(updatedValue);
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> tenantIds() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                .map(s -> "TENANT_" + s);
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
     * DB 데이터 변경을 시뮬레이션하는 Mapper stub.
     * AtomicReference를 통해 반환 값을 동적으로 변경할 수 있다.
     */
    private static class MutableStubMapper implements ReferenceDataMapper {

        private final AtomicReference<String> currentValue;

        MutableStubMapper(AtomicReference<String> currentValue) {
            this.currentValue = currentValue;
        }

        @Override
        public Object selectReferenceData(String tenantId, String type, String keyValue) {
            return currentValue.get();
        }

        @Override
        public List<Map<String, Object>> selectBulkReferenceData(
                String tenantId, List<Map<String, String>> keys) {
            return List.of();
        }
    }
}

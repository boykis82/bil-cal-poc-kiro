package com.billing.charge.calculation.impl.cache;

import com.billing.charge.calculation.internal.mapper.ReferenceDataMapper;
import net.jqwik.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 6: 기준정보 캐시 라운드트립
 *
 * 임의의 기준정보에 대해, ReferenceDataCache에 적재(preload 또는 최초 조회)한 후
 * 동일 키로 조회하면 원본 데이터와 동일한 값이 반환되어야 한다.
 *
 * **Validates: Requirements 4.1**
 */
@Tag("Feature: billing-charge-calculation, Property 6: 기준정보 캐시 라운드트립")
class ReferenceDataCacheRoundTripPropertyTest {

    @Property(tries = 100)
    void cachedDataShouldMatchOriginalOnSubsequentQuery(
            @ForAll("tenantIds") String tenantId,
            @ForAll("referenceDataKeys") ReferenceDataKey key,
            @ForAll("referenceValues") String originalValue) {

        // Stub mapper: 항상 originalValue를 반환
        StubReferenceDataMapper mapper = new StubReferenceDataMapper(originalValue);
        CaffeineReferenceDataCache cache = new CaffeineReferenceDataCache(mapper);

        // 최초 조회 (캐시 미스 → DB fallback → 캐싱)
        String firstResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(firstResult).isEqualTo(originalValue);

        // 두 번째 조회 (캐시 히트)
        String secondResult = cache.getReferenceData(tenantId, key, String.class);
        assertThat(secondResult).isEqualTo(originalValue);

        // mapper가 첫 번째 조회에서만 호출되었는지 확인 (캐시 히트 검증)
        assertThat(mapper.getSelectCallCount()).isEqualTo(1);
    }

    @Property(tries = 100)
    void preloadedDataShouldBeRetrievableByKey(
            @ForAll("tenantIds") String tenantId,
            @ForAll("referenceDataKeys") ReferenceDataKey key,
            @ForAll("referenceValues") String originalValue) {

        // Stub mapper: preload용 bulk 데이터 반환
        StubReferenceDataMapper mapper = new StubReferenceDataMapper(originalValue);
        mapper.setBulkData(key, originalValue);
        CaffeineReferenceDataCache cache = new CaffeineReferenceDataCache(mapper);

        // preload로 사전 적재
        cache.preload(tenantId, List.of(key));

        // 조회 시 preload된 데이터가 반환되어야 함
        String result = cache.getReferenceData(tenantId, key, String.class);
        assertThat(result).isEqualTo(originalValue);

        // preload 후 조회이므로 selectReferenceData는 호출되지 않아야 함
        assertThat(mapper.getSelectCallCount()).isEqualTo(0);
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
     * 테스트용 ReferenceDataMapper stub.
     * selectReferenceData 호출 시 고정 값을 반환하고 호출 횟수를 기록한다.
     */
    private static class StubReferenceDataMapper implements ReferenceDataMapper {

        private final String fixedValue;
        private int selectCallCount = 0;
        private final Map<String, Object> bulkDataMap = new HashMap<>();

        StubReferenceDataMapper(String fixedValue) {
            this.fixedValue = fixedValue;
        }

        void setBulkData(ReferenceDataKey key, Object value) {
            bulkDataMap.put(key.type().name() + "|" + key.keyValue(), value);
        }

        int getSelectCallCount() {
            return selectCallCount;
        }

        @Override
        public Object selectReferenceData(String tenantId, String type, String keyValue) {
            selectCallCount++;
            return fixedValue;
        }

        @Override
        public List<Map<String, Object>> selectBulkReferenceData(
                String tenantId, List<Map<String, String>> keys) {
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, String> keyMap : keys) {
                String type = keyMap.get("type");
                String keyValue = keyMap.get("keyValue");
                String compositeKey = type + "|" + keyValue;
                Object data = bulkDataMap.get(compositeKey);
                if (data != null) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("type", type);
                    row.put("keyValue", keyValue);
                    row.put("data", data);
                    results.add(row);
                }
            }
            return results;
        }
    }
}

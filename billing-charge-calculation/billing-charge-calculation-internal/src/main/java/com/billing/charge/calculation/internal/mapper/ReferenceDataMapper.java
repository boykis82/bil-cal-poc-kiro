package com.billing.charge.calculation.internal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 기준정보 테이블 조회 MyBatis Mapper.
 * XML 매핑 파일은 Task 20에서 생성 예정.
 *
 * 기준정보 유형(type)과 키 값(keyValue)을 기반으로 기준정보를 조회한다.
 * impl 모듈의 ReferenceDataKey를 직접 참조하지 않고 primitive 파라미터를 사용한다.
 */
@Mapper
public interface ReferenceDataMapper {

    /**
     * 단건 기준정보를 조회한다.
     *
     * @param tenantId 테넌트 ID
     * @param type     기준정보 유형 (ReferenceDataType의 name())
     * @param keyValue 기준정보 식별 키 값
     * @return 기준정보 데이터 (없으면 null)
     */
    Object selectReferenceData(
            @Param("tenantId") String tenantId,
            @Param("type") String type,
            @Param("keyValue") String keyValue);

    /**
     * 복수 건의 기준정보를 일괄 조회한다.
     * 정기청구 배치 시작 시 bulk 적재에 사용한다.
     *
     * @param tenantId 테넌트 ID
     * @param keys     조회할 기준정보 키 목록 (각 항목은 type, keyValue 필드를 가진 Map)
     * @return 기준정보 키-값 맵 (key: "type|keyValue" 형태의 복합키, value: 기준정보 데이터)
     */
    List<Map<String, Object>> selectBulkReferenceData(
            @Param("tenantId") String tenantId,
            @Param("keys") List<Map<String, String>> keys);
}

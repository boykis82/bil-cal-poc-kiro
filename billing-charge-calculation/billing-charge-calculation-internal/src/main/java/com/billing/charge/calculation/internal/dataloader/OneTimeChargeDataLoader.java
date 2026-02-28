package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.model.OneTimeChargeDomain;

import java.util.List;
import java.util.Map;

/**
 * 일회성 요금 유형별 제네릭 데이터 로더 인터페이스.
 * <p>
 * {@link OneTimeChargeDomain} 구현체별로 특화된 데이터를 chunk 단위로 조회한다.
 * 새로운 일회성 요금 유형(할부이력, 위약금, 가입비 등)이 추가될 때
 * 기존 구현체를 변경하지 않고 새로운 {@code OneTimeChargeDomain} 구현체와
 * 해당 로더를 추가하는 것만으로 동작한다 (OCP 준수).
 * </p>
 *
 * @param <T> {@link OneTimeChargeDomain}을 구현하는 일회성 요금 도메인 타입
 */
public interface OneTimeChargeDataLoader<T extends OneTimeChargeDomain> {

    /**
     * 이 로더가 담당하는 일회성 요금 도메인 클래스를 반환한다.
     *
     * @return 도메인 클래스 타입
     */
    Class<T> getDomainType();

    /**
     * chunk 단위로 데이터를 조회하여 계약ID별로 그룹핑하여 반환한다.
     *
     * @param contracts chunk 내 계약정보 리스트
     * @return 계약ID → 도메인 데이터 리스트 매핑
     */
    Map<String, List<T>> loadData(List<ContractInfo> contracts);
}

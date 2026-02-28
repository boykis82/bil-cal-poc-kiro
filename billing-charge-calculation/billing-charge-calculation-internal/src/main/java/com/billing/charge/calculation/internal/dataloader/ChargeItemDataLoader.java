package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.model.ChargeInput;

import java.util.List;
import java.util.Map;

/**
 * 요금항목별 데이터 로더 인터페이스.
 * <p>
 * 각 요금항목(월정액, 할인, 청구/수납, 선납내역 등)은 이 인터페이스를 구현하여
 * 독립적인 데이터 조회 로직을 제공한다.
 * Spring 컨텍스트에 Bean으로 등록되면 DataLoadOrchestrator가 자동으로 인식한다.
 * </p>
 * <p>
 * 새로운 요금항목 유형이 추가될 때 기존 구현체 및 오케스트레이터 코드를 변경하지 않고
 * 새로운 로더 구현체를 추가하는 것만으로 동작한다 (OCP 준수).
 * </p>
 */
public interface ChargeItemDataLoader {

    /**
     * 이 로더가 담당하는 요금항목 유형 식별자를 반환한다.
     *
     * @return 요금항목 유형
     */
    ChargeItemType getChargeItemType();

    /**
     * chunk 단위로 데이터를 조회하여 계약ID별로 ChargeInput에 설정한다.
     * <p>
     * 구현체는 전달받은 계약정보 리스트에 대해 한 번의 쿼리로 데이터를 조회하고,
     * 조회 결과를 계약ID 기준으로 매핑하여 chargeInputMap의 해당 ChargeInput에 설정한다.
     * </p>
     *
     * @param contracts      chunk 내 계약정보 리스트
     * @param chargeInputMap 계약ID → ChargeInput 매핑 (로더가 해당 필드를 설정)
     */
    void loadAndPopulate(List<ContractInfo> contracts, Map<String, ChargeInput> chargeInputMap);
}

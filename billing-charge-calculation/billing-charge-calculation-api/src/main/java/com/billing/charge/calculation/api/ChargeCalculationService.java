package com.billing.charge.calculation.api;

import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.dto.ChargeCalculationResponse;

/**
 * 요금 계산 단일 진입점 서비스 인터페이스.
 * billing-charge-calculation-api.jar에 위치.
 * 타 컴포넌트는 이 인터페이스를 통해 요금 계산을 요청한다.
 */
public interface ChargeCalculationService {

    /**
     * 요금 계산을 수행한다.
     * 정기청구 시 복수 건, OLTP 시 단건 처리.
     *
     * @param request 요금 계산 요청 (유스케이스 구분, 테넌트ID, 계약정보 리스트 포함)
     * @return 요금 계산 응답 (계약별 계산 결과 리스트)
     */
    ChargeCalculationResponse calculate(ChargeCalculationRequest request);
}

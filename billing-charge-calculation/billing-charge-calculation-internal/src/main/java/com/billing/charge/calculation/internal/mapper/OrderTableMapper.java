package com.billing.charge.calculation.internal.mapper;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.model.ChargeInput;
import org.apache.ibatis.annotations.Mapper;

/**
 * 접수 테이블 MyBatis Mapper.
 * 개통 완료 전 접수 테이블에서 요금 계산 입력 데이터를 조회한다.
 */
@Mapper
public interface OrderTableMapper {

    /**
     * 계약정보 기반으로 접수 테이블에서 요금 계산 입력 데이터를 조회한다.
     *
     * @param contractInfo 계약정보
     * @return 요금 계산 입력 데이터
     */
    ChargeInput readChargeInput(ContractInfo contractInfo);
}

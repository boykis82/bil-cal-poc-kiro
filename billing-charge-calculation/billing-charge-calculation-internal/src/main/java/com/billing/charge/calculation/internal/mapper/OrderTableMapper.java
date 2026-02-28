package com.billing.charge.calculation.internal.mapper;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.model.ChargeInput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 계약ID 리스트로 접수 테이블에서 기본 계약정보를 일괄 조회한다.
     * SQL IN 조건을 사용하며, 호출 측에서 1000건 단위 분할을 처리한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 계약 기본정보 리스트
     */
    List<ContractInfo> selectBaseContractsByOrder(@Param("contractIds") List<String> contractIds);
}

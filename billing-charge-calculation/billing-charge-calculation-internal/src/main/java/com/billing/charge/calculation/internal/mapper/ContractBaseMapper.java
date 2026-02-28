package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.api.dto.ContractInfo;

/**
 * 계약 기본정보 MyBatis Mapper.
 * 요금 계산 대상 가입자의 계약ID와 최소한의 기본 정보만을 조회한다.
 * 기준정보 테이블과의 join 없이 마스터 테이블에서만 데이터를 조회한다.
 */
@Mapper
public interface ContractBaseMapper {

    /**
     * 계약ID 리스트로 기본 계약정보를 조회한다.
     * SQL IN 조건을 사용하며, 호출 측에서 1000건 단위 분할을 처리한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 계약 기본정보 리스트
     */
    List<ContractInfo> selectBaseContracts(@Param("contractIds") List<String> contractIds);
}

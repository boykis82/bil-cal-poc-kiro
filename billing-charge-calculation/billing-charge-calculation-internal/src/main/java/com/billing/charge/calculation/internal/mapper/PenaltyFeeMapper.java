package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.PenaltyFee;

/**
 * 위약금 MyBatis Mapper.
 * 일회성 요금 중 위약금 데이터를 chunk 단위로 조회한다.
 */
@Mapper
public interface PenaltyFeeMapper {

    /**
     * 계약ID 리스트로 위약금을 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 위약금 리스트
     */
    List<PenaltyFee> selectPenaltyFees(@Param("contractIds") List<String> contractIds);
}

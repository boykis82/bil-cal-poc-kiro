package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.InstallmentHistory;

/**
 * 할부이력 MyBatis Mapper.
 * 일회성 요금 중 할부이력 데이터를 chunk 단위로 조회한다.
 */
@Mapper
public interface InstallmentHistoryMapper {

    /**
     * 계약ID 리스트로 할부이력을 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 할부이력 리스트
     */
    List<InstallmentHistory> selectInstallmentHistories(@Param("contractIds") List<String> contractIds);
}

package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.PrepaidRecord;

/**
 * 선납내역 MyBatis Mapper.
 * 선납반제 처리에 필요한 선납내역을 chunk 단위로 조회한다.
 */
@Mapper
public interface PrepaidMapper {

    /**
     * 계약ID 리스트로 선납내역을 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 선납내역 리스트
     */
    List<PrepaidRecord> selectPrepaidRecords(@Param("contractIds") List<String> contractIds);
}

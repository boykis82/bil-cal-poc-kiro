package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.VoiceUsage;

/**
 * 음성통화 사용량 MyBatis Mapper.
 * 통화료/종량료 중 음성통화 사용량 데이터를 chunk 단위로 조회한다.
 */
@Mapper
public interface VoiceUsageMapper {

    /**
     * 계약ID 리스트로 음성통화 사용량을 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 음성통화 사용량 리스트
     */
    List<VoiceUsage> selectVoiceUsages(@Param("contractIds") List<String> contractIds);
}

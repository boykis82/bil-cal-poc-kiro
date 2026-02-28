package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import com.billing.charge.calculation.internal.model.SuspensionHistory;

/**
 * 월정액 데이터 MyBatis Mapper.
 * 상품 가입정보와 정지이력을 chunk 단위로 조회한다.
 * 기준정보 테이블과의 join 없이 마스터 테이블에서만 데이터를 조회한다.
 */
@Mapper
public interface MonthlyFeeMapper {

    /**
     * 계약ID 리스트로 상품 가입정보를 조회한다.
     * 월정액 계산에 필요한 핵심 필드만 조회하며, 일회성/사용량/할인 항목은 별도 로더에서 처리한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 상품 가입정보 리스트
     */
    List<SubscriptionInfo> selectSubscriptionInfos(@Param("contractIds") List<String> contractIds);

    /**
     * 계약ID 리스트로 정지이력을 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 정지이력 리스트
     */
    List<SuspensionHistory> selectSuspensionHistories(@Param("contractIds") List<String> contractIds);
}

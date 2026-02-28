package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.BillingInfo;
import com.billing.charge.calculation.internal.model.PaymentInfo;

/**
 * 청구/수납 데이터 MyBatis Mapper.
 * 연체가산금, 자동납부할인 계산에 필요한 청구정보와 수납정보를 chunk 단위로 조회한다.
 */
@Mapper
public interface BillingPaymentMapper {

    /**
     * 계약ID 리스트로 청구정보를 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 청구정보 리스트
     */
    List<BillingInfo> selectBillingInfos(@Param("contractIds") List<String> contractIds);

    /**
     * 계약ID 리스트로 수납정보를 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 수납정보 리스트
     */
    List<PaymentInfo> selectPaymentInfos(@Param("contractIds") List<String> contractIds);
}

package com.billing.charge.calculation.internal.mapper;

import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.internal.model.ChargeResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 요금 계산 결과 MyBatis Mapper.
 * 요금 계산 결과 저장 및 처리 상태 갱신을 담당한다.
 */
@Mapper
public interface ChargeResultMapper {

    /**
     * 요금 계산 결과를 DB에 저장한다.
     *
     * @param result 요금 계산 결과
     */
    void insertChargeResult(ChargeResult result);

    /**
     * 요금 항목 처리 상태를 DB에 갱신한다.
     *
     * @param chargeItemId 요금 항목 ID
     * @param status       처리 상태
     */
    void updateProcessingStatus(@Param("chargeItemId") String chargeItemId,
            @Param("status") ProcessingStatus status);
}

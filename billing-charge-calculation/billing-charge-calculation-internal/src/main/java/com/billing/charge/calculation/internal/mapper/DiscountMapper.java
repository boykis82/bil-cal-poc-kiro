package com.billing.charge.calculation.internal.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.billing.charge.calculation.internal.model.DiscountSubscription;

/**
 * 할인 데이터 MyBatis Mapper.
 * 할인 가입정보를 chunk 단위로 조회한다.
 * 할인 기준정보는 인메모리 캐시에서 별도로 조회하므로 여기서는 조회하지 않는다.
 */
@Mapper
public interface DiscountMapper {

    /**
     * 계약ID 리스트로 할인 가입정보를 조회한다.
     *
     * @param contractIds 계약ID 리스트
     * @return 할인 가입정보 리스트
     */
    List<DiscountSubscription> selectDiscountSubscriptions(@Param("contractIds") List<String> contractIds);
}

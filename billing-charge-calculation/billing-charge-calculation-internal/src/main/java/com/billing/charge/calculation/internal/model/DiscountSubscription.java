package com.billing.charge.calculation.internal.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 할인 가입정보.
 * 할인 계산에 필요한 할인 가입 관련 데이터를 담는다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountSubscription {

    private String contractId;
    private String discountId;
    private String discountCode;
    private String discountName;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 할인율 (백분율, 예: 10이면 10% 할인). 인메모리 캐시에서 조회하여 설정. */
    private java.math.BigDecimal discountRate;
}

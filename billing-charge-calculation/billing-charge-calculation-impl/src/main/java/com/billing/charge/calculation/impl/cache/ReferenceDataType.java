package com.billing.charge.calculation.impl.cache;

/**
 * 기준정보 유형.
 * 요금 계산에 필요한 다양한 기준정보의 종류를 정의한다.
 */
public enum ReferenceDataType {
    PRODUCT_FEE, // 상품 요금
    DISCOUNT_POLICY, // 할인 정책
    TAX_RULE, // 과세 기준
    SPLIT_BILLING_RULE, // 분리과금 기준
    AUTO_PAY_DISCOUNT, // 자동납부할인 기준
    SPECIAL_PRODUCT_INFO // 특이상품 추가 정보
}

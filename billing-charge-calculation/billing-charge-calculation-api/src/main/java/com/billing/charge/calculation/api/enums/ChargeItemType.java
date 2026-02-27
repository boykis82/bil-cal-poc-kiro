package com.billing.charge.calculation.api.enums;

/**
 * 요금 항목 유형.
 */
public enum ChargeItemType {
    MONTHLY_FEE, // 월정액
    ONE_TIME_FEE, // 일회성
    USAGE_FEE, // 통화료/종량료
    PERIOD_DISCOUNT, // 할인1 (기간 존재)
    FLAT_DISCOUNT, // 할인2 (기간 미존재)
    LATE_FEE, // 연체가산금
    AUTO_PAY_DISCOUNT, // 자동납부할인
    VAT, // 부가세
    PREPAID_OFFSET, // 선납반제
    SPLIT_BILLING // 분리과금
}

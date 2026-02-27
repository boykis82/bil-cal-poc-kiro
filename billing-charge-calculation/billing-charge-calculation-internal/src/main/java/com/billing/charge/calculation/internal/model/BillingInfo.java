package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 청구정보 모델.
 * 연체가산금, 자동납부할인 계산에 필요한 청구/수납 관련 정보를 담는다.
 */
public record BillingInfo(
                String billingId,
                String contractId,
                /** 이전 청구 금액 */
                BigDecimal previousBilledAmount,
                /** 미납 금액 */
                BigDecimal unpaidAmount,
                /** 납기일 */
                LocalDate dueDate,
                /** 현재일 */
                LocalDate currentDate,
                /** 자동납부 여부 */
                boolean autoPayEnabled,
                /** 자동납부 할인율 (백분율, 예: 1.0 = 1%) */
                BigDecimal autoPayDiscountRate) {

        /**
         * 하위 호환용 팩토리 메서드.
         */
        public static BillingInfo of(String billingId, String contractId) {
                return new BillingInfo(billingId, contractId, null, null, null, null, false, null);
        }
}

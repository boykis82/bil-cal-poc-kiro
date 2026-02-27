package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 수납정보 모델.
 * 납부 관련 정보를 담는다.
 */
public record PaymentInfo(
                String paymentId,
                String contractId,
                /** 납부 금액 */
                BigDecimal paidAmount,
                /** 납부일 */
                LocalDate paymentDate) {

        /**
         * 하위 호환용 팩토리 메서드.
         */
        public static PaymentInfo of(String paymentId, String contractId) {
                return new PaymentInfo(paymentId, contractId, null, null);
        }
}

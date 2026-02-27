package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 가입정보 모델.
 * 요금 계산에 필요한 상품 가입 관련 정보를 담는다.
 */
public record SubscriptionInfo(
                String subscriptionId,
                String productId,
                String subscriberId,
                /** 월정액 요금 */
                BigDecimal monthlyRate,
                /** 가입 시작일 */
                LocalDate startDate,
                /** 가입 종료일 */
                LocalDate endDate,
                /** 특이상품 여부 (Y/N) */
                String specialProductYn,
                /** 특이상품 추가 요율 (특이상품인 경우 월정액에 가산) */
                BigDecimal specialProductSurcharge,
                /** 일회성 요금 항목 목록 */
                List<OneTimeFeeItem> oneTimeFeeItems,
                /** 사용량 기반 요금 항목 목록 */
                List<UsageFeeItem> usageFeeItems,
                /** 할인 항목 목록 */
                List<DiscountItem> discountItems,
                /** 분리과금 비율 (백분율, 예: 50이면 50%) */
                BigDecimal splitRatio,
                /** 분리과금 대상 ID */
                String splitTargetId) {

        /**
         * 일회성 요금 항목.
         */
        public record OneTimeFeeItem(
                        String feeItemCode,
                        String feeItemName,
                        BigDecimal amount) {
        }

        /**
         * 사용량 기반 요금 항목 (통화료/종량료).
         */
        public record UsageFeeItem(
                        String feeItemCode,
                        String feeItemName,
                        BigDecimal unitPrice,
                        BigDecimal usageQuantity) {
        }

        /**
         * 할인 항목.
         * discountRate는 백분율 (예: 10이면 10% 할인).
         */
        public record DiscountItem(
                        String discountCode,
                        String discountName,
                        BigDecimal discountRate) {
        }
}

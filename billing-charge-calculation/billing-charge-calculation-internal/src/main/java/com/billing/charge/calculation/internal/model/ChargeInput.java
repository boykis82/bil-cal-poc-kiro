package com.billing.charge.calculation.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 요금 계산 입력 모델.
 * 다양한 원천(마스터 테이블, 접수 테이블, 기준정보)에서 조회한 데이터를 통합하는 추상화 객체.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeInput {
    private SubscriptionInfo subscriptionInfo;
    private List<SuspensionHistory> suspensionHistories;
    private BillingInfo billingInfo;
    private PaymentInfo paymentInfo;
    private List<PrepaidRecord> prepaidRecords;
}

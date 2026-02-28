package com.billing.charge.calculation.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 요금 계산 입력 모델.
 * 다양한 원천(마스터 테이블, 접수 테이블, 기준정보)에서 조회한 데이터를 통합하는 추상화 객체.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeInput {

    // === 기존 필드 (하위 호환) ===
    private SubscriptionInfo subscriptionInfo;
    private List<SuspensionHistory> suspensionHistories;
    private BillingInfo billingInfo;
    private PaymentInfo paymentInfo;
    private List<PrepaidRecord> prepaidRecords;

    // === 신규 필드 ===

    /** 할인 가입정보 */
    private List<DiscountSubscription> discountSubscriptions;

    /** 일회성 요금 도메인 데이터 (유형별 저장) */
    @Builder.Default
    private Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>> oneTimeChargeDataMap = new HashMap<>();

    /** 통화료/종량료 도메인 데이터 (유형별 저장) */
    @Builder.Default
    private Map<Class<? extends UsageChargeDomain>, List<? extends UsageChargeDomain>> usageChargeDataMap = new HashMap<>();

    // === 유틸리티 메서드 ===

    @SuppressWarnings("unchecked")
    public <T extends OneTimeChargeDomain> List<T> getOneTimeChargeData(Class<T> type) {
        List<? extends OneTimeChargeDomain> data = oneTimeChargeDataMap.get(type);
        return data != null ? (List<T>) data : List.of();
    }

    public <T extends OneTimeChargeDomain> void putOneTimeChargeData(Class<T> type, List<T> data) {
        oneTimeChargeDataMap.put(type, data);
    }

    @SuppressWarnings("unchecked")
    public <T extends UsageChargeDomain> List<T> getUsageChargeData(Class<T> type) {
        List<? extends UsageChargeDomain> data = usageChargeDataMap.get(type);
        return data != null ? (List<T>) data : List.of();
    }

    public <T extends UsageChargeDomain> void putUsageChargeData(Class<T> type, List<T> data) {
        usageChargeDataMap.put(type, data);
    }
}

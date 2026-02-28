package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 음성통화 사용량.
 * 통화료/종량료 도메인으로서 음성통화 사용량 데이터를 담는다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceUsage implements UsageChargeDomain {

    private String contractId;
    private String usageId;
    /** 통화시간(초) */
    private BigDecimal duration;
    /** 단가 */
    private BigDecimal unitPrice;
}

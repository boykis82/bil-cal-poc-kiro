package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 데이터 사용량.
 * 통화료/종량료 도메인으로서 데이터 사용량 정보를 담는다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataUsage implements UsageChargeDomain {

    private String contractId;
    private String usageId;
    /** 사용량(MB) */
    private BigDecimal dataVolume;
    /** 단가 */
    private BigDecimal unitPrice;
}

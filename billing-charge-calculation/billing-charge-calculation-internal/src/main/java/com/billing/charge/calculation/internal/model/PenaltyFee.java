package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 위약금.
 * 일회성 요금 도메인으로서 위약금 관련 데이터를 담는다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyFee implements OneTimeChargeDomain {

    private String contractId;
    private String penaltyId;
    private BigDecimal penaltyAmount;
    private String penaltyReason;
}

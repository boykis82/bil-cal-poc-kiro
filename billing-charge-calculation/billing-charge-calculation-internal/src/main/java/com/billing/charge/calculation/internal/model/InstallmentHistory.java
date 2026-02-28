package com.billing.charge.calculation.internal.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 할부이력.
 * 일회성 요금 도메인으로서 할부 관련 데이터를 담는다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentHistory implements OneTimeChargeDomain {

    private String contractId;
    private String installmentId;
    private BigDecimal installmentAmount;
    private int currentInstallment;
    private int totalInstallments;
}

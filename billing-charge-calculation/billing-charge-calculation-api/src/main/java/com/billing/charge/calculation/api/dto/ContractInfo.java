package com.billing.charge.calculation.api.dto;

import java.time.LocalDate;

/**
 * 계약정보 DTO.
 * 요금 계산 요청 시 전달되는 개별 계약 정보.
 */
public record ContractInfo(
        String contractId,
        String subscriberId,
        String productId,
        LocalDate billingStartDate,
        LocalDate billingEndDate) {
}

package com.billing.charge.calculation.api.dto;

import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 요금 계산 요청 DTO.
 * 단일 진입점 API에서 수신하는 요청 객체.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeCalculationRequest {
    private String tenantId;
    private UseCaseType useCaseType;
    private ProductType productType;
    private List<ContractInfo> contracts;
}

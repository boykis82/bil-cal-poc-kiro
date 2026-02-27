package com.billing.charge.calculation.internal.mapper.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * PIPELINE_CONFIG 테이블 조회 결과 DTO.
 */
@Getter
@Setter
public class PipelineConfigDto {

    private String pipelineConfigId;
    private String tenantId;
    private String productType;
    private String useCaseType;
    private String description;
}

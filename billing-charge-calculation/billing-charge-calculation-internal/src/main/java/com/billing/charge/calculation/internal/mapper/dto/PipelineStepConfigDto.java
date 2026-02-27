package com.billing.charge.calculation.internal.mapper.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * PIPELINE_STEP_CONFIG 테이블 조회 결과 DTO.
 */
@Getter
@Setter
public class PipelineStepConfigDto {

    private String pipelineConfigId;
    private String stepId;
    private int stepOrder;
}

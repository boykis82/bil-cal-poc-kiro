package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.internal.step.ChargeItemStep;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

/**
 * 요금 계산 Pipeline 모델.
 * 테넌트ID, 상품유형, 유스케이스 조합에 따라 구성된 Step 목록을 보유한다.
 * Step은 order 순서대로 정렬되어 실행된다.
 */
@Getter
public class Pipeline {

    private final String pipelineId;
    private final List<ChargeItemStep> steps;

    public Pipeline(String pipelineId, List<ChargeItemStep> steps) {
        this.pipelineId = pipelineId;
        this.steps = steps.stream()
                .sorted(Comparator.comparingInt(ChargeItemStep::getOrder))
                .toList();
    }
}

package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.internal.context.ChargeContext;

/**
 * Pipeline 내 개별 요금 항목 계산 Step.
 * 각 요금 항목(월정액, 일회성, 할인 등)은 이 인터페이스를 구현한다.
 * 새로운 요금 항목 추가 시 이 인터페이스를 구현하는 것만으로 Pipeline에 편입 가능하다.
 */
public interface ChargeItemStep {

    /**
     * 이 Step의 고유 식별자.
     */
    String getStepId();

    /**
     * 이 Step의 실행 순서 (Pipeline 내 정렬 기준).
     */
    int getOrder();

    /**
     * 요금 계산을 수행한다.
     * ChargeContext에서 입력을 읽고, 계산 결과를 ChargeContext에 누적한다.
     *
     * @param context 요금 계산 컨텍스트 (입력 데이터 + 이전 Step 결과)
     */
    void process(ChargeContext context);

    /**
     * 이 Step 완료 후 처리 상태를 DB에 기록해야 하는지 여부.
     */
    boolean requiresStatusUpdate();
}

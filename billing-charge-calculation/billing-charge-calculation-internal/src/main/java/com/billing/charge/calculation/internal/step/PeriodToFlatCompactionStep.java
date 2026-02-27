package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.internal.context.ChargeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Period → Flat 압축 Step.
 * 할인1 완료 후 기간 존재 결과(원금 + 할인1)를 기간 정보 제거 + group by sum 압축하여
 * FlatChargeResult로 변환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodToFlatCompactionStep implements ChargeItemStep {

    private static final String STEP_ID = "PERIOD_TO_FLAT_COMPACTION";
    private static final int ORDER = 500;

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void process(ChargeContext context) {
        int periodCount = context.getPeriodResults().size();
        log.debug("Period → Flat 압축 시작: periodResults 건수={}", periodCount);

        context.compactPeriodToFlat();

        log.debug("Period → Flat 압축 완료: flatResults 건수={}", context.getFlatResults().size());
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

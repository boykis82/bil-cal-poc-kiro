package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.PrepaidRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 선납반제 계산 Step.
 * 선납된 금액을 당월 요금에서 차감 처리한다.
 * 선납내역의 합계를 음수 FlatChargeResult로 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrepaidOffsetStep implements ChargeItemStep {

    private static final String STEP_ID = "PREPAID_OFFSET";
    private static final int ORDER = 1000;

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
        List<PrepaidRecord> prepaidRecords = context.getChargeInput().getPrepaidRecords();
        if (prepaidRecords == null || prepaidRecords.isEmpty()) {
            log.debug("선납반제 계산 생략: 선납내역 없음");
            return;
        }

        BigDecimal totalPrepaid = prepaidRecords.stream()
                .filter(r -> r.amount() != null)
                .map(PrepaidRecord::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPrepaid.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("선납반제 계산 생략: 선납 합계 0 이하");
            return;
        }

        FlatChargeResult result = new FlatChargeResult(
                STEP_ID,
                "선납반제",
                ChargeItemType.PREPAID_OFFSET,
                totalPrepaid.negate(),
                "KRW",
                Map.of("totalPrepaid", totalPrepaid));

        context.addFlatResult(result);
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

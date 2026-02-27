package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.BillingInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 연체가산금 계산 Step.
 * 청구/수납정보와 이전 계산 결과를 기반으로 연체가산금을 계산한다.
 * 연체가산금 = 미납금액 × 연이율(12%) × 연체일수 / 365
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LateFeeStep implements ChargeItemStep {

    private static final String STEP_ID = "LATE_FEE";
    private static final int ORDER = 700;
    /** 연체 연이율 12% */
    private static final BigDecimal LATE_FEE_ANNUAL_RATE = new BigDecimal("0.12");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

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
        BillingInfo billing = context.getChargeInput().getBillingInfo();
        if (billing == null) {
            log.debug("연체가산금 계산 생략: 청구정보 없음");
            return;
        }

        if (billing.unpaidAmount() == null || billing.unpaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("연체가산금 계산 생략: 미납 금액 없음");
            return;
        }

        if (billing.dueDate() == null || billing.currentDate() == null) {
            log.debug("연체가산금 계산 생략: 납기일 또는 현재일 없음");
            return;
        }

        long overdueDays = ChronoUnit.DAYS.between(billing.dueDate(), billing.currentDate());
        if (overdueDays <= 0) {
            log.debug("연체가산금 계산 생략: 연체일수 없음 (overdueDays={})", overdueDays);
            return;
        }

        BigDecimal lateFee = calculateLateFee(billing.unpaidAmount(), overdueDays);

        FlatChargeResult result = new FlatChargeResult(
                STEP_ID,
                "연체가산금",
                ChargeItemType.LATE_FEE,
                lateFee,
                "KRW",
                Map.of("overdueDays", overdueDays));

        context.addFlatResult(result);
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }

    /**
     * 연체가산금 계산: 미납금액 × 연이율 × 연체일수 / 365
     */
    static BigDecimal calculateLateFee(BigDecimal unpaidAmount, long overdueDays) {
        return unpaidAmount
                .multiply(LATE_FEE_ANNUAL_RATE)
                .multiply(BigDecimal.valueOf(overdueDays))
                .divide(DAYS_IN_YEAR, 0, RoundingMode.HALF_UP);
    }
}

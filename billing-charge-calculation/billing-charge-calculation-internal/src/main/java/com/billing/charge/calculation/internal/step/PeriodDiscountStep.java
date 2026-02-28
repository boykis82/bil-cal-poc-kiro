package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.DiscountSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 할인1 Step (기간 존재 할인).
 * ChargeContext에서 기간 존재 원금성 항목(MONTHLY_FEE)을 조회하고,
 * ChargeInput의 discountSubscriptions를 기반으로 할인을 계산한다.
 * 할인 결과는 PeriodChargeResult(PERIOD_DISCOUNT, 음수 금액)로 출력한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodDiscountStep implements ChargeItemStep {

    private static final String STEP_ID = "PERIOD_DISCOUNT";
    private static final int ORDER = 400;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

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
        List<DiscountSubscription> discountSubscriptions = context.getChargeInput().getDiscountSubscriptions();
        if (discountSubscriptions == null || discountSubscriptions.isEmpty()) {
            log.debug("기간 할인 계산 생략: 할인 가입정보 없음");
            return;
        }

        // 원금성 기간 존재 항목 (MONTHLY_FEE) 조회
        List<PeriodChargeResult> principalResults = context.getPeriodResultsByType(ChargeItemType.MONTHLY_FEE);

        if (principalResults.isEmpty()) {
            log.debug("기간 할인 계산 생략: 대상 원금성 항목 없음");
            return;
        }

        for (DiscountSubscription discount : discountSubscriptions) {
            if (discount.getDiscountRate() == null || discount.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("할인율 없음 또는 0 이하, 건너뜀: discountCode={}", discount.getDiscountCode());
                continue;
            }

            for (PeriodChargeResult principal : principalResults) {
                BigDecimal discountAmount = calculateDiscountAmount(principal.amount(), discount.getDiscountRate());

                PeriodChargeResult discountResult = new PeriodChargeResult(
                        discount.getDiscountCode(),
                        discount.getDiscountName(),
                        ChargeItemType.PERIOD_DISCOUNT,
                        discountAmount,
                        principal.periodFrom(),
                        principal.periodTo(),
                        "KRW",
                        Map.of());

                context.addPeriodResult(discountResult);
            }
        }
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }

    /**
     * 할인 금액 계산.
     * 할인 금액 = -(원금 × 할인율 / 100), 반올림 적용.
     * 할인 금액의 절대값은 원금을 초과하지 않는다.
     */
    static BigDecimal calculateDiscountAmount(BigDecimal principalAmount, BigDecimal discountRate) {
        // 할인율을 0~100 범위로 제한
        BigDecimal effectiveRate = discountRate.min(HUNDRED).max(BigDecimal.ZERO);

        BigDecimal rawDiscount = principalAmount
                .multiply(effectiveRate)
                .divide(HUNDRED, 0, RoundingMode.HALF_UP);

        // 할인 금액 상한: 원금을 초과하지 않도록 보장
        if (rawDiscount.abs().compareTo(principalAmount.abs()) > 0) {
            rawDiscount = principalAmount;
        }

        return rawDiscount.negate();
    }
}

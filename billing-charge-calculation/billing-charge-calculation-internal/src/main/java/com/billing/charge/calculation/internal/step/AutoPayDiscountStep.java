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
import java.util.Map;
import java.util.Set;

/**
 * 자동납부할인 계산 Step.
 * 자동납부 가입자에 대해 기존 요금 합계에서 할인율을 적용하여 할인 금액을 계산한다.
 * 할인 대상에서 AUTO_PAY_DISCOUNT, VAT, PREPAID_OFFSET, SPLIT_BILLING은 제외한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPayDiscountStep implements ChargeItemStep {

    private static final String STEP_ID = "AUTO_PAY_DISCOUNT";
    private static final int ORDER = 800;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 할인 대상에서 제외할 요금 항목 유형 */
    private static final Set<ChargeItemType> EXCLUDED_TYPES = Set.of(
            ChargeItemType.AUTO_PAY_DISCOUNT,
            ChargeItemType.VAT,
            ChargeItemType.PREPAID_OFFSET,
            ChargeItemType.SPLIT_BILLING);

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
        if (billing == null || !billing.autoPayEnabled()) {
            log.debug("자동납부할인 계산 생략: 청구정보 없음 또는 자동납부 미가입");
            return;
        }

        if (billing.autoPayDiscountRate() == null
                || billing.autoPayDiscountRate().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("자동납부할인 계산 생략: 할인율 없음");
            return;
        }

        // 할인 대상 요금 합계 계산
        BigDecimal totalCharges = context.getFlatResults().stream()
                .filter(r -> !EXCLUDED_TYPES.contains(r.chargeItemType()))
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCharges.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("자동납부할인 계산 생략: 할인 대상 요금 합계 0 이하");
            return;
        }

        BigDecimal discountAmount = totalCharges
                .multiply(billing.autoPayDiscountRate())
                .divide(HUNDRED, 0, RoundingMode.HALF_UP)
                .negate();

        FlatChargeResult result = new FlatChargeResult(
                STEP_ID,
                "자동납부할인",
                ChargeItemType.AUTO_PAY_DISCOUNT,
                discountAmount,
                "KRW",
                Map.of("discountRate", billing.autoPayDiscountRate()));

        context.addFlatResult(result);
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

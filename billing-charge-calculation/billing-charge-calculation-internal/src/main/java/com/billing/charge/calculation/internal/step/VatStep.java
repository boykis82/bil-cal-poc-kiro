package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

/**
 * 부가세 계산 Step.
 * 이전 계산 결과에서 과세 대상 요금을 합산하고 한국 표준 부가세율(10%)을 적용한다.
 * VAT, PREPAID_OFFSET, SPLIT_BILLING은 과세 대상에서 제외한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VatStep implements ChargeItemStep {

    private static final String STEP_ID = "VAT";
    private static final int ORDER = 900;
    /** 한국 표준 부가세율 10% */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");

    /** 과세 대상에서 제외할 요금 항목 유형 */
    private static final Set<ChargeItemType> EXCLUDED_TYPES = Set.of(
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
        // 과세 대상 요금 합계 계산
        BigDecimal taxableAmount = context.getFlatResults().stream()
                .filter(r -> !EXCLUDED_TYPES.contains(r.chargeItemType()))
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (taxableAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("부가세 계산 생략: 과세 대상 금액 없음");
            return;
        }

        BigDecimal vatAmount = taxableAmount
                .multiply(VAT_RATE)
                .setScale(0, RoundingMode.HALF_UP);

        FlatChargeResult result = new FlatChargeResult(
                STEP_ID,
                "부가세",
                ChargeItemType.VAT,
                vatAmount,
                "KRW",
                Map.of("taxableAmount", taxableAmount, "vatRate", VAT_RATE));

        context.addFlatResult(result);
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.DiscountSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 할인2 Step (기간 미존재 할인).
 * ChargeContext에서 FlatChargeResult(원금성 항목: ONE_TIME_FEE, USAGE_FEE, MONTHLY_FEE
 * 압축분)를 조회하고,
 * ChargeInput의 discountSubscriptions를 기반으로 할인을 계산한다.
 * 할인 결과는 FlatChargeResult(FLAT_DISCOUNT, 음수 금액)로 출력한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlatDiscountStep implements ChargeItemStep {

    private static final String STEP_ID = "FLAT_DISCOUNT";
    private static final int ORDER = 600;
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
            log.debug("Flat 할인 계산 생략: 할인 가입정보 없음");
            return;
        }

        // 원금성 Flat 항목 조회 (ONE_TIME_FEE, USAGE_FEE, 압축된 MONTHLY_FEE 등)
        // FLAT_DISCOUNT 타입은 제외하여 이미 적용된 할인에 중복 할인하지 않음
        List<FlatChargeResult> principalResults = collectPrincipalFlatResults(context);

        if (principalResults.isEmpty()) {
            log.debug("Flat 할인 계산 생략: 대상 원금성 항목 없음");
            return;
        }

        for (DiscountSubscription discount : discountSubscriptions) {
            if (discount.getDiscountRate() == null || discount.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("할인율 없음 또는 0 이하, 건너뜀: discountCode={}", discount.getDiscountCode());
                continue;
            }

            for (FlatChargeResult principal : principalResults) {
                BigDecimal discountAmount = calculateDiscountAmount(principal.amount(), discount.getDiscountRate());

                FlatChargeResult discountResult = new FlatChargeResult(
                        discount.getDiscountCode(),
                        discount.getDiscountName(),
                        ChargeItemType.FLAT_DISCOUNT,
                        discountAmount,
                        "KRW",
                        Map.of());

                context.addFlatResult(discountResult);
            }
        }
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }

    /**
     * 원금성 Flat 결과를 수집한다.
     * 할인 대상: ONE_TIME_FEE, USAGE_FEE, 그리고 chargeItemType이 null인 항목(압축된 MONTHLY_FEE 등).
     * 이미 적용된 할인(FLAT_DISCOUNT, PERIOD_DISCOUNT 등)은 제외한다.
     */
    private List<FlatChargeResult> collectPrincipalFlatResults(ChargeContext context) {
        List<FlatChargeResult> result = new ArrayList<>();
        for (FlatChargeResult flat : context.getFlatResults()) {
            ChargeItemType type = flat.chargeItemType();
            if (type == null
                    || type == ChargeItemType.ONE_TIME_FEE
                    || type == ChargeItemType.USAGE_FEE
                    || type == ChargeItemType.MONTHLY_FEE) {
                result.add(flat);
            }
        }
        return result;
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

package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 분리과금 처리 Step.
 * 분리과금 기준정보(splitRatio, splitTargetId)에 따라 요금을 분리하여 과금한다.
 * 원래 대상에서 차감하는 음수 결과와 분리 대상에 부과하는 양수 결과를 쌍으로 생성하여
 * 총액을 보존한다 (Property 14).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SplitBillingStep implements ChargeItemStep {

    private static final String STEP_ID = "SPLIT_BILLING";
    private static final int ORDER = 1100;
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
        SubscriptionInfo subscription = context.getChargeInput().getSubscriptionInfo();
        if (subscription == null) {
            log.debug("분리과금 처리 생략: 가입정보 없음");
            return;
        }

        BigDecimal splitRatio = subscription.splitRatio();
        String splitTargetId = subscription.splitTargetId();

        if (splitRatio == null || splitRatio.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("분리과금 처리 생략: 분리과금 비율 없음 또는 0 이하");
            return;
        }

        if (splitTargetId == null || splitTargetId.isBlank()) {
            log.debug("분리과금 처리 생략: 분리과금 대상 ID 없음");
            return;
        }

        // 기존 전체 요금 합계 (SPLIT_BILLING 제외)
        BigDecimal totalCharges = context.getFlatResults().stream()
                .filter(r -> r.chargeItemType() != ChargeItemType.SPLIT_BILLING)
                .map(FlatChargeResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCharges.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("분리과금 처리 생략: 대상 요금 합계 0");
            return;
        }

        // 분리 금액 = 총액 × splitRatio / 100
        BigDecimal splitAmount = totalCharges
                .multiply(splitRatio)
                .divide(HUNDRED, 0, RoundingMode.HALF_UP);

        // 원래 대상에서 차감 (음수)
        FlatChargeResult deduction = new FlatChargeResult(
                STEP_ID + "_DEDUCT",
                "분리과금_차감",
                ChargeItemType.SPLIT_BILLING,
                splitAmount.negate(),
                "KRW",
                Map.of("splitTargetId", splitTargetId, "splitRatio", splitRatio));

        // 분리 대상에 부과 (양수)
        FlatChargeResult assignment = new FlatChargeResult(
                STEP_ID + "_ASSIGN",
                "분리과금_부과",
                ChargeItemType.SPLIT_BILLING,
                splitAmount,
                "KRW",
                Map.of("splitTargetId", splitTargetId, "splitRatio", splitRatio));

        context.addFlatResult(deduction);
        context.addFlatResult(assignment);
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

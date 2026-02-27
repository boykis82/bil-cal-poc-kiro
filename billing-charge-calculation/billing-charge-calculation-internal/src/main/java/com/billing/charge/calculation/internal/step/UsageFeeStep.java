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
import java.util.List;
import java.util.Map;

/**
 * 통화료/종량료 계산 Step.
 * 가입정보와 기준정보를 기반으로 사용량 기반 요금을 계산한다.
 * 단가 × 사용량으로 FlatChargeResult를 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageFeeStep implements ChargeItemStep {

    private static final String STEP_ID = "USAGE_FEE";
    private static final int ORDER = 300;

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
            log.debug("통화료/종량료 계산 생략: 가입정보 없음");
            return;
        }

        List<SubscriptionInfo.UsageFeeItem> usageItems = subscription.usageFeeItems();
        if (usageItems == null || usageItems.isEmpty()) {
            log.debug("통화료/종량료 계산 생략: 사용량 요금 항목 없음");
            return;
        }

        for (SubscriptionInfo.UsageFeeItem item : usageItems) {
            if (item.unitPrice() == null || item.usageQuantity() == null) {
                log.debug("통화료/종량료 항목 단가 또는 사용량 없음, 건너뜀: feeItemCode={}", item.feeItemCode());
                continue;
            }

            BigDecimal amount = item.unitPrice()
                    .multiply(item.usageQuantity())
                    .setScale(0, RoundingMode.HALF_UP);

            FlatChargeResult result = new FlatChargeResult(
                    item.feeItemCode(),
                    item.feeItemName(),
                    ChargeItemType.USAGE_FEE,
                    amount,
                    "KRW",
                    Map.of());

            context.addFlatResult(result);
        }
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

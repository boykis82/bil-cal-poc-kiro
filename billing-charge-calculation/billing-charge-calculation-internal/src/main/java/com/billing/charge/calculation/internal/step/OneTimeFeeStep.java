package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 일회성 요금 계산 Step.
 * 다양한 종류의 일회성 요금을 하나의 추상화된 인터페이스로 처리한다.
 * 가입정보와 기준정보를 입력으로 받아 FlatChargeResult를 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneTimeFeeStep implements ChargeItemStep {

    private static final String STEP_ID = "ONE_TIME_FEE";
    private static final int ORDER = 200;

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
            log.debug("일회성 요금 계산 생략: 가입정보 없음");
            return;
        }

        List<SubscriptionInfo.OneTimeFeeItem> feeItems = subscription.oneTimeFeeItems();
        if (feeItems == null || feeItems.isEmpty()) {
            log.debug("일회성 요금 계산 생략: 일회성 요금 항목 없음");
            return;
        }

        for (SubscriptionInfo.OneTimeFeeItem item : feeItems) {
            if (item.amount() == null) {
                log.debug("일회성 요금 항목 금액 없음, 건너뜀: feeItemCode={}", item.feeItemCode());
                continue;
            }

            FlatChargeResult result = new FlatChargeResult(
                    item.feeItemCode(),
                    item.feeItemName(),
                    ChargeItemType.ONE_TIME_FEE,
                    item.amount(),
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

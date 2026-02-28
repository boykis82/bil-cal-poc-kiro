package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.InstallmentHistory;
import com.billing.charge.calculation.internal.model.OneTimeChargeDomain;
import com.billing.charge.calculation.internal.model.PenaltyFee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 일회성 요금 계산 Step.
 * ChargeInput의 oneTimeChargeDataMap에서 등록된 OneTimeChargeDomain 유형별 데이터를 읽어
 * 각 유형에 대한 FlatChargeResult를 생성한다.
 * 데이터가 없으면 안전하게 생략한다 (예외 없이 return).
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
        Map<Class<? extends OneTimeChargeDomain>, List<? extends OneTimeChargeDomain>> dataMap =
                context.getChargeInput().getOneTimeChargeDataMap();

        if (dataMap == null || dataMap.isEmpty()) {
            log.debug("일회성 요금 계산 생략: 일회성 요금 데이터 없음");
            return;
        }

        for (var entry : dataMap.entrySet()) {
            processOneTimeChargeType(entry.getKey(), entry.getValue(), context);
        }
    }

    private void processOneTimeChargeType(
            Class<? extends OneTimeChargeDomain> type,
            List<? extends OneTimeChargeDomain> items,
            ChargeContext context) {
        for (OneTimeChargeDomain item : items) {
            BigDecimal amount = resolveAmount(item);
            if (amount == null) {
                log.debug("일회성 요금 항목 금액 없음, 건너뜀: type={}", type.getSimpleName());
                continue;
            }

            FlatChargeResult result = new FlatChargeResult(
                    type.getSimpleName(),
                    type.getSimpleName(),
                    ChargeItemType.ONE_TIME_FEE,
                    amount,
                    "KRW",
                    Map.of());

            context.addFlatResult(result);
        }
    }

    private BigDecimal resolveAmount(OneTimeChargeDomain item) {
        if (item instanceof InstallmentHistory ih) {
            return ih.getInstallmentAmount();
        } else if (item instanceof PenaltyFee pf) {
            return pf.getPenaltyAmount();
        }
        return null;
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}


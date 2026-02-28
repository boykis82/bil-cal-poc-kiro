package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.DataUsage;
import com.billing.charge.calculation.internal.model.UsageChargeDomain;
import com.billing.charge.calculation.internal.model.VoiceUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 통화료/종량료 계산 Step.
 * ChargeInput의 usageChargeDataMap에서 등록된 UsageChargeDomain 유형별 데이터를 읽어
 * 각 유형에 대한 FlatChargeResult를 생성한다.
 * 데이터가 없으면 안전하게 생략한다 (예외 없이 return).
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
        Map<Class<? extends UsageChargeDomain>, List<? extends UsageChargeDomain>> dataMap =
                context.getChargeInput().getUsageChargeDataMap();

        if (dataMap == null || dataMap.isEmpty()) {
            log.debug("통화료/종량료 계산 생략: 사용량 데이터 없음");
            return;
        }

        for (var entry : dataMap.entrySet()) {
            processUsageChargeType(entry.getKey(), entry.getValue(), context);
        }
    }

    private void processUsageChargeType(
            Class<? extends UsageChargeDomain> type,
            List<? extends UsageChargeDomain> items,
            ChargeContext context) {
        for (UsageChargeDomain item : items) {
            BigDecimal amount = resolveAmount(item);
            if (amount == null) {
                log.debug("통화료/종량료 항목 금액 산출 불가, 건너뜀: type={}", type.getSimpleName());
                continue;
            }

            FlatChargeResult result = new FlatChargeResult(
                    type.getSimpleName(),
                    type.getSimpleName(),
                    ChargeItemType.USAGE_FEE,
                    amount,
                    "KRW",
                    Map.of());

            context.addFlatResult(result);
        }
    }

    private BigDecimal resolveAmount(UsageChargeDomain item) {
        if (item instanceof VoiceUsage vu) {
            if (vu.getDuration() != null && vu.getUnitPrice() != null) {
                return vu.getUnitPrice().multiply(vu.getDuration()).setScale(0, RoundingMode.HALF_UP);
            }
        } else if (item instanceof DataUsage du) {
            if (du.getDataVolume() != null && du.getUnitPrice() != null) {
                return du.getUnitPrice().multiply(du.getDataVolume()).setScale(0, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    @Override
    public boolean requiresStatusUpdate() {
        return false;
    }
}

package com.billing.charge.calculation.internal.step;

import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.IntersectedPeriod;
import com.billing.charge.calculation.internal.model.PeriodHistory;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import com.billing.charge.calculation.internal.model.SuspensionHistory;
import com.billing.charge.calculation.internal.util.PeriodIntersectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 월정액 요금 계산 Step.
 * 상품 가입정보, 정지이력, 상품 요금 기준정보를 기반으로
 * 선분이력 교차 처리 후 구간별 일할 계산을 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyFeeStep implements ChargeItemStep {

    private static final String STEP_ID = "MONTHLY_FEE";
    private static final int ORDER = 100;
    private static final String HISTORY_TYPE_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String HISTORY_TYPE_SUSPENSION = "SUSPENSION";

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
        if (subscription == null || subscription.monthlyRate() == null) {
            log.debug("월정액 계산 생략: 가입정보 또는 월정액 요금 없음");
            return;
        }

        BigDecimal monthlyRate = resolveMonthlyRate(subscription);

        // 선분이력 구성
        List<List<PeriodHistory>> periodHistories = buildPeriodHistories(context);

        if (periodHistories.isEmpty()) {
            log.debug("월정액 계산 생략: 유효한 선분이력 없음");
            return;
        }

        // 선분이력 교차 처리
        List<IntersectedPeriod> intersectedPeriods = PeriodIntersectionUtil.intersect(periodHistories);

        // 구간별 일할 계산
        for (IntersectedPeriod period : intersectedPeriods) {
            // 정지 구간은 요금 계산에서 제외
            if (period.getActiveHistories().containsKey(HISTORY_TYPE_SUSPENSION)) {
                log.debug("정지 구간 제외: {} ~ {}", period.getFrom(), period.getTo());
                continue;
            }

            BigDecimal proRatedAmount = calculateProRatedAmount(monthlyRate, period.getFrom(), period.getTo());

            PeriodChargeResult result = new PeriodChargeResult(
                    subscription.productId(),
                    "월정액_" + subscription.productId(),
                    ChargeItemType.MONTHLY_FEE,
                    proRatedAmount,
                    period.getFrom(),
                    period.getTo(),
                    "KRW",
                    Map.of());

            context.addPeriodResult(result);
        }
    }

    @Override
    public boolean requiresStatusUpdate() {
        return true;
    }

    /**
     * 월정액 요율 결정. 특이상품인 경우 추가 요율을 가산한다.
     */
    private BigDecimal resolveMonthlyRate(SubscriptionInfo subscription) {
        BigDecimal rate = subscription.monthlyRate();
        if ("Y".equals(subscription.specialProductYn()) && subscription.specialProductSurcharge() != null) {
            rate = rate.add(subscription.specialProductSurcharge());
            log.debug("특이상품 추가 요율 적용: productId={}, surcharge={}",
                    subscription.productId(), subscription.specialProductSurcharge());
        }
        return rate;
    }

    /**
     * 선분이력 리스트를 구성한다.
     * 가입이력과 정지이력을 PeriodHistory 형태로 변환한다.
     */
    private List<List<PeriodHistory>> buildPeriodHistories(ChargeContext context) {
        SubscriptionInfo subscription = context.getChargeInput().getSubscriptionInfo();
        List<SuspensionHistory> suspensions = context.getChargeInput().getSuspensionHistories();

        List<List<PeriodHistory>> result = new ArrayList<>();

        // 가입이력 (필수)
        if (subscription.startDate() != null && subscription.endDate() != null) {
            PeriodHistory subscriptionHistory = PeriodHistory.builder()
                    .startDate(subscription.startDate())
                    .endDate(subscription.endDate())
                    .historyType(HISTORY_TYPE_SUBSCRIPTION)
                    .build();
            result.add(List.of(subscriptionHistory));
        }

        // 정지이력 (선택)
        if (suspensions != null && !suspensions.isEmpty()) {
            List<PeriodHistory> suspensionHistories = suspensions.stream()
                    .map(s -> PeriodHistory.builder()
                            .startDate(s.startDate())
                            .endDate(s.endDate())
                            .historyType(HISTORY_TYPE_SUSPENSION)
                            .build())
                    .toList();
            result.add(suspensionHistories);
        }

        return result;
    }

    /**
     * 일할 계산: amount = monthlyRate * daysInPeriod / totalDaysInMonth
     * BigDecimal HALF_UP 반올림 적용.
     */
    static BigDecimal calculateProRatedAmount(BigDecimal monthlyRate, LocalDate from, LocalDate to) {
        long daysInPeriod = ChronoUnit.DAYS.between(from, to) + 1;
        YearMonth yearMonth = YearMonth.from(from);
        int totalDaysInMonth = yearMonth.lengthOfMonth();

        return monthlyRate
                .multiply(BigDecimal.valueOf(daysInPeriod))
                .divide(BigDecimal.valueOf(totalDaysInMonth), 0, RoundingMode.HALF_UP);
    }
}

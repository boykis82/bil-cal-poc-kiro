package com.billing.charge.calculation.internal.util;

import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.api.model.PeriodChargeResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 기간 존재 결과를 기간 미존재 결과로 압축한다.
 * 할인1 Step 완료 후 실행되는 PeriodToFlatCompactionStep에서 사용.
 */
public class PeriodToFlatCompactor {

    private PeriodToFlatCompactor() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * Period_Charge_Result 목록을 항목별로 group by sum하여
     * Flat_Charge_Result 목록으로 변환한다.
     *
     * @param periodResults 기간 존재 요금 계산 결과 목록
     * @return 항목별 합산된 기간 미존재 결과 목록
     */
    public static List<FlatChargeResult> compact(List<PeriodChargeResult> periodResults) {
        return periodResults.stream()
                .collect(Collectors.groupingBy(
                        PeriodChargeResult::chargeItemCode,
                        Collectors.reducing(BigDecimal.ZERO, PeriodChargeResult::amount, BigDecimal::add)))
                .entrySet().stream()
                .map(entry -> FlatChargeResult.of(entry.getKey(), entry.getValue()))
                .toList();
    }
}

package com.billing.charge.calculation.internal.util;

import com.billing.charge.calculation.internal.model.IntersectedPeriod;
import com.billing.charge.calculation.internal.model.PeriodHistory;

import java.time.LocalDate;
import java.util.*;

/**
 * 선분이력 교차(intersection) 처리 유틸리티.
 * 복수의 선분이력(가입이력, 정지이력 등)을 교차하여 겹치지 않는 구간으로 분리한다.
 */
public class PeriodIntersectionUtil {

    private PeriodIntersectionUtil() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    /**
     * 여러 선분이력 리스트를 교차 처리하여 최종 구간 목록을 생성한다.
     *
     * 알고리즘:
     * 1. 모든 선분이력의 시작/종료 시점을 수집하여 정렬
     * 2. 인접한 시점 쌍으로 구간을 생성
     * 3. 각 구간에 대해 해당 시점에 유효한 이력 정보를 매핑
     *
     * @param periodHistories 선분이력 리스트들 (가입이력, 정지이력, 요금이력 등)
     * @return 교차 처리된 구간 목록
     */
    public static List<IntersectedPeriod> intersect(List<List<PeriodHistory>> periodHistories) {
        if (periodHistories == null || periodHistories.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 모든 경계 시점 수집 및 정렬
        TreeSet<LocalDate> boundaries = new TreeSet<>();
        for (List<PeriodHistory> histories : periodHistories) {
            for (PeriodHistory h : histories) {
                boundaries.add(h.getStartDate());
                boundaries.add(h.getEndDate().plusDays(1));
            }
        }

        if (boundaries.size() < 2) {
            return Collections.emptyList();
        }

        // 2. 인접 경계 시점 쌍으로 구간 생성
        List<IntersectedPeriod> result = new ArrayList<>();
        LocalDate[] dates = boundaries.toArray(new LocalDate[0]);
        for (int i = 0; i < dates.length - 1; i++) {
            LocalDate from = dates[i];
            LocalDate to = dates[i + 1].minusDays(1);

            // 3. 각 구간에 유효한 이력 매핑
            Map<String, PeriodHistory> activeHistories = new HashMap<>();
            for (List<PeriodHistory> histories : periodHistories) {
                for (PeriodHistory h : histories) {
                    if (!h.getEndDate().isBefore(from) && !h.getStartDate().isAfter(to)) {
                        activeHistories.put(h.getHistoryType(), h);
                    }
                }
            }

            if (!activeHistories.isEmpty()) {
                result.add(new IntersectedPeriod(from, to, activeHistories));
            }
        }

        return result;
    }
}

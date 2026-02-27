package com.billing.charge.calculation.internal.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

/**
 * 선분이력 교차 처리 결과 모델.
 * 교차 처리를 통해 생성된 겹치지 않는 구간과 해당 구간에 유효한 이력 정보를 담는다.
 */
@Getter
@AllArgsConstructor
public class IntersectedPeriod {
    /** 구간 시작일 */
    private final LocalDate from;
    /** 구간 종료일 */
    private final LocalDate to;
    /** 해당 구간에 유효한 이력 맵 (historyType → PeriodHistory) */
    private final Map<String, PeriodHistory> activeHistories;
}

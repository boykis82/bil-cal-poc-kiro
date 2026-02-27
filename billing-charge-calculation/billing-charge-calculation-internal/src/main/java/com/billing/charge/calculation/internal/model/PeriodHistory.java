package com.billing.charge.calculation.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 선분이력 모델.
 * 시간 구간별로 분리된 이력 데이터를 표현한다.
 * 가입이력, 정지이력, 요금이력 등 다양한 유형의 선분이력에 사용된다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodHistory {
    /** 이력 시작일 */
    private LocalDate startDate;
    /** 이력 종료일 */
    private LocalDate endDate;
    /** 이력 유형 (가입이력, 정지이력, 요금이력 등) */
    private String historyType;
}

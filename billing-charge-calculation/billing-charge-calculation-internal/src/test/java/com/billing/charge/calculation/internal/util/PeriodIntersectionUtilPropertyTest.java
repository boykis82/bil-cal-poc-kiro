package com.billing.charge.calculation.internal.util;

import com.billing.charge.calculation.internal.model.IntersectedPeriod;
import com.billing.charge.calculation.internal.model.PeriodHistory;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: billing-charge-calculation, Property 9: 선분이력 교차 처리 결과 비중첩성
 *
 * 임의의 선분이력 집합에 대해, PeriodIntersectionUtil.intersect()의 결과로 생성된
 * 구간들은 서로 겹치지 않아야 한다.
 * 즉, 인접한 두 구간 A, B에 대해 A.to < B.from이어야 한다.
 *
 * Validates: Requirements 5.3
 */
@Tag("Feature: billing-charge-calculation, Property 9: 선분이력 교차 비중첩성")
class PeriodIntersectionUtilPropertyTest {

        @Property(tries = 100)
        void intersectedPeriodsShouldNotOverlap(
                        @ForAll("periodHistoryLists") List<List<PeriodHistory>> histories) {

                List<IntersectedPeriod> result = PeriodIntersectionUtil.intersect(histories);

                for (int i = 0; i < result.size() - 1; i++) {
                        IntersectedPeriod current = result.get(i);
                        IntersectedPeriod next = result.get(i + 1);
                        assertThat(current.getTo()).isBefore(next.getFrom());
                }
        }

        @Provide
        Arbitrary<List<List<PeriodHistory>>> periodHistoryLists() {
                Arbitrary<String> historyTypes = Arbitraries.of(
                                "SUBSCRIPTION", "SUSPENSION", "RATE_PLAN", "DISCOUNT");

                Arbitrary<PeriodHistory> periodHistory = Combinators.combine(
                                Arbitraries.integers().between(2020, 2026),
                                Arbitraries.integers().between(1, 12),
                                Arbitraries.integers().between(1, 28),
                                Arbitraries.integers().between(1, 30),
                                historyTypes).as((year, month, day, days, type) -> {
                                        LocalDate start = LocalDate.of(year, month, day);
                                        return new PeriodHistory(start, start.plusDays(days), type);
                                });

                return periodHistory.list().ofMinSize(1).ofMaxSize(5)
                                .list().ofMinSize(1).ofMaxSize(3);
        }
}

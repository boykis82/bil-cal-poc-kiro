package com.billing.charge.calculation.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 리스트를 최대 크기 단위로 분할하는 유틸리티.
 * Oracle IN 절 제한(1000건) 등을 준수하기 위해 chunk 단위로 분할한다.
 */
public class ChunkPartitioner {

    private ChunkPartitioner() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
    }

    /**
     * 리스트를 최대 maxSize 단위로 분할한다.
     *
     * @param items   분할 대상 리스트
     * @param maxSize 각 chunk의 최대 크기 (1 이상)
     * @param <T>     원소 타입
     * @return 분할된 리스트의 리스트 (빈 입력 시 빈 리스트 반환)
     * @throws IllegalArgumentException maxSize가 1 미만인 경우
     */
    public static <T> List<List<T>> partition(List<T> items, int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be at least 1, but was: " + maxSize);
        }
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        int size = items.size();
        List<List<T>> result = new ArrayList<>((size + maxSize - 1) / maxSize);
        for (int i = 0; i < size; i += maxSize) {
            result.add(new ArrayList<>(items.subList(i, Math.min(i + maxSize, size))));
        }
        return result;
    }
}

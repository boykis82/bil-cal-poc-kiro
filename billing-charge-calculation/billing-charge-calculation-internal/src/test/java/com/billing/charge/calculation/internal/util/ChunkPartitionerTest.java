package com.billing.charge.calculation.internal.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkPartitionerTest {

    @Test
    void shouldReturnEmptyListForNullInput() {
        List<List<String>> result = ChunkPartitioner.partition(null, 1000);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        List<List<String>> result = ChunkPartitioner.partition(Collections.emptyList(), 1000);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSingleChunkWhenSizeIsWithinMax() {
        List<String> items = List.of("A", "B", "C");
        List<List<String>> result = ChunkPartitioner.partition(items, 1000);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsExactly("A", "B", "C");
    }

    @Test
    void shouldSplitIntoMultipleChunks() {
        List<String> items = IntStream.rangeClosed(1, 2500)
                .mapToObj(String::valueOf)
                .toList();

        List<List<String>> result = ChunkPartitioner.partition(items, 1000);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).hasSize(1000);
        assertThat(result.get(1)).hasSize(1000);
        assertThat(result.get(2)).hasSize(500);
    }

    @Test
    void shouldHandleExactMultipleOfMaxSize() {
        List<String> items = IntStream.rangeClosed(1, 2000)
                .mapToObj(String::valueOf)
                .toList();

        List<List<String>> result = ChunkPartitioner.partition(items, 1000);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(1000);
        assertThat(result.get(1)).hasSize(1000);
    }

    @Test
    void shouldPreserveOrderAcrossChunks() {
        List<String> items = IntStream.rangeClosed(1, 5)
                .mapToObj(String::valueOf)
                .toList();

        List<List<String>> result = ChunkPartitioner.partition(items, 2);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).containsExactly("1", "2");
        assertThat(result.get(1)).containsExactly("3", "4");
        assertThat(result.get(2)).containsExactly("5");
    }

    @Test
    void shouldThrowExceptionForInvalidMaxSize() {
        assertThatThrownBy(() -> ChunkPartitioner.partition(List.of("A"), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChunkPartitioner.partition(List.of("A"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnIndependentChunks() {
        List<String> items = new ArrayList<>(List.of("A", "B", "C", "D"));
        List<List<String>> result = ChunkPartitioner.partition(items, 2);

        // 원본 리스트 변경이 결과에 영향을 주지 않아야 함
        items.set(0, "X");
        assertThat(result.get(0).getFirst()).isEqualTo("A");
    }
}

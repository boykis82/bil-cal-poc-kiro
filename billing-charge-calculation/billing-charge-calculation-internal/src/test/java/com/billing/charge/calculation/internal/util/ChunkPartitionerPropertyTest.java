package com.billing.charge.calculation.internal.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 1: chunk 분할 시 원소 보존 및 크기 제한
 *
 * 임의 크기(0~5000)의 String 리스트에 대해:
 * - 각 chunk 크기 ≤ 1000
 * - 모든 chunk 합치면 원본과 동일
 * - chunk 개수 = ceil(N/1000)
 *
 * Validates: 요구사항 1.3, 1.4, 8.5
 */
@Tag("Feature: subscription-data-load-refactor, Property 1: chunk 분할 시 원소 보존 및 크기 제한")
class ChunkPartitionerPropertyTest {

    private static final int MAX_CHUNK_SIZE = 1000;

    @Property(tries = 100)
    void eachChunkSizeShouldNotExceedMaxSize(
            @ForAll @Size(min = 0, max = 5000) List<@From("arbitraryString") String> items) {

        List<List<String>> chunks = ChunkPartitioner.partition(items, MAX_CHUNK_SIZE);

        for (List<String> chunk : chunks) {
            assertThat(chunk.size()).isLessThanOrEqualTo(MAX_CHUNK_SIZE);
        }
    }

    @Property(tries = 100)
    void flattenedChunksShouldEqualOriginalList(
            @ForAll @Size(min = 0, max = 5000) List<@From("arbitraryString") String> items) {

        List<List<String>> chunks = ChunkPartitioner.partition(items, MAX_CHUNK_SIZE);

        List<String> flattened = new ArrayList<>();
        for (List<String> chunk : chunks) {
            flattened.addAll(chunk);
        }

        assertThat(flattened).isEqualTo(items);
    }

    @Property(tries = 100)
    void chunkCountShouldEqualCeilOfNDividedByMaxSize(
            @ForAll @Size(min = 0, max = 5000) List<@From("arbitraryString") String> items) {

        List<List<String>> chunks = ChunkPartitioner.partition(items, MAX_CHUNK_SIZE);

        int expectedChunkCount = items.isEmpty() ? 0 : (int) Math.ceil((double) items.size() / MAX_CHUNK_SIZE);
        assertThat(chunks.size()).isEqualTo(expectedChunkCount);
    }

    @Provide
    Arbitrary<String> arbitraryString() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
    }
}

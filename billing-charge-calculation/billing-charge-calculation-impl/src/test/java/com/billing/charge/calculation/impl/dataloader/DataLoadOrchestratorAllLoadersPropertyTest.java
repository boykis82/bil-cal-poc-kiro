package com.billing.charge.calculation.impl.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import com.billing.charge.calculation.internal.dataloader.OneTimeChargeDataLoader;
import com.billing.charge.calculation.internal.dataloader.UsageChargeDataLoader;
import com.billing.charge.calculation.internal.model.ChargeInput;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: subscription-data-load-refactor, Property 5: 오케스트레이터 전체 로더 호출
 *
 * 임의의 N개의 ChargeItemDataLoader가 등록된 DataLoadOrchestrator에 대해,
 * loadAll을 호출하면 등록된 모든 N개의 로더가 호출된다.
 *
 * Validates: 요구사항 6.1
 */
@Tag("Feature: subscription-data-load-refactor")
@Tag("Property 5: 오케스트레이터 전체 로더 호출")
class DataLoadOrchestratorAllLoadersPropertyTest {

    @Property(tries = 100)
    void allRegisteredChargeItemLoadersShouldBeInvoked(
            @ForAll("loaderCounts") int loaderCount) {

        // 각 로더의 호출 여부를 추적하는 플래그 리스트
        List<AtomicBoolean> invocationFlags = new ArrayList<>();
        List<ChargeItemDataLoader> itemLoaders = new ArrayList<>();

        ChargeItemType[] types = ChargeItemType.values();
        for (int i = 0; i < loaderCount; i++) {
            AtomicBoolean invoked = new AtomicBoolean(false);
            invocationFlags.add(invoked);
            ChargeItemType type = types[i % types.length];
            itemLoaders.add(new TrackingChargeItemDataLoader(type, invoked));
        }

        // 빈 OneTimeChargeDataLoader, UsageChargeDataLoader 리스트로 오케스트레이터 생성
        DataLoadOrchestrator orchestrator = new DataLoadOrchestrator(
                List.of(),
                List.of()
        );

        // 최소 1건의 계약ID로 loadAll 호출
        ContractBaseLoader baseLoader = contractIds -> contractIds.stream()
                .map(id -> new ContractInfo(id, "SUB-" + id, "PROD-001",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .toList();

        List<String> contractIds = List.of("CONTRACT-001");

        Map<String, ChargeInput> result = orchestrator.loadAll(baseLoader, itemLoaders, contractIds);

        // 모든 N개 로더가 호출되었는지 검증
        for (int i = 0; i < loaderCount; i++) {
            assertThat(invocationFlags.get(i).get())
                    .as("Loader %d should have been invoked", i)
                    .isTrue();
        }

        // 결과가 비어있지 않아야 함
        assertThat(result).isNotEmpty();
    }

    // --- Generators ---

    @Provide
    Arbitrary<Integer> loaderCounts() {
        return Arbitraries.integers().between(0, 10);
    }

    // --- Test Doubles ---

    /**
     * 호출 여부를 추적하는 ChargeItemDataLoader 구현체.
     */
    private static class TrackingChargeItemDataLoader implements ChargeItemDataLoader {
        private final ChargeItemType chargeItemType;
        private final AtomicBoolean invoked;

        TrackingChargeItemDataLoader(ChargeItemType chargeItemType, AtomicBoolean invoked) {
            this.chargeItemType = chargeItemType;
            this.invoked = invoked;
        }

        @Override
        public ChargeItemType getChargeItemType() {
            return chargeItemType;
        }

        @Override
        public void loadAndPopulate(List<ContractInfo> contracts, Map<String, ChargeInput> chargeInputMap) {
            invoked.set(true);
        }
    }
}

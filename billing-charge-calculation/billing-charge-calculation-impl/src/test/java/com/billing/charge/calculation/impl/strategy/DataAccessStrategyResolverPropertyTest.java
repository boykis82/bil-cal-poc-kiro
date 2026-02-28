package com.billing.charge.calculation.impl.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.exception.UnsupportedUseCaseException;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import net.jqwik.api.*;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feature: billing-charge-calculation, Property 1: Strategy 선택 일관성
 *
 * 임의의 UseCaseType에 대해, DataAccessStrategyResolver가 반환하는 Strategy의
 * supportedUseCaseType()은 요청된 UseCaseType과 항상 일치해야 한다.
 *
 * **Validates: Requirements 1.2**
 */
@Tag("Feature: billing-charge-calculation, Property 1: Strategy 선택 일관성")
class DataAccessStrategyResolverPropertyTest {

    private final DataAccessStrategyResolver resolver;

    DataAccessStrategyResolverPropertyTest() {
        // 모든 UseCaseType에 대한 stub Strategy를 생성하여 Resolver에 등록
        List<DataAccessStrategy> strategies = Arrays.stream(UseCaseType.values())
                .map(StubStrategy::new)
                .map(s -> (DataAccessStrategy) s)
                .toList();
        this.resolver = new DataAccessStrategyResolver(strategies);
    }

    /**
     * Property 1: 임의의 UseCaseType에 대해, resolve()가 반환하는 Strategy의
     * supportedUseCaseType()은 요청된 UseCaseType과 항상 일치해야 한다.
     */
    @Property(tries = 100)
    void resolvedStrategyShouldMatchRequestedUseCaseType(
            @ForAll("useCaseTypes") UseCaseType useCaseType) {

        DataAccessStrategy strategy = resolver.resolve(useCaseType);

        assertThat(strategy.supportedUseCaseType()).isEqualTo(useCaseType);
    }

    /**
     * 등록되지 않은 UseCaseType으로 resolve 시 UnsupportedUseCaseException이 발생해야 한다.
     */
    @Example
    void shouldThrowExceptionForUnregisteredUseCaseType() {
        // 빈 Strategy 목록으로 Resolver 생성
        DataAccessStrategyResolver emptyResolver = new DataAccessStrategyResolver(List.of());

        for (UseCaseType useCaseType : UseCaseType.values()) {
            assertThatThrownBy(() -> emptyResolver.resolve(useCaseType))
                    .isInstanceOf(UnsupportedUseCaseException.class);
        }
    }

    // --- Generators ---

    @Provide
    Arbitrary<UseCaseType> useCaseTypes() {
        return Arbitraries.of(UseCaseType.values());
    }

    // --- Test Doubles ---

    /**
     * 특정 UseCaseType을 지원하는 stub Strategy.
     */
    private static class StubStrategy implements DataAccessStrategy {
        private final UseCaseType useCaseType;

        StubStrategy(UseCaseType useCaseType) {
            this.useCaseType = useCaseType;
        }

        @Override
        public UseCaseType supportedUseCaseType() {
            return useCaseType;
        }

        @Override
        public ChargeInput readChargeInput(ContractInfo contractInfo) {
            return ChargeInput.builder().build();
        }

        @Override
        public void writeChargeResult(ChargeResult result) {
        }

        @Override
        public void updateProcessingStatus(String chargeItemId, ProcessingStatus status) {
        }

        @Override
        public ContractBaseLoader getContractBaseLoader() {
            throw new UnsupportedOperationException("Not used in test");
        }

        @Override
        public List<ChargeItemDataLoader> getChargeItemDataLoaders() {
            throw new UnsupportedOperationException("Not used in test");
        }
    }
}

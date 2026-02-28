package com.billing.charge.calculation.impl.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.dataloader.*;
import com.billing.charge.calculation.internal.mapper.*;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * DataAccessStrategy 유스케이스별 로더 선택 검증 단위 테스트.
 *
 * 각 전략이 올바른 ContractBaseLoader 구현체를 반환하고,
 * 적절한 ChargeItemDataLoader 목록을 제공하는지 검증한다.
 *
 * Validates: 요구사항 7.2, 7.3, 7.4, 8.4
 */
class DataAccessStrategyLoaderSelectionTest {

    // Mocked mappers
    private MasterTableMapper masterTableMapper;
    private ChargeResultMapper chargeResultMapper;
    private OrderTableMapper orderTableMapper;
    private ContractBaseMapper contractBaseMapper;
    private MonthlyFeeMapper monthlyFeeMapper;
    private DiscountMapper discountMapper;
    private BillingPaymentMapper billingPaymentMapper;
    private PrepaidMapper prepaidMapper;

    // Concrete loaders (constructed with mocked mappers)
    private MasterContractBaseLoader masterContractBaseLoader;
    private OrderContractBaseLoader orderContractBaseLoader;
    private QuotationContractBaseLoader quotationContractBaseLoader;
    private MonthlyFeeDataLoader monthlyFeeDataLoader;
    private DiscountDataLoader discountDataLoader;
    private BillingPaymentDataLoader billingPaymentDataLoader;
    private PrepaidDataLoader prepaidDataLoader;

    @BeforeEach
    void setUp() {
        masterTableMapper = mock(MasterTableMapper.class);
        chargeResultMapper = mock(ChargeResultMapper.class);
        orderTableMapper = mock(OrderTableMapper.class);
        contractBaseMapper = mock(ContractBaseMapper.class);
        monthlyFeeMapper = mock(MonthlyFeeMapper.class);
        discountMapper = mock(DiscountMapper.class);
        billingPaymentMapper = mock(BillingPaymentMapper.class);
        prepaidMapper = mock(PrepaidMapper.class);

        masterContractBaseLoader = new MasterContractBaseLoader(contractBaseMapper);
        orderContractBaseLoader = new OrderContractBaseLoader(orderTableMapper);
        quotationContractBaseLoader = new QuotationContractBaseLoader();
        monthlyFeeDataLoader = new MonthlyFeeDataLoader(monthlyFeeMapper);
        discountDataLoader = new DiscountDataLoader(discountMapper);
        billingPaymentDataLoader = new BillingPaymentDataLoader(billingPaymentMapper);
        prepaidDataLoader = new PrepaidDataLoader(prepaidMapper);
    }

    @Nested
    @DisplayName("정기청구 전략 (RegularBillingStrategy)")
    class RegularBillingStrategyTest {

        private RegularBillingStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new RegularBillingStrategy(
                    masterTableMapper, chargeResultMapper, masterContractBaseLoader,
                    monthlyFeeDataLoader, discountDataLoader, billingPaymentDataLoader, prepaidDataLoader);
        }

        @Test
        @DisplayName("MasterContractBaseLoader를 반환해야 한다")
        void shouldReturnMasterContractBaseLoader() {
            ContractBaseLoader loader = strategy.getContractBaseLoader();

            assertThat(loader).isInstanceOf(MasterContractBaseLoader.class);
            assertThat(loader).isSameAs(masterContractBaseLoader);
        }

        @Test
        @DisplayName("월정액, 할인, 청구/수납, 선납 로더를 모두 포함해야 한다")
        void shouldReturnAllChargeItemDataLoaders() {
            List<ChargeItemDataLoader> loaders = strategy.getChargeItemDataLoaders();

            assertThat(loaders).hasSize(4);
            assertThat(loaders).extracting(ChargeItemDataLoader::getChargeItemType)
                    .containsExactly(
                            ChargeItemType.MONTHLY_FEE,
                            ChargeItemType.DISCOUNT,
                            ChargeItemType.LATE_FEE,
                            ChargeItemType.PREPAID_OFFSET);
        }
    }

    @Nested
    @DisplayName("실시간 조회 전략 (RealtimeQueryStrategy)")
    class RealtimeQueryStrategyTest {

        private RealtimeQueryStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new RealtimeQueryStrategy(
                    masterTableMapper, masterContractBaseLoader,
                    monthlyFeeDataLoader, discountDataLoader, billingPaymentDataLoader, prepaidDataLoader);
        }

        @Test
        @DisplayName("MasterContractBaseLoader를 반환해야 한다")
        void shouldReturnMasterContractBaseLoader() {
            ContractBaseLoader loader = strategy.getContractBaseLoader();

            assertThat(loader).isInstanceOf(MasterContractBaseLoader.class);
            assertThat(loader).isSameAs(masterContractBaseLoader);
        }

        @Test
        @DisplayName("월정액, 할인, 청구/수납, 선납 로더를 모두 포함해야 한다")
        void shouldReturnAllChargeItemDataLoaders() {
            List<ChargeItemDataLoader> loaders = strategy.getChargeItemDataLoaders();

            assertThat(loaders).hasSize(4);
            assertThat(loaders).extracting(ChargeItemDataLoader::getChargeItemType)
                    .containsExactly(
                            ChargeItemType.MONTHLY_FEE,
                            ChargeItemType.DISCOUNT,
                            ChargeItemType.LATE_FEE,
                            ChargeItemType.PREPAID_OFFSET);
        }
    }

    @Nested
    @DisplayName("예상 조회 전략 (EstimateQueryStrategy)")
    class EstimateQueryStrategyTest {

        private EstimateQueryStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new EstimateQueryStrategy(
                    orderTableMapper, orderContractBaseLoader,
                    monthlyFeeDataLoader, discountDataLoader);
        }

        @Test
        @DisplayName("OrderContractBaseLoader를 반환해야 한다")
        void shouldReturnOrderContractBaseLoader() {
            ContractBaseLoader loader = strategy.getContractBaseLoader();

            assertThat(loader).isInstanceOf(OrderContractBaseLoader.class);
            assertThat(loader).isSameAs(orderContractBaseLoader);
        }

        @Test
        @DisplayName("월정액, 할인 로더만 포함해야 한다 (청구/수납, 선납 제외)")
        void shouldReturnOnlyMonthlyFeeAndDiscountLoaders() {
            List<ChargeItemDataLoader> loaders = strategy.getChargeItemDataLoaders();

            assertThat(loaders).hasSize(2);
            assertThat(loaders).extracting(ChargeItemDataLoader::getChargeItemType)
                    .containsExactly(ChargeItemType.MONTHLY_FEE, ChargeItemType.DISCOUNT);
        }
    }

    @Nested
    @DisplayName("견적 조회 전략 (QuotationQueryStrategy)")
    class QuotationQueryStrategyTest {

        private QuotationQueryStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new QuotationQueryStrategy(quotationContractBaseLoader);
        }

        @Test
        @DisplayName("QuotationContractBaseLoader를 반환해야 한다")
        void shouldReturnQuotationContractBaseLoader() {
            ContractBaseLoader loader = strategy.getContractBaseLoader();

            assertThat(loader).isInstanceOf(QuotationContractBaseLoader.class);
            assertThat(loader).isSameAs(quotationContractBaseLoader);
        }

        @Test
        @DisplayName("ChargeItemDataLoader가 비어있어야 한다 (기준정보만 사용)")
        void shouldReturnEmptyChargeItemDataLoaders() {
            List<ChargeItemDataLoader> loaders = strategy.getChargeItemDataLoaders();

            assertThat(loaders).isEmpty();
        }
    }

    @Nested
    @DisplayName("OLTP 단건 처리 동일 인터페이스 동작 확인")
    class OltpSingleContractTest {

        @Test
        @DisplayName("단건 계약정보로 모든 전략의 getContractBaseLoader/getChargeItemDataLoaders가 정상 동작해야 한다")
        void allStrategiesShouldWorkWithSingleContract() {
            List<DataAccessStrategy> strategies = List.of(
                    new RegularBillingStrategy(
                            masterTableMapper, chargeResultMapper, masterContractBaseLoader,
                            monthlyFeeDataLoader, discountDataLoader, billingPaymentDataLoader, prepaidDataLoader),
                    new RealtimeQueryStrategy(
                            masterTableMapper, masterContractBaseLoader,
                            monthlyFeeDataLoader, discountDataLoader, billingPaymentDataLoader, prepaidDataLoader),
                    new EstimateQueryStrategy(
                            orderTableMapper, orderContractBaseLoader,
                            monthlyFeeDataLoader, discountDataLoader),
                    new QuotationQueryStrategy(quotationContractBaseLoader)
            );

            for (DataAccessStrategy strategy : strategies) {
                // 동일 인터페이스로 ContractBaseLoader 접근 가능
                ContractBaseLoader baseLoader = strategy.getContractBaseLoader();
                assertThat(baseLoader).isNotNull();

                // 동일 인터페이스로 ChargeItemDataLoader 목록 접근 가능
                List<ChargeItemDataLoader> itemLoaders = strategy.getChargeItemDataLoaders();
                assertThat(itemLoaders).isNotNull();
            }
        }
    }
}

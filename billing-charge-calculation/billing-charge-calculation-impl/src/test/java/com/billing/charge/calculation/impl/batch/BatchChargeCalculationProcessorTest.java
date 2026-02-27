package com.billing.charge.calculation.impl.batch;

import com.billing.charge.calculation.api.dto.ContractChargeResult;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.exception.ProcessingStatusUpdateException;
import com.billing.charge.calculation.api.exception.StepExecutionException;
import com.billing.charge.calculation.impl.pipeline.Pipeline;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BatchChargeCalculationProcessor 단위 테스트.
 * chunk 내 일부 계약 실패 시 나머지 정상 처리 확인.
 * Requirements: 1.4
 */
@ExtendWith(MockitoExtension.class)
class BatchChargeCalculationProcessorTest {

    @Mock
    private PipelineConfigurator pipelineConfigurator;

    @Mock
    private PipelineEngine pipelineEngine;

    @Mock
    private DataAccessStrategyResolver strategyResolver;

    @Mock
    private DataAccessStrategy strategy;

    @Mock
    private Pipeline pipeline;

    @InjectMocks
    private BatchChargeCalculationProcessor processor;

    private static final String TENANT_ID = "TENANT-001";
    private static final ProductType PRODUCT_TYPE = ProductType.WIRELESS;
    private static final UseCaseType USE_CASE_TYPE = UseCaseType.REGULAR_BILLING;

    private List<ContractInfo> contracts;

    @BeforeEach
    void setUp() {
        contracts = List.of(
                new ContractInfo("C-001", "S-001", "P-001", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)),
                new ContractInfo("C-002", "S-002", "P-002", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)),
                new ContractInfo("C-003", "S-003", "P-003", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)));
    }

    @Test
    @DisplayName("모든 계약 정상 처리 시 전체 결과 반환")
    void shouldReturnAllResultsWhenAllContractsSucceed() {
        // given
        when(strategyResolver.resolve(USE_CASE_TYPE)).thenReturn(strategy);
        when(pipelineConfigurator.configure(TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE)).thenReturn(pipeline);
        when(strategy.readChargeInput(any(ContractInfo.class))).thenReturn(ChargeInput.builder().build());

        // when
        List<ContractChargeResult> results = processor.processChunk(TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE, contracts);

        // then
        assertThat(results).hasSize(3);
        assertThat(results).extracting(ContractChargeResult::contractId)
                .containsExactly("C-001", "C-002", "C-003");
        verify(pipelineEngine, times(3)).execute(eq(pipeline), any(ChargeContext.class), eq(strategy));
        verify(strategy, times(3)).writeChargeResult(any());
    }

    @Test
    @DisplayName("StepExecutionException 발생 시 해당 건만 실패, 나머지 정상 처리")
    void shouldContinueProcessingWhenStepExecutionExceptionOccurs() {
        // given
        when(strategyResolver.resolve(USE_CASE_TYPE)).thenReturn(strategy);
        when(pipelineConfigurator.configure(TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE)).thenReturn(pipeline);
        when(strategy.readChargeInput(any(ContractInfo.class))).thenReturn(ChargeInput.builder().build());

        // 두 번째 계약에서 StepExecutionException 발생
        doNothing()
                .doThrow(new StepExecutionException("MONTHLY_FEE", "C-002", new RuntimeException("계산 오류")))
                .doNothing()
                .when(pipelineEngine).execute(eq(pipeline), any(ChargeContext.class), eq(strategy));

        // when
        List<ContractChargeResult> results = processor.processChunk(TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE, contracts);

        // then
        assertThat(results).hasSize(3);
        // 첫 번째, 세 번째 계약은 정상 결과
        assertThat(results.get(0).contractId()).isEqualTo("C-001");
        assertThat(results.get(2).contractId()).isEqualTo("C-003");
        // 두 번째 계약은 실패 → 빈 결과
        assertThat(results.get(1).contractId()).isEqualTo("C-002");
        assertThat(results.get(1).chargeResults()).isEmpty();
        // writeChargeResult는 성공한 2건만 호출
        verify(strategy, times(2)).writeChargeResult(any());
    }

    @Test
    @DisplayName("ProcessingStatusUpdateException 발생 시 해당 건만 실패, 나머지 정상 처리")
    void shouldContinueProcessingWhenProcessingStatusUpdateExceptionOccurs() {
        // given
        when(strategyResolver.resolve(USE_CASE_TYPE)).thenReturn(strategy);
        when(pipelineConfigurator.configure(TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE)).thenReturn(pipeline);
        when(strategy.readChargeInput(any(ContractInfo.class))).thenReturn(ChargeInput.builder().build());

        // 첫 번째 계약에서 ProcessingStatusUpdateException 발생
        doThrow(new ProcessingStatusUpdateException("MONTHLY_FEE", "C-001"))
                .doNothing()
                .doNothing()
                .when(pipelineEngine).execute(eq(pipeline), any(ChargeContext.class), eq(strategy));

        // when
        List<ContractChargeResult> results = processor.processChunk(TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE, contracts);

        // then
        assertThat(results).hasSize(3);
        // 첫 번째 계약은 실패 → 빈 결과
        assertThat(results.get(0).contractId()).isEqualTo("C-001");
        assertThat(results.get(0).chargeResults()).isEmpty();
        // 두 번째, 세 번째 계약은 정상 결과
        assertThat(results.get(1).contractId()).isEqualTo("C-002");
        assertThat(results.get(2).contractId()).isEqualTo("C-003");
        // writeChargeResult는 성공한 2건만 호출
        verify(strategy, times(2)).writeChargeResult(any());
    }
}

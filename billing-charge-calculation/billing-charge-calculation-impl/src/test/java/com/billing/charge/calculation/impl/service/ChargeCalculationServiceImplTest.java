package com.billing.charge.calculation.impl.service;

import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.exception.InvalidRequestException;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChargeCalculationServiceImpl 유효성 검증 단위 테스트.
 * Requirements: 1.5, 1.6
 */
@ExtendWith(MockitoExtension.class)
class ChargeCalculationServiceImplTest {

    @Mock
    private PipelineConfigurator pipelineConfigurator;

    @Mock
    private PipelineEngine pipelineEngine;

    @Mock
    private DataAccessStrategyResolver strategyResolver;

    @InjectMocks
    private ChargeCalculationServiceImpl service;

    @Test
    @DisplayName("유스케이스 구분 누락 시 InvalidRequestException 발생")
    void shouldThrowInvalidRequestExceptionWhenUseCaseTypeIsNull() {
        ChargeCalculationRequest request = ChargeCalculationRequest.builder()
                .tenantId("TENANT-001")
                .useCaseType(null)
                .productType(ProductType.WIRELESS)
                .contracts(Collections.emptyList())
                .build();

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("유스케이스 구분");
    }

    @Test
    @DisplayName("계약정보 리스트가 null인 경우 InvalidRequestException 발생")
    void shouldThrowInvalidRequestExceptionWhenContractsIsNull() {
        ChargeCalculationRequest request = ChargeCalculationRequest.builder()
                .tenantId("TENANT-001")
                .useCaseType(UseCaseType.REGULAR_BILLING)
                .productType(ProductType.WIRELESS)
                .contracts(null)
                .build();

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("계약정보 리스트");
    }

    @Test
    @DisplayName("계약정보 리스트가 빈 경우 InvalidRequestException 발생")
    void shouldThrowInvalidRequestExceptionWhenContractsIsEmpty() {
        ChargeCalculationRequest request = ChargeCalculationRequest.builder()
                .tenantId("TENANT-001")
                .useCaseType(UseCaseType.REGULAR_BILLING)
                .productType(ProductType.WIRELESS)
                .contracts(Collections.emptyList())
                .build();

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("계약정보 리스트");
    }
}

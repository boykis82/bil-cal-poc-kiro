package com.billing.charge.calculation.internal.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.mapper.OrderTableMapper;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 예상 요금 조회 유스케이스 데이터 접근 전략.
 * 접수 테이블에서 계약정보를 읽되, 결과를 DB에 기록하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstimateQueryStrategy implements DataAccessStrategy {

    private final OrderTableMapper orderTableMapper;

    @Override
    public UseCaseType supportedUseCaseType() {
        return UseCaseType.ESTIMATE_QUERY;
    }

    @Override
    public ChargeInput readChargeInput(ContractInfo contractInfo) {
        log.debug("예상 조회 - 접수 테이블에서 계약정보 조회: contractId={}", contractInfo.contractId());
        return orderTableMapper.readChargeInput(contractInfo);
    }

    @Override
    public void writeChargeResult(ChargeResult result) {
        // 예상 조회는 결과를 DB에 기록하지 않는다 (no-op)
    }

    @Override
    public void updateProcessingStatus(String chargeItemId, ProcessingStatus status) {
        // 예상 조회는 처리 상태를 갱신하지 않는다 (no-op)
    }
}

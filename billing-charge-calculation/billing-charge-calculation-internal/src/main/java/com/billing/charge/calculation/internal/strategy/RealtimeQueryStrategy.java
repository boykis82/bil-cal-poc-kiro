package com.billing.charge.calculation.internal.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.mapper.MasterTableMapper;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 실시간 요금 조회 유스케이스 데이터 접근 전략.
 * 마스터 테이블에서 계약정보를 읽되, 결과를 DB에 기록하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeQueryStrategy implements DataAccessStrategy {

    private final MasterTableMapper masterTableMapper;

    @Override
    public UseCaseType supportedUseCaseType() {
        return UseCaseType.REALTIME_QUERY;
    }

    @Override
    public ChargeInput readChargeInput(ContractInfo contractInfo) {
        log.debug("실시간 조회 - 마스터 테이블에서 계약정보 조회: contractId={}", contractInfo.contractId());
        return masterTableMapper.readChargeInput(contractInfo);
    }

    @Override
    public void writeChargeResult(ChargeResult result) {
        // 실시간 조회는 결과를 DB에 기록하지 않는다 (no-op)
    }

    @Override
    public void updateProcessingStatus(String chargeItemId, ProcessingStatus status) {
        // 실시간 조회는 처리 상태를 갱신하지 않는다 (no-op)
    }
}

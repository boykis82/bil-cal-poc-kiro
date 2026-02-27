package com.billing.charge.calculation.internal.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.mapper.ChargeResultMapper;
import com.billing.charge.calculation.internal.mapper.MasterTableMapper;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 정기청구 유스케이스 데이터 접근 전략.
 * 마스터 테이블에서 계약정보를 읽고, 계산 결과를 DB에 기록하며, 처리 상태를 갱신한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegularBillingStrategy implements DataAccessStrategy {

    private final MasterTableMapper masterTableMapper;
    private final ChargeResultMapper chargeResultMapper;

    @Override
    public UseCaseType supportedUseCaseType() {
        return UseCaseType.REGULAR_BILLING;
    }

    @Override
    public ChargeInput readChargeInput(ContractInfo contractInfo) {
        log.debug("정기청구 - 마스터 테이블에서 계약정보 조회: contractId={}", contractInfo.contractId());
        return masterTableMapper.readChargeInput(contractInfo);
    }

    @Override
    public void writeChargeResult(ChargeResult result) {
        log.debug("정기청구 - 요금 계산 결과 DB 기록: contractId={}", result.contractId());
        chargeResultMapper.insertChargeResult(result);
    }

    @Override
    public void updateProcessingStatus(String chargeItemId, ProcessingStatus status) {
        log.debug("정기청구 - 처리 상태 갱신: chargeItemId={}, status={}", chargeItemId, status);
        chargeResultMapper.updateProcessingStatus(chargeItemId, status);
    }
}

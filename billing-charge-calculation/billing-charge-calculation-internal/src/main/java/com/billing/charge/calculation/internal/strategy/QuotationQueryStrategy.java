package com.billing.charge.calculation.internal.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import com.billing.charge.calculation.internal.dataloader.QuotationContractBaseLoader;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 견적 요금 조회 유스케이스 데이터 접근 전략.
 * 기준정보만 사용하여 요금을 계산한다. (기준정보는 캐시를 통해 Step에서 직접 접근)
 * 마스터/접수 테이블 어디에도 데이터가 없으므로 빈 ChargeInput을 반환한다.
 * ChargeItemDataLoader는 사용하지 않는다 (기준정보만으로 계산).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationQueryStrategy implements DataAccessStrategy {

    private final QuotationContractBaseLoader quotationContractBaseLoader;

    @Override
    public UseCaseType supportedUseCaseType() {
        return UseCaseType.QUOTATION_QUERY;
    }

    @Override
    public ChargeInput readChargeInput(ContractInfo contractInfo) {
        log.debug("견적 조회 - 기준정보만 사용, 빈 ChargeInput 반환: contractId={}", contractInfo.contractId());
        return ChargeInput.builder()
                .suspensionHistories(List.of())
                .prepaidRecords(List.of())
                .build();
    }

    @Override
    public void writeChargeResult(ChargeResult result) {
        // 견적 조회는 결과를 DB에 기록하지 않는다 (no-op)
    }

    @Override
    public void updateProcessingStatus(String chargeItemId, ProcessingStatus status) {
        // 견적 조회는 처리 상태를 갱신하지 않는다 (no-op)
    }

    @Override
    public ContractBaseLoader getContractBaseLoader() {
        return quotationContractBaseLoader;
    }

    @Override
    public List<ChargeItemDataLoader> getChargeItemDataLoaders() {
        return List.of();
    }
}

package com.billing.charge.calculation.internal.strategy;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ProcessingStatus;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;

import java.util.List;

/**
 * 요금 계산 유스케이스별 데이터 읽기/쓰기 전략.
 * Strategy Pattern의 핵심 인터페이스.
 * 유스케이스(정기청구, 실시간 조회, 예상 조회, 견적 조회)에 따라
 * 데이터 접근 방식을 교체할 수 있다.
 */
public interface DataAccessStrategy {

    /**
     * 이 전략이 지원하는 유스케이스 유형.
     */
    UseCaseType supportedUseCaseType();

    /**
     * 계약정보 기반으로 요금 계산에 필요한 입력 데이터를 조회한다.
     */
    ChargeInput readChargeInput(ContractInfo contractInfo);

    /**
     * 요금 계산 결과를 저장한다.
     * 견적/예상 조회 등 저장이 불필요한 전략은 no-op.
     */
    void writeChargeResult(ChargeResult result);

    /**
     * 요금 항목 처리 상태를 DB에 갱신한다.
     * 저장이 불필요한 전략은 no-op.
     */
    void updateProcessingStatus(String chargeItemId, ProcessingStatus status);

    /**
     * 이 전략에 해당하는 ContractBaseLoader를 반환한다.
     */
    ContractBaseLoader getContractBaseLoader();

    /**
     * 이 전략에 해당하는 ChargeItemDataLoader 목록을 반환한다.
     */
    List<ChargeItemDataLoader> getChargeItemDataLoaders();
}

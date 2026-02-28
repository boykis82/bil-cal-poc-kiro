package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 기준정보 기반 계약 기본정보 로더.
 * <p>
 * 견적 요금 조회 유스케이스에서 사용한다.
 * 마스터 테이블, 접수 테이블 어디에도 데이터가 없고
 * 상품/할인 기준정보만으로 요금을 조회하는 경우에 해당한다.
 * 실제 DB 조회 없이 입력된 계약ID로 최소한의 ContractInfo stub을 생성하여 반환한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationContractBaseLoader implements ContractBaseLoader {

    @Override
    public List<ContractInfo> loadBaseContracts(List<String> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }

        return contractIds.stream()
                .map(contractId -> new ContractInfo(contractId, null, null, null, null))
                .toList();
    }
}

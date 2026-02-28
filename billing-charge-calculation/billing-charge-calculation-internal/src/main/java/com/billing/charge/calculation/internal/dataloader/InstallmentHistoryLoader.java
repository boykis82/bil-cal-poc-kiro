package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.mapper.InstallmentHistoryMapper;
import com.billing.charge.calculation.internal.model.InstallmentHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 할부이력 데이터 로더.
 * <p>
 * {@link OneTimeChargeDataLoader}의 구현체로서 할부이력 데이터를
 * chunk 단위로 조회하고 계약ID 기준으로 그룹핑하여 반환한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallmentHistoryLoader implements OneTimeChargeDataLoader<InstallmentHistory> {

    private final InstallmentHistoryMapper installmentHistoryMapper;

    @Override
    public Class<InstallmentHistory> getDomainType() {
        return InstallmentHistory.class;
    }

    @Override
    public Map<String, List<InstallmentHistory>> loadData(List<ContractInfo> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        List<InstallmentHistory> histories = installmentHistoryMapper.selectInstallmentHistories(contractIds);
        if (histories == null || histories.isEmpty()) {
            return Collections.emptyMap();
        }

        return histories.stream()
                .filter(h -> h.getContractId() != null)
                .collect(Collectors.groupingBy(InstallmentHistory::getContractId));
    }
}

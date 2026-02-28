package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.mapper.DataUsageMapper;
import com.billing.charge.calculation.internal.model.DataUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 데이터 사용량 데이터 로더.
 * <p>
 * {@link UsageChargeDataLoader}의 구현체로서 데이터 사용량 정보를
 * chunk 단위로 조회하고 계약ID 기준으로 그룹핑하여 반환한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataUsageLoader implements UsageChargeDataLoader<DataUsage> {

    private final DataUsageMapper dataUsageMapper;

    @Override
    public Class<DataUsage> getDomainType() {
        return DataUsage.class;
    }

    @Override
    public Map<String, List<DataUsage>> loadData(List<ContractInfo> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        List<DataUsage> usages = dataUsageMapper.selectDataUsages(contractIds);
        if (usages == null || usages.isEmpty()) {
            return Collections.emptyMap();
        }

        return usages.stream()
                .filter(u -> u.getContractId() != null)
                .collect(Collectors.groupingBy(DataUsage::getContractId));
    }
}

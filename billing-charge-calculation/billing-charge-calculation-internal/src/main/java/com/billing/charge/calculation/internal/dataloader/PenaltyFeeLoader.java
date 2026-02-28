package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.mapper.PenaltyFeeMapper;
import com.billing.charge.calculation.internal.model.PenaltyFee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 위약금 데이터 로더.
 * <p>
 * {@link OneTimeChargeDataLoader}의 구현체로서 위약금 데이터를
 * chunk 단위로 조회하고 계약ID 기준으로 그룹핑하여 반환한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenaltyFeeLoader implements OneTimeChargeDataLoader<PenaltyFee> {

    private final PenaltyFeeMapper penaltyFeeMapper;

    @Override
    public Class<PenaltyFee> getDomainType() {
        return PenaltyFee.class;
    }

    @Override
    public Map<String, List<PenaltyFee>> loadData(List<ContractInfo> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        List<PenaltyFee> penalties = penaltyFeeMapper.selectPenaltyFees(contractIds);
        if (penalties == null || penalties.isEmpty()) {
            return Collections.emptyMap();
        }

        return penalties.stream()
                .filter(p -> p.getContractId() != null)
                .collect(Collectors.groupingBy(PenaltyFee::getContractId));
    }
}

package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.mapper.VoiceUsageMapper;
import com.billing.charge.calculation.internal.model.VoiceUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 음성통화 사용량 데이터 로더.
 * <p>
 * {@link UsageChargeDataLoader}의 구현체로서 음성통화 사용량 데이터를
 * chunk 단위로 조회하고 계약ID 기준으로 그룹핑하여 반환한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceUsageLoader implements UsageChargeDataLoader<VoiceUsage> {

    private final VoiceUsageMapper voiceUsageMapper;

    @Override
    public Class<VoiceUsage> getDomainType() {
        return VoiceUsage.class;
    }

    @Override
    public Map<String, List<VoiceUsage>> loadData(List<ContractInfo> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        List<VoiceUsage> usages = voiceUsageMapper.selectVoiceUsages(contractIds);
        if (usages == null || usages.isEmpty()) {
            return Collections.emptyMap();
        }

        return usages.stream()
                .filter(u -> u.getContractId() != null)
                .collect(Collectors.groupingBy(VoiceUsage::getContractId));
    }
}

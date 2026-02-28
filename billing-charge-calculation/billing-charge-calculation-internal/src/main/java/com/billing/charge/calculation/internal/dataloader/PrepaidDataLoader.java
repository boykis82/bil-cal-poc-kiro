package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.mapper.PrepaidMapper;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.PrepaidRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 선납내역 데이터 로더.
 * <p>
 * 선납반제 처리에 필요한 선납내역을 chunk 단위로 조회한다.
 * 조회 결과를 계약ID 기준으로 그룹핑하여 ChargeInput의 prepaidRecords에 설정한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrepaidDataLoader implements ChargeItemDataLoader {

    private final PrepaidMapper prepaidMapper;

    @Override
    public ChargeItemType getChargeItemType() {
        return ChargeItemType.PREPAID_OFFSET;
    }

    @Override
    public void loadAndPopulate(List<ContractInfo> contracts, Map<String, ChargeInput> chargeInputMap) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        // 선납내역 조회 (chunk 단위 일괄)
        List<PrepaidRecord> prepaidRecords = prepaidMapper.selectPrepaidRecords(contractIds);
        if (prepaidRecords != null) {
            Map<String, List<PrepaidRecord>> prepaidByContractId = prepaidRecords.stream()
                    .filter(pr -> pr.contractId() != null)
                    .collect(Collectors.groupingBy(PrepaidRecord::contractId));

            prepaidByContractId.forEach((contractId, records) -> {
                ChargeInput chargeInput = chargeInputMap.get(contractId);
                if (chargeInput != null) {
                    chargeInput.setPrepaidRecords(records);
                }
            });
        }
    }
}

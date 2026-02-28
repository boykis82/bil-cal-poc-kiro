package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.mapper.MonthlyFeeMapper;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.SubscriptionInfo;
import com.billing.charge.calculation.internal.model.SuspensionHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 월정액 데이터 로더.
 * <p>
 * 월정액 요금 계산에 필요한 상품 가입정보와 정지이력을 chunk 단위로 조회한다.
 * 기준정보 테이블과의 join 없이 마스터 테이블에서만 데이터를 조회하며,
 * 기준정보는 인메모리 캐시에서 별도로 조회한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyFeeDataLoader implements ChargeItemDataLoader {

    private final MonthlyFeeMapper monthlyFeeMapper;

    @Override
    public ChargeItemType getChargeItemType() {
        return ChargeItemType.MONTHLY_FEE;
    }

    @Override
    public void loadAndPopulate(List<ContractInfo> contracts, Map<String, ChargeInput> chargeInputMap) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        // 상품 가입정보 조회 (chunk 단위 일괄)
        List<SubscriptionInfo> subscriptionInfos = monthlyFeeMapper.selectSubscriptionInfos(contractIds);
        if (subscriptionInfos != null) {
            Map<String, SubscriptionInfo> subscriptionByContractId = subscriptionInfos.stream()
                    .filter(s -> s.contractId() != null)
                    .collect(Collectors.toMap(
                            SubscriptionInfo::contractId,
                            s -> s,
                            (existing, replacement) -> existing
                    ));

            subscriptionByContractId.forEach((contractId, subscriptionInfo) -> {
                ChargeInput chargeInput = chargeInputMap.get(contractId);
                if (chargeInput != null) {
                    chargeInput.setSubscriptionInfo(subscriptionInfo);
                }
            });
        }

        // 정지이력 조회 (chunk 단위 일괄)
        List<SuspensionHistory> suspensionHistories = monthlyFeeMapper.selectSuspensionHistories(contractIds);
        if (suspensionHistories != null) {
            Map<String, List<SuspensionHistory>> suspensionsByContractId = suspensionHistories.stream()
                    .filter(sh -> sh.contractId() != null)
                    .collect(Collectors.groupingBy(SuspensionHistory::contractId));

            suspensionsByContractId.forEach((contractId, histories) -> {
                ChargeInput chargeInput = chargeInputMap.get(contractId);
                if (chargeInput != null) {
                    chargeInput.setSuspensionHistories(histories);
                }
            });
        }
    }
}

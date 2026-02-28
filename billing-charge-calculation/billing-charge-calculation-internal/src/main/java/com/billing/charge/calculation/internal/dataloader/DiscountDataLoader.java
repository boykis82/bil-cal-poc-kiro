package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.mapper.DiscountMapper;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.DiscountSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 할인 데이터 로더.
 * <p>
 * 할인 계산에 필요한 할인 가입정보를 chunk 단위로 조회한다.
 * 할인 기준정보는 인메모리 캐시에서 별도로 조회하므로 여기서는 조회하지 않는다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountDataLoader implements ChargeItemDataLoader {

    private final DiscountMapper discountMapper;

    @Override
    public ChargeItemType getChargeItemType() {
        return ChargeItemType.DISCOUNT;
    }

    @Override
    public void loadAndPopulate(List<ContractInfo> contracts, Map<String, ChargeInput> chargeInputMap) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        // 할인 가입정보 조회 (chunk 단위 일괄)
        List<DiscountSubscription> discountSubscriptions = discountMapper.selectDiscountSubscriptions(contractIds);
        if (discountSubscriptions != null) {
            Map<String, List<DiscountSubscription>> discountsByContractId = discountSubscriptions.stream()
                    .filter(ds -> ds.getContractId() != null)
                    .collect(Collectors.groupingBy(DiscountSubscription::getContractId));

            discountsByContractId.forEach((contractId, subscriptions) -> {
                ChargeInput chargeInput = chargeInputMap.get(contractId);
                if (chargeInput != null) {
                    chargeInput.setDiscountSubscriptions(subscriptions);
                }
            });
        }
    }
}

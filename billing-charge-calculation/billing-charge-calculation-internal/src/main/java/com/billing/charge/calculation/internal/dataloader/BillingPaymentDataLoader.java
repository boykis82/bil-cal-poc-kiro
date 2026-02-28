package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.internal.mapper.BillingPaymentMapper;
import com.billing.charge.calculation.internal.model.BillingInfo;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.PaymentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 청구/수납 데이터 로더.
 * <p>
 * 연체가산금, 자동납부할인 계산에 필요한 청구정보와 수납정보를 chunk 단위로 조회한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPaymentDataLoader implements ChargeItemDataLoader {

    private final BillingPaymentMapper billingPaymentMapper;

    @Override
    public ChargeItemType getChargeItemType() {
        return ChargeItemType.LATE_FEE;
    }

    @Override
    public void loadAndPopulate(List<ContractInfo> contracts, Map<String, ChargeInput> chargeInputMap) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }

        List<String> contractIds = contracts.stream()
                .map(ContractInfo::contractId)
                .toList();

        // 청구정보 조회 (chunk 단위 일괄)
        List<BillingInfo> billingInfos = billingPaymentMapper.selectBillingInfos(contractIds);
        if (billingInfos != null) {
            Map<String, BillingInfo> billingByContractId = billingInfos.stream()
                    .filter(b -> b.contractId() != null)
                    .collect(Collectors.toMap(
                            BillingInfo::contractId,
                            b -> b,
                            (existing, replacement) -> existing
                    ));

            billingByContractId.forEach((contractId, billingInfo) -> {
                ChargeInput chargeInput = chargeInputMap.get(contractId);
                if (chargeInput != null) {
                    chargeInput.setBillingInfo(billingInfo);
                }
            });
        }

        // 수납정보 조회 (chunk 단위 일괄)
        List<PaymentInfo> paymentInfos = billingPaymentMapper.selectPaymentInfos(contractIds);
        if (paymentInfos != null) {
            Map<String, PaymentInfo> paymentByContractId = paymentInfos.stream()
                    .filter(p -> p.contractId() != null)
                    .collect(Collectors.toMap(
                            PaymentInfo::contractId,
                            p -> p,
                            (existing, replacement) -> existing
                    ));

            paymentByContractId.forEach((contractId, paymentInfo) -> {
                ChargeInput chargeInput = chargeInputMap.get(contractId);
                if (chargeInput != null) {
                    chargeInput.setPaymentInfo(paymentInfo);
                }
            });
        }
    }
}

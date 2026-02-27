package com.billing.charge.calculation.internal.context;

import com.billing.charge.calculation.api.dto.ContractChargeResult;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.enums.ChargeItemType;
import com.billing.charge.calculation.api.model.FlatChargeResult;
import com.billing.charge.calculation.api.model.PeriodChargeResult;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import com.billing.charge.calculation.internal.util.PeriodToFlatCompactor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 요금 계산 과정에서 입력 데이터와 중간 계산 결과를 담는 컨텍스트.
 * Pipeline의 각 Step 간 데이터 전달 매개체.
 */
@Getter
public class ChargeContext {

    private final String tenantId;
    private final ContractInfo contractInfo;
    private final ChargeInput chargeInput;
    private final List<PeriodChargeResult> periodResults = new ArrayList<>();
    private final List<FlatChargeResult> flatResults = new ArrayList<>();

    private ChargeContext(String tenantId, ContractInfo contractInfo, ChargeInput chargeInput) {
        this.tenantId = tenantId;
        this.contractInfo = contractInfo;
        this.chargeInput = chargeInput;
    }

    public static ChargeContext of(String tenantId, ContractInfo contractInfo, ChargeInput input) {
        return new ChargeContext(tenantId, contractInfo, input);
    }

    /** 기간 존재 결과 추가 (월정액, 할인1) */
    public void addPeriodResult(PeriodChargeResult result) {
        periodResults.add(result);
    }

    /** 기간 미존재 결과 추가 (일회성, 통화료, 할인2, 부가세 등) */
    public void addFlatResult(FlatChargeResult result) {
        flatResults.add(result);
    }

    /** 전체 기간 존재 결과 조회 (불변 뷰) */
    public List<PeriodChargeResult> getPeriodResults() {
        return Collections.unmodifiableList(periodResults);
    }

    /** 전체 기간 미존재 결과 조회 (불변 뷰) */
    public List<FlatChargeResult> getFlatResults() {
        return Collections.unmodifiableList(flatResults);
    }

    /** 특정 유형의 기간 존재 결과만 조회 */
    public List<PeriodChargeResult> getPeriodResultsByType(ChargeItemType type) {
        return periodResults.stream()
                .filter(r -> r.chargeItemType() == type)
                .toList();
    }

    /** 특정 유형의 기간 미존재 결과만 조회 */
    public List<FlatChargeResult> getFlatResultsByType(ChargeItemType type) {
        return flatResults.stream()
                .filter(r -> r.chargeItemType() == type)
                .toList();
    }

    /**
     * 할인1 완료 후 호출.
     * 기간 존재 결과(원금 + 할인1)를 항목코드별 합산하여 Flat 결과로 압축한다.
     */
    public void compactPeriodToFlat() {
        List<FlatChargeResult> compacted = PeriodToFlatCompactor.compact(periodResults);
        flatResults.addAll(compacted);
        periodResults.clear();
    }

    /** 최종 요금 계산 결과 생성 */
    public ChargeResult toChargeResult() {
        return new ChargeResult(contractInfo.contractId(), List.copyOf(flatResults));
    }

    /** 계약별 요금 계산 결과 DTO 생성 */
    public ContractChargeResult toContractChargeResult() {
        return new ContractChargeResult(contractInfo.contractId(), List.copyOf(flatResults));
    }
}

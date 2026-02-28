package com.billing.charge.calculation.impl.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.dataloader.ChargeItemDataLoader;
import com.billing.charge.calculation.internal.dataloader.ContractBaseLoader;
import com.billing.charge.calculation.internal.dataloader.OneTimeChargeDataLoader;
import com.billing.charge.calculation.internal.dataloader.UsageChargeDataLoader;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.OneTimeChargeDomain;
import com.billing.charge.calculation.internal.model.UsageChargeDomain;
import com.billing.charge.calculation.internal.util.ChunkPartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 요금항목별 분리 조회를 오케스트레이션하는 핵심 컴포넌트.
 * <p>
 * 파이프라인 실행 전 모든 데이터 로딩을 일괄 수행한다.
 * Spring 컨텍스트에 등록된 {@link OneTimeChargeDataLoader}, {@link UsageChargeDataLoader}를
 * 자동 인식하며, {@link ContractBaseLoader}와 {@link ChargeItemDataLoader} 목록은
 * 유스케이스별 DataAccessStrategy에서 제공받는다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoadOrchestrator {

    private static final int MAX_CHUNK_SIZE = 1000;

    // Spring 컨텍스트에서 자동 주입되는 전체 등록 로더
    private final List<OneTimeChargeDataLoader<?>> oneTimeChargeDataLoaders;
    private final List<UsageChargeDataLoader<?>> usageChargeDataLoaders;

    /**
     * chunk 단위로 모든 요금항목 데이터를 로딩하여 계약별 ChargeInput을 조립한다.
     *
     * @param baseLoader   계약 기본정보 로더 (유스케이스별 전략에서 제공)
     * @param itemLoaders  요금항목별 데이터 로더 목록 (유스케이스별 전략에서 제공)
     * @param contractIds  계약ID 리스트
     * @return 계약ID → ChargeInput 매핑
     */
    public Map<String, ChargeInput> loadAll(
            ContractBaseLoader baseLoader,
            List<ChargeItemDataLoader> itemLoaders,
            List<String> contractIds) {

        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 1000건 초과 시 chunk 분할하여 ContractBaseLoader로 기본정보 조회
        List<ContractInfo> baseContracts = loadBaseContractsInChunks(baseLoader, contractIds);
        if (baseContracts.isEmpty()) {
            log.debug("ContractBaseLoader 조회 결과가 비어있어 후속 로더 호출을 생략합니다.");
            return Collections.emptyMap();
        }

        // 2. ChargeInput 초기화 (계약ID → ChargeInput)
        Map<String, ChargeInput> chargeInputMap = new LinkedHashMap<>();
        for (ContractInfo contract : baseContracts) {
            chargeInputMap.put(contract.contractId(), ChargeInput.builder().build());
        }

        // 3. ChargeItemDataLoader 순차 호출
        for (ChargeItemDataLoader loader : itemLoaders) {
            log.debug("ChargeItemDataLoader 호출: {}", loader.getChargeItemType());
            loader.loadAndPopulate(baseContracts, chargeInputMap);
        }

        // 4. OneTimeChargeDataLoader 순차 호출 → ChargeInput에 설정
        for (OneTimeChargeDataLoader<?> loader : oneTimeChargeDataLoaders) {
            log.debug("OneTimeChargeDataLoader 호출: {}", loader.getDomainType().getSimpleName());
            populateOneTimeChargeData(loader, baseContracts, chargeInputMap);
        }

        // 5. UsageChargeDataLoader 순차 호출 → ChargeInput에 설정
        for (UsageChargeDataLoader<?> loader : usageChargeDataLoaders) {
            log.debug("UsageChargeDataLoader 호출: {}", loader.getDomainType().getSimpleName());
            populateUsageChargeData(loader, baseContracts, chargeInputMap);
        }

        return chargeInputMap;
    }

    private List<ContractInfo> loadBaseContractsInChunks(
            ContractBaseLoader baseLoader, List<String> contractIds) {
        if (contractIds.size() <= MAX_CHUNK_SIZE) {
            return baseLoader.loadBaseContracts(contractIds);
        }

        List<List<String>> chunks = ChunkPartitioner.partition(contractIds, MAX_CHUNK_SIZE);
        List<ContractInfo> allContracts = new ArrayList<>();
        for (List<String> chunk : chunks) {
            allContracts.addAll(baseLoader.loadBaseContracts(chunk));
        }
        return allContracts;
    }

    private <T extends OneTimeChargeDomain> void populateOneTimeChargeData(
            OneTimeChargeDataLoader<T> loader,
            List<ContractInfo> contracts,
            Map<String, ChargeInput> chargeInputMap) {
        Map<String, List<T>> dataByContract = loader.loadData(contracts);
        dataByContract.forEach((contractId, dataList) -> {
            ChargeInput input = chargeInputMap.get(contractId);
            if (input != null) {
                input.putOneTimeChargeData(loader.getDomainType(), dataList);
            }
        });
    }

    private <T extends UsageChargeDomain> void populateUsageChargeData(
            UsageChargeDataLoader<T> loader,
            List<ContractInfo> contracts,
            Map<String, ChargeInput> chargeInputMap) {
        Map<String, List<T>> dataByContract = loader.loadData(contracts);
        dataByContract.forEach((contractId, dataList) -> {
            ChargeInput input = chargeInputMap.get(contractId);
            if (input != null) {
                input.putUsageChargeData(loader.getDomainType(), dataList);
            }
        });
    }
}

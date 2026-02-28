package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.mapper.OrderTableMapper;
import com.billing.charge.calculation.internal.util.ChunkPartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 접수 테이블 기반 계약 기본정보 로더.
 * <p>
 * 예상 요금 조회 유스케이스에서 사용한다.
 * 개통 완료 전 마스터 테이블에는 아직 데이터가 없고
 * 접수 테이블에만 존재하는 경우에 해당한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderContractBaseLoader implements ContractBaseLoader {

    private static final int CHUNK_SIZE = 1000;

    private final OrderTableMapper orderTableMapper;

    @Override
    public List<ContractInfo> loadBaseContracts(List<String> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<String>> chunks = ChunkPartitioner.partition(contractIds, CHUNK_SIZE);
        List<ContractInfo> results = new ArrayList<>();

        for (List<String> chunk : chunks) {
            List<ContractInfo> chunkResult = orderTableMapper.selectBaseContractsByOrder(chunk);
            if (chunkResult != null) {
                results.addAll(chunkResult);
            }
        }

        return results.isEmpty() ? Collections.emptyList() : results;
    }
}

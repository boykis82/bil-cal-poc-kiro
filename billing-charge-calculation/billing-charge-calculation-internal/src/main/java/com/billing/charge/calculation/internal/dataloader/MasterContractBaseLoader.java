package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.internal.mapper.ContractBaseMapper;
import com.billing.charge.calculation.internal.util.ChunkPartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 마스터 테이블 기반 계약 기본정보 로더.
 * <p>
 * 정기청구(배치)와 실시간 조회(OLTP) 유스케이스에서 사용한다.
 * 마스터 테이블에서 계약ID와 최소한의 기본 정보만을 조회하며,
 * 기준정보 테이블과의 join 없이 처리한다.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterContractBaseLoader implements ContractBaseLoader {

    private static final int CHUNK_SIZE = 1000;

    private final ContractBaseMapper contractBaseMapper;

    @Override
    public List<ContractInfo> loadBaseContracts(List<String> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<String>> chunks = ChunkPartitioner.partition(contractIds, CHUNK_SIZE);
        List<ContractInfo> results = new ArrayList<>();

        for (List<String> chunk : chunks) {
            List<ContractInfo> chunkResult = contractBaseMapper.selectBaseContracts(chunk);
            if (chunkResult != null) {
                results.addAll(chunkResult);
            }
        }

        return results.isEmpty() ? Collections.emptyList() : results;
    }
}

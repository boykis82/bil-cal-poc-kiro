package com.billing.charge.calculation.internal.dataloader;

import com.billing.charge.calculation.api.dto.ContractInfo;

import java.util.List;

/**
 * 계약 기본정보 로더 인터페이스.
 * <p>
 * 유스케이스별 DataAccessStrategy 구현체에서 이 인터페이스를 구현한다.
 * 요금 계산 대상 가입자의 계약ID와 최소한의 기본 정보만을 조회하며,
 * 기준정보 테이블과의 join 없이 마스터 테이블(또는 유스케이스에 따른 원천 테이블)에서만 데이터를 조회한다.
 * </p>
 */
public interface ContractBaseLoader {

    /**
     * 계약ID 리스트로 기본정보를 chunk 단위 조회한다.
     * <p>
     * IN 조건 최대 1000건 제한은 구현체 내부에서 {@code ChunkPartitioner}를 활용하여 처리한다.
     * 1000건을 초과하는 경우 1000건 단위로 분할하여 복수 회 조회를 수행한다.
     * </p>
     *
     * @param contractIds 계약ID 리스트
     * @return 계약 기본정보 리스트 (조회 결과 없으면 빈 리스트)
     */
    List<ContractInfo> loadBaseContracts(List<String> contractIds);
}

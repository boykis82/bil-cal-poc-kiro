package com.billing.charge.calculation.impl.strategy;

import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.api.exception.UnsupportedUseCaseException;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UseCaseType에 따라 적절한 DataAccessStrategy 구현체를 선택하는 Resolver.
 * Spring에 의해 주입된 모든 DataAccessStrategy 구현체를 supportedUseCaseType() 기준으로 매핑한다.
 */
@Slf4j
@Component
public class DataAccessStrategyResolver {

    private final Map<UseCaseType, DataAccessStrategy> strategyMap;

    public DataAccessStrategyResolver(List<DataAccessStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        DataAccessStrategy::supportedUseCaseType,
                        Function.identity()));
        log.info("DataAccessStrategy 등록 완료: {}", strategyMap.keySet());
    }

    /**
     * 주어진 UseCaseType에 해당하는 DataAccessStrategy를 반환한다.
     *
     * @param useCaseType 요금 계산 유스케이스 유형
     * @return 해당 유스케이스의 DataAccessStrategy 구현체
     * @throws UnsupportedUseCaseException 지원하지 않는 유스케이스 유형인 경우
     */
    public DataAccessStrategy resolve(UseCaseType useCaseType) {
        DataAccessStrategy strategy = strategyMap.get(useCaseType);
        if (strategy == null) {
            throw new UnsupportedUseCaseException(useCaseType);
        }
        return strategy;
    }
}

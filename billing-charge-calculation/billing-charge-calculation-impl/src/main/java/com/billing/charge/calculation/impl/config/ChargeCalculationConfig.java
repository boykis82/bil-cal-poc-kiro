package com.billing.charge.calculation.impl.config;

import com.billing.charge.calculation.internal.step.ChargeItemStep;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 요금 계산 모듈 Spring 설정 클래스.
 * <p>
 * - impl, internal 패키지의 컴포넌트 스캔
 * - MyBatis Mapper 인터페이스 스캔
 * - ChargeItemStep Bean들의 stepId 매핑
 */
@Configuration
@ComponentScan(basePackages = {
        "com.billing.charge.calculation.impl",
        "com.billing.charge.calculation.internal"
})
@MapperScan("com.billing.charge.calculation.internal.mapper")
public class ChargeCalculationConfig {

    /**
     * ChargeItemStep Bean들을 stepId로 매핑하는 Map을 제공한다.
     * PipelineConfigurator에서 stepId로 Step Bean을 조회할 때 사용.
     *
     * @param steps Spring에 의해 주입된 모든 ChargeItemStep 구현체
     * @return stepId → ChargeItemStep 매핑
     */
    @Bean
    public Map<String, ChargeItemStep> chargeItemStepMap(List<ChargeItemStep> steps) {
        return steps.stream()
                .collect(Collectors.toMap(ChargeItemStep::getStepId, Function.identity()));
    }
}

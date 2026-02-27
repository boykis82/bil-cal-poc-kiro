package com.billing.charge.calculation.impl.pipeline;

import com.billing.charge.calculation.api.enums.ProductType;
import com.billing.charge.calculation.api.enums.UseCaseType;
import com.billing.charge.calculation.internal.configurator.DbPipelineConfigurator;
import org.springframework.stereotype.Component;

/**
 * PipelineConfigurator의 기본 구현체.
 * DbPipelineConfigurator에 위임하여 DB 기반 Pipeline 구성을 수행하고,
 * 결과를 Pipeline 객체로 변환한다.
 */
@Component
public class DefaultPipelineConfigurator implements PipelineConfigurator {

    private final DbPipelineConfigurator dbPipelineConfigurator;

    public DefaultPipelineConfigurator(DbPipelineConfigurator dbPipelineConfigurator) {
        this.dbPipelineConfigurator = dbPipelineConfigurator;
    }

    @Override
    public Pipeline configure(String tenantId, ProductType productType, UseCaseType useCaseType) {
        DbPipelineConfigurator.PipelineConfiguration config = dbPipelineConfigurator.configure(tenantId, productType,
                useCaseType);

        return new Pipeline(config.pipelineConfigId(), config.steps());
    }
}

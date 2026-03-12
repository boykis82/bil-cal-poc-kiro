# 요금 계산 모듈 통합 UML 다이어그램

본 문서는 billing-charge-calculation 모듈의 전체 아키텍처를 시각화한 통합 UML 다이어그램을 제공합니다.

## 목차
1. [전체 시스템 아키텍처](#1-전체-시스템-아키텍처)
2. [3-Jar 모듈 구조](#2-3-jar-모듈-구조)
3. [전체 처리 흐름 시퀀스](#3-전체-처리-흐름-시퀀스)
4. [데이터 로딩 상세 시퀀스](#4-데이터-로딩-상세-시퀀스)
5. [Pipeline 기반 DataLoader 선택 시퀀스](#5-pipeline-기반-dataloader-선택-시퀀스)
6. [핵심 컴포넌트 클래스 다이어그램](#6-핵심-컴포넌트-클래스-다이어그램)
7. [Strategy 패턴 클래스 다이어그램](#7-strategy-패턴-클래스-다이어그램)
8. [Step 계층 구조](#8-step-계층-구조)
9. [DataLoader 계층 구조](#9-dataloader-계층-구조)
10. [Domain Model 구조](#10-domain-model-구조)
11. [Pipeline 구성 및 실행](#11-pipeline-구성-및-실행)
12. [캐시 아키텍처](#12-캐시-아키텍처)
13. [예외 계층 구조](#13-예외-계층-구조)

---

## 1. 전체 시스템 아키텍처

```mermaid
graph TB
    subgraph "External Callers"
        BATCH[Spring Batch<br/>정기청구]
        OLTP[OLTP Services<br/>실시간/예상/견적 조회]
    end

    subgraph "billing-charge-calculation-api (인터페이스 jar)"
        API_IF[ChargeCalculationService<br/>인터페이스]
        DTO_IF[Request/Response DTO]
        ENUM_IF[Enums<br/>UseCaseType, ProductType]
        EX_IF[Exceptions]
    end

    subgraph "billing-charge-calculation-impl (구현체 jar)"
        API_IMPL[ChargeCalculationServiceImpl]
        PIPELINE_ENGINE[PipelineEngine]
        PIPELINE_CONFIG[PipelineConfigurator]
        CACHE[ReferenceDataCache]
        ORCHESTRATOR[DataLoadOrchestrator]
        REGISTRY[DataLoaderRegistry]
        RESOLVER[DataAccessStrategyResolver]
    end

    subgraph "billing-charge-calculation-internal (내부 jar)"
        STEPS[ChargeItemSteps<br/>MonthlyFee, OneTimeFee, etc]
        STRATEGIES[DataAccessStrategies<br/>RegularBilling, Realtime, etc]
        LOADERS[DataLoaders<br/>ChargeItem, OneTime, Usage]
        MAPPERS[MyBatis Mappers]
        MODELS[Domain Models]
    end

    subgraph "Database"
        MASTER_DB[(마스터 테이블<br/>계약정보)]
        ORDER_DB[(접수 테이블<br/>예상 계약)]
        REF_DB[(기준정보 테이블<br/>요금/할인 정책)]
        RESULT_DB[(계산결과 테이블)]
    end

    BATCH --> API_IF
    OLTP --> API_IF
    API_IF --> API_IMPL
    API_IMPL --> RESOLVER
    API_IMPL --> PIPELINE_CONFIG
    API_IMPL --> ORCHESTRATOR
    PIPELINE_CONFIG --> PIPELINE_ENGINE
    ORCHESTRATOR --> REGISTRY
    ORCHESTRATOR --> LOADERS
    PIPELINE_ENGINE --> STEPS
    STEPS --> CACHE
    STEPS --> STRATEGIES
    STRATEGIES --> LOADERS
    LOADERS --> MAPPERS
    MAPPERS --> MASTER_DB
    MAPPERS --> ORDER_DB
    MAPPERS --> RESULT_DB
    CACHE --> REF_DB
```

---

## 2. 3-Jar 모듈 구조

```mermaid
graph LR
    subgraph "billing-charge-calculation-api.jar"
        direction TB
        A1[ChargeCalculationService]
        A2[ChargeCalculationRequest]
        A3[ChargeCalculationResponse]
        A4[ContractInfo]
        A5[ContractChargeResult]
        A6[PeriodChargeResult]
        A7[FlatChargeResult]
        A8[Enums]
        A9[Exceptions]
    end

    subgraph "billing-charge-calculation-impl.jar"
        direction TB
        B1[ChargeCalculationServiceImpl]
        B2[PipelineEngine]
        B3[PipelineConfigurator]
        B4[ReferenceDataCacheManager]
        B5[DataLoadOrchestrator]
        B6[DataLoaderRegistry]
        B7[DataAccessStrategyResolver]
        B8[BatchChargeCalculationProcessor]
    end

    subgraph "billing-charge-calculation-internal.jar"
        direction TB
        C1[ChargeItemSteps]
        C2[DataAccessStrategies]
        C3[DataLoaders]
        C4[Domain Models]
        C5[MyBatis Mappers]
        C6[Utilities]
    end

    A1 -.->|구현| B1
    B1 --> B2
    B1 --> B5
    B1 --> B7
    B2 --> B3
    B5 --> B6
    B2 --> C1
    B5 --> C3
    B7 --> C2
    C1 --> C4
    C2 --> C3
    C3 --> C5
    B2 --> B4
```

---

## 3. 전체 처리 흐름 시퀀스

```mermaid
sequenceDiagram
    participant Caller as 호출자<br/>(Batch/OLTP)
    participant Service as ChargeCalculationServiceImpl
    participant Resolver as StrategyResolver
    participant Configurator as PipelineConfigurator
    participant Orchestrator as DataLoadOrchestrator
    participant Registry as DataLoaderRegistry
    participant Engine as PipelineEngine
    participant Step as ChargeItemStep
    participant Strategy as DataAccessStrategy
    participant Cache as ReferenceDataCache

    Caller->>Service: calculate(request)

    Note over Service: 1. 유효성 검증
    Service->>Service: validate(request)

    Note over Service: 2. Strategy 결정
    Service->>Resolver: resolve(useCaseType)
    Resolver-->>Service: DataAccessStrategy

    Note over Service: 3. Pipeline 구성
    Service->>Configurator: configure(tenantId, productType, useCaseType)
    Configurator-->>Service: Pipeline

    Note over Service: 4. 데이터 일괄 로딩
    Service->>Orchestrator: loadAll(baseLoader, itemLoaders, contractIds, pipeline)
    Orchestrator->>Registry: getRequiredDataLoaders(pipeline)
    Registry-->>Orchestrator: RequiredDataLoaders

    loop 필요한 DataLoader만 선택적 호출
        Orchestrator->>Orchestrator: loadData()
    end
    Orchestrator-->>Service: Map<contractId, ChargeInput>

    Note over Service: 5. 계약건별 Pipeline 실행
    loop 각 계약
        Service->>Engine: execute(pipeline, context, strategy)

        loop 각 Step (순서대로)
            Engine->>Step: process(context)
            Step->>Cache: getReferenceData()
            Cache-->>Step: 기준정보
            Step->>Step: 요금 계산
            Step-->>Engine: context 업데이트

            opt requiresStatusUpdate() == true
                Engine->>Strategy: updateProcessingStatus()
            end
        end

        Engine-->>Service: ChargeContext
        Service->>Strategy: writeChargeResult(result)
    end

    Service-->>Caller: ChargeCalculationResponse
```

---

## 4. 데이터 로딩 상세 시퀀스

```mermaid
sequenceDiagram
    participant Service as ChargeCalculationServiceImpl
    participant Orchestrator as DataLoadOrchestrator
    participant Registry as DataLoaderRegistry
    participant BaseLoader as ContractBaseLoader
    participant ItemLoader as ChargeItemDataLoader
    participant OneTimeLoader as OneTimeChargeDataLoader
    participant UsageLoader as UsageChargeDataLoader
    participant Mapper as MyBatis Mapper

    Service->>Orchestrator: loadAll(baseLoader, itemLoaders, contractIds, pipeline)

    Note over Orchestrator: 1. 계약 기본정보 조회 (chunk 단위)
    Orchestrator->>BaseLoader: loadBaseContracts(contractIds)
    BaseLoader->>Mapper: selectBaseContracts (IN절 최대 1000건)
    Mapper-->>BaseLoader: List<ContractInfo>
    BaseLoader-->>Orchestrator: List<ContractInfo>

    Note over Orchestrator: 2. ChargeInput 초기화
    Orchestrator->>Orchestrator: Map<contractId, ChargeInput> 생성

    Note over Orchestrator: 3. Pipeline 기반 필요 DataLoader 수집
    Orchestrator->>Registry: getRequiredDataLoaders(pipeline)

    alt 캐시 히트
        Registry-->>Orchestrator: RequiredDataLoaders (캐시)
    else 캐시 미스
        loop 각 Step
            Registry->>Registry: step.getRequiredChargeItemTypes()
            Registry->>Registry: step.getRequiredOneTimeChargeDomains()
            Registry->>Registry: step.getRequiredUsageChargeDomains()
        end
        Registry->>Registry: 중복 제거 및 캐싱
        Registry-->>Orchestrator: RequiredDataLoaders
    end

    Note over Orchestrator: 4. ChargeItemDataLoader 순차 호출
    loop 각 ChargeItemDataLoader
        Orchestrator->>ItemLoader: loadAndPopulate(contracts, chargeInputMap)
        ItemLoader->>Mapper: SELECT (chunk 단위)
        Mapper-->>ItemLoader: 데이터
        ItemLoader->>ItemLoader: ChargeInput에 설정
    end

    Note over Orchestrator: 5. OneTimeChargeDataLoader 선택적 호출
    loop 필요한 OneTimeChargeDataLoader만
        Orchestrator->>OneTimeLoader: loadData(contracts)
        OneTimeLoader->>Mapper: SELECT (chunk 단위)
        Mapper-->>OneTimeLoader: Map<contractId, List<Domain>>
        OneTimeLoader-->>Orchestrator: 데이터
        Orchestrator->>Orchestrator: putOneTimeChargeData()
    end

    Note over Orchestrator: 6. UsageChargeDataLoader 선택적 호출
    loop 필요한 UsageChargeDataLoader만
        Orchestrator->>UsageLoader: loadData(contracts)
        UsageLoader->>Mapper: SELECT (chunk 단위)
        Mapper-->>UsageLoader: Map<contractId, List<Domain>>
        UsageLoader-->>Orchestrator: 데이터
        Orchestrator->>Orchestrator: putUsageChargeData()
    end

    Orchestrator-->>Service: Map<contractId, ChargeInput>
```

---

## 5. Pipeline 기반 DataLoader 선택 시퀀스

```mermaid
sequenceDiagram
    participant Orchestrator as DataLoadOrchestrator
    participant Registry as DataLoaderRegistry
    participant Pipeline
    participant Step as ChargeItemStep
    participant Cache as LRU Cache

    Orchestrator->>Registry: getRequiredDataLoaders(pipeline)

    Registry->>Pipeline: getPipelineId()
    Pipeline-->>Registry: pipelineId

    Registry->>Cache: cache.get(pipelineId)

    alt 캐시 히트
        Cache-->>Registry: RequiredDataLoaders
        Note over Registry: Step 순회 생략
    else 캐시 미스
        Note over Registry: Pipeline의 모든 Step 순회

        loop 각 Step
            Registry->>Step: getRequiredChargeItemTypes()
            Step-->>Registry: List<ChargeItemType>

            Registry->>Step: getRequiredOneTimeChargeDomains()
            Step-->>Registry: List<Class<OneTimeChargeDomain>>

            Registry->>Step: getRequiredUsageChargeDomains()
            Step-->>Registry: List<Class<UsageChargeDomain>>
        end

        Note over Registry: 중복 제거
        Registry->>Registry: Set으로 중복 제거

        Note over Registry: Spring bean에서 매칭
        Registry->>Registry: allChargeItemDataLoaders.stream()<br/>.filter(매칭)
        Registry->>Registry: allOneTimeChargeDataLoaders.stream()<br/>.filter(매칭)
        Registry->>Registry: allUsageChargeDataLoaders.stream()<br/>.filter(매칭)

        Registry->>Registry: new RequiredDataLoaders(...)

        Note over Registry: 캐싱 (LRU)
        Registry->>Cache: cache.put(pipelineId, required)
    end

    Registry-->>Orchestrator: RequiredDataLoaders
```

---

## 6. 핵심 컴포넌트 클래스 다이어그램

```mermaid
classDiagram
    class ChargeCalculationService {
        <<interface>>
        +calculate(ChargeCalculationRequest) ChargeCalculationResponse
    }

    class ChargeCalculationServiceImpl {
        -pipelineConfigurator: PipelineConfigurator
        -pipelineEngine: PipelineEngine
        -strategyResolver: DataAccessStrategyResolver
        -dataLoadOrchestrator: DataLoadOrchestrator
        +calculate(request) ChargeCalculationResponse
        -validate(request) void
    }

    class PipelineConfigurator {
        <<interface>>
        +configure(tenantId, productType, useCaseType) Pipeline
    }

    class DefaultPipelineConfigurator {
        -pipelineConfigMapper: PipelineConfigMapper
        -stepRegistry: Map~String,ChargeItemStep~
        +configure(tenantId, productType, useCaseType) Pipeline
        -loadPipelineConfig(...) PipelineConfigDto
        -buildPipeline(config) Pipeline
    }

    class PipelineEngine {
        -strategyResolver: DataAccessStrategyResolver
        +execute(pipeline, context, strategy) void
    }

    class Pipeline {
        -pipelineId: String
        -steps: List~ChargeItemStep~
        +getPipelineId() String
        +getSteps() List~ChargeItemStep~
    }

    class DataLoadOrchestrator {
        -dataLoaderRegistry: DataLoaderRegistry
        -MAX_CHUNK_SIZE: int
        +loadAll(baseLoader, itemLoaders, contractIds, pipeline) Map~String,ChargeInput~
        -loadBaseContractsInChunks(...) List~ContractInfo~
        -populateOneTimeChargeData(...) void
        -populateUsageChargeData(...) void
    }

    class DataLoaderRegistry {
        -allChargeItemDataLoaders: List
        -allOneTimeChargeDataLoaders: List
        -allUsageChargeDataLoaders: List
        -cache: Map~String,RequiredDataLoaders~
        -MAX_CACHE_SIZE: int
        +getRequiredDataLoaders(pipeline) RequiredDataLoaders
        -collectRequiredDataLoaders(pipeline) RequiredDataLoaders
    }

    class RequiredDataLoaders {
        <<record>>
        +chargeItemDataLoaders: List~ChargeItemDataLoader~
        +oneTimeChargeDataLoaders: List~OneTimeChargeDataLoader~
        +usageChargeDataLoaders: List~UsageChargeDataLoader~
    }

    class DataAccessStrategyResolver {
        -strategies: List~DataAccessStrategy~
        +resolve(useCaseType) DataAccessStrategy
    }

    class ChargeContext {
        -tenantId: String
        -contractInfo: ContractInfo
        -chargeInput: ChargeInput
        -periodResults: List~PeriodChargeResult~
        -flatResults: List~FlatChargeResult~
        +addPeriodResult(result) void
        +addFlatResult(result) void
        +compactPeriodToFlat() void
        +toChargeResult() ChargeResult
    }

    ChargeCalculationService <|.. ChargeCalculationServiceImpl
    PipelineConfigurator <|.. DefaultPipelineConfigurator
    ChargeCalculationServiceImpl --> PipelineConfigurator
    ChargeCalculationServiceImpl --> PipelineEngine
    ChargeCalculationServiceImpl --> DataLoadOrchestrator
    ChargeCalculationServiceImpl --> DataAccessStrategyResolver
    PipelineEngine --> Pipeline
    Pipeline --> ChargeItemStep
    DataLoadOrchestrator --> DataLoaderRegistry
    DataLoaderRegistry --> RequiredDataLoaders
    PipelineEngine --> ChargeContext
```

---

## 7. Strategy 패턴 클래스 다이어그램

```mermaid
classDiagram
    class DataAccessStrategy {
        <<interface>>
        +supportedUseCaseType() UseCaseType
        +readChargeInput(contractInfo) ChargeInput
        +writeChargeResult(result) void
        +updateProcessingStatus(chargeItemId, status) void
        +getContractBaseLoader() ContractBaseLoader
        +getChargeItemDataLoaders() List~ChargeItemDataLoader~
    }

    class RegularBillingStrategy {
        -masterTableMapper: MasterTableMapper
        -resultMapper: ChargeResultMapper
        -masterContractBaseLoader: MasterContractBaseLoader
        -monthlyFeeDataLoader: MonthlyFeeDataLoader
        -discountDataLoader: DiscountDataLoader
        -billingPaymentDataLoader: BillingPaymentDataLoader
        -prepaidDataLoader: PrepaidDataLoader
        +supportedUseCaseType() UseCaseType
        +readChargeInput(contractInfo) ChargeInput
        +writeChargeResult(result) void
        +updateProcessingStatus(...) void
        +getContractBaseLoader() ContractBaseLoader
        +getChargeItemDataLoaders() List
    }

    class RealtimeQueryStrategy {
        -masterTableMapper: MasterTableMapper
        -masterContractBaseLoader: MasterContractBaseLoader
        -monthlyFeeDataLoader: MonthlyFeeDataLoader
        +supportedUseCaseType() UseCaseType
        +readChargeInput(contractInfo) ChargeInput
        +writeChargeResult(result) void
        +updateProcessingStatus(...) void
    }

    class EstimateQueryStrategy {
        -orderTableMapper: OrderTableMapper
        -orderContractBaseLoader: OrderContractBaseLoader
        -monthlyFeeDataLoader: MonthlyFeeDataLoader
        +supportedUseCaseType() UseCaseType
        +readChargeInput(contractInfo) ChargeInput
        +writeChargeResult(result) void
    }

    class QuotationQueryStrategy {
        -referenceDataCache: ReferenceDataCache
        -quotationContractBaseLoader: QuotationContractBaseLoader
        +supportedUseCaseType() UseCaseType
        +readChargeInput(contractInfo) ChargeInput
        +writeChargeResult(result) void
    }

    DataAccessStrategy <|.. RegularBillingStrategy
    DataAccessStrategy <|.. RealtimeQueryStrategy
    DataAccessStrategy <|.. EstimateQueryStrategy
    DataAccessStrategy <|.. QuotationQueryStrategy
```

---

## 8. Step 계층 구조

```mermaid
classDiagram
    class ChargeItemStep {
        <<interface>>
        +getStepId() String
        +getOrder() int
        +process(ChargeContext) void
        +requiresStatusUpdate() boolean
        +getRequiredChargeItemTypes() List~ChargeItemType~
        +getRequiredOneTimeChargeDomains() List~Class~
        +getRequiredUsageChargeDomains() List~Class~
    }

    class MonthlyFeeStep {
        -STEP_ID: String
        -ORDER: int
        +getRequiredChargeItemTypes() List~ChargeItemType~
        +process(context) void
        -calculateMonthlyFee(...) void
        -intersectPeriods(...) List~IntersectedPeriod~
    }

    class OneTimeFeeStep {
        -STEP_ID: String
        -ORDER: int
        -oneTimeChargeDataLoaders: List
        +getRequiredOneTimeChargeDomains() List~Class~
        +process(context) void
    }

    class UsageFeeStep {
        -STEP_ID: String
        -ORDER: int
        -usageChargeDataLoaders: List
        +getRequiredUsageChargeDomains() List~Class~
        +process(context) void
    }

    class PeriodDiscountStep {
        -STEP_ID: String
        -ORDER: int
        +getRequiredChargeItemTypes() List~ChargeItemType~
        +process(context) void
        -applyDiscount(...) void
    }

    class PeriodToFlatCompactionStep {
        -STEP_ID: String
        -ORDER: int
        +process(context) void
    }

    class FlatDiscountStep {
        -STEP_ID: String
        -ORDER: int
        +getRequiredChargeItemTypes() List~ChargeItemType~
        +process(context) void
    }

    class LateFeeStep {
        -STEP_ID: String
        -ORDER: int
        +process(context) void
    }

    class AutoPayDiscountStep {
        -STEP_ID: String
        -ORDER: int
        +process(context) void
    }

    class VatStep {
        -STEP_ID: String
        -ORDER: int
        +process(context) void
    }

    class PrepaidOffsetStep {
        -STEP_ID: String
        -ORDER: int
        +getRequiredChargeItemTypes() List~ChargeItemType~
        +process(context) void
    }

    class SplitBillingStep {
        -STEP_ID: String
        -ORDER: int
        +process(context) void
    }

    ChargeItemStep <|.. MonthlyFeeStep
    ChargeItemStep <|.. OneTimeFeeStep
    ChargeItemStep <|.. UsageFeeStep
    ChargeItemStep <|.. PeriodDiscountStep
    ChargeItemStep <|.. PeriodToFlatCompactionStep
    ChargeItemStep <|.. FlatDiscountStep
    ChargeItemStep <|.. LateFeeStep
    ChargeItemStep <|.. AutoPayDiscountStep
    ChargeItemStep <|.. VatStep
    ChargeItemStep <|.. PrepaidOffsetStep
    ChargeItemStep <|.. SplitBillingStep
```

---

## 9. DataLoader 계층 구조

```mermaid
classDiagram
    class ContractBaseLoader {
        <<interface>>
        +loadBaseContracts(contractIds) List~ContractInfo~
    }

    class MasterContractBaseLoader {
        -contractBaseMapper: ContractBaseMapper
        -MAX_CHUNK_SIZE: int
        +loadBaseContracts(contractIds) List~ContractInfo~
    }

    class OrderContractBaseLoader {
        -contractBaseMapper: ContractBaseMapper
        +loadBaseContracts(contractIds) List~ContractInfo~
    }

    class QuotationContractBaseLoader {
        +loadBaseContracts(contractIds) List~ContractInfo~
    }

    class ChargeItemDataLoader {
        <<interface>>
        +getChargeItemType() ChargeItemType
        +loadAndPopulate(contracts, chargeInputMap) void
    }

    class MonthlyFeeDataLoader {
        -monthlyFeeMapper: MonthlyFeeMapper
        +getChargeItemType() ChargeItemType
        +loadAndPopulate(contracts, chargeInputMap) void
    }

    class DiscountDataLoader {
        -discountMapper: DiscountMapper
        +getChargeItemType() ChargeItemType
        +loadAndPopulate(contracts, chargeInputMap) void
    }

    class BillingPaymentDataLoader {
        -billingPaymentMapper: BillingPaymentMapper
        +getChargeItemType() ChargeItemType
        +loadAndPopulate(contracts, chargeInputMap) void
    }

    class PrepaidDataLoader {
        -prepaidMapper: PrepaidMapper
        +getChargeItemType() ChargeItemType
        +loadAndPopulate(contracts, chargeInputMap) void
    }

    class OneTimeChargeDataLoader~T~ {
        <<interface>>
        +getDomainType() Class~T~
        +loadData(contracts) Map~String,List~T~~
    }

    class InstallmentHistoryLoader {
        -installmentHistoryMapper: InstallmentHistoryMapper
        +getDomainType() Class
        +loadData(contracts) Map
    }

    class PenaltyFeeLoader {
        -penaltyFeeMapper: PenaltyFeeMapper
        +getDomainType() Class
        +loadData(contracts) Map
    }

    class UsageChargeDataLoader~T~ {
        <<interface>>
        +getDomainType() Class~T~
        +loadData(contracts) Map~String,List~T~~
    }

    class VoiceUsageLoader {
        -voiceUsageMapper: VoiceUsageMapper
        +getDomainType() Class
        +loadData(contracts) Map
    }

    class DataUsageLoader {
        -dataUsageMapper: DataUsageMapper
        +getDomainType() Class
        +loadData(contracts) Map
    }

    ContractBaseLoader <|.. MasterContractBaseLoader
    ContractBaseLoader <|.. OrderContractBaseLoader
    ContractBaseLoader <|.. QuotationContractBaseLoader

    ChargeItemDataLoader <|.. MonthlyFeeDataLoader
    ChargeItemDataLoader <|.. DiscountDataLoader
    ChargeItemDataLoader <|.. BillingPaymentDataLoader
    ChargeItemDataLoader <|.. PrepaidDataLoader

    OneTimeChargeDataLoader <|.. InstallmentHistoryLoader
    OneTimeChargeDataLoader <|.. PenaltyFeeLoader

    UsageChargeDataLoader <|.. VoiceUsageLoader
    UsageChargeDataLoader <|.. DataUsageLoader
```

---

## 10. Domain Model 구조

```mermaid
classDiagram
    class ChargeInput {
        -subscriptionInfo: SubscriptionInfo
        -suspensionHistories: List~SuspensionHistory~
        -billingInfo: BillingInfo
        -paymentInfo: PaymentInfo
        -prepaidRecords: List~PrepaidRecord~
        -discountSubscriptions: List~DiscountSubscription~
        -oneTimeChargeDataMap: Map~Class,List~
        -usageChargeDataMap: Map~Class,List~
        +getOneTimeChargeData(type) List~T~
        +getUsageChargeData(type) List~T~
        +putOneTimeChargeData(type, data) void
        +putUsageChargeData(type, data) void
    }

    class OneTimeChargeDomain {
        <<interface>>
        +getContractId() String
    }

    class InstallmentHistory {
        -contractId: String
        -installmentId: String
        -installmentAmount: BigDecimal
        -currentInstallment: int
        -totalInstallments: int
    }

    class PenaltyFee {
        -contractId: String
        -penaltyId: String
        -penaltyAmount: BigDecimal
        -penaltyReason: String
    }

    class UsageChargeDomain {
        <<interface>>
        +getContractId() String
    }

    class VoiceUsage {
        -contractId: String
        -usageId: String
        -duration: BigDecimal
        -unitPrice: BigDecimal
    }

    class DataUsage {
        -contractId: String
        -usageId: String
        -dataVolume: BigDecimal
        -unitPrice: BigDecimal
    }

    class SubscriptionInfo {
        -contractId: String
        -productId: String
        -subscriptionDate: LocalDate
        -monthlyFee: BigDecimal
    }

    class SuspensionHistory {
        -contractId: String
        -suspensionStartDate: LocalDate
        -suspensionEndDate: LocalDate
        -suspensionType: String
    }

    class DiscountSubscription {
        -contractId: String
        -discountId: String
        -discountCode: String
        -discountName: String
        -startDate: LocalDate
        -endDate: LocalDate
    }

    class PeriodChargeResult {
        <<record>>
        -chargeItemCode: String
        -chargeItemName: String
        -chargeItemType: ChargeItemType
        -amount: BigDecimal
        -periodFrom: LocalDate
        -periodTo: LocalDate
        -currencyCode: String
        -metadata: Map
    }

    class FlatChargeResult {
        <<record>>
        -chargeItemCode: String
        -chargeItemName: String
        -chargeItemType: ChargeItemType
        -amount: BigDecimal
        -currencyCode: String
        -metadata: Map
    }

    OneTimeChargeDomain <|.. InstallmentHistory
    OneTimeChargeDomain <|.. PenaltyFee
    UsageChargeDomain <|.. VoiceUsage
    UsageChargeDomain <|.. DataUsage

    ChargeInput --> SubscriptionInfo
    ChargeInput --> SuspensionHistory
    ChargeInput --> DiscountSubscription
    ChargeInput --> OneTimeChargeDomain
    ChargeInput --> UsageChargeDomain
```

---

## 11. Pipeline 구성 및 실행

```mermaid
graph TB
    subgraph "정기청구 - 무선상품 Pipeline"
        direction LR
        S1[1. MonthlyFeeStep<br/>월정액 계산<br/>기간존재]
        S2[2. OneTimeFeeStep<br/>일회성 요금<br/>기간미존재]
        S3[3. UsageFeeStep<br/>통화료/종량료<br/>기간미존재]
        S4[4. PeriodDiscountStep<br/>할인1<br/>기간존재]
        S5[5. PeriodToFlatCompactionStep<br/>Period→Flat 압축]
        S6[6. FlatDiscountStep<br/>할인2<br/>기간미존재]
        S7[7. LateFeeStep<br/>연체가산금]
        S8[8. AutoPayDiscountStep<br/>자동납부할인]
        S9[9. VatStep<br/>부가세]
        S10[10. PrepaidOffsetStep<br/>선납반제]
        S11[11. SplitBillingStep<br/>분리과금]

        S1 --> S2 --> S3 --> S4 --> S5 --> S6 --> S7 --> S8 --> S9 --> S10 --> S11
    end

    subgraph "견적 조회 - 구독상품 Pipeline (간소화)"
        direction LR
        Q1[1. MonthlyFeeStep<br/>월정액]
        Q2[2. PeriodDiscountStep<br/>할인1]
        Q3[3. PeriodToFlatCompactionStep<br/>압축]
        Q4[4. FlatDiscountStep<br/>할인2]
        Q5[5. VatStep<br/>부가세]

        Q1 --> Q2 --> Q3 --> Q4 --> Q5
    end

    style S1 fill:#e1f5ff
    style S4 fill:#e1f5ff
    style S5 fill:#fff3cd
    style S2 fill:#f8f9fa
    style S3 fill:#f8f9fa
    style S6 fill:#f8f9fa
    style S7 fill:#f8f9fa
    style S8 fill:#f8f9fa
    style S9 fill:#f8f9fa
    style S10 fill:#f8f9fa
    style S11 fill:#f8f9fa
```

---

## 12. 캐시 아키텍처

```mermaid
classDiagram
    class ReferenceDataCache {
        <<interface>>
        +getReferenceData(tenantId, key, type) T
        +preload(tenantId, keys) void
        +invalidate(tenantId, key) void
        +invalidateAll(tenantId) void
    }

    class CaffeineReferenceDataCache {
        -tenantCaches: ConcurrentHashMap~String,Cache~
        -referenceDataMapper: ReferenceDataMapper
        -MAX_SIZE: int
        -EXPIRE_AFTER_WRITE: Duration
        +getReferenceData(tenantId, key, type) T
        +preload(tenantId, keys) void
        +invalidate(tenantId, key) void
        +invalidateAll(tenantId) void
        -getOrCreateCache(tenantId) Cache
    }

    class ReferenceDataKey {
        <<record>>
        -type: ReferenceDataType
        -keyValue: String
    }

    class ReferenceDataType {
        <<enumeration>>
        PRODUCT_FEE
        DISCOUNT_POLICY
        TAX_RULE
        SPLIT_BILLING_RULE
        AUTO_PAY_DISCOUNT
        SPECIAL_PRODUCT_INFO
    }

    class DataLoaderRegistry {
        -cache: Map~String,RequiredDataLoaders~
        -MAX_CACHE_SIZE: int
        +getRequiredDataLoaders(pipeline) RequiredDataLoaders
        -collectRequiredDataLoaders(pipeline) RequiredDataLoaders
    }

    ReferenceDataCache <|.. CaffeineReferenceDataCache
    CaffeineReferenceDataCache --> ReferenceDataKey
    ReferenceDataKey --> ReferenceDataType
```

---

## 13. 예외 계층 구조

```mermaid
classDiagram
    class ChargeCalculationException {
        <<abstract>>
        -errorCode: String
        -message: String
    }

    class InvalidRequestException {
        -fieldName: String
        -reason: String
    }

    class UnsupportedUseCaseException {
        -useCaseType: UseCaseType
    }

    class PipelineConfigNotFoundException {
        -tenantId: String
        -productType: ProductType
        -useCaseType: UseCaseType
    }

    class ReferenceDataNotFoundException {
        -key: ReferenceDataKey
    }

    class StepExecutionException {
        -stepId: String
        -contractId: String
        -cause: Throwable
    }

    class ProcessingStatusUpdateException {
        -stepId: String
        -contractId: String
    }

    class DataLoadException {
        -loaderType: String
        -contractIds: List~String~
        -cause: Throwable
    }

    ChargeCalculationException <|-- InvalidRequestException
    ChargeCalculationException <|-- UnsupportedUseCaseException
    ChargeCalculationException <|-- PipelineConfigNotFoundException
    ChargeCalculationException <|-- ReferenceDataNotFoundException
    ChargeCalculationException <|-- StepExecutionException
    ChargeCalculationException <|-- ProcessingStatusUpdateException
    ChargeCalculationException <|-- DataLoadException
```

---

## 추가 다이어그램

### Pipeline 구성 데이터베이스 스키마

```mermaid
erDiagram
    PIPELINE_CONFIG ||--o{ PIPELINE_STEP_CONFIG : contains
    PIPELINE_CONFIG {
        varchar pipeline_config_id PK
        varchar tenant_id
        varchar product_type
        varchar use_case_type
        varchar description
        char active_yn
    }

    PIPELINE_STEP_CONFIG {
        varchar pipeline_config_id PK,FK
        varchar step_id PK
        number step_order
        char active_yn
    }

    CHARGE_PROCESSING_STATUS {
        varchar processing_id PK
        varchar contract_id
        varchar step_id
        varchar status
        timestamp processed_at
        varchar error_message
    }
```

### Chunk 분할 처리 흐름

```mermaid
flowchart TD
    A[계약ID 리스트<br/>예: 2500건] --> B{1000건 초과?}
    B -->|No| C[단일 chunk 처리]
    B -->|Yes| D[ChunkPartitioner.partition]

    D --> E[Chunk 1<br/>1~1000건]
    D --> F[Chunk 2<br/>1001~2000건]
    D --> G[Chunk 3<br/>2001~2500건]

    E --> H[ContractBaseLoader]
    F --> I[ContractBaseLoader]
    G --> J[ContractBaseLoader]

    H --> K[SQL IN 절<br/>최대 1000개]
    I --> L[SQL IN 절<br/>최대 1000개]
    J --> M[SQL IN 절<br/>최대 500개]

    K --> N[결과 병합]
    L --> N
    M --> N

    C --> O[ChargeItemDataLoader<br/>순차 호출]
    N --> O

    O --> P[OneTimeChargeDataLoader<br/>선택적 호출]
    P --> Q[UsageChargeDataLoader<br/>선택적 호출]
    Q --> R[Map~contractId, ChargeInput~]
```

### 선분이력 교차 처리 알고리즘

```mermaid
flowchart TD
    A[가입이력, 정지이력,<br/>요금이력 입력] --> B[모든 경계 시점 수집]
    B --> C[TreeSet으로 정렬]
    C --> D[인접 경계 시점 쌍으로<br/>구간 생성]

    D --> E{각 구간마다}
    E --> F[해당 시점에 유효한<br/>이력 매핑]

    F --> G[가입상태 확인]
    F --> H[정지상태 확인]
    F --> I[요금정보 확인]

    G --> J[IntersectedPeriod 생성]
    H --> J
    I --> J

    J --> K{다음 구간?}
    K -->|Yes| E
    K -->|No| L[List~IntersectedPeriod~ 반환]

    L --> M[월정액 계산<br/>구간별 일할 계산]
```

### Period → Flat 압축 알고리즘

```mermaid
flowchart TD
    A[PeriodChargeResult 리스트<br/>예: 월정액 10개 구간] --> B[chargeItemCode로<br/>groupingBy]

    B --> C[항목별 금액 합산<br/>reducing by sum]

    C --> D{각 항목}
    D --> E[월정액_001: 30,000원]
    D --> F[할인1_001: -5,000원]
    D --> G[...]

    E --> H[FlatChargeResult 생성]
    F --> H
    G --> H

    H --> I[List~FlatChargeResult~]
    I --> J[ChargeContext에 추가]
    J --> K[periodResults.clear]
```

---

## 요약

본 문서는 다음 3개의 설계를 통합한 UML 다이어그램을 제공합니다:

1. **billing-charge-calculation**: Pipeline/Step 기반 요금 계산 프레임워크
2. **subscription-data-load-refactor**: 요금항목별 분리 조회 및 chunk 단위 일괄 로딩
3. **pipeline-based-dataloader-selection**: Pipeline 기반 필요 DataLoader 선택적 호출

### 핵심 패턴
- **Strategy Pattern**: 유스케이스별 데이터 접근 전략 (RegularBilling, Realtime, Estimate, Quotation)
- **Pipeline Pattern**: Step을 순서대로 실행하여 요금 계산 (OCP 준수)
- **3-Jar Architecture**: API/Impl/Internal 분리로 의존성 역전
- **Registry Pattern**: DataLoaderRegistry를 통한 선택적 DataLoader 호출
- **Chunk Pattern**: 최대 1000건 단위 일괄 조회로 DB round trip 최소화

### 성능 최적화
- **Selective DataLoading**: Pipeline에 포함된 Step의 DataLoader만 호출 (최대 75% DB 조회 감소)
- **LRU Cache**: Pipeline별 필요 DataLoader 목록 캐싱
- **Reference Data Cache**: Caffeine 기반 테넌트별 분리 캐시
- **Chunk Partitioning**: Oracle IN 절 제한(1000개) 준수

### 확장성
- 새로운 요금항목 추가 시 ChargeItemStep 구현체만 추가
- 새로운 일회성/사용량 요금 추가 시 DataLoader 구현체만 추가
- 기존 코드 변경 없이 새로운 유스케이스 추가 가능 (OCP)

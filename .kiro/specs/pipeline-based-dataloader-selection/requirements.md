# 요구사항 문서

## 소개

본 문서는 유무선 통신 billing system의 요금 계산 모듈에서 DataLoader 선택 로직을 개선하기 위한 요구사항을 정의한다. 현재 시스템은 Spring 컨텍스트에 등록된 모든 DataLoader bean을 무조건 로드하고 있다. 이로 인해 Pipeline에 포함되지 않은 Step의 데이터까지 불필요하게 조회하여 성능 저하와 리소스 낭비가 발생한다. 본 개선은 Pipeline 구성에 따라 실제로 사용될 Step에서 필요한 DataLoader만 선택적으로 로드하도록 변경한다.

## 용어 정의

- **Pipeline**: 테넌트ID, 상품유형, 유스케이스에 따라 구성된 요금 계산 흐름. 여러 Step으로 구성된다.
- **Step**: Pipeline 내에서 개별 요금 항목 계산을 수행하는 단위 처리 모듈. ChargeItemStep 인터페이스를 구현한다.
- **DataLoader**: 요금 계산에 필요한 데이터를 chunk 단위로 조회하는 컴포넌트. ChargeItemDataLoader, OneTimeChargeDataLoader, UsageChargeDataLoader 등이 있다.
- **DataLoadOrchestrator**: 요금항목별 분리 조회를 오케스트레이션하는 핵심 컴포넌트. 파이프라인 실행 전 모든 데이터 로딩을 일괄 수행한다.
- **ChargeItemDataLoader**: 요금항목별 데이터 로더 인터페이스. 월정액, 할인, 청구/수납 등의 데이터를 조회한다.
- **OneTimeChargeDataLoader**: 일회성 요금 유형별 제네릭 데이터 로더. 할부이력, 위약금 등 다양한 일회성 요금 데이터를 조회한다.
- **UsageChargeDataLoader**: 통화료/종량료 유형별 제네릭 데이터 로더. 음성통화, 데이터 사용량 등을 조회한다.
- **DataLoaderRegistry**: Step이 필요로 하는 DataLoader를 선언하고 조회할 수 있는 레지스트리 인터페이스.
- **PipelineConfigurator**: 테넌트ID, 상품유형, 유스케이스에 따라 Pipeline을 구성하는 컴포넌트.
- **ChargeContext**: 요금 계산 과정에서 입력 데이터와 중간 계산 결과를 담는 컨텍스트 객체.
- **ChargeInput**: 다양한 원천에서 조회한 데이터를 통합하는 요금 계산 입력 모델.

## 요구사항

### 요구사항 1: Step의 필요 DataLoader 선언

**사용자 스토리:** 요금 계산 담당 개발자로서, 각 Step이 자신이 필요로 하는 DataLoader를 명시적으로 선언하고 싶다. 이를 통해 Pipeline 구성 시 해당 Step에 필요한 DataLoader만 식별할 수 있다.

#### 인수 조건

1. THE ChargeItemStep SHALL 자신이 필요로 하는 ChargeItemDataLoader 유형 목록을 반환하는 메서드를 제공한다.
2. THE ChargeItemStep SHALL 자신이 필요로 하는 OneTimeChargeDomain 유형 목록을 반환하는 메서드를 제공한다.
3. THE ChargeItemStep SHALL 자신이 필요로 하는 UsageChargeDomain 유형 목록을 반환하는 메서드를 제공한다.
4. WHEN Step이 특정 유형의 DataLoader를 필요로 하지 않으면, THE ChargeItemStep SHALL 해당 메서드에서 빈 리스트를 반환한다.
5. THE ChargeItemStep SHALL 기본 구현으로 모든 DataLoader 선언 메서드가 빈 리스트를 반환하도록 한다.

### 요구사항 2: Pipeline 기반 DataLoader 수집

**사용자 스토리:** 요금 계산 담당 개발자로서, Pipeline에 포함된 모든 Step이 필요로 하는 DataLoader를 자동으로 수집하고 싶다. 이를 통해 불필요한 DataLoader 호출을 제거할 수 있다.

#### 인수 조건

1. WHEN Pipeline이 구성되면, THE DataLoaderRegistry SHALL Pipeline에 포함된 모든 Step을 순회하며 필요한 DataLoader 유형을 수집한다.
2. THE DataLoaderRegistry SHALL 수집된 ChargeItemDataLoader 유형 목록에서 중복을 제거한다.
3. THE DataLoaderRegistry SHALL 수집된 OneTimeChargeDomain 유형 목록에서 중복을 제거한다.
4. THE DataLoaderRegistry SHALL 수집된 UsageChargeDomain 유형 목록에서 중복을 제거한다.
5. WHEN 여러 Step이 동일한 DataLoader를 필요로 하면, THE DataLoaderRegistry SHALL 해당 DataLoader를 한 번만 포함한다.

### 요구사항 3: DataLoader 인스턴스 조회

**사용자 스토리:** 요금 계산 담당 개발자로서, 수집된 DataLoader 유형에 해당하는 실제 bean 인스턴스를 조회하고 싶다. 이를 통해 DataLoadOrchestrator가 필요한 DataLoader만 호출할 수 있다.

#### 인수 조건

1. THE DataLoaderRegistry SHALL Spring 컨텍스트에서 ChargeItemDataLoader bean 목록을 주입받는다.
2. THE DataLoaderRegistry SHALL Spring 컨텍스트에서 OneTimeChargeDataLoader bean 목록을 주입받는다.
3. THE DataLoaderRegistry SHALL Spring 컨텍스트에서 UsageChargeDataLoader bean 목록을 주입받는다.
4. WHEN ChargeItemType 목록이 주어지면, THE DataLoaderRegistry SHALL 해당 유형에 매칭되는 ChargeItemDataLoader 인스턴스 목록을 반환한다.
5. WHEN OneTimeChargeDomain Class 목록이 주어지면, THE DataLoaderRegistry SHALL 해당 도메인 유형에 매칭되는 OneTimeChargeDataLoader 인스턴스 목록을 반환한다.
6. WHEN UsageChargeDomain Class 목록이 주어지면, THE DataLoaderRegistry SHALL 해당 도메인 유형에 매칭되는 UsageChargeDataLoader 인스턴스 목록을 반환한다.
7. IF 요청된 유형에 매칭되는 DataLoader bean이 존재하지 않으면, THEN THE DataLoaderRegistry SHALL 해당 유형을 무시하고 나머지 유형만 반환한다.

### 요구사항 4: DataLoadOrchestrator 선택적 로딩

**사용자 스토리:** 요금 계산 담당 개발자로서, DataLoadOrchestrator가 Pipeline에 필요한 DataLoader만 호출하도록 하고 싶다. 이를 통해 불필요한 DB 조회를 제거하고 성능을 향상시킬 수 있다.

#### 인수 조건

1. THE DataLoadOrchestrator SHALL 모든 DataLoader bean을 자동 주입받는 대신, DataLoaderRegistry를 통해 필요한 DataLoader만 조회한다.
2. WHEN Pipeline이 전달되면, THE DataLoadOrchestrator SHALL DataLoaderRegistry를 사용하여 해당 Pipeline에 필요한 DataLoader 목록을 획득한다.
3. THE DataLoadOrchestrator SHALL 획득한 ChargeItemDataLoader 목록만 순차 호출한다.
4. THE DataLoadOrchestrator SHALL 획득한 OneTimeChargeDataLoader 목록만 순차 호출한다.
5. THE DataLoadOrchestrator SHALL 획득한 UsageChargeDataLoader 목록만 순차 호출한다.
6. WHEN Pipeline에 포함되지 않은 Step의 DataLoader는, THE DataLoadOrchestrator SHALL 호출하지 않는다.

### 요구사항 5: ChargeCalculationServiceImpl 연동

**사용자 스토리:** 요금 계산 담당 개발자로서, ChargeCalculationServiceImpl이 Pipeline 정보를 DataLoadOrchestrator에 전달하도록 하고 싶다. 이를 통해 Pipeline 기반 선택적 로딩이 실제로 동작하도록 한다.

#### 인수 조건

1. THE ChargeCalculationServiceImpl SHALL Pipeline 구성 후 DataLoadOrchestrator 호출 시 Pipeline 객체를 전달한다.
2. THE DataLoadOrchestrator SHALL Pipeline 객체를 받아 DataLoaderRegistry를 통해 필요한 DataLoader를 식별한다.
3. THE ChargeCalculationServiceImpl SHALL 기존 loadAll 메서드 시그니처를 유지하되, 내부적으로 Pipeline 정보를 활용한다.
4. WHEN 동일한 Pipeline이 반복 실행되면, THE DataLoaderRegistry SHALL 필요한 DataLoader 목록을 캐싱하여 재사용한다.

### 요구사항 6: 기존 Step 구현체 DataLoader 선언 추가

**사용자 스토리:** 요금 계산 담당 개발자로서, 기존 Step 구현체들이 자신이 필요로 하는 DataLoader를 선언하도록 하고 싶다. 이를 통해 모든 Step이 선택적 로딩 메커니즘을 활용할 수 있다.

#### 인수 조건

1. THE MonthlyFeeStep SHALL ChargeItemType.MONTHLY_FEE를 필요 DataLoader로 선언한다.
2. THE OneTimeFeeStep SHALL 등록된 모든 OneTimeChargeDomain 유형을 필요 DataLoader로 선언한다.
3. THE UsageFeeStep SHALL 등록된 모든 UsageChargeDomain 유형을 필요 DataLoader로 선언한다.
4. THE PeriodDiscountStep SHALL ChargeItemType.DISCOUNT를 필요 DataLoader로 선언한다.
5. THE FlatDiscountStep SHALL ChargeItemType.DISCOUNT를 필요 DataLoader로 선언한다.
6. THE LateFeeStep SHALL ChargeItemType.LATE_FEE를 필요 DataLoader로 선언한다.
7. THE AutoPayDiscountStep SHALL ChargeItemType.LATE_FEE를 필요 DataLoader로 선언한다.
8. THE PrepaidOffsetStep SHALL ChargeItemType.PREPAID를 필요 DataLoader로 선언한다.
9. THE TaxStep SHALL 필요 DataLoader 없음을 선언한다 (기존 계산 결과만 사용).
10. THE SeparateBillingStep SHALL 필요 DataLoader 없음을 선언한다 (기존 계산 결과만 사용).

### 요구사항 7: 하위 호환성 보장

**사용자 스토리:** 요금 계산 담당 개발자로서, 기존 코드가 변경 없이 동작하도록 하위 호환성을 보장하고 싶다. 이를 통해 점진적 마이그레이션이 가능하다.

#### 인수 조건

1. THE ChargeItemStep SHALL 기본 구현으로 빈 리스트를 반환하여, DataLoader 선언을 하지 않은 Step도 정상 동작한다.
2. WHEN Step이 필요 DataLoader를 선언하지 않으면, THE DataLoaderRegistry SHALL 해당 Step을 무시하고 다른 Step의 선언만 수집한다.
3. THE DataLoadOrchestrator SHALL 기존 loadAll 메서드 시그니처를 유지한다.
4. THE ChargeInput SHALL 기존 필드와 메서드를 모두 유지한다.

### 요구사항 8: 성능 최적화 - DataLoader 목록 캐싱

**사용자 스토리:** 요금 계산 담당 개발자로서, 동일한 Pipeline에 대해 필요한 DataLoader 목록을 반복 계산하지 않고 캐싱하고 싶다. 이를 통해 정기청구 배치 처리 시 성능을 향상시킬 수 있다.

#### 인수 조건

1. THE DataLoaderRegistry SHALL Pipeline ID를 키로 하여 필요한 DataLoader 목록을 캐싱한다.
2. WHEN 동일한 Pipeline ID로 DataLoader 목록이 요청되면, THE DataLoaderRegistry SHALL 캐시된 목록을 반환한다.
3. THE DataLoaderRegistry SHALL 캐시 크기를 제한하여 메모리 사용량을 관리한다 (최대 100개 Pipeline).
4. WHEN 캐시가 가득 차면, THE DataLoaderRegistry SHALL LRU(Least Recently Used) 정책으로 오래된 항목을 제거한다.

### 요구사항 9: 로깅 및 모니터링

**사용자 스토리:** 요금 계산 담당 개발자로서, 어떤 DataLoader가 실제로 호출되었는지 로그로 확인하고 싶다. 이를 통해 선택적 로딩이 올바르게 동작하는지 검증할 수 있다.

#### 인수 조건

1. THE DataLoaderRegistry SHALL Pipeline에서 수집된 필요 DataLoader 유형 목록을 DEBUG 레벨로 로깅한다.
2. THE DataLoadOrchestrator SHALL 실제로 호출된 각 DataLoader의 유형과 조회 건수를 DEBUG 레벨로 로깅한다.
3. THE DataLoadOrchestrator SHALL 호출되지 않은 DataLoader 유형을 DEBUG 레벨로 로깅한다.
4. WHEN DataLoader 조회 결과가 비어있으면, THE DataLoadOrchestrator SHALL 해당 정보를 DEBUG 레벨로 로깅한다.

### 요구사항 10: OneTimeFeeStep과 UsageFeeStep의 동적 DataLoader 선언

**사용자 스토리:** 요금 계산 담당 개발자로서, OneTimeFeeStep과 UsageFeeStep이 Spring 컨텍스트에 등록된 모든 해당 유형의 DataLoader를 자동으로 선언하도록 하고 싶다. 이를 통해 새로운 일회성 요금이나 종량료가 추가되어도 Step 코드를 수정하지 않을 수 있다.

#### 인수 조건

1. THE OneTimeFeeStep SHALL Spring 컨텍스트에 등록된 모든 OneTimeChargeDataLoader bean을 주입받는다.
2. THE OneTimeFeeStep SHALL 주입받은 OneTimeChargeDataLoader 목록에서 도메인 유형을 추출하여 필요 DataLoader로 선언한다.
3. THE UsageFeeStep SHALL Spring 컨텍스트에 등록된 모든 UsageChargeDataLoader bean을 주입받는다.
4. THE UsageFeeStep SHALL 주입받은 UsageChargeDataLoader 목록에서 도메인 유형을 추출하여 필요 DataLoader로 선언한다.
5. WHEN 새로운 OneTimeChargeDataLoader bean이 추가되면, THE OneTimeFeeStep SHALL 자동으로 해당 유형을 필요 DataLoader에 포함한다.
6. WHEN 새로운 UsageChargeDataLoader bean이 추가되면, THE UsageFeeStep SHALL 자동으로 해당 유형을 필요 DataLoader에 포함한다.

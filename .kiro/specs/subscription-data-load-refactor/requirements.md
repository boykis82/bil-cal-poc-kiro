# 요구사항 문서

## 소개

본 문서는 유무선 통신 billing system의 요금 계산 모듈에서 가입자 데이터 로딩(subscription data load) 로직을 개선하기 위한 요구사항을 정의한다. 기존 legacy system에서는 요금 계산에 필요한 모든 데이터를 하나의 SQL에서 수십 개의 테이블을 outer join으로 연결하여 조회하고 있다. 이로 인해 SQL 복잡도가 높고, record 중복 추출 문제가 발생하며, I/O 비효율이 존재한다. 본 개선은 데이터 로딩을 요금항목별로 분리하고, chunk 단위 일괄 조회 방식을 도입하여 이러한 문제를 해결한다.

## 용어 정의

- **Data_Load_Orchestrator**: 요금 계산 파이프라인 실행 전 필요한 데이터를 요금항목별로 분리 조회하고 조합하는 데이터 로딩 오케스트레이터.
- **Contract_Base_Loader**: 요금 계산 대상 가입자의 계약ID와 최소한의 기본 정보만을 조회하는 기본 로더.
- **Charge_Item_Data_Loader**: 개별 요금항목(월정액, 일회성, 통화료 등)에 필요한 데이터를 chunk 단위로 조회하는 요금항목별 데이터 로더 인터페이스.
- **Chunk**: 한 번의 DB 조회에서 처리하는 계약정보 묶음 단위. SQL IN 조건에 최대 1000건의 계약ID를 포함한다.
- **OneTimeChargeDomain**: 일회성 요금, 통화료, 종량료 등 다양한 유형의 비기간성 요금 데이터를 추상화하는 마커 인터페이스.
- **OneTimeChargeDataLoader**: OneTimeChargeDomain 구현체별로 특화된 데이터를 chunk 단위로 조회하는 제네릭 데이터 로더 인터페이스.
- **Data_Access_Strategy**: 요금 계산 유스케이스(정기청구, 실시간 조회, 예상 조회, 견적 조회)에 따라 데이터 읽기/쓰기 방식을 결정하는 전략 인터페이스.
- **ChargeContext**: 요금 계산 과정에서 입력 데이터와 중간 계산 결과를 담는 컨텍스트 객체.
- **ChargeInput**: 다양한 원천에서 조회한 데이터를 통합하는 요금 계산 입력 모델.
- **ContractInfo**: 요금 계산 요청 시 전달되는 개별 계약 정보 DTO.
- **Pipeline**: 요금 계산 흐름을 구성하는 추상화된 처리 단위. 여러 Step으로 구성된다.
- **Step**: Pipeline 내에서 개별 요금 항목 계산을 수행하는 단위 처리 모듈.
- **정기청구**: 월 단위로 수행되는 대용량 bulk 배치 처리 방식의 요금 계산. Spring Batch chunk 단위로 수행된다.
- **기준정보**: 상품 요금, 할인 정책, 과세 기준 등 요금 계산에 필요한 마스터 참조 데이터. 인메모리 캐시에서 관리된다.

## 요구사항

### 요구사항 1: 계약 기본정보 분리 조회

**사용자 스토리:** 요금 계산 담당 개발자로서, 요금 계산 대상 가입자의 계약ID와 최소한의 기본 정보만을 먼저 조회하고 싶다. 이를 통해 기존의 수십 개 테이블 outer join을 제거하고 SQL 복잡도를 낮출 수 있다.

#### 인수 조건

1. WHEN 요금 계산 요청이 수신되면, THE Contract_Base_Loader SHALL 계약ID와 최소한의 기본 정보(가입자ID, 상품ID, 청구시작일, 청구종료일)만을 조회한다.
2. WHEN 복수 건의 계약ID가 입력으로 전달되면, THE Contract_Base_Loader SHALL 해당 계약ID 전체를 SQL의 IN 조건에 포함하여 한 번의 쿼리로 조회한다.
3. THE Contract_Base_Loader SHALL 한 번의 IN 조건에 포함하는 계약ID 수를 최대 1000건으로 제한한다.
4. WHEN 1000건을 초과하는 계약ID가 입력되면, THE Contract_Base_Loader SHALL 1000건 단위로 분할하여 복수 회 조회를 수행한다.
5. THE Contract_Base_Loader SHALL 기준정보 테이블과의 join 없이 마스터 테이블(또는 유스케이스에 따른 원천 테이블)에서만 데이터를 조회한다.
6. IF 조회 결과가 비어 있으면, THEN THE Contract_Base_Loader SHALL 빈 리스트를 반환하고 후속 요금항목별 데이터 로딩을 생략한다.

### 요구사항 2: 요금항목별 데이터 로더 인터페이스

**사용자 스토리:** 요금 계산 담당 개발자로서, 각 요금항목이 필요로 하는 데이터를 독립적인 로더를 통해 조회하고 싶다. 이를 통해 요금항목 간 데이터 조회 로직의 결합도를 제거하고, 새로운 요금항목 추가 시 기존 로더를 수정하지 않을 수 있다.

#### 인수 조건

1. THE Charge_Item_Data_Loader SHALL 요금항목별로 독립된 데이터 조회 인터페이스를 제공한다.
2. THE Charge_Item_Data_Loader SHALL 복수 건의 ContractInfo를 입력으로 받아 chunk 단위로 데이터를 조회한다.
3. THE Charge_Item_Data_Loader SHALL 조회 결과를 계약ID 기준으로 그룹핑하여 반환한다.
4. WHEN 새로운 요금항목 유형이 추가되면, THE Data_Load_Orchestrator SHALL 기존 Charge_Item_Data_Loader 구현체 및 오케스트레이터 코드를 변경하지 않고 새로운 로더 구현체를 추가하는 것만으로 동작한다.
5. THE Charge_Item_Data_Loader SHALL 각 구현체가 자신이 담당하는 요금항목 유형을 식별할 수 있는 식별자를 제공한다.

### 요구사항 3: 월정액 데이터 로더

**사용자 스토리:** 요금 계산 담당 개발자로서, 월정액 요금 계산에 필요한 상품 가입정보와 정지이력을 chunk 단위로 한 번에 조회하고 싶다. 이를 통해 DB round trip을 최소화하면서 월정액 계산에 필요한 데이터를 효율적으로 확보할 수 있다.

#### 인수 조건

1. WHEN 월정액 데이터 로딩이 요청되면, THE Charge_Item_Data_Loader SHALL chunk에 포함된 전체 계약ID에 대한 상품 가입정보를 한 번의 쿼리로 조회한다.
2. WHEN 월정액 데이터 로딩이 요청되면, THE Charge_Item_Data_Loader SHALL chunk에 포함된 전체 계약ID에 대한 정지이력을 한 번의 쿼리로 조회한다.
3. THE Charge_Item_Data_Loader SHALL 상품 가입정보와 정지이력 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput에 설정한다.
4. THE Charge_Item_Data_Loader SHALL 월정액 데이터 조회 시 기준정보 테이블과의 join을 수행하지 않는다. 기준정보는 인메모리 캐시에서 별도로 조회한다.

### 요구사항 4: 일회성 요금 제네릭 데이터 로더

**사용자 스토리:** 요금 계산 담당 개발자로서, 다양한 유형의 일회성 요금(할부이력, 위약금, 가입비 등) 데이터를 제네릭 인터페이스를 통해 조회하고 싶다. 이를 통해 새로운 일회성 요금 유형이 추가되어도 기존 로직을 수정하지 않을 수 있다.

#### 인수 조건

1. THE OneTimeChargeDomain SHALL 다양한 일회성 요금 유형의 데이터를 추상화하는 마커 인터페이스를 제공한다.
2. THE OneTimeChargeDataLoader SHALL 제네릭 타입 파라미터를 사용하여 OneTimeChargeDomain 구현체별로 특화된 데이터를 chunk 단위로 조회한다.
3. WHEN 새로운 일회성 요금 유형이 추가되면, THE OneTimeChargeDataLoader SHALL 기존 OneTimeChargeDataLoader 구현체를 변경하지 않고 새로운 OneTimeChargeDomain 구현체와 해당 로더를 추가하는 것만으로 동작한다.
4. THE OneTimeFeeStep SHALL 등록된 모든 OneTimeChargeDataLoader 구현체를 순회하며 데이터를 로딩하고 계산 로직을 호출한다.
5. THE OneTimeChargeDataLoader SHALL 각 구현체가 조회한 데이터를 계약ID 기준으로 그룹핑하여 반환한다.

### 요구사항 5: 통화료 및 종량료 제네릭 데이터 로더

**사용자 스토리:** 요금 계산 담당 개발자로서, 다양한 유형의 통화료와 종량료 데이터를 일회성 요금과 동일한 제네릭 패턴으로 조회하고 싶다. 이를 통해 새로운 종량료 유형이 추가되어도 기존 로직을 수정하지 않을 수 있다.

#### 인수 조건

1. THE UsageChargeDomain SHALL 다양한 통화료/종량료 유형의 데이터를 추상화하는 마커 인터페이스를 제공한다.
2. THE UsageChargeDataLoader SHALL 제네릭 타입 파라미터를 사용하여 UsageChargeDomain 구현체별로 특화된 데이터를 chunk 단위로 조회한다.
3. WHEN 새로운 통화료/종량료 유형이 추가되면, THE UsageChargeDataLoader SHALL 기존 구현체를 변경하지 않고 새로운 UsageChargeDomain 구현체와 해당 로더를 추가하는 것만으로 동작한다.
4. THE UsageFeeStep SHALL 등록된 모든 UsageChargeDataLoader 구현체를 순회하며 데이터를 로딩하고 계산 로직을 호출한다.
5. THE UsageChargeDataLoader SHALL 각 구현체가 조회한 데이터를 계약ID 기준으로 그룹핑하여 반환한다.

### 요구사항 6: 데이터 로딩 오케스트레이션

**사용자 스토리:** 요금 계산 담당 개발자로서, 요금항목별로 분리된 데이터 로더들을 파이프라인 실행 전에 일괄 호출하여 ChargeInput을 조립하고 싶다. 이를 통해 각 Step이 데이터 로딩 로직을 직접 호출하지 않고 계산 로직에만 집중할 수 있다.

#### 인수 조건

1. WHEN 파이프라인 실행이 시작되면, THE Data_Load_Orchestrator SHALL 계약 기본정보 조회 후 등록된 모든 Charge_Item_Data_Loader를 순차적으로 호출하여 요금항목별 데이터를 조회한다.
2. THE Data_Load_Orchestrator SHALL 각 Charge_Item_Data_Loader가 반환한 데이터를 계약ID 기준으로 매핑하여 계약별 ChargeInput 객체를 조립한다.
3. THE Data_Load_Orchestrator SHALL 모든 데이터 로딩을 chunk 단위로 수행하여 DB round trip을 최소화한다.
4. THE Data_Load_Orchestrator SHALL 조립된 ChargeInput을 ChargeContext에 설정하여 후속 Step에서 참조할 수 있도록 한다.
5. WHEN 새로운 Charge_Item_Data_Loader 구현체가 Spring 컨텍스트에 등록되면, THE Data_Load_Orchestrator SHALL 해당 로더를 자동으로 인식하여 데이터 로딩 대상에 포함한다.

### 요구사항 7: Data_Access_Strategy와의 통합

**사용자 스토리:** 요금 계산 담당 개발자로서, 유스케이스별 Data_Access_Strategy가 요금항목별 데이터 로더를 활용하도록 하고 싶다. 이를 통해 정기청구, 실시간 조회, 예상 조회, 견적 조회 각각의 데이터 원천에 맞는 로더를 사용할 수 있다.

#### 인수 조건

1. THE Data_Access_Strategy SHALL 기존의 단일 readChargeInput 메서드 대신 Data_Load_Orchestrator를 통해 요금항목별 분리 조회를 수행한다.
2. WHEN 정기청구 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 마스터 테이블 기반의 Charge_Item_Data_Loader 구현체들을 사용하여 데이터를 조회한다.
3. WHEN 예상_요금_조회 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 접수 테이블 기반의 Charge_Item_Data_Loader 구현체들을 사용하여 데이터를 조회한다.
4. WHEN 견적_요금_조회 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 기준정보만을 사용하는 Charge_Item_Data_Loader 구현체들을 사용하여 데이터를 조회한다.
5. THE Data_Access_Strategy SHALL 복수 건의 ContractInfo를 입력으로 받아 chunk 단위 일괄 데이터 로딩을 지원한다.

### 요구사항 8: chunk 단위 일괄 처리 지원

**사용자 스토리:** 요금 계산 담당 개발자로서, 정기청구 배치에서 Spring Batch chunk 단위로 전달된 복수 건의 계약정보에 대해 데이터 로딩을 일괄 수행하고 싶다. 이를 통해 건별 DB 조회를 제거하고 배치 처리 성능을 향상시킬 수 있다.

#### 인수 조건

1. WHEN 정기청구 배치에서 chunk 단위로 계약정보 리스트가 전달되면, THE Data_Load_Orchestrator SHALL 해당 리스트 전체에 대해 요금항목별 데이터를 일괄 조회한다.
2. THE Data_Load_Orchestrator SHALL chunk 내 모든 계약에 대한 데이터를 요금항목당 한 번의 SQL로 조회한다.
3. THE Data_Load_Orchestrator SHALL 일괄 조회된 데이터를 계약ID 기준으로 분배하여 계약별 ChargeInput을 조립한다.
4. WHEN OLTP 유스케이스에서 단건 계약정보가 전달되면, THE Data_Load_Orchestrator SHALL 동일한 인터페이스를 사용하여 단건 조회를 수행한다.
5. THE Data_Load_Orchestrator SHALL chunk 크기가 1000건을 초과하는 경우 1000건 단위로 분할하여 조회한다.

### 요구사항 9: 할인 데이터 로더

**사용자 스토리:** 요금 계산 담당 개발자로서, 할인 계산에 필요한 할인 가입정보를 chunk 단위로 조회하고 싶다. 이를 통해 할인 Step이 필요한 데이터를 효율적으로 확보할 수 있다.

#### 인수 조건

1. WHEN 할인 데이터 로딩이 요청되면, THE Charge_Item_Data_Loader SHALL chunk에 포함된 전체 계약ID에 대한 할인 가입정보를 한 번의 쿼리로 조회한다.
2. THE Charge_Item_Data_Loader SHALL 할인 가입정보 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput에 설정한다.
3. THE Charge_Item_Data_Loader SHALL 할인 기준정보는 조회하지 않는다. 할인 기준정보는 인메모리 캐시에서 별도로 조회한다.

### 요구사항 10: 청구/수납 데이터 로더

**사용자 스토리:** 요금 계산 담당 개발자로서, 연체가산금과 자동납부할인 계산에 필요한 청구정보와 수납정보를 chunk 단위로 조회하고 싶다.

#### 인수 조건

1. WHEN 청구/수납 데이터 로딩이 요청되면, THE Charge_Item_Data_Loader SHALL chunk에 포함된 전체 계약ID에 대한 청구정보를 한 번의 쿼리로 조회한다.
2. WHEN 청구/수납 데이터 로딩이 요청되면, THE Charge_Item_Data_Loader SHALL chunk에 포함된 전체 계약ID에 대한 수납정보를 한 번의 쿼리로 조회한다.
3. THE Charge_Item_Data_Loader SHALL 청구정보와 수납정보 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput에 설정한다.

### 요구사항 11: 선납내역 데이터 로더

**사용자 스토리:** 요금 계산 담당 개발자로서, 선납반제 처리에 필요한 선납내역을 chunk 단위로 조회하고 싶다.

#### 인수 조건

1. WHEN 선납내역 데이터 로딩이 요청되면, THE Charge_Item_Data_Loader SHALL chunk에 포함된 전체 계약ID에 대한 선납내역을 한 번의 쿼리로 조회한다.
2. THE Charge_Item_Data_Loader SHALL 선납내역 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput에 설정한다.

### 요구사항 12: Step에서 데이터 로딩 로직 분리

**사용자 스토리:** 요금 계산 담당 개발자로서, 각 Step이 데이터 조회 로직을 직접 수행하지 않고 ChargeContext에 이미 적재된 데이터만을 사용하여 계산에 집중하도록 하고 싶다. 이를 통해 Step의 책임을 계산 로직으로 한정하고 테스트 용이성을 높일 수 있다.

#### 인수 조건

1. THE ChargeItemStep SHALL ChargeContext에 적재된 ChargeInput 데이터만을 사용하여 요금 계산을 수행한다.
2. THE ChargeItemStep SHALL MyBatis Mapper를 직접 호출하지 않는다.
3. WHEN ChargeContext에 해당 Step에 필요한 데이터가 존재하지 않으면, THE ChargeItemStep SHALL 해당 Step의 계산을 생략하고 다음 Step으로 진행한다.
4. THE OneTimeFeeStep SHALL 등록된 OneTimeChargeDataLoader 구현체들이 로딩한 데이터를 순회하며 각 유형별 계산 로직을 호출한다.
5. THE UsageFeeStep SHALL 등록된 UsageChargeDataLoader 구현체들이 로딩한 데이터를 순회하며 각 유형별 계산 로직을 호출한다.

### 요구사항 13: ChargeInput 모델 확장

**사용자 스토리:** 요금 계산 담당 개발자로서, 요금항목별 분리 조회된 데이터를 담을 수 있도록 ChargeInput 모델을 확장하고 싶다. 이를 통해 다양한 유형의 일회성 요금, 통화료/종량료 데이터를 유연하게 수용할 수 있다.

#### 인수 조건

1. THE ChargeInput SHALL 기존 필드(subscriptionInfo, suspensionHistories, billingInfo, paymentInfo, prepaidRecords)를 유지한다.
2. THE ChargeInput SHALL OneTimeChargeDomain 구현체 리스트를 유형별로 저장할 수 있는 구조를 제공한다.
3. THE ChargeInput SHALL UsageChargeDomain 구현체 리스트를 유형별로 저장할 수 있는 구조를 제공한다.
4. THE ChargeInput SHALL 할인 가입정보 리스트를 저장할 수 있는 필드를 제공한다.
5. THE ChargeInput SHALL 새로운 데이터 유형이 추가되어도 기존 필드 구조를 변경하지 않고 확장할 수 있는 구조를 제공한다.

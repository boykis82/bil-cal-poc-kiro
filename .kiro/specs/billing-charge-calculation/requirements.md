# 요구사항 문서

## 소개

본 문서는 유무선 통신 billing system의 요금 계산 모듈에 대한 요구사항을 정의한다. 기존 legacy system을 재구축하는 과정에서, 다양한 상품 유형(무선, 유선, 비회선, 구독상품)에 대한 요금 계산을 수행하고 청구 결과를 생성하는 모듈을 설계한다. TMForum 사양을 준수하며, 멀티테넌시를 지원하는 modular monolithic 아키텍처 기반으로 구현한다.

## 용어 정의

- **Charge_Calculation_Engine**: 요금 계산의 핵심 비즈니스 로직을 수행하는 엔진. 다양한 요금 항목을 파이프라인 기반으로 처리한다.
- **Charge_Calculation_API**: 요금 계산 요청을 수신하는 단일 진입점 API.
- **Pipeline**: 요금 계산 흐름을 구성하는 추상화된 처리 단위. 여러 Step으로 구성된다.
- **Step**: Pipeline 내에서 개별 요금 항목 계산을 수행하는 단위 처리 모듈.
- **Pipeline_Configurator**: 테넌트ID, 상품유형, 요금계산유스케이스구분에 따라 Pipeline의 Step 구성을 결정하는 모듈.
- **Reference_Data_Cache**: 기준정보를 인메모리에서 관리하는 캐시 모듈.
- **Charge_Item_Calculator**: 개별 요금 항목(월정액, 일회성, 통화료 등)을 계산하는 구현체의 공통 인터페이스.
- **Data_Access_Strategy**: 요금 계산 유스케이스(정기청구, 실시간 조회, 예상 조회, 견적 조회)에 따라 데이터 읽기/쓰기 방식을 결정하는 전략 인터페이스.
- **Charge_Context**: 요금 계산 과정에서 입력 데이터와 중간 계산 결과를 담는 추상화된 컨텍스트 객체.
- **Period_Charge_Result**: 사용기간(from/to)이 존재하는 요금 계산 결과.
- **Flat_Charge_Result**: 기간 정보가 없는 요금 계산 결과.
- **정기청구**: 월 단위로 수행되는 대용량 bulk 배치 처리 방식의 요금 계산.
- **실시간_요금_조회**: 가입자별 OLTP 방식의 요금 조회(해지 시 요금 조회 포함).
- **예상_요금_조회**: 개통 완료 전 접수 테이블 데이터 기반의 요금 조회.
- **견적_요금_조회**: 마스터/접수 테이블에 데이터가 없는 상태에서 기준정보만으로 수행하는 요금 조회.
- **선분이력**: 시간 구간별로 분리된 이력 데이터. 구간 겹침 시 박치기(intersection) 처리가 필요하다.
- **테넌트**: 멀티테넌시 환경에서 독립적으로 운영되는 사업 단위.
- **기준정보**: 상품 요금, 할인 정책, 과세 기준 등 요금 계산에 필요한 마스터 참조 데이터.

## 요구사항

### 요구사항 1: 단일 진입점 API

**사용자 스토리:** 요금 계산 담당 개발자로서, 다양한 요금 계산 유스케이스를 하나의 API로 처리하고 싶다. 이를 통해 호출 측에서 유스케이스별로 다른 API를 알 필요 없이 일관된 방식으로 요금 계산을 요청할 수 있다.

#### 인수 조건

1. THE Charge_Calculation_API SHALL 정기청구, 실시간_요금_조회, 예상_요금_조회, 견적_요금_조회 유스케이스를 단일 엔드포인트로 수신한다.
2. WHEN 요금 계산 요청이 수신되면, THE Charge_Calculation_API SHALL 요청에 포함된 유스케이스 구분 값을 기반으로 적절한 Data_Access_Strategy를 선택한다.
3. THE Charge_Calculation_API SHALL 하나 이상의 계약정보를 리스트 형태로 수신할 수 있다.
4. WHEN 정기청구 유스케이스로 요청이 수신되면, THE Charge_Calculation_API SHALL Spring Batch chunk 단위로 전달된 복수 건의 계약정보를 일괄 처리한다.
5. IF 요청에 유스케이스 구분 값이 누락되면, THEN THE Charge_Calculation_API SHALL 유효성 검증 오류 응답을 반환한다.
6. IF 요청에 포함된 계약정보 리스트가 비어 있으면, THEN THE Charge_Calculation_API SHALL 유효성 검증 오류 응답을 반환한다.

### 요구사항 2: Strategy Pattern 기반 데이터 접근

**사용자 스토리:** 요금 계산 담당 개발자로서, 새로운 요금 계산 유스케이스가 추가되더라도 핵심 비즈니스 로직을 변경하지 않고 데이터 접근 방식만 교체하고 싶다. 이를 통해 OCP 원칙을 준수하고 유지보수성을 높일 수 있다.

#### 인수 조건

1. THE Charge_Calculation_Engine SHALL 데이터 읽기/쓰기 로직을 Data_Access_Strategy 인터페이스를 통해 추상화한다.
2. WHEN 정기청구 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 마스터 테이블에서 계약정보를 읽고 계산 결과를 DB에 기록한다.
3. WHEN 실시간_요금_조회 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 마스터 테이블에서 계약정보를 읽고 계산 결과를 DB에 기록하지 않는다.
4. WHEN 예상_요금_조회 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 접수 테이블에서 계약정보를 읽는다.
5. WHEN 견적_요금_조회 유스케이스가 실행되면, THE Data_Access_Strategy SHALL 기준정보만을 사용하여 요금을 계산한다.
6. THE Charge_Calculation_Engine SHALL Data_Access_Strategy 구현체가 추가되더라도 엔진 내부의 요금 계산 비즈니스 로직 코드를 변경하지 않고 동작한다.

### 요구사항 3: Pipeline 및 Step 기반 요금 계산 흐름

**사용자 스토리:** 요금 계산 담당 개발자로서, 테넌트, 상품유형, 유스케이스에 따라 요금 계산 흐름을 유연하게 구성하고 싶다. 이를 통해 if/else 분기 없이 선언적으로 계산 흐름을 정의할 수 있다.

#### 인수 조건

1. THE Charge_Calculation_Engine SHALL 요금 계산 흐름을 Pipeline과 Step의 조합으로 구성한다.
2. THE Pipeline_Configurator SHALL 테넌트ID, 상품유형, 요금계산유스케이스구분의 조합에 따라 Pipeline에 포함될 Step 목록을 결정한다.
3. WHEN Pipeline이 실행되면, THE Pipeline SHALL 구성된 Step 목록을 순서대로 실행한다.
4. THE Pipeline SHALL Step 간에 Charge_Context를 전달하여 이전 Step의 계산 결과를 다음 Step에서 참조할 수 있도록 한다.
5. WHEN 새로운 요금 항목 유형이 추가되면, THE Charge_Calculation_Engine SHALL 기존 Pipeline 프레임워크 코드를 변경하지 않고 새로운 Step 구현체를 추가하는 것만으로 동작한다.
6. THE Pipeline_Configurator SHALL 동일한 Step을 서로 다른 Pipeline 구성에서 재사용할 수 있도록 한다.

### 요구사항 4: 기준정보 인메모리 캐시

**사용자 스토리:** 요금 계산 담당 개발자로서, 요금 계산 시 필요한 기준정보를 인메모리 캐시에서 조회하고 싶다. 이를 통해 SQL 조인 복잡도를 줄이고 I/O 효율을 높일 수 있다.

#### 인수 조건

1. THE Reference_Data_Cache SHALL 상품 요금, 할인 정책, 과세 기준 등 요금 계산에 필요한 기준정보를 인메모리에 저장하고 제공한다.
2. WHEN 정기청구 배치가 시작되면, THE Reference_Data_Cache SHALL 해당 배치에 필요한 기준정보 전체를 DB에서 메모리로 사전 적재한다.
3. WHILE OLTP 업무가 수행 중인 동안, THE Reference_Data_Cache SHALL 기준정보 변경 시 캐시 무효화를 수행한다.
4. WHILE OLTP 업무가 수행 중인 동안, THE Reference_Data_Cache SHALL 무효화된 캐시 항목에 대해 다음 조회 시 DB에서 최신 데이터를 다시 적재한다.
5. THE Reference_Data_Cache SHALL 요금 계산 로직이 캐시 사용 여부를 인지하지 않도록 투명한 인터페이스를 제공한다.
6. IF 캐시에서 기준정보 조회가 실패하면, THEN THE Reference_Data_Cache SHALL DB에서 해당 기준정보를 직접 조회하여 반환한다.

### 요구사항 5: 월정액 요금 계산

**사용자 스토리:** 요금 계산 담당 개발자로서, 상품 가입정보와 정지이력을 기반으로 월정액 요금을 일할 계산하고 싶다. 이를 통해 사용 기간에 따른 정확한 월정액을 산출할 수 있다.

#### 인수 조건

1. WHEN 월정액 Step이 실행되면, THE Charge_Item_Calculator SHALL 상품 가입정보, 정지이력, 상품 요금 기준정보를 입력으로 받아 요금을 계산한다.
2. THE Charge_Item_Calculator SHALL 월정액 계산 결과를 사용기간 from/to가 포함된 Period_Charge_Result 형태로 출력한다.
3. WHEN 복수의 선분이력이 존재하면, THE Charge_Item_Calculator SHALL 각 선분이력을 교차(intersection) 처리하여 겹치지 않는 구간으로 분리한 후 구간별 요금을 계산한다.
4. WHEN 특이상품에 대한 월정액 계산이 요청되면, THE Charge_Item_Calculator SHALL 해당 상품 유형에 필요한 추가 정보를 조회하여 계산에 반영한다.

### 요구사항 6: 일회성 요금 계산

**사용자 스토리:** 요금 계산 담당 개발자로서, 다양한 종류의 일회성 요금을 하나의 추상화된 단위로 처리하고 싶다. 이를 통해 일회성 요금 종류가 추가되더라도 일관된 방식으로 계산할 수 있다.

#### 인수 조건

1. WHEN 일회성 요금 Step이 실행되면, THE Charge_Item_Calculator SHALL 가입정보와 기준정보를 입력으로 받아 일회성 요금을 계산한다.
2. THE Charge_Item_Calculator SHALL 일회성 요금 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.
3. THE Charge_Item_Calculator SHALL 다양한 종류의 일회성 요금을 하나의 추상화된 인터페이스로 처리한다.

### 요구사항 7: 통화료 및 종량료 계산

**사용자 스토리:** 요금 계산 담당 개발자로서, 가입자의 사용량 기반 통화료와 종량료를 계산하고 싶다.

#### 인수 조건

1. WHEN 통화료/종량료 Step이 실행되면, THE Charge_Item_Calculator SHALL 가입정보와 기준정보를 입력으로 받아 통화료 및 종량료를 계산한다.
2. THE Charge_Item_Calculator SHALL 통화료/종량료 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 8: 기간 존재 할인 계산 (할인1)

**사용자 스토리:** 요금 계산 담당 개발자로서, 기간이 존재하는 원금성 항목에 대해 할인을 적용하고 싶다. 이를 통해 구간별 정확한 할인 금액을 산출할 수 있다.

#### 인수 조건

1. WHEN 할인1 Step이 실행되면, THE Charge_Item_Calculator SHALL 이전 Step에서 계산된 기간 존재 원금성 항목(Period_Charge_Result), 할인 가입정보, 할인 기준정보를 입력으로 받아 할인을 계산한다.
2. THE Charge_Item_Calculator SHALL 할인1 계산 결과를 기간 정보가 포함된 Period_Charge_Result 형태로 출력한다.
3. WHEN 할인1 Step 처리가 완료되면, THE Charge_Calculation_Engine SHALL 기간 존재 원금성 항목과 할인1 결과를 기간 정보를 제거하고 항목별 합산(group by sum)하여 Flat_Charge_Result로 압축한다.

### 요구사항 9: 기간 미존재 할인 계산 (할인2)

**사용자 스토리:** 요금 계산 담당 개발자로서, 기간이 없는 원금성 항목에 대해 할인을 적용하고 싶다.

#### 인수 조건

1. WHEN 할인2 Step이 실행되면, THE Charge_Item_Calculator SHALL 기간 미존재 원금성 항목(Flat_Charge_Result), 할인 가입정보, 할인 기준정보를 입력으로 받아 할인을 계산한다.
2. THE Charge_Item_Calculator SHALL 할인2 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 10: 연체가산금 계산

**사용자 스토리:** 요금 계산 담당 개발자로서, 미납 요금에 대한 연체가산금을 계산하고 싶다.

#### 인수 조건

1. WHEN 연체가산금 Step이 실행되면, THE Charge_Item_Calculator SHALL 청구정보, 수납정보, 이전 Step의 계산 결과를 입력으로 받아 연체가산금을 계산한다.
2. THE Charge_Item_Calculator SHALL 연체가산금 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 11: 자동납부할인 계산

**사용자 스토리:** 요금 계산 담당 개발자로서, 자동납부 가입자에 대한 할인을 계산하고 싶다.

#### 인수 조건

1. WHEN 자동납부할인 Step이 실행되면, THE Charge_Item_Calculator SHALL 청구정보, 수납정보, 이전 Step의 계산 결과, 자동납부할인 기준정보를 입력으로 받아 자동납부할인을 계산한다.
2. THE Charge_Item_Calculator SHALL 자동납부할인 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 12: 부가세 계산

**사용자 스토리:** 요금 계산 담당 개발자로서, 계산된 요금에 대해 과세 기준에 따른 부가세를 산출하고 싶다.

#### 인수 조건

1. WHEN 부가세 Step이 실행되면, THE Charge_Item_Calculator SHALL 이전 Step의 계산 결과와 과세 기준정보를 입력으로 받아 부가세를 계산한다.
2. THE Charge_Item_Calculator SHALL 부가세 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 13: 선납반제 처리

**사용자 스토리:** 요금 계산 담당 개발자로서, 선납된 금액을 당월 요금에서 차감 처리하고 싶다.

#### 인수 조건

1. WHEN 선납반제 Step이 실행되면, THE Charge_Item_Calculator SHALL 이전 Step의 계산 결과와 선납내역을 입력으로 받아 선납반제 금액을 계산한다.
2. THE Charge_Item_Calculator SHALL 선납반제 계산 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 14: 분리과금 처리

**사용자 스토리:** 요금 계산 담당 개발자로서, 분리과금 기준에 따라 요금을 분리하여 과금하고 싶다.

#### 인수 조건

1. WHEN 분리과금 Step이 실행되면, THE Charge_Item_Calculator SHALL 이전 Step의 계산 결과와 분리과금 기준정보를 입력으로 받아 분리과금을 처리한다.
2. THE Charge_Item_Calculator SHALL 분리과금 처리 결과를 기간 정보가 없는 Flat_Charge_Result 형태로 출력한다.

### 요구사항 15: 요금 계산 결과 모델 추상화

**사용자 스토리:** 요금 계산 담당 개발자로서, 다양한 요금 항목의 입출력을 일관된 모델로 추상화하고 싶다. 이를 통해 Step 간 데이터 전달과 결과 집계를 단순화할 수 있다.

#### 인수 조건

1. THE Charge_Calculation_Engine SHALL 요금 계산 입력을 다양한 원천(마스터 테이블, 접수 테이블, 기준정보)에 관계없이 하나의 추상화된 Charge_Context 객체로 통합한다.
2. THE Charge_Calculation_Engine SHALL 요금 계산 결과를 기간 존재 여부에 따라 Period_Charge_Result 또는 Flat_Charge_Result 두 가지 유형으로 분류한다.
3. THE Charge_Context SHALL 이전 Step에서 생성된 계산 결과를 누적하여 후속 Step에서 참조할 수 있도록 한다.

### 요구사항 16: 요금 항목 처리 상태 DB 갱신

**사용자 스토리:** 요금 계산 담당 개발자로서, 특정 요금 항목의 처리 완료 상태를 DB에 기록하고 싶다. 이를 통해 중복 처리를 방지하고 처리 이력을 추적할 수 있다.

#### 인수 조건

1. WHEN 처리 상태 기록이 필요한 요금 항목의 Step이 완료되면, THE Charge_Calculation_Engine SHALL 해당 요금 항목의 처리 완료 상태를 DB에 갱신한다.
2. THE Charge_Calculation_Engine SHALL 처리 상태 기록 대상 여부를 Step 단위로 설정할 수 있도록 한다.
3. IF 처리 상태 DB 갱신 중 오류가 발생하면, THEN THE Charge_Calculation_Engine SHALL 오류를 기록하고 해당 계약의 요금 계산을 실패 처리한다.

### 요구사항 17: 멀티테넌시 지원

**사용자 스토리:** 시스템 운영자로서, 복수의 테넌트가 각각 독립적인 요금 계산 정책을 적용받을 수 있도록 하고 싶다.

#### 인수 조건

1. THE Charge_Calculation_Engine SHALL 요금 계산 요청에 포함된 테넌트ID를 기반으로 해당 테넌트의 요금 계산 정책을 적용한다.
2. THE Pipeline_Configurator SHALL 테넌트ID별로 서로 다른 Pipeline Step 구성을 지원한다.
3. THE Reference_Data_Cache SHALL 테넌트별로 기준정보를 분리하여 관리한다.

### 요구사항 18: 컴포넌트 인터페이스 분리

**사용자 스토리:** 시스템 아키텍트로서, 요금 계산 컴포넌트가 modular monolithic 아키텍처의 컴포넌트 구조 규칙을 준수하도록 하고 싶다.

#### 인수 조건

1. THE Charge_Calculation_Engine SHALL 타 컴포넌트에 제공하는 기능을 인터페이스 전용 jar에 정의한다.
2. THE Charge_Calculation_Engine SHALL 인터페이스의 구현체를 별도의 구현 jar에 배치한다.
3. THE Charge_Calculation_Engine SHALL 내부 전용 기능을 별도의 내부 jar에 배치한다.
4. THE Charge_Calculation_Engine SHALL 타 컴포넌트와 인터페이스 jar를 통해서만 통신한다.

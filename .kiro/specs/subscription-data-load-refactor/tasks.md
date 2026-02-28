# 구현 계획: Subscription Data Load 리팩토링

## 개요

기존 단일 SQL outer join 기반 데이터 로딩을 요금항목별 분리 로더 + chunk 단위 일괄 조회 방식으로 리팩토링한다. 마커 인터페이스와 제네릭 로더 패턴을 도입하여 OCP를 준수하고, DataLoadOrchestrator가 파이프라인 실행 전 데이터 로딩을 일괄 수행하도록 한다.

## Tasks

- [x] 1. 마커 인터페이스 및 도메인 모델 생성
  - [x] 1.1 OneTimeChargeDomain, UsageChargeDomain 마커 인터페이스 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/model/` 패키지에 생성
    - `OneTimeChargeDomain`: `getContractId()` 메서드를 가진 인터페이스
    - `UsageChargeDomain`: `getContractId()` 메서드를 가진 인터페이스
    - _요구사항: 4.1, 5.1_

  - [x] 1.2 구체 도메인 클래스 생성
    - `InstallmentHistory`, `PenaltyFee` → `OneTimeChargeDomain` 구현
    - `VoiceUsage`, `DataUsage` → `UsageChargeDomain` 구현
    - `DiscountSubscription` 모델 클래스 생성
    - 모든 클래스에 Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 적용
    - `billing-charge-calculation-internal` 모듈의 `internal/model/` 패키지에 생성
    - _요구사항: 4.1, 5.1, 9.1_

  - [x] 1.3 ChargeInput 모델 확장
    - 기존 필드(subscriptionInfo, suspensionHistories, billingInfo, paymentInfo, prepaidRecords) 유지
    - `discountSubscriptions` (List\<DiscountSubscription\>) 필드 추가
    - `oneTimeChargeDataMap` (Map\<Class\<? extends OneTimeChargeDomain\>, List\<? extends OneTimeChargeDomain\>\>) 필드 추가 (Builder.Default로 빈 HashMap 초기화)
    - `usageChargeDataMap` (Map\<Class\<? extends UsageChargeDomain\>, List\<? extends UsageChargeDomain\>\>) 필드 추가 (Builder.Default로 빈 HashMap 초기화)
    - 제네릭 유틸리티 메서드 추가: `getOneTimeChargeData(Class<T>)`, `putOneTimeChargeData(Class<T>, List<T>)`, `getUsageChargeData(Class<T>)`, `putUsageChargeData(Class<T>, List<T>)`
    - 파일: `billing-charge-calculation-internal/.../internal/model/ChargeInput.java`
    - _요구사항: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 1.4 Property 3 속성 테스트: ChargeInput 도메인 데이터 저장/조회 round-trip
    - **Property 3: ChargeInput 도메인 데이터 저장/조회 round-trip**
    - **Validates: 요구사항 13.2, 13.3**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 OneTimeChargeDomain/UsageChargeDomain 구현체 리스트를 ChargeInput에 put한 후 동일 Class 타입으로 get하면 동일 리스트 반환 검증
    - 태그: `Feature: subscription-data-load-refactor, Property 3: ChargeInput 도메인 데이터 저장/조회 round-trip`

- [x] 2. chunk 분할 유틸리티 및 데이터 로더 인터페이스 생성
  - [x] 2.1 chunk 분할 유틸리티 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/util/` 패키지에 `ChunkPartitioner` 클래스 생성
    - `List<T>` 입력을 최대 1000건 단위로 분할하는 `partition(List<T> items, int maxSize)` 정적 메서드 구현
    - 빈 리스트 입력 시 빈 리스트 반환
    - _요구사항: 1.3, 1.4, 8.5_

  - [x] 2.2 Property 1 속성 테스트: chunk 분할 시 원소 보존 및 크기 제한
    - **Property 1: chunk 분할 시 원소 보존 및 크기 제한**
    - **Validates: 요구사항 1.3, 1.4, 8.5**
    - jqwik `@Property(tries = 100)` 사용
    - 임의 크기(0~5000)의 String 리스트에 대해: 각 chunk 크기 ≤ 1000, 모든 chunk 합치면 원본과 동일, chunk 개수 = ceil(N/1000) 검증
    - 태그: `Feature: subscription-data-load-refactor, Property 1: chunk 분할 시 원소 보존 및 크기 제한`

  - [x] 2.3 ContractBaseLoader 인터페이스 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/dataloader/` 패키지에 생성
    - `loadBaseContracts(List<String> contractIds)` 메서드 정의
    - IN 조건 최대 1000건 제한을 내부에서 ChunkPartitioner를 활용하여 처리
    - _요구사항: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 2.4 ChargeItemDataLoader 인터페이스 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/dataloader/` 패키지에 생성
    - `getChargeItemType()`, `loadAndPopulate(List<ContractInfo>, Map<String, ChargeInput>)` 메서드 정의
    - _요구사항: 2.1, 2.2, 2.3, 2.5_

  - [x] 2.5 OneTimeChargeDataLoader\<T\>, UsageChargeDataLoader\<T\> 제네릭 인터페이스 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/dataloader/` 패키지에 생성
    - `OneTimeChargeDataLoader<T extends OneTimeChargeDomain>`: `getDomainType()`, `loadData(List<ContractInfo>)` 메서드
    - `UsageChargeDataLoader<T extends UsageChargeDomain>`: `getDomainType()`, `loadData(List<ContractInfo>)` 메서드
    - 반환 타입: `Map<String, List<T>>` (계약ID 기준 그룹핑)
    - _요구사항: 4.2, 4.3, 4.5, 5.2, 5.3, 5.5_

  - [x] 2.6 Property 2 속성 테스트: 계약ID 기준 그룹핑 정합성
    - **Property 2: 계약ID 기준 그룹핑 정합성**
    - **Validates: 요구사항 2.3, 3.3, 4.5, 5.5, 6.2, 8.3, 9.2, 10.3, 11.2**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 contractId를 가진 도메인 객체 리스트에 대해: 각 key의 value 리스트 항목이 해당 contractId를 가짐, 원본 전체 항목이 결과에 존재, 항목 수 합계 동일 검증
    - 태그: `Feature: subscription-data-load-refactor, Property 2: 계약ID 기준 그룹핑 정합성`

- [x] 3. Checkpoint - 모든 테스트 통과 확인
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [x] 4. MyBatis Mapper 인터페이스 및 XML 생성
  - [x] 4.1 ContractBaseMapper 인터페이스 및 XML 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/mapper/` 패키지에 인터페이스 생성
    - `selectBaseContracts(@Param("contractIds") List<String> contractIds)` 메서드 정의
    - `resources/mapper/ContractBaseMapper.xml` 생성 (Oracle SQL, IN 조건 사용)
    - _요구사항: 1.1, 1.2, 1.5_

  - [x] 4.2 MonthlyFeeMapper 인터페이스 및 XML 생성
    - `selectSubscriptionInfos(@Param("contractIds") List<String>)`, `selectSuspensionHistories(@Param("contractIds") List<String>)` 메서드 정의
    - `resources/mapper/MonthlyFeeMapper.xml` 생성
    - _요구사항: 3.1, 3.2, 3.4_

  - [x] 4.3 DiscountMapper, BillingPaymentMapper, PrepaidMapper 인터페이스 및 XML 생성
    - `DiscountMapper`: `selectDiscountSubscriptions` 메서드
    - `BillingPaymentMapper`: `selectBillingInfos`, `selectPaymentInfos` 메서드
    - `PrepaidMapper`: `selectPrepaidRecords` 메서드
    - 각각 `resources/mapper/` 하위에 XML 생성
    - _요구사항: 9.1, 10.1, 10.2, 11.1_

- [x] 5. 요금항목별 데이터 로더 구현체 생성
  - [x] 5.1 ContractBaseLoader 구현체 생성 (유스케이스별)
    - `billing-charge-calculation-internal` 모듈의 `internal/dataloader/` 패키지에 구현
    - `MasterContractBaseLoader`: 마스터 테이블 기반 (정기청구, 실시간 조회)
    - `OrderContractBaseLoader`: 접수 테이블 기반 (예상 조회)
    - `QuotationContractBaseLoader`: 기준정보 기반 (견적 조회)
    - 각 구현체에서 ChunkPartitioner를 사용하여 1000건 단위 분할 처리
    - 조회 결과 비어있으면 빈 리스트 반환
    - _요구사항: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 7.2, 7.3, 7.4_

  - [x] 5.2 MonthlyFeeDataLoader 구현체 생성
    - `ChargeItemDataLoader` 구현
    - MonthlyFeeMapper를 사용하여 상품 가입정보, 정지이력을 chunk 단위 조회
    - 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput의 subscriptionInfo, suspensionHistories에 설정
    - 기준정보 테이블 join 없음
    - _요구사항: 3.1, 3.2, 3.3, 3.4_

  - [x] 5.3 DiscountDataLoader 구현체 생성
    - `ChargeItemDataLoader` 구현
    - DiscountMapper를 사용하여 할인 가입정보를 chunk 단위 조회
    - 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput의 discountSubscriptions에 설정
    - 할인 기준정보는 조회하지 않음
    - _요구사항: 9.1, 9.2, 9.3_

  - [x] 5.4 BillingPaymentDataLoader 구현체 생성
    - `ChargeItemDataLoader` 구현
    - BillingPaymentMapper를 사용하여 청구정보, 수납정보를 chunk 단위 조회
    - 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput의 billingInfo, paymentInfo에 설정
    - _요구사항: 10.1, 10.2, 10.3_

  - [x] 5.5 PrepaidDataLoader 구현체 생성
    - `ChargeItemDataLoader` 구현
    - PrepaidMapper를 사용하여 선납내역을 chunk 단위 조회
    - 조회 결과를 계약ID 기준으로 매핑하여 ChargeInput의 prepaidRecords에 설정
    - _요구사항: 11.1, 11.2_

  - [x] 5.6 일회성 요금 제네릭 로더 구현체 생성 (InstallmentHistoryLoader, PenaltyFeeLoader)
    - `OneTimeChargeDataLoader<InstallmentHistory>`, `OneTimeChargeDataLoader<PenaltyFee>` 구현
    - 각각 전용 MyBatis Mapper를 호출하여 chunk 단위 조회
    - 조회 결과를 계약ID 기준으로 그룹핑하여 반환
    - _요구사항: 4.2, 4.3, 4.5_

  - [x] 5.7 통화료/종량료 제네릭 로더 구현체 생성 (VoiceUsageLoader, DataUsageLoader)
    - `UsageChargeDataLoader<VoiceUsage>`, `UsageChargeDataLoader<DataUsage>` 구현
    - 각각 전용 MyBatis Mapper를 호출하여 chunk 단위 조회
    - 조회 결과를 계약ID 기준으로 그룹핑하여 반환
    - _요구사항: 5.2, 5.3, 5.5_

- [x] 6. DataLoadOrchestrator 구현
  - [x] 6.1 DataLoadOrchestrator 클래스 생성
    - `billing-charge-calculation-impl` 모듈의 `impl/dataloader/` 패키지에 생성
    - `@Component`, `@RequiredArgsConstructor` 적용
    - Spring 컨텍스트에서 `List<ChargeItemDataLoader>`, `List<OneTimeChargeDataLoader<?>>`, `List<UsageChargeDataLoader<?>>` 자동 주입
    - `loadAll(ContractBaseLoader baseLoader, List<ChargeItemDataLoader> itemLoaders, List<String> contractIds)` 메서드 구현
    - 1000건 초과 시 ChunkPartitioner로 분할 처리
    - ContractBaseLoader 조회 결과 비어있으면 빈 Map 반환하고 후속 로더 호출 생략
    - 각 ChargeItemDataLoader의 loadAndPopulate 호출
    - OneTimeChargeDataLoader, UsageChargeDataLoader 순회하며 ChargeInput에 데이터 설정
    - _요구사항: 6.1, 6.2, 6.3, 6.4, 6.5, 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 6.2 Property 5 속성 테스트: 오케스트레이터 전체 로더 호출
    - **Property 5: 오케스트레이터 전체 로더 호출**
    - **Validates: 요구사항 6.1**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 N개 mock ChargeItemDataLoader가 등록된 DataLoadOrchestrator에 대해 loadAll 호출 시 모든 N개 로더가 호출되는지 검증
    - 태그: `Feature: subscription-data-load-refactor, Property 5: 오케스트레이터 전체 로더 호출`

- [x] 7. DataAccessStrategy 확장 및 유스케이스별 전략 수정
  - [x] 7.1 DataAccessStrategy 인터페이스 확장
    - `getContractBaseLoader()` 메서드 추가
    - `getChargeItemDataLoaders()` 메서드 추가
    - 기존 `readChargeInput`, `writeChargeResult`, `updateProcessingStatus` 메서드 유지
    - 파일: `billing-charge-calculation-internal/.../internal/strategy/DataAccessStrategy.java`
    - _요구사항: 7.1, 7.5_

  - [x] 7.2 유스케이스별 DataAccessStrategy 구현체 수정
    - `RegularBillingStrategy`: MasterContractBaseLoader + 마스터 테이블 기반 ChargeItemDataLoader 목록 반환
    - `RealtimeQueryStrategy`: MasterContractBaseLoader + 마스터 테이블 기반 ChargeItemDataLoader 목록 반환
    - `EstimateQueryStrategy`: OrderContractBaseLoader + 접수 테이블 기반 ChargeItemDataLoader 목록 반환
    - `QuotationQueryStrategy`: QuotationContractBaseLoader + 기준정보 기반 ChargeItemDataLoader 목록 반환
    - _요구사항: 7.2, 7.3, 7.4_

- [x] 8. Step 리팩토링 (데이터 로딩 로직 분리)
  - [x] 8.1 OneTimeFeeStep 리팩토링
    - 기존 SubscriptionInfo 기반 로직 제거
    - ChargeInput의 oneTimeChargeDataMap에서 데이터를 읽어 유형별 계산 로직 호출
    - 데이터 없으면 안전하게 생략 (예외 없이 return)
    - MyBatis Mapper 직접 호출 제거
    - _요구사항: 4.4, 12.1, 12.2, 12.3, 12.4_

  - [x] 8.2 UsageFeeStep 리팩토링
    - 기존 SubscriptionInfo 기반 로직 제거
    - ChargeInput의 usageChargeDataMap에서 데이터를 읽어 유형별 계산 로직 호출
    - 데이터 없으면 안전하게 생략 (예외 없이 return)
    - MyBatis Mapper 직접 호출 제거
    - _요구사항: 5.4, 12.1, 12.2, 12.3, 12.5_

  - [x] 8.3 기타 Step 리팩토링 (MonthlyFeeStep, DiscountStep 등)
    - ChargeContext에 적재된 ChargeInput 데이터만 사용하도록 변경
    - 데이터 부재 시 안전하게 생략
    - _요구사항: 12.1, 12.2, 12.3_

  - [x] 8.4 Property 4 속성 테스트: Step 데이터 부재 시 안전한 생략
    - **Property 4: Step 데이터 부재 시 안전한 생략**
    - **Validates: 요구사항 12.3**
    - jqwik `@Property(tries = 100)` 사용
    - 빈 또는 불완전한 ChargeInput을 가진 ChargeContext에 대해 각 Step 실행 시 예외 미발생, 결과 리스트에 새 항목 미추가 검증
    - 태그: `Feature: subscription-data-load-refactor, Property 4: Step 데이터 부재 시 안전한 생략`

  - [x] 8.5 Property 6 속성 테스트: Step의 모든 등록 유형 데이터 처리
    - **Property 6: Step의 모든 등록 유형 데이터 처리**
    - **Validates: 요구사항 4.4, 5.4, 12.4, 12.5**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 N개 유형의 OneTimeChargeDomain/UsageChargeDomain 데이터가 ChargeInput에 존재할 때 해당 Step 실행 시 모든 N개 유형에 대해 계산 결과 생성 검증
    - 태그: `Feature: subscription-data-load-refactor, Property 6: Step의 모든 등록 유형 데이터 처리`

- [x] 9. Checkpoint - 모든 테스트 통과 확인
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [x] 10. ChargeCalculationServiceImpl 연동 및 통합
  - [x] 10.1 ChargeCalculationServiceImpl 수정
    - DataLoadOrchestrator 의존성 주입 추가
    - 기존 건별 `strategy.readChargeInput(contract)` 호출을 DataLoadOrchestrator 기반 일괄 로딩으로 변경
    - Strategy에서 ContractBaseLoader, ChargeItemDataLoader 목록을 가져와 DataLoadOrchestrator에 전달
    - 일괄 로딩된 `Map<String, ChargeInput>`을 사용하여 계약별 ChargeContext 생성
    - OLTP 단건 처리도 동일 인터페이스로 동작하도록 보장
    - 파일: `billing-charge-calculation-impl/.../impl/service/ChargeCalculationServiceImpl.java`
    - _요구사항: 6.4, 7.1, 8.1, 8.4_

  - [x] 10.2 DataLoadException 예외 클래스 생성
    - `billing-charge-calculation-api` 모듈의 `api/exception/` 패키지에 생성
    - 데이터 로딩 단계에서 발생하는 오류를 구분하기 위한 전용 예외
    - 기존 `ChargeCalculationException`을 상속
    - _요구사항: 6.1_

  - [x] 10.3 단위 테스트: DataAccessStrategy 유스케이스별 로더 선택 검증
    - 정기청구 → MasterContractBaseLoader, 예상 조회 → OrderContractBaseLoader, 견적 조회 → QuotationContractBaseLoader 반환 확인
    - OLTP 단건 처리 시 동일 인터페이스 정상 동작 확인
    - _요구사항: 7.2, 7.3, 7.4, 8.4_

- [x] 11. Final Checkpoint - 전체 테스트 통과 확인
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

## Notes

- `*` 표시된 태스크는 선택사항이며 빠른 MVP를 위해 건너뛸 수 있다
- 각 태스크는 추적성을 위해 구체적인 요구사항을 참조한다
- Checkpoint에서 점진적 검증을 수행한다
- 속성 테스트는 보편적 정합성 속성을 검증하고, 단위 테스트는 구체적 예시와 엣지 케이스를 검증한다
- 모든 코드는 Java 25, Spring Boot 4.0.1, Lombok, MyBatis, Oracle 기반으로 작성한다

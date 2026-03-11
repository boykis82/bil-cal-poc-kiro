# 구현 계획: Pipeline 기반 DataLoader 선택

## 개요

현재 DataLoadOrchestrator는 Spring 컨텍스트에 등록된 모든 OneTimeChargeDataLoader와 UsageChargeDataLoader bean을 무조건 호출한다. 이를 Pipeline 구성에 따라 실제로 필요한 DataLoader만 선택적으로 호출하도록 개선한다. 각 Step이 필요한 DataLoader를 명시적으로 선언하고, DataLoaderRegistry가 Pipeline에서 이를 수집하여 필요한 DataLoader만 필터링한다.

## Tasks

- [ ] 1. ChargeItemStep 인터페이스 확장
  - [ ] 1.1 필요 DataLoader 선언 메서드 추가
    - `billing-charge-calculation-internal` 모듈의 `internal/step/ChargeItemStep.java` 수정
    - `getRequiredChargeItemTypes()` default 메서드 추가 (빈 리스트 반환)
    - `getRequiredOneTimeChargeDomains()` default 메서드 추가 (빈 리스트 반환)
    - `getRequiredUsageChargeDomains()` default 메서드 추가 (빈 리스트 반환)
    - 기존 메서드 (getStepId, getOrder, process, requiresStatusUpdate) 유지
    - _요구사항: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 2. DataLoaderRegistry 구현
  - [ ] 2.1 RequiredDataLoaders 레코드 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/dataloader/` 패키지에 생성
    - `chargeItemDataLoaders`, `oneTimeChargeDataLoaders`, `usageChargeDataLoaders` 필드를 가진 record
    - _요구사항: 2.1, 2.2, 2.3, 2.4_

  - [ ] 2.2 DataLoaderRegistry 클래스 생성
    - `billing-charge-calculation-internal` 모듈의 `internal/dataloader/` 패키지에 생성
    - `@Component`, `@RequiredArgsConstructor`, `@Slf4j` 적용
    - Spring 컨텍스트에서 모든 ChargeItemDataLoader, OneTimeChargeDataLoader, UsageChargeDataLoader 주입
    - LRU 캐시 (MAX_CACHE_SIZE = 100) 구현 (Collections.synchronizedMap + LinkedHashMap)
    - `getRequiredDataLoaders(Pipeline)` 메서드 구현
    - `collectRequiredDataLoaders(Pipeline)` private 메서드 구현
    - _요구사항: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 8.1, 8.2, 8.3, 8.4_

  - [ ] 2.3 Property 1 속성 테스트: Pipeline의 모든 Step 필요 DataLoader 수집
    - **Property 1: Pipeline의 모든 Step 필요 DataLoader 수집**
    - **Validates: 요구사항 2.1, 2.2, 2.3, 2.4, 2.5**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 Step 목록을 가진 Pipeline에 대해: 수집된 DataLoader 유형이 모든 Step의 선언을 포함, 중복 제거, Pipeline 외부 Step 미포함 검증
    - 태그: `Feature: pipeline-based-dataloader-selection, Property 1: Pipeline의 모든 Step 필요 DataLoader 수집`

  - [ ] 2.4 Property 2 속성 테스트: DataLoader 인스턴스 매칭 정합성
    - **Property 2: DataLoader 인스턴스 매칭 정합성**
    - **Validates: 요구사항 3.4, 3.5, 3.6, 3.7**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 ChargeItemType/Domain Class 목록에 대해: 반환된 DataLoader가 요청 유형에만 매칭, 미요청 유형 미포함, 매칭 없는 유형 무시 검증
    - 태그: `Feature: pipeline-based-dataloader-selection, Property 2: DataLoader 인스턴스 매칭 정합성`

- [ ] 3. DataLoadOrchestrator 수정
  - [ ] 3.1 DataLoaderRegistry 의존성 주입 추가
    - `billing-charge-calculation-impl` 모듈의 `impl/dataloader/DataLoadOrchestrator.java` 수정
    - 기존 `List<OneTimeChargeDataLoader<?>>`, `List<UsageChargeDataLoader<?>>` 필드 제거
    - `DataLoaderRegistry` 필드 추가
    - _요구사항: 4.1_

  - [ ] 3.2 loadAll 메서드 시그니처 변경
    - `Pipeline pipeline` 파라미터 추가
    - 기존 파라미터 (baseLoader, itemLoaders, contractIds) 유지
    - _요구사항: 4.2, 5.1, 5.2_

  - [ ] 3.3 Pipeline 기반 DataLoader 선택 로직 구현
    - `dataLoaderRegistry.getRequiredDataLoaders(pipeline)` 호출
    - 반환된 RequiredDataLoaders에서 oneTimeChargeDataLoaders, usageChargeDataLoaders 획득
    - 획득한 목록만 순회하며 populateOneTimeChargeData, populateUsageChargeData 호출
    - _요구사항: 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 3.4 로깅 추가
    - 호출된 DataLoader 유형과 조회 건수 DEBUG 레벨 로깅
    - 호출되지 않은 DataLoader 유형 DEBUG 레벨 로깅
    - _요구사항: 9.1, 9.2, 9.3, 9.4_

  - [ ] 3.5 Property 3 속성 테스트: DataLoadOrchestrator 선택적 호출
    - **Property 3: DataLoadOrchestrator 선택적 호출**
    - **Validates: 요구사항 4.3, 4.4, 4.5, 4.6**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 Pipeline과 mock DataLoader에 대해: Registry 반환 목록의 DataLoader만 호출, 미포함 DataLoader 미호출, 모든 DataLoader 정확히 1회 호출 검증
    - 태그: `Feature: pipeline-based-dataloader-selection, Property 3: DataLoadOrchestrator 선택적 호출`

- [ ] 4. ChargeCalculationServiceImpl 수정
  - [ ] 4.1 Pipeline 객체를 DataLoadOrchestrator에 전달
    - `billing-charge-calculation-impl` 모듈의 `impl/service/ChargeCalculationServiceImpl.java` 수정
    - `dataLoadOrchestrator.loadAll()` 호출 시 `pipeline` 파라미터 추가
    - 기존 로직 (Strategy 결정, Pipeline 구성, 계약별 처리) 유지
    - _요구사항: 5.1, 5.2, 5.3_

- [ ] 5. Checkpoint - 모든 테스트 통과 확인
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

- [ ] 6. Step 구현체 DataLoader 선언 추가
  - [ ] 6.1 MonthlyFeeStep DataLoader 선언
    - `getRequiredChargeItemTypes()` 오버라이드
    - `List.of(ChargeItemType.MONTHLY_FEE)` 반환
    - _요구사항: 6.1_

  - [ ] 6.2 OneTimeFeeStep 동적 DataLoader 선언
    - `List<OneTimeChargeDataLoader<?>>` 필드 주입 (생성자)
    - `getRequiredOneTimeChargeDomains()` 오버라이드
    - 주입받은 모든 OneTimeChargeDataLoader의 getDomainType() 결과를 리스트로 반환
    - _요구사항: 6.2, 10.1, 10.2, 10.5_

  - [ ] 6.3 UsageFeeStep 동적 DataLoader 선언
    - `List<UsageChargeDataLoader<?>>` 필드 주입 (생성자)
    - `getRequiredUsageChargeDomains()` 오버라이드
    - 주입받은 모든 UsageChargeDataLoader의 getDomainType() 결과를 리스트로 반환
    - _요구사항: 6.3, 10.3, 10.4, 10.6_

  - [ ] 6.4 PeriodDiscountStep DataLoader 선언
    - `getRequiredChargeItemTypes()` 오버라이드
    - `List.of(ChargeItemType.DISCOUNT)` 반환
    - _요구사항: 6.4_

  - [ ] 6.5 FlatDiscountStep DataLoader 선언
    - `getRequiredChargeItemTypes()` 오버라이드
    - `List.of(ChargeItemType.DISCOUNT)` 반환
    - _요구사항: 6.5_

  - [ ] 6.6 LateFeeStep DataLoader 선언
    - `getRequiredChargeItemTypes()` 오버라이드
    - `List.of(ChargeItemType.LATE_FEE)` 반환
    - _요구사항: 6.6_

  - [ ] 6.7 AutoPayDiscountStep DataLoader 선언
    - `getRequiredChargeItemTypes()` 오버라이드
    - `List.of(ChargeItemType.LATE_FEE)` 반환
    - _요구사항: 6.7_

  - [ ] 6.8 PrepaidOffsetStep DataLoader 선언
    - `getRequiredChargeItemTypes()` 오버라이드
    - `List.of(ChargeItemType.PREPAID)` 반환
    - _요구사항: 6.8_

  - [ ] 6.9 TaxStep DataLoader 선언 확인
    - 기본 구현 사용 (빈 리스트 반환) - 코드 변경 불필요
    - 단위 테스트로 빈 리스트 반환 확인
    - _요구사항: 6.9_

  - [ ] 6.10 SeparateBillingStep DataLoader 선언 확인
    - 기본 구현 사용 (빈 리스트 반환) - 코드 변경 불필요
    - 단위 테스트로 빈 리스트 반환 확인
    - _요구사항: 6.10_

- [ ] 7. 하위 호환성 및 캐시 테스트
  - [ ] 7.1 Property 4 속성 테스트: 캐시 일관성
    - **Property 4: 캐시 일관성**
    - **Validates: 요구사항 5.4, 8.1, 8.2, 8.3, 8.4**
    - jqwik `@Property(tries = 100)` 사용
    - 임의의 Pipeline ID 시퀀스에 대해: 동일 ID 반복 요청 시 동일 결과 반환, 캐시 히트 시 Step 순회 생략, 캐시 크기 제한 준수 검증
    - 태그: `Feature: pipeline-based-dataloader-selection, Property 4: 캐시 일관성`

  - [ ] 7.2 Property 5 속성 테스트: 하위 호환성 보장
    - **Property 5: 하위 호환성 보장**
    - **Validates: 요구사항 7.1, 7.2**
    - jqwik `@Property(tries = 100)` 사용
    - DataLoader 선언 없는 Step을 포함한 Pipeline에 대해: Registry가 해당 Step 무시, 예외 미발생, Step process 정상 호출 검증
    - 태그: `Feature: pipeline-based-dataloader-selection, Property 5: 하위 호환성 보장`

  - [ ] 7.3 단위 테스트: DataLoaderRegistry 캐시 동작
    - 동일 Pipeline ID 반복 요청 시 캐시 히트 확인
    - 캐시 크기 초과 시 LRU 제거 확인
    - _요구사항: 8.2, 8.4_

  - [ ] 7.4 단위 테스트: 하위 호환성
    - DataLoader 선언 없는 Step이 포함된 Pipeline 정상 동작 확인
    - 기존 Step 구현체가 변경 없이 동작하는지 확인
    - _요구사항: 7.1, 7.2, 7.3_

- [ ] 8. 통합 테스트 및 성능 검증
  - [ ] 8.1 통합 테스트: 전체 흐름 검증
    - ChargeCalculationServiceImpl → DataLoadOrchestrator → DataLoaderRegistry 전체 흐름
    - Pipeline에 OneTimeFeeStep 미포함 시 OneTimeChargeDataLoader 미호출 확인
    - Pipeline에 UsageFeeStep 미포함 시 UsageChargeDataLoader 미호출 확인
    - _요구사항: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2_

  - [ ] 8.2 통합 테스트: 로깅 검증
    - 호출된 DataLoader 유형이 올바르게 로깅되는지 확인
    - 호출되지 않은 DataLoader 유형이 올바르게 로깅되는지 확인
    - _요구사항: 9.1, 9.2, 9.3, 9.4_

  - [ ] 8.3 성능 테스트: DataLoader 호출 횟수 측정
    - 개선 전후 DataLoader 호출 횟수 비교
    - 정기청구 시나리오 (10,000건) 시뮬레이션
    - 예상: 75% 감소 확인
    - _요구사항: 4.6_

  - [ ] 8.4 성능 테스트: 캐시 효과 측정
    - 동일 Pipeline 반복 실행 시 캐시 히트율 측정
    - 정기청구 배치 시나리오 (1,000건 chunk × 10회) 시뮬레이션
    - 예상: 90% 캐시 히트율 확인
    - _요구사항: 5.4, 8.2_

- [ ] 9. Final Checkpoint - 전체 테스트 통과 확인
  - 모든 테스트가 통과하는지 확인하고, 질문이 있으면 사용자에게 문의한다.

## Notes

- 각 태스크는 추적성을 위해 구체적인 요구사항을 참조한다
- Checkpoint에서 점진적 검증을 수행한다
- 속성 테스트는 보편적 정합성 속성을 검증하고, 단위 테스트는 구체적 예시와 통합 시나리오를 검증한다
- 모든 코드는 Java 25, Spring Boot 4.0.1, Lombok 기반으로 작성한다
- 기존 코드 변경을 최소화하여 하위 호환성을 보장한다
- 성능 테스트를 통해 실제 개선 효과를 검증한다

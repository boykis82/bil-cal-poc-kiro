# 구현 계획: 요금 계산 모듈 (billing-charge-calculation)

## 개요

Pipeline/Step 기반 요금 계산 프레임워크를 Java 25 + Spring Boot 4.x + MyBatis + Oracle 환경에서 구현한다. 3-jar 컴포넌트 구조(api/impl/internal)를 따르며, Strategy Pattern으로 데이터 접근을 추상화하고, Caffeine 캐시로 기준정보를 관리한다.

## Tasks

- [x] 1. 프로젝트 구조 및 멀티모듈 설정
  - [x] 1.1 Gradle 멀티모듈 프로젝트 생성 (billing-charge-calculation-api, billing-charge-calculation-impl, billing-charge-calculation-internal)
    - 루트 build.gradle에 Java 25, Spring Boot 4.x, 공통 의존성 설정
    - 각 서브모듈 build.gradle에 모듈 간 의존성 설정 (impl → api, internal → api, impl → internal)
    - MyBatis, Caffeine, jqwik, Spring Batch 의존성 추가
    - _Requirements: 18.1, 18.2, 18.3, 18.4_

- [x] 2. 핵심 도메인 모델 및 Enum 정의
  - [x] 2.1 Enum 클래스 생성 (UseCaseType, ProductType, ChargeItemType, ProcessingStatus)
    - api 모듈에 배치 (타 컴포넌트에서 참조 가능)
    - _Requirements: 1.1, 3.2, 15.2_
  - [x] 2.2 요금 계산 결과 모델 생성 (PeriodChargeResult, FlatChargeResult record)
    - api 모듈에 배치
    - PeriodChargeResult: chargeItemCode, chargeItemName, chargeItemType, amount, periodFrom, periodTo, currencyCode, metadata
    - FlatChargeResult: chargeItemCode, chargeItemName, chargeItemType, amount, currencyCode, metadata
    - _Requirements: 15.2, 5.2, 6.2, 7.2_
  - [x] 2.3 요청/응답 DTO 생성 (ChargeCalculationRequest, ChargeCalculationResponse, ContractInfo, ContractChargeResult)
    - api 모듈에 배치
    - _Requirements: 1.1, 1.3_
  - [x] 2.4 ChargeInput, ChargeResult 모델 생성
    - internal 모듈에 배치
    - ChargeInput: subscriptionInfo, suspensionHistories, billingInfo, paymentInfo, prepaidRecords
    - _Requirements: 15.1_
  - [x] 2.5 ChargeContext 클래스 구현
    - internal 모듈에 배치
    - periodResults/flatResults 누적, 유형별 조회, compactPeriodToFlat(), toChargeResult() 구현
    - _Requirements: 15.1, 15.2, 15.3, 3.4_
  - [x]* 2.6 Property 테스트: ChargeContext 결과 누적 및 참조
    - **Property 4: ChargeContext 결과 누적 및 참조**
    - **Validates: Requirements 3.4, 15.3**

- [x] 3. 예외 계층 구조 정의
  - [x] 3.1 예외 클래스 생성
    - ChargeCalculationException (추상 기반 클래스)
    - InvalidRequestException, UnsupportedUseCaseException, PipelineConfigNotFoundException
    - ReferenceDataNotFoundException, StepExecutionException, ProcessingStatusUpdateException
    - _Requirements: 1.5, 1.6, 16.3_

- [x] 4. Checkpoint - 기본 구조 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. 선분이력 교차 처리 유틸리티 구현
  - [x] 5.1 PeriodHistory, IntersectedPeriod 모델 생성
    - internal 모듈에 배치
    - _Requirements: 5.3_
  - [x] 5.2 PeriodIntersectionUtil 구현
    - 경계 시점 수집 → 정렬 → 구간 생성 → 유효 이력 매핑 알고리즘
    - _Requirements: 5.3_
  - [x]* 5.3 Property 테스트: 선분이력 교차 비중첩성
    - **Property 9: 선분이력 교차 처리 결과 비중첩성**
    - **Validates: Requirements 5.3**

- [x] 6. Period → Flat 압축 유틸리티 구현
  - [x] 6.1 PeriodToFlatCompactor 구현
    - PeriodChargeResult 목록을 chargeItemCode별 group by sum하여 FlatChargeResult 목록으로 변환
    - _Requirements: 8.3_
  - [x]* 6.2 Property 테스트: Period→Flat 압축 금액 보존
    - **Property 13: Period → Flat 압축 금액 보존**
    - **Validates: Requirements 8.3**

- [x] 7. Pipeline/Step 프레임워크 구현
  - [x] 7.1 ChargeItemStep 인터페이스 정의
    - internal 모듈에 배치
    - getStepId(), getOrder(), process(ChargeContext), requiresStatusUpdate()
    - _Requirements: 3.1, 3.5, 5.1_
  - [x] 7.2 Pipeline 모델 클래스 생성
    - impl 모듈에 배치
    - pipelineId, steps 리스트, getSteps()
    - _Requirements: 3.1_
  - [x] 7.3 PipelineEngine 구현
    - impl 모듈에 배치
    - Step 목록을 order 순서대로 실행, requiresStatusUpdate() true인 Step 완료 시 처리 상태 DB 갱신
    - StepExecutionException, ProcessingStatusUpdateException 처리
    - _Requirements: 3.3, 16.1, 16.2, 16.3_
  - [x]* 7.4 Property 테스트: Pipeline Step 실행 순서 보장
    - **Property 3: Pipeline Step 실행 순서 보장**
    - **Validates: Requirements 3.3**
  - [x]* 7.5 Property 테스트: 처리 상태 기록 조건부 실행
    - **Property 15: 처리 상태 기록 조건부 실행**
    - **Validates: Requirements 16.1**

- [x] 8. Pipeline 구성 (PipelineConfigurator) 구현
  - [x] 8.1 MyBatis Mapper 생성 (PipelineConfigMapper)
    - PIPELINE_CONFIG, PIPELINE_STEP_CONFIG 테이블 조회
    - internal 모듈에 배치
    - _Requirements: 3.2_
  - [x] 8.2 PipelineConfigurator 인터페이스 및 구현체 (DbPipelineConfigurator) 생성
    - impl 모듈에 인터페이스, internal 모듈에 구현체
    - tenantId + productType + useCaseType 조합으로 Pipeline 구성
    - Spring Bean으로 등록된 ChargeItemStep들을 stepId로 매핑
    - _Requirements: 3.2, 3.5, 3.6, 17.2_
  - [x]* 8.3 Property 테스트: Pipeline 구성의 테넌트/상품유형/유스케이스 결정성
    - **Property 5: Pipeline 구성의 테넌트/상품유형/유스케이스 결정성**
    - **Validates: Requirements 3.2, 17.2**

- [x] 9. Checkpoint - 프레임워크 핵심 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. DataAccessStrategy 구현
  - [x] 10.1 DataAccessStrategy 인터페이스 정의
    - internal 모듈에 배치
    - supportedUseCaseType(), readChargeInput(), writeChargeResult(), updateProcessingStatus()
    - _Requirements: 2.1_
  - [x] 10.2 DataAccessStrategyResolver 구현
    - impl 모듈에 배치
    - UseCaseType으로 적절한 Strategy 구현체 선택
    - _Requirements: 1.2, 2.1_
  - [x] 10.3 RegularBillingStrategy 구현
    - 마스터 테이블 읽기 + 결과 DB 기록 + 처리 상태 갱신
    - _Requirements: 2.2_
  - [x] 10.4 RealtimeQueryStrategy 구현
    - 마스터 테이블 읽기 + 결과 미기록 (writeChargeResult, updateProcessingStatus는 no-op)
    - _Requirements: 2.3_
  - [x] 10.5 EstimateQueryStrategy 구현
    - 접수 테이블 읽기 + 결과 미기록
    - _Requirements: 2.4_
  - [x] 10.6 QuotationQueryStrategy 구현
    - 기준정보만 사용 + 결과 미기록
    - _Requirements: 2.5_
  - [x] 10.7 MyBatis Mapper 생성 (MasterTableMapper, OrderTableMapper, ChargeResultMapper)
    - internal 모듈에 배치
    - _Requirements: 2.2, 2.3, 2.4_
  - [x]* 10.8 Property 테스트: Strategy 선택 일관성
    - **Property 1: Strategy 선택 일관성**
    - **Validates: Requirements 1.2**

- [x] 11. 기준정보 인메모리 캐시 구현
  - [x] 11.1 ReferenceDataKey, ReferenceDataType 모델 생성
    - impl 모듈에 배치
    - _Requirements: 4.1_
  - [x] 11.2 ReferenceDataCache 인터페이스 정의
    - impl 모듈에 배치
    - getReferenceData(), preload(), invalidate(), invalidateAll()
    - _Requirements: 4.1, 4.5_
  - [x] 11.3 CaffeineReferenceDataCache 구현
    - 테넌트별 ConcurrentHashMap<String, Cache> 관리
    - preload: 배치 시작 시 bulk 적재
    - invalidate: OLTP 중 캐시 무효화
    - DB fallback: 캐시 미스 시 DB 조회 후 캐싱
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.6_
  - [x] 11.4 ReferenceDataMapper (MyBatis) 생성
    - selectReferenceData, selectBulkReferenceData
    - internal 모듈에 배치
    - _Requirements: 4.1_
  - [x]* 11.5 Property 테스트: 기준정보 캐시 라운드트립
    - **Property 6: 기준정보 캐시 라운드트립**
    - **Validates: Requirements 4.1**
  - [x]* 11.6 Property 테스트: 캐시 무효화 후 최신 데이터 반환
    - **Property 7: 캐시 무효화 후 최신 데이터 반환**
    - **Validates: Requirements 4.3, 4.4**
  - [x]* 11.7 Property 테스트: 테넌트별 캐시 격리
    - **Property 8: 테넌트별 캐시 격리**
    - **Validates: Requirements 17.3**

- [x] 12. Checkpoint - 인프라 계층 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. 요금 항목 Step 구현 - 원금성 항목
  - [x] 13.1 MonthlyFeeStep 구현 (월정액)
    - PeriodIntersectionUtil을 사용하여 선분이력 교차 처리
    - 구간별 일할 계산, PeriodChargeResult 생성
    - 특이상품 추가 정보 조회 로직 포함
    - requiresStatusUpdate() = true
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - [x]* 13.2 Property 테스트: 월정액 계산 결과 기간 유효성
    - **Property 10: 월정액 계산 결과 기간 유효성**
    - **Validates: Requirements 5.1, 5.2**
  - [x] 13.3 OneTimeFeeStep 구현 (일회성 요금)
    - 다양한 일회성 요금을 추상화된 인터페이스로 처리
    - FlatChargeResult 생성
    - _Requirements: 6.1, 6.2, 6.3_
  - [x] 13.4 UsageFeeStep 구현 (통화료/종량료)
    - 가입정보 + 기준정보 기반 통화료/종량료 계산
    - FlatChargeResult 생성
    - _Requirements: 7.1, 7.2_
  - [x]* 13.5 Property 테스트: 요금 항목 결과 유형 일관성 (원금성)
    - **Property 11: 요금 항목 결과 유형 일관성 (MonthlyFeeStep, OneTimeFeeStep, UsageFeeStep)**
    - **Validates: Requirements 5.2, 6.2, 7.2**

- [x] 14. 요금 항목 Step 구현 - 할인 항목
  - [x] 14.1 PeriodDiscountStep 구현 (할인1 - 기간 존재)
    - ChargeContext에서 PeriodChargeResult 조회, 할인 가입정보/기준정보 기반 할인 계산
    - Period_Charge_Result 형태로 출력
    - _Requirements: 8.1, 8.2_
  - [x] 14.2 PeriodToFlatCompactionStep 구현
    - ChargeContext.compactPeriodToFlat() 호출
    - 할인1 완료 후 기간 정보 제거 + group by sum 압축
    - _Requirements: 8.3_
  - [x] 14.3 FlatDiscountStep 구현 (할인2 - 기간 미존재)
    - ChargeContext에서 FlatChargeResult 조회, 할인 계산
    - FlatChargeResult 형태로 출력
    - _Requirements: 9.1, 9.2_
  - [x]* 14.4 Property 테스트: 할인 금액 상한 불변식
    - **Property 12: 할인 금액 상한 불변식**
    - **Validates: Requirements 8.1, 9.1**

- [x] 15. 요금 항목 Step 구현 - 후처리 항목
  - [x] 15.1 LateFeeStep 구현 (연체가산금)
    - 청구/수납정보 + 이전 계산 결과 기반 연체가산금 계산
    - FlatChargeResult 생성
    - _Requirements: 10.1, 10.2_
  - [x] 15.2 AutoPayDiscountStep 구현 (자동납부할인)
    - 청구/수납정보 + 이전 계산 결과 + 자동납부할인 기준정보 기반 계산
    - FlatChargeResult 생성
    - _Requirements: 11.1, 11.2_
  - [x] 15.3 VatStep 구현 (부가세)
    - 이전 계산 결과 + 과세 기준정보 기반 부가세 계산
    - FlatChargeResult 생성
    - _Requirements: 12.1, 12.2_
  - [x] 15.4 PrepaidOffsetStep 구현 (선납반제)
    - 이전 계산 결과 + 선납내역 기반 선납반제 계산
    - FlatChargeResult 생성
    - _Requirements: 13.1, 13.2_
  - [x] 15.5 SplitBillingStep 구현 (분리과금)
    - 이전 계산 결과 + 분리과금 기준정보 기반 분리과금 처리
    - FlatChargeResult 생성
    - _Requirements: 14.1, 14.2_
  - [x]* 15.6 Property 테스트: 요금 항목 결과 유형 일관성 (후처리)
    - **Property 11: 요금 항목 결과 유형 일관성 (LateFeeStep, AutoPayDiscountStep, VatStep, PrepaidOffsetStep, SplitBillingStep)**
    - **Validates: Requirements 10.2, 11.2, 12.2, 13.2, 14.2**
  - [x]* 15.7 Property 테스트: 분리과금 총액 보존
    - **Property 14: 분리과금 총액 보존**
    - **Validates: Requirements 14.1**

- [x] 16. Checkpoint - 요금 항목 Step 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. 단일 진입점 API 및 통합 연결
  - [x] 17.1 ChargeCalculationService 인터페이스 정의
    - api 모듈에 배치
    - calculate(ChargeCalculationRequest) 메서드
    - _Requirements: 1.1_
  - [x] 17.2 ChargeCalculationServiceImpl 구현
    - impl 모듈에 배치
    - 유효성 검증 (유스케이스 구분 누락, 빈 계약정보 리스트)
    - DataAccessStrategyResolver로 Strategy 결정
    - PipelineConfigurator로 Pipeline 구성
    - 계약정보 건별 처리: 입력 조회 → ChargeContext 생성 → Pipeline 실행 → 결과 저장
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.6, 2.6, 17.1_
  - [x]* 17.3 Property 테스트: 계약정보 건수와 결과 건수 일치
    - **Property 2: 계약정보 건수와 결과 건수 일치**
    - **Validates: Requirements 1.3**
  - [x]* 17.4 단위 테스트: 유효성 검증 오류 케이스
    - 유스케이스 구분 누락 시 InvalidRequestException
    - 빈 계약정보 리스트 시 InvalidRequestException
    - _Requirements: 1.5, 1.6_

- [x] 18. 정기청구 배치 연동
  - [x] 18.1 BatchChargeCalculationProcessor 구현
    - Spring Batch ItemProcessor로 구현
    - chunk 단위 복수 건 계약정보 처리
    - 개별 계약 실패 시 해당 건만 실패 처리, 나머지 계속 진행
    - _Requirements: 1.4_
  - [x]* 18.2 단위 테스트: Spring Batch chunk 단위 처리
    - chunk 내 일부 계약 실패 시 나머지 정상 처리 확인
    - _Requirements: 1.4_

- [x] 19. 멀티테넌시 통합 검증
  - [x]* 19.1 Property 테스트: 테넌트별 요금 계산 정책 격리
    - **Property 16: 테넌트별 요금 계산 정책 격리**
    - **Validates: Requirements 17.1**

- [x] 20. DB 스키마 및 MyBatis 매핑 파일 생성
  - [x] 20.1 Oracle DDL 스크립트 생성
    - PIPELINE_CONFIG, PIPELINE_STEP_CONFIG, CHARGE_PROCESSING_STATUS 테이블
    - _Requirements: 3.2, 16.1_
  - [x] 20.2 MyBatis XML 매핑 파일 생성
    - PipelineConfigMapper.xml, MasterTableMapper.xml, OrderTableMapper.xml
    - ChargeResultMapper.xml, ReferenceDataMapper.xml
    - _Requirements: 2.2, 2.3, 2.4, 4.1_

- [x] 21. Spring 설정 및 컴포넌트 와이어링
  - [x] 21.1 Spring Boot 설정 클래스 생성
    - Caffeine 캐시 설정, MyBatis 설정, 컴포넌트 스캔 설정
    - ChargeItemStep Bean 등록 및 stepId 매핑
    - DataAccessStrategy Bean 등록
    - _Requirements: 4.1, 2.1, 3.5_
  - [x] 21.2 application.yml 설정
    - Oracle 데이터소스, MyBatis 설정, 캐시 설정, Spring Batch 설정
    - _Requirements: 4.2_

- [x] 22. Final Checkpoint - 전체 통합 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- `*` 표시된 태스크는 선택사항이며 빠른 MVP를 위해 건너뛸 수 있습니다
- 각 태스크는 추적성을 위해 구체적인 요구사항을 참조합니다
- Checkpoint에서 점진적 검증을 수행합니다
- Property 테스트는 jqwik 라이브러리를 사용하여 정합성 속성을 검증합니다
- 단위 테스트는 구체적인 예시와 에지 케이스를 검증합니다

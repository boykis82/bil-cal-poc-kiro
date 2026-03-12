# ChargeCalculationService 리팩토링 상세 계획

## 개요

현재 `ChargeCalculationService`의 `calculate()` 메소드는 read, process, write를 모두 포함하고 있습니다.
이를 각각의 메소드로 분리하여 온라인과 배치 환경 모두에서 사용할 수 있도록 리팩토링합니다.

### 목표
- **온라인**: `calculate()` 메소드 호출 → 내부에서 read → process → write 순차 실행
- **배치**: Spring Batch의 ItemReader/Processor/Writer가 각각 `read()`, `process()`, `write()` 직접 호출

---

## 1단계: ChargeCalculationService 인터페이스 수정

**파일**: `/billing-charge-calculation-api/src/main/java/com/billing/charge/calculation/api/ChargeCalculationService.java`

### 수정 전 (현재)
```java
public interface ChargeCalculationService {
    ChargeCalculationResponse calculate(ChargeCalculationRequest request);
}
```

### 수정 후
```java
package com.billing.charge.calculation.api;

import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.dto.ChargeCalculationResponse;
import com.billing.charge.calculation.api.dto.ContractChargeResult;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;

import java.util.List;

/**
 * 요금 계산 단일 진입점 서비스 인터페이스.
 * billing-charge-calculation-api.jar에 위치.
 * 타 컴포넌트는 이 인터페이스를 통해 요금 계산을 요청한다.
 */
public interface ChargeCalculationService {

    /**
     * 데이터 읽기 단계.
     * 배치 ItemReader에서 호출되며, 전체 계약의 ChargeInput을 일괄 로드한다.
     *
     * @param request 요금 계산 요청
     * @return 계약별 ChargeInput 리스트 (요청한 계약 순서대로)
     */
    List<ChargeInput> read(ChargeCalculationRequest request);

    /**
     * 처리 단계.
     * 배치 ItemProcessor에서 호출되며, 단일 ChargeInput에 대해 요금을 계산한다.
     *
     * @param request 요금 계산 요청
     * @param input 단일 계약의 요금 계산 입력 데이터
     * @return 요금 계산 결과
     */
    ChargeResult process(ChargeCalculationRequest request, ChargeInput input);

    /**
     * 쓰기 단계.
     * 배치 ItemWriter에서 호출되며, 계산 결과를 저장한다.
     *
     * @param request 요금 계산 요청
     * @param results 요금 계산 결과 리스트
     */
    void write(ChargeCalculationRequest request, List<ChargeResult> results);

    /**
     * 온라인 요금 계산 (기존 인터페이스 유지).
     * 내부적으로 read → process → write 순서로 실행한다.
     *
     * @param request 요금 계산 요청 (유스케이스 구분, 테넌트ID, 계약정보 리스트 포함)
     * @return 요금 계산 응답 (계약별 계산 결과 리스트)
     */
    default ChargeCalculationResponse calculate(ChargeCalculationRequest request) {
        // 1. 데이터 읽기
        List<ChargeInput> inputs = read(request);

        // 2. 각 입력에 대해 처리
        List<ChargeResult> results = inputs.stream()
                .map(input -> process(request, input))
                .toList();

        // 3. 결과 저장
        write(request, results);

        // 4. 응답 변환
        return buildResponse(results);
    }

    /**
     * ChargeResult 리스트를 ChargeCalculationResponse로 변환.
     *
     * @param results 요금 계산 결과 리스트
     * @return 요금 계산 응답 DTO
     */
    default ChargeCalculationResponse buildResponse(List<ChargeResult> results) {
        List<ContractChargeResult> contractResults = results.stream()
                .map(r -> new ContractChargeResult(r.contractId(), r.chargeResults()))
                .toList();
        return ChargeCalculationResponse.of(contractResults);
    }
}
```

---

## 2단계: ChargeCalculationServiceImpl 리팩토링

**파일**: `/billing-charge-calculation-impl/src/main/java/com/billing/charge/calculation/impl/service/ChargeCalculationServiceImpl.java`

### 전체 코드 (리팩토링 후)

```java
package com.billing.charge.calculation.impl.service;

import com.billing.charge.calculation.api.ChargeCalculationService;
import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.api.dto.ContractInfo;
import com.billing.charge.calculation.api.exception.InvalidRequestException;
import com.billing.charge.calculation.impl.dataloader.DataLoadOrchestrator;
import com.billing.charge.calculation.impl.pipeline.Pipeline;
import com.billing.charge.calculation.impl.pipeline.PipelineConfigurator;
import com.billing.charge.calculation.impl.pipeline.PipelineEngine;
import com.billing.charge.calculation.impl.strategy.DataAccessStrategyResolver;
import com.billing.charge.calculation.internal.context.ChargeContext;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import com.billing.charge.calculation.internal.strategy.DataAccessStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 요금 계산 단일 진입점 서비스 구현체.
 * read/process/write 단계로 분리하여 온라인/배치 양방향 지원.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeCalculationServiceImpl implements ChargeCalculationService {

    private final PipelineConfigurator pipelineConfigurator;
    private final PipelineEngine pipelineEngine;
    private final DataAccessStrategyResolver strategyResolver;
    private final DataLoadOrchestrator dataLoadOrchestrator;

    @Override
    public List<ChargeInput> read(ChargeCalculationRequest request) {
        // 1. 유효성 검증
        validate(request);

        // 2. DataAccessStrategy 결정
        DataAccessStrategy strategy = strategyResolver.resolve(request.getUseCaseType());

        // 3. 계약ID 리스트 추출
        List<String> contractIds = request.getContracts().stream()
                .map(ContractInfo::contractId)
                .toList();

        // 4. 일괄 데이터 로딩
        Map<String, ChargeInput> chargeInputMap = dataLoadOrchestrator.loadAll(
                strategy.getContractBaseLoader(),
                strategy.getChargeItemDataLoaders(),
                contractIds);

        // 5. contractIds 순서대로 ChargeInput 리스트 생성
        // 없는 경우 빈 ChargeInput 생성
        List<ChargeInput> inputs = new ArrayList<>();
        for (String contractId : contractIds) {
            ChargeInput input = chargeInputMap.getOrDefault(
                    contractId,
                    ChargeInput.builder().build());
            inputs.add(input);
        }

        log.info("Data read completed: {} contracts", inputs.size());
        return inputs;
    }

    @Override
    public ChargeResult process(ChargeCalculationRequest request, ChargeInput input) {
        // 1. ChargeInput에서 contractId 추출
        String contractId = extractContractId(input);

        // 2. request에서 해당 ContractInfo 검색
        ContractInfo contractInfo = findContractInfo(request, contractId);

        // 3. DataAccessStrategy 결정
        DataAccessStrategy strategy = strategyResolver.resolve(request.getUseCaseType());

        // 4. Pipeline 구성
        Pipeline pipeline = pipelineConfigurator.configure(
                request.getTenantId(),
                request.getProductType(),
                request.getUseCaseType());

        // 5. ChargeContext 생성
        ChargeContext context = ChargeContext.of(
                request.getTenantId(),
                contractInfo,
                input);

        // 6. Pipeline 실행
        pipelineEngine.execute(pipeline, context, strategy);

        // 7. ChargeResult 반환
        ChargeResult result = context.toChargeResult();

        log.debug("Process completed for contract: {}", contractId);
        return result;
    }

    @Override
    public void write(ChargeCalculationRequest request, List<ChargeResult> results) {
        // 1. DataAccessStrategy 결정
        DataAccessStrategy strategy = strategyResolver.resolve(request.getUseCaseType());

        // 2. 각 결과를 저장 (전략에 따라 no-op 가능)
        for (ChargeResult result : results) {
            strategy.writeChargeResult(result);
        }

        log.info("Write completed: {} results", results.size());
    }

    // === Private 헬퍼 메소드 ===

    /**
     * ChargeInput에서 contractId 추출.
     * SubscriptionInfo가 null인 경우 예외 발생.
     */
    private String extractContractId(ChargeInput input) {
        if (input.getSubscriptionInfo() == null) {
            throw new IllegalArgumentException("ChargeInput must have subscriptionInfo with contractId");
        }
        return input.getSubscriptionInfo().contractId();
    }

    /**
     * request의 contracts 리스트에서 contractId에 해당하는 ContractInfo 검색.
     */
    private ContractInfo findContractInfo(ChargeCalculationRequest request, String contractId) {
        return request.getContracts().stream()
                .filter(c -> c.contractId().equals(contractId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "ContractInfo not found for contractId: " + contractId));
    }

    /**
     * 요청 유효성 검증.
     */
    private void validate(ChargeCalculationRequest request) {
        if (request.getUseCaseType() == null) {
            throw new InvalidRequestException("useCaseType", "유스케이스 구분 값이 누락되었습니다.");
        }
        if (request.getContracts() == null || request.getContracts().isEmpty()) {
            throw new InvalidRequestException("contracts", "계약정보 리스트가 비어 있습니다.");
        }
    }
}
```

### 변경 사항 요약

**삭제**:
- 기존 `calculate()` 메소드 전체 (라인 40-83) → 인터페이스 default 메소드로 이동

**추가**:
- `read()` 메소드: 기존 calculate()의 라인 41-61 로직 + 순서 보장 로직
- `process()` 메소드: 기존 calculate()의 라인 65-79 반복문 내부 로직 (단일 항목 처리)
- `write()` 메소드: 기존 calculate()의 라인 77 결과 저장 로직
- `extractContractId()`: ChargeInput에서 contractId 추출 헬퍼
- `findContractInfo()`: contractId로 ContractInfo 검색 헬퍼

---

## 3단계: Spring Batch 설정 추가

**파일**: `/billing-charge-calculation-impl/src/main/java/com/billing/charge/calculation/impl/config/BatchConfiguration.java` (신규)

```java
package com.billing.charge.calculation.impl.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 활성화 설정.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
}
```

---

## 4단계: Spring Batch 어댑터 구현

### 4-1. ItemReader

**파일**: `/billing-charge-calculation-impl/src/main/java/com/billing/charge/calculation/impl/batch/ChargeCalculationItemReader.java` (신규)

```java
package com.billing.charge.calculation.impl.batch;

import com.billing.charge.calculation.api.ChargeCalculationService;
import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.internal.model.ChargeInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Batch ItemReader 어댑터.
 * ChargeCalculationService.read()를 호출하여 전체 데이터를 로드한 후,
 * 한 건씩 반환한다.
 */
@Slf4j
@Component("chargeCalculationItemReader")
@Scope("step")
public class ChargeCalculationItemReader implements ItemReader<ChargeInput> {

    @Autowired
    private ChargeCalculationService chargeCalculationService;

    private List<ChargeInput> inputs;
    private int currentIndex = 0;

    /**
     * Step 실행 전 전체 데이터 로드.
     * JobParameters에서 request 정보를 가져온다고 가정.
     */
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        // JobParameters에서 request 구성 (실제 구현 시 조정 필요)
        ChargeCalculationRequest request = buildRequestFromJobParameters(stepExecution);

        // 전체 데이터 읽기
        this.inputs = chargeCalculationService.read(request);
        this.currentIndex = 0;

        log.info("ItemReader initialized with {} items", inputs.size());
    }

    @Override
    public ChargeInput read() {
        if (currentIndex < inputs.size()) {
            ChargeInput input = inputs.get(currentIndex);
            currentIndex++;
            return input;
        }
        return null; // 더 이상 데이터가 없음
    }

    /**
     * JobParameters로부터 ChargeCalculationRequest 구성.
     * 실제 구현 시 비즈니스 요구사항에 맞게 조정 필요.
     */
    private ChargeCalculationRequest buildRequestFromJobParameters(StepExecution stepExecution) {
        // TODO: JobParameters에서 추출하여 request 구성
        throw new UnsupportedOperationException("JobParameters로부터 request 구성 로직 필요");
    }
}
```

### 4-2. ItemProcessor

**파일**: `/billing-charge-calculation-impl/src/main/java/com/billing/charge/calculation/impl/batch/ChargeCalculationItemProcessor.java` (신규)

```java
package com.billing.charge.calculation.impl.batch;

import com.billing.charge.calculation.api.ChargeCalculationService;
import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Spring Batch ItemProcessor 어댑터.
 * ChargeCalculationService.process()를 호출하여 요금을 계산한다.
 */
@Slf4j
@Component("chargeCalculationItemProcessor")
@Scope("step")
public class ChargeCalculationItemProcessor implements ItemProcessor<ChargeInput, ChargeResult> {

    @Autowired
    private ChargeCalculationService chargeCalculationService;

    private ChargeCalculationRequest request;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        // JobParameters에서 request 구성
        this.request = buildRequestFromJobParameters(stepExecution);
    }

    @Override
    public ChargeResult process(ChargeInput input) throws Exception {
        return chargeCalculationService.process(request, input);
    }

    private ChargeCalculationRequest buildRequestFromJobParameters(StepExecution stepExecution) {
        // TODO: JobParameters에서 추출하여 request 구성
        throw new UnsupportedOperationException("JobParameters로부터 request 구성 로직 필요");
    }
}
```

### 4-3. ItemWriter

**파일**: `/billing-charge-calculation-impl/src/main/java/com/billing/charge/calculation/impl/batch/ChargeCalculationItemWriter.java` (신규)

```java
package com.billing.charge.calculation.impl.batch;

import com.billing.charge.calculation.api.ChargeCalculationService;
import com.billing.charge.calculation.api.dto.ChargeCalculationRequest;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring Batch ItemWriter 어댑터.
 * ChargeCalculationService.write()를 호출하여 결과를 저장한다.
 */
@Slf4j
@Component("chargeCalculationItemWriter")
@Scope("step")
public class ChargeCalculationItemWriter implements ItemWriter<ChargeResult> {

    @Autowired
    private ChargeCalculationService chargeCalculationService;

    private ChargeCalculationRequest request;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        // JobParameters에서 request 구성
        this.request = buildRequestFromJobParameters(stepExecution);
    }

    @Override
    public void write(List<? extends ChargeResult> items) throws Exception {
        chargeCalculationService.write(request, (List<ChargeResult>) items);
    }

    private ChargeCalculationRequest buildRequestFromJobParameters(StepExecution stepExecution) {
        // TODO: JobParameters에서 추출하여 request 구성
        throw new UnsupportedOperationException("JobParameters로부터 request 구성 로직 필요");
    }
}
```

---

## 5단계: Job/Step 정의

**파일**: `/billing-charge-calculation-impl/src/main/java/com/billing/charge/calculation/impl/batch/ChargeCalculationBatchJob.java` (신규)

```java
package com.billing.charge.calculation.impl.batch;

import com.billing.charge.calculation.internal.model.ChargeInput;
import com.billing.charge.calculation.internal.model.ChargeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 요금 계산 배치 Job 정의.
 */
@Configuration
@RequiredArgsConstructor
public class ChargeCalculationBatchJob {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ChargeCalculationItemReader itemReader;
    private final ChargeCalculationItemProcessor itemProcessor;
    private final ChargeCalculationItemWriter itemWriter;

    @Bean
    public Job billingChargeCalculationJob() {
        return jobBuilderFactory.get("billingChargeCalculationJob")
                .incrementer(new RunIdIncrementer())
                .start(billingChargeCalculationStep())
                .build();
    }

    @Bean
    public Step billingChargeCalculationStep() {
        return stepBuilderFactory.get("billingChargeCalculationStep")
                .<ChargeInput, ChargeResult>chunk(100) // 청크 크기 100
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }
}
```

---

## 6단계: application.yml 수정

**파일**: `/billing-charge-calculation-impl/src/main/resources/application.yml`

### 수정 전
```yaml
spring:
  batch:
    jdbc:
      initialize-schema: never
    job:
      enabled: false
```

### 수정 후
```yaml
spring:
  batch:
    jdbc:
      initialize-schema: never
    job:
      enabled: true  # false에서 true로 변경
```

---

## 핵심 설계 결정사항

### 1. Strategy/Pipeline 재계산 이슈

**현재 방식**: `process()`와 `write()`에서 매번 resolve/configure 수행

**장점**:
- Stateless 유지
- Thread-safe
- 구현 단순

**단점**:
- 성능 오버헤드 (캐싱되지 않음)
- 동일한 연산 반복

**향후 최적화 옵션**:
- ThreadLocal에 캐싱
- StepExecutionContext에 저장
- Spring Cache 활용
- @StepScope bean으로 상태 공유

### 2. ContractInfo 검색

**현재 구조**:
- ChargeInput에는 SubscriptionInfo.contractId만 존재
- `process()`에서 request.getContracts()를 순회하여 매칭

**성능 개선 방안**:
- Map<String, ContractInfo> 캐싱
- @BeforeStep에서 Map 생성 후 재사용

### 3. 순서 보장

- `read()`가 contractIds 순서로 List<ChargeInput> 반환
- Spring Batch chunk 처리 시에도 순서 유지됨
- 결과 저장 시 순서 보장

### 4. 빈 데이터 처리

- chargeInputMap에 없는 contractId는 빈 ChargeInput 생성
- 기존 로직과 동일한 처리 방식 유지

### 5. 트랜잭션 경계

**온라인 (calculate)**:
- 전체 프로세스가 하나의 트랜잭션 (필요 시 @Transactional 추가)

**배치**:
- Chunk 단위 트랜잭션
- 100건씩 커밋 (설정 가능)
- 일부 실패 시 해당 chunk만 롤백

---

## 사용 시나리오

### 온라인 호출 (변경 없음)

```java
// 기존 코드 그대로 사용 가능
ChargeCalculationRequest request = ChargeCalculationRequest.builder()
    .tenantId("TENANT_001")
    .useCaseType(UseCaseType.REGULAR_BILLING)
    .productType(ProductType.WIRELESS)
    .contracts(contractInfoList)
    .build();

ChargeCalculationResponse response = chargeCalculationService.calculate(request);
```

### 배치 실행

```java
// JobParameters 구성
JobParameters params = new JobParametersBuilder()
    .addString("tenantId", "TENANT_001")
    .addString("useCaseType", "REGULAR_BILLING")
    .addString("productType", "WIRELESS")
    .addLong("timestamp", System.currentTimeMillis())
    .toJobParameters();

// Job 실행
JobExecution execution = jobLauncher.run(billingChargeCalculationJob, params);
```

### 배치에서 개별 메소드 직접 호출 (선택적)

```java
// Reader에서
List<ChargeInput> inputs = chargeCalculationService.read(request);

// Processor에서
for (ChargeInput input : inputs) {
    ChargeResult result = chargeCalculationService.process(request, input);
    results.add(result);
}

// Writer에서
chargeCalculationService.write(request, results);
```

---

## 구현 순서

1. **1단계**: ChargeCalculationService 인터페이스 수정
   - read/process/write 메소드 추가
   - calculate() default 메소드로 변경

2. **2단계**: ChargeCalculationServiceImpl 리팩토링
   - 기존 calculate() 로직 분리
   - read/process/write 구현
   - 헬퍼 메소드 추가

3. **테스트**: 온라인 호출 동작 확인
   - 기존 calculate() 사용 코드 정상 동작 확인
   - 단위 테스트 작성

4. **3단계**: Spring Batch 설정 추가
   - BatchConfiguration 생성

5. **4단계**: Batch 어댑터 구현
   - ItemReader/Processor/Writer 구현

6. **5단계**: Job/Step 정의
   - ChargeCalculationBatchJob 생성

7. **6단계**: application.yml 수정

8. **통합 테스트**: 배치 실행 테스트
   - Job 실행 확인
   - 결과 검증

---

## 주의사항

### 하위 호환성
- 기존 `calculate()` 사용 코드는 수정 없이 그대로 동작
- 인터페이스 변경이지만 default 메소드이므로 하위 호환
- 기존 테스트 코드 영향 없음

### 성능 고려사항
- 대량 데이터 처리 시 청크 크기 조정 (기본 100)
- 메모리 사용량 모니터링
- Strategy/Pipeline 재계산 오버헤드

### 에러 처리
- 배치 실패 시 retry/skip 정책 추가 검토
- 온라인 호출 시 트랜잭션 처리 확인

### TODO 항목
- JobParameters → ChargeCalculationRequest 변환 로직 구현
- 성능 최적화 (Strategy/Pipeline 캐싱)
- ContractInfo Map 캐싱
- 배치 모니터링 추가
- 에러 핸들링 강화

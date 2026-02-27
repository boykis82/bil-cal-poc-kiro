-- ============================================================
-- 요금 계산 모듈 Oracle DDL 스크립트
-- ============================================================

-- Pipeline 구성 마스터 테이블
-- 테넌트ID, 상품유형, 유스케이스 조합별 Pipeline 구성을 관리한다.
CREATE TABLE PIPELINE_CONFIG (
    PIPELINE_CONFIG_ID  VARCHAR2(36)  PRIMARY KEY,
    TENANT_ID           VARCHAR2(20)  NOT NULL,
    PRODUCT_TYPE        VARCHAR2(20)  NOT NULL,
    USE_CASE_TYPE       VARCHAR2(20)  NOT NULL,
    DESCRIPTION         VARCHAR2(200),
    ACTIVE_YN           CHAR(1)       DEFAULT 'Y',
    CONSTRAINT UK_PIPELINE_CONFIG UNIQUE (TENANT_ID, PRODUCT_TYPE, USE_CASE_TYPE)
);

-- Pipeline Step 구성 상세 테이블
-- Pipeline에 포함될 Step 목록과 실행 순서를 관리한다.
CREATE TABLE PIPELINE_STEP_CONFIG (
    PIPELINE_CONFIG_ID  VARCHAR2(36)  NOT NULL,
    STEP_ID             VARCHAR2(50)  NOT NULL,
    STEP_ORDER          NUMBER(3)     NOT NULL,
    ACTIVE_YN           CHAR(1)       DEFAULT 'Y',
    CONSTRAINT PK_PIPELINE_STEP PRIMARY KEY (PIPELINE_CONFIG_ID, STEP_ID),
    CONSTRAINT FK_PIPELINE_STEP FOREIGN KEY (PIPELINE_CONFIG_ID)
        REFERENCES PIPELINE_CONFIG(PIPELINE_CONFIG_ID)
);

-- 요금 항목 처리 상태 테이블
-- Step 완료 시 처리 상태를 기록하여 중복 처리 방지 및 이력 추적에 사용한다.
CREATE TABLE CHARGE_PROCESSING_STATUS (
    PROCESSING_ID       VARCHAR2(36)  PRIMARY KEY,
    CONTRACT_ID         VARCHAR2(36)  NOT NULL,
    STEP_ID             VARCHAR2(50)  NOT NULL,
    STATUS              VARCHAR2(20)  NOT NULL,
    PROCESSED_AT        TIMESTAMP     DEFAULT SYSTIMESTAMP,
    ERROR_MESSAGE       VARCHAR2(4000),
    CONSTRAINT UK_PROCESSING UNIQUE (CONTRACT_ID, STEP_ID)
);

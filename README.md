# Let'z Collab: 프로젝트 협업 관리 플랫폼

---------
<img src="./logo.png" alt="Let'z Collab Logo" width="350">

> 워크스페이스 단위로 조직을 관리하고, 프로젝트와 업무를 체계적으로 추적하는 협업 솔루션

---

## 1. 프로젝트 개요

**Let'z Collab**은 현대적인 조직이 겪는 협업의 파편화 문제를 해결하기 위해 설계된 플랫폼입니다. 단순한 업무 관리를 넘어, **워크스페이스 - 프로젝트 - 업무 - 댓글**로 이어지는 계층적인 데이터
구조를 기반으로 조직/팀 단위의 대규모 협업을 안정적으로 지원합니다.

본 프로젝트는 엔터프라이즈 급 백엔드 아키텍처를 구축하고, 안전한 인증 시스템 및 역할 기반의 권한 제어(RBAC)를 구현하는 데 초점을 맞추었습니다. 네이버 클라우드 플랫폼(NCP)과 Github Actions를
사용하여 CI/CD 파이프라인을 직접 구축하고 실제 운영 환경과 동일한 배포 프로세스를 경험했습니다.

- **서비스 URL**: https://letzcollab.xyz/
- **API 명세서 (Swagger)**: https://letzcollab.xyz/api/swagger-ui/index.html

---

## 2. 기술 스택

| 구분       | 기술                                  |
|----------|-------------------------------------|
| Backend  | Java 21, Spring Boot 3.5            |
| Frontend | React 19.2, Vite 7.3                |
| Database | PostgreSQL                          |
| Mail     | AWS SES                             |
| Infra    | Naver Cloud Platform, Docker, NGINX |
| SSL      | Let's Encrypt / Certbot             |
| CI/CD    | GitHub Actions                      |

### 2.1 백엔드 라이브러리

| 라이브러리             | 버전       | 용도             |
|-------------------|----------|----------------|
| Spring Data JPA   | 3.5.8    | ORM / DB 연동    |
| Hibernate         | 6.6.41   | ORM 구현체        |
| Spring Security   | 6.5.7    | 인증 / 인가        |
| Spring Validation | 8.0.3    | 요청 데이터 검증      |
| Spring Mail       | 2.0.5    | 이메일 발송         |
| Thymeleaf         | 3.1.3    | 이메일 HTML 템플릿   |
| Spring Retry      | 2.0.12   | 재시도 처리         |
| AspectJ (AOP)     | 1.9.25.1 | AOP            |
| QueryDSL          | 5.1.0    | 동적 쿼리          |
| jjwt              | 0.12.6   | JWT 생성 및 검증    |
| springdoc-openapi | 2.8.5    | Swagger UI     |
| Lombok            | 1.18.42  | 보일러플레이트 코드 제거  |
| p6spy             | 3.9.1    | SQL 로깅 (개발 환경) |
| PostgreSQL Driver | 42.7.9   | DB 드라이버        |

### 2.2 프론트엔드 라이브러리

| 라이브러리             | 버전      | 용도                |
|-------------------|---------|-------------------|
| React             | 19.2.0  | UI 프레임워크          |
| React Router DOM  | 7.13.0  | 클라이언트 사이드 라우팅     |
| TanStack Query    | 5.90.21 | 서버 상태 관리 / 데이터 패칭 |
| Axios             | 1.13.5  | HTTP 클라이언트        |
| Ant Design        | 6.3.0   | UI 컴포넌트 라이브러리     |
| @ant-design/icons | 6.1.0   | 아이콘               |

### 2.3 로컬 실행 방법

#### 사전 요구사항

- Java 21+
- Node.js 20+
- Docker
- SMTP 서버 (AWS SES 권장)
    - 다른걸 쓰면 `application.yml`에서 `host` 부분을 해당 SMTP 서버 주소로 바꿔야합니다.

#### 1. 환경 변수 설정

`letzcollab-backend/.env` 파일을 생성하고 아래 값을 채워주세요.

```env
# AWS SES SMTP 계정
EMAIL_USERNAME=
EMAIL_PASSWORD=

# JWT 설정 
JWT_ENC_KEY= (Base64 인코딩된 시크릿 키, 길이는 256비트 이상)
JWT_ACCESS_EXP_TIME= (ms로 유효기간 설정)

# PostgreSQL 계정
DB_USER=
DB_PASSWORD=
DB_NAME=
```

#### 2. 백엔드 실행

```bash
cd letzcollab-backend
docker-compose up -d            # PostgreSQL 컨테이너 실행

export $(cat .env | xargs)      # .env 환경변수 현재 셸에 주입
./gradlew bootRun --args='--spring.profiles.active=local'
```

> Windows(PowerShell)의 경우 `.env` 파일의 각 항목을 `$env:KEY=VALUE` 형식으로 직접 설정해주세요

### 3. 프론트엔드 실행

```bash
cd letzcollab-frontend
npm install
npm run dev
```

프론트엔드는 `http://localhost:5173`, 백엔드 API는 `http://localhost:8080`에서 실행됩니다.

---

## 3. 시스템 아키텍처 및 구현 디테일

### 3.1. 아키텍처 다이어그램

<img width="4252" height="942" alt="Image" src="https://github.com/user-attachments/assets/7c158978-55ff-486f-a080-d5a597070e1a" />

NGINX를 리버스 프록시로 설정하여, 리액트 정적 파일 서빙과 `/api/*` 백엔드 라우팅을 분리했습니다.
<br> Let's Encrypt + Certbot으로 SSL 인증서를 발급하여 NGINX 서버에 HTTPS를 적용했습니다.

### 3.2. 논리적 데이터 아키텍처 및 권한 모델

Let'z Collab은 세밀한 역할 기반 권한 제어(RBAC)를 통해 보안을 강화했습니다.

1. **계층 구조**: **Workspace** > Projects > Tasks > Comments
    - 프로젝트는 공개/비공개로 나뉩니다.
        - 공개 프로젝트는 해당 워크스페이스에 속한 멤버들이 모두 조회 가능.
        - 비공개 프로젝트는 해당 프로젝트 멤버로 추가된 워크스페이스 멤버만 접근 가능.
2. **권한 모델 (Role-Based Access Control)**:
    - **Workspace Member**:
        - `OWNER`: 최상위 워크스페이스 소유자 (프로젝트 생성 가능)
        - `ADMIN`: 워크스페이스 관리자 (프로젝트 생성 가능)
        - `MEMBER`: 일반 참여자
        - `GUEST`: 조회 중심(+ 댓글 작성) 외부 협력자
    - **Project Member**:
        - `ADMIN`: 프로젝트 관리자 (프로젝트 관리, 멤버 추가/강퇴, 모든 업무 관리)
            - 프로젝트 리더도 `ADMIN` 권한을 갖지만, `Project` 엔티티에 `User leader` 필드로 관리되어, 일반 `ADMIN` 권한을 갖는 멤버보다 더 높은 권한을 가집니다.
        - `MEMBER`: 일반 프로젝트 참여자 (업무 생성, 상태 일부 수정 등 실무 수행)
        - `VIEWER`: 프로젝트 조회자 (+ 댓글 허용)

#### 워크스페이스 권한

| 기능                       | OWNER | ADMIN | MEMBER | GUEST |
|:-------------------------|:-----:|:-----:|:------:|:-----:|
| 워크스페이스 수정 / 삭제 / 소유권 이전  |   ✅   |   ❌   |   ❌    |   ❌   |
| 멤버 초대                    |   ✅   |   ✅   |   ❌    |   ❌   |
| 타 멤버 역할 수정 (자신보다 낮은 권한만) |   ✅   |   ✅   |   ❌    |   ❌   |
| 타 멤버 강퇴 (자신보다 낮은 권한만)    |   ✅   |   ✅   |   ❌    |   ❌   |

---

#### 프로젝트 권한

> 프로젝트 생성은 워크스페이스 `OWNER` / `ADMIN`만 가능하며, 생성자는 자동으로 프로젝트 리더 + `ProjectRole.ADMIN` 권한을 가집니다.

| 기능                | 리더 | ADMIN | MEMBER | VIEWER |
|:------------------|:--:|:-----:|:------:|:------:|
| 프로젝트 삭제 / 리더 변경   | ✅  |   ❌   |   ❌    |   ❌    |
| 공개 / 비공개 여부 수정    | ✅  |   ❌   |   ❌    |   ❌    |
| 프로젝트 정보 수정        | ✅  |   ✅   |   ❌    |   ❌    |
| 멤버 초대 / 강퇴 / 수정   | ✅  |  ⚠️*  |   ❌    |   ❌    |

> *일반 `ADMIN`은 자신보다 낮은 권한의 멤버만 강퇴 / 역할 수정 가능. `ADMIN` 권한 부여는 리더만 가능.

---

#### 업무 권한

| 기능               |      ADMIN      |                       MEMBER                        | VIEWER |
|:-----------------|:---------------:|:---------------------------------------------------:|:------:|
| 최상위 업무 생성        |        ✅        |            ⚠️ (본인 또는 다른 MEMBER에게만 할당 가능)            |   ❌    |
| 하위 업무 생성         |        ✅        |   ⚠️ (보고자 또는 담당자인 경우만, 본인 또는 다른 MEMBER에게만 할당 가능)    |   ❌    |
| 업무 수정 (전체 필드)    |        ✅        |                    ⚠️ (보고자인 경우만)                    |   ❌    |
| 업무 수정 (상태만)      |        ✅        | ⚠️ (담당자인 경우, `TODO` / `IN_PROGRESS` / `IN_REVIEW`만) |   ❌    |
| 업무 삭제            |        ✅        |                    ⚠️ (보고자인 경우만)                    |   ❌    |
| 업무 조회 (비공개 프로젝트) |        ✅        |                          ✅                          |   ✅    |
| 업무 조회 (공개 프로젝트)  | 워크스페이스 전체 멤버 가능 |                                                     |        |

> - 보고자는 항상 업무 생성 요청자로 고정, 담당자로 자기 자신 할당 가능   
> - `VIEWER`를 담당자로 지정 불가

---

#### 업무 댓글 권한

| 기능         | 비공개 프로젝트 |   공개 프로젝트    |
|:-----------|:--------:|:------------:|
| 댓글 작성 및 조회 | 프로젝트 멤버만 | 워크스페이스 전체 멤버 |
| 댓글 수정 및 삭제 | 작성자 본인만  |   작성자 본인만    |

---

### 3.3. 핵심 구현 사항

#### A. FETCH JOIN으로 쿼리 수 최적화 및 N+1 방지

- 호출 맥락별로 최적화된 FETCH JOIN 쿼리 메서드를 작성하여 불필요한 쿼리를 제거했습니다.
- `default_batch_fetch_size: 100` 설정으로 2개 이상 컬렉션이 있을 때 하나는 FETCH JOIN으로 나머지는 IN절 배치로 처리하여 쿼리 수를 최소화했습니다.

---

#### B. 플랫폼 내부 알림 시스템 구현 (이벤트 + 스케줄러)

알림은 두 가지 경로로 생성됩니다.

1. **사용자의 행동(업무 할당, 댓글 작성 등)으로 발생하는 알림**
    - 이벤트 발행 → `@Async` + `@TransactionalEventListener` 이벤트 리스너가 알림을 DB에 저장
2. **스케줄러가 매일 새벽 3시에 배치로 생성하는 알림**
    - (마감 1일 전 업무 + 마감 초과 미완료 업무)에 대한 알림을 생성
    - 담당자/보고자가 다른 경우 양쪽 모두에게 발송

> **알림 정리 스케줄러** (매일 새벽 4시): 읽음 처리 후 30일 경과한 알림을 벌크 삭제하여 테이블 비대화 방지

---

#### C. 복잡한 동적 쿼리 최적화 (QueryDSL)

프로젝트/업무 목록처럼 다중 필터 조건이 붙는 검색에 JPQL 문자열 조합 대신 QueryDSL을 적용했습니다.

- 각 필터 조건(상태, 우선순위, 담당자, 마감일 범위 등)을 `BooleanExpression`을 반환하는 메서드로 분리
- 동적 정렬은 `PathBuilder` 기반 `OrderSpecifier`로 허용 필드(`name`, `createdAt`, `status`)만 화이트리스트로 제한 → SQL Injection 및 의도치 않은 정렬
  방지
- 프로젝트 상세 조회 시 리더 유저, 프로젝트 멤버, 멤버별 유저 정보를 FETCH JOIN으로 단 1개의 쿼리로 처리하며, 접근 권한 검증도 `where`절에서 함께 수행

---

#### D. 보안 및 인증 (Spring Security + JWT + Audit)

1. **웹/모바일 듀얼 클라이언트 인증 전략**
    - **로그인 시** : `X-Client-Type` 헤더 값에 따라 JWT 전달 방식 분기
        - `web` → `HttpOnly` + `SameSite=Lax` + `Secure` 쿠키로 전달 (React 대응)
            - JS 접근 차단(XSS 방어), CSRF 방어, HTTPS 환경에서만 전송
        - 그 외 → 응답 본문으로 전달 (모바일 앱 대응)
    - **로그인 후** : 요청마다 클라이언트 유형에 맞는 방식으로 JWT 전달
        - `web` → 브라우저가 쿠키(`accessToken`)를 자동으로 요청에 포함
        - 그 외 → `Authorization: Bearer ...` 헤더로 전달

2. **커스텀 Security 컴포넌트 (`JwtAuthenticationEntryPoint`, `AuthErrorHandler`)**
    - Spring Security 기본 설정은 미인증 요청 시 403을 반환하는데, REST API에서는 401이 맞으므로 `AuthenticationEntryPoint`를 직접 구현해 필터 체인에 등록
    - JWT 예외(`JwtException` 등)는 `DispatcherServlet` 앞단의 필터 계층에서 발생하기 때문에 `@RestControllerAdvice`가 잡지 못함
    - 두 경우 모두 `AuthErrorHandler`를 통해 `HttpServletResponse`에 공통 `ApiResponse<T>` JSON을 직접 출력하여 일관된 에러 응답 형식 유지

3. **DB 조회 없는 인증 복원**
    - JWT 클레임(`publicId`, `email`, `role`)을 파싱해 `CustomUserDetails` 객체를 즉시 생성
    - `CustomUserDetailsService`(DB 조회)는 로그인 최초 자격증명 검증 시에만 호출 → 매 요청마다 발생하던 불필요한 DB 쿼리 제거

4. **`UserStatus` enum 기반 계정 상태 관리**
    - `ACTIVE`, `PENDING`(이메일 미인증), `BANNED`, `DELETED` 4가지 상태를 enum으로 관리
    - 각 상태마다 `enabled`, `notLocked` boolean 필드 값을 조합하여 `CustomUserDetails`의 `isEnabled()`, `isAccountNonLocked()` 메소드를
      구현

5. **Custom AuditorAware**
    - `SecurityContextHolder`에서 현재 사용자의 `publicId`(UUID)를 꺼내 `@CreatedBy`, `@LastModifiedBy`에 자동 기록

---

#### E. UUID publicId / Long id 이중 키 전략

- 내부 PK: `Long id` → 테이블 JOIN 성능 확보
- 외부 노출: `UUID publicId` → API URL 및 응답 바디에만 사용
    - `@PrePersist`로 영속화 직전 자동 생성, `@Column(updatable = false, unique = true)`로 불변성 보장
    - PK 순번 노출로 인한 데이터 규모 추정 및 IDOR(Insecure Direct Object Reference) 취약점 방지

---

#### F. AWS SES 기반 비동기 이메일 서비스

> ⚠️ 현재 AWS SES가 샌드박스 모드로 운영 중이므로, 이메일은 사전에 등록된 주소로만 수신 가능합니다.

- `EmailContext` 인터페이스(`getTemplateName()`, `getSubject()`, `getVariables()`)를 전략 패턴으로 설계
    - `VerifyEmailContext`, `PasswordResetEmailContext` 등: 각 이메일 유형을 record로 작성하고 인터페이스를 `EmailContext` 구현
    - 새 이메일 유형 추가 시 서비스 코드 수정 없이 `EmailContext` 구현체만 추가하면 됨 → OCP 준수
- `@Async` + `@Retryable` + `@TransactionalEventListener` 조합으로 비동기 처리 및 재시도 메커니즘 적용
    - 발송 실패 시 지수 백오프(2s → 4s)로 최대 3회 재시도, 전부 실패 시 `@Recover`에서 에러 로그 기록

---

#### G. 공통 인프라 설계

1. **3단계 베이스 엔티티 계층**
    - `DateBaseEntity`(createdAt, updatedAt) → `PublicIdAndDateBaseEntity`(+ publicId) →
      `PublicIdAndFullAuditBaseEntity`(+ createdBy, updatedBy)
    - 엔티티 성격에 맞는 수준의 베이스 엔티티만 선택적으로 상속

2. **일관된 API 응답 구조**
    - `ApiResponse<T>`로 `success`, `message`, `data`, `errorCode`, `timestamp`를 항상 포함
    - 팩토리 메서드 오버로딩으로 상황에 맞게 사용, 프론트엔드는 `success` 플래그 하나로 분기 가능

3. **도메인별 ErrorCode enum**
    - `C`(공통), `A`(인증), `U`(유저), `W`(워크스페이스), `P`(프로젝트), `T`(업무) 등 접두사로 분류
    - 보안상 민감한 케이스는 의도적으로 동일 에러코드 반환 → 정보 노출 차단 (예: `W001 WORKSPACE_NOT_FOUND_OR_ACCESS_DENIED`)

4. **GlobalExceptionHandler**
    - `CustomException`, Spring Security 예외, `@Valid` 검증 예외, JSON 파싱 오류 등을 계층별로 전부 커버
    - 검증 실패 시 `List<ValidationError>`를 `data` 필드에 담아 프론트엔드가 필드별 에러 처리 가능

---

#### H. Soft Delete

- 워크스페이스, 프로젝트 등 핵심 엔티티 삭제 시, 바로 DB에서 삭제하는 대신 테이블의 `deleted_at` 컬럼으로 삭제 상태를 표시
    - 엔티티에 `@SQLRestriction("deleted_at IS NULL")`을 사용해서 soft delete 처리된 행은 조회되지 않도록 처리
- 데이터 복구 가능성을 열어두고, 연관된 프로젝트, 업무 등의 내역이 외래 키 제약으로 유실되지 않도록 방지

---

#### I. CI/CD 파이프라인 (GitHub Actions + Docker + 네이버 클라우드)

- `main` 브랜치 push를 트리거로 3단계 파이프라인 자동 실행
    1. **Test** : `spring.profiles.active=test` 프로파일로 전체 테스트 실행, 실패 시 배포 중단
    2. **Build & Push** : 백엔드/프론트엔드 Docker 이미지를 빌드하여 GHCR(GitHub Container Registry)에 push
    3. **Deploy** : NCP 서버에 SSH로 접속 후 `docker-compose.prod.yml`로 이미지 pull → 컨테이너 재시작 → 미사용 이미지 정리

- 민감한 정보(`DB 계정`, `JWT 시크릿` 등)는 GitHub Secrets의 `ENV_FILE`로 관리하여 서버에 있는 `.env` 파일에 주입
- `application-prod.yml`에서 운영 환경 설정 분리
    - p6spy 로깅 비활성화, `ddl-auto: update` 전환, DB 연결은 Docker 내부 네트워크 서비스명으로 통신

---

## 4. ERD

<img width="2138" height="2514" alt="Image" src="https://github.com/user-attachments/assets/6838449a-6db3-4dee-b4d8-4c50b20df050" />

---

## 5. API

> Base URL: `/api/v1` | 인증: JWT Bearer Token 또는 HttpOnly Cookie (웹)

### Auth

| 메소드  | Endpoint                       | 설명              | 인증 |
|------|--------------------------------|-----------------|----|
| POST | `/auth/signup`                 | 회원가입            | ❌  |
| POST | `/auth/login`                  | 로그인             | ❌  |
| POST | `/auth/logout`                 | 로그아웃            | 🔒 |
| POST | `/auth/verify-email`           | 이메일 인증          | ❌  |
| POST | `/auth/verify-email/resend`    | 이메일 인증 재발송      | ❌  |
| POST | `/auth/password/reset-request` | 비밀번호 초기화 이메일 요청 | ❌  |
| POST | `/auth/password/reset`         | 비밀번호 초기화 수행     | ❌  |

### User

| 메소드    | Endpoint    | 설명                  | 인증 |
|--------|-------------|---------------------|----|
| GET    | `/users/me` | 내 정보 조회             | 🔒 |
| PATCH  | `/users/me` | 내 정보 수정 (이름, 전화번호)  | 🔒 |
| DELETE | `/users/me` | 회원 탈퇴 (Soft Delete) | 🔒 |

### Workspace

| 메소드    | Endpoint                          | 설명                           | 인증 |
|--------|-----------------------------------|------------------------------|----|
| POST   | `/workspaces`                     | 워크스페이스 생성                    | 🔒 |
| GET    | `/workspaces`                     | 내 워크스페이스 목록 조회               | 🔒 |
| GET    | `/workspaces/{workspacePublicId}` | 워크스페이스 상세 조회 (멤버, 소유자 정보 포함) | 🔒 |
| PATCH  | `/workspaces/{workspacePublicId}` | 워크스페이스 수정                    | 🔒 |
| DELETE | `/workspaces/{workspacePublicId}` | 워크스페이스 삭제 (Soft Delete)      | 🔒 |

### Workspace Member

| 메소드    | Endpoint                                                                      | 설명            | 인증 |
|--------|-------------------------------------------------------------------------------|---------------|----|
| POST   | `/workspaces/{workspacePublicId}/invitations`                                 | 멤버 초대 이메일 발송  | 🔒 |
| POST   | `/workspaces/invitations/accept`                                              | 초대 수락         | 🔒 |
| PATCH  | `/workspaces/{workspacePublicId}/members/me`                                  | 본인 직책 수정      | 🔒 |
| PATCH  | `/workspaces/{workspacePublicId}/members/{memberPublicId}`                    | 타 멤버 권한/직책 수정 | 🔒 |
| DELETE | `/workspaces/{workspacePublicId}/members/me`                                  | 워크스페이스 자진 탈퇴  | 🔒 |
| DELETE | `/workspaces/{workspacePublicId}/members/{memberPublicId}`                    | 멤버 강퇴         | 🔒 |
| POST   | `/workspaces/{workspacePublicId}/members/{memberPublicId}/transfer-ownership` | 소유권 이전        | 🔒 |

### Project

| 메소드    | Endpoint                                                     | 설명                        | 인증 |
|--------|--------------------------------------------------------------|---------------------------|----|
| POST   | `/workspaces/{workspacePublicId}/projects`                   | 프로젝트 생성                   | 🔒 |
| GET    | `/workspaces/{workspacePublicId}/projects`                   | 프로젝트 목록 조회 (필터 + 페이징)     | 🔒 |
| GET    | `/workspaces/{workspacePublicId}/projects/{projectPublicId}` | 프로젝트 상세 조회 (멤버, 리더 정보 포함) | 🔒 |
| PATCH  | `/workspaces/{workspacePublicId}/projects/{projectPublicId}` | 프로젝트 수정                   | 🔒 |
| DELETE | `/workspaces/{workspacePublicId}/projects/{projectPublicId}` | 프로젝트 삭제 (Soft Delete)     | 🔒 |

### Project Member

| 메소드    | Endpoint                                                                                                | 설명            | 인증 |
|--------|---------------------------------------------------------------------------------------------------------|---------------|----|
| POST   | `/workspaces/{workspacePublicId}/projects/{projectPublicId}/members`                                    | 멤버 추가         | 🔒 |
| PATCH  | `/workspaces/{workspacePublicId}/projects/{projectPublicId}/members/me`                                 | 본인 직책 수정      | 🔒 |
| PATCH  | `/workspaces/{workspacePublicId}/projects/{projectPublicId}/members/{memberPublicId}`                   | 타 멤버 역할/직책 수정 | 🔒 |
| DELETE | `/workspaces/{workspacePublicId}/projects/{projectPublicId}/members/me`                                 | 프로젝트 자진 탈퇴    | 🔒 |
| DELETE | `/workspaces/{workspacePublicId}/projects/{projectPublicId}/members/{memberPublicId}`                   | 멤버 강퇴         | 🔒 |
| POST   | `/workspaces/{workspacePublicId}/projects/{projectPublicId}/members/{targetUserPublicId}/change-leader` | 프로젝트 리더 변경    | 🔒 |

### Task

| 메소드    | Endpoint                                                          | 설명                  | 인증 |
|--------|-------------------------------------------------------------------|---------------------|----|
| POST   | `/projects/{projectPublicId}/tasks`                               | 최상위 업무 생성           | 🔒 |
| POST   | `/projects/{projectPublicId}/tasks/{parentTaskPublicId}/subtasks` | 하위 업무 생성            | 🔒 |
| GET    | `/projects/{projectPublicId}/tasks`                               | 업무 목록 조회 (필터 + 페이징) | 🔒 |
| GET    | `/projects/{projectPublicId}/tasks/{taskPublicId}`                | 업무 상세 조회            | 🔒 |
| PATCH  | `/projects/{projectPublicId}/tasks/{taskPublicId}`                | 업무 수정               | 🔒 |
| DELETE | `/projects/{projectPublicId}/tasks/{taskPublicId}`                | 업무 삭제 (Soft Delete) | 🔒 |

### Task Comment

| 메소드    | Endpoint                                                                | 설명          | 인증 |
|--------|-------------------------------------------------------------------------|-------------|----|
| POST   | `/projects/{projectPublicId}/tasks/{taskPublicId}/comments`             | 댓글 / 대댓글 작성 | 🔒 |
| GET    | `/projects/{projectPublicId}/tasks/{taskPublicId}/comments`             | 댓글 목록 조회    | 🔒 |
| PATCH  | `/projects/{projectPublicId}/tasks/{taskPublicId}/comments/{commentId}` | 댓글 수정       | 🔒 |
| DELETE | `/projects/{projectPublicId}/tasks/{taskPublicId}/comments/{commentId}` | 댓글 삭제       | 🔒 |

### Notification

| 메소드   | Endpoint                               | 설명             | 인증 |
|-------|----------------------------------------|----------------|----|
| GET   | `/notifications`                       | 알림 목록 조회 (페이징) | 🔒 |
| GET   | `/notifications/unread-count`          | 읽지 않은 알림 개수 조회 | 🔒 |
| PATCH | `/notifications/{notificationId}/read` | 단건 알림 읽음 처리    | 🔒 |
| PATCH | `/notifications/read-all`              | 전체 알림 읽음 처리    | 🔒 |

### My

| 메소드 | Endpoint    | 설명                     | 인증 |
|-----|-------------|------------------------|----|
| GET | `/my/tasks` | 내 업무 전체 조회 (필터링 + 페이징) | 🔒 |

---

## 6. 성능 개선

### 이메일 발송 성능 개선

기존에는 이메일 발송을 서비스 레이어에서 동기적으로 직접 처리하여, SMTP 응답 지연이 API 응답 속도에 직접적인 영향을 미쳤습니다.

<img width="2234" height="1268" alt="Image" src="https://github.com/user-attachments/assets/b09f1516-6688-443d-82fb-f3f863632740" />

#### 개선 과정

- **이벤트 기반 아키텍처 적용**으로 서비스 레이어와 이메일 발송 로직을 느슨하게 분리
    - `AuthService`, `WorkspaceMemberService`에서 `EmailService` 직접 의존 제거
    - `ApplicationEventPublisher`로 이벤트 발행 → `EmailEventListener`가 구독하여 처리
    - `@TransactionalEventListener(AFTER_COMMIT)` 적용 → DB 트랜잭션 롤백 시 이메일 미발송 보장
- **`@Async("emailExecutor")`** 로 이메일 발송을 별도 스레드 풀에서 처리
    - 네트워크 I/O 위주로 대기 시간이 길어서 `Core 5 / Max 20`으로 설정
    - 외부 SMTP 서버 장애 시에도 메인 서비스 가용성 유지
- **`@Retryable`** 로 일시적인 네트워크 장애 대응
    - 지수 백오프(2s → 4s)로 최대 3회 재시도, 전체 실패 시 `@Recover`에서 에러 로그 기록

#### 성능 개선 결과 (NCP vCPU 2코어, 10회 측정 기준)

<img width="2226" height="1260" alt="Image" src="https://github.com/user-attachments/assets/e4f6237f-a753-427a-885c-1092a1875de0" />

| 측정 회차     | 동기 방식 (기존)   | 비동기 방식 (개선 후) |
|:----------|:-------------|:--------------|
| 1회차       | 1,070 ms     | 519 ms        |
| 2회차       | 474 ms       | **19 ms**     |
| 3회차       | 332 ms       | 87 ms         |
| 평균(2~10회) | **약 452 ms** | **약 85 ms**   |

> → 평균 응답 속도 **약 81% 단축**

---

## 7. 프로젝트 구조

```markdown
letzcollab/
├── README.md
├── docker-compose.prod.yml              # 프로덕션 전체 서비스 오케스트레이션
├── nginx.conf                           # 리버스 프록시 및 정적 파일 서빙 설정
├── logo.png
│
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── workflows/
│       └── deploy.yml                   # 프로덕션 자동 배포 워크플로우
│
├── letzcollab-frontend/                 # React + Vite
│   ├── Dockerfile
│   ├── index.html                       
│   ├── vite.config.js
│   ├── eslint.config.js
│   ├── package.json
│   ├── public/                          
│   └── src/
│       ├── api/
│       │   └── axios.js                 # Axios 인스턴스 (baseURL, credentials 설정)
│       ├── components/                  # 재사용 UI 컴포넌트
│       │   ├── AuthFormInput.jsx        # 공통 인증 폼 인풋
│       │   └── Logo.jsx
│       ├── layouts/                     # 페이지 레이아웃 래퍼
│       │   ├── AuthLayout.jsx           # 인증 관련 페이지 레이아웃
│       │   └── MainLayout.jsx           # 로그인 후 메인 페이지 레이아웃
│       ├── pages/                       # 라우트 단위 페이지 컴포넌트
│       │   ├── Login.jsx
│       │   ├── Signup.jsx
│       │   ├── VerifyEmail.jsx
│       │   ├── RequestPasswordReset.jsx
│       │   └── ResetPassword.jsx
│       ├── routes/
│       │   ├── PrivateRoute.jsx         # 인증 필요 라우트 가드
│       │   └── PublicRoute.jsx          # 비인증 전용 라우트 가드
│       ├── App.css
│       ├── App.jsx                      # 라우터 및 QueryClient 설정
│       ├── index.css
│       └── main.jsx
│
└── letzcollab-backend/                  # Spring Boot
    ├── Dockerfile
    ├── build.gradle                     
    ├── docker-compose.yml               # 로컬 개발용 PostgreSQL 실행
    └── src/main/
        ├── resources/
        │   ├── application.yml          
        │   ├── application-local.yml    
        │   └── application-prod.yml     
        └── java/xyz/letzcollab/backend/
            ├── controller/              
            │   ├── AuthController.java  
            │   ├── UserController.java
            │   ├── WorkspaceController.java
            │   ├── WorkspaceMemberController.java
            │   ├── ProjectController.java
            │   ├── ProjectMemberController.java
            │   ├── TaskController.java
            │   ├── TaskCommentController.java
            │   └── NotificationController.java
            ├── service/                 
            │   ├── AuthService.java
            │   ├── UserService.java
            │   ├── WorkspaceService.java
            │   ├── WorkspaceMemberService.java
            │   ├── ProjectService.java
            │   ├── ProjectMemberService.java
            │   ├── TaskService.java
            │   ├── TaskCommentService.java
            │   └── NotificationService.java
            ├── repository/              # Spring Data JPA + QueryDSL
            │   ├── UserRepository.java
            │   ├── VerificationTokenRepository.java
            │   ├── WorkspaceRepository.java
            │   ├── WorkspaceMemberRepository.java
            │   ├── WorkspaceInvitationRepository.java
            │   ├── ProjectRepository.java
            │   ├── ProjectRepositoryCustom.java    
            │   ├── ProjectRepositoryCustomImpl.java
            │   ├── ProjectMemberRepository.java
            │   ├── TaskRepository.java
            │   ├── TaskRepositoryCustom.java
            │   ├── TaskRepositoryCustomImpl.java
            │   ├── TaskCommentRepository.java
            │   └── NotificationRepository.java
            ├── entity/                  
            │   ├── User.java
            │   ├── VerificationToken.java
            │   ├── Workspace.java
            │   ├── WorkspaceMember.java
            │   ├── WorkspaceInvitation.java
            │   ├── Project.java
            │   ├── ProjectMember.java
            │   ├── Task.java
            │   ├── TaskComment.java
            │   ├── Notification.java
            │   └── vo/                  # Enum 값 객체
            │       ├── UserRole.java
            │       ├── UserStatus.java
            │       ├── WorkspaceRole.java
            │       ├── ProjectRole.java
            │       ├── ProjectStatus.java
            │       ├── TaskStatus.java
            │       ├── TaskPriority.java
            │       ├── NotificationType.java
            │       ├── ReferenceType.java
            │       └── TokenType.java
            ├── dto/                     # 요청/응답 DTO
            │   ├── auth/
            │   │   ├── SignupRequest.java
            │   │   ├── LoginRequest.java
            │   │   ├── LoginResponse.java
            │   │   ├── EmailVerificationRequest.java
            │   │   ├── ResendEmailVerificationRequest.java
            │   │   ├── PasswordResetEmailRequest.java
            │   │   └── PasswordResetRequest.java
            │   ├── user/
            │   │   ├── UserResponse.java
            │   │   └── UserUpdateRequest.java
            │   ├── workspace/
            │   │   ├── CreateWorkspaceRequest.java
            │   │   ├── UpdateWorkspaceRequest.java
            │   │   ├── WorkspaceResponse.java
            │   │   ├── WorkspaceDetailsResponse.java
            │   │   ├── WorkspaceInviteRequest.java
            │   │   ├── AcceptWorkspaceInvitationRequest.java
            │   │   ├── OwnerDto.java
            │   │   ├── MemberSummaryDto.java
            │   │   ├── MemberUpdateMyselfRequest.java
            │   │   └── MemberUpdateOtherRequest.java
            │   ├── project/
            │   │   ├── CreateProjectRequest.java
            │   │   ├── UpdateProjectRequest.java
            │   │   ├── ProjectResponse.java
            │   │   ├── ProjectDetailsResponse.java
            │   │   ├── ProjectSearchCond.java
            │   │   ├── AddMemberRequest.java
            │   │   ├── UpdateMyselfRequest.java
            │   │   ├── UpdateOtherMemberRequest.java
            │   │   ├── LeaderDto.java
            │   │   └── MemberSummaryDto.java
            │   ├── task/
            │   │   ├── CreateTaskRequest.java
            │   │   ├── UpdateTaskRequest.java
            │   │   ├── TaskResponse.java
            │   │   ├── TaskDetailsResponse.java
            │   │   └── TaskSearchCond.java
            │   ├── comment/
            │   │   ├── CreateCommentRequest.java
            │   │   ├── UpdateCommentRequest.java
            │   │   ├── CommentResponse.java
            │   │   └── ReplyResponse.java
            │   └── notification/
            │       └── NotificationResponse.java
            ├── global/
            │   ├── config/
            │   │   ├── AsyncConfig.java         # 비동기 처리 활성화 + 커스텀 쓰레드 풀 정의
            │   │   ├── SchedulingConfig.java    # 스케줄러 활성화 설정
            │   │   ├── SwaggerConfig.java       # Swagger / OpenAPI 설정
            │   │   └── WebConfig.java           # Page 객체를 PagedModel DTO로 변환해서 응답 반환
            │   ├── dto/
            │   │   └── ApiResponse.java         # 공통 API 응답 클래스
            │   ├── email/                       
            │   │   ├── context/
            │   │   │   ├── EmailContext.java                    # 각 EmailContext가 구현하는 인터페이스
            │   │   │   ├── VerifyEmailContext.java
            │   │   │   ├── PasswordResetEmailContext.java
            │   │   │   └── WorkspaceInvitationEmailContext.java
            │   │   └── EmailService.java                        # 이메일 발송 서비스
            │   ├── entity/                      # 공통 Base 엔티티 (3단계 상속 구조)
            │   │   ├── DateBaseEntity.java
            │   │   ├── PublicIdAndDateBaseEntity.java
            │   │   └── PublicIdAndFullAuditBaseEntity.java
            │   ├── event/                       # Spring 이벤트 기반 비동기 처리
            │   │   ├── dto/
            │   │   │   ├── EmailEvent.java
            │   │   │   └── NotificationEvent.java
            │   │   └── listener/
            │   │       ├── EmailEventListener.java
            │   │       └── NotificationEventListener.java
            │   ├── exception/
            │   │   ├── dto/
            │   │   │   └── ValidationError.java      # 필드 검증 오류 DTO
            │   │   ├── CustomException.java          # 비즈니스 로직 예외 클래스
            │   │   ├── ErrorCode.java
            │   │   └── GlobalExceptionHandler.java   # 공통 예외 처리기 (@RestControllerAdvice)
            │   ├── persistence/
            │   │   ├── CustomAuditorAware.java       # Auditing 사용자 공급자
            │   │   ├── DataInitializer.java          # 초기 시드 데이터 생성
            │   │   ├── JpaConfig.java
            │   │   └── QuerydslConfig.java
            │   ├── scheduler/
            │   │   ├── NotificationCleanupScheduler.java     # 오래된 알림 정리
            │   │   └── TaskDeadlineScheduler.java            # 업무 마감 알림 발송
            │   └── security/
            │       ├── jwt/
            │       │   ├── JwtAuthenticationEntryPoint.java  # 미인증 요청 진입 시 401 응답 처리
            │       │   ├── JwtAuthenticationFilter.java      # 요청마다 쿠키 또는 헤더에서 JWT 추출 및 인증 처리
            │       │   └── JwtTokenProvider.java             # JWT 생성/파싱/Authentication 객체 반환
            │       ├── userdetails/
            │       │   ├── CustomUserDetails.java            
            │       │   └── CustomUserDetailsService.java     
            │       ├── AuthErrorHandler.java                 # 필터 레이어에서 예외 발생 시 ApiResponse 형식으로 JSON 직렬화하여 응답
            │       └── SecurityConfig.java                   # Security 필터 체인, CORS, 비밀번호 인코더 등 설정
            └── LetzcollabBackendApplication.java
```
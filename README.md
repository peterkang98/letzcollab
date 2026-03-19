# Let'z Collab: 프로젝트 협업 관리 플랫폼

---------
<img src="./logo.png" alt="Let'z Collab Logo" width="350">

> 워크스페이스 단위로 조직을 관리하고, 프로젝트와 업무를 체계적으로 추적하는 협업 솔루션

---

## 1. 프로젝트 개요

**Let'z Collab**은 현대적인 조직이 겪는 협업의 파편화 문제를 해결하기 위해 설계된 플랫폼입니다. 단순한 업무 관리를 넘어, **워크스페이스 - 프로젝트 - 업무 - 댓글**로 이어지는 계층적인 데이터 구조를 기반으로 조직/팀 단위의 대규모 협업을 안정적으로 지원합니다.

본 프로젝트는 엔터프라이즈 급 백엔드 아키텍처를 구축하고, 안전한 인증 시스템 및 역할 기반의 권한 제어(RBAC)를 구현하는 데 초점을 맞추었습니다. 네이버 클라우드 플랫폼(NCP)과 Github Actions를 사용하여 CI/CD 파이프라인을 직접 구축하고 실제 운영 환경과 동일한 배포 프로세스를 경험했습니다.

- **서비스 URL**: https://letzcollab.xyz/
- **API 명세서 (Swagger)**: https://letzcollab.xyz/api/swagger-ui/index.html#/

---

## 2. 기술 스택

---

## 3. 시스템 아키텍처 및 구현 디테일

### 3.1. 아키텍처 다이어그램

### 3.2. 논리적 데이터 아키텍처 및 권한 모델

Let'z Collab은 세밀한 역할 기반 권한 제어(RBAC)를 통해 보안을 강화했습니다.

1.  **계층 구조**: **Workspace** > Projects > Tasks > Comments 
    - 프로젝트는 공개/비공개로 나뉩니다.
      - 공개 프로젝트는 해당 워크스페이스에 속한 멤버들이 모두 조회 가능합니다.
      - 비공개 프로젝트는 해당 프로젝트 멤버로 추가된 워크스페이스 멤버만 접근 가능합니다.
2.  **권한 모델 (Role-Based Access Control)**:
    - **Workspace Member**: 
      - `OWNER`: 최상위 워크스페이스 소유자
      - `ADMIN`: 워크스페이스 관리자
      - `MEMBER`: 일반 참여자
      - `GUEST`: 조회 중심(+ 댓글 작성 ) 외부 협력자
    - **Project Member**: 
      - `ADMIN`: 프로젝트 관리자 (프로젝트 관리, 멤버 추가/강퇴, 모든 업무 관리)
        - 프로젝트 리더도 `ADMIN` 권한을 갖지만, `Project` 엔티티에 `User leader` 필드로 관리되어, 일반 `ADMIN` 권한을 갖는 멤버보다 더 높은 권한을 가집니다.
      - `MEMBER`: 일반 프로젝트 참여자 (업무 생성, 수정, 완료 처리 등 실무 수행) 
      - `VIEWER`: 프로젝트 조회자 (+ 댓글 허용) 

### 3.3. 핵심 구현 사항

#### A. FETCH JOIN으로 쿼리 수 최적화

#### B. 플랫폼 내부 알림 시스템 구현

#### C. 복잡한 동적 쿼리 최적화 (QueryDSL)
QueryDSL을 활용하여 업무를 조회할 때, 사용자가 원하는 다중 조건(담당자, 상태, 우선순위, 키워드 검색 등)에 따른 복잡한 페이징 쿼리를 구현했습니다. 이를 통해 성능을 최적화하고 컴파일 타임에 쿼리 오류를 발견할 수 있는 안정성을 확보했습니다.

#### D. 보안 및 인증 (Spring Security + JWT)
`JwtAuthenticationFilter.java`를 통해 모든 HTTP 요청의 Header에서 JWT를 추출하고 검증하는 무상태 인증 시스템을 구현했습니다. `SecurityConfig.java`에서 엔드포인트별 권한을 세분화하여(`hasRole(...)`), 허가되지 않은 사용자의 접근을 원천 차단했습니다.

#### E. AWS SES 기반 이메일 서비스 구축

#### F. 실제 운영 환경 고려 (Nginx & LetsEncrypt)
NCP 인프라에서 Nginx를 리버스 프록시로 설정하여, 리액트 정적 파일 서빙과 `/api/*` 경로의 백엔드 컨테이너 라우팅을 분리했습니다. `letsencrypt` 및 `certbot`을 활용하여 전체 아키텍처에 HTTPS(SSL/TLS)를 적용, 보안성을 실제 서비스 수준으로 끌어올렸습니다.

---

## 4. ERD
<img width="2138" height="2514" alt="Image" src="https://github.com/user-attachments/assets/6838449a-6db3-4dee-b4d8-4c50b20df050" />

---

## 5. API

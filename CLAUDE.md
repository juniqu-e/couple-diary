# CLAUDE.md — couple-diary 범용 규칙

이 파일은 Claude Code가 이 repo에서 작업할 때 **항상 따르는 공통 규칙**입니다.
프론트엔드/백엔드/인프라 각 영역의 세부 규칙은 다음 위치에 분리되어 있습니다 (없으면 아직 미작성):

- `frontend/CLAUDE.md` — Next.js/React/Tailwind 관련 세부 규칙
- `backend/CLAUDE.md` 또는 각 서비스 폴더 하위 `CLAUDE.md` — Spring Boot/JPA/도메인 모델 세부 규칙
- `infra/CLAUDE.md` 또는 `deploy/` — EKS/K8s manifest, GitHub Actions, 배포 세부 규칙

세부 규칙이 본 문서와 충돌할 때는 **세부 규칙 우선**.

---

## 0. 프로젝트 정의 (2026-05-12 합의 반영)

**couple-diary (이 repo)** = 운영자(juniqu-e) 본인의 커플 다이어리 서비스. **AWS EKS 배포 환경**.
현재 public이지만 **향후 private화 예정**이므로 외부 배포자를 위한 중립화·추상화 작업은 **하지 않는다** (불필요한 ports/adapters 만들지 않음). 단, **MSA 서비스 경계의 인터페이스는 별개**로 유지 (§5 참조).

**dijkstra (별도 repo, 비공개)** = 동일 앱의 **홈서버 + 로컬 K8s** 배포 환경.
couple-diary가 코드의 single source of truth, dijkstra는 환경별 values·manifest·운영 자산을 들고 있음. couple-diary main 푸시 시 `.github/workflows/notify-downstream.yml`이 dispatch 발화.

**왜 두 환경?** 학습 목적 — EKS(클라우드 K8s 운영) + 로컬 K8s(자체 인프라 운영) 둘 다 경험. Helm chart도 학습 대상.

`HANDOFF_v2.md` / `couple-app-mvp-spec.md`는 "홈서버 단일 커플 전용" 가정이었으나, 두 배포 환경 + 학습 목적이 추가된 이후 다음과 같이 해석한다:

- 도메인 모델·화면·기능 명세 → 그대로 유효
- "멀티테넌트 미고려" 문구 → **무시.** 단일 운영자 서비스라 해도 `couple_id` row-level 격리는 보안상 강제 (본인+파트너 외 접근 차단)
- 홈서버 특정 인프라(Cloudflare Tunnel, NPM, 본인 도메인) → **dijkstra 영역**
- Telegram bot · n8n · 개인 Google Calendar 통합 → **코드는 couple-diary에 있고 env로 enable/credential 주입**, 실제 wiring/시크릿은 dijkstra (또는 EKS Secret)
- 문서 충돌 시 우선순위: **본 §0 > `HANDOFF_v2.md` > `couple-app-mvp-spec.md`**

MVP 기능 범위 (변경 없음): D-Day·기념일 자동화 / 일정 공유 / 데이트 장소·visit 기록.

---

## 1. 스택 (고정)

| 영역 | 스택 | 비고 |
|---|---|---|
| 프론트엔드 | Next.js 16 (App Router) / React 19 / TypeScript / Tailwind CSS 4 | PWA 셸 `next-pwa`로 Phase 1 도입 |
| 백엔드 | Spring Boot 3.5 / Java 21 / Spring Data JPA / Bean Validation | **MSA — 4 서비스 + 1 게이트웨이** (§5.1) |
| DB | PostgreSQL 16 | 단일 PG 인스턴스, **서비스별 schema 분리** (`identity`, `diary`, `schedule`, `media`) |
| 스토리지 | AWS S3 SDK v2 (로컬 MinIO, 운영 AWS S3) | S3 호환 인터페이스 |
| 인증 | Spring Security + JWT | identity-service 발급, **각 서비스가 직접 검증** (공통 `auth-lib`) |
| 메시징 | **Apache Kafka** | Phase 0부터 도입. 운영 방식(MSK vs Strimzi/K8s) §5.1 후속 결정 |
| 로컬 개발 | Docker Compose (`docker-compose.local.yml`) | 단일 머신 빠른 기동용 |
| 배포 타겟 | **AWS EKS** (couple-diary) / 홈서버 로컬 K8s (dijkstra) | 동일 Helm chart, values만 다름 |
| 배포 패키징 | **Helm chart** (`deploy/helm/couple-diary/`) | Kustomize 미사용 — Helm 학습 단일화 |
| CD | **ArgoCD (GitOps)** 처음부터 | GitHub Actions가 빌드/push, ArgoCD가 EKS rollout |
| 이미지 레지스트리 | **AWS ECR** | dijkstra와의 이미지 공유 모델은 §3.3 |
| 시크릿 관리 | K8s Secret + **External Secrets Operator → AWS Secrets Manager** | dijkstra는 자체 sealed-secrets/sops 등 |
| 외부 라이브러리 | 사진 EXIF: `com.drewnoakes:metadata-extractor` / 지도: MapLibre GL JS + D3.js (한국 시군구 GeoJSON) | media-service / 프론트 |
| 옵저버빌리티 | Spring Boot Actuator + 표준 출력 로그 + Kafka → EKS 수집 | 구체 스택 (Prometheus·Loki·OTel) Phase 0 후반 결정 |

**원칙**
- 위 스택을 **사용자 승인 없이 교체하지 않는다.** 라이브러리 추가는 가능하지만 commit body 또는 PR 본문에 *왜 필요한지* 한 줄 명시
- **MSA 서비스 경계 인터페이스는 명시적으로 유지** (auth, calendar sync, notification 등) — 외부 OSS 추상화 의도가 아니라 **서비스 분리 자체가 인터페이스의 본질**
- 외부 통합 코드(Telegram, n8n webhook, GCal OAuth, Naver Local Search 등)는 **couple-diary에 직접 구현하되 credential·enable flag는 env 주입**. 본인 토큰·계정 정보 자체는 dijkstra/EKS Secret으로 격리 (§3)

---

## 2. 작업 모드와 휴먼 게이트

### 2.1 항상 사용자 승인을 받아야 하는 행위 (Human Gate)
다음은 작업을 시작하기 전에 사용자에게 반드시 묻는다:

- **새 의존성 추가** (특히 build.gradle.kts / package.json / 새 서비스의 빌드 파일)
- **DB 스키마 변경** (새 테이블·컬럼 추가, 컬럼 타입 변경, FK·인덱스 변경)
- **공개 API 시그니처 변경** (`/api/...` 경로·메서드·요청/응답 스키마) — downstream 호환성 영향
- **MSA 서비스 경계 변경** (새 서비스 신설, 서비스 통합·분할, 책임 이동)
- **서비스 간 통신 방식 변경** (sync→async, broker 도입·교체)
- **인증/보안 관련 변경** (SecurityFilterChain, JWT 설정, CORS, CSRF, 서비스 간 인증)
- **인프라 변경** (docker-compose, K8s manifest/Helm chart, env 키 추가·이름 변경, GitHub Actions, 도메인/포트)
- **dijkstra와 호환성에 영향 가는 변경** (env 키 rename·삭제, API 경로 변경, 이미지 태깅 규칙 변경) — §3 참조
- **시안에 없는 새 화면 추가** (HANDOFF v2 §10 참조 — 버킷리스트 등)
- **파일/디렉터리 대량 이동·삭제** (10개 이상)

### 2.2 사용자 승인 없이 진행 가능한 행위
- 기존 API의 내부 구현 리팩토링
- 컴포넌트 내부 마크업/스타일 조정 (디자인 토큰 범위 내)
- 테스트 추가
- 오타·주석·README 수정
- 명백한 버그 수정 (재현 + 1줄 설명을 commit body에 남김)

### 2.3 모호하면 멈춘다
"이거 사용자 승인 필요한가?" 싶은 경우 **묻는다.** 묻는 비용 < 잘못 진행한 뒤 되돌리는 비용.

---

## 3. 두 배포 환경의 책임 분담 (EKS vs 홈서버)

### 3.1 couple-diary에 들어가는 것 (코드의 source of truth + EKS 환경)
- **모든 도메인 코드** (User, Couple, DiaryEntry, Place, Photo, Schedule, Anniversary 등)
- **모든 화면** (S1~S4, M1~M5) — 프론트엔드 Next.js
- **REST API 정의** 및 OpenAPI 스펙
- **DB 스키마·마이그레이션** (Flyway 또는 Liquibase — Phase 0 후속 결정)
- **모든 외부 통합 코드** (Telegram bot, GCal OAuth/sync, Naver Local Search, 알림 라우팅 등) — 비활성화 기본값, env로 enable
- **Helm chart** (`deploy/helm/couple-diary/`) — 모든 K8s 리소스 정의의 origin
- **EKS용 values** (`deploy/helm/values-eks.yaml`) + ArgoCD Application manifest
- **GitHub Actions CI** — 빌드·테스트·ECR push, 매니페스트 변경 시 ArgoCD가 자동 감지
- 로컬 개발용 `docker-compose.local.yml` (MinIO + Postgres + Kafka 로컬 기동)
- `notify-downstream.yml` (main 푸시 시 dijkstra 알림 — 유지)

### 3.2 dijkstra에 들어가는 것 (홈서버 환경)
- **본인 홈서버 로컬 K8s용 Helm values** (`values-homeserver.yaml` 또는 유사) — 도메인, 리소스 한도, 이미지 태그 핀, 외부 통합 enable flag
- **추가 K8s 리소스** — PV/PVC(로컬 디스크), Cloudflare Tunnel, NPM 라우팅, 홈서버 특화 ingress
- **모든 시크릿값** — sealed-secrets / sops / SOPS-encrypted secrets
- **본인 Telegram bot 토큰**, **본인 GCal 계정 OAuth client/refresh token**, 본인 도메인, n8n 인스턴스 URL
- **n8n 워크플로우 정의** (외부 자동화 — Telegram 알림 wiring 등)
- 본인용 백업 정책·모니터링 대시보드 설정
- 본인용 패치 (가능하면 couple-diary로 commit, 환경 특이 사항만 dijkstra 잔류)

### 3.3 이미지 공유 모델 (TBD — Phase 0 인프라 셋업 시 확정)
couple-diary는 **ECR**로 push. dijkstra(홈서버)가 그 이미지를 사용하는 방법 후보:
- (A) ECR public 또는 cross-account 허용 → 홈서버 노드가 직접 pull
- (B) couple-diary가 ECR + GHCR 양쪽 push, dijkstra는 GHCR pull (인증 단순)
- (C) dijkstra가 자체 빌드 (소스 동기화 워크플로우 필요)

**기본은 (A) 시도, 막히면 (B). (C)는 최후.** 결정 시 본 §11에 기록.

### 3.4 호환성 규칙
- **API breaking change**는 dijkstra 업그레이드와 짝지어 진행. PR 본문에 dijkstra 영향 섹션 필수
- **env 키 rename/삭제, Helm values 키 변경**은 §2.1 게이트. 변경 시 deprecated 표기 후 최소 1회 릴리스 유지 권장
- **Helm chart는 couple-diary가 source of truth.** dijkstra는 chart를 fork하지 않고 values만 분기

### 3.5 의심스러우면
"이거 dijkstra 영역인 것 같은데?" 싶으면 **묻는다.** 본인 시크릿·도메인·외부 계정 식별자가 couple-diary 코드/설정에 들어오는 것이 가장 큰 사고.

---

## 4. 브랜치·커밋·PR 규칙

자세한 규칙은 `docs/Git_convention.md`. 핵심 요약:

- 메인 브랜치: `main` (운영, dijkstra 동기화 트리거 발화 지점), 통합 브랜치: `develop` (현재 작업 대상)
- 작업 브랜치: `feature/<짧은-이름>` 또는 `fix/<...>` — develop에서 분기, develop으로 머지
- 커밋 메시지: `<type>: <subject>` — `feat`, `fix`, `refactor`, `infra`, `docs`, `chore`, `style`, `test`, `init` 중 하나
- subject는 한글, 50자 이내, 마침표 없음, 명령문
- 하나의 커밋 = 하나의 논리적 변경. 큰 작업은 쪼개서 커밋
- PR 본문에는: **무엇을 / 왜 / 어떻게 검증했는지** 3섹션. dijkstra 영향이 있으면 별도 섹션 추가. 스크린샷이 있으면 첨부

Claude Code는 **사용자가 명시적으로 요청하기 전에는 자동으로 커밋·PR 생성 금지.**
스테이지 변경은 OK, `git commit` / `git push` / `gh pr create`는 명시 요청 후에만.

---

## 5. 작업 흐름 (Build Order)

### 5.1 Phase 0 아키텍처 결정 (2026-05-12 합의 완료)

| # | 항목 | 결정 |
|---|---|---|
| 1 | MSA 서비스 경계 | **4 서비스 + 1 게이트웨이 (mono-repo, Gradle multi-module)** — `identity-service` / `diary-service` / `schedule-service` / `media-service` / `api-gateway`. 게이트웨이는 처음부터 도입 |
| 2 | DB 격리 | **단일 PostgreSQL + 서비스별 schema 분리** (`identity`, `diary`, `schedule`, `media`). 서비스 간 FK 금지 |
| 3 | 서비스 간 통신 | **동기 REST (OpenFeign 또는 Spring `RestClient`) + Apache Kafka 비동기** 처음부터. Kafka 운영 방식(MSK vs Strimzi/K8s self-host)은 EKS 초기 셋업 시 후속 결정 |
| 4 | 인증 모델 | **각 서비스가 JWT 직접 검증**. 공통 `auth-lib` Gradle module로 검증 로직 공유. Gateway는 라우팅·CORS·rate limit 담당 |
| 5 | K8s 배포 패키징 | **Helm chart 단일**. couple-diary chart가 source of truth, EKS·홈서버 둘 다 동일 chart + 다른 values. Kustomize 미사용 |
| 6 | 이미지 레지스트리 | **AWS ECR**. dijkstra와의 이미지 공유 모델은 §3.3에 따라 결정 |
| 7 | CI/CD | **GitHub Actions(빌드·테스트·ECR push) + ArgoCD GitOps (EKS 자동 rollout)** — 처음부터 도입 |

**관련 부수 결정**
- 엔티티 명칭: **`DiaryEntry`** (visit/Visit 미사용, 시안 용어로만 한국어 보조)
- 시간대: **KST 단일** (저장도 직렬화도 KST. 범용 OSS 의도 폐기로 단순화)
- 시크릿: K8s Secret + **External Secrets Operator → AWS Secrets Manager** (EKS). dijkstra는 자체 방식
- PWA: **`next-pwa`** (manual SW 작성 안 함)
- EXIF: **`com.drewnoakes:metadata-extractor`** (media-service)
- 지도: **MapLibre GL JS (해외) + D3.js 시군구 GeoJSON (국내)**. Naver/Kakao 네이티브 SDK는 도입 안 함, Naver Local Search는 백엔드 프록시 경유 검색용만

**Phase 0 후속 확인 필요 항목 (코드 작성 도중 도달 시 정지·질문)**
- Kafka 운영: AWS MSK(관리형, 비용) vs Strimzi(self-host K8s 위, 학습) — EKS 비용 추산 함께
- ECR 이미지 공유 모델: §3.3 (A)/(B)/(C) 중
- DB 마이그레이션 도구: Flyway vs Liquibase
- 옵저버빌리티 구체 스택: Prometheus + Loki + Grafana / OpenTelemetry / AWS CloudWatch
- 비용 추산: ECR + EKS + RDS + ALB + (MSK or self-host) + S3 — 사용자가 별도로 요청 (Phase 0 인프라 결정 직전)

### 5.2 Phase 0 이후
HANDOFF v2 §7의 Phase 0 → 1 → 2 → 3 순서를 유지하되, 각 도메인은 §5.1에서 결정한 서비스 경계에 맞춰 해당 서비스 안에 구현한다.

### 5.3 매 작업 시작 시 점검
1. 이 작업이 어떤 Phase·번호에 해당하는가? (스코프 확인)
2. 사용자 승인이 필요한 변경인가? (§2.1)
3. 이미 비슷한 코드/패턴이 있는가? (먼저 grep)
4. 영향받는 화면·API는 무엇인가? (HANDOFF §6 매핑)
5. dijkstra에 영향이 가는가? (§3.3)

---

## 6. 코드 작성 원칙

### 6.1 공통
- **있는 것을 먼저 쓴다.** 새 유틸 만들기 전에 기존 코드 grep. 비슷한 게 있으면 그걸 확장
- **YAGNI.** 지금 안 쓰는 추상화·옵션·플래그 추가 금지. "나중에 필요할 것 같아서"는 금지 사유. 단, **서비스 경계에서의 인터페이스 정의는 예외** — MSA에선 경계가 추상화의 본질
- **주석은 *왜*만.** *무엇을* 하는지는 코드가 말한다. 비자명한 제약·우회·결정 사유만 주석
- **금지어 주석:** `// TODO`, `// FIXME`는 이슈로 옮기거나 즉시 처리. 코드에 남기지 않는다
- **에러 처리:** 외부 경계(요청 입력, 외부 API, 파일 IO, 서비스 간 호출)에서만. 내부 함수 호출에 try/catch 도배 금지
- **하드코딩 금지:** 매직 넘버·문자열은 상수 또는 설정으로. 단, 1회용 마이그레이션 스크립트는 예외
- **개인 운영 식별자(Telegram chat id, 본인 이메일, 본인 GCal id 등) 절대 commit 금지.** §3.2 영역

### 6.2 네이밍 규칙 — HANDOFF §3 용어 매핑 강제
디자인 시안의 한글 용어와 백엔드 엔티티 이름이 다르다. **백엔드/DB는 영어 엔티티 이름**으로 고정 (2026-05-12 합의):

| 시안/한글 | 코드 |
|---|---|
| 방문 / visit | **`DiaryEntry`** (확정 — `Visit` 미사용) |
| 장소 | `Place` |
| 사진 | `Photo` |
| 계획 / 일정 | `Schedule` |
| 기념일 | `Anniversary` |
| 커플 | `Couple` |
| 사용자 | `User` |

MVP spec §6.2의 `visit` + `visit_entries` 분리 모델은 채택하지 않음. **하나의 `DiaryEntry`에 양쪽 사용자의 글·사진을 author 구분 필드로 묶는다.** 구체 컬럼은 Phase 0에서 ERD 확정.

### 6.3 언어·로케일
- UI 텍스트: 한국어 단일 (i18n 도입 안 함, MVP 범위 외 — 단, 범용 repo 정체성 고려해 i18n 가능 구조는 유지)
- 코드 주석: 한국어 OK, 영어 OK — 한 파일 내 일관성만 유지
- 커밋 메시지: 한국어 (Git_convention)
- PR 제목/본문: 한국어
- 변수·함수·클래스명: 영어 (도메인 용어는 §6.2 매핑 사용)
- 시간대: **KST (Asia/Seoul) 단일.** DB는 `TIMESTAMP WITH TIME ZONE`에 KST로 저장, API 직렬화도 KST. 운영 오버라이드용 추상화 도입 안 함 (범용 OSS 의도 폐기)

---

## 7. 보안 베이스라인

- **시크릿은 절대 commit 금지.** `.env`, `application-local.yml`의 비밀번호 필드 등. 이미 들어있으면 즉시 알림
- `.env.example` / `.env.local.example`에는 **값이 아닌 키 이름과 더미값**만
- JWT 시크릿·DB 비밀번호·S3 키는 환경변수로만 주입. 코드에 default 시크릿 박지 않기 (개발 편의용이라도)
- 모든 API는 기본적으로 **인증 필요**, 명시적으로 public인 엔드포인트(`/health`, `/api/auth/*`)만 허용
- **`couple_id` 기반 row-level 격리는 강제.** 범용 멀티테넌트 전제이므로 다른 커플의 데이터 노출은 곧 보안 사고. 모든 도메인 쿼리는 현재 인증된 사용자의 `couple_id` 필터 강제 — 서비스 레이어에 헬퍼 마련 (Phase 0 인증 작업)
- 사용자 입력 검증: `@Valid` + Bean Validation, 프론트는 zod 또는 유사한 스키마 (도입 시 사용자 확인)
- CORS: 로컬은 `http://localhost:3000`, 운영은 env 주입
- **서비스 간 호출 인증:** Phase 0에서 mTLS / JWT 전파 / mesh 중 결정. 결정 전까지 내부 호출도 외부와 동일하게 JWT 필수 가정
- **운영자 개인 식별자가 코드에 들어오지 않게.** 본인 이메일·Telegram id·도메인 등은 dijkstra의 env에서 주입 (§3.2)

---

## 8. 테스트 정책

- **새 비즈니스 로직에는 최소 1개 단위 테스트.** 컨트롤러 얇은 위임은 생략 OK, 서비스/도메인 메서드는 필수
- 통합 테스트: Phase 1 끝까지는 의무 아님. Phase 2부터 핵심 흐름(온보딩, visit 작성)에 대해 추가
- **DB를 mock하지 않는다.** 통합 테스트는 Testcontainers Postgres. 이유: prod DB가 Postgres라 mock과 prod 동작이 갈릴 수 있음
- **MSA 서비스 간 호출은 contract test 또는 mock server** (예: WireMock). 다른 서비스의 실제 인스턴스 의존하지 않도록 — CI 단순 유지
- 프론트: 컴포넌트 시각 회귀는 생략, 핵심 훅·유틸 위주 단위 테스트만 — 도구는 Phase 1에서 결정 (사용자 확인)

---

## 9. 파일·디렉터리 규칙

- **새 파일을 만들기 전에 기존 파일을 편집할 수 없는지 확인.** 특히 README, 문서, 유틸
- **문서 파일(`*.md`)을 사용자 요청 없이 새로 만들지 않는다.** HANDOFF, MVP spec, CLAUDE.md는 사용자가 만들었거나 명시 요청
- 디렉터리 구조는 `README.md` "Project Structure" 섹션과 일치 유지. 큰 폴더 추가는 §2.1 게이트 적용
- **MSA 분해 후 백엔드 디렉터리 구조 (Phase 0 확정 시 본 항목 갱신):**
  - `services/<service-name>/` — 서비스별 Spring Boot 모듈
  - `shared/` — 서비스 간 공유 DTO·유틸 (최소화, 도메인 모델은 공유 금지)
  - `deploy/` — K8s manifest, Helm chart, Kustomize overlay 등
- 백엔드 패키지 컨벤션: `com.couplediary.<service>.<layer>` 형태 — Phase 0에서 첫 서비스 분해할 때 확정
- **dijkstra 관련 파일은 본 repo에 두지 않는다.** 잘못 들어왔다면 즉시 알림 (§3.2)

---

## 10. Claude Code 상호작용 규칙

### 10.1 응답 톤
- 짧고 정확하게. 불필요한 요약·복기 금지
- 작업 시작 전 1문장으로 "지금 무엇을 한다" 선언, 끝나면 1-2문장 결과
- 사용자가 한국어로 말하면 한국어로 답한다

### 10.2 모르면 묻는다
- 추측해서 코드 짜기 전, repo 안에서 grep으로 1분 정도 확인. 그래도 모르면 묻는다
- 사용자에게 묻는 질문은 **구체적으로**: "어떤 라이브러리 쓸까요?"보다 "X와 Y 중 어느 쪽?" 형태

### 10.3 진행 보고
- 파일 여러 개 수정 중이면 1줄씩 진행 상황 알림 (어떤 파일을 왜)
- 막혔으면 즉시 알린다. 우회로를 임의로 선택하지 않는다

### 10.4 신뢰 경계
- HANDOFF v2 §10의 미확정 항목 6개 + §5.1의 아키텍처 결정 7개 — **이 중 어느 하나라도 코드에 영향이 갈 시점에 도달하면 작업을 멈추고 사용자 확인.** 임의 결정 금지

---

## 11. 살아있는 결정 로그

이 섹션은 작업하며 결정된 사항을 누적한다. 새 결정이 생기면 위 본문 규칙을 수정하고 여기에 한 줄 기록.

| 날짜 | 결정 | 위치/관련 섹션 |
|---|---|---|
| 2026-05-12 | CLAUDE.md 초안 작성, 범용 규칙만 포함 | 본 파일 |
| 2026-05-12 | (폐기) couple-diary를 OSS 업스트림으로 / 멀티테넌트 범용. → 본인 개인용 EKS, 향후 private화로 정정 | §0 |
| 2026-05-12 | couple-diary = 본인 EKS 배포 환경, dijkstra = 본인 홈서버 로컬 K8s 환경. 둘 다 본인 운영. 학습 목적 (EKS + 자체 K8s) | §0, §3 |
| 2026-05-12 | MSA 4서비스(`identity`/`diary`/`schedule`/`media`) + `api-gateway` 처음부터. Mono-repo + Gradle multi-module | §5.1 #1 |
| 2026-05-12 | DB는 단일 PostgreSQL + 서비스별 schema 분리 | §5.1 #2 |
| 2026-05-12 | 서비스 간 통신: REST + Kafka 처음부터 도입 (학습 목적 포함) | §5.1 #3 |
| 2026-05-12 | 인증: 각 서비스가 JWT 직접 검증, 공통 `auth-lib`. Gateway는 라우팅/CORS/rate limit만 | §5.1 #4 |
| 2026-05-12 | K8s 배포: Helm chart 단일, EKS/홈서버 동일 chart + 다른 values. Kustomize 미사용 | §5.1 #5 |
| 2026-05-12 | 이미지 레지스트리: AWS ECR | §5.1 #6 |
| 2026-05-12 | CI/CD: GitHub Actions + ArgoCD GitOps 처음부터 | §5.1 #7 |
| 2026-05-12 | 엔티티 명칭: `DiaryEntry`. `Visit` 미사용 | §6.2 |
| 2026-05-12 | 시간대: KST 단일 (저장도 직렬화도) | §6.3 |
| 2026-05-12 | 시크릿: K8s Secret + External Secrets Operator → AWS Secrets Manager | §1 표, §7 |
| 2026-05-12 | PWA: `next-pwa` | §1 표 |
| 2026-05-12 | EXIF: `com.drewnoakes:metadata-extractor` (media-service) | §1 표 |
| 2026-05-12 | 지도: MapLibre GL JS (해외) + D3.js 시군구 GeoJSON (국내). Naver/Kakao 네이티브 SDK 미도입 | §1 표 |
| (TBD) | Kafka 운영: AWS MSK vs Strimzi self-host | §5.1 후속 |
| (TBD) | ECR 이미지 공유 모델 (§3.3 A/B/C 중) | §3.3 |
| (TBD) | DB 마이그레이션 도구 (Flyway vs Liquibase) | §5.1 후속 |
| (TBD) | 옵저버빌리티 스택 (Prometheus/Loki/Grafana vs OTel vs CloudWatch) | §1 표 |
| (TBD) | 종합 비용 추산 (ECR + EKS + RDS + ALB + Kafka + S3) | 사용자 요청 |

---

## 12. 빠른 참조

- 디자인 토큰: `hifi/tokens.css` → `frontend/app/globals.css`의 `@theme`
- 시안 9개: `hifi/index.html` (모바일+데스크탑 페어)
- 빌드 순서: `HANDOFF_v2.md` §7 (+ 본 §5.1의 아키텍처 결정이 선행)
- 사용자 확인 대기 항목: `HANDOFF_v2.md` §10 + 본 §5.1
- API 스펙 초안: `HANDOFF_v2.md` §5
- 데이터 모델 초안: `HANDOFF_v2.md` §4, `couple-app-mvp-spec.md` §6
- Git 컨벤션: `docs/Git_convention.md`
- 다운스트림 동기화: `.github/workflows/notify-downstream.yml`

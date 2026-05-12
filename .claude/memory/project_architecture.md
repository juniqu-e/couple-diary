---
name: couple-diary 아키텍처 방향
description: couple-diary는 운영자 본인의 EKS 배포 환경. dijkstra는 동일 앱의 홈서버 로컬 K8s 환경. 둘 다 본인 운영. Phase 0 결정 합의 완료
type: project
---
**couple-diary (이 repo)** = 운영자(juniqu-e) 본인의 커플 다이어리 서비스. **AWS EKS 배포 환경**. 현재 public이지만 **향후 private화 예정**. 외부 OSS 배포자를 위한 추상화는 만들지 않는다 (불필요한 ports/adapters 금지). 단, MSA 서비스 경계 인터페이스는 별개.

**dijkstra (별도 repo, 비공개)** = 동일 앱의 **홈서버 + 로컬 K8s** 배포 환경. couple-diary가 코드의 single source of truth, dijkstra는 환경별 Helm values·시크릿·홈서버 특화 manifest(Cloudflare Tunnel, NPM)·n8n 워크플로우 보관. `notify-downstream.yml`이 main 푸시 시 dispatch.

**Why 두 환경:** EKS 운영 학습 + 자체 인프라(홈서버 K8s) 운영 학습 둘 다 경험. Helm chart도 학습 대상.

**Phase 0 합의 (2026-05-12):**
- MSA 4 서비스 + 1 게이트웨이: `identity-service` / `diary-service` / `schedule-service` / `media-service` / `api-gateway`. Mono-repo + Gradle multi-module
- DB: 단일 PostgreSQL + 서비스별 schema 분리 (`identity`, `diary`, `schedule`, `media`)
- 통신: 동기 REST + **Apache Kafka 처음부터** (학습 목적 포함)
- 인증: 각 서비스 JWT 직접 검증, 공통 `auth-lib`
- 배포: **Helm chart 단일** (EKS·홈서버 같은 chart, 다른 values). Kustomize 미사용
- 레지스트리: **AWS ECR**. 이미지 공유 모델은 TBD
- CD: GitHub Actions + **ArgoCD GitOps 처음부터**
- 엔티티: `DiaryEntry` (Visit 미사용)
- 시간대: KST 단일
- 시크릿: K8s Secret + External Secrets Operator → AWS Secrets Manager (EKS)
- PWA: `next-pwa` / EXIF: `metadata-extractor` / 지도: MapLibre + D3 시군구 GeoJSON

**Phase 0 후속 결정 (코드 작성 도중 도달 시 멈추고 사용자에게 확인):**
- Kafka 운영: AWS MSK vs Strimzi self-host
- ECR 이미지 공유 모델 (§3.3)
- DB 마이그레이션: Flyway vs Liquibase
- 옵저버빌리티 스택
- 종합 비용 추산 (사용자가 별도 요청함)

**금지:**
- 본인 식별자(이메일, Telegram chat id, GCal id, 본인 도메인) couple-diary commit 금지
- 외부 통합 코드는 couple-diary에 둠 (Telegram bot, GCal sync, n8n webhook 등) — credential·enable은 env 주입
- MVP spec의 "멀티테넌트 미고려" 문구는 무시. `couple_id` row-level 격리는 보안상 강제

문서 우선순위: **CLAUDE.md §0 > HANDOFF_v2.md > MVP spec**. MVP spec의 FastAPI/홈서버 단독 가정은 모두 outdated.

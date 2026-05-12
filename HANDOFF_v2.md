# Claude Code Handoff — couple-diary (juniqu-e/couple-diary @ develop)

이 폴더는 **하이파이 디자인 + 백엔드 통합 스펙**을 Claude Code에 넘기기 위한 패키지입니다.
대상 repo: `https://github.com/juniqu-e/couple-diary` (develop 브랜치)

---

## 1. 입력 자료

| 자료 | 위치 | 용도 |
|---|---|---|
| 하이파이 시안 9개 화면 | `hifi/index.html` + `hifi/screens/*.jsx` | 픽셀 레벨 참조. 컬러·간격·컴포넌트 구조 그대로 가져가기 |
| 디자인 토큰 | `hifi/tokens.css` | Tailwind config로 옮길 색상/타이포/spacing 원본 |
| 와이어프레임 (이전 버전) | `design_handoff_couple_app/` | UX 흐름·시안 비교용 (구버전이므로 hifi 우선) |

**Claude Code 작업 순서 권장:** 위 자료를 먼저 다 읽고 → `DESIGN_NOTES` 절(아래) → `DATA MODEL` → `API` → `BUILD ORDER` 순.

---

## 2. 현재 repo 상태 (develop, 2026-05)

### 백엔드 (`backend/`)
- **스택:** Spring Boot 3.5 / Java 21 / JPA / PostgreSQL 16
- **구현된 것:** `MediaController` + `StorageService`만 존재. **모든 도메인 로직은 미구현.**
- **사용 가능한 엔드포인트:**
  ```
  POST   /api/media/upload         multipart file → { fileName, fileUrl }
  GET    /api/media/download/{fileName}   파일 다운로드
  DELETE /api/media/{fileName}     파일 삭제
  GET    /api/media/health         { status, service }
  ```
- **storage:** S3Client (AWS SDK v2) — local은 MinIO, prod는 S3. 버킷 자동 생성됨.
- ⚠️ **`StorageService.getFileUrl()`은 현재 `bucketName/filename`만 리턴함 (실제 URL 아님).** MinIO presigned URL 또는 public endpoint 기반으로 고쳐야 함. (작업 1번 후보)

### 프론트엔드 (`frontend/`)
- **스택:** Next.js 16.2.2 / React 19.2 / TypeScript / Tailwind CSS 4
- **현재 파일:** `app/layout.tsx`, `app/page.tsx`, `app/globals.css`만 있는 빈 스캐폴드 (Next.js 기본 템플릿)
- **앱 라우터** 사용 (app/ directory). Pages router 아님.
- PWA화는 아직 안 됨 — `next-pwa` 또는 manifest+SW를 별도로 추가해야 함.

### 인프라
- `docker-compose.local.yml` — Postgres + MinIO + Backend + Frontend 다 띄움
- `application-local.yml`, `application-prod.yml` 프로파일 분리됨
- ⚠️ **인증/JWT 코드 없음.** Spring Security + JWT 라이브러리 도입부터 시작해야 함.

---

## 3. 용어 매핑 (디자인 ↔ 백엔드 도메인)

디자인 시안 용어가 README 기능 목록과 정확히 일치하지 않으니, **백엔드 엔티티는 아래 우측 이름으로 생성**:

| 디자인 시안 (한글) | 백엔드 엔티티 | 비고 |
|---|---|---|
| visit (방문 기록) | `DiaryEntry` | 1번의 외출 = 1 diary entry. 사진/캡션/위치/날짜 포함 |
| place (장소) | `Place` | DiaryEntry N → Place 1 (FK). 좌표·이름·카테고리 |
| 계획 / 다가오는 일정 | `Schedule` | start/end + isShared (개인/공유) + anniversary 자동 생성 |
| 사진 | `Photo` | DiaryEntry 1 → Photo N. EXIF 메타 분리 저장 |
| D-Day / 시작일 | `Couple.startedAt` | Couple 엔티티의 단일 필드 |
| 두 사람 / 우리 | `Couple` ↔ `User` (2명) | M:N이 아니라 Couple 1:2 User |
| 버킷리스트 (시안 없음) | `BucketItem` | ⚠️ **시안 없음 — 별도 와이어 필요 시 사용자에게 요청** |

**중요:** 시안에는 "버킷리스트"가 없습니다. README엔 있음. Claude Code는 버킷리스트 화면을 임의로 만들지 말고, 사용자에게 "시안 추가할까요?" 물어볼 것.

---

## 4. 제안 데이터 모델 (JPA)

```
User
  id (PK, Long)
  email, passwordHash, displayName, avatarColor
  createdAt
  couple_id (FK, nullable — 가입 직후엔 없음)

Couple
  id (PK)
  startedAt (LocalDate)     ← D-Day 기준
  inviteCode (unique, 짧은 토큰)
  createdAt
  // 멤버는 User.couple_id로 역참조

DiaryEntry  (= visit)
  id (PK)
  couple_id (FK)
  author_id (FK → User)
  place_id (FK → Place, nullable)
  visitedAt (LocalDateTime) ← D-Day 계산 + 정렬용
  caption (TEXT, markdown 허용)
  createdAt, updatedAt
  // photos: 1:N

Photo
  id (PK)
  diary_entry_id (FK)
  fileName (← StorageService가 리턴하는 UUID 키)
  width, height
  exif_json (JSONB)         ← EXIF 추출 시 채움. lat/lng도 여기서 추출 가능
  takenAt (LocalDateTime, EXIF에서)
  order (int, 같은 entry 안에서 순서)

Place
  id (PK)
  couple_id (FK)
  name
  category (ENUM: CAFE, RESTAURANT, PARK, OTHER…)
  lat, lng (DOUBLE)
  address
  firstVisitedAt (계산 캐시 가능)

Schedule
  id (PK)
  couple_id (FK)
  title
  startsAt, endsAt
  isShared (bool)            ← 공유 vs 개인
  isAnniversary (bool)       ← 자동 생성된 기념일이면 true
  createdBy (FK → User)

BucketItem
  id (PK)
  couple_id (FK)
  title
  category
  doneAt (nullable)
```

---

## 5. 제안 REST API

### Auth (Spring Security + JWT)
```
POST /api/auth/signup     { email, password, displayName } → { token, user }
POST /api/auth/login      { email, password }              → { token, user }
GET  /api/auth/me                                          → { user, couple? }
POST /api/auth/refresh    { refreshToken }                 → { token }
```

### Couple (커플 페어링)
```
POST /api/couple/create                        → { couple, inviteCode }
POST /api/couple/join     { inviteCode }       → { couple }
GET  /api/couple/me                            → { couple, members[2] }
PATCH /api/couple/me      { startedAt? }       → { couple }
```

### Diary entries (= visit)
```
GET    /api/entries?cursor=&limit=20          시간순 페이지네이션
GET    /api/entries/by-date/{yyyy-MM}         월별 (캘린더용)
POST   /api/entries       { placeId?, visitedAt, caption, photoIds[] }
GET    /api/entries/{id}
PATCH  /api/entries/{id}
DELETE /api/entries/{id}
```

### Places
```
GET    /api/places                            지도용 전체 리스트
GET    /api/places/{id}                       장소 + 방문 횟수/통계
GET    /api/places/{id}/entries               이 장소의 모든 entries
POST   /api/places       { name, category, lat, lng, address }
PATCH  /api/places/{id}
```

### Schedules
```
GET    /api/schedules?from=&to=               기간 조회
POST   /api/schedules
PATCH  /api/schedules/{id}
DELETE /api/schedules/{id}
GET    /api/schedules/anniversaries           자동 생성 (100일/200일/1년 등)
```

### Media (이미 있음 — 재사용)
```
POST   /api/media/upload                      ← 그대로 사용
DELETE /api/media/{fileName}                  ← 그대로 사용
```
⚠️ **개선 필요:** `getFileUrl()` 을 MinIO presigned URL 또는 public path로 고치기. 프론트가 `<img src={fileUrl}>` 로 바로 쓸 수 있어야 함.

### Dashboard (S1 오늘 화면용)
```
GET    /api/dashboard/today
       → { dDay, upcomingSchedules[], recentEntries[], latestPhoto }
```

---

## 6. 화면 ↔ API 매핑

| 화면 | 호출하는 API |
|---|---|
| S1 오늘 | `GET /api/dashboard/today` (단일 호출로 한 번에) |
| S2 추억 | `GET /api/entries?cursor=…` (타임라인) / `GET /api/places` (지도) |
| S3 계획 | `GET /api/schedules?from&to` + `GET /api/schedules/anniversaries` |
| S4 우리 | `GET /api/couple/me` + 통계용 집계 엔드포인트 (TBD) |
| M1 새 기록 | `POST /api/media/upload` (N번) → `POST /api/entries` |
| M2 visit 상세 | `GET /api/entries/{id}` |
| M3 장소 상세 | `GET /api/places/{id}` + `GET /api/places/{id}/entries` |
| M4 라이트박스 | 이미 로드된 entry 데이터 재사용 (별도 호출 X) |
| M5 온보딩 | `POST /api/auth/signup` → `POST /api/couple/create` 또는 `/join` → `PATCH /api/couple/me { startedAt }` |

---

## 7. Build Order (Claude Code에 명시)

**Phase 0 — 기반 (백엔드부터):**
1. Spring Security + JWT 설정, `User`/`Couple` 엔티티, `/api/auth/*` 구현
2. `StorageService.getFileUrl()` → MinIO public URL 또는 presigned URL 리턴하게 수정
3. `MultipartFile` 업로드 시 EXIF 추출(metadata-extractor 라이브러리) → `Photo.exif_json` 채우기

**Phase 1 — 디자인 토큰 + 셸:**
4. `hifi/tokens.css` → `frontend/tailwind.config.ts` 로 옮기기 (색·폰트·spacing). Gaegu 폰트는 Tailwind 4 `@theme`에 등록.
5. PWA 셋업: `next-pwa` 또는 manifest+SW
6. 모바일 하단 탭바 + 데스크탑 좌측 사이드바 레이아웃 (responsive)
7. 인증 라우트 `/login`, `/signup`

**Phase 2 — 핵심 흐름:**
8. **M5 온보딩** → 가장 먼저. 페어링 없이는 나머지 화면이 의미 없음.
9. `DiaryEntry`/`Place`/`Photo` 엔티티 + REST + Service
10. **M1 새 기록** (사진 우선 시안 = `hifi/screens/m1-create.jsx`)
11. **S1 오늘** (대시보드)
12. **M2 visit 상세** → **M4 라이트박스**
13. **S2 추억** (타임라인 먼저, 지도는 나중)

**Phase 3 — 확장:**
14. **S3 계획** + Schedule 엔티티 + 기념일 자동 생성 로직
15. **M3 장소 상세** + 통계
16. **S4 우리**
17. 지도 통합 (MapLibre GL JS 또는 Naver/Kakao map SDK)
18. 버킷리스트 — **시안 먼저 요청**

---

## 8. 디자인 시안 읽는 법

`hifi/index.html` 을 브라우저로 열면 9개 화면이 모바일+데스크탑 페어로 한 번에 보임. 각 시안의 React/JSX 소스를 그대로 옮기는 게 아니라 **시안의 구조·간격·색·타이포를 그대로 따라 Next.js (`app/` 라우터) 페이지로 재작성**할 것.

- `hifi/components.jsx` 의 `Phone`/`Desktop`/`StatusBar`/`I` (아이콘) 컴포넌트는 **시안 액자용 wrapper**임. 실제 앱에는 옮기지 말 것.
- 그 안의 화면 본문 div들이 옮길 대상.
- 클래스명 `card`, `chip`, `btn--primary`, `photo photo--cafe` 등은 `tokens.css` 정의를 봐야 함 — Tailwind utility로 1:1 변환하거나 `globals.css`에 같은 클래스를 정의.
- `photo--cafe`, `photo--gold`, `photo--dusk` 같은 그라데이션 placeholder는 **실제 사진 컴포넌트로 교체** (placeholder 그라데이션 유지하되 `<img>`가 있으면 그걸 우선).

---

## 9. 디자인 토큰 (Tailwind 4 변환 가이드)

`hifi/tokens.css` 안의 CSS 변수들을 Tailwind 4 `@theme` 블록으로:

```css
/* frontend/app/globals.css */
@import "tailwindcss";

@theme {
  --color-coral: #E8654F;
  --color-coral-deep: #C44432;
  --color-coral-wash: #FCE8E2;
  --color-gold: #C9A875;
  --color-gold-deep: #8A6D3E;
  --color-sage: #6B8E73;
  --color-ink: #2B2420;
  --color-bg: #FAF7F2;
  --font-display: "Gaegu", cursive;
  --font-sans: ...;
  --radius-md: 12px;
  /* etc */
}
```

`tokens.css` 의 모든 변수 → 위 형태로 옮기고, `bg-coral`, `text-ink`, `font-display` 등 Tailwind 유틸로 사용.

---

## 10. ⚠️ 사용자에게 확인 받을 것

Claude Code 첫 PR 이전에 다음을 사용자(=프로젝트 소유자)에게 물어볼 것:

1. **버킷리스트 화면 시안 — 없습니다. 만들고 진행하면 될까요, 아니면 시안 요청하시겠어요?**
2. **PWA 셋업 방식 — `next-pwa` 사용 OK인가요? (또는 manual SW)**
3. **지도 SDK — MapLibre(오픈) vs Naver Map vs Kakao Map vs Google?** (한국 위치 기반이라 Naver/Kakao 추천)
4. **EXIF 라이브러리 — `com.drewnoakes:metadata-extractor` 추가해도 될까요?**
5. **JWT 시크릿 관리 — `.env`에 둘지, AWS Secrets Manager 같은 외부에 둘지?**
6. **D-Day 기준 시간대 — KST 고정인가요? 사용자 디바이스 TZ 따르나요?**

---

## Claude Code에 보낼 첫 메시지 예시

> 이 repo는 develop 브랜치에서 작업 중이야. `design_handoff_couple_app/HANDOFF_v2.md` 부터 끝까지 읽어. 거기 7번 "Build Order"의 Phase 0부터 순서대로 진행해줘. Phase 0 (백엔드 인증/JWT) PR 하나로 마무리하고, 진행 전에 9번 "사용자에게 확인 받을 것" 질문 다 던져줘.

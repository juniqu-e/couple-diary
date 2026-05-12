# 커플 다이어리 앱 — MVP 설계 문서

**버전** v0.1 · **작성일** 2026-05-12 · **상태** 초안 (와이어프레임 직전)

---

## 1. 프로젝트 개요

### 1.1 목적
둘이서 함께 일정을 공유하고, 다녀온 장소를 기록하며, 기념일을 자연스럽게 챙기는 비공개 커플 전용 PWA. 외부 SNS·기존 다이어리 앱과의 차별점은 **"장소 중심 누적 기록"**과 **"내 인프라(홈서버)에서 완전한 데이터 소유"**.

### 1.2 MVP 범위
다음 3가지 기능에 집중. 그 외 아이디어(버킷리스트·한마디·감정기록 등)는 Phase 2로 분리.

1. **D-Day / 기념일 알림** — Google Calendar 자동 이벤트 생성
2. **일정 공유** — 공유 캘린더 + 개인 캘린더 풀링(옵션)
3. **데이트 장소 기록** — 장소가 메인 키, 방문 누적, 지도 핀

### 1.3 사용자
2인 커플 (한 쌍의 사용자만 사용). 멀티 테넌트는 고려하지 않음(만약 추후 확장 시에는 `couples` 테이블 기준으로 격리).

### 1.4 플랫폼
- **프론트엔드**: PWA (모바일 우선, 반응형)
- **부가 진입점**: Telegram Bot (빠른 캡처용, 일부 명령어)
- **호스팅**: 자체 홈서버 (wnsdlr.com, Docker)

---

## 2. 페르소나

### 2.1 P1 — 작성자형 (예: 본인)
- 20대 후반, IT 직종, 안드로이드 주 사용
- Telegram·Notion·홈서버 친숙
- 데이터 소유에 민감, 외부 SaaS 의존 회피
- 디테일하게 기록하는 편, 사진 EXIF·위치 정보 활용

### 2.2 P2 — 향유자형 (예: 파트너)
- 20대 후반, IT 비종사자, iOS 주 사용
- 가벼운 입력만 선호 — 사진 1장 + 짧은 글
- 푸시 알림 받는 것에 더 큰 가치
- 기념일·약속 확인용으로 더 자주 진입

설계 원칙: **P1은 작성 흐름이 빠르고 디테일해야 / P2는 보는 흐름이 직관적이고 알림이 신뢰 가능해야**.

---

## 3. 유저 시나리오

### 3.1 시나리오 A — D-Day 알림 도착
**상황** 화요일 아침 9시, 둘 다 출근길.

1. n8n이 새벽에 D-200 도래를 감지, 공유 Google Calendar에 "💝 200일" 이벤트 자동 생성
2. Google Calendar 기본 알림이 9시에 양쪽 폰에 푸시
3. P2가 PWA 진입 → [오늘] 탭에 D-200 카운터 + 다음 기념일 카드
4. P1에게 "내일 뭐할까?" 카톡 전송

**거치는 화면** 외부 알림 → [오늘]
**빈도** 100일 단위, 1주년 단위, 양쪽 생일·기념일 등 → 월 1~3회 수준

### 3.2 시나리오 B — 약속 잡기 전 일정 확인
**상황** 금요일 오후. P2가 "이번 주말 시간 돼?" 메시지.

1. P1이 PWA [계획] 탭 진입
2. 캘린더에서 이번 주말 토일 일정 스캔 — 토요일 오후 막힘 / 일요일 비어있음
3. 일정 카드 탭 → 구글 캘린더 앱으로 점프, 새 이벤트 추가 (공유 캘린더 선택)
4. 추가 즉시 양쪽에 동기화 노출

**거치는 화면** [계획] → 캘린더 뷰 → (외부 GCal 앱) → [계획] 갱신
**빈도** 약속 조율할 때마다. 진입 잦음, 머무는 시간 짧음.

### 3.3 시나리오 C — 다녀온 곳 기록 (메인)
**상황** 일요일 밤. 낮에 성수동 카페 + 한남동 식당 다녀옴.

1. P2가 PWA [추억 → 새 기록] 진입
2. **장소 입력**: "성수동 ㅇㅇ카페" 입력 → 네이버 Local Search 결과 드롭다운 → 선택 → place 자동 생성(또는 기존 매칭)
3. 사진 5장 첨부 (EXIF에 GPS 있으면 자동 매칭됨)
4. 짧은 글 작성 → 저장
5. 같은 흐름으로 "한남동 ㅁㅁ식당" 두 번째 visit 작성
6. P1에게 Telegram 알림 ("새 기록 2개")
7. P1이 같은 visit 들어가서 자기 글·사진 추가 (한 visit에 둘이 각자 작성)
8. 다음 주, P2가 [추억 → 지도(국내)] 진입 → 성수동 핀 탭 → 장소 상세에 visit 1개(오늘 거)
9. 한 달 후 다시 같은 카페 → 같은 place에 visit 누적 2개 ("3번째 왔다" 정서)

**거치는 화면** [추억] → 새 기록 작성 → [지도] → 장소 상세 → visit 상세
**빈도** 작성은 주 1~3회, 조회는 더 자주.

---

## 4. 기능적 요구사항 (Functional Requirements)

### 4.1 인증·계정
- **FR-1.1** Google OAuth 로그인 (Calendar scope 포함)
- **FR-1.2** 커플 페어링 — 초대 코드 발급 → 상대방이 입력해서 연결
- **FR-1.3** 커플 정보: 만난 날, 양쪽 생일, 결혼기념일(선택)

### 4.2 D-Day / 기념일
- **FR-2.1** 만난 날 기준 자동 D-Day 계산
- **FR-2.2** 기념일 자동 생성 규칙: 만난 날 기준 +100, +200, +300…(100 단위) / +365, +730…(1년 단위) / 양쪽 생일 매년 / 결혼기념일 매년
- **FR-2.3** 사용자 정의 기념일 등록 (수동)
- **FR-2.4** 다가오는 기념일을 n8n이 매일 새벽 스캔 → 임박한 항목을 공유 Google Calendar에 이벤트 자동 생성 (7일 전 / 당일 알림 2회)
- **FR-2.5** 이벤트 제목 규칙: `💝 만난 지 200일`, `🎂 OOO 생일` 등

### 4.3 일정
- **FR-3.1** 공유 Google Calendar 자동 생성 (페어링 직후) — 둘 다 쓰기 권한
- **FR-3.2** PWA 캘린더 뷰: 월간 / 주간 / 일간 토글
- **FR-3.3** 공유 캘린더 일정 표시 (진한 색)
- **FR-3.4** 개인 캘린더 풀링 옵션 — 각자 메인 캘린더 추가 연동, ON/OFF 토글. 노출 시 옅은 색 + 작성자 아바타 도트
- **FR-3.5** 일정 카드 탭 → 구글 캘린더 앱/웹 deep link 점프 (편집은 GCal에서)
- **FR-3.6** PWA에서 직접 일정 추가는 MVP 범위 외 (GCal에서 추가, 자동 반영)

### 4.4 장소·방문 기록
- **FR-4.1** 장소 등록 흐름
  - 한국 장소: 네이버 Local Search API로 검색 → 결과 중 선택 → name·lat·lng·address·naver_place_id 자동 저장
  - 해외 장소: 도시 단위로 직접 입력 (도시명·국가·대표 좌표 수동)
- **FR-4.2** 방문(visit) 작성
  - place 선택(또는 신규 등록)
  - 방문일자 (기본값: 오늘)
  - 둘이 각자의 글 (visit_entries, 작성자별 분리)
  - 둘이 각자의 사진 (visit_photos, 작성자별 분리)
- **FR-4.3** 양쪽 모두 visit 수정·삭제 권한 보유
- **FR-4.4** 같은 날 여러 장소 방문 시 visit 별도 생성 (장소당 1 visit)
- **FR-4.5** 사진 업로드: EXIF에서 촬영시각·GPS 추출, MinIO에 원본 저장, 썸네일 별도 생성

### 4.5 지도
- **FR-5.1** 지도 화면 토글 세그먼트 `[국내 🇰🇷] [해외 🌏]`
- **FR-5.2** 국내 — D3.js + 한국 시/군/구 GeoJSON, 핀은 visit 좌표
- **FR-5.3** 해외 — MapLibre GL JS + OSM 벡터 타일, 핀은 city 단위
- **FR-5.4** 핀 클릭 → 장소 상세 패널 슬라이드 업
- **FR-5.5** 누적 방문 횟수가 많은 핀은 시각적으로 강조 (크기 또는 색 농도)

### 4.6 장소 상세
- **FR-6.1** 장소명·주소·총 방문 횟수·최초/최근 방문일 표시
- **FR-6.2** visit 목록 (시간 역순, 작성자 아바타 표시)
- **FR-6.3** 외부 지도 리다이렉트 버튼
  - 한국: 네이버 지도 (place_id 있으면 비즈니스 페이지로, 없으면 좌표 마커)
  - 해외: Google Maps 좌표 마커
- **FR-6.4** 길찾기 버튼 (네이버 지도 nmap://route 또는 Google Maps directions)

### 4.7 Telegram Bot
- **FR-7.1** 명령어: `/diary` — PWA의 새 기록 작성 URL을 채팅으로 전송
- **FR-7.2** 명령어: `/today` — 오늘의 D-Day와 다가오는 기념일 응답
- **FR-7.3** 명령어: `/last` — 가장 최근 visit 카드 응답
- **FR-7.4** 신규 visit 작성 시 상대방에게 자동 알림 메시지
- **FR-7.5** EXIF GPS 손실을 막기 위해 사진은 "파일로 보내기" 안내 (메시지로 안내, 자동 첨부는 PWA에서)

---

## 5. 비기능적 요구사항 (Non-Functional Requirements)

### 5.1 성능
- **NFR-1.1** PWA 초기 로드 ≤ 3초 (3G 기준)
- **NFR-1.2** 지도 화면 핀 100개까지 60fps 유지
- **NFR-1.3** 이미지 썸네일 lazy load, 원본은 명시적 요청 시에만

### 5.2 가용성·신뢰성
- **NFR-2.1** 홈서버 기준 목표 가동률 99% (월 다운타임 ≤ 7시간)
- **NFR-2.2** Cloudflare Tunnel을 통한 외부 접근 (ISP 80/443 차단 우회 기설정)
- **NFR-2.3** n8n 워크플로우 실패 시 Telegram 본인 채널로 알림

### 5.3 보안·프라이버시
- **NFR-3.1** Google OAuth, 자체 비밀번호 보관 X
- **NFR-3.2** 모든 API 통신 HTTPS (Cloudflare 처리)
- **NFR-3.3** 사진·일기 원본은 외부 클라우드로 나가지 않음 (MinIO 자체 호스팅)
- **NFR-3.4** 커플 페어링 외 다른 사용자는 데이터에 접근 불가 (couple_id 기반 row-level 격리)
- **NFR-3.5** 세션 토큰 만료 90일, 갱신 가능

### 5.4 데이터 무결성·백업
- **NFR-4.1** PostgreSQL 일 1회 pg_dump → MinIO 별도 버킷
- **NFR-4.2** MinIO 데이터는 주 1회 외장 디스크로 sync
- **NFR-4.3** 백업 보관 정책: 일별 30일, 주별 12주

### 5.5 운영·확장성
- **NFR-5.1** 모든 서비스 Docker 컨테이너 (compose 단일 정의)
- **NFR-5.2** Nginx Proxy Manager로 도메인·SSL 관리
- **NFR-5.3** 추후 멀티 커플 확장 가능성 — 스키마는 `couple_id` FK 기준으로 설계

### 5.6 사용성
- **NFR-6.1** 모바일 우선 (375px ~ 430px 폭에서 최적), 데스크탑은 보조
- **NFR-6.2** 오프라인 시 마지막 로드한 데이터는 조회 가능 (PWA 캐시)
- **NFR-6.3** 한국어 UI 단일 (i18n은 범위 외)
- **NFR-6.4** WCAG AA 수준 색 대비 (다크모드 대응)

---

## 6. 데이터 모델

### 6.1 ERD 개요
```
couples ──┬─< users
          │
          ├─< anniversaries     (기념일 마스터, 자동/수동 둘 다)
          │
          ├─< places ──< visits ──< visit_entries
          │                      ──< visit_photos
          │
          └─ google_calendars   (공유 캘린더 1, 개인 캘린더 N)
```

### 6.2 테이블 정의

```sql
-- 커플 단위
couples (
  id, name, start_date, marriage_date NULL,
  shared_gcal_id, created_at
)

-- 사용자
users (
  id, couple_id FK,
  google_email, name, birthday,
  avatar_minio_key, role ('P1'|'P2'),
  personal_gcal_id NULL,
  personal_gcal_enabled BOOL DEFAULT false
)

-- 기념일 (자동 생성 + 수동 등록)
anniversaries (
  id, couple_id FK,
  type ('day_count'|'birthday'|'marriage'|'custom'),
  rule TEXT,        -- 예: "every 100 days from start_date"
  title, next_occurrence_at,
  gcal_event_id NULL  -- 생성한 GCal 이벤트 추적
)

-- 장소 (메인 키)
places (
  id, couple_id FK,
  name, lat, lng,
  country ('KR'|'OTHER'),
  region,           -- 한국: 시/구 / 해외: 도시명
  address NULL,
  naver_place_id NULL,
  created_by FK users, created_at
)

-- 방문 (장소 × 1회)
visits (
  id, place_id FK, couple_id FK,
  visit_date, created_by FK users,
  last_edited_by FK users, updated_at
)

-- 방문별 작성자별 글
visit_entries (
  id, visit_id FK, user_id FK,
  body TEXT, updated_at
  -- UNIQUE(visit_id, user_id)  ← 사용자당 visit에 1개
)

-- 방문별 사진
visit_photos (
  id, visit_id FK, user_id FK,
  minio_key, thumbnail_minio_key,
  taken_at NULL, exif_lat NULL, exif_lng NULL,
  uploaded_at
)
```

### 6.3 인덱스 가이드
- `visits(place_id, visit_date DESC)` — 장소 상세에서 시간 역순 조회
- `visits(couple_id, visit_date DESC)` — 타임라인 뷰
- `places(couple_id, country)` — 국내/해외 지도 분리 조회
- `places(naver_place_id)` — 중복 등록 방지

---

## 7. 화면 명세

### 7.1 메인 네비게이션 (하단 탭 4개)

```
📱 4-Tab Bottom Navigation
├─ 1. 오늘 (Home)
├─ 2. 추억 (Memories)
├─ 3. 계획 (Plans)
└─ 4. 우리 (Us)
```

### 7.2 화면별 상세

#### S1. 오늘 (Home)
- 헤더: 두 사람 아바타 + 만난 지 N일
- D-Day 카운터 (큰 숫자) + 다음 기념일까지 D-N
- 카드: 다음 기념일 (제목·날짜·D-N)
- 카드: 최근 visit (썸네일 1장 + 장소명 + 둘의 짧은 글 발췌)
- CTA 버튼: 새 기록 작성

#### S2. 추억 (Memories)
- 상단 세그먼트: `[타임라인] [지도] [앨범]`
- **2a. 타임라인**: visit 카드 시간 역순 무한스크롤. 카드 = 장소명·날짜·썸네일 1~3장·작성자 아바타·짧은 글 1줄
- **2b. 지도**: `[국내 🇰🇷] [해외 🌏]` 토글. 국내 D3 한국 지도, 해외 MapLibre. 핀 탭 → 장소 상세 슬라이드 업
- **2c. 앨범**: 사진 그리드 (3열). 필터: 전체 / 작성자별 / 장소별
- FAB(우하단): 새 기록 작성

#### S3. 계획 (Plans)
- 캘린더 (월간 기본, 토글로 주간·일간)
- 일정 점: 공유=진한색 / 개인=옅은색+점
- 상단 필터 토글: `[공유만] [내 개인 포함] [전부]`
- 일자 탭 → 그날 일정 리스트 시트
- 일정 탭 → 구글 캘린더 앱/웹 deep link
- 하단 카드: 다가오는 기념일 3개 (자동 GCal 등록 표시)

#### S4. 우리 (Us)
- 커플 프로필 (만난 날, 양쪽 생일·결혼기념일)
- 연동 상태 카드
  - Google Calendar (공유·개인 별)
  - Telegram Bot
  - MinIO 사용량
- 데이터 백업·내보내기
- 테마 (라이트/다크/시스템)
- 알림 설정 (GCal 알림 시각, n8n 알림 채널)
- 로그아웃·페어링 해제

### 7.3 모달·상세 화면

#### M1. 새 기록 작성 (visit 작성)
- 단계 1: 장소 선택
  - 검색 입력 → 네이버 Local Search 자동완성 드롭다운
  - "직접 입력 (해외)" 버튼 → 도시명·국가 입력 폼
  - 기존 등록된 장소 빠른 선택
- 단계 2: 방문일자 (기본 오늘)
- 단계 3: 글 작성 (자신의 entry)
- 단계 4: 사진 첨부 (다중)
- 저장 버튼 → 저장 후 visit 상세로 이동

#### M2. visit 상세
- 장소명·날짜
- 양쪽 글 카드 (작성자 아바타 + body)
  - 본인 글: 편집 가능
  - 상대 글: 읽기 전용 (단 visit 자체 삭제는 양쪽 권한)
- 사진 그리드 (작성자별 살짝 구분)
- 액션: 글 추가 / 사진 추가 / 삭제

#### M3. 장소 상세
- 장소명·주소·총 방문 횟수·최근 방문일
- 외부 지도 버튼: `[🗺️ 네이버 지도]` `[🚗 길찾기]`
- visit 목록 (시간 역순) — 탭하면 M2로

#### M4. 사진 상세 (라이트박스)
- 풀스크린 사진 + 메타데이터 (촬영시각·EXIF GPS)
- 좌우 스와이프로 visit 내 다음/이전 사진

#### M5. 온보딩 (최초 1회)
- Google 로그인
- 커플 페어링: 초대 코드 발급 또는 입력
- 만난 날 입력
- 양쪽 생일·결혼기념일(선택)
- 공유 캘린더 자동 생성 안내

### 7.4 화면 ↔ 시나리오 매핑

| 시나리오 | 거치는 화면 |
|---|---|
| A. D-Day 알림 | 외부 GCal 알림 → S1(오늘) |
| B. 약속 잡기 | S3(계획) → 외부 GCal 앱 → S3 |
| C. 다녀온 곳 기록 | S2(추억) → M1(새 기록) → M2(visit) → S2 지도 → M3(장소 상세) |

---

## 8. 외부 시스템 연동

### 8.1 Google Calendar
- OAuth 2.0, 스코프: `calendar.events`, `calendar`
- 페어링 직후 공유 캘린더 1개 자동 생성, 양쪽에 권한 부여
- n8n 워크플로우가 매일 새벽 03:00 anniversaries 스캔 → GCal 이벤트 생성/갱신
- 일정 deep link: `https://calendar.google.com/calendar/event?eid={eventId}`

### 8.2 네이버 Local Search API
- Endpoint: `https://openapi.naver.com/v1/search/local.json`
- 무료 쿼터 월 25,000건 (충분)
- 응답에서 `title`(HTML 태그 제거), `mapx`/`mapy` (KATEC → WGS84 변환), `address`, `link` 추출
- 클라이언트가 직접 호출하지 않고 백엔드 프록시 경유 (키 노출 방지)

### 8.3 외부 지도 리다이렉트
- 네이버 지도 웹 URL: `https://map.naver.com/p?title=...&lat=...&lng=...`
- 네이버 place 직접: `https://map.naver.com/p/entry/place/{naver_place_id}`
- 길찾기: `nmap://route/public?dlat=...&dlng=...&dname=...&appname=...`
- Google Maps: `https://www.google.com/maps/search/?api=1&query=lat,lng`

### 8.4 Telegram Bot
- BotFather에서 봇 생성
- n8n의 Telegram Trigger 노드로 명령어 수신
- 알림 발송은 n8n Telegram Send 노드
- 그룹/개인 모두 가능, MVP에선 개인 1:1 봇 두 개 (각자 따로)

### 8.5 MinIO
- 버킷: `couple-photos` (원본), `couple-thumbs` (썸네일), `couple-backup` (DB 덤프)
- Presigned URL로 PWA에서 직접 업로드/다운로드
- 사진 업로드 후 별도 워커가 thumbnail 생성 + EXIF 추출 (Python + Pillow)

---

## 9. 기술 스택

| 레이어 | 선택 | 이유 |
|---|---|---|
| 프론트엔드 | Next.js (App Router) PWA | SSR + 정적 자원 + Service Worker 한 번에 |
| 인증 | Auth.js + Google OAuth | 표준, Calendar scope 통합 |
| 상태 관리 | TanStack Query | 서버 상태 캐싱·낙관적 업데이트 |
| 백엔드 API | FastAPI | 빠른 OpenAPI, 비동기 |
| DB | PostgreSQL 16 | 관계형 + JSON 필드 둘 다 |
| 미디어 저장 | MinIO | 자체 호스팅 S3 호환, 기존 자산 활용 |
| 자동화 | n8n | 기념일 스캔·Telegram·GCal 동기화 |
| 지도(국내) | D3.js + 통계청 시군구 GeoJSON | 시그니처 시각화 |
| 지도(해외) | MapLibre GL JS + OSM 타일 | 무료 OSS, 도시 단위 핀 |
| 메시징 | Telegram Bot API | 빠른 캡처 진입점 |
| 외부 검색 | 네이버 Local Search API | 한국 장소 정확도 |
| 호스팅 | Docker Compose · NPM · Cloudflare Tunnel | 기존 홈서버 자산 재활용 |

---

## 10. MVP 일정 가이드 (러프)

| 단계 | 주요 산출물 | 예상 소요 |
|---|---|---|
| 1. 와이어프레임 | 화면 7~10개 박스+화살표 | 1주 |
| 2. 비주얼 디자인 | Claude Design 시안 → 컴포넌트 시스템 | 1~2주 |
| 3. DB·API 스캐폴딩 | FastAPI + PostgreSQL + Auth | 1주 |
| 4. 프론트 골격 | Next.js 4탭 + 라우팅 + 인증 | 1주 |
| 5. 장소·visit 기능 | M1·M2·M3 + 네이버 검색 + MinIO | 1.5주 |
| 6. 지도 | D3 한국 + MapLibre 해외 + 핀 인터랙션 | 1주 |
| 7. 일정·기념일 | GCal 연동 + n8n 자동화 + Telegram | 1주 |
| 8. 마무리 | PWA 설정·백업·QA | 0.5주 |

총 8~9주 (주말 작업 기준 더 길어질 수 있음)

---

## 11. Phase 2 (MVP 이후 후보)

- 버킷리스트 (같이 할 것 / 맛집 / 영화)
- 한마디·롤링페이퍼
- 데일리 감정 기록
- Samsung Health Data SDK 연동 (걸음 수 공유 등)
- Wear OS 컴플리케이션 (D-Day, 최근 visit)
- 알림 채널 추가: PWA FCM 푸시, 카카오 알림톡(필요 시)
- 일정 PWA 내 직접 추가/편집 (현재는 GCal 점프)
- 행정구역 폴리곤 채색 (한국 시/구 단위 방문 음영)

---

## 12. 결정 사항 로그

| 일자 | 결정 | 근거 |
|---|---|---|
| 2026-05-12 | 알림은 GCal 자동 이벤트 단일 채널 | 카카오 알림톡 비용·심사 부담, FCM은 사용자 권한 마찰. GCal이 가장 가벼움 |
| 2026-05-12 | 일정은 공유 캘린더 + 개인 풀링(옵션) 하이브리드 | 프라이버시 분리하면서도 누락 우려 해소 |
| 2026-05-12 | 국내 D3 + 해외 MapLibre 분리, 토글 세그먼트 | 시그니처(한국 지도)와 실용성(해외) 모두 챙김. D3로 전세계 시/구는 불가 |
| 2026-05-12 | 장소 중심 데이터 모델 (visit이 place에 종속) | "같은 곳 N번째 왔다" 정서 살리기. 같은 날 여러 장소 = visit 따로 |
| 2026-05-12 | 장소 등록은 네이버 Local Search API + 해외 직접 입력 | 정확도와 입력 부담의 균형 |
| 2026-05-12 | Phase 2로 분리한 것들 | MVP 범위 통제, deliverable 압박 줄임 |

---

## 13. 미해결·확인 필요

- [ ] 사진 EXIF GPS와 등록된 place 좌표 불일치 시 처리 정책 (자동 매칭 임계값?)
- [ ] visit 작성 시 한쪽이 글 쓰고 다른 쪽이 안 쓰는 경우 UI 처리 (placeholder vs 숨김)
- [ ] 페어링 해제 후 데이터 정책 (보존 / 백업만 / 삭제)
- [ ] 다크모드 색 토큰 (Claude Design에서 결정)
- [ ] 폰트 (Pretendard 확정?)
- [ ] 도메인 — wnsdlr.com 서브도메인? (예: `couple.wnsdlr.com`)

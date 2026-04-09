# Couple Diary

[English](../README.md)

커플을 위한 일정 관리 & 추억 저장 오픈소스 웹 서비스

## 기능

- **일정 관리** - 캘린더 기반 커플 공통/개인 일정, 기념일 자동 생성
- **다이어리** - 타임라인/캘린더 뷰, 사진 첨부, 마크다운 지원
- **사진 갤러리** - S3 호환 스토리지 기반 사진 저장, EXIF 메타데이터 자동 추출
- **추억 지도** - 지도 기반 추억/여행 계획 핀 관리
- **버킷리스트** - 카테고리별 버킷리스트 관리

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.5, Java 21, JPA |
| Frontend | Next.js 15, TypeScript, Tailwind CSS |
| Database | PostgreSQL 16 |
| Storage | AWS S3 / MinIO (S3 호환) |
| Infra | Docker Compose |

## 시작하기

### 사전 요구사항

- Docker & Docker Compose

### 로컬 개발

```bash
git clone https://github.com/juniqu-e/couple-diary.git
cd couple-diary

# 전체 서비스 실행 (DB + MinIO + Backend + Frontend)
docker compose -f docker-compose.local.yml up -d
```

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| MinIO Console | http://localhost:9001 |

### 운영 배포

```bash
# 환경변수 설정
cp .env.example .env
# .env 파일을 편집하여 실제 값 입력

docker compose up -d
```

### 스토리지 설정

S3 호환 스토리지를 모두 지원합니다:

| 제공자 | `STORAGE_ENDPOINT` |
|--------|-------------------|
| AWS S3 | *(비워두기)* |
| MinIO | `http://minio:9000` |
| Cloudflare R2 | `https://<account-id>.r2.cloudflarestorage.com` |

## 프로젝트 구조

```
couple-diary/
├── backend/                  # Spring Boot 백엔드
│   └── src/main/resources/
│       ├── application.yml           # 공통 설정
│       ├── application-local.yml     # 로컬 개발 설정
│       └── application-prod.yml      # 운영 설정
├── frontend/                 # Next.js 프론트엔드
├── docs/                     # 프로젝트 문서
├── docker-compose.yml        # 운영 배포용
└── docker-compose.local.yml  # 로컬 개발용
```

## 기여하기

1. 레포지토리 Fork
2. Feature 브랜치 생성 (`git checkout -b feature/amazing-feature`)
3. 변경사항 커밋 ([커밋 컨벤션](Git_convention.md) 참고)
4. 브랜치에 Push (`git push origin feature/amazing-feature`)
5. Pull Request 생성

## 라이선스

MIT License - [LICENSE](../LICENSE) 참고

# Couple Diary

[한국어](docs/README_ko.md)

An open-source web service for couples to manage schedules, write diaries, and store memories together.

## Features

- **Schedule Management** - Calendar-based shared/personal schedules, auto-generated anniversaries
- **Diary** - Timeline & calendar views, photo attachments, markdown support
- **Photo Gallery** - S3-compatible storage, automatic EXIF metadata extraction
- **Memory Map** - Pin-based memory & travel plan management on a map
- **Bucket List** - Categorized bucket list with completion tracking

## Tech Stack

| Area | Technology |
|------|-----------|
| Backend | Spring Boot 3.5, Java 21, JPA |
| Frontend | Next.js 15, TypeScript, Tailwind CSS |
| Database | PostgreSQL 16 |
| Storage | AWS S3 / MinIO (S3-compatible) |
| Infra | Docker Compose |

## Getting Started

### Prerequisites

- Docker & Docker Compose

### Local Development

```bash
git clone https://github.com/juniqu-e/couple-diary.git
cd couple-diary

# Start all services (DB + MinIO + Backend + Frontend)
docker compose -f docker-compose.local.yml up -d
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| MinIO Console | http://localhost:9001 |

### Production Deployment

```bash
# Configure environment variables
cp .env.example .env
# Edit .env with your actual values

docker compose up -d
```

### Storage Configuration

Couple Diary uses the AWS S3 SDK, which supports any S3-compatible storage.  
The SDK automatically resolves the endpoint from the `region` when using AWS S3, so `STORAGE_ENDPOINT` can be left empty.  
For self-hosted or third-party storage, set the endpoint explicitly.

| Provider | `STORAGE_ENDPOINT` | Note |
|----------|-------------------|------|
| AWS S3 | *(leave empty)* | SDK auto-resolves from `STORAGE_REGION` |
| MinIO | `http://minio:9000` | Self-hosted, included in local dev compose |
| Cloudflare R2 | `https://<account-id>.r2.cloudflarestorage.com` | S3-compatible |

Example `.env` for MinIO:
```env
STORAGE_ENDPOINT=http://minio:9000
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin
STORAGE_BUCKET=couple-diary-media
STORAGE_REGION=us-east-1
```

Example `.env` for AWS S3:
```env
STORAGE_ENDPOINT=
STORAGE_ACCESS_KEY=AKIA...
STORAGE_SECRET_KEY=...
STORAGE_BUCKET=couple-diary-media
STORAGE_REGION=ap-northeast-2
```

## Project Structure

```
couple-diary/
├── backend/                  # Spring Boot backend
│   └── src/main/resources/
│       ├── application.yml           # Common config
│       ├── application-local.yml     # Local dev config
│       └── application-prod.yml      # Production config
├── frontend/                 # Next.js frontend
├── docs/                     # Documentation
├── docker-compose.yml        # Production deployment
└── docker-compose.local.yml  # Local development
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes ([Commit Convention](docs/Git_convention.md))
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT License - See [LICENSE](LICENSE) for details.

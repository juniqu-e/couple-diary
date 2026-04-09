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

Couple Diary supports any S3-compatible storage:

| Provider | `STORAGE_ENDPOINT` |
|----------|-------------------|
| AWS S3 | *(leave empty)* |
| MinIO | `http://minio:9000` |
| Cloudflare R2 | `https://<account-id>.r2.cloudflarestorage.com` |

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

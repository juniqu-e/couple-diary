# Git Commit Convention

Couple Diary 프로젝트의 Git 커밋 메시지 작성 규칙입니다.

## 커밋 메시지 형식

```
<type>: <subject>

[optional body]
```

## Type 종류

### init
- 프로젝트 초기 설정 및 보일러플레이트
- 예: `init: 프로젝트 기본 설정 파일 추가`

### feat
- 새로운 기능 추가
- 예: `feat: 사용자 인증 기능 구현`

### infra
- 인프라 및 DevOps 관련 설정
- CI/CD, Docker, 배포 설정 등
- 예: `infra: Jenkins CI/CD 파이프라인 구축`

### refactor
- 코드 리팩토링 (기능 변경 없음)
- 예: `refactor: UserService 로직 개선`

### docs
- 문서 작성 및 수정
- README, 주석, 기술 문서 등
- 예: `docs: API 문서 작성`

### fix
- 버그 수정
- 예: `fix: 로그인 시 토큰 만료 오류 수정`

### chore
- 기타 작업
- 의존성 업데이트, 설정 변경 등
- 예: `chore: 의존성 버전 업데이트`

### style
- 코드 포맷팅, 세미콜론 누락 등
- 기능에 영향을 주지 않는 코드 변경
- 예: `style: 코드 포맷팅 적용`

### test
- 테스트 코드 추가 및 수정
- 예: `test: UserService 단위 테스트 추가`

## Subject 작성 규칙

1. 50자 이내로 작성
2. 명령문 사용 (동사원형으로 시작)
3. 마침표(.)를 붙이지 않음
4. 한글로 작성

## Body 작성 규칙 (선택사항)

1. Subject와 한 줄 띄우기
2. 무엇을, 왜 변경했는지 작성
3. 72자마다 줄바꿈

## 예시

### 단순 커밋
```
feat: 파일 업로드 기능 추가
```

### 본문이 있는 커밋
```
feat: 파일 업로드 기능 추가

MinIO를 사용한 이미지 및 영상 파일 업로드 기능을 구현했습니다.
- MultipartFile 처리
- 파일 크기 제한 100MB
- UUID를 사용한 고유 파일명 생성
```

## 브랜치 전략

- `main`: 운영 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `hotfix/*`: 긴급 버그 수정 브랜치

## Commit 주의사항

1. 하나의 커밋은 하나의 기능/수정만 포함
2. 작은 단위로 자주 커밋
3. 의미 있는 메시지 작성
4. 커밋 전 테스트 확인

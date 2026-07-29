# Auth Service 설정 컨벤션

Discovery / Gateway 와 동일한 YAML 프로필 구조를 사용합니다.

## YAML 파일 구조

| 파일 | 역할 |
|------|------|
| `application.yml` | 모든 환경 공통 설정 (앱명, JPA, Eureka, springdoc) |
| `application-local.yml` | 로컬 개발 (팀 공통, Git 포함) |
| `application-dev.yml` | 통합/개발 서버 |
| `application-prod.yml` | 운영 서버 |

## 프로필 활성화

```yaml
spring.profiles.active: ${SPRING_PROFILES_ACTIVE:local}
```

- 개인 PC: 기본 `local`
- 통합 검증 노트북: `SPRING_PROFILES_ACTIVE=dev`
- 배포: `SPRING_PROFILES_ACTIVE=prod`

## 포트 및 Eureka

- `server.port: 0` — OS가 할당하는 사용 가능한 랜덤 포트
- Eureka instance-id: `${spring.application.name}:${random.uuid}` — 인스턴스마다 고유 ID
- `eureka.instance.prefer-ip-address: true`

## Secret 관리 규칙

**YAML 파일에 비밀번호, JWT Key, DB 계정을 직접 작성하지 않습니다.**

| 항목 | local | dev / prod |
|------|-------|------------|
| DB URL / 계정 / 비밀번호 | `.env` | 배포 환경변수 |
| JWT Secret | `.env` | 배포 환경변수 |
| Eureka URL (local) | `application-local.yml` (`localhost:8761`) | `${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE}` |

### 로컬 실행 준비

```bash
cp .env.example .env
# .env 파일에 실제 값 입력
```

개인별 override가 필요하면 Git에 포함되지 않는 `application-local-secret.yml` 을 추가할 수 있습니다.

## 로컬 실행

```bash
# 1. Discovery 실행
cd ../discovery && ./gradlew bootRun

# 2. .env 설정 후 Auth Service 실행
cd ../auth-service && ./gradlew bootRun
```

Eureka Dashboard (`http://localhost:8761`) 에서 `AUTH-SERVICE` 등록을 확인합니다.

## 필수 환경변수 (local)

| 변수 | 설명 |
|------|------|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 (32자 이상 권장) |

## 필수 환경변수 (dev / prod)

| 변수 | 설명 |
|------|------|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka Server URL |

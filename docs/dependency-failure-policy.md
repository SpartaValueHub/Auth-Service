# Redis 및 외부 인증 API 장애 정책

auth-service·gateway의 의존성(Redis, reCAPTCHA, PortOne) 장애 시 HTTP 상태·fail-open/closed 정책을 정의한다.

## 장애 정책 표

| 기능 | 정책 | HTTP (장애 시) | 코드 | 사유 |
|------|------|----------------|------|------|
| IP login rate limit | **fail-open** | (정상 허용) | — | 전체 로그인 중단 방지 |
| Login fail count / lock | **fail-closed** | 503 | `AUTH_SECURITY_STORE_UNAVAILABLE` | CAPTCHA/잠금 우회 위험 |
| Refresh token query/rotate | **fail-closed** | 503 | `AUTH_SECURITY_STORE_UNAVAILABLE` | Redis 없이 새 토큰 발급 금지 |
| Active access / blacklist | **fail-closed** | 503 | `AUTH_SECURITY_STORE_UNAVAILABLE` | 단일 세션·revoke 보장 |
| Logout revoke | **fail-closed** | 503 | `AUTH_SECURITY_STORE_UNAVAILABLE` | revoke 실패 시 204 금지 |
| Gateway blacklist | **fail-closed** | 503 | `AUTH_SECURITY_STORE_UNAVAILABLE` | protected API 무단 통과 방지 |
| reCAPTCHA (사용자 실패) | (기존) | 400/403 | `AUTH_CAPTCHA_INVALID` / `AUTH_CAPTCHA_REQUIRED` | 사용자 검증 실패 |
| reCAPTCHA (제공자 장애) | **fail-closed** | 503 | `AUTH_CAPTCHA_PROVIDER_UNAVAILABLE` | 미검증을 성공으로 처리 금지 |
| PortOne identity | **fail-closed** | 503 | `AUTH_IDENTITY_PROVIDER_UNAVAILABLE` | 외부 검증 없이 SUCCESS 저장 금지 |

**공통:** fail-closed 의존성 장애는 **401/403이 아닌 503**을 사용한다.

## 공통 503 응답

- **메시지:** `인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.`
- **Retry-After:** `auth.dependency-failure.retry-after-seconds` (기본 5초, env: `AUTH_DEPENDENCY_RETRY_AFTER_SECONDS`)
- **노출 금지:** DB URL, Redis host, 외부 API URL, token, JTI, requestToken, secret, PII

## Redis 기능별 동작

| Redis 키/기능 | 장애 시 | 비고 |
|---------------|---------|------|
| `login:rate:*` | allowed (fail-open) | Lua/execute 예외·null 결과 모두 허용 |
| `login:fail:*` / `login:lock:*` | 503 | 손상된 count → 503 (0으로 간주 안 함) |
| `auth:refresh:*` | 503 (execute 실패) / 401 (JTI mismatch) | missing key → rotate false |
| `auth:access:*` | 503 | missing key → empty |
| `auth:blacklist:access:*` | 503 | missing key → false (미블랙리스트) |

## reCAPTCHA 구분

| 상황 | 결과 |
|------|------|
| `success=false`, hostname/challenge_ts 불일치, blank token | `false` → `AUTH_CAPTCHA_INVALID` / `AUTH_CAPTCHA_REQUIRED` |
| 타임아웃, ConnectException, 5xx, 429, JSON 파싱 실패 | `CaptchaProviderUnavailableException` → 503 |
| 제공자 장애 시 | fail count 증가·잠금·자동 재시도 없음 |

## PortOne 오류 분류

| 상황 | 결과 |
|------|------|
| HTTP 404 | `Optional.empty()` → `IDENTITY_VERIFICATION_NOT_FOUND` |
| timeout / network / 5xx / 429 | `ExternalIdentityProviderUnavailableException` → 503 |
| malformed JSON / status 누락 / birthDate 파싱 실패 | fail-closed 503, SUCCESS DB 저장 없음 |
| 타임아웃 기본값 | connect 2000ms, read 5000ms (`PORTONE_CONNECT_TIMEOUT_MILLIS`, `PORTONE_READ_TIMEOUT_MILLIS`) |

## Gateway blacklist

| 상황 | 결과 |
|------|------|
| blacklist hit | 401 (기존) |
| blacklist miss | pass |
| Redis timeout/connection (protected JWT route) | 503 `AUTH_SECURITY_STORE_UNAVAILABLE` |
| public auth route (`sign-in`, `sign-up`, …) | Redis 장애와 무관하게 pass |

## Logout 쿠키 삭제

- **성공(204):** HttpOnly access/refresh 쿠키 삭제
- **503 (Redis revoke 실패):** GlobalExceptionHandler가 503 JSON만 반환 — **쿠키는 삭제되지 않음** (클라이언트에 토큰 잔존, 서버 revoke 미완료). 재시도·모니터링 필요.

## 비원자성 및 운영 한계

- Logout/refresh/inactive revoke는 여러 Redis 명령 순차 실행 — 부분 성공 가능
- DB `@Transactional`과 Redis 세션 정리는 원자적이지 않음
- IP rate limit fail-open 시 Redis 장애 중 brute-force 완화 불가 — 모니터링(`login_rate_limit_redis_failure`) 필요
- Gateway/Gateway blacklist 503 시 클라이언트는 Retry-After 후 재시도

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `AUTH_DEPENDENCY_RETRY_AFTER_SECONDS` | 5 | 503 Retry-After |
| `PORTONE_CONNECT_TIMEOUT_MILLIS` | 2000 | PortOne connect timeout |
| `PORTONE_READ_TIMEOUT_MILLIS` | 5000 | PortOne read timeout |
| `AUTH_CAPTCHA_CONNECT_TIMEOUT_MILLIS` | 2000 | reCAPTCHA connect timeout |
| `AUTH_CAPTCHA_READ_TIMEOUT_MILLIS` | 3000 | reCAPTCHA read timeout |

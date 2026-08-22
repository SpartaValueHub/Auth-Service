# Auth API

공통 Error Response:

```json
{
  "timestamp": "2026-08-04T08:00:00Z",
  "status": 400,
  "code": "ERROR_CODE",
  "message": "설명",
  "path": "/api/v1/..."
}
```

---

## 회원가입

### Summary
본인인증 SUCCESS 이력(`requestToken`) 확인 후 PortOne에서 CI·인증정보를 서버가 조회해 회원을 생성합니다. CI는 `identity_verifications.ci_hash`(HMAC-SHA256)로만 저장하며, `member_uuid`가 연결된 동일 `ci_hash` 이력으로 중복 가입을 방지합니다.

### Method · Path
`POST /api/v1/auth/sign-up`

### Auth
불필요 (Gateway public)

### Request (Body)

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| requestToken | string | O | PortOne identityVerificationId, confirm API SUCCESS |
| logInId | string | O | 영소문자+숫자 4~20 |
| password | string | O | 8~20, 대소문자·숫자·특수문자 각 1+ |
| email | string | O | 50자 이하 |

```json
{
  "requestToken": "identity-verification-001",
  "logInId": "user01",
  "password": "Password1!",
  "email": "user@example.com"
}
```

### Response (201)

| 필드 | 타입 |
|------|------|
| signupCompletionToken | string (member 프로필 생성 전용 단기 토큰) |
| authUuid | string |
| logInId | string |
| email | string |
| memberName | string |
| birthdayDate | string (yyyy-MM-dd) |

### Errors

| status | code | 의미 |
|--------|------|------|
| 400 | IDENTITY_VERIFICATION_NOT_READY | 본인인증 미완료 |
| 400 | IDENTITY_VERIFICATION_ALREADY_USED | requestToken 이미 사용 |
| 404 | IDENTITY_VERIFICATION_NOT_FOUND | 이력/PortOne 조회 실패 |
| 409 | AUTH_DUPLICATE_* | 중복 (loginId/email/phone/ci_hash 가입 이력) |
| 500 | INTERNAL_ERROR | 알 수 없는 DB 무결성 오류 (NOT NULL·FK 등) |

---

## 회원가입 재개 (completion token 재발급)

### Summary
auth 계정은 있으나 member 프로필 생성이 끝나지 않은 경우, 로그인 자격증명으로 signup completion token을 다시 발급합니다.

### Method · Path
`POST /api/v1/auth/sign-up/resume`

### Auth
불필요 (Gateway public — signup completion 경로)

### Request (Body)

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| logInId | string | O | |
| password | string | O | |
| captchaToken | string | △ | 로그인과 동일 — 실패 누적 시 필요 |

```json
{
  "logInId": "user01",
  "password": "Password1!"
}
```

### Response (200)

| 필드 | 타입 |
|------|------|
| authUuid | string |
| signupCompletionToken | string |

### Errors

로그인과 동일한 보안 정책(실패 누적·CAPTCHA·잠금·IP rate limit)을 적용합니다.
비밀번호·계정 존재 여부는 일반 로그인과 같은 메시지로 처리합니다.

| status | code | 의미 |
|--------|------|------|
| 401 | AUTH_UNAUTHORIZED | 아이디/비밀번호 오류 (1~4회 실패) |
| 403 | AUTH_CAPTCHA_REQUIRED | 5번째 실패 직후 또는 fail count ≥ 5 — captcha 필요 |
| 400 | AUTH_CAPTCHA_INVALID | captcha 누락/실패 — fail count 증가 없음 |
| 503 | AUTH_CAPTCHA_PROVIDER_UNAVAILABLE | Google siteverify 제공자 장애 — fail count 증가·잠금 없음 |
| 503 | AUTH_SECURITY_STORE_UNAVAILABLE | Redis security store 장애 — Retry-After 포함 |
| 423 | AUTH_ACCOUNT_LOCKED | 계정 잠금. `Retry-After`·`retryAfterSeconds` 포함 |
| 429 | AUTH_RATE_LIMITED | IP rate limit 초과. `Retry-After`·`retryAfterSeconds` 포함 |
| 403 | AUTH_MEMBER_NOT_ACTIVE | 비활성 계정 |

---

## 로그인

### Method · Path
`POST /api/v1/auth/sign-in`

### Auth
불필요

### Request (Body)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| logInId | string | O | |
| password | string | O | |
| captchaToken | string | △ | reCAPTCHA — 5번째 실패 직후부터 필수 |

### Response (200)

**Body** (JWT는 응답 본문에 포함하지 않음):

| 필드 | 타입 |
|------|------|
| memberUuid | string |
| nickname | string |
| role | string |

**Set-Cookie** (HttpOnly):

| Cookie | 설명 |
|--------|------|
| `vh_access_token` (설정: `auth.cookie.access-name`) | Access JWT, Max-Age=access TTL |
| `vh_refresh_token` (설정: `auth.cookie.refresh-name`) | Refresh JWT, Max-Age=refresh TTL |

Cookie 속성: `HttpOnly`, access·refresh 모두 `Path=/`, `SameSite=Lax`(기본), `Secure`(prod), `Domain`(선택)

```json
{
  "memberUuid": "550e8400-e29b-41d4-a716-446655440000",
  "nickname": "홍길동",
  "role": "USER"
}
```

### Errors

| status | code | 의미 |
|--------|------|------|
| 401 | AUTH_UNAUTHORIZED | 아이디/비밀번호 오류 (1~4회 실패) |
| 403 | AUTH_CAPTCHA_REQUIRED | 5번째 실패 직후 또는 fail count ≥ 5 — captcha 필요 (비밀번호 검증 전) |
| 400 | AUTH_CAPTCHA_INVALID | captcha 누락/실패 — fail count 증가 없음 |
| 503 | AUTH_CAPTCHA_PROVIDER_UNAVAILABLE | Google siteverify 제공자 장애(타임아웃·5xx·429·파싱 실패) — fail count 증가·잠금 없음 |
| 503 | AUTH_SECURITY_STORE_UNAVAILABLE | Redis security store 장애 — Retry-After 포함 |
| 503 | AUTH_IDENTITY_PROVIDER_UNAVAILABLE | PortOne 등 외부 본인인증 API 장애 — Retry-After 포함 |
| 423 | AUTH_ACCOUNT_LOCKED | fail count 6 — captcha 통과 후 비밀번호 오류 시 잠금. `Retry-After` 헤더·`retryAfterSeconds` 포함 |
| 429 | AUTH_RATE_LIMITED | IP rate limit 초과 — 비밀번호·captcha·user 조회 없음. `Retry-After`·`retryAfterSeconds` 포함 |

423 응답 예시:

```json
{
  "status": 423,
  "code": "AUTH_ACCOUNT_LOCKED",
  "message": "로그인 시도가 많아 1분간 로그인이 제한됩니다.",
  "retryAfterSeconds": 60
}
```

잠금 중 재시도 시 `message`는 `로그인이 일시적으로 제한되었습니다. 잠시 후 다시 시도해 주세요.`이며 `retryAfterSeconds`는 Redis lock TTL 잔여 초.

429 응답 예시:

```json
{
  "status": 429,
  "code": "AUTH_RATE_LIMITED",
  "message": "로그인 요청이 많습니다. 잠시 후 다시 시도해 주세요.",
  "retryAfterSeconds": 60
}
```

| 403 | AUTH_MEMBER_NOT_ACTIVE | member_status ≠ ACTIVE |

**로그인 실패 정책 (`auth.login-attempt`)**

| 설정 | 기본값 | 설명 |
|------|--------|------|
| captcha-threshold | 5 | fail count 5 → CAPTCHA_REQUIRED (5번째 실패 응답) |
| lock-threshold | 6 | fail count 6 → 잠금(423) |
| lock-duration-minutes | 1 | 잠금 TTL(분), 만료 시 fail·lock 초기화 |
| fail-count-window-minutes | 10 | 잠금 전 fail count TTL(분) |

**reCAPTCHA v2 서버 검증 (`captcha.recaptcha`)**

Google reCAPTCHA v2 Checkbox `siteverify` 응답을 서버에서 재검증합니다. v3 score/action은 사용하지 않습니다.

| 설정 | env | 기본값 | 설명 |
|------|-----|--------|------|
| enabled | `CAPTCHA_ENABLED` | true | false면 verify 항상 통과(로컬 테스트) |
| secret-key | `RECAPTCHA_SECRET_KEY` | — | Google Secret Key (enabled=true 시 필수) |
| allowed-hostnames | `AUTH_CAPTCHA_ALLOWED_HOSTNAMES` | `localhost,127.0.0.1` | siteverify `hostname` 허용 목록(쉼표 구분, trim·소문자 정규화, **exact match**). **비어 있으면 모든 hostname 거부** — prod는 반드시 설정 |
| challenge-max-age-seconds | `AUTH_CAPTCHA_CHALLENGE_MAX_AGE_SECONDS` | 120 | `challenge_ts` 최대 허용 경과(초, ≥1). 누락·파싱 실패·미래·만료 시 `AUTH_CAPTCHA_INVALID` |

검증 성공 조건(모두 충족):

- `success == true`
- `hostname`이 allowlist에 exact match
- `challenge_ts`가 UTC 기준 유효하고 `challenge-max-age-seconds` 이내

Google **사용자 검증 실패**(success=false, hostname/challenge_ts 불일치) → `AUTH_CAPTCHA_INVALID` / `AUTH_CAPTCHA_REQUIRED`.  
**제공자 장애**(타임아웃·5xx·429·응답 파싱 실패) → `503 AUTH_CAPTCHA_PROVIDER_UNAVAILABLE`(fail-closed). 클라이언트에는 Google `error-codes`·토큰 미노출.  
상세 정책: [dependency-failure-policy.md](./dependency-failure-policy.md)

**Redis 로그인 키**

| Key | 값 |
|-----|-----|
| `login:fail:{loginId}` | 실패 횟수 |
| `login:lock:{loginId}` | 잠금 플래그 |

로그인 성공 또는 TTL 만료 시 키 삭제/무효화.

**IP rate limit (`auth.login-rate-limit`) — loginId fail count와 독립**

| 설정 | 기본값 | 설명 |
|------|--------|------|
| enabled | true | IP rate limit 사용 여부 |
| max-attempts | 20 | 윈도우 내 허용 sign-in POST 수 |
| window-seconds | 60 | 카운터 윈도우(초) |
| block-seconds | 60 | 초과 시 차단 TTL(초). 성공 시 카운터 리셋 없음 |

| Key | 값 |
|-----|-----|
| `login:rate:count:{sha256(ip)}` | IP별 sign-in 요청 수 (window TTL) |
| `login:rate:block:{sha256(ip)}` | IP 차단 플래그 (block TTL) |

**신뢰 프록시·클라이언트 IP (rate limit 전제조건)**

| 계층 | 역할 | 미구성 시 |
|------|------|-----------|
| **Gateway** (`StripUntrustedForwardedHeadersFilter`) | ingress에서 클라이언트 `Forwarded` / `X-Forwarded-*` **제거** → Gateway `ForwardedHeadersFilter`가 TCP remote address 기준으로 재설정 | 클라이언트 X-Forwarded-For spoofing → auth rate limit 우회 |
| **auth-service** (`server.forward-headers-strategy=native` + Tomcat `internal-proxies`) | **사설망 게이트웨이**에서 온 X-Forwarded-For만 신뢰 → `getRemoteAddr()` = 실제 클라이언트 IP | 직접 접속 + `framework` 전략 시 spoofing 우회 |
| **auth-service 직접 노출** | Eureka 랜덤 포트(`server.port=0`) — **인터넷/FE 직접 노출 금지**. 방화벽·K8s NetworkPolicy로 Gateway만 허용 | 직접 접속 시 spoofing 없이도 API 우회·rate limit 분산 |

- Rate limit **집행 위치: auth-service** (Lua + Redis). Gateway는 IP 헤더 정합성만 담당.
- `ClientIpResolver`는 X-Forwarded-For 직접 파싱·DNS 조회 없음 — Tomcat RemoteIpValve 결과만 정규화.
- 운영: `AUTH_TRUSTED_PROXY_PATTERNS`에 **실제 Gateway/Pod CIDR** 반영 (기본값은 RFC1918 사설망).

**단일 세션 (이중 로그인 방지)**

- 계정당 Redis에 refresh jti 1개(`auth:refresh:{authUuid}`) + 활성 access jti 1개(`auth:access:{authUuid}`)만 유지
- **새 로그인** 시 기존 access jti → `auth:blacklist:access:{jti}` 등록, refresh jti 덮어쓰기 → 이전 기기 refresh·API 호출 거부
- **토큰 갱신** 시 refresh rotation만 수행 (동일 세션 내 access jti는 blacklist 하지 않음)
- Gateway(`SECURITY_JWT_ENABLED=true`)가 blacklist jti access token을 401로 거부

---

## 토큰 갱신

### Method · Path
`POST /api/v1/auth/refresh`

### Auth
불필요 (Refresh Token HttpOnly Cookie). **Origin 허용 목록 검증** (`auth.origin`, prod 기본 `require-origin=true`).

### Request
Body 없음. `vh_refresh_token` Cookie 필수. 브라우저/BFF는 `Origin` 헤더 필요(prod, `require-origin=true` 시).

### Response (200)
로그인과 동일 (body + Set-Cookie rotation)

### Errors

| status | code | 의미 |
|--------|------|------|
| 401 | INVALID_TOKEN | refresh 무효/만료·JWT 파싱 실패·Redis 키 없음·jti 불일치·동시 refresh 패배 |
| 403 | AUTH_MEMBER_NOT_ACTIVE | member_status ≠ ACTIVE — refresh·active Redis 삭제, access blacklist |
| 403 | AUTH_FORBIDDEN_ORIGIN | Origin 누락/허용 목록 외 |

**Refresh rotation 실패 판별**

| 상황 | code |
|------|------|
| refresh JWT 만료·파싱 실패 | `INVALID_TOKEN` |
| Redis `auth:refresh:{authUuid}` 키 없음 (로그아웃·TTL 만료) | `INVALID_TOKEN` |
| Redis jti 불일치 (다른 기기 sign-in·동시 refresh 패배 등) | `INVALID_TOKEN` |

**Refresh reuse 감지 범위**

- 동시에 같은 refresh token으로 갱신 요청 시 Lua atomic rotate로 **1건만 성공**
- 이미 rotation된 구 refresh token 재사용 시 Redis jti 불일치 → **401 INVALID_TOKEN** (access blacklist 없음)
- token-family theft detection(구 refresh 재사용 시 계정 전체 세션 revoke 등)은 **이번 범위 미포함**

---

## 로그아웃

### Method · Path
`POST /api/v1/auth/logout`

### Auth
Gateway JWT — Access Token HttpOnly Cookie (`vh_access_token`). **Origin 허용 목록 검증** (refresh와 동일).

### Request
Body 없음. `vh_access_token`·`vh_refresh_token` Cookie에서 토큰 읽음. prod에서 `Origin` 헤더 필요(`require-origin=true`).

### Response
`204 No Content` + 만료 Cookie (`Max-Age=0`) 2개

Refresh Redis 삭제 + Access jti blacklist(TTL=잔여 만료) + `auth:access:{authUuid}` 삭제

이미 무효화·만료된 토큰으로 호출해도 **204** (best-effort idempotent).

### Errors

| status | code | 의미 |
|--------|------|------|
| 403 | AUTH_FORBIDDEN_ORIGIN | Origin 누락/허용 목록 외 |

**Origin 설정 (prod)**

- `auth.origin.require-origin=true` (prod 고정), `auth.origin.allowed-origins=${AUTH_ALLOWED_ORIGINS}` — 비어 있으면 기동 실패
- 허용 값: `http(s)://host` 또는 `http(s)://host:port` exact match (정규화 후 scheme·host·effective port 비교)
- prod allowlist에 `localhost`/`127.0.0.1`·`*`·path/query 포함 Origin 불가
- Referer fallback 없음 — `Origin` 헤더만 검증

---

## 회원 탈퇴

### Summary
PASS 본인인증(`purpose=WITHDRAWAL`) confirm SUCCESS 후, 가입 시 연결된 CI와 탈퇴 인증 CI가 일치하면 `member_status`를 `WITHDRAWN`으로 변경하고 세션을 무효화합니다.  
진행 중 거래·미처리 환불 확인은 Auth 범위 밖(후속)입니다.

### Method · Path
`POST /api/v1/auth/withdraw`

### Auth
필요 — Gateway JWT + `X-Member-Uuid`. **Origin 허용 목록 검증** (refresh·logout과 동일).

### 사전 조건 (FE)
1. `POST /api/v1/identity-verifications/confirm` — `purpose=WITHDRAWAL`
2. confirm SUCCESS의 `requestToken`으로 본 API 호출

### Request (Body)

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| requestToken | string | O | WITHDRAWAL confirm SUCCESS |

```json
{
  "requestToken": "identity-verification-withdraw-001"
}
```

### Response
`204 No Content` + 만료 Cookie (`Max-Age=0`) 2개

- `member_status` → `WITHDRAWN` (이미 탈퇴면 상태 변경 생략, 세션 revoke는 수행 — 멱등)
- 탈퇴 본인인증 이력에 `memberUuid` 연결(재사용 방지)
- 활성 access jti blacklist + access/refresh Redis 삭제

### Errors

| status | code | 의미 |
|--------|------|------|
| 401 | AUTH_UNAUTHORIZED | `X-Member-Uuid` 없음·공백 |
| 403 | AUTH_FORBIDDEN_ORIGIN | Origin 누락/허용 목록 외 |
| 403 | AUTH_IDENTITY_MISMATCH | 가입 CI ≠ 탈퇴 인증 CI |
| 403 | AUTH_MEMBER_NOT_ACTIVE | ACTIVE·WITHDRAWN이 아닌 상태 |
| 400 | IDENTITY_VERIFICATION_NOT_READY | 탈퇴용 본인인증 미완료·purpose 불일치 |
| 400 | IDENTITY_VERIFICATION_ALREADY_USED | requestToken이 다른 회원에 연결됨 |
| 404 | AUTH_NOT_FOUND | 계정 없음 |
| 404 | IDENTITY_VERIFICATION_NOT_FOUND | 탈퇴·가입 본인인증 이력 없음 |

---

## 중복 확인

### 아이디
`GET /api/v1/auth/check/login-id?loginId={value}`

### 이메일
`GET /api/v1/auth/check/email?email={value}`

### Response (200)

```json
{ "available": true }
```

---

## 본인인증 확인

### Summary
PortOne 본인인증 완료 후 서버에서 인증 결과를 확인하고 이력에 `ci_hash`와 상태만 저장합니다. prefill은 응답으로만 제공합니다.

### Method · Path
`POST /api/v1/identity-verifications/confirm`

### Auth
불필요

### Request (Body)

| 필드 | 타입 | 필수 |
|------|------|------|
| identityVerificationId | string | O |
| purpose | enum | O |

### Response (200)

| 필드 | 타입 |
|------|------|
| requestToken | string |
| purpose | enum |
| status | enum |
| memberName | string | prefill (confirm SUCCESS 시) |
| phoneNumber | string | prefill (confirm SUCCESS 시) |
| gender | enum | prefill (confirm SUCCESS 시) |

`birthdayDate`는 confirm 응답에 포함하지 않음.

**Cache-Control:** `no-store`

### Errors

| status | code | 의미 |
|--------|------|------|
| 400 | IDENTITY_VERIFICATION_FAILED | CI/고객정보 불완전 |
| 404 | IDENTITY_VERIFICATION_NOT_FOUND | PortOne 조회 실패 |
| 502 | PORTONE_API_ERROR | PortOne 통신 실패 |

---

## 본인인증 상태 조회

### Summary
저장된 본인인증 상태를 DB에서 조회합니다. PortOne 재조회·PII 미포함.

### Method · Path
`POST /api/v1/identity-verifications/status`

`requestToken`은 URL path·query·cookie에 넣지 않고 **POST body**로만 전달합니다 (프록시·접근 로그 노출 방지).

### Auth
불필요

### Request (Body)

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| requestToken | string | O | max 255 |

```json
{
  "requestToken": "identity-verification-001"
}
```

### Response (200)

| 필드 | 타입 |
|------|------|
| purpose | enum |
| status | enum |

```json
{
  "purpose": "SIGN_UP",
  "status": "SUCCESS"
}
```

**Cache-Control:** `no-store`

PII(memberName·phone·birth·gender·requestToken) 미포함. PortOne 재조회 없음.

### Errors

| status | code | 의미 |
|--------|------|------|
| 400 | VALIDATION_ERROR | requestToken 누락·형식 오류 |
| 404 | IDENTITY_VERIFICATION_NOT_FOUND | DB 이력 없음 |

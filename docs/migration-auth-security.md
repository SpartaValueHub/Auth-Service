# Auth Security Architecture Migration

Hibernate `ddl-auto: update` 사용 중이므로 dev 환경은 재기동 시 스키마가 자동 반영됩니다.  
**운영(prod)은 아래 SQL을 검토·적용한 뒤 배포하세요.**

## 1. auth 테이블

### UNIQUE 제약 (JPA @Table uniqueConstraints)

| 제약명 | 컬럼 |
|--------|------|
| `uk_auth_auth_uuid` | auth_uuid |
| `uk_auth_login_id` | login_id |
| `uk_auth_email` | email |
| `uk_auth_phone_number` | phone_number |

> `@Column(unique=true)` 대신 named `@UniqueConstraint`만 사용한다.  
> CI 중복 검사는 `identity_verifications.ci_hash` 이력 pre-check(`existsSignUpLinkedByCiHash`)로 수행하며, ci_hash UNIQUE 없이 동시 가입 race를 완전히 막지는 못한다.

```sql
-- member_status 추가 (기존 행은 ACTIVE)
ALTER TABLE auth
  ADD COLUMN member_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- CI·로그인 잠금 컬럼 제거 (legacy)
ALTER TABLE auth DROP COLUMN identity_key;
ALTER TABLE auth DROP COLUMN login_fail_count;
ALTER TABLE auth DROP COLUMN locked_until;

-- legacy identity_key_hash 제거 (CI는 identity_verifications에만 저장)
ALTER TABLE auth DROP INDEX uk_identity_key_hash;
ALTER TABLE auth DROP COLUMN identity_key_hash;
```

> **참고:** CI 평문·암호문은 저장하지 않습니다. 본인인증 이력은 `identity_verifications.ci_hash`(HMAC-SHA256 hex, 64자)만 보관하고, 가입 중복 검사는 `identity_verifications`에서 `ci_hash + purpose=SIGN_UP + member_uuid IS NOT NULL` 조건으로 수행합니다.

## 2. identity_verifications 테이블

최종 컬럼:

| 컬럼 | 설명 |
|------|------|
| identity_verification_id | PK |
| verification_uuid | unique |
| member_uuid | 가입 완료 후 연결된 회원 UUID (nullable) |
| purpose | SIGN_UP 등 |
| request_token | unique |
| verification_method | PASS 등 |
| ci_hash | HMAC-SHA256 hex (64), **UNIQUE 없음** |
| verification_status | REQUESTED / SUCCESS / FAILED |
| verified_at | |
| created_at | |

```sql
ALTER TABLE identity_verifications
  ADD COLUMN verification_uuid VARCHAR(36) NOT NULL DEFAULT (UUID()),
  ADD COLUMN verification_method VARCHAR(30) NULL,
  ADD COLUMN ci_hash VARCHAR(64) NULL,
  ADD COLUMN verified_at TIMESTAMP(6) NULL,
  ADD COLUMN verification_status VARCHAR(20) NULL;

-- 기존 status → verification_status 이전
UPDATE identity_verifications
SET verification_status = status
WHERE verification_status IS NULL;

ALTER TABLE identity_verifications
  MODIFY verification_status VARCHAR(20) NOT NULL;

-- verification_uuid unique (기존 행 UUID 채운 뒤)
ALTER TABLE identity_verifications
  ADD UNIQUE INDEX uk_verification_uuid (verification_uuid);

-- ci_hash UNIQUE 제거 (동일인 재인증 이력 허용)
ALTER TABLE identity_verifications DROP INDEX uk_ci_hash;

-- legacy ci_value 제거 (암호화 CI 컬럼 — 더 이상 사용하지 않음)
ALTER TABLE identity_verifications DROP COLUMN ci_value;

-- auth_uuid → member_uuid 되돌리기 (인증 완료 후 연결된 회원 식별자)
ALTER TABLE identity_verifications
  CHANGE COLUMN auth_uuid member_uuid VARCHAR(36) NULL;

-- ci_hash 길이 64로 조정 (HMAC-SHA256 hex)
ALTER TABLE identity_verifications
  MODIFY ci_hash VARCHAR(64) NULL;

-- legacy status 컬럼 제거 (이전 검증 후)
-- ALTER TABLE identity_verifications DROP COLUMN status;
```

> **UNIQUE 유지:** `request_token`, `verification_uuid`  
> **UNIQUE 없음:** `ci_hash` (이력 테이블 — signup·find-id·password-reset 등 재인증 허용)

## 3. CI 처리 흐름

```
PortOne 원본 CI → Backend HMAC-SHA256(hex) → ci_hash 저장 → 원본 CI 폐기
```

- `IdentityKeyHashPort` / `HmacSha256IdentityKeyHashAdapter` 사용 (AES-GCM 미사용)
- `auth` 테이블에는 CI 관련 컬럼 없음

## 4. 환경 변수 (auth-service)

| 변수 | 용도 |
|------|------|
| `CI_HASH_KEY` | Base64 HMAC 키 (≥16 bytes) — CI 해시 |
| `RECAPTCHA_SECRET_KEY` | Google reCAPTCHA secret |
| `CAPTCHA_ENABLED` | `false` 시 captcha 검증 스킵 (local/dev) |

키 생성 예 (PowerShell):

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 })))
```

## 5. ValueHub-FO

| 변수 | 용도 |
|------|------|
| `NEXT_PUBLIC_RECAPTCHA_SITE_KEY` | reCAPTCHA v2 site key |

## 6. Redis (변경 없음 — docker-compose redis:6379)

로그인 실패/잠금 키 (TTL 10분):

- `login:fail:{loginId}`
- `login:lock:{loginId}`

기존 refresh/blacklist 키(`auth:refresh:*`, `auth:blacklist:access:*`, `auth:access:*`)와 공존.

## 7. IP sign-in rate limit Redis 키

| Key | 값 | TTL |
|-----|-----|-----|
| `login:rate:count:{sha256(ip)}` | 윈도우 내 sign-in POST 수 | `window-seconds` |
| `login:rate:block:{sha256(ip)}` | 차단 플래그 | `block-seconds` |

loginId fail count(`login:fail:*`, `login:lock:*`)와 **독립**.

## 8. 신뢰 프록시·rate limit (Gateway + auth-service)

**아키텍처 결정:** IP rate limit은 **auth-service Redis Lua**에서 집행. Gateway는 클라이언트 IP 헤더 spoofing 방지만 담당.

### 확인된 구성 (코드·설정 기준)

| 항목 | 상태 |
|------|------|
| auth-service 직접 URL | FE/인터넷 공개 아님 — Gateway `lb://auth-service` (Eureka), `server.port=0` |
| Gateway ingress | `StripUntrustedForwardedHeadersFilter` — 클라이언트 `Forwarded` / `X-Forwarded-*` 제거 |
| auth-service | `forward-headers-strategy=native`, Tomcat `internal-proxies` (`AUTH_TRUSTED_PROXY_PATTERNS`, 기본 RFC1918) |
| 직접 접속 spoofing | 비신뢰(remote) 연결의 X-Forwarded-For **무시** → `getRemoteAddr()` = 실제 TCP peer |

### 운영 필수 (미적용 시 “보안 완료” 아님)

1. **Network:** auth-service 포트를 Gateway(및 헬스체크) 외부에서 차단.
2. **Gateway:** `StripUntrustedForwardedHeadersFilter` 배포 유지 (ingress spoofing 방지).
3. **auth-service:** `AUTH_TRUSTED_PROXY_PATTERNS`에 **실제 Gateway Pod/VM CIDR** 설정 (Docker/K8s CNI 대역).
4. **통합 테스트:** `RedisLoginRateLimitAdapterIntegrationTest` — `docker compose up -d` (localhost:6379) 필요. Testcontainers 미사용.

### Spoofing 시나리오

| 경로 | X-Forwarded-For spoof | 결과 |
|------|----------------------|------|
| Client → Gateway → auth | Gateway가 ingress 헤더 제거·재설정 | 실제 client IP로 rate limit |
| Client → auth 직접 (포트 노출) | Tomcat `internal-proxies` 외 peer — 헤더 무시 | 실제 client IP (spoofing 불가) |
| Client → auth 직접 + `framework` (legacy) | 임의 X-Forwarded-For 신뢰 | **우회 가능** — `native` 전환 필수 |

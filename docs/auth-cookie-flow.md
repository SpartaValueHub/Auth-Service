# Auth Cookie Flow (auth-service)

Gateway·FO와 동일 cookie name: `vh_access_token`, `vh_refresh_token`.

설정: `application.yml` → `auth.cookie.*`

## 단일 세션 (이중 로그인 방지)

1. Redis `auth:access:{authUuid}` — 현재 활성 access jti (TTL=JWT `exp` 잔여 초, CEIL)
2. **sign-in** — 기존 access jti blacklist (Redis 값에 `exp` 없음 → `jwt.access-token-minutes` 전체 수명 TTL) → 새 access·refresh 발급·저장
3. **refresh** — refresh rotation + 활성 access jti 갱신 (이전 access blacklist 없음)
4. Gateway blacklist filter — 무효화된 access jti 즉시 401

## Refresh Rotation

1. refresh cookie parse → JWT에서 `authUuid`·`jti` 추출
2. Redis Lua **atomic rotate**: `GET auth:refresh:{authUuid}` → `jti` 일치 시 `SET newJti EX ttl` (GET·SET 분리 없음)
3. rotate 성공 후 새 access+refresh JWT 발급·응답, 활성 access jti 갱신

### Reuse 감지 범위

| 시나리오 | 동작 |
|----------|------|
| **동시 refresh** (같은 refresh token) | Lua compare-and-set — **1건만 성공**, 나머지 `INVALID_TOKEN` |
| **이미 rotation된 구 refresh token 재사용** | Redis jti 불일치 → `INVALID_TOKEN` |
| **token-family theft detection** | **미구현** — refresh reuse 시 전체 세션/계정 revoke 없음 |

> token-family 탈취 감지(구 refresh 재사용 시 모든 refresh 무효화 등)는 이번 범위에 포함하지 않는다.

## Logout

1. refresh cookie → Redis delete
2. access cookie jti → blacklist (TTL=잔여 `exp` CEIL, 만료 시 Redis 미기록)
3. `auth:access:{authUuid}` delete
4. Response `Set-Cookie` Max-Age=0

## CSRF / Cookie

- auth-service API는 Gateway 경유. 브라우저 직접 호출 시 CORS는 Gateway 담당.
- Cookie `SameSite=Lax` 기본 — cross-origin fetch(3000→8000)는 prod에서 domain·SameSite 조정 필요.

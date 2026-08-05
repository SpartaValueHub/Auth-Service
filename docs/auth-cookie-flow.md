# Auth Cookie Flow (auth-service)

Gateway·FO와 동일 cookie name: `vh_access_token`, `vh_refresh_token`.

설정: `application.yml` → `auth.cookie.*`

## Refresh Rotation

1. refresh cookie parse → Redis `matches(authUuid, jti)`
2. `delete(authUuid)` — 기존 refresh 무효
3. 새 access+refresh 발급 + Redis save

## Logout

1. refresh cookie → Redis delete
2. access cookie jti → blacklist (TTL=잔여 exp)
3. Response `Set-Cookie` Max-Age=0

## CSRF / Cookie

- auth-service API는 Gateway 경유. 브라우저 직접 호출 시 CORS는 Gateway 담당.
- Cookie `SameSite=Lax` 기본 — cross-origin fetch(3000→8000)는 prod에서 domain·SameSite 조정 필요.

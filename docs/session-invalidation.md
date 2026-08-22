# Session Invalidation (세션 무효화)

비밀번호 변경·회원 상태 변경 시 기존 JWT/Redis 세션을 어떻게 끊을지에 대한 **현재 구현 상태**와 **운영·향후 구현 가이드**.

관련: [auth-cookie-flow.md](./auth-cookie-flow.md), [auth-api.md](./auth-api.md)

## 1. 비밀번호 변경 — 현재 없음

코드베이스 검색 기준, 비밀번호 변경(또는 재설정 완료) UseCase·API·Domain 행위는 **미구현**이다.

| 검색 대상 | 결과 |
|-----------|------|
| `AuthUseCase` | `signUp` / `signIn` / `refresh` / `logout` / 중복 확인만 존재 — password change 없음 |
| `AuthController` | `/auth/sign-up`, `/auth/sign-in`, `/auth/refresh`, `/auth/logout`, `/auth/check/*` — password 관련 엔드포인트 없음 |
| `AuthDomain` | `passwordChangedAt`은 `createSignUp` 시점에만 설정. `changePassword` 등 상태 전이 메서드 없음 |
| `changePassword` / `updatePassword` / `ChangePassword` | `src` 전역 매칭 없음 |

> `VerificationPurpose.RESET_PASSWORD`, `password_changed_at` 컬럼은 스키마·본인인증 목적 enum 수준만 존재. **비밀번호 변경 완료 후 세션 revoke 경로는 없음.**

**통합 검증:** password change → session invalidation 플로우는 구현·E2E 검증 **불가**.

## 2. 회원 상태 변경 — 탈퇴(WITHDRAWN) 경로 추가

`MemberStatus`(ACTIVE / SUSPENDED / WITHDRAWN / DORMANT)는 Domain·Entity·DB 컬럼(`member_status`)으로 저장·조회한다.

| 검색 대상 | 결과 |
|-----------|------|
| `WithdrawMemberUseCase` / `POST /auth/withdraw` | PASS(`WITHDRAWAL`) confirm 후 CI 매칭 → anonymize + claim release + `WITHDRAWN` + 세션 revoke |
| `AuthDomain` | `withdraw()` — ACTIVE/WITHDRAWN → WITHDRAWN + 식별자 anonymize (멱등). `isWithdrawn()` |
| 사용처 | `signIn`·`refresh`에서 `auth.isActive()` 검사 후 `MemberNotActiveException` |

가입 시 기본값 `ACTIVE`(`AuthDomain.createSignUp`).

**통합 검증:** 탈퇴 → session invalidation은 `SessionInvalidationPort.revokeAllSessions` 경유.  
`AuthService.refresh` inactive 경로와 **동일 Port**를 사용한다 (`RedisSessionInvalidationAdapter`).

## 3. inactive 계정 refresh 시 `SessionInvalidationPort.revokeAllSessions()` (`AuthService`)

유일하게 **세션을 적극 revoke**하는 Application 경로:

```
refresh() inactive / withdraw()
  → sessionInvalidationPort.revokeAllSessions(authUuid)
```

`RedisSessionInvalidationAdapter.revokeAllSessions(String authUuid)` 동작:

1. Redis `auth:access:{authUuid}`에서 활성 access **jti** 조회
2. jti가 있으면 `auth:blacklist:access:{jti}` 등록 (TTL = `jwt.access-token-minutes` 전체 수명 — Redis에 `exp` 없음)
3. `auth:access:{authUuid}` 삭제
4. `auth:refresh:{authUuid}` 삭제

> **sign-in**에서 inactive 계정은 `MemberNotActiveException`만 던지고 **revoke 호출 없음** (기존 세션이 Redis에 남을 수 있음). inactive 세션 정리는 **refresh 시도** 또는 **탈퇴** 시 발생.

Gateway·auth-api 명세: refresh 403 `AUTH_MEMBER_NOT_ACTIVE` 시 위 Redis 정리·access blacklist 수행 ([auth-api.md](./auth-api.md) refresh Errors 참고).

## 4. auth DB 직접 UPDATE 시 access token 유효 기간

타 서비스·운영자가 `auth` 테이블을 **SQL 직접 UPDATE**(`member_status`, `password_hash`, `password_changed_at` 등)하면:

| 토큰 | Gateway·클라이언트 관점 |
|------|-------------------------|
| **Access JWT** | Gateway는 JWT 서명·만료·**blacklist(jti)** 만 검사. DB `member_status`·비밀번호 변경 시각을 **조회하지 않음**. blacklist에 없으면 **JWT `exp`까지 유효** |
| **Refresh** | 다음 `/auth/refresh` 시 DB에서 `member_status` 재조회 — inactive면 revoke + 403. 비밀번호만 바뀐 경우 refresh는 **현재 구현상 계속 성공** (password change UseCase·invalidation 없음) |

즉, DB만 바꾸고 Redis blacklist·refresh·active 키를 건드리지 않으면 **기존 access token은 만료 또는 blacklist 등록 전까지 API 호출 가능**.

## 5. Gateway — blacklist만, `member_status` 미조회

`AccessTokenBlacklistWebFilter` (gateway):

- 인증된 요청의 JWT **jti**로 Redis `auth:blacklist:access:{jti}` 존재 여부만 확인
- hit 시 **401**
- **`member_status`·`password_changed_at` DB/캐시 조회 없음**

세션 무효화의 Edge 집행은 **blacklist 등록에 의존**한다.

## 6. `SessionInvalidationPort` — 구현됨

계정 단위 전체 세션 revoke는 `SessionInvalidationPort` / `RedisSessionInvalidationAdapter`로 공통화했다.

| 호출처 | 용도 |
|--------|------|
| `AuthService.refresh` (inactive) | inactive 계정 refresh 거부 시 |
| `WithdrawMemberService.withdraw` | 탈퇴 후 |

`logout()`·sign-in 시 이전 access blacklist는 토큰/jti 단위라 Port와 별도로 유지한다.

비밀번호 변경 등 추가 UseCase도 **동일 Port**를 호출한다 (아래 §7).

## 7. 향후 구현 시 필수: 공통 session invalidation

다음 UseCase(또는 동등 Application 경로)를 추가할 때는 **DB 갱신 성공 후 반드시** 세션 무효화를 호출해야 한다.

| UseCase (예정) | 무효화 필요 이유 |
|----------------|------------------|
| Password change / reset complete | 이전 비밀번호로 발급된 refresh·access 차단 |
| Member status change (SUSPENDED, DORMANT 등) | inactive 계정의 기존 세션 즉시 종료 |

권장 순서 (개념):

1. `@Transactional` 내 auth Domain/DB 갱신
2. **`sessionInvalidationPort.revokeAllSessions(authUuid)`**
3. (선택) Gateway blacklist TTL은 기존과 동일 정책 (`auth-cookie-flow.md` sign-in blacklist 참고)

`logout()`의 토큰 단위 무효화와 키 규칙은 유지하되, **계정 단위 revoke는 Port만 사용**해 drift를 막는다.

## 8. 타 서비스 auth 테이블 직접 UPDATE 금지

- `auth`는 auth-service 소유 데이터. **member-service 등 타 서비스에서 `auth` 테이블 직접 UPDATE 금지**
- 상태·비밀번호 변경은 auth-service API(향후 UseCase)를 통해서만 수행하고, 그 경로에서 §7 invalidation을 보장
- 불가피한 운영 SQL 후에는 해당 `auth_uuid`에 대해 refresh 키 삭제·access jti blacklist를 **수동 보정**하거나, 사용자 재로그인·refresh 시도까지 **access JWT 만료 윈도우**를 감수

## 9. DB–Redis 비원자성 한계

`RedisSessionInvalidationAdapter` 주석과 동일:

> Redis 세션 정리는 DB `@Transactional`과 **원자적이지 않음** — 부분 실패 시 재시도·모니터링으로 보완.

| 구간 | 리스크 |
|------|--------|
| DB commit ↔ Redis delete/blacklist | DB는 반영됐는데 Redis revoke 실패 → 구 refresh/access 잔존 가능 |
| Redis blacklist TTL | 활성 access Redis 값에 `expiresAt` 없음 — 설정된 access 전체 수명 TTL 사용(과잉 보관 허용) |

대응: invalidation 실패 로그·알림, idempotent 재시도, 운영 runbook. 분산 트랜잭션(2PC)은 현재 범위에 없음.

## 10. Redis 키 요약

| Key | 역할 |
|-----|------|
| `auth:access:{authUuid}` | 현재 활성 access jti |
| `auth:refresh:{authUuid}` | 현재 유효 refresh jti |
| `auth:blacklist:access:{jti}` | 무효화된 access — Gateway 401 |

---

**요약:** 계정 단위 revoke는 `SessionInvalidationPort`로 공통화됨 (inactive refresh·탈퇴). password change 등은 미구현이지만 동일 Port 호출이 **필수**. 실제 revoke는 **logout(토큰 단위)**, **sign-in(이전 access blacklist)**, **inactive refresh·탈퇴(Port)** 경로. auth DB 직접 UPDATE는 세션 일관성을 깨므로 금지.

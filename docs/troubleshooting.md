# Auth 연동 트러블슈팅 (로컬 · Gateway · FE)

로컬에서 **ValueHub-FO → Gateway → auth-service** 회원가입·본인인증·로그인 E2E 시 자주 발생한 증상과 조치입니다.

## 로컬 기동 순서

| 순서 | 서비스 | 포트(예) | 비고 |
|------|--------|----------|------|
| 1 | Redis | 6379 | refresh·blacklist |
| 2 | Eureka (Discovery) | 8761 | |
| 3 | auth-service | (Eureka 등록) | MySQL·PortOne·JWT 키 필요 |
| 4 | **Gateway** | 8000 | **코드 변경 후 재기동 필수** |
| 5 | ValueHub-FO | 3000 | `pnpm dev` |

auth-service MySQL (G31 고정): `192.168.10.45:3307/auth_db`

JWT 키 미생성 시:

```bash
cd auth-service
./gradlew generateJwtKeys
```

---

## Gateway POST 403 Forbidden

### 증상

- FE Server Action / `curl`에서 **GET**은 200인데 **POST**만 403
- 예: `check/login-id` GET 200 → `sign-up` POST 403
- 예: PortOne SDK 성공 후 `confirmIdentityVerificationAction` → `API 오류 (403 Forbidden)`
- 응답 body 비어 있거나 Spring Security JSON

### 원인

`SECURITY_JWT_ENABLED=true` 일 때 Gateway `JwtSecurityConfig`가 public API를 `permitAll` 하지 못하면, Bearer 없는 POST가 JWT 검증 단계에서 차단됩니다.

과거 `PathPattern`(`/*/api/v1/auth/sign-up/**` 등)만으로는 POST 경로 매칭이 누락되는 경우가 있었습니다.

### 조치 (코드)

Gateway `AuthPublicPathMatcher` + `AuthPublicServerWebExchangeMatcher`로 URI path regex 판별:

- `/auth-service/api/v1/auth/sign-up|sign-in|refresh|check/*`
- `/auth-service/api/v1/identity-verifications/**` (confirm 포함)

관련 파일:

- `gateway/.../AuthPublicPathMatcher.java`
- `gateway/.../JwtSecurityConfig.java`

### 조치 (운영)

1. **Gateway 재기동** (컴파일만으로는 반영 안 됨)
2. 재기동 후 확인:

```bash
curl.exe -s -o NUL -w "%{http_code}" -X POST "http://localhost:8000/auth-service/api/v1/auth/sign-up" ^
  -H "Content-Type: application/json" -d "{}"
```

- **403** → Gateway Security / JWT public path 미반영
- **400** 이하(4xx) → Gateway 통과, auth-service 검증 응답 (정상)

본인인증 confirm:

```bash
curl.exe -s -o NUL -w "%{http_code}" -X POST "http://localhost:8000/auth-service/api/v1/identity-verifications/confirm" ^
  -H "Content-Type: application/json" -d "{\"identityVerificationId\":\"test\",\"purpose\":\"SIGN_UP\"}"
```

### 임시 우회 (로컬 초기)

Gateway `.env` 또는 환경 변수:

```env
SECURITY_JWT_ENABLED=false
```

JWT Edge 검증 없이 라우팅만 수행합니다. JWT on E2E 전 `false`로 FE·auth 연동 확인 후 `true` + public key 설정을 권장합니다.

---

## 본인인증 confirm 실패

### 403

→ 위 **Gateway POST 403** 절차 (Gateway 재기동·public path).

### 400 / IDENTITY_VERIFICATION_FAILED

- PortOne `VERIFIED` 상태·CI·고객정보(이름·전화·생년월일·**gender**) 불완전
- auth-service `PORTONE_API_SECRET` 확인
- PortOne 대시보드에서 본인인증 완료 여부 확인

### 404 IDENTITY_VERIFICATION_NOT_FOUND

- `identityVerificationId` 불일치 또는 PortOne 조회 실패

---

## 회원가입 sign-up 실패

| HTTP | code / 메시지 | 조치 |
|------|----------------|------|
| 403 | (Gateway) | Gateway public path·재기동 |
| 400 | validation | FE zod·auth Domain 규칙 확인 (비밀번호 대소문자·특수문자 등) |
| 409 | AUTH_DUPLICATE_* | loginId·email·phone·CI 중복 |
| 400 | IDENTITY_VERIFICATION_* | confirm 미완료·purpose 불일치·토큰 재사용 |

sign-up body: `requestToken`, `logInId`, `password`, `email` 만 전송 (실명·전화·gender는 PortOne→서버 조회).

---

## auth-service 500 Internal Server Error

- MySQL 연결 (`192.168.10.45:3307`)·스키마·`gender` 컬럼 등 DDL (`ddl-auto: update`) 확인
- auth-service 로그에서 stack trace 확인
- Eureka 등록·Gateway `lb://auth-service` 라우팅 확인

---

## ValueHub-FO (프론트)

상세: [ValueHub-FO/docs/troubleshooting.md](../../ValueHub-FO/docs/troubleshooting.md)

요약:

| 증상 | 조치 |
|------|------|
| `useActionState` transition 경고 | `<form action={formAction}>` 사용, 수동 `formAction()` 호출 금지 |
| `pnpm install` EPERM | node 종료 → `node_modules`·`package-lock.json` 삭제 → **pnpm만** 재설치 |
| Zod `.email()` deprecated | Zod 4: `z.email({ error: "..." })` |
| 성별 기본 선택 | 초기값 없음, PortOne confirm 후에만 highlight |

FE `.env.local`:

```env
API_URL=http://localhost:8000/auth-service
NEXT_PUBLIC_PORTONE_STORE_ID=...
NEXT_PUBLIC_PORTONE_CHANNEL_KEY=...
```

---

## Gateway (Edge)

상세: [gateway/docs/troubleshooting.md](../../gateway/docs/troubleshooting.md)

---

## 체크리스트 (회원가입 E2E)

- [ ] Redis · Eureka · auth-service · **Gateway(재기동)** · `pnpm dev` 기동
- [ ] `curl` sign-up POST → 403 아님
- [ ] `curl` identity confirm POST → 403 아님
- [ ] PortOne Store ID / Channel Key (FE) + API Secret (auth)
- [ ] 본인인증 → confirm → sign-up → sign-in

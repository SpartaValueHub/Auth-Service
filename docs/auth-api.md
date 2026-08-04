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
본인인증 SUCCESS 이력(`requestToken`) 확인 후 PortOne에서 CI·인증정보를 서버가 조회해 회원을 생성합니다.

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
| 409 | AUTH_DUPLICATE_* | 중복 (loginId/email/phone/CI) |

---

## 로그인

### Method · Path
`POST /api/v1/auth/sign-in`

### Auth
불필요

### Request (Body)

| 필드 | 타입 | 필수 |
|------|------|------|
| logInId | string | O |
| password | string | O |

### Response (200)

| 필드 | 타입 |
|------|------|
| accessToken | string | RS256 JWT, 15분, claims: sub(authUuid), tokenType=access |
| refreshToken | string | RS256 JWT, 14일 |
| authUuid | string |
| logInId | string |
| memberName | string |
| email | string |

### Errors

| status | code | 의미 |
|--------|------|------|
| 401 | AUTH_UNAUTHORIZED | 아이디/비밀번호 오류 |
| 403 | AUTH_ACCOUNT_LOCKED | 5회 실패 10분 잠금 |

---

## 토큰 갱신

### Method · Path
`POST /api/v1/auth/refresh`

### Auth
불필요 (Refresh Token body)

### Request (Body)

| 필드 | 타입 | 필수 |
|------|------|------|
| refreshToken | string | O |

### Response (200)
로그인과 동일

### Errors

| status | code | 의미 |
|--------|------|------|
| 401 | INVALID_TOKEN | refresh 무효/만료/Redis 불일치 |

---

## 로그아웃

### Method · Path
`POST /api/v1/auth/logout`

### Auth
Gateway JWT (Access Token Bearer)

### Request (Body)

| 필드 | 타입 | 필수 |
|------|------|------|
| accessToken | string | O |
| refreshToken | string | O |

### Response
`204 No Content`

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
PortOne 본인인증 완료 후 서버에서 인증 결과를 확인하고 이력에 상태만 저장합니다. prefill은 응답으로만 제공합니다.

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
| memberName | string |
| phoneNumber | string |
| birthdayDate | string |

### Errors

| status | code | 의미 |
|--------|------|------|
| 400 | IDENTITY_VERIFICATION_FAILED | CI/고객정보 불완전 |
| 404 | IDENTITY_VERIFICATION_NOT_FOUND | PortOne 조회 실패 |
| 502 | PORTONE_API_ERROR | PortOne 통신 실패 |

---

## 본인인증 상태 조회

### Method · Path
`GET /api/v1/identity-verifications/{requestToken}`

### Auth
불필요

### Response (200)
본인인증 확인과 동일 형식

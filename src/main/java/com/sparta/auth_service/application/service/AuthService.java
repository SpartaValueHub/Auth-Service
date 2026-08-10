package com.sparta.auth_service.application.service;

import com.sparta.auth_service.adaptor.in.web.config.LoginAttemptProperties;
import com.sparta.auth_service.adaptor.out.security.JwtProperties;
import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.CaptchaInvalidException;
import com.sparta.auth_service.application.exception.CaptchaRequiredException;
import com.sparta.auth_service.application.exception.IdentityVerificationAlreadyUsedException;
import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.exception.InvalidTokenException;
import com.sparta.auth_service.application.exception.LoginRateLimitedException;
import com.sparta.auth_service.application.exception.MemberNotActiveException;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import com.sparta.auth_service.application.port.in.AuthUseCase;
import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResumeRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResumeResultDto;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.ActiveAccessTokenPort;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.CaptchaVerificationPort;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.LoginAttemptPort;
import com.sparta.auth_service.application.port.out.LoginRateLimitPort;
import com.sparta.auth_service.application.port.out.dto.LoginRateLimitResultDto;
import com.sparta.auth_service.application.port.out.PasswordEncoderPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.SignupCompletionTokenPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;
import com.sparta.auth_service.application.port.out.dto.RefreshTokenRotationResult;
import com.sparta.auth_service.application.support.TokenTtlCalculator;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * 인증 계정(로그인·JWT) 전용 Application Service.
 * CI는 identity_verifications.ci_hash(HMAC)로만 저장, 가입 중복은 ci_hash+member_uuid 이력으로 검사, 로그인 실패·잠금은 Redis.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

    private static final String DEFAULT_ROLE = "USER";
    private static final String GENERIC_SIGN_IN_FAILURE = "아이디 또는 비밀번호가 올바르지 않습니다.";

    private final AuthRepositoryPort authRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;
    private final RefreshTokenPort refreshTokenPort;
    private final ActiveAccessTokenPort activeAccessTokenPort;
    private final AccessTokenBlacklistPort accessTokenBlacklistPort;
    private final IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    private final FetchIdentityVerificationPort fetchIdentityVerificationPort;
    private final LoginAttemptPort loginAttemptPort;
    private final LoginRateLimitPort loginRateLimitPort;
    private final CaptchaVerificationPort captchaVerificationPort;
    private final IdentityKeyHashPort identityKeyHashPort;
    private final SignupCompletionTokenPort signupCompletionTokenPort;
    private final SignupPersistenceService signupPersistenceService;
    private final JwtProperties jwtProperties;
    private final LoginAttemptProperties loginAttemptProperties;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthSignUpResultDto signUp(AuthSignUpRequestDto requestDto) {
        if (requestDto.getRequestToken() == null || requestDto.getRequestToken().isBlank()) {
            throw new IllegalArgumentException("requestToken은 필수입니다.");
        }

        String requestToken = requestDto.getRequestToken().trim();
        IdentityVerificationDomain verification = identityVerificationRepositoryPort.findByRequestToken(requestToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));

        if (verification.getPurpose() != VerificationPurpose.SIGN_UP) {
            throw new IllegalArgumentException("SIGN_UP 본인인증이 필요합니다.");
        }
        if (!verification.isAvailableForSignUp()) {
            if (verification.isSuccessful()) {
                throw new IdentityVerificationAlreadyUsedException("이미 사용된 본인인증 요청입니다.");
            }
            throw new IdentityVerificationNotReadyException("본인인증이 완료되지 않았습니다.");
        }

        ExternalIdentityVerificationDto external = fetchIdentityVerificationPort.fetchByRequestToken(requestToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));
        validateVerifiedCustomer(external);
        validateCiMatchesStoredVerification(external, verification);

        String ciHash = verification.getCiHash();
        AuthDomain.validatePlainPassword(requestDto.getPassword());
        String passwordHash = passwordEncoderPort.encode(requestDto.getPassword());

        AuthDomain authDomain = AuthDomain.createSignUp(
                requestDto.getLoginId(),
                passwordHash,
                requestDto.getEmail(),
                external.getMemberName(),
                external.getBirthdayDate(),
                external.getPhoneNumber(),
                external.getGender()
        );

        PreparedSignupCompletionToken preparedToken = prepareSignupCompletionToken(authDomain.getAuthUuid());
        AuthDomain saved = signupPersistenceService.persist(
                requestToken,
                ciHash,
                authDomain,
                preparedToken.tokenId(),
                preparedToken.ttlSeconds()
        );

        return AuthSignUpResultDto.builder()
                .signupCompletionToken(preparedToken.token())
                .authUuid(saved.getAuthUuid())
                .loginId(saved.getLoginId())
                .email(saved.getEmail())
                .memberName(saved.getMemberName())
                .birthdayDate(saved.getBirthdayDate())
                .build();
    }

    private String issueSignupCompletionToken(String authUuid) {
        PreparedSignupCompletionToken prepared = prepareSignupCompletionToken(authUuid);
        signupCompletionTokenPort.save(authUuid, prepared.tokenId(), prepared.ttlSeconds());
        return prepared.token();
    }

    private PreparedSignupCompletionToken prepareSignupCompletionToken(String authUuid) {
        String token = tokenProviderPort.createSignupCompletionToken(authUuid);
        ParsedTokenDto parsed = tokenProviderPort.parseSignupCompletionToken(token);
        long ttlSeconds = remainingTtlSeconds(parsed.getExpiresAt());
        return new PreparedSignupCompletionToken(token, parsed.getTokenId(), ttlSeconds);
    }

    private record PreparedSignupCompletionToken(String token, String tokenId, long ttlSeconds) {
    }

    @Override
    @Transactional
    public AuthSignUpResumeResultDto resumeSignUp(AuthSignUpResumeRequestDto requestDto) {
        enforceLoginRateLimit(requestDto.getClientIp());

        String loginId = requestDto.getLoginId() == null ? "" : requestDto.getLoginId().trim();
        String password = requestDto.getPassword();
        if (loginId.isBlank() || password == null || password.isBlank()) {
            throw new UnauthorizedException(GENERIC_SIGN_IN_FAILURE);
        }
        if (loginAttemptPort.isLocked(loginId)) {
            throw accountLockedWhileRetrying(loginId);
        }
        if (loginAttemptPort.getFailCount(loginId) >= loginAttemptProperties.getCaptchaThreshold()) {
            requireValidCaptcha(requestDto.getCaptchaToken());
        }

        AuthDomain auth = authRepositoryPort.findByLoginId(loginId)
                .orElseGet(() -> {
                    throwLoginFailure(loginId);
                    throw new IllegalStateException("unreachable");
                });
        if (!auth.isActive()) {
            throw new MemberNotActiveException("현재 사용할 수 없는 계정입니다.");
        }
        if (!passwordEncoderPort.matches(password, auth.getPasswordHash())) {
            throwLoginFailure(loginId);
        }

        loginAttemptPort.reset(loginId);
        return AuthSignUpResumeResultDto.builder()
                .authUuid(auth.getAuthUuid())
                .signupCompletionToken(issueSignupCompletionToken(auth.getAuthUuid()))
                .build();
    }

    @Override
    @Transactional
    public AuthSignInResultDto signIn(AuthSignInRequestDto requestDto) {
        enforceLoginRateLimit(requestDto.getClientIp());

        String loginId = requestDto.getLoginId() == null ? "" : requestDto.getLoginId().trim();
        String password = requestDto.getPassword();

        if (loginId.isBlank() || password == null || password.isBlank()) {
            throw new UnauthorizedException(GENERIC_SIGN_IN_FAILURE);
        }

        if (loginAttemptPort.isLocked(loginId)) {
            throw accountLockedWhileRetrying(loginId);
        }

        int failCount = loginAttemptPort.getFailCount(loginId);
        if (failCount >= loginAttemptProperties.getCaptchaThreshold()) {
            requireValidCaptcha(requestDto.getCaptchaToken());
        }

        Optional<AuthDomain> authOptional = authRepositoryPort.findByLoginId(loginId);
        if (authOptional.isEmpty()) {
            throwLoginFailure(loginId);
        }

        AuthDomain auth = authOptional.get();
        if (!auth.isActive()) {
            throw new MemberNotActiveException("현재 로그인할 수 없는 계정입니다.");
        }

        if (!passwordEncoderPort.matches(password, auth.getPasswordHash())) {
            throwLoginFailure(loginId);
        }

        loginAttemptPort.reset(loginId);
        return issueTokens(auth, true);
    }

    @Override
    @Transactional
    public AuthSignInResultDto refresh(AuthRefreshRequestDto requestDto) {
        if (requestDto.refreshToken() == null || requestDto.refreshToken().isBlank()) {
            throw new InvalidTokenException("refreshToken은 필수입니다.");
        }

        ParsedTokenDto parsed = tokenProviderPort.parseRefreshToken(requestDto.refreshToken().trim());

        AuthDomain auth = authRepositoryPort.findByAuthUuid(parsed.getAuthUuid())
                .orElseThrow(() -> new InvalidTokenException("유효하지 않은 refresh token입니다."));

        if (!auth.isActive()) {
            revokeSessionForInactiveAccount(parsed.getAuthUuid());
            throw new MemberNotActiveException("현재 로그인할 수 없는 계정입니다.");
        }

        String accessToken = tokenProviderPort.createAccessToken(auth.getAuthUuid());
        String refreshToken = tokenProviderPort.createRefreshToken(auth.getAuthUuid());
        ParsedTokenDto parsedAccess = tokenProviderPort.parseAccessToken(accessToken);
        ParsedTokenDto parsedRefresh = tokenProviderPort.parseRefreshToken(refreshToken);

        long refreshTtlSeconds = remainingTtlSeconds(parsedRefresh.getExpiresAt());
        RefreshTokenRotationResult rotationResult = refreshTokenPort.rotate(
                parsed.getAuthUuid(),
                parsed.getTokenId(),
                parsedRefresh.getTokenId(),
                refreshTtlSeconds
        );
        if (rotationResult != RefreshTokenRotationResult.SUCCESS) {
            handleRefreshRotationFailure(rotationResult);
        }

        saveActiveAccessToken(parsed.getAuthUuid(), parsedAccess.getTokenId(), parsedAccess.getExpiresAt());

        return AuthSignInResultDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authUuid(auth.getAuthUuid())
                .memberName(auth.getMemberName())
                .role(DEFAULT_ROLE)
                .build();
    }

    @Override
    @Transactional
    public void logout(AuthLogoutRequestDto requestDto) {
        if (requestDto.getRefreshToken() != null && !requestDto.getRefreshToken().isBlank()) {
            try {
                ParsedTokenDto refresh = tokenProviderPort.parseRefreshToken(requestDto.getRefreshToken().trim());
                refreshTokenPort.deleteIfMatches(refresh.getAuthUuid(), refresh.getTokenId());
            } catch (InvalidTokenException ex) {
                // 이미 무효화·만료 refresh — best-effort logout
            }
        }

        if (requestDto.getAccessToken() != null && !requestDto.getAccessToken().isBlank()) {
            try {
                ParsedTokenDto access = tokenProviderPort.parseAccessToken(requestDto.getAccessToken().trim());
                blacklistAccessToken(access.getTokenId(), access.getExpiresAt());
                activeAccessTokenPort.deleteIfMatches(access.getAuthUuid(), access.getTokenId());
            } catch (InvalidTokenException ex) {
                // 이미 무효화·만료 access — best-effort logout
            }
        }
    }

    @Override
    public AuthAvailabilityResultDto checkLoginIdAvailability(String loginId) {
        String normalized = AuthDomain.normalizeLoginIdForLookup(loginId);
        return AuthAvailabilityResultDto.builder()
                .available(!authRepositoryPort.existsByLoginId(normalized))
                .build();
    }

    @Override
    public AuthAvailabilityResultDto checkEmailAvailability(String email) {
        String normalized = AuthDomain.normalizeEmailForLookup(email);
        return AuthAvailabilityResultDto.builder()
                .available(!authRepositoryPort.existsByEmail(normalized))
                .build();
    }

    private AuthSignInResultDto issueTokens(AuthDomain auth, boolean invalidatePreviousSession) {
        String authUuid = auth.getAuthUuid();

        if (invalidatePreviousSession) {
            // Redis auth:access 값은 jti만 저장 — 이전 토큰 expiresAt 없음. 설정된 access 전체 수명으로 blacklist TTL(과잉 보관 허용).
            activeAccessTokenPort.find(authUuid).ifPresent(previousAccessJti ->
                    accessTokenBlacklistPort.blacklist(previousAccessJti, configuredAccessTokenTtlSeconds())
            );
        }

        String accessToken = tokenProviderPort.createAccessToken(authUuid);
        String refreshToken = tokenProviderPort.createRefreshToken(authUuid);
        ParsedTokenDto parsedAccess = tokenProviderPort.parseAccessToken(accessToken);
        ParsedTokenDto parsedRefresh = tokenProviderPort.parseRefreshToken(refreshToken);

        saveActiveAccessToken(authUuid, parsedAccess.getTokenId(), parsedAccess.getExpiresAt());
        saveRefreshToken(authUuid, parsedRefresh.getTokenId(), parsedRefresh.getExpiresAt());

        return AuthSignInResultDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authUuid(auth.getAuthUuid())
                .memberName(auth.getMemberName())
                .role(DEFAULT_ROLE)
                .build();
    }

    private void saveActiveAccessToken(String authUuid, String tokenId, Instant expiresAt) {
        long ttlSeconds = remainingTtlSeconds(expiresAt);
        if (ttlSeconds > 0) {
            activeAccessTokenPort.save(authUuid, tokenId, ttlSeconds);
        }
    }

    private void saveRefreshToken(String authUuid, String tokenId, Instant expiresAt) {
        long ttlSeconds = remainingTtlSeconds(expiresAt);
        if (ttlSeconds > 0) {
            refreshTokenPort.save(authUuid, tokenId, ttlSeconds);
        }
    }

    private void blacklistAccessToken(String tokenId, Instant expiresAt) {
        long ttlSeconds = remainingTtlSeconds(expiresAt);
        accessTokenBlacklistPort.blacklist(tokenId, ttlSeconds);
    }

    private void handleRefreshRotationFailure(RefreshTokenRotationResult rotationResult) {
        if (rotationResult == RefreshTokenRotationResult.KEY_NOT_FOUND) {
            throw new InvalidTokenException("유효하지 않은 refresh token입니다.");
        }
        throw new InvalidTokenException("유효하지 않은 refresh token입니다.");
    }

    private void revokeSessionForInactiveAccount(String authUuid) {
        // Redis 세션 정리는 DB @Transactional과 원자적이지 않음 — 부분 실패 시 재시도·모니터링으로 보완.
        // 활성 access Redis 값에 expiresAt 없음 — parseable access token blacklist와 동일하게 설정 TTL 사용.
        activeAccessTokenPort.find(authUuid).ifPresent(jti ->
                accessTokenBlacklistPort.blacklist(jti, configuredAccessTokenTtlSeconds())
        );
        activeAccessTokenPort.delete(authUuid);
        refreshTokenPort.delete(authUuid);
    }

    private void enforceLoginRateLimit(String clientIp) {
        LoginRateLimitResultDto result = loginRateLimitPort.checkAndRecord(clientIp);
        if (!result.isAllowed()) {
            throw new LoginRateLimitedException(result.getRetryAfterSeconds());
        }
    }

    private void requireValidCaptcha(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new CaptchaRequiredException("로그인 시도가 많습니다. 보안 확인을 완료해 주세요.");
        }
        if (!captchaVerificationPort.verify(captchaToken)) {
            throw new CaptchaInvalidException("보안 확인에 실패했습니다. 다시 시도해 주세요.");
        }
    }

    private void throwLoginFailure(String loginId) {
        LoginFailureOutcome outcome = recordLoginFailure(loginId);
        switch (outcome) {
            case CAPTCHA_REQUIRED -> throw new CaptchaRequiredException(
                    "로그인 시도가 많습니다. 보안 확인을 완료해 주세요."
            );
            case ACCOUNT_LOCKED -> throw accountLockedOnTrigger();
            default -> throw new UnauthorizedException(GENERIC_SIGN_IN_FAILURE);
        }
    }

    private LoginFailureOutcome recordLoginFailure(String loginId) {
        int nextFailCount = loginAttemptPort.incrementFailCount(loginId);
        if (nextFailCount >= loginAttemptProperties.getLockThreshold()) {
            loginAttemptPort.lock(loginId);
            return LoginFailureOutcome.ACCOUNT_LOCKED;
        }
        if (nextFailCount >= loginAttemptProperties.getCaptchaThreshold()) {
            return LoginFailureOutcome.CAPTCHA_REQUIRED;
        }
        return LoginFailureOutcome.NORMAL_FAILURE;
    }

    private AccountLockedException accountLockedOnTrigger() {
        long retryAfterSeconds = lockDurationSeconds();
        return new AccountLockedException(accountLockedOnTriggerMessage(), retryAfterSeconds);
    }

    private AccountLockedException accountLockedWhileRetrying(String loginId) {
        long retryAfterSeconds = resolveLockRetryAfterSeconds(loginId);
        return new AccountLockedException(accountLockedWhileRetryingMessage(), retryAfterSeconds);
    }

    private long resolveLockRetryAfterSeconds(String loginId) {
        long remaining = loginAttemptPort.getLockRemainingSeconds(loginId);
        return remaining > 0L ? remaining : lockDurationSeconds();
    }

    private long lockDurationSeconds() {
        return loginAttemptProperties.getLockDurationMinutes() * 60L;
    }

    private String accountLockedOnTriggerMessage() {
        return "로그인 시도가 많아 "
                + loginAttemptProperties.getLockDurationMinutes()
                + "분간 로그인이 제한됩니다.";
    }

    private String accountLockedWhileRetryingMessage() {
        return "로그인이 일시적으로 제한되었습니다. 잠시 후 다시 시도해 주세요.";
    }

    private long configuredAccessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenMinutes() * 60L;
    }

    private long remainingTtlSeconds(Instant expiresAt) {
        return TokenTtlCalculator.remainingTtlSeconds(expiresAt, clock.instant());
    }

    private void validateVerifiedCustomer(ExternalIdentityVerificationDto external) {
        if (!"VERIFIED".equals(external.getPortOneStatus())) {
            throw new IdentityVerificationNotReadyException("본인인증이 완료되지 않았습니다.");
        }
        if (external.getIdentityKey() == null || external.getIdentityKey().isBlank()) {
            throw new IdentityVerificationFailedException("본인인증 CI를 확인할 수 없습니다.");
        }
        if (external.getMemberName() == null || external.getMemberName().isBlank()
                || external.getPhoneNumber() == null || external.getPhoneNumber().isBlank()
                || external.getBirthdayDate() == null
                || external.getGender() == null) {
            throw new IdentityVerificationFailedException("본인인증 고객 정보가 불완전합니다.");
        }
    }

    private void validateCiMatchesStoredVerification(
            ExternalIdentityVerificationDto external,
            IdentityVerificationDomain verification
    ) {
        String ciHash = identityKeyHashPort.hashForLookup(external.getIdentityKey());
        if (!ciHash.equals(verification.getCiHash())) {
            throw new IdentityVerificationFailedException("본인인증 정보가 일치하지 않습니다.");
        }
    }

}

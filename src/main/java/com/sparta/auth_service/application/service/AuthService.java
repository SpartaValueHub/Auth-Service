package com.sparta.auth_service.application.service;

import com.sparta.auth_service.application.exception.AccountLockedException;
import com.sparta.auth_service.application.exception.DuplicateResourceException;
import com.sparta.auth_service.application.exception.IdentityVerificationAlreadyUsedException;
import com.sparta.auth_service.application.exception.IdentityVerificationFailedException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotFoundException;
import com.sparta.auth_service.application.exception.IdentityVerificationNotReadyException;
import com.sparta.auth_service.application.exception.InvalidTokenException;
import com.sparta.auth_service.application.exception.UnauthorizedException;
import com.sparta.auth_service.application.port.in.AuthUseCase;
import com.sparta.auth_service.application.port.in.dto.AuthAvailabilityResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthLogoutRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthRefreshRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignInResultDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpRequestDto;
import com.sparta.auth_service.application.port.in.dto.AuthSignUpResultDto;
import com.sparta.auth_service.application.port.out.AccessTokenBlacklistPort;
import com.sparta.auth_service.application.port.out.AuthRepositoryPort;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.IdentityVerificationRepositoryPort;
import com.sparta.auth_service.application.port.out.PasswordEncoderPort;
import com.sparta.auth_service.application.port.out.RefreshTokenPort;
import com.sparta.auth_service.application.port.out.TokenProviderPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.application.port.out.dto.ParsedTokenDto;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.model.AuthDomain;
import com.sparta.auth_service.domain.model.IdentityVerificationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 인증 계정(로그인·JWT·CI) 전용 Application Service.
 * 닉네임·프로필은 member-service 책임 — sign-up body에 포함하지 않음.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

    private static final long REFRESH_TOKEN_TTL_SECONDS = 14L * 24 * 60 * 60;
    private static final String DEFAULT_ROLE = "USER";

    private final AuthRepositoryPort authRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;
    private final RefreshTokenPort refreshTokenPort;
    private final AccessTokenBlacklistPort accessTokenBlacklistPort;
    private final IdentityVerificationRepositoryPort identityVerificationRepositoryPort;
    private final FetchIdentityVerificationPort fetchIdentityVerificationPort;

    @Override
    @Transactional
    public AuthSignUpResultDto signUp(AuthSignUpRequestDto requestDto) {
        if (requestDto.getRequestToken() == null || requestDto.getRequestToken().isBlank()) {
            throw new IllegalArgumentException("requestToken은 필수입니다.");
        }

        String requestToken = requestDto.getRequestToken().trim();
        // confirm API SUCCESS 이력 + purpose SIGN_UP + 미사용(requestToken) 검증
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

        // 실명·전화·CI는 클라이언트 입력을 신뢰하지 않고 PortOne 조회 결과만 사용
        ExternalIdentityVerificationDto external = fetchIdentityVerificationPort.fetchByRequestToken(requestToken)
                .orElseThrow(() -> new IdentityVerificationNotFoundException("본인인증 내역을 찾을 수 없습니다."));
        validateVerifiedCustomer(external);

        AuthDomain.validatePlainPassword(requestDto.getPassword());
        String passwordHash = passwordEncoderPort.encode(requestDto.getPassword());

        AuthDomain authDomain = AuthDomain.createSignUp(
                requestDto.getLoginId(),
                passwordHash,
                requestDto.getEmail(),
                external.getMemberName(),
                external.getBirthdayDate(),
                external.getPhoneNumber(),
                external.getGender(),
                external.getIdentityKey()
        );

        validateDuplication(authDomain);

        AuthDomain saved = authRepositoryPort.save(authDomain);
        // 동일 requestToken 재가입 방지 — verification에 authUuid 연결
        identityVerificationRepositoryPort.save(verification.withMemberUuid(saved.getAuthUuid()));

        return AuthSignUpResultDto.builder()
                .authUuid(saved.getAuthUuid())
                .loginId(saved.getLoginId())
                .email(saved.getEmail())
                .memberName(saved.getMemberName())
                .birthdayDate(saved.getBirthdayDate())
                .build();
    }

    @Override
    @Transactional
    public AuthSignInResultDto signIn(AuthSignInRequestDto requestDto) {
        String loginId = requestDto.getLoginId() == null ? "" : requestDto.getLoginId().trim();
        String password = requestDto.getPassword();

        if (loginId.isBlank() || password == null || password.isBlank()) {
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 존재 여부·비밀번호 오류를 구분하지 않아 계정 열거를 막음
        AuthDomain auth = authRepositoryPort.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다."));

        Instant now = Instant.now();
        if (auth.isLocked(now)) {
            throw new AccountLockedException("계정이 잠겼습니다. 잠시 후 다시 시도하거나 본인인증으로 잠금을 해제해 주세요.");
        }

        if (!passwordEncoderPort.matches(password, auth.getPasswordHash())) {
            AuthDomain failedAuth = auth.recordLoginFailure(now);
            authRepositoryPort.save(failedAuth);
            throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        AuthDomain successAuth = auth.resetLoginFailure();
        // 잠금·실패횟수 변경 시에만 DB write (불필요한 update 방지)
        if (successAuth.getLoginFailCount() != auth.getLoginFailCount()
                || successAuth.getLockedUntil() != auth.getLockedUntil()) {
            auth = authRepositoryPort.save(successAuth);
        } else {
            auth = successAuth;
        }

        return issueTokens(auth);
    }

    @Override
    @Transactional
    public AuthSignInResultDto refresh(AuthRefreshRequestDto requestDto) {
        if (requestDto.getRefreshToken() == null || requestDto.getRefreshToken().isBlank()) {
            throw new InvalidTokenException("refreshToken은 필수입니다.");
        }

        ParsedTokenDto parsed = tokenProviderPort.parseRefreshToken(requestDto.getRefreshToken().trim());
        if (!refreshTokenPort.matches(parsed.getAuthUuid(), parsed.getTokenId())) {
            throw new InvalidTokenException("유효하지 않은 refresh token입니다.");
        }

        AuthDomain auth = authRepositoryPort.findByAuthUuid(parsed.getAuthUuid())
                .orElseThrow(() -> new InvalidTokenException("유효하지 않은 refresh token입니다."));

        // refresh token rotation — 기존 refresh는 Redis에서 삭제 후 재발급
        refreshTokenPort.delete(parsed.getAuthUuid());
        return issueTokens(auth);
    }

    @Override
    @Transactional
    public void logout(AuthLogoutRequestDto requestDto) {
        if (requestDto.getRefreshToken() != null && !requestDto.getRefreshToken().isBlank()) {
            ParsedTokenDto refresh = tokenProviderPort.parseRefreshToken(requestDto.getRefreshToken().trim());
            refreshTokenPort.delete(refresh.getAuthUuid());
        }

        // access token은 Gateway blacklist(Redis TTL=잔여 만료)로 무효화
        if (requestDto.getAccessToken() != null && !requestDto.getAccessToken().isBlank()) {
            ParsedTokenDto access = tokenProviderPort.parseAccessToken(requestDto.getAccessToken().trim());
            long ttlSeconds = Duration.between(Instant.now(), access.getExpiresAt()).getSeconds();
            accessTokenBlacklistPort.blacklist(access.getTokenId(), ttlSeconds);
        }
    }

    @Override
    public AuthAvailabilityResultDto checkLoginIdAvailability(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 필수입니다.");
        }
        return AuthAvailabilityResultDto.builder()
                .available(!authRepositoryPort.existsByLoginId(loginId.trim()))
                .build();
    }

    @Override
    public AuthAvailabilityResultDto checkEmailAvailability(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }
        String normalized = email.trim().toLowerCase();
        return AuthAvailabilityResultDto.builder()
                .available(!authRepositoryPort.existsByEmail(normalized))
                .build();
    }

    private AuthSignInResultDto issueTokens(AuthDomain auth) {
        String accessToken = tokenProviderPort.createAccessToken(auth.getAuthUuid());
        String refreshToken = tokenProviderPort.createRefreshToken(auth.getAuthUuid());
        ParsedTokenDto parsedRefresh = tokenProviderPort.parseRefreshToken(refreshToken);
        // refresh jti를 Redis에 저장 — rotation·logout 시 matches/delete
        refreshTokenPort.save(parsedRefresh.getAuthUuid(), parsedRefresh.getTokenId(), REFRESH_TOKEN_TTL_SECONDS);

        return AuthSignInResultDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authUuid(auth.getAuthUuid())
                .loginId(auth.getLoginId())
                .memberName(auth.getMemberName())
                .email(auth.getEmail())
                .role(DEFAULT_ROLE)
                .build();
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

    private void validateDuplication(AuthDomain authDomain) {
        // CI(identityKey) 포함 — 동일인·연락처·loginId·email 중복 가입 차단
        if (authRepositoryPort.existsByLoginId(authDomain.getLoginId())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_LOGIN_ID", "이미 사용 중인 loginId입니다.");
        }
        if (authRepositoryPort.existsByEmail(authDomain.getEmail())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_EMAIL", "이미 사용 중인 email입니다.");
        }
        if (authRepositoryPort.existsByPhoneNumber(authDomain.getPhoneNumber())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_PHONE", "이미 사용 중인 phoneNumber입니다.");
        }
        if (authRepositoryPort.existsByIdentityKey(authDomain.getIdentityKey())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_IDENTITY", "이미 가입된 본인인증 정보입니다.");
        }
    }
}

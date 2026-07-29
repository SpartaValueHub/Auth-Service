package com.unionclass.auth_service.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthDomain {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^01[016789]\\d{7,8}$");

    private String userId;
    private String logInId;
    private String password;
    private String email;
    private String name;
    private String phone;
    private boolean deleted;

    public static AuthDomain createSignUp(
            String logInId,
            String rawPassword,
            String email,
            String name,
            String phone
    ) {
        validateLogInId(logInId);
        validatePassword(rawPassword);
        validateEmail(email);
        validateName(name);
        validatePhone(phone);

        return AuthDomain.builder()
                .userId(UUID.randomUUID().toString())
                .logInId(logInId.trim())
                .password(rawPassword)
                .email(email.trim())
                .name(name.trim())
                .phone(phone.trim())
                .deleted(false)
                .build();
    }

    public static AuthDomain reconstitute(
            String userId,
            String logInId,
            String password,
            String email,
            String name,
            String phone,
            boolean deleted
    ) {
        return AuthDomain.builder()
                .userId(userId)
                .logInId(logInId)
                .password(password)
                .email(email)
                .name(name)
                .phone(phone)
                .deleted(deleted)
                .build();
    }

    public AuthDomain withEncodedPassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("암호화된 비밀번호는 필수입니다.");
        }
        return AuthDomain.builder()
                .userId(this.userId)
                .logInId(this.logInId)
                .password(encodedPassword)
                .email(this.email)
                .name(this.name)
                .phone(this.phone)
                .deleted(this.deleted)
                .build();
    }

    private static void validateLogInId(String logInId) {
        if (logInId == null || logInId.isBlank()) {
            throw new IllegalArgumentException("loginId는 필수입니다.");
        }
        if (logInId.trim().length() < 4) {
            throw new IllegalArgumentException("loginId는 4자 이상이어야 합니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("password는 8자 이상이어야 합니다.");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("email 형식이 올바르지 않습니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }
    }

    private static void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone은 필수입니다.");
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("phone 형식이 올바르지 않습니다.");
        }
    }

    @Builder(access = AccessLevel.PRIVATE)
    private AuthDomain(
            String userId,
            String logInId,
            String password,
            String email,
            String name,
            String phone,
            boolean deleted
    ) {
        this.userId = userId;
        this.logInId = logInId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.deleted = deleted;
    }
}

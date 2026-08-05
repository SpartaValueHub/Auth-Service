package com.sparta.auth_service.adaptor.out.mysql;

import com.sparta.auth_service.application.exception.DuplicateResourceException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;
import java.util.Optional;

/**
 * auth 테이블 unique 위반 → DuplicateResourceException 변환.
 * Hibernate ConstraintViolationException.getConstraintName() 우선, 메시지 파싱은 fallback.
 */
final class AuthDataIntegrityViolationMapper {

    private AuthDataIntegrityViolationMapper() {
    }

    static Optional<DuplicateResourceException> mapDuplicate(DataIntegrityViolationException ex) {
        Optional<DuplicateResourceException> byConstraint = mapByConstraintName(ex);
        if (byConstraint.isPresent()) {
            return byConstraint;
        }
        return mapByMessageFallback(ex);
    }

    static boolean isNonDuplicateIntegrityViolation(DataIntegrityViolationException ex) {
        String normalized = collectMessages(ex).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.contains("cannot be null")
                || normalized.contains("foreign key constraint")
                || normalized.contains("a foreign key constraint fails");
    }

    static boolean isDuplicateEntryViolation(DataIntegrityViolationException ex) {
        return collectMessages(ex).toLowerCase(Locale.ROOT).contains("duplicate entry");
    }

    private static Optional<DuplicateResourceException> mapByConstraintName(DataIntegrityViolationException ex) {
        ConstraintViolationException hibernateEx = findHibernateConstraintViolation(ex);
        if (hibernateEx == null) {
            return Optional.empty();
        }
        String constraintName = hibernateEx.getConstraintName();
        if (constraintName == null || constraintName.isBlank()) {
            return Optional.empty();
        }
        DuplicateResourceException mapped = mapConstraintName(constraintName.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(mapped);
    }

    private static ConstraintViolationException findHibernateConstraintViolation(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException hibernateEx) {
                return hibernateEx;
            }
            current = current.getCause();
        }
        return null;
    }

    private static DuplicateResourceException mapConstraintName(String constraintName) {
        if (constraintName.contains("uk_auth_login_id")) {
            return duplicateLoginId();
        }
        if (constraintName.contains("uk_auth_email")) {
            return duplicateEmail();
        }
        if (constraintName.contains("uk_auth_phone_number")) {
            return duplicatePhone();
        }
        if (constraintName.contains("uk_auth_auth_uuid")) {
            return genericDuplicate();
        }
        return null;
    }

    private static Optional<DuplicateResourceException> mapByMessageFallback(DataIntegrityViolationException ex) {
        String message = collectMessages(ex);
        if (message.isBlank()) {
            return Optional.empty();
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (!normalized.contains("duplicate entry")) {
            return Optional.empty();
        }
        if (normalized.contains("uk_auth_login_id")
                || (normalized.contains("login_id") && normalized.contains("duplicate"))) {
            return Optional.of(duplicateLoginId());
        }
        if (normalized.contains("uk_auth_email")
                || (normalized.contains("email") && normalized.contains("duplicate"))) {
            return Optional.of(duplicateEmail());
        }
        if (normalized.contains("uk_auth_phone_number")
                || normalized.contains("phone_number")
                || (normalized.contains("phone") && normalized.contains("duplicate"))) {
            return Optional.of(duplicatePhone());
        }
        return Optional.empty();
    }

    private static String collectMessages(DataIntegrityViolationException ex) {
        StringBuilder messages = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                if (!messages.isEmpty()) {
                    messages.append(' ');
                }
                messages.append(current.getMessage());
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    private static DuplicateResourceException duplicateLoginId() {
        return new DuplicateResourceException("AUTH_DUPLICATE_LOGIN_ID", "이미 사용 중인 loginId입니다.");
    }

    private static DuplicateResourceException duplicateEmail() {
        return new DuplicateResourceException("AUTH_DUPLICATE_EMAIL", "이미 사용 중인 email입니다.");
    }

    private static DuplicateResourceException duplicatePhone() {
        return new DuplicateResourceException("AUTH_DUPLICATE_PHONE", "이미 사용 중인 phoneNumber입니다.");
    }

    private static DuplicateResourceException genericDuplicate() {
        return new DuplicateResourceException("AUTH_DUPLICATE", "이미 사용 중인 정보입니다.");
    }
}

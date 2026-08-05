package com.sparta.auth_service.adaptor.out.portone;

import com.sparta.auth_service.adaptor.out.portone.dto.PortOneIdentityVerificationResponse;
import com.sparta.auth_service.application.exception.ExternalIdentityProviderUnavailableException;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import com.sparta.auth_service.domain.enums.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * PortOne V2 본인인증 조회 Outbound Adapter.
 * API Secret은 Authorization 헤더로만 전달 — 로그·응답에 노출 금지.
 * <p>
 * 404 → empty(NotFound). timeout/network/5xx/429/파싱 실패 → fail-closed 503.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneIdentityVerificationAdapter implements FetchIdentityVerificationPort {

    private final RestClient portOneRestClient;

    @Override
    public Optional<ExternalIdentityVerificationDto> fetchByRequestToken(String requestToken) {
        try {
            PortOneIdentityVerificationResponse response = portOneRestClient.get()
                    .uri("/identity-verifications/{identityVerificationId}", requestToken)
                    .retrieve()
                    .body(PortOneIdentityVerificationResponse.class);

            if (response == null || response.getStatus() == null) {
                throw new ExternalIdentityProviderUnavailableException(
                        new IllegalStateException("PortOne response missing status")
                );
            }

            PortOneIdentityVerificationResponse.VerifiedCustomer customer = response.getVerifiedCustomer();
            // CI·고객정보는 DTO로 Application에 전달 — identity_verifications 테이블에는 status만 저장
            return Optional.of(ExternalIdentityVerificationDto.builder()
                    .requestToken(requestToken)
                    .portOneStatus(response.getStatus())
                    .identityKey(customer != null ? customer.getCi() : null)
                    .memberName(customer != null ? customer.getName() : null)
                    .phoneNumber(customer != null ? customer.getPhoneNumber() : null)
                    .birthdayDate(parseBirthDate(customer))
                    .gender(parseGender(customer != null ? customer.getGender() : null))
                    .build());
        } catch (ExternalIdentityProviderUnavailableException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            if (isProviderFailureStatus(ex.getStatusCode().value())) {
                log.warn("portone provider unavailable: status={} exception={}",
                        ex.getStatusCode().value(), ex.getClass().getSimpleName());
                throw new ExternalIdentityProviderUnavailableException(ex);
            }
            log.warn("portone request failed: status={} exception={}",
                    ex.getStatusCode().value(), ex.getClass().getSimpleName());
            throw new ExternalIdentityProviderUnavailableException(ex);
        } catch (Exception ex) {
            if (isProviderFailure(ex)) {
                log.warn("portone provider unavailable: reason={} exception={}",
                        resolveFailureReason(ex), ex.getClass().getSimpleName());
                log.debug("portone provider failure detail", ex);
                throw new ExternalIdentityProviderUnavailableException(ex);
            }
            throw new ExternalIdentityProviderUnavailableException(ex);
        }
    }

    private static LocalDate parseBirthDate(PortOneIdentityVerificationResponse.VerifiedCustomer customer) {
        if (customer == null || customer.getBirthDate() == null || customer.getBirthDate().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(customer.getBirthDate());
        } catch (Exception ex) {
            throw new ExternalIdentityProviderUnavailableException(ex);
        }
    }

    private static Gender parseGender(String rawGender) {
        if (rawGender == null || rawGender.isBlank()) {
            return null;
        }
        return switch (rawGender.trim().toUpperCase()) {
            case "MALE" -> Gender.MALE;
            case "FEMALE" -> Gender.FEMALE;
            case "OTHER" -> Gender.OTHER;
            default -> null;
        };
    }

    private static boolean isProviderFailureStatus(int status) {
        return status >= 500 || status == 429;
    }

    private static boolean isProviderFailure(Throwable ex) {
        if (hasCauseOfType(ex, HttpMessageNotReadableException.class)) {
            return true;
        }
        if (hasTimeoutCause(ex)) {
            return true;
        }
        return ex instanceof ConnectException;
    }

    private static String resolveFailureReason(Throwable ex) {
        if (hasCauseOfType(ex, HttpMessageNotReadableException.class)) {
            return "portone_parse_failed";
        }
        if (hasTimeoutCause(ex)) {
            return "portone_timeout";
        }
        return "portone_request_failed";
    }

    private static boolean hasCauseOfType(Throwable ex, Class<? extends Throwable> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasTimeoutCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

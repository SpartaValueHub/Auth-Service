package com.sparta.auth_service.adaptor.out.portone;

import com.sparta.auth_service.adaptor.out.portone.dto.PortOneIdentityVerificationResponse;
import com.sparta.auth_service.application.port.out.FetchIdentityVerificationPort;
import com.sparta.auth_service.application.port.out.dto.ExternalIdentityVerificationDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Optional;

/**
 * PortOne V2 본인인증 조회 Outbound Adapter.
 * API Secret은 Authorization 헤더로만 전달 — 로그·응답에 노출 금지.
 */
@Component
@RequiredArgsConstructor
public class PortOneIdentityVerificationAdapter implements FetchIdentityVerificationPort {

    private final PortOneProperties properties;
    private RestClient restClient;

    @PostConstruct
    void init() {
        if (properties.getApiSecret() == null || properties.getApiSecret().isBlank()) {
            throw new IllegalStateException("portone.api-secret must be configured");
        }
        restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "PortOne " + properties.getApiSecret())
                .build();
    }

    @Override
    public Optional<ExternalIdentityVerificationDto> fetchByRequestToken(String requestToken) {
        try {
            PortOneIdentityVerificationResponse response = restClient.get()
                    .uri("/identity-verifications/{identityVerificationId}", requestToken)
                    .retrieve()
                    .body(PortOneIdentityVerificationResponse.class);

            if (response == null || response.getStatus() == null) {
                return Optional.empty();
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
                    .build());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new PortOneApiException("PortOne 본인인증 조회에 실패했습니다.", ex);
        } catch (Exception ex) {
            throw new PortOneApiException("PortOne 본인인증 조회에 실패했습니다.", ex);
        }
    }

    private static LocalDate parseBirthDate(PortOneIdentityVerificationResponse.VerifiedCustomer customer) {
        if (customer == null || customer.getBirthDate() == null || customer.getBirthDate().isBlank()) {
            return null;
        }
        return LocalDate.parse(customer.getBirthDate());
    }
}

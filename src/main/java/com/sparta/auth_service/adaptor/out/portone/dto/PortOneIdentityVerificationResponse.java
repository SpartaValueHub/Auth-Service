package com.sparta.auth_service.adaptor.out.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/** PortOne V2 본인인증 API 역직렬화 — ci·고객정보는 Adapter에서 Application DTO로만 전달 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOneIdentityVerificationResponse {

    private String status;
    private String id;

    @JsonProperty("verifiedCustomer")
    private VerifiedCustomer verifiedCustomer;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifiedCustomer {

        private String ci;
        private String name;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("birthDate")
        private String birthDate;

        private String gender;
    }
}

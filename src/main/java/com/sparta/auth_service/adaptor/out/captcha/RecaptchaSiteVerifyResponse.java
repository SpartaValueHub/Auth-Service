package com.sparta.auth_service.adaptor.out.captcha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Google reCAPTCHA v2 siteverify 응답 — score/action(v3) 미사용 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class RecaptchaSiteVerifyResponse {

    private boolean success;

    @JsonProperty("challenge_ts")
    private String challengeTs;

    private String hostname;

    @JsonProperty("error-codes")
    private String[] errorCodes;

    RecaptchaSiteVerifyResponse(boolean success, String challengeTs, String hostname, String[] errorCodes) {
        this.success = success;
        this.challengeTs = challengeTs;
        this.hostname = hostname;
        this.errorCodes = errorCodes;
    }
}

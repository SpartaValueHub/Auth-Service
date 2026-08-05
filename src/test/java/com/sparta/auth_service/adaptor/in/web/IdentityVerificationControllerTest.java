package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.mapper.IdentityVerificationWebMapper;
import com.sparta.auth_service.application.port.in.IdentityVerificationUseCase;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationStatusResultDto;
import com.sparta.auth_service.domain.enums.VerificationPurpose;
import com.sparta.auth_service.domain.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IdentityVerificationControllerTest {

    @Mock
    private IdentityVerificationUseCase identityVerificationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        IdentityVerificationController controller = new IdentityVerificationController(
                identityVerificationUseCase,
                new IdentityVerificationWebMapper()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandlerTestSupport.handler())
                .setValidator(validator)
                .build();
    }

    @Test
    void getStatusByPathVariable_isNotAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/identity-verifications/verify-token-001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postStatus_returnsPurposeAndStatusOnly() throws Exception {
        when(identityVerificationUseCase.getStatus(eq("verify-token-001")))
                .thenReturn(IdentityVerificationStatusResultDto.builder()
                        .purpose(VerificationPurpose.SIGN_UP)
                        .status(VerificationStatus.SUCCESS)
                        .build());

        mockMvc.perform(post("/api/v1/identity-verifications/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestToken":"verify-token-001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.purpose").value("SIGN_UP"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.requestToken").doesNotExist())
                .andExpect(jsonPath("$.memberName").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.gender").doesNotExist());

        verify(identityVerificationUseCase).getStatus("verify-token-001");
    }

    @Test
    void postStatus_blankRequestToken_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/identity-verifications/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestToken":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("requestToken"));
    }

    @Test
    void postStatus_missingRequestToken_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/identity-verifications/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("requestToken"));
    }
}

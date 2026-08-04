package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.mapper.IdentityVerificationWebMapper;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationConfirmRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationResponseVo;
import com.sparta.auth_service.application.port.in.IdentityVerificationUseCase;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 본인인증 Inbound Controller — confirm·status, PII는 응답 prefill만 */
@Tag(name = "IdentityVerification", description = "본인인증 API — PII는 응답 prefill만, DB 이력은 status")
@RequestMapping("/api/v1/identity-verifications")
@RestController
@RequiredArgsConstructor
public class IdentityVerificationController {

    private final IdentityVerificationUseCase identityVerificationUseCase;
    private final IdentityVerificationWebMapper identityVerificationWebMapper;

    @Operation(summary = "본인인증 확인", description = "PortOne 본인인증 완료 후 서버에서 인증 결과를 확인·저장합니다.")
    @PostMapping("/confirm")
    public IdentityVerificationResponseVo confirm(
            @RequestBody IdentityVerificationConfirmRequestVo requestVo
    ) {
        IdentityVerificationConfirmRequestDto requestDto = identityVerificationWebMapper.toDto(requestVo);
        IdentityVerificationResultDto resultDto = identityVerificationUseCase.confirm(requestDto);
        return identityVerificationWebMapper.toVo(resultDto);
    }

    @Operation(summary = "본인인증 상태 조회", description = "저장된 본인인증 상태를 조회합니다.")
    @GetMapping("/{requestToken}")
    public IdentityVerificationResponseVo getStatus(@PathVariable String requestToken) {
        IdentityVerificationResultDto resultDto = identityVerificationUseCase.getStatus(requestToken);
        return identityVerificationWebMapper.toVo(resultDto);
    }
}

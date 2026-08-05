package com.sparta.auth_service.adaptor.in.web;

import com.sparta.auth_service.adaptor.in.web.mapper.IdentityVerificationWebMapper;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationConfirmRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationStatusRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationStatusResponseVo;
import com.sparta.auth_service.application.port.in.IdentityVerificationUseCase;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationResultDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationStatusResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 본인인증 Inbound Controller — confirm·status, PII는 confirm prefill만 */
@Tag(name = "IdentityVerification", description = "본인인증 API — confirm prefill·status 조회")
@RequestMapping("/api/v1/identity-verifications")
@RestController
@RequiredArgsConstructor
public class IdentityVerificationController {

    private final IdentityVerificationUseCase identityVerificationUseCase;
    private final IdentityVerificationWebMapper identityVerificationWebMapper;

    @Operation(summary = "본인인증 확인", description = "PortOne 본인인증 완료 후 서버에서 인증 결과를 확인·저장합니다.")
    @PostMapping("/confirm")
    public ResponseEntity<IdentityVerificationResponseVo> confirm(
            @RequestBody IdentityVerificationConfirmRequestVo requestVo
    ) {
        IdentityVerificationConfirmRequestDto requestDto = identityVerificationWebMapper.toDto(requestVo);
        IdentityVerificationResultDto resultDto = identityVerificationUseCase.confirm(requestDto);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(identityVerificationWebMapper.toConfirmVo(resultDto));
    }

    @Operation(
            summary = "본인인증 상태 조회",
            description = "저장된 본인인증 상태를 DB에서 조회합니다. requestToken은 body로만 전달합니다(URL·query·cookie 노출 방지)."
    )
    @PostMapping("/status")
    public ResponseEntity<IdentityVerificationStatusResponseVo> getStatus(
            @Valid @RequestBody IdentityVerificationStatusRequestVo requestVo
    ) {
        IdentityVerificationStatusResultDto resultDto =
                identityVerificationUseCase.getStatus(requestVo.getRequestToken());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(identityVerificationWebMapper.toStatusVo(resultDto));
    }
}

package com.sparta.auth_service.adaptor.in.web.mapper;

import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationConfirmRequestVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationResponseVo;
import com.sparta.auth_service.adaptor.in.web.vo.IdentityVerificationStatusResponseVo;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationConfirmRequestDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationResultDto;
import com.sparta.auth_service.application.port.in.dto.IdentityVerificationStatusResultDto;
import org.springframework.stereotype.Component;

/** VO ↔ Input DTO — confirm prefill(memberName·phone·gender)는 confirm 응답 VO에만 매핑 */
@Component
public class IdentityVerificationWebMapper {

    public IdentityVerificationConfirmRequestDto toDto(IdentityVerificationConfirmRequestVo vo) {
        return IdentityVerificationConfirmRequestDto.builder()
                .identityVerificationId(vo.getIdentityVerificationId())
                .purpose(vo.getPurpose())
                .build();
    }

    public IdentityVerificationResponseVo toConfirmVo(IdentityVerificationResultDto dto) {
        return IdentityVerificationResponseVo.builder()
                .requestToken(dto.getRequestToken())
                .purpose(dto.getPurpose())
                .status(dto.getStatus())
                .memberName(dto.getMemberName())
                .phoneNumber(dto.getPhoneNumber())
                .birthdayDate(dto.getBirthdayDate())
                .gender(dto.getGender())
                .build();
    }

    public IdentityVerificationStatusResponseVo toStatusVo(IdentityVerificationStatusResultDto dto) {
        return IdentityVerificationStatusResponseVo.builder()
                .purpose(dto.getPurpose())
                .status(dto.getStatus())
                .build();
    }
}

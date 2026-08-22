package com.sparta.auth_service.application.port.in;

import com.sparta.auth_service.application.port.in.dto.WithdrawMemberRequestDto;

/** PASS 본인인증 후 회원 탈퇴 — Auth 1차(거래·환불 확인 제외) */
public interface WithdrawMemberUseCase {

    void withdraw(WithdrawMemberRequestDto requestDto);
}

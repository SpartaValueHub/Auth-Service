package com.sparta.auth_service.application.port.in;

import com.sparta.auth_service.application.port.in.dto.GetMyAuthAccountResultDto;

/** 로그인 회원 계정 정보 조회 — 마이페이지 Auth 필드 */
public interface GetMyAuthAccountUseCase {

    GetMyAuthAccountResultDto getMyAuthAccount(String authUuid);
}

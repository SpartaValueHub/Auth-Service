package com.sparta.auth_service.application.port.in;

import com.sparta.auth_service.application.port.in.dto.GetMemberJoinedAtResultDto;

/** 타인(또는 본인) ACTIVE 계정의 가입일 공개 조회 */
public interface GetMemberJoinedAtUseCase {

    GetMemberJoinedAtResultDto getMemberJoinedAt(String memberUuid);
}

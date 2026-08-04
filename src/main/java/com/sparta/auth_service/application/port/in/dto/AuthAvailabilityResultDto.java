package com.sparta.auth_service.application.port.in.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthAvailabilityResultDto {

    private final boolean available;
}

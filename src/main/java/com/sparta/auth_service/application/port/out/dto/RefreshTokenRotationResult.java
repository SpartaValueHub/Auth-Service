package com.sparta.auth_service.application.port.out.dto;

/** Refresh Token Redis rotate 결과 — Lua atomic compare-and-set */
public enum RefreshTokenRotationResult {

    SUCCESS,
    KEY_NOT_FOUND,
    JTI_MISMATCH
}

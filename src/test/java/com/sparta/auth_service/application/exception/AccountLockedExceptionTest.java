package com.sparta.auth_service.application.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLockedExceptionTest {

    @Test
    void carriesRetryAfterSeconds() {
        AccountLockedException ex = new AccountLockedException("로그인 시도가 많아 1분간 로그인이 제한됩니다.", 60L);

        assertThat(ex.getMessage()).isEqualTo("로그인 시도가 많아 1분간 로그인이 제한됩니다.");
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(60L);
    }
}

package com.sparta.auth_service.application.port.out;

/** BCrypt encode/matches — Domain에는 hash만 전달 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}

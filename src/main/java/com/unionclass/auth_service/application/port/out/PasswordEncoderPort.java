package com.unionclass.auth_service.application.port.out;

public interface PasswordEncoderPort {

    String encode(String rawPassword);
}

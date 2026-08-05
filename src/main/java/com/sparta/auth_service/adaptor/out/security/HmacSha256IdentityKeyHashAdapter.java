package com.sparta.auth_service.adaptor.out.security;

import com.sparta.auth_service.application.port.out.IdentityKeyHashPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** CI HMAC-SHA256 검색 해시 */
@Component
@RequiredArgsConstructor
public class HmacSha256IdentityKeyHashAdapter implements IdentityKeyHashPort {

    @Value("${security.ci.hash-key:}")
    private String hashKeyBase64;

    private SecretKey hashKey;

    @PostConstruct
    void init() {
        if (hashKeyBase64 == null || hashKeyBase64.isBlank()) {
            throw new IllegalStateException("security.ci.hash-key must be configured");
        }
        byte[] hashBytes = Base64.getDecoder().decode(hashKeyBase64.trim());
        if (hashBytes.length < 16) {
            throw new IllegalStateException("security.ci.hash-key must decode to at least 16 bytes");
        }
        this.hashKey = new SecretKeySpec(hashBytes, "HmacSHA256");
    }

    @Override
    public String hashForLookup(String identityKey) {
        if (identityKey == null || identityKey.isBlank()) {
            throw new IllegalArgumentException("identityKey는 필수입니다.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hashKey);
            byte[] digest = mac.doFinal(identityKey.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("CI 해시 생성에 실패했습니다.", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}

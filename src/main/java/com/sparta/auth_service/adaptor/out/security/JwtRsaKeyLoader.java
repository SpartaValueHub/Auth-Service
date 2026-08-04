package com.sparta.auth_service.adaptor.out.security;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** JWT RS256 Private Key PEM 로드 — file/env, Git·로그에 키 본문 금지 */
@Component
public class JwtRsaKeyLoader {

    private final ResourceLoader resourceLoader;

    public JwtRsaKeyLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public PrivateKey loadPrivateKey(JwtProperties properties) {
        String pem = resolvePem(properties.getPrivateKey(), properties.getPrivateKeyLocation());
        return parsePrivateKey(pem);
    }

    public PublicKey loadPublicKeyFromPrivate(PrivateKey privateKey) {
        if (!(privateKey instanceof java.security.interfaces.RSAPrivateCrtKey rsaPrivateKey)) {
            throw new IllegalStateException("RSA private key is required");
        }
        try {
            java.security.spec.RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent()
            );
            return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to derive RSA public key", ex);
        }
    }

    private String resolvePem(String inlinePem, String location) {
        if (StringUtils.hasText(inlinePem)) {
            return inlinePem;
        }
        if (!StringUtils.hasText(location)) {
            throw new IllegalStateException("jwt.private-key or jwt.private-key-location must be configured");
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load JWT key from " + location, ex);
        }
    }

    static PrivateKey parsePrivateKey(String pem) {
        try {
            String sanitized = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid JWT private key PEM", ex);
        }
    }

    static PublicKey parsePublicKey(String pem) {
        try {
            String sanitized = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid JWT public key PEM", ex);
        }
    }
}

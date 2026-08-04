package com.sparta.auth_service.test.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class TestJwtKeyFixtures {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestJwtKeyFixtures() {
    }

    public static String privateKeyPem() {
        return toPrivatePem(KEY_PAIR);
    }

    public static String publicKeyPem() {
        return toPublicPem(KEY_PAIR);
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate test RSA key pair", ex);
        }
    }

    private static String toPrivatePem(KeyPair keyPair) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----";
    }

    private static String toPublicPem(KeyPair keyPair) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----";
    }
}

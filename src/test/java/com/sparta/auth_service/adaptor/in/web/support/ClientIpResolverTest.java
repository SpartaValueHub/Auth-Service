package com.sparta.auth_service.adaptor.in.web.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void resolveUsesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveNormalizesIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        assertThat(resolver.resolve(request)).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    void normalizeIpLowercasesExpandedIpv6() {
        assertThat(ClientIpResolver.normalizeIp("2001:0DB8::1")).isEqualTo("2001:db8:0:0:0:0:0:1");
    }

    @Test
    void normalizeIpAcceptsIpv4() {
        assertThat(ClientIpResolver.normalizeIp("  203.0.113.10  ")).isEqualTo("203.0.113.10");
    }

    @Test
    void normalizeIpRejectsHostnameWithoutDnsLookup() {
        assertThat(ClientIpResolver.normalizeIp("evil.example.com")).isEmpty();
    }

    @Test
    void normalizeIpRejectsInvalidLiteral() {
        assertThat(ClientIpResolver.normalizeIp("999.999.999.999")).isEmpty();
        assertThat(ClientIpResolver.normalizeIp("not-an-ip")).isEmpty();
    }

    @Test
    void resolveReturnsEmptyWhenRemoteAddrMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("");

        assertThat(resolver.resolve(request)).isEmpty();
    }
}

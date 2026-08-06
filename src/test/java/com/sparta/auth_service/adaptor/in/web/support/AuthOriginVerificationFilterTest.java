package com.sparta.auth_service.adaptor.in.web.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sparta.auth_service.adaptor.in.web.config.AuthOriginProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthOriginVerificationFilterTest {

    @Mock
    private FilterChain filterChain;

    private AuthOriginVerificationFilter filter;

    @BeforeEach
    void setUp() {
        AuthOriginProperties properties = new AuthOriginProperties("http://localhost:3000", true);
        filter = new AuthOriginVerificationFilter(properties, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void refresh_allowsConfiguredOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.addHeader(HttpHeaders.ORIGIN, "HTTP://LOCALHOST:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void refresh_rejectsDisallowedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.addHeader(HttpHeaders.ORIGIN, "http://evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("AUTH_FORBIDDEN_ORIGIN");
        assertThat(response.getContentAsString()).doesNotContain("evil.example.com");
    }

    @Test
    void refresh_rejectsMissingOriginWhenRequired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void refresh_allowsMissingOriginWhenNotRequired() throws Exception {
        AuthOriginProperties properties = new AuthOriginProperties("http://localhost:3000", false);
        AuthOriginVerificationFilter relaxedFilter = new AuthOriginVerificationFilter(
                properties,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        relaxedFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void signIn_isNotFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/sign-in");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void refreshEvilPath_isNotFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh-evil");
        request.addHeader(HttpHeaders.ORIGIN, "http://evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void refreshWithTrailingSlash_isFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh/");
        request.setServletPath("/api/v1/auth/refresh/");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(AuthOriginVerificationFilter.isProtectedPath(request)).isTrue();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void refreshWithContextPath_isFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth-service/api/v1/auth/refresh");
        request.setContextPath("/auth-service");
        request.setServletPath("/api/v1/auth/refresh");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(AuthOriginVerificationFilter.isProtectedPath(request)).isTrue();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void getRequest_isNotFiltered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}

package com.sparta.auth_service.adaptor.in.web.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.auth_service.adaptor.in.web.config.AuthOriginProperties;
import com.sparta.auth_service.adaptor.in.web.vo.ErrorResponseVo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * refresh·logout Cookie 기반 엔드포인트 Origin 검증.
 * CSRF double-submit 대신 Origin allowlist로 cross-site 요청을 차단한다.
 */
@Component
@RequiredArgsConstructor
public class AuthOriginVerificationFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    private final AuthOriginProperties authOriginProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        return !isProtectedPath(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (!authOriginProperties.isAllowed(origin)) {
            writeForbiddenOrigin(response, request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    static boolean isProtectedPath(HttpServletRequest request) {
        return PROTECTED_PATHS.contains(normalizeRequestPath(request));
    }

    static String normalizeRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String path = (servletPath != null ? servletPath : "")
                + (pathInfo != null ? pathInfo : "");

        if (!StringUtils.hasText(path)) {
            path = stripContextPath(request.getRequestURI(), request.getContextPath());
        }

        return stripTrailingSlash(path);
    }

    private static String stripContextPath(String requestUri, String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return requestUri;
        }
        if (requestUri != null && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri != null ? requestUri : "";
    }

    private static String stripTrailingSlash(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return path != null ? path : "";
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void writeForbiddenOrigin(HttpServletResponse response, String path) throws IOException {
        ErrorResponseVo body = ErrorResponseVo.builder()
                .timestamp(Instant.now())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .code("AUTH_FORBIDDEN_ORIGIN")
                .message("허용되지 않은 Origin입니다.")
                .path(path)
                .build();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

package com.orang.apigateway.config;

import com.orang.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitingConfigTest {

    private RateLimitingConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimitingConfig();
    }

    @Test
    @DisplayName("jwtUtils bean is created from secret")
    void jwtUtilsBeanCreated() {
        JwtUtils jwtUtils = config.jwtUtils("my-test-secret-key-for-jwt-signing-123456");
        assertThat(jwtUtils).isNotNull();
    }

    @Test
    @DisplayName("ipKeyResolver returns unknown when remote address missing")
    void ipKeyResolverReturnsUnknownWithoutRemoteAddress() {
        KeyResolver resolver = config.ipKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("unknown");
    }

    @Test
    @DisplayName("ipKeyResolver resolves host address")
    void ipKeyResolverResolvesHostAddress() throws Exception {
        KeyResolver resolver = config.ipKeyResolver();
        InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8080);
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .remoteAddress(remote)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("userKeyResolver uses jwt user id when bearer token is valid")
    void userKeyResolverUsesJwtUserId() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        String userId = UUID.randomUUID().toString();
        when(jwtUtils.extractUserId("valid-token")).thenReturn(Optional.of(userId));
        KeyResolver resolver = config.userKeyResolver(jwtUtils);

        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo(userId);
    }

    @Test
    @DisplayName("userKeyResolver falls back to anonymous when no auth and no remote")
    void userKeyResolverFallsBackToAnonymous() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        KeyResolver resolver = config.userKeyResolver(jwtUtils);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("userKeyResolver falls back to anon-host when token is invalid")
    void userKeyResolverFallsBackToAnonHostForInvalidToken() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.extractUserId("bad-token")).thenReturn(Optional.empty());
        KeyResolver resolver = config.userKeyResolver(jwtUtils);
        InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName("10.10.0.7"), 8080);

        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                .remoteAddress(remote)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("anon-10.10.0.7");
    }
}

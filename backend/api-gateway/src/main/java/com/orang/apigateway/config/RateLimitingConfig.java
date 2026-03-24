package com.orang.apigateway.config;

import com.orang.shared.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitingConfig {

    @Bean
    public JwtUtils jwtUtils(@Value("${jwt.secret}") String secretKey) {
        return new JwtUtils(secretKey);
    }

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

            if (remoteAddress == null) {
                return Mono.just("unknown");
            }

            String host;
            if (remoteAddress.getAddress() != null) {
                host = remoteAddress.getAddress().getHostAddress();
            } else {
                host = remoteAddress.getHostString();
            }

            return Mono.just(host);
        };
    }

    @Bean
    public KeyResolver userKeyResolver(JwtUtils jwtUtils) {
        return exchange -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                return jwtUtils.extractUserId(token)
                        .map(Mono::just)
                        .orElseGet(() -> {
                            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                            if (remoteAddress != null) {
                                String host;
                                if (remoteAddress.getAddress() != null) {
                                    host = remoteAddress.getAddress().getHostAddress();
                                } else {
                                    host = remoteAddress.getHostString();
                                }
                                return Mono.just("anon-" + host);
                            }
                            return Mono.just("anonymous");
                        });
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null) {
                String host;
                if (remoteAddress.getAddress() != null) {
                    host = remoteAddress.getAddress().getHostAddress();
                } else {
                    host = remoteAddress.getHostString();
                }
                return Mono.just("anon-" + host);
            }

            return Mono.just("anonymous");
        };
    }
}

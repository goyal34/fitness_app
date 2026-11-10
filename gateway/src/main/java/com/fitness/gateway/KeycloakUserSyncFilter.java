package com.fitness.gateway;

import com.fitness.gateway.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Runs on every routed request, after Spring Security has verified the JWT.
 *
 * On a user's first request their Keycloak account is copied into the User
 * Service. The services behind the gateway identify the caller from the token
 * itself, so any X-User-ID header the client sends is simply dropped.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements GlobalFilter, Ordered {

    private static final String LEGACY_USER_ID_HEADER = "X-User-ID";

    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(LEGACY_USER_ID_HEADER))
                .build();
        ServerWebExchange cleaned = exchange.mutate().request(request).build();

        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(principal -> ((JwtAuthenticationToken) principal).getToken())
                .flatMap(this::syncUser)
                .then(chain.filter(cleaned));
    }

    private Mono<Void> syncUser(Jwt jwt) {
        String userId = jwt.getSubject();
        String token = jwt.getTokenValue();
        return userService.validateUser(userId, token)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.empty();
                    }
                    log.info("First request from Keycloak user {}, creating local profile.", userId);
                    return userService.registerUser(userId, token).then();
                });
    }

    @Override
    public int getOrder() {
        // Before the routing filters, so the cleaned request is what gets forwarded.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

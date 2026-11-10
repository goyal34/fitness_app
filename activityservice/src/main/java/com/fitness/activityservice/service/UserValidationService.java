package com.fitness.activityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserValidationService {
    private final WebClient userServiceWebClient;

    /**
     * Checks that the caller has a profile in the User Service. The caller's own
     * token is passed along, because the User Service checks it on every request.
     */
    public boolean validateUser(String userId) {
        log.info("Calling User Validation API for userId: {}", userId);
        try {
            Boolean exists = userServiceWebClient.get()
                    .uri("/api/users/{userId}/validate", userId)
                    .headers(h -> h.setBearerAuth(currentToken()))
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            return Boolean.TRUE.equals(exists);
        } catch (WebClientResponseException e) {
            log.warn("User validation failed for {}: {}", userId, e.getStatusCode());
            return false;
        }
    }

    private String currentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user token on this request");
    }
}

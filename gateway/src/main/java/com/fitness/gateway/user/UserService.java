package com.fitness.gateway.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Calls the User Service on behalf of the logged-in user. The user's own token
 * is passed along, because the User Service checks it on every request.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final WebClient userServiceWebClient;

    public Mono<Boolean> validateUser(String userId, String token) {
        log.info("Calling User Validation API for userId: {}", userId);
        return userServiceWebClient.get()
                .uri("/api/users/{userId}/validate", userId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorMap(WebClientResponseException.class,
                        e -> new RuntimeException("User validation failed (" + e.getStatusCode() + ") for " + userId, e));
    }

    public Mono<UserResponse> registerUser(String userId, String token) {
        log.info("Calling User Registration API for userId: {}", userId);
        return userServiceWebClient.post()
                .uri("/api/users/register")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserResponse.class)
                .onErrorMap(WebClientResponseException.class,
                        e -> new RuntimeException("User registration failed (" + e.getStatusCode() + "): "
                                + e.getResponseBodyAsString(), e));
    }
}

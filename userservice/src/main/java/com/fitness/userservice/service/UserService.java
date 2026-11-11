package com.fitness.userservice.service;

import org.springframework.security.oauth2.jwt.Jwt;
import com.fitness.common.dto.UserResponse;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    /**
     * Creates the local profile for a Keycloak user, or updates it if it exists.
     *
     * If a profile with the same email exists but is linked to a different
     * Keycloak account (for example the Keycloak user was deleted and recreated),
     * it is re-linked only when Keycloak says the email is verified. Otherwise
     * anyone could claim another person's profile by signing up with their email.
     */
    public UserResponse register(Jwt token) {
        String keycloakId = token.getSubject();
        String email = token.getClaimAsString("email");
        boolean emailVerified = Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"));

        User user = repository.findByKeycloakId(keycloakId).orElse(null);

        if (user == null) {
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Your Keycloak account has no email address.");
            }
            user = repository.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setEmail(email);
            } else if (!emailVerified) {
                log.warn("Refusing to link Keycloak user {} to existing profile {}: email not verified.",
                        keycloakId, user.getId());
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A profile with this email already exists. Verify your email to link it.");
            }
            user.setKeycloakId(keycloakId);
        }

        String firstName = token.getClaimAsString("given_name");
        String lastName = token.getClaimAsString("family_name");
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

        return mapToResponse(repository.save(user));
    }

    /**
     * Returns a profile, but only to its owner.
     *
     * @param userId     the profile's database id
     * @param callerId   the caller's Keycloak id (the "sub" of their verified token)
     */
    public UserResponse getUserProfile(String userId, String callerId) {
        User user = repository.findById(userId)
                .filter(u -> u.getKeycloakId() != null && u.getKeycloakId().equals(callerId))
                // Same answer for "missing" and "not yours", so ids can't be probed.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToResponse(user);
    }

    public UserResponse getProfileByKeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Boolean existByUserId(String userId) {
        log.info("Calling User Validation API for userId: {}", userId);
        return repository.existsByKeycloakId(userId);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setKeycloakId(user.getKeycloakId());
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());
        return userResponse;
    }
}

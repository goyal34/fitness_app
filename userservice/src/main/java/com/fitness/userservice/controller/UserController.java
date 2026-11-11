package com.fitness.userservice.controller;

import com.fitness.common.dto.UserResponse;
import com.fitness.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** The caller is always the "sub" of their verified token. */
@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    /** The logged-in user's own profile. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getProfileByKeycloakId(jwt.getSubject()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUserProfile(userId, jwt.getSubject()));
    }

    /**
     * Creates or updates the caller's own profile from their token's claims.
     * Called by the API Gateway on first login (blocked for browsers at the gateway).
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.register(jwt));
    }

    /** Whether the caller's own profile exists. Used by the gateway and the Activity Service. */
    @GetMapping("/{userId}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable String userId,
                                                @AuthenticationPrincipal Jwt jwt) {
        if (!userId.equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only check your own account");
        }
        return ResponseEntity.ok(userService.existByUserId(userId));
    }
}

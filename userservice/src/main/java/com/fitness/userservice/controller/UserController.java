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

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getProfileByKeycloakId(jwt.getSubject()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUserProfile(userId, jwt.getSubject()));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.register(jwt));
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable String userId,
                                                @AuthenticationPrincipal Jwt jwt) {
        if (!userId.equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only check your own account");
        }
        return ResponseEntity.ok(userService.existByUserId(userId));
    }
}

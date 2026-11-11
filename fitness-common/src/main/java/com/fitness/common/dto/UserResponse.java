package com.fitness.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * The profile shape returned by the User Service and consumed by the gateway.
 * Kept in one place so the two cannot drift apart.
 */
@Data
public class UserResponse {
    private String id;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

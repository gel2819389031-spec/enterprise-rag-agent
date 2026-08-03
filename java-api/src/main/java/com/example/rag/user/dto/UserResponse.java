package com.example.rag.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;

    private Long tenantId;

    private String username;

    private String displayName;

    private String email;

    private String roleCode;

    private Integer status;

    private Instant lastLoginAt;

    private Instant createdAt;

    private Instant updatedAt;
}
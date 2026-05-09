package com.realestate.backend.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Permission — gjeneron tabelën public.permissions
 * Çdo permission = HTTP Method + API Path
 * P.sh: POST + /api/properties
 */
@Entity
@Table(
        name   = "permissions",
        schema = "public",
        indexes = {
                @Index(name = "idx_permissions_method", columnList = "http_method"),
                @Index(name = "idx_permissions_path",   columnList = "api_path")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // HTTP verb: GET, POST, PUT, DELETE, PATCH
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    // API path me wildcard: /api/properties, /api/properties/*
    @Column(name = "api_path", nullable = false, length = 255)
    private String apiPath;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
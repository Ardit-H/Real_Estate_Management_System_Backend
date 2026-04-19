package com.realestate.backend.entity.profile;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agent_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK cross-schema: public.users — UNIQUE (1 profil per user)
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String license;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(length = 100)
    private String specialization;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;
}

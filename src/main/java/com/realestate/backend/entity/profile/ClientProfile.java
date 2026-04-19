package com.realestate.backend.entity.profile;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "client_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK cross-schema: public.users — UNIQUE (1 profil per user)
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(length = 30)
    private String phone;

    // EMAIL | PHONE | WHATSAPP
    @Column(name = "preferred_contact", length = 20)
    @Builder.Default
    private String preferredContact = "EMAIL";

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "preferred_type", length = 50)
    private String preferredType;

    @Column(name = "preferred_city", length = 100)
    private String preferredCity;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;
}

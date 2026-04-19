package com.realestate.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserProfileDtos {

    // ── USER RESPONSE ─────────────────────────────────────────

    public record UserResponse(
            Long id,
            String email,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name")  String lastName,
            String role,
            @JsonProperty("tenant_id")  Long tenantId,
            @JsonProperty("is_active")  Boolean isActive,
            @JsonProperty("created_at") LocalDateTime createdAt
    ) {}

    public record UserUpdateRequest(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name")  String lastName,
            @Email String email
    ) {}

    public record ChangePasswordRequest(
            @NotBlank
            @JsonProperty("current_password")
            String currentPassword,

            @NotBlank
            @Size(min = 8)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "Password duhet të përmbajë shkronja dhe numra")
            @JsonProperty("new_password")
            String newPassword
    ) {}

    // ── AGENT PROFILE ─────────────────────────────────────────

    public record AgentProfileRequest(
            String phone,
            String license,
            String bio,
            @JsonProperty("experience_years") Integer experienceYears,
            String specialization,
            @JsonProperty("photo_url") String photoUrl
    ) {}

    public record AgentProfileResponse(
            Long id,
            @JsonProperty("user_id")          Long userId,
            String phone,
            String license,
            String bio,
            @JsonProperty("experience_years")  Integer experienceYears,
            String specialization,
            @JsonProperty("photo_url")         String photoUrl,
            BigDecimal rating,
            @JsonProperty("total_reviews")     Integer totalReviews
    ) {}

    // ── CLIENT PROFILE ────────────────────────────────────────

    public record ClientProfileRequest(
            String phone,
            @Schema(allowableValues = {"EMAIL","PHONE","WHATSAPP"})
            @JsonProperty("preferred_contact") String preferredContact,
            @JsonProperty("budget_min")        BigDecimal budgetMin,
            @JsonProperty("budget_max")        BigDecimal budgetMax,
            @JsonProperty("preferred_type")    String preferredType,
            @JsonProperty("preferred_city")    String preferredCity,
            @JsonProperty("photo_url")         String photoUrl
    ) {}

    public record ClientProfileResponse(
            Long id,
            @JsonProperty("user_id")           Long userId,
            String phone,
            @JsonProperty("preferred_contact") String preferredContact,
            @JsonProperty("budget_min")        BigDecimal budgetMin,
            @JsonProperty("budget_max")        BigDecimal budgetMax,
            @JsonProperty("preferred_type")    String preferredType,
            @JsonProperty("preferred_city")    String preferredCity,
            @JsonProperty("photo_url")         String photoUrl
    ) {}

    // ── USER STATUS (ADMIN) ───────────────────────────────────

    public record UserStatusRequest(
            @NotNull
            @JsonProperty("is_active")
            Boolean isActive
    ) {}

    public record UserRoleRequest(
            @NotNull
            @Schema(allowableValues = {"ADMIN","AGENT","CLIENT"})
            Role role
    ) {}
}

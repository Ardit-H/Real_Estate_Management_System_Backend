package com.realestate.backend.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_roles", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @EmbeddedId
    private UserRolePK id;

    public UserRole(Long userId, Long roleId) {
        this.id = new UserRolePK(userId, roleId);
    }
}
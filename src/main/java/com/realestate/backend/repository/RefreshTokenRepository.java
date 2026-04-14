package com.realestate.backend.repository;

import com.realestate.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // ----------------------------------------------------
    // REVOKE ALL USER TOKENS
    // ----------------------------------------------------
    @Modifying
    @Transactional
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revoked = true
        WHERE rt.user.id = :userId
    """)
    void revokeAllForUser(@Param("userId") Long userId);

    // ----------------------------------------------------
    // DELETE EXPIRED + REVOKED TOKENS
    // ----------------------------------------------------
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM RefreshToken rt
        WHERE rt.expiresAt < :now
           OR rt.revoked = true
    """)
    int deleteExpiredAndRevoked(@Param("now") LocalDateTime now);

    // ----------------------------------------------------
    // VALID TOKEN CHECK
    // ----------------------------------------------------
    @Query("""
        SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END
        FROM RefreshToken rt
        WHERE rt.token = :token
          AND rt.revoked = false
          AND rt.expiresAt > :now
    """)
    boolean existsValidToken(
            @Param("token") String token,
            @Param("now") LocalDateTime now
    );
}
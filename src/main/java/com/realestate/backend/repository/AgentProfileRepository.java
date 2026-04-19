package com.realestate.backend.repository;

import com.realestate.backend.entity.profile.AgentProfile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    Optional<AgentProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // Agjentët sipas vlerësimit
    List<AgentProfile> findAllByOrderByRatingDesc();

    // Agjentët me vlerësim mbi threshold
    List<AgentProfile> findByRatingGreaterThanEqual(BigDecimal minRating);

    // Agjentët sipas specializimit
    List<AgentProfile> findBySpecializationContainingIgnoreCase(String specialization);

    // Ndrysho vlerësimin mesatar
    @Modifying
    @Query("""
        UPDATE AgentProfile ap
        SET ap.rating       = :rating,
            ap.totalReviews = ap.totalReviews + 1
        WHERE ap.userId = :userId
    """)
    void updateRating(@Param("userId") Long userId,
                      @Param("rating") BigDecimal rating);
}

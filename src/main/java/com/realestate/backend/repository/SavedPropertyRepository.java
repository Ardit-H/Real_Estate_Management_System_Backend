package com.realestate.backend.repository;

import com.realestate.backend.entity.property.SavedProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Long> {

    // Të gjitha pronat e ruajtura nga një user
    Page<SavedProperty> findByUserIdOrderBySavedAtDesc(Long userId, Pageable pageable);

    // A e ka ruajtur ky user këtë pronë?
    boolean existsByUserIdAndProperty_Id(Long userId, Long propertyId);

    // Gjej saved entry specifik
    Optional<SavedProperty> findByUserIdAndProperty_Id(Long userId, Long propertyId);

    // Fshi me userId + propertyId
    @Modifying
    @Query("""
        DELETE FROM SavedProperty s
        WHERE s.userId = :userId
          AND s.property.id = :propertyId
    """)
    void deleteByUserIdAndPropertyId(
            @Param("userId") Long userId,
            @Param("propertyId") Long propertyId
    );

    // Sa prona ka ruajtur ky user
    long countByUserId(Long userId);
}
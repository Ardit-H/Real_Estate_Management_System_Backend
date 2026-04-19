package com.realestate.backend.repository;

import com.realestate.backend.entity.property.SavedProperty;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Long> {

    // Lista e wishlist të userit
    List<SavedProperty> findByUserIdOrderBySavedAtDesc(Long userId);

    // A e ka ruajtur ky user këtë pronë?
    boolean existsByUserIdAndProperty_Id(Long userId, Long propertyId);

    // Gjej një saved_property specifik
    Optional<SavedProperty> findByUserIdAndProperty_Id(Long userId, Long propertyId);

    // Fshij nga wishlist
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM SavedProperty sp
        WHERE sp.userId = :userId
          AND sp.property.id = :propertyId
    """)
    void deleteByUserIdAndPropertyId(
            @Param("userId") Long userId,
            @Param("propertyId") Long propertyId
    );

    // Sa herë është ruajtur një pronë (populariteti)
    long countByProperty_Id(Long propertyId);
}
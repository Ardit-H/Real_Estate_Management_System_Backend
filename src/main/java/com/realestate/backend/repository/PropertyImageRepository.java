package com.realestate.backend.repository;

import com.realestate.backend.entity.property.PropertyImage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findByProperty_IdOrderBySortOrderAsc(Long propertyId);

    @Modifying
    @Query("""
        UPDATE PropertyImage i
        SET i.isPrimary = false
        WHERE i.property.id = :propertyId
    """)
    void clearPrimaryForProperty(@Param("propertyId") Long propertyId);

    @Modifying
    @Query("""
        UPDATE PropertyImage i
        SET i.isPrimary = true
        WHERE i.id = :imageId
    """)
    void setPrimary(@Param("imageId") Long imageId);


    @Query("""
        SELECT MAX(i.sortOrder)
        FROM PropertyImage i
        WHERE i.property.id = :propertyId
    """)
    Optional<Integer> maxSortOrder(@Param("propertyId") Long propertyId);
}
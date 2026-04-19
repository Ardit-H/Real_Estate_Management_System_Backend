package com.realestate.backend.repository;

import com.realestate.backend.entity.property.PropertyView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PropertyViewRepository extends JpaRepository<PropertyView, Long> {

    // Sa herë është parë një pronë
    long countByProperty_Id(Long propertyId);

    // Shikimet e fundit 30 ditëve
    @Query("""
        SELECT pv FROM PropertyView pv
        WHERE pv.property.id = :propertyId
          AND pv.viewedAt >= :from
        ORDER BY pv.viewedAt DESC
    """)
    List<PropertyView> findRecentViews(
            @Param("propertyId") Long propertyId,
            @Param("from") LocalDateTime from
    );

    // Pronat më të shikuara (për dashboard)
    @Query(value = """
        SELECT property_id, COUNT(*) AS view_count
        FROM property_views
        WHERE viewed_at >= :from
        GROUP BY property_id
        ORDER BY view_count DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findMostViewed(
            @Param("from") LocalDateTime from,
            @Param("limit") int limit
    );

    // A e ka parë ky user këtë pronë sot?
    @Query("""
        SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END
        FROM PropertyView pv
        WHERE pv.property.id = :propertyId
          AND pv.userId = :userId
          AND pv.viewedAt >= :from
    """)
    boolean existsViewToday(
            @Param("propertyId") Long propertyId,
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from
    );
}
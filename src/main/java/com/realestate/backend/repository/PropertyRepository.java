package com.realestate.backend.repository;


import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.PropertyType;
import com.realestate.backend.entity.property.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PropertyRepository
 *
 * JpaSpecificationExecutor → filtrim dinamik (kërkes 20: Search & Filtering)
 * Native queries → Full-Text Search me PostgreSQL tsvector
 */
@Repository
public interface PropertyRepository
        extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    // ── Gjej vetëm pronat aktive (soft delete) ────────────────────
    Optional<Property> findByIdAndDeletedAtIsNull(Long id);

    Page<Property> findAllByDeletedAtIsNull(Pageable pageable);

    // ── Pronat e featured ─────────────────────────────────────────
    List<Property> findByIsFeaturedTrueAndDeletedAtIsNull();

    // ── Filtrim sipas statusit ────────────────────────────────────
    Page<Property> findByStatusAndDeletedAtIsNull(
            PropertyStatus status, Pageable pageable);

    // ── Filtrim sipas agent ───────────────────────────────────────
    Page<Property> findByAgentIdAndDeletedAtIsNull(
            Long agentId, Pageable pageable);

    // ── Full-Text Search me PostgreSQL tsvector ───────────────────
    // Kjo është native query — search_vector është GENERATED STORED
    @Query(
            value = """
            SELECT * FROM properties
            WHERE search_vector @@ plainto_tsquery('english', :keyword)
              AND deleted_at IS NULL
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :keyword)) DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM properties
            WHERE search_vector @@ plainto_tsquery('english', :keyword)
              AND deleted_at IS NULL
            """,
            nativeQuery = true
    )
    Page<Property> fullTextSearch(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ── Filtrim i avancuar me çmim + dhoma + qytet ─────────────────
    @Query("""
        SELECT p FROM Property p
        LEFT JOIN p.address a
        WHERE (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:bedrooms IS NULL OR p.bedrooms >= :bedrooms)
          AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:type IS NULL OR p.type = :type)
          AND (:listingType IS NULL OR p.listingType = :listingType)
          AND (:status IS NULL OR p.status = :status)
          AND p.deletedAt IS NULL
    """)
    Page<Property> filterProperties(
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("bedrooms")   Integer bedrooms,
            @Param("city")       String city,
            @Param("type")       PropertyType type,
            @Param("listingType") ListingType listingType,
            @Param("status")     PropertyStatus status,
            Pageable pageable
    );

    // ── Statistika për dashboard ──────────────────────────────────
    @Query("""
        SELECT COUNT(p) FROM Property p
        WHERE p.status = :status
          AND p.deletedAt IS NULL
    """)
    Long countByStatus(@Param("status") PropertyStatus status);

    @Query("""
        SELECT COUNT(p) FROM Property p
        WHERE p.agentId = :agentId
          AND p.deletedAt IS NULL
    """)
    Long countByAgent(@Param("agentId") Long agentId);

    // ── Ndrysho statusin direkt (pa load entity) ─────────────────
    @Modifying
    @Query("""
        UPDATE Property p
        SET p.status = :status, p.updatedAt = CURRENT_TIMESTAMP
        WHERE p.id = :id
    """)
    void updateStatus(@Param("id") Long id,
                      @Param("status") PropertyStatus status);

    // ── Incremento view count ─────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE Property p
        SET p.viewCount = p.viewCount + 1
        WHERE p.id = :id
    """)
    void incrementViewCount(@Param("id") Long id);

    // ── Soft delete ───────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE Property p
        SET p.deletedAt = CURRENT_TIMESTAMP
        WHERE p.id = :id
    """)
    void softDelete(@Param("id") Long id);
}

package com.realestate.backend.repository;

import com.realestate.backend.entity.property.PropertyPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyPriceHistoryRepository
        extends JpaRepository<PropertyPriceHistory, Long> {

    List<PropertyPriceHistory> findByPropertyIdOrderByChangedAtDesc(Long propertyId);
}

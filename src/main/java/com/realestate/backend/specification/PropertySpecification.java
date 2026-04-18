package com.realestate.backend.specification;


import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.PropertyType;
import com.realestate.backend.entity.property.Address;
import com.realestate.backend.entity.property.Property;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class PropertySpecification {

    private PropertySpecification() {}


    public record PropertyFilter(
            BigDecimal    minPrice,
            BigDecimal    maxPrice,
            Integer       minBedrooms,
            Integer       maxBedrooms,
            Integer       minBathrooms,
            BigDecimal    minArea,
            BigDecimal    maxArea,
            String        city,
            String        country,
            PropertyType  type,
            ListingType   listingType,
            PropertyStatus status,
            Boolean       isFeatured,
            Integer       minYearBuilt,
            Integer       maxYearBuilt,
            String        currency
    ) {}


    public static Specification<Property> build(PropertyFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();


            predicates.add(cb.isNull(root.get("deletedAt")));


            if (f.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.maxPrice()));
            }


            if (f.minBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), f.minBedrooms()));
            }
            if (f.maxBedrooms() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bedrooms"), f.maxBedrooms()));
            }


            if (f.minBathrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bathrooms"), f.minBathrooms()));
            }


            if (f.minArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("areaSqm"), f.minArea()));
            }
            if (f.maxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("areaSqm"), f.maxArea()));
            }


            if (f.city() != null && !f.city().isBlank()) {
                Join<Property, Address> addressJoin = root.join("address", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(addressJoin.get("city")),
                        "%" + f.city().toLowerCase() + "%"
                ));
            }
            if (f.country() != null && !f.country().isBlank()) {
                Join<Property, Address> addressJoin = root.join("address", JoinType.LEFT);
                predicates.add(cb.like(
                        cb.lower(addressJoin.get("country")),
                        "%" + f.country().toLowerCase() + "%"
                ));
            }


            if (f.type() != null) {
                predicates.add(cb.equal(root.get("type"), f.type()));
            }
            if (f.listingType() != null) {
                predicates.add(cb.equal(root.get("listingType"), f.listingType()));
            }
            if (f.status() != null) {
                predicates.add(cb.equal(root.get("status"), f.status()));
            }


            if (f.isFeatured() != null) {
                predicates.add(cb.equal(root.get("isFeatured"), f.isFeatured()));
            }


            if (f.minYearBuilt() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearBuilt"), f.minYearBuilt()));
            }
            if (f.maxYearBuilt() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("yearBuilt"), f.maxYearBuilt()));
            }


            if (f.currency() != null && !f.currency().isBlank()) {
                predicates.add(cb.equal(root.get("currency"), f.currency().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

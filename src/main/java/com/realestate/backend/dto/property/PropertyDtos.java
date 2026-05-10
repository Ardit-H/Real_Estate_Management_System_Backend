package com.realestate.backend.dto.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.io.Serializable;


public class PropertyDtos {


    public record AddressRequest(
            String street,
            @NotBlank(message = "Qyteti është i detyrueshëm")
            String city,
            String state,
            String country,
            @JsonProperty("zip_code")
            String zipCode,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record AddressResponse(
            Long id,
            String street,
            String city,
            String state,
            String country,
            @JsonProperty("zip_code")  String zipCode,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}



    public record PropertyCreateRequest(

            @NotBlank(message = "Titulli është i detyrueshëm")
            @Size(max = 255)
            String title,

            String description,

            @NotNull(message = "Tipi i pronës është i detyrueshëm")
            @Schema(allowableValues = {"APARTMENT","HOUSE","VILLA","COMMERCIAL","LAND","OFFICE"})
            PropertyType type,

            @JsonProperty("listing_type")
            @Schema(allowableValues = {"SALE","RENT","BOTH"})
            ListingType listingType,

            @Min(0) Integer bedrooms,
            @Min(0) Integer bathrooms,

            @JsonProperty("area_sqm")
            @DecimalMin("0") BigDecimal areaSqm,

            Integer floor,

            @JsonProperty("total_floors")
            Integer totalFloors,

            @JsonProperty("year_built")
            Integer yearBuilt,

            @NotNull(message = "Çmimi është i detyrueshëm")
            @DecimalMin("0")
            BigDecimal price,

            @Size(max = 10) String currency,

            @JsonProperty("price_per_sqm")
            BigDecimal pricePerSqm,

            @JsonProperty("is_featured")
            Boolean isFeatured,

            AddressRequest address,

            List<String> features
    ) {}

    public record PropertyUpdateRequest(
            String title,
            String description,
            PropertyType type,
            PropertyStatus status,
            ListingType listingType,
            Integer bedrooms,
            Integer bathrooms,
            @JsonProperty("area_sqm") BigDecimal areaSqm,
            Integer floor,
            @JsonProperty("total_floors") Integer totalFloors,
            @JsonProperty("year_built") Integer yearBuilt,
            @DecimalMin("0") BigDecimal price,
            String currency,
            @JsonProperty("price_per_sqm") BigDecimal pricePerSqm,
            @JsonProperty("is_featured") Boolean isFeatured,
            AddressRequest address,
            List<String> features
    ) {}



    public record PropertyStatusRequest(
            @NotNull(message = "Statusi është i detyrueshëm")
            @Schema(allowableValues = {"AVAILABLE","PENDING","SOLD","RENTED","INACTIVE"})
            PropertyStatus status
    ) {}



    public record PropertyImageResponse(
            Long id,
            @JsonProperty("image_url")   String imageUrl,
            String caption,
            @JsonProperty("sort_order")  Integer sortOrder,
            @JsonProperty("is_primary")  Boolean isPrimary
    ) {}



    public record PropertyResponse(
            Long id,
            String title,
            String description,
            PropertyType type,
            PropertyStatus status,
            @JsonProperty("listing_type") ListingType listingType,
            Integer bedrooms,
            Integer bathrooms,
            @JsonProperty("area_sqm")     BigDecimal areaSqm,
            Integer floor,
            @JsonProperty("total_floors") Integer totalFloors,
            @JsonProperty("year_built")   Integer yearBuilt,
            BigDecimal price,
            String currency,
            @JsonProperty("price_per_sqm") BigDecimal pricePerSqm,
            @JsonProperty("is_featured")   Boolean isFeatured,
            @JsonProperty("view_count")    Integer viewCount,
            AddressResponse address,
            List<PropertyImageResponse> images,
            List<String> features,
            @JsonProperty("agent_id")      Long agentId,
            @JsonProperty("created_at")    LocalDateTime createdAt,
            @JsonProperty("updated_at")    LocalDateTime updatedAt
    )implements Serializable {}



    public record PropertySummaryResponse(
            Long id,
            String title,
            PropertyType type,
            PropertyStatus status,
            @JsonProperty("listing_type") ListingType listingType,
            Integer bedrooms,
            Integer bathrooms,
            @JsonProperty("area_sqm")     BigDecimal areaSqm,
            BigDecimal price,
            String currency,
            @JsonProperty("is_featured")  Boolean isFeatured,
            @JsonProperty("view_count")   Integer viewCount,
            String city,
            String country,
            @JsonProperty("primary_image") String primaryImage,
            @JsonProperty("agent_id")      Long agentId,
            @JsonProperty("created_at")    LocalDateTime createdAt
    )implements Serializable {}



    public record PropertyFilterRequest(
            @JsonProperty("min_price")    BigDecimal minPrice,
            @JsonProperty("max_price")    BigDecimal maxPrice,
            @JsonProperty("min_bedrooms") Integer minBedrooms,
            @JsonProperty("max_bedrooms") Integer maxBedrooms,
            @JsonProperty("min_bathrooms") Integer minBathrooms,
            @JsonProperty("min_area")     BigDecimal minArea,
            @JsonProperty("max_area")     BigDecimal maxArea,
            String city,
            String country,
            PropertyType type,
            @JsonProperty("listing_type") ListingType listingType,
            PropertyStatus status,
            @JsonProperty("is_featured")  Boolean isFeatured,
            @JsonProperty("min_year")     Integer minYearBuilt,
            @JsonProperty("max_year")     Integer maxYearBuilt,
            String currency
    ) {}



    public record PriceHistoryResponse(
            Long id,
            @JsonProperty("old_price")   BigDecimal oldPrice,
            @JsonProperty("new_price")   BigDecimal newPrice,
            String currency,
            String reason,
            @JsonProperty("changed_at")  LocalDateTime changedAt
    ) {}
}

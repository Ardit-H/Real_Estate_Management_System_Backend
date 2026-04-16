package com.realestate.backend.entity.property;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "property_features",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "idx_features_unique",
                        columnNames = {"property_id", "feature"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // 'parking','pool','furnished','elevator','balcony',
    // 'garden','gym','security','air_conditioning','storage','fireplace'
    @Column(nullable = false, length = 100)
    private String feature;
}

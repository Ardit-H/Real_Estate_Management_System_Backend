package com.realestate.backend.service;

import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.property.PropertyImage;
import com.realestate.backend.entity.property.SavedProperty;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.repository.SavedPropertyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedPropertyService — Unit Tests")
class SavedPropertyServiceTest {

    @Mock SavedPropertyRepository savedRepo;
    @Mock PropertyRepository      propertyRepo;

    @InjectMocks SavedPropertyService service;

    private MockedStatic<TenantContext> tenantCtx;
    private static final Long USER_ID     = 10L;
    private static final Long PROPERTY_ID = 5L;

    @BeforeEach
    void mockTenant() {
        tenantCtx = mockStatic(TenantContext.class);
        tenantCtx.when(TenantContext::getUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void closeTenant() {
        tenantCtx.close();
    }

    // ── Helper builders ──────────────────────────────────────────

    private Property buildProperty(Long id) {
        Property p = Property.builder()
                .price(new BigDecimal("150000"))
                .currency("EUR")
                .build();
        try {
            var f = Property.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        return p;
    }

    private SavedProperty buildSaved(Long id, Property property, String note) {
        SavedProperty s = SavedProperty.builder()
                .userId(USER_ID)
                .property(property)
                .note(note)
                .build();
        try {
            var f = SavedProperty.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
            var at = SavedProperty.class.getDeclaredField("savedAt");
            at.setAccessible(true);
            at.set(s, LocalDateTime.now());
        } catch (Exception ignored) {}
        return s;
    }

    // ── getMySaved ───────────────────────────────────────────────

    @Nested
    @DisplayName("getMySaved()")
    class GetMySaved {

        @Test
        @DisplayName("kthen faqe me pronat e ruajtura")
        void returnsSavedPage() {
            Property p = buildProperty(PROPERTY_ID);
            SavedProperty sp = buildSaved(1L, p, "shënim");
            Pageable pageable = PageRequest.of(0, 10);

            when(savedRepo.findByUserIdOrderBySavedAtDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(sp)));

            Page<SavedPropertyService.SavedPropertyResponse> result =
                    service.getMySaved(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            SavedPropertyService.SavedPropertyResponse r = result.getContent().get(0);
            assertThat(r.propertyId()).isEqualTo(PROPERTY_ID);
            assertThat(r.note()).isEqualTo("shënim");
            assertThat(r.currency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("kthen faqe bosh kur nuk ka të ruajtura")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(savedRepo.findByUserIdOrderBySavedAtDesc(USER_ID, pageable))
                    .thenReturn(Page.empty());

            assertThat(service.getMySaved(pageable).getTotalElements()).isEqualTo(0);
        }
    }

    // ── save ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("sukses — ruan pronën dhe kthen response")
        void success() {
            Property p = buildProperty(PROPERTY_ID);
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(false);
            when(propertyRepo.findByIdAndDeletedAtIsNull(PROPERTY_ID))
                    .thenReturn(Optional.of(p));
            when(savedRepo.save(any(SavedProperty.class)))
                    .thenAnswer(inv -> {
                        SavedProperty s = inv.getArgument(0);
                        // simulate DB auto-set savedAt
                        try {
                            var at = SavedProperty.class.getDeclaredField("savedAt");
                            at.setAccessible(true);
                            at.set(s, LocalDateTime.now());
                        } catch (Exception ignored) {}
                        return s;
                    });

            SavedPropertyService.SavedPropertyResponse resp =
                    service.save(PROPERTY_ID, "noti im");

            assertThat(resp.propertyId()).isEqualTo(PROPERTY_ID);
            assertThat(resp.note()).isEqualTo("noti im");
            verify(savedRepo).save(any(SavedProperty.class));
        }

        @Test
        @DisplayName("hedh ConflictException kur prona është tashmë e ruajtur")
        void throws_whenAlreadySaved() {
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.save(PROPERTY_ID, null))
                    .isInstanceOf(ConflictException.class);

            verify(savedRepo, never()).save(any());
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur prona nuk ekziston")
        void throws_whenPropertyNotFound() {
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(false);
            when(propertyRepo.findByIdAndDeletedAtIsNull(PROPERTY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.save(PROPERTY_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PROPERTY_ID));
        }

        @Test
        @DisplayName("ruhet me note null")
        void savesWithNullNote() {
            Property p = buildProperty(PROPERTY_ID);
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(false);
            when(propertyRepo.findByIdAndDeletedAtIsNull(PROPERTY_ID))
                    .thenReturn(Optional.of(p));
            when(savedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SavedPropertyService.SavedPropertyResponse resp = service.save(PROPERTY_ID, null);
            assertThat(resp.note()).isNull();
        }
    }

    // ── unsave ───────────────────────────────────────────────────

    @Nested
    @DisplayName("unsave()")
    class Unsave {

        @Test
        @DisplayName("sukses — fshin pronën nga të ruajturat")
        void success() {
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(true);

            service.unsave(PROPERTY_ID);

            verify(savedRepo).deleteByUserIdAndPropertyId(USER_ID, PROPERTY_ID);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur prona nuk është e ruajtur")
        void throws_whenNotSaved() {
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> service.unsave(PROPERTY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(savedRepo, never()).deleteByUserIdAndPropertyId(anyLong(), anyLong());
        }
    }

    // ── isSaved ──────────────────────────────────────────────────

    @Nested
    @DisplayName("isSaved()")
    class IsSaved {

        @Test
        @DisplayName("kthen true kur prona është e ruajtur")
        void returnsTrue() {
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(true);
            assertThat(service.isSaved(PROPERTY_ID)).isTrue();
        }

        @Test
        @DisplayName("kthen false kur prona nuk është e ruajtur")
        void returnsFalse() {
            when(savedRepo.existsByUserIdAndProperty_Id(USER_ID, PROPERTY_ID))
                    .thenReturn(false);
            assertThat(service.isSaved(PROPERTY_ID)).isFalse();
        }
    }

    // ── toResponse mapper ────────────────────────────────────────

    @Nested
    @DisplayName("Mapper — toResponse()")
    class ToResponse {

        @Test
        @DisplayName("primaryImage merret nga imazhi me isPrimary=true")
        void primaryImage_fromIsPrimary() {
            Property p = buildProperty(PROPERTY_ID);
            PropertyImage notPrimary = PropertyImage.builder()
                    .imageUrl("/not-primary.jpg").isPrimary(false).build();
            PropertyImage primary = PropertyImage.builder()
                    .imageUrl("/primary.jpg").isPrimary(true).build();
            p.setImages(List.of(notPrimary, primary));

            SavedProperty sp = buildSaved(1L, p, null);
            Pageable pageable = PageRequest.of(0, 10);
            when(savedRepo.findByUserIdOrderBySavedAtDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(sp)));

            SavedPropertyService.SavedPropertyResponse r =
                    service.getMySaved(pageable).getContent().get(0);

            assertThat(r.primaryImage()).isEqualTo("/primary.jpg");
        }

        @Test
        @DisplayName("primaryImage merret nga i pari kur asnjë nuk është primary")
        void primaryImage_fallsBackToFirst() {
            Property p = buildProperty(PROPERTY_ID);
            PropertyImage img = PropertyImage.builder()
                    .imageUrl("/first.jpg").isPrimary(false).build();
            p.setImages(List.of(img));

            SavedProperty sp = buildSaved(1L, p, null);
            Pageable pageable = PageRequest.of(0, 10);
            when(savedRepo.findByUserIdOrderBySavedAtDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(sp)));

            SavedPropertyService.SavedPropertyResponse r =
                    service.getMySaved(pageable).getContent().get(0);

            assertThat(r.primaryImage()).isEqualTo("/first.jpg");
        }

        @Test
        @DisplayName("primaryImage është null kur nuk ka imazhe")
        void primaryImage_nullWhenNoImages() {
            Property p = buildProperty(PROPERTY_ID);
            // images list empty by default

            SavedProperty sp = buildSaved(1L, p, null);
            Pageable pageable = PageRequest.of(0, 10);
            when(savedRepo.findByUserIdOrderBySavedAtDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(sp)));

            SavedPropertyService.SavedPropertyResponse r =
                    service.getMySaved(pageable).getContent().get(0);

            assertThat(r.primaryImage()).isNull();
        }

        @Test
        @DisplayName("city dhe country janë null kur address mungon")
        void cityCountry_nullWhenNoAddress() {
            Property p = buildProperty(PROPERTY_ID);

            SavedProperty sp = buildSaved(1L, p, null);
            Pageable pageable = PageRequest.of(0, 10);
            when(savedRepo.findByUserIdOrderBySavedAtDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(sp)));

            SavedPropertyService.SavedPropertyResponse r =
                    service.getMySaved(pageable).getContent().get(0);

            assertThat(r.city()).isNull();
            assertThat(r.country()).isNull();
        }
    }
}
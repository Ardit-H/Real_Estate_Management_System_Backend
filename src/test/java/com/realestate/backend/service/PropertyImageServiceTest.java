package com.realestate.backend.service;

import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.property.PropertyImage;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.repository.PropertyImageRepository;
import com.realestate.backend.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyImageService — Unit Tests")
class PropertyImageServiceTest {

    @Mock PropertyRepository      propertyRepository;
    @Mock PropertyImageRepository imageRepository;
    @Mock ImageStorageService     storageService;
    @Mock MultipartFile           mockFile;

    @InjectMocks PropertyImageService service;

    private Property property;

    @BeforeEach
    void setUp() {
        property = Property.builder().build();
        // Reflection-set id since Lombok @Builder doesn't call setter
        try {
            var f = Property.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(property, 1L);
        } catch (Exception ignored) {
            // id may already be accessible or set via builder
        }
    }

    // ── uploadImage ──────────────────────────────────────────────

    @Nested
    @DisplayName("uploadImage()")
    class UploadImage {

        @Test
        @DisplayName("sukses — imazh jo-primar")
        void success_nonPrimary() throws IOException {
            when(propertyRepository.findByIdAndDeletedAtIsNull(1L))
                    .thenReturn(Optional.of(property));
            when(storageService.store(mockFile, 1L))
                    .thenReturn("/uploads/properties/1/abc.jpg");
            when(imageRepository.maxSortOrder(1L))
                    .thenReturn(Optional.of(2));

            PropertyImage saved = PropertyImage.builder()
                    .imageUrl("/uploads/properties/1/abc.jpg")
                    .sortOrder(3)
                    .isPrimary(false)
                    .build();
            when(imageRepository.save(any(PropertyImage.class))).thenReturn(saved);

            PropertyImage result = service.uploadImage(1L, mockFile, "caption", false);

            assertThat(result.getImageUrl()).isEqualTo("/uploads/properties/1/abc.jpg");
            assertThat(result.getIsPrimary()).isFalse();
            verify(imageRepository, never()).clearPrimaryForProperty(anyLong());
            verify(imageRepository).save(any(PropertyImage.class));
        }

        @Test
        @DisplayName("sukses — setPrimary=true pastron primary-n ekzistues")
        void success_setPrimary() throws IOException {
            when(propertyRepository.findByIdAndDeletedAtIsNull(1L))
                    .thenReturn(Optional.of(property));
            when(storageService.store(mockFile, 1L))
                    .thenReturn("/uploads/properties/1/primary.jpg");
            when(imageRepository.maxSortOrder(1L)).thenReturn(Optional.empty());

            PropertyImage saved = PropertyImage.builder()
                    .imageUrl("/uploads/properties/1/primary.jpg")
                    .sortOrder(0)
                    .isPrimary(true)
                    .build();
            when(imageRepository.save(any(PropertyImage.class))).thenReturn(saved);

            PropertyImage result = service.uploadImage(1L, mockFile, null, true);

            assertThat(result.getIsPrimary()).isTrue();
            verify(imageRepository).clearPrimaryForProperty(1L);
        }

        @Test
        @DisplayName("sortOrder fillon nga 0 kur nuk ka imazhe")
        void sortOrder_startsAtZero_whenNoImages() throws IOException {
            when(propertyRepository.findByIdAndDeletedAtIsNull(1L))
                    .thenReturn(Optional.of(property));
            when(storageService.store(mockFile, 1L)).thenReturn("/uploads/1/x.jpg");
            when(imageRepository.maxSortOrder(1L)).thenReturn(Optional.empty());
            when(imageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PropertyImage result = service.uploadImage(1L, mockFile, null, false);

            assertThat(result.getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur prona nuk ekziston")
        void throws_whenPropertyNotFound() {
            when(propertyRepository.findByIdAndDeletedAtIsNull(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.uploadImage(99L, mockFile, null, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("propagohet IOException nga storageService")
        void propagates_IOException() throws IOException {
            when(propertyRepository.findByIdAndDeletedAtIsNull(1L))
                    .thenReturn(Optional.of(property));
            when(storageService.store(mockFile, 1L)).thenThrow(new IOException("disk full"));

            assertThatThrownBy(() -> service.uploadImage(1L, mockFile, null, false))
                    .isInstanceOf(IOException.class)
                    .hasMessage("disk full");
        }
    }

    // ── getImages ────────────────────────────────────────────────

    @Nested
    @DisplayName("getImages()")
    class GetImages {

        @Test
        @DisplayName("kthen listën e imazheve të renditura")
        void returnsOrderedList() {
            PropertyImage img1 = PropertyImage.builder().sortOrder(0).build();
            PropertyImage img2 = PropertyImage.builder().sortOrder(1).build();
            when(imageRepository.findByProperty_IdOrderBySortOrderAsc(1L))
                    .thenReturn(List.of(img1, img2));

            List<PropertyImage> result = service.getImages(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSortOrder()).isEqualTo(0);
            assertThat(result.get(1).getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("kthen listë bosh kur nuk ka imazhe")
        void returnsEmptyList() {
            when(imageRepository.findByProperty_IdOrderBySortOrderAsc(1L))
                    .thenReturn(List.of());

            assertThat(service.getImages(1L)).isEmpty();
        }
    }

    // ── setPrimary ───────────────────────────────────────────────

    @Nested
    @DisplayName("setPrimary()")
    class SetPrimary {

        @Test
        @DisplayName("pastron primary-n e vjetër dhe vendos të riun")
        void clearsThenSets() {
            service.setPrimary(1L, 42L);

            verify(imageRepository).clearPrimaryForProperty(1L);
            verify(imageRepository).setPrimary(42L);
        }
    }

    // ── deleteImage ──────────────────────────────────────────────

    @Nested
    @DisplayName("deleteImage()")
    class DeleteImage {

        @Test
        @DisplayName("sukses — fshin nga storage dhe nga DB")
        void success() {
            PropertyImage img = PropertyImage.builder()
                    .imageUrl("/uploads/properties/1/old.jpg")
                    .build();
            when(imageRepository.findById(10L)).thenReturn(Optional.of(img));

            service.deleteImage(1L, 10L);

            verify(storageService).delete("/uploads/properties/1/old.jpg");
            verify(imageRepository).delete(img);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur imazhi nuk ekziston")
        void throws_whenImageNotFound() {
            when(imageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteImage(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(storageService, never()).delete(anyString());
            verify(imageRepository, never()).delete(any());
        }
    }
}

package com.realestate.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageStorageService — Unit Tests")
class ImageStorageServiceTest {

    @Mock MultipartFile mockFile;

    @TempDir Path tempDir;

    ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "maxSizeMb", 5L);
    }

    // ── store() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("store()")
    class Store {

        @Test
        @DisplayName("sukses — kthen URL me formatIN e saktë")
        void success_returnsCorrectUrl() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
            when(mockFile.getInputStream())
                    .thenReturn(new ByteArrayInputStream("fake-image-bytes".getBytes()));

            String url = service.store(mockFile, 1L);

            assertThat(url).startsWith("/uploads/properties/1/");
            assertThat(url).endsWith(".jpg");
            assertThat(url.split("/")).hasSize(5);
        }

        @Test
        @DisplayName("imazhi ruhet fizikisht në disk")
        void success_fileExistsOnDisk() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/png");
            when(mockFile.getSize()).thenReturn(512L);
            when(mockFile.getOriginalFilename()).thenReturn("img.png");
            when(mockFile.getInputStream())
                    .thenReturn(new ByteArrayInputStream("png-data".getBytes()));

            String url = service.store(mockFile, 2L);
            // url = /uploads/properties/2/<uuid>.png
            String filename = url.substring(url.lastIndexOf('/') + 1);
            Path stored = tempDir.resolve("properties/2/" + filename);

            assertThat(Files.exists(stored)).isTrue();
        }

        @Test
        @DisplayName("PNG dhe WEBP janë tippe të lejuara")
        void accepts_png_and_webp() throws IOException {
            for (String type : new String[]{"image/png", "image/webp"}) {
                when(mockFile.isEmpty()).thenReturn(false);
                when(mockFile.getContentType()).thenReturn(type);
                when(mockFile.getSize()).thenReturn(100L);
                when(mockFile.getOriginalFilename()).thenReturn("img.png");
                when(mockFile.getInputStream())
                        .thenReturn(new ByteArrayInputStream("data".getBytes()));

                assertThatNoException().isThrownBy(() -> service.store(mockFile, 1L));
            }
        }

        @Test
        @DisplayName("hedh IllegalArgumentException kur fajlli është bosh")
        void throws_whenFileEmpty() {
            when(mockFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> service.store(mockFile, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bosh");
        }

        @Test
        @DisplayName("hedh IllegalArgumentException kur fajlli është null")
        void throws_whenFileNull() {
            assertThatThrownBy(() -> service.store(null, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("hedh IllegalArgumentException kur tipi nuk lejohet (PDF)")
        void throws_whenContentTypeNotAllowed() {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("application/pdf");

            assertThatThrownBy(() -> service.store(mockFile, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JPEG");
        }

        @Test
        @DisplayName("hedh IllegalArgumentException kur tejkalohet madhësia maksimale")
        void throws_whenFileTooLarge() {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            long sixMb = 6L * 1024 * 1024;
            when(mockFile.getSize()).thenReturn(sixMb);

            assertThatThrownBy(() -> service.store(mockFile, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5MB");
        }

        @Test
        @DisplayName("filename pa extension merr 'jpg' si default")
        void noExtension_defaultsToJpg() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getSize()).thenReturn(100L);
            when(mockFile.getOriginalFilename()).thenReturn("photonoextension");
            when(mockFile.getInputStream())
                    .thenReturn(new ByteArrayInputStream("data".getBytes()));

            String url = service.store(mockFile, 1L);
            assertThat(url).endsWith(".jpg");
        }

        @Test
        @DisplayName("filename null merr 'jpg' si default")
        void nullFilename_defaultsToJpg() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getSize()).thenReturn(100L);
            when(mockFile.getOriginalFilename()).thenReturn(null);
            when(mockFile.getInputStream())
                    .thenReturn(new ByteArrayInputStream("data".getBytes()));

            String url = service.store(mockFile, 1L);
            assertThat(url).endsWith(".jpg");
        }

        @Test
        @DisplayName("krijon direktoritë e nevojshme nëse nuk ekzistojnë")
        void createsDirectories() throws IOException {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getContentType()).thenReturn("image/jpeg");
            when(mockFile.getSize()).thenReturn(100L);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
            when(mockFile.getInputStream())
                    .thenReturn(new ByteArrayInputStream("data".getBytes()));

            service.store(mockFile, 999L);

            Path dir = tempDir.resolve("properties/999");
            assertThat(Files.isDirectory(dir)).isTrue();
        }
    }

    // ── delete() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("sukses — fshin fajllin nga disku")
        void success_deletesFile() throws IOException {
            // Krijo fajllin
            Path dir = tempDir.resolve("properties/1");
            Files.createDirectories(dir);
            Path file = dir.resolve("toDelete.jpg");
            Files.writeString(file, "data");

            service.delete("/uploads/properties/1/toDelete.jpg");

            assertThat(Files.exists(file)).isFalse();
        }

        @Test
        @DisplayName("nuk hedh exception kur URL është null")
        void noException_whenUrlNull() {
            assertThatNoException().isThrownBy(() -> service.delete(null));
        }

        @Test
        @DisplayName("nuk hedh exception kur URL është bosh")
        void noException_whenUrlBlank() {
            assertThatNoException().isThrownBy(() -> service.delete("   "));
        }

        @Test
        @DisplayName("nuk hedh exception kur fajlli nuk ekziston")
        void noException_whenFileDoesNotExist() {
            assertThatNoException().isThrownBy(
                    () -> service.delete("/uploads/properties/1/nonexistent.jpg"));
        }
    }
}
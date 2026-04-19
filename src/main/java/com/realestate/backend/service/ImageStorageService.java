package com.realestate.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class ImageStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size-mb:5}")
    private long maxSizeMb;

    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );


    public String store(MultipartFile file, Long propertyId) throws IOException {


        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fajlli është bosh");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Tipi i fajllit nuk lejohet. Lejo: JPEG, PNG, WEBP");
        }
        long maxBytes = maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "Imazhi tejkalon madhësinë maksimale " + maxSizeMb + "MB");
        }


        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;

        Path dir = Paths.get(uploadDir, "properties", String.valueOf(propertyId));
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("Imazhi u ruajt: {}", target);


        return "/uploads/properties/" + propertyId + "/" + filename;
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // imageUrl = /uploads/properties/1/abc.jpg
            // Hiq "/" e parë dhe ndërto path
            Path file = Paths.get(uploadDir, imageUrl.replaceFirst("^/uploads/", ""));
            Files.deleteIfExists(file);
            log.info("Imazhi u fshi: {}", file);
        } catch (IOException e) {
            log.warn("Nuk u fshi imazhi {}: {}", imageUrl, e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
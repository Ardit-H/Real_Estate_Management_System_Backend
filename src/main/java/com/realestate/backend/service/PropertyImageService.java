package com.realestate.backend.service;

import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.property.PropertyImage;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.repository.PropertyImageRepository;
import com.realestate.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyImageService {

    private final PropertyRepository      propertyRepository;
    private final PropertyImageRepository imageRepository;
    private final ImageStorageService     storageService;


    @Transactional
    public PropertyImage uploadImage(Long propertyId,
                                     MultipartFile file,
                                     String caption,
                                     boolean setPrimary) throws IOException {

        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prona nuk u gjet: " + propertyId));

        String url = storageService.store(file, propertyId);

        if (setPrimary) {
            imageRepository.clearPrimaryForProperty(propertyId);
        }


        int nextOrder = imageRepository.maxSortOrder(propertyId)
                .map(m -> m + 1).orElse(0);


        PropertyImage image = PropertyImage.builder()
                .property(property)
                .imageUrl(url)
                .caption(caption)
                .sortOrder(nextOrder)
                .isPrimary(setPrimary)
                .build();

        PropertyImage saved = imageRepository.save(image);
        log.info("Imazh u shtua: propertyId={}, url={}", propertyId, url);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PropertyImage> getImages(Long propertyId) {
        return imageRepository.findByProperty_IdOrderBySortOrderAsc(propertyId);
    }

    @Transactional
    public void setPrimary(Long propertyId, Long imageId) {
        imageRepository.clearPrimaryForProperty(propertyId);
        imageRepository.setPrimary(imageId);
    }

    @Transactional
    public void deleteImage(Long propertyId, Long imageId) {
        PropertyImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Imazhi nuk u gjet: " + imageId));


        storageService.delete(image.getImageUrl());


        imageRepository.delete(image);
        log.info("Imazh u fshi: id={}", imageId);
    }
}
package com.realestate.backend.service;

import com.realestate.backend.dto.notification.NotificationDtos.*;
import com.realestate.backend.entity.enums.NotificationType;
import com.realestate.backend.entity.notification.Notification;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

    // ── Merr notifikimet e mia ────────────────────────────────
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long userId = TenantContext.getUserId();
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    // ── Numëro të palexuarat ──────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable(value = "notif-count", key = "T(com.realestate.backend.multitenancy.TenantContext).getUserId()")
    public UnreadCountResponse getUnreadCount() {
        long count = notificationRepo.countByUserIdAndIsReadFalse(TenantContext.getUserId());
        return new UnreadCountResponse(count);
    }

    // ── Merr të palexuarat ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread() {
        return notificationRepo
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(TenantContext.getUserId())
                .stream().map(this::toResponse).toList();
    }

    // ── Shëno një si të lexuar ────────────────────────────────
    @Transactional
    @CacheEvict(value = "notif-count", allEntries = true)
    public void markOneRead(Long id) {
        Long userId = TenantContext.getUserId();
        int updated = notificationRepo.markOneRead(id, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Njoftimi nuk u gjet: " + id);
        }
    }

    // ── Shëno të gjitha si të lexuara ─────────────────────────
    @Transactional
    @CacheEvict(value = "notif-count", allEntries = true)
    public BatchReadResponse markAllRead() {
        int marked = notificationRepo.markAllReadForUser(TenantContext.getUserId());
        return new BatchReadResponse(marked, marked + " njoftime u shënuan si të lexuara");
    }

    // ── Fshij të lexuarat ─────────────────────────────────────
    @Transactional
    public void deleteRead() {
        notificationRepo.deleteReadForUser(TenantContext.getUserId());
    }

    // ── Krijo njoftim (nga sistemi / service-t e tjera) ───────
    @Transactional
    public NotificationResponse create(NotificationCreateRequest req) {
        Notification notification = Notification.builder()
                .userId(req.userId())
                .title(req.title())
                .message(req.message())
                .type(req.type() != null ? req.type() : NotificationType.INFO)
                .relatedEntityType(req.relatedEntityType())
                .relatedEntityId(req.relatedEntityId())
                .actionUrl(req.actionUrl())
                .isRead(false)
                .build();

        Notification saved = notificationRepo.save(notification);
        log.debug("Njoftim u krijua për userId={}, type={}", req.userId(), req.type());
        return toResponse(saved);
    }

    // ── Helper i brendshëm (pa DTO) ───────────────────────────
    @Transactional
    public void sendNotification(Long userId, String title, String message,
                                 NotificationType type, String entityType,
                                 Long entityId, String actionUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .actionUrl(actionUrl)
                .isRead(false)
                .build();
        notificationRepo.save(notification);
    }

    // ── Mapper ────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getUserId(), n.getTitle(), n.getMessage(),
                n.getType(), n.getRelatedEntityType(), n.getRelatedEntityId(),
                n.getActionUrl(), n.getIsRead(), n.getReadAt(), n.getCreatedAt()
        );
    }
}

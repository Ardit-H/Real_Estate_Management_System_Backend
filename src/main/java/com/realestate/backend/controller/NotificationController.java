package com.realestate.backend.controller;

import com.realestate.backend.dto.notification.NotificationDtos.*;
import com.realestate.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Njoftimet e mia (me pagination)")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(notificationService.getMyNotifications(page(page, size)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Njoftimet e palexuara")
    public ResponseEntity<List<NotificationResponse>> getUnread() {
        return ok(notificationService.getUnread());
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Numri i njoftimeve të palexuara")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ok(notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Shëno njoftimin si të lexuar")
    public ResponseEntity<Void> markOneRead(@PathVariable Long id) {
        notificationService.markOneRead(id);
        return noContent();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Shëno të gjitha si të lexuara")
    public ResponseEntity<BatchReadResponse> markAllRead() {
        return ok(notificationService.markAllRead());
    }

    @DeleteMapping("/read")
    @Operation(summary = "Fshij njoftimet e lexuara")
    public ResponseEntity<Void> deleteRead() {
        notificationService.deleteRead();
        return noContent();
    }

    @PostMapping
    @Operation(summary = "Krijo njoftim (vetëm ADMIN — për teste/manual)")
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody NotificationCreateRequest request) {
        return created(notificationService.create(request));
    }
}
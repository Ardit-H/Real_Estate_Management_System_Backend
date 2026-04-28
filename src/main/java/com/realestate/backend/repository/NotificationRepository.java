package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.NotificationType;
import com.realestate.backend.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    // Shëno të gjitha si të lexuara
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.userId = :userId AND n.isRead = false
    """)
    int markAllReadForUser(@Param("userId") Long userId);

    // Shëno një si të lexuar
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.id = :id AND n.userId = :userId
    """)
    int markOneRead(@Param("id") Long id, @Param("userId") Long userId);

    // Fshij notifikimet e vjetra (cleanup)
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM Notification n
        WHERE n.userId = :userId AND n.isRead = true
    """)
    int deleteReadForUser(@Param("userId") Long userId);
}

package com.pinnacle.alerts.service;

import com.pinnacle.alerts.dto.NotificationResponse;
import com.pinnacle.entity.Notification;
import com.pinnacle.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persists notifications (so a user can see history after the fact) and
 * pushes each one live over STOMP to /topic/notifications/{userId}, mirroring
 * the /topic/prices/{symbol} pattern already used for market data.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void notify(UUID userId, String message, UUID referenceAlertId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setReferenceAlertId(referenceAlertId);
        notificationRepository.save(notification);

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                NotificationResponse.from(notification)
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notification not found");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}

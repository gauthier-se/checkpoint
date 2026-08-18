package com.checkpoint.api.services.impl;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.checkpoint.api.dto.catalog.PagedResponseDto;
import com.checkpoint.api.dto.notification.NotificationResponseDto;
import com.checkpoint.api.dto.notification.UnreadCountDto;
import com.checkpoint.api.entities.Notification;
import com.checkpoint.api.entities.User;
import com.checkpoint.api.enums.NotificationType;
import com.checkpoint.api.exceptions.NotificationNotFoundException;
import com.checkpoint.api.exceptions.UnauthorizedNotificationAccessException;
import com.checkpoint.api.mapper.NotificationMapper;
import com.checkpoint.api.repositories.NotificationRepository;
import com.checkpoint.api.repositories.UserRepository;
import com.checkpoint.api.services.NotificationPreferencesService;
import com.checkpoint.api.services.NotificationService;

/**
 * Implementation of {@link NotificationService}.
 * Manages notification persistence and real-time delivery via WebSocket.
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationPreferencesService preferencesService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   NotificationMapper notificationMapper,
                                   SimpMessagingTemplate messagingTemplate,
                                   NotificationPreferencesService preferencesService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
        this.messagingTemplate = messagingTemplate;
        this.preferencesService = preferencesService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationResponseDto createNotification(UUID recipientId, UUID senderId, NotificationType type,
                                                      UUID referenceId, String message) {
        // Self-notification prevention: never notify a user about their own actions
        if (senderId != null && senderId.equals(recipientId)) {
            log.debug("Skipping self-notification: sender and recipient are the same user: {}", senderId);
            return null;
        }

        // Duplicate prevention: don't create a notification if one already exists
        // for the same sender+recipient+type+reference combination
        if (senderId != null && referenceId != null
                && notificationRepository.existsBySenderIdAndRecipientIdAndTypeAndReferenceId(
                        senderId, recipientId, type, referenceId)) {
            log.debug("Skipping duplicate notification, type: {}, sender: {}, recipient: {}, reference: {}",
                    type, senderId, recipientId, referenceId);
            return null;
        }

        // Preference gating: skip persistence + WS push when the recipient has opted out of this type
        if (!preferencesService.isEnabled(recipientId, type)) {
            log.debug("Skipping notification: recipient {} has disabled type {}", recipientId, type);
            return null;
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found with ID: " + recipientId));

        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }

        Notification notification = new Notification(recipient, sender, type, referenceId, message);
        Notification savedNotification = notificationRepository.save(notification);

        NotificationResponseDto responseDto = notificationMapper.toResponseDto(savedNotification);

        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/notifications",
                responseDto
        );

        log.info("Notification created for user {}: type: {}", recipient.getPseudo(), type);

        return responseDto;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<NotificationResponseDto> getNotifications(String userEmail, int page, int size,
                                                                      NotificationType type, Boolean isRead) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository
                .findByRecipientWithFilters(user.getId(), type, isRead, pageable);

        Page<NotificationResponseDto> dtoPage = notificationPage.map(notificationMapper::toResponseDto);

        return PagedResponseDto.from(dtoPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        long count = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());

        return new UnreadCountDto(count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markAsRead(UUID notificationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new UnauthorizedNotificationAccessException(notificationId);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        log.info("Notification {} marked as read by user {}", notificationId, user.getPseudo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        int updatedCount = notificationRepository.markAllAsReadByRecipientId(user.getId());

        log.info("Marked {} notifications as read for user {}", updatedCount, user.getPseudo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int markAsReadBulk(Set<UUID> ids, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        int updatedCount = notificationRepository.markAsReadByIdsAndRecipientId(ids, user.getId());

        log.info("Bulk-marked {} notifications as read for user {} (requested: {})",
                updatedCount, user.getPseudo(), ids.size());

        return updatedCount;
    }
}

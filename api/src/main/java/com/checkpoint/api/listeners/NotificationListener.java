package com.checkpoint.api.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.checkpoint.api.events.NotificationEvent;
import com.checkpoint.api.services.NotificationService;

/**
 * Listens for {@link NotificationEvent} and delegates notification creation
 * to the {@link NotificationService}.
 *
 * <p>All handlers are asynchronous to avoid blocking the publishing thread.</p>
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    /**
     * Constructs a new NotificationListener.
     *
     * @param notificationService the notification service
     */
    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Handles a {@link NotificationEvent} by creating and delivering
     * the notification to the recipient.
     *
     * @param event the notification event
     */
    @Async
    @EventListener
    public void onNotificationEvent(NotificationEvent event) {
        log.info("Received notification event, type: {}, recipient: {}", event.getType(), event.getRecipientId());

        notificationService.createNotification(
                event.getRecipientId(),
                event.getSenderId(),
                event.getType(),
                event.getReferenceId(),
                event.getMessage()
        );
    }
}

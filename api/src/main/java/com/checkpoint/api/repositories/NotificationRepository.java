package com.checkpoint.api.repositories;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.Notification;
import com.checkpoint.api.enums.NotificationType;

/**
 * Repository for {@link Notification} entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns a paginated list of notifications for the given recipient,
     * ordered by creation date descending (newest first).
     *
     * @param recipientId the recipient's user ID
     * @param pageable    pagination parameters
     * @return a page of notifications
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /**
     * Counts the number of unread notifications for the given recipient.
     *
     * @param recipientId the recipient's user ID
     * @return the unread notification count
     */
    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    /**
     * Marks all unread notifications as read for the given recipient.
     *
     * @param recipientId the recipient's user ID
     * @return the number of updated notifications
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") UUID recipientId);

    /**
     * Checks whether a notification already exists for the given sender, recipient,
     * type, and reference combination. Used to prevent duplicate notifications.
     *
     * @param senderId    the sender's user ID
     * @param recipientId the recipient's user ID
     * @param type        the notification type
     * @param referenceId the related entity ID
     * @return true if a matching notification already exists
     */
    boolean existsBySenderIdAndRecipientIdAndTypeAndReferenceId(
            UUID senderId, UUID recipientId, NotificationType type, UUID referenceId);

    /**
     * Returns a paginated list of notifications for the given recipient,
     * optionally filtered by type and/or read status, ordered by creation date descending.
     *
     * @param recipientId the recipient's user ID
     * @param type        the notification type filter (null = no filter)
     * @param isRead      the read-status filter (null = no filter)
     * @param pageable    pagination parameters
     * @return a page of notifications
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient.id = :recipientId
              AND (:type IS NULL OR n.type = :type)
              AND (:isRead IS NULL OR n.isRead = :isRead)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findByRecipientWithFilters(
            @Param("recipientId") UUID recipientId,
            @Param("type") NotificationType type,
            @Param("isRead") Boolean isRead,
            Pageable pageable);

    /**
     * Marks the given notifications as read, but only those that belong to
     * the given recipient and are currently unread. The recipient check is
     * enforced in the query so callers cannot mark another user's notifications.
     *
     * @param ids         the notification IDs to update
     * @param recipientId the recipient's user ID (ownership guard)
     * @return the number of updated notifications
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE n.id IN :ids AND n.recipient.id = :recipientId AND n.isRead = false")
    int markAsReadByIdsAndRecipientId(@Param("ids") Set<UUID> ids, @Param("recipientId") UUID recipientId);

    /**
     * Deletes every notification where the given user is either the recipient
     * or the sender. Used when erasing a user account so that no notification
     * survives with a dangling user reference (the {@code sender} association
     * is not cascade-managed by the {@code User} entity).
     *
     * @param userId the user whose notifications should be deleted
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :userId OR n.sender.id = :userId")
    void deleteAllForUser(@Param("userId") UUID userId);
}

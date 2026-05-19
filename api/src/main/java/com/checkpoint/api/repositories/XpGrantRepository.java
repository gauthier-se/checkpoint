package com.checkpoint.api.repositories;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.XpGrant;
import com.checkpoint.api.enums.XpEventType;

/**
 * Repository for {@link XpGrant} entities.
 */
@Repository
public interface XpGrantRepository extends JpaRepository<XpGrant, UUID> {

    /**
     * Counts grants of a given type awarded to a user since the given timestamp.
     * Used by the listener layer to enforce per-window caps (e.g. 10 review-like
     * grants per 24h per author).
     */
    @Query("""
            SELECT COUNT(g) FROM XpGrant g
            WHERE g.user.id = :userId
              AND g.eventType = :type
              AND g.grantedAt > :since
            """)
    long countByUserIdAndEventTypeAfter(@Param("userId") UUID userId,
                                        @Param("type") XpEventType type,
                                        @Param("since") LocalDateTime since);
}

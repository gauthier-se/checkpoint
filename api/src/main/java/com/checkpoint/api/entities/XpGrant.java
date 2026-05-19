package com.checkpoint.api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.checkpoint.api.enums.XpEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Audit row recording that a specific XP grant has already been awarded.
 *
 * <p>The combination of {@code user_id}, {@code event_type} and {@code target_id}
 * is unique: attempting to insert a duplicate will fail with a constraint
 * violation, which the gamification service treats as "already granted, skip".</p>
 *
 * <p>{@code target_id} is nullable for grants that don't have a natural target
 * (e.g. streak bonuses, where same-day dedup is enforced at the service level).</p>
 */
@Entity
@Table(name = "xp_grants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_xp_grants_user_event_target",
                columnNames = {"user_id", "event_type", "target_id"})
})
public class XpGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private XpEventType eventType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "xp_amount", nullable = false)
    private int xpAmount;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
    }

    public XpGrant() {}

    public XpGrant(User user, XpEventType eventType, UUID targetId, int xpAmount) {
        this.user = user;
        this.eventType = eventType;
        this.targetId = targetId;
        this.xpAmount = xpAmount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public XpEventType getEventType() {
        return eventType;
    }

    public void setEventType(XpEventType eventType) {
        this.eventType = eventType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public int getXpAmount() {
        return xpAmount;
    }

    public void setXpAmount(int xpAmount) {
        this.xpAmount = xpAmount;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
}

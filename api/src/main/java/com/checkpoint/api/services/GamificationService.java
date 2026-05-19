package com.checkpoint.api.services;

import java.util.UUID;

import com.checkpoint.api.enums.XpEventType;

/**
 * Service responsible for managing user XP and leveling.
 */
public interface GamificationService {

    /**
     * Adds XP to a user and handles level-up if the XP threshold is reached.
     * The level-up threshold is calculated as {@code level * 1000}.
     *
     * @param userId   the ID of the user to award XP to
     * @param xpAmount the amount of XP to add
     */
    void addXp(UUID userId, int xpAmount);

    /**
     * Awards XP to a user with persistent dedup. A row is inserted into
     * {@code xp_grants} keyed by {@code (userId, eventType, targetId)}; if that
     * key already exists, the grant is skipped silently. A null {@code targetId}
     * disables dedup (Postgres treats nulls as distinct), so callers using a
     * null target must enforce their own dedup (e.g. the streak service's
     * same-day check).
     *
     * @param userId    the ID of the user to award XP to
     * @param xpAmount  the amount of XP to add
     * @param eventType the kind of action being rewarded
     * @param targetId  a deterministic per-action key (may be {@code null})
     */
    void awardXp(UUID userId, int xpAmount, XpEventType eventType, UUID targetId);
}

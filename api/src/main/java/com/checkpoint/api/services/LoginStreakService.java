package com.checkpoint.api.services;

import java.util.UUID;

/**
 * Service tracking each user's consecutive-day activity streak and awarding
 * the corresponding XP bonuses.
 */
public interface LoginStreakService {

    /**
     * Records that a user performed a meaningful activity today (UTC).
     *
     * <p>If today is a continuation of the user's streak, their day counter is
     * incremented and a daily XP bonus is awarded (capped at 7 days). Every 7th
     * consecutive day, a weekly milestone bonus is also awarded. If the user
     * already has activity recorded for today, the call is a no-op.</p>
     *
     * @param userId the user performing the activity
     */
    void recordActivity(UUID userId);
}

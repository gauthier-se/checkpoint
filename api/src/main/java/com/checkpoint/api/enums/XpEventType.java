package com.checkpoint.api.enums;

/**
 * Identifies the kind of action that produced an XP grant.
 *
 * <p>Combined with the granting user and a target ID, this forms the deterministic
 * dedup key persisted in {@code xp_grants}, so retried or reversed actions cannot
 * farm XP a second time.</p>
 */
public enum XpEventType {
    USER_FOLLOWED,
    USER_GAINED_FOLLOWER,
    FIRST_LIST_CREATED,
    REVIEW_LIKED,
    GAME_RATED,
    COMMENT_REPLY,
    STREAK_DAILY,
    STREAK_WEEKLY_MILESTONE
}

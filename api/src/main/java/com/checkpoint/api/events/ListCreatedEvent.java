package com.checkpoint.api.events;

import java.util.UUID;

/**
 * Event published when a user creates their first public list.
 * The publisher is responsible for filtering: the event must only
 * fire on the user's very first list AND only when it is public.
 */
public class ListCreatedEvent {

    private final UUID userId;
    private final UUID listId;

    public ListCreatedEvent(UUID userId, UUID listId) {
        this.userId = userId;
        this.listId = listId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getListId() {
        return listId;
    }
}

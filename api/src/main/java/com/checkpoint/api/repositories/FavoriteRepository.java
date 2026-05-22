package com.checkpoint.api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.checkpoint.api.entities.Favorite;
import com.checkpoint.api.entities.User;

/**
 * Repository for {@link Favorite} entity.
 */
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    /**
     * Finds all favorites for a user, ordered by {@code displayOrder} ascending.
     *
     * @param user the user
     * @return the user's favorites in display order
     */
    List<Favorite> findByUserOrderByDisplayOrderAsc(User user);

    /**
     * Deletes all favorite rows belonging to a user.
     * Used by the replace operation before re-inserting the new ordered set.
     *
     * @param user the user whose favorites should be cleared
     */
    void deleteByUser(User user);

    /**
     * Counts how many favorite entries reference a given video game.
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);
}

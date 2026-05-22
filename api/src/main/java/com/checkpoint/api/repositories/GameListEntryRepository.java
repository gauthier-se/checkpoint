package com.checkpoint.api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.GameListEntry;

/**
 * Repository for GameListEntry entity.
 */
@Repository
public interface GameListEntryRepository extends JpaRepository<GameListEntry, UUID> {

    /**
     * Finds all entries for a list, ordered by position ascending.
     */
    @Query("""
            SELECT e FROM GameListEntry e
            JOIN FETCH e.videoGame
            WHERE e.gameList.id = :listId
            ORDER BY e.position ASC
            """)
    List<GameListEntry> findByGameListIdOrderByPositionAsc(@Param("listId") UUID listId);

    /**
     * Checks if a video game is already in a list.
     */
    boolean existsByGameListIdAndVideoGameId(UUID gameListId, UUID videoGameId);

    /**
     * Finds a specific entry by list and video game.
     */
    Optional<GameListEntry> findByGameListIdAndVideoGameId(UUID gameListId, UUID videoGameId);

    /**
     * Finds the maximum position in a list, or null if the list is empty.
     */
    @Query("SELECT MAX(e.position) FROM GameListEntry e WHERE e.gameList.id = :listId")
    Optional<Integer> findMaxPositionByGameListId(@Param("listId") UUID listId);

    /**
     * Deletes an entry by list and video game.
     */
    void deleteByGameListIdAndVideoGameId(UUID gameListId, UUID videoGameId);

    /**
     * Counts how many user-list entries reference a given video game.
     * Used by the admin delete-game integrity check.
     */
    long countByVideoGameId(UUID videoGameId);
}

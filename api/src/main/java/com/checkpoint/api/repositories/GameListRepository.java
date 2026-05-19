package com.checkpoint.api.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.GameList;

/**
 * Repository for GameList entity.
 */
@Repository
public interface GameListRepository extends JpaRepository<GameList, UUID> {

    /**
     * Finds all lists owned by a user, ordered by creation date descending.
     */
    @Query("""
            SELECT gl FROM GameList gl
            JOIN FETCH gl.user
            WHERE gl.user.id = :userId
            """)
    Page<GameList> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds public lists by a user's pseudo, ordered by creation date descending.
     */
    @Query("""
            SELECT gl FROM GameList gl
            JOIN FETCH gl.user
            WHERE gl.user.pseudo = :pseudo AND gl.isPrivate = false
            """)
    Page<GameList> findPublicByUserPseudo(@Param("pseudo") String pseudo, Pageable pageable);

    /**
     * Finds all public lists, ordered by creation date descending.
     */
    @Query("""
            SELECT gl FROM GameList gl
            JOIN FETCH gl.user
            WHERE gl.isPrivate = false
            """)
    Page<GameList> findAllPublic(Pageable pageable);

    /**
     * Finds public lists ordered by like count (most popular first).
     */
    @Query("""
            SELECT gl FROM GameList gl
            JOIN FETCH gl.user
            WHERE gl.isPrivate = false
            ORDER BY (SELECT COUNT(l) FROM Like l WHERE l.gameList.id = gl.id) DESC
            """)
    Page<GameList> findPopularPublic(Pageable pageable);

    /**
     * Counts the total number of lists owned by a user. Used by the
     * gamification system to detect the user's first-ever list.
     */
    long countByUserId(UUID userId);
}

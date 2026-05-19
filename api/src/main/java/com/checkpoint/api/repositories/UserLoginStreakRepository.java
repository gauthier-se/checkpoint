package com.checkpoint.api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.checkpoint.api.entities.UserLoginStreak;

/**
 * Repository for {@link UserLoginStreak} entities.
 */
@Repository
public interface UserLoginStreakRepository extends JpaRepository<UserLoginStreak, UUID> {

    Optional<UserLoginStreak> findByUserId(UUID userId);
}

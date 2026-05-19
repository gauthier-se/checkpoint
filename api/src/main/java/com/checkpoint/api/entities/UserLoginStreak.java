package com.checkpoint.api.entities;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Tracks a user's consecutive-day activity streak.
 *
 * <p>{@code currentDay} is the number of consecutive UTC days the user has been
 * active. {@code lastActivityDate} is the UTC date of the most recent recorded
 * activity. The streak service decides whether each new activity continues,
 * resets or no-ops the streak by comparing today's UTC date against this row.</p>
 */
@Entity
@Table(name = "user_login_streaks")
public class UserLoginStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_day", nullable = false)
    private int currentDay;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    public UserLoginStreak() {}

    public UserLoginStreak(User user) {
        this.user = user;
        this.currentDay = 0;
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

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }
}

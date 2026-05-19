package com.checkpoint.api.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.checkpoint.api.events.UserActivityEvent;
import com.checkpoint.api.services.LoginStreakService;

/**
 * Forwards {@link UserActivityEvent}s to the {@link LoginStreakService} so the
 * user's consecutive-day counter is updated and the daily / weekly XP bonuses
 * are awarded.
 */
@Component
public class LoginStreakListener {

    private static final Logger log = LoggerFactory.getLogger(LoginStreakListener.class);

    private final LoginStreakService loginStreakService;

    public LoginStreakListener(LoginStreakService loginStreakService) {
        this.loginStreakService = loginStreakService;
    }

    @Async
    @EventListener
    public void onUserActivity(UserActivityEvent event) {
        log.debug("Handling UserActivityEvent for user {}", event.getUserId());
        loginStreakService.recordActivity(event.getUserId());
    }
}

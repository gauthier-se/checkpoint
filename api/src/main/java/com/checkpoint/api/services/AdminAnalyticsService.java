package com.checkpoint.api.services;

import com.checkpoint.api.dto.admin.AdminAnalyticsDto;

/**
 * Service interface for the admin analytics dashboard.
 */
public interface AdminAnalyticsService {

    /**
     * Aggregates platform-wide KPIs and top-5 lists into a single DTO.
     *
     * @return the analytics snapshot
     */
    AdminAnalyticsDto getAnalytics();
}

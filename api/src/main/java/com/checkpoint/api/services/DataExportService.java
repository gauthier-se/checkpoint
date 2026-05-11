package com.checkpoint.api.services;

import com.checkpoint.api.dto.export.UserDataExportDto;

/**
 * Service interface for the right-to-data-portability export
 * (GDPR Article 20).
 */
public interface DataExportService {

    /**
     * Aggregates every personal data category we hold for the user
     * identified by the given email into a single export payload.
     *
     * @param email the email of the user, taken from the authenticated principal
     * @return a populated {@link UserDataExportDto}
     */
    UserDataExportDto exportForUser(String email);
}

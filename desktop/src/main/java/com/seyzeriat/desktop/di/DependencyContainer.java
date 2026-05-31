package com.seyzeriat.desktop.di;

import com.seyzeriat.desktop.service.*;
import com.seyzeriat.desktop.service.impl.*;

/**
 * A simple dependency injection container for managing service instances
 * and creating controllers with their required dependencies.
 */
public class DependencyContainer {

    private static DependencyContainer instance;

    private final AuthenticationService authenticationService;
    private final AnalyticsService analyticsService;
    private final GameService gameService;
    private final UserService userService;
    private final NewsService newsService;
    private final ReportService reportService;
    private final ReviewService reviewService;

    private DependencyContainer() {
        this.authenticationService = new AuthApiClient();
        this.analyticsService = new AnalyticsApiClient(authenticationService);
        this.gameService = new GameApiClient(authenticationService);
        this.userService = new UserApiClient(authenticationService);
        this.newsService = new NewsApiClient(authenticationService);
        this.reportService = new ReportApiClient(authenticationService);
        this.reviewService = new ReviewApiClient(authenticationService);
    }

    /**
     * Gets the singleton instance of the DependencyContainer.
     *
     * @return the singleton instance
     */
    public static synchronized DependencyContainer getInstance() {
        if (instance == null) {
            instance = new DependencyContainer();
        }
        return instance;
    }

    /**
     * Creates a controller instance of the specified type, injecting any required dependencies.
     *
     * @param type the class of the controller to create
     * @return an instance of the specified controller class
     * @throws RuntimeException if the controller cannot be created
     */
    public Object createController(Class<?> type) {
        try {
            if (type.equals(com.seyzeriat.desktop.controller.LoginController.class)) {
                return new com.seyzeriat.desktop.controller.LoginController(authenticationService);
            } else if (type.equals(com.seyzeriat.desktop.controller.AnalyticsController.class)) {
                return new com.seyzeriat.desktop.controller.AnalyticsController(analyticsService);
            } else if (type.equals(com.seyzeriat.desktop.controller.ManageGamesController.class)) {
                return new com.seyzeriat.desktop.controller.ManageGamesController(gameService);
            } else if (type.equals(com.seyzeriat.desktop.controller.ImportGamesController.class)) {
                return new com.seyzeriat.desktop.controller.ImportGamesController(gameService);
            } else if (type.equals(com.seyzeriat.desktop.controller.BulkImportController.class)) {
                return new com.seyzeriat.desktop.controller.BulkImportController(gameService);
            } else if (type.equals(com.seyzeriat.desktop.controller.GameFormController.class)) {
                return new com.seyzeriat.desktop.controller.GameFormController(gameService);
            } else if (type.equals(com.seyzeriat.desktop.controller.UserManagementController.class)) {
                return new com.seyzeriat.desktop.controller.UserManagementController(userService);
            } else if (type.equals(com.seyzeriat.desktop.controller.UserDetailController.class)) {
                return new com.seyzeriat.desktop.controller.UserDetailController(userService);
            } else if (type.equals(com.seyzeriat.desktop.controller.ReviewModerationController.class)) {
                return new com.seyzeriat.desktop.controller.ReviewModerationController(reviewService, userService);
            } else if (type.equals(com.seyzeriat.desktop.controller.ReviewReportsController.class)) {
                return new com.seyzeriat.desktop.controller.ReviewReportsController(reviewService);
            } else if (type.equals(com.seyzeriat.desktop.controller.ReportModerationController.class)) {
                return new com.seyzeriat.desktop.controller.ReportModerationController(reportService, reviewService);
            } else if (type.equals(com.seyzeriat.desktop.controller.NewsManagementController.class)) {
                return new com.seyzeriat.desktop.controller.NewsManagementController(newsService);
            } else if (type.equals(com.seyzeriat.desktop.controller.NewsEditorDialogController.class)) {
                return new com.seyzeriat.desktop.controller.NewsEditorDialogController(newsService);
            }

            // Fallback for controllers without arguments
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create controller: " + type.getName(), e);
        }
    }
}

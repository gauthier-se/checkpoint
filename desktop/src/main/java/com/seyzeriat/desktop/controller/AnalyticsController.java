package com.seyzeriat.desktop.controller;

import java.util.List;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.AnalyticsResult;
import com.seyzeriat.desktop.dto.AnalyticsResult.TopGame;
import com.seyzeriat.desktop.dto.AnalyticsResult.TopReviewer;
import com.seyzeriat.desktop.service.ApiService;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller for the admin analytics dashboard view.
 *
 * <p>Sends a {@code GET} request to {@code /api/admin/analytics} with the JWT
 * Bearer token. The backend verifies the user has {@code ROLE_ADMIN} authority
 * before returning the aggregated KPIs and top-5 lists.</p>
 */
public class AnalyticsController {

    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML private Label totalUsersValue;
    @FXML private Label activeUsersValue;
    @FXML private Label totalGamesValue;
    @FXML private Label totalReviewsValue;
    @FXML private Label openReportsValue;

    @FXML private TableView<TopGame> topGamesTable;
    @FXML private TableColumn<TopGame, String> topGameTitleColumn;
    @FXML private TableColumn<TopGame, Number> topGameCountColumn;

    @FXML private TableView<TopReviewer> topReviewersTable;
    @FXML private TableColumn<TopReviewer, String> topReviewerNameColumn;
    @FXML private TableColumn<TopReviewer, Number> topReviewerCountColumn;

    private final ApiService apiService = new ApiService();
    private HelloApplication application;

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);

        topGameTitleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));
        topGameCountColumn.setCellValueFactory(cellData ->
                new SimpleLongProperty(cellData.getValue().getReviewCount()));

        topReviewerNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUsername()));
        topReviewerCountColumn.setCellValueFactory(cellData ->
                new SimpleLongProperty(cellData.getValue().getReviewCount()));

        fetchAnalytics();
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    @FXML
    private void onRefresh() {
        fetchAnalytics();
    }

    private void fetchAnalytics() {
        setLoading(true);
        statusLabel.setText("Chargement des analytics...");

        Task<AnalyticsResult> fetchTask = new Task<>() {
            @Override
            protected AnalyticsResult call() throws Exception {
                return apiService.getAnalytics();
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            AnalyticsResult analytics = fetchTask.getValue();
            setLoading(false);
            populate(analytics);
            statusLabel.setText("Analytics à jour.");
        }));

        fetchTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = fetchTask.getException();
            if (ex instanceof ApiService.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(fetchTask, "fetch-analytics-thread").start();
    }

    private void populate(AnalyticsResult analytics) {
        totalUsersValue.setText(String.valueOf(analytics.getTotalUsers()));
        activeUsersValue.setText(String.valueOf(analytics.getActiveUsers()));
        totalGamesValue.setText(String.valueOf(analytics.getTotalGames()));
        totalReviewsValue.setText(String.valueOf(analytics.getTotalReviews()));
        openReportsValue.setText(String.valueOf(analytics.getOpenReports()));

        List<TopGame> games = analytics.getTopReviewedGames();
        topGamesTable.getItems().setAll(games != null ? games : List.of());

        List<TopReviewer> reviewers = analytics.getTopReviewers();
        topReviewersTable.getItems().setAll(reviewers != null ? reviewers : List.of());
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        refreshButton.setDisable(loading);
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }
}

package com.seyzeriat.desktop;

import java.io.IOException;
import java.util.Objects;

import com.seyzeriat.desktop.controller.AnalyticsController;
import com.seyzeriat.desktop.controller.BulkImportController;
import com.seyzeriat.desktop.controller.ImportGamesController;
import com.seyzeriat.desktop.controller.LoginController;
import com.seyzeriat.desktop.controller.ManageGamesController;
import com.seyzeriat.desktop.controller.NewsManagementController;
import com.seyzeriat.desktop.controller.ReportModerationController;
import com.seyzeriat.desktop.controller.ReviewModerationController;
import com.seyzeriat.desktop.controller.UserManagementController;
import com.seyzeriat.desktop.service.TokenManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    private Stage primaryStage;
    private StackPane contentArea;
    private Button activeNavButton;
    private String stylesheetUrl;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.stylesheetUrl = Objects.requireNonNull(
                getClass().getResource("styles.css")).toExternalForm();

        stage.setTitle("Checkpoint — Administration");

        // Always start with the login screen
        showLoginView();
    }

    /**
     * Shows the login screen. Called on startup and when the user is
     * redirected after a 401/403 or logout.
     */
    public void showLoginView() {
        TokenManager.getInstance().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Node loginView = loader.load();

            LoginController controller = loader.getController();
            controller.setApplication(this);

            StackPane root = new StackPane(loginView);
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("login-root");

            Scene scene = new Scene(root, 500, 600);
            scene.getStylesheets().add(stylesheetUrl);

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load login view", e);
        }
    }

    /**
     * Shows the main application view (sidebar + content area).
     * Called by {@link LoginController} after successful authentication.
     */
    public void showMainView() {
        // Sidebar navigation
        VBox sidebar = createSidebar();

        // Content area
        contentArea = new StackPane();
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Main layout
        HBox root = new HBox();
        root.getChildren().addAll(sidebar, contentArea);

        // Load the analytics dashboard by default
        showAnalyticsView();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(stylesheetUrl);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.getStyleClass().add("sidebar");

        Label appTitle = new Label("Checkpoint");
        appTitle.getStyleClass().add("app-title");

        Button analyticsBtn = createNavButton("Tableau de bord", this::showAnalyticsView);
        Button importBtn = createNavButton("Importer des jeux", this::showImportGamesView);
        Button bulkImportBtn = createNavButton("Import en masse", this::showBulkImportView);
        Button manageGamesBtn = createNavButton("Gérer les jeux", this::showManageGamesView);
        Button usersBtn = createNavButton("Utilisateurs", this::showUsersView);
        Button reviewsBtn = createNavButton("Modération des avis", this::showReviewsView);
        Button reportsBtn = createNavButton("Modération des signalements", this::showReportsView);
        Button newsBtn = createNavButton("Actualités", this::showNewsView);

        // Set analytics dashboard as active by default
        analyticsBtn.getStyleClass().add("active");
        activeNavButton = analyticsBtn;

        // Spacer to push logout to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Logout button
        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.getStyleClass().addAll("nav-button", "logout-button");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(event -> showLoginView());

        sidebar.getChildren().addAll(appTitle, analyticsBtn, importBtn, bulkImportBtn, manageGamesBtn, usersBtn, reviewsBtn, reportsBtn, newsBtn, spacer, logoutBtn);
        return sidebar;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(event -> {
            if (activeNavButton != null) {
                activeNavButton.getStyleClass().remove("active");
            }
            btn.getStyleClass().add("active");
            activeNavButton = btn;
            action.run();
        });
        return btn;
    }

    private void showAnalyticsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("analytics-view.fxml"));
            Node analyticsView = loader.load();

            AnalyticsController controller = loader.getController();
            controller.setApplication(this);

            setContent(analyticsView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showImportGamesView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("import-games-view.fxml"));
            Node importView = loader.load();

            ImportGamesController controller = loader.getController();
            controller.setApplication(this);

            setContent(importView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showBulkImportView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("bulk-import-view.fxml"));
            Node bulkImportView = loader.load();

            BulkImportController controller = loader.getController();
            controller.setApplication(this);

            setContent(bulkImportView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showManageGamesView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("manage-games-view.fxml"));
            Node manageGamesView = loader.load();

            ManageGamesController controller = loader.getController();
            controller.setApplication(this);

            setContent(manageGamesView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showUsersView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("users-view.fxml"));
            Node usersView = loader.load();

            UserManagementController controller = loader.getController();
            controller.setApplication(this);

            setContent(usersView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showReviewsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("reviews-view.fxml"));
            Node reviewsView = loader.load();

            ReviewModerationController controller = loader.getController();
            controller.setApplication(this);

            setContent(reviewsView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showReportsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("reports-view.fxml"));
            Node reportsView = loader.load();

            ReportModerationController controller = loader.getController();
            controller.setApplication(this);

            setContent(reportsView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    private void showNewsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("news-management-view.fxml"));
            Node newsView = loader.load();

            NewsManagementController controller = loader.getController();
            controller.setApplication(this);

            setContent(newsView);
        } catch (IOException e) {
            Label error = new Label("Erreur lors du chargement de la vue : " + e.getMessage());
            setContent(error);
        }
    }

    /**
     * Replaces the content area with the given node.
     * Public so child controllers can navigate to other views.
     *
     * @param content the node to display
     */
    public void setContent(Node content) {
        contentArea.getChildren().setAll(content);
    }
}

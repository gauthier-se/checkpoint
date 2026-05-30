package com.seyzeriat.desktop.controller;

import java.io.IOException;
import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.UserDetailResult;
import com.seyzeriat.desktop.service.UserService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Controller for the User Detail view.
 *
 * <p>Displays full profile information for a user and provides
 * moderation actions: clear bio, clear picture, ban/unban.</p>
 */
public class UserDetailController {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox detailContent;

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label bannedLabel;
    @FXML private Label privateLabel;
    @FXML private Label levelLabel;
    @FXML private Label xpLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label reviewCountLabel;
    @FXML private Label reportCountLabel;
    @FXML private Label pictureLabel;
    @FXML private Label bioLabel;

    @FXML private Button clearBioBtn;
    @FXML private Button clearPictureBtn;
    @FXML private Button banBtn;

    private final UserService userService;
    private HelloApplication application;

    public UserDetailController(UserService userService) {
        this.userService = userService;
    }
    private String userId;
    private UserDetailResult currentUser;

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);
        detailContent.setVisible(false);
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    /**
     * Loads the user detail from the API.
     *
     * @param userId the user ID to load
     */
    public void loadUser(String userId) {
        this.userId = userId;
        fetchUserDetail();
    }

    @FXML
    private void onBack() {
        if (application == null) {
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/users-view.fxml");
            Node usersView = loader.load();

            UserManagementController controller = loader.getController();
            controller.setApplication(application);

            application.setContent(usersView);
        } catch (IOException e) {
            statusLabel.setText("Erreur lors du retour : " + e.getMessage());
        }
    }

    @FXML
    private void onClearBio() {
        if (currentUser == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la bio de " + currentUser.getUsername() + " ?");
        alert.setContentText("La bio de l'utilisateur sera effacée. Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            editUser("{\"clearBio\": true, \"clearPicture\": false}");
        }
    }

    @FXML
    private void onClearPicture() {
        if (currentUser == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la photo de profil de " + currentUser.getUsername() + " ?");
        alert.setContentText("La photo de profil sera effacée. Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            editUser("{\"clearBio\": false, \"clearPicture\": true}");
        }
    }

    @FXML
    private void onToggleBan() {
        if (currentUser == null) {
            return;
        }

        if (currentUser.isBanned()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Débannir l'utilisateur " + currentUser.getUsername() + " ?");
            alert.setContentText("L'utilisateur pourra de nouveau se connecter à CheckPoint.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                unbanUser();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Bannir l'utilisateur " + currentUser.getUsername() + " ?");
            alert.setContentText("L'utilisateur ne pourra plus se connecter à CheckPoint.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                banUser();
            }
        }
    }

    private void fetchUserDetail() {
        setLoading(true);
        statusLabel.setText("Chargement du profil...");

        Task<UserDetailResult> fetchTask = new Task<>() {
            @Override
            protected UserDetailResult call() throws Exception {
                return userService.getUserDetail(userId);
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            setLoading(false);
            currentUser = fetchTask.getValue();
            displayUser(currentUser);
        }));

        fetchTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = fetchTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(fetchTask, "fetch-user-detail-thread").start();
    }

    private void displayUser(UserDetailResult user) {
        detailContent.setVisible(true);
        statusLabel.setText("");

        titleLabel.setText("Détail — " + user.getUsername());
        usernameLabel.setText(user.getUsername());
        emailLabel.setText(user.getEmail());
        bannedLabel.setText(user.isBanned() ? "Banni" : "Actif");
        privateLabel.setText(user.isPrivate() ? "Oui" : "Non");
        levelLabel.setText(String.valueOf(user.getLevel()));
        xpLabel.setText(String.valueOf(user.getXpPoint()));
        createdAtLabel.setText(user.getCreatedAt() != null ? user.getCreatedAt() : "—");
        reviewCountLabel.setText(String.valueOf(user.getReviewCount()));
        reportCountLabel.setText(String.valueOf(user.getReportCount()));
        pictureLabel.setText(user.getPicture() != null ? user.getPicture() : "Aucune");
        bioLabel.setText(user.getBio() != null ? user.getBio() : "Aucune");

        // Update button states
        clearBioBtn.setDisable(user.getBio() == null);
        clearPictureBtn.setDisable(user.getPicture() == null);

        if (user.isBanned()) {
            banBtn.setText("Débannir");
            banBtn.getStyleClass().removeAll("destructive-button");
            banBtn.getStyleClass().add("search-button");
        } else {
            banBtn.setText("Bannir");
            banBtn.getStyleClass().removeAll("search-button");
            banBtn.getStyleClass().add("destructive-button");
        }
    }

    private void editUser(String jsonBody) {
        setLoading(true);
        statusLabel.setText("Modification en cours...");

        Task<UserDetailResult> editTask = new Task<>() {
            @Override
            protected UserDetailResult call() throws Exception {
                return userService.editUser(userId, jsonBody);
            }
        };

        editTask.setOnSucceeded(event -> Platform.runLater(() -> {
            setLoading(false);
            currentUser = editTask.getValue();
            displayUser(currentUser);
            statusLabel.setText("Profil modifié avec succès.");
        }));

        editTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = editTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(editTask, "edit-user-thread").start();
    }

    private void banUser() {
        setLoading(true);
        statusLabel.setText("Bannissement en cours...");

        Task<Void> banTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.banUser(userId);
                return null;
            }
        };

        banTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Utilisateur banni avec succès.");
            fetchUserDetail();
        }));

        banTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = banTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(banTask, "ban-user-thread").start();
    }

    private void unbanUser() {
        setLoading(true);
        statusLabel.setText("Débannissement en cours...");

        Task<Void> unbanTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.unbanUser(userId);
                return null;
            }
        };

        unbanTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Utilisateur débanni avec succès.");
            fetchUserDetail();
        }));

        unbanTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = unbanTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(unbanTask, "unban-user-thread").start();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }
}

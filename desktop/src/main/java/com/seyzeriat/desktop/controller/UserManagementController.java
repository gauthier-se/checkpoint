package com.seyzeriat.desktop.controller;

import java.util.List;
import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.UserResult;
import com.seyzeriat.desktop.service.UserService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/**
 * Controller for the User Management view.
 *
 * <p>Sends a {@code GET} request to {@code /api/admin/users} with the JWT Bearer token.
 * The backend verifies the user has {@code ROLE_ADMIN} authority before returning
 * the list. Displays ID, Username, Email, Status, and Actions in a table.</p>
 */
public class UserManagementController {

    @FXML private TableView<UserResult> usersTable;
    @FXML private TableColumn<UserResult, String> idColumn;
    @FXML private TableColumn<UserResult, String> usernameColumn;
    @FXML private TableColumn<UserResult, String> emailColumn;
    @FXML private TableColumn<UserResult, String> statusColumn;
    @FXML private TableColumn<UserResult, Void> actionColumn;
    @FXML private Label statusLabel;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final UserService userService;
    private HelloApplication application;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);

        // Configure table columns
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId()));
        usernameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getUsername()));
        emailColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEmail()));
        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isBanned() ? "Banni" : "Actif"));

        setupActionColumn();

        // Load users on view init
        fetchUsers();
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button detailBtn = new Button("Détails");
            private final Button banBtn = new Button();
            private final HBox buttons = new HBox(5, detailBtn, banBtn);

            {
                detailBtn.getStyleClass().add("search-button");

                detailBtn.setOnAction(event -> {
                    UserResult user = getTableView().getItems().get(getIndex());
                    showUserDetail(user);
                });

                banBtn.setOnAction(event -> {
                    UserResult user = getTableView().getItems().get(getIndex());
                    if (user.isBanned()) {
                        confirmAndUnban(user);
                    } else {
                        confirmAndBan(user);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    UserResult user = getTableView().getItems().get(getIndex());
                    if (user.isBanned()) {
                        banBtn.setText("Débannir");
                        banBtn.getStyleClass().removeAll("destructive-button");
                        banBtn.getStyleClass().add("search-button");
                    } else {
                        banBtn.setText("Bannir");
                        banBtn.getStyleClass().removeAll("search-button");
                        banBtn.getStyleClass().add("destructive-button");
                    }
                    setGraphic(buttons);
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        fetchUsers();
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    /**
     * Fetches all users from the admin API on a background thread.
     */
    private void fetchUsers() {
        setLoading(true);
        statusLabel.setText("Chargement des utilisateurs...");

        Task<List<UserResult>> fetchTask = new Task<>() {
            @Override
            protected List<UserResult> call() throws Exception {
                return userService.getUsers();
            }
        };

        fetchTask.setOnSucceeded(event -> Platform.runLater(() -> {
            List<UserResult> users = fetchTask.getValue();
            setLoading(false);

            usersTable.getItems().setAll(users);

            if (users.isEmpty()) {
                statusLabel.setText("Aucun utilisateur trouvé.");
            } else {
                statusLabel.setText(users.size() + " utilisateur(s) chargé(s).");
            }
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

        new Thread(fetchTask, "fetch-users-thread").start();
    }

    private void showUserDetail(UserResult user) {
        if (application == null) {
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/user-detail-view.fxml");
            javafx.scene.Node detailView = loader.load();

            UserDetailController controller = loader.getController();
            controller.setApplication(application);
            controller.loadUser(user.getId());

            application.setContent(detailView);
        } catch (java.io.IOException e) {
            statusLabel.setText("Erreur lors du chargement de la vue détail : " + e.getMessage());
        }
    }

    private void confirmAndBan(UserResult user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Bannir l'utilisateur " + user.getUsername() + " ?");
        alert.setContentText("L'utilisateur ne pourra plus se connecter à CheckPoint.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            banUser(user.getId());
        }
    }

    private void confirmAndUnban(UserResult user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Débannir l'utilisateur " + user.getUsername() + " ?");
        alert.setContentText("L'utilisateur pourra de nouveau se connecter à CheckPoint.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            unbanUser(user.getId());
        }
    }

    private void banUser(String id) {
        setLoading(true);
        statusLabel.setText("Bannissement en cours...");

        Task<Void> banTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.banUser(id);
                return null;
            }
        };

        banTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Utilisateur banni avec succès.");
            fetchUsers();
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

    private void unbanUser(String id) {
        setLoading(true);
        statusLabel.setText("Débannissement en cours...");

        Task<Void> unbanTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                userService.unbanUser(id);
                return null;
            }
        };

        unbanTask.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText("Utilisateur débanni avec succès.");
            fetchUsers();
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
        refreshButton.setDisable(loading);
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }
}

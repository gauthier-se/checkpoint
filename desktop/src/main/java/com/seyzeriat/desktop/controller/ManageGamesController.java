package com.seyzeriat.desktop.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.GameSummaryResult;
import com.seyzeriat.desktop.dto.PagedResponse;
import com.seyzeriat.desktop.service.GameService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/**
 * Controller for the "Gérer les jeux" admin view. Lists games (paginated) and
 * exposes Create / Edit / Delete actions per row.
 */
public class ManageGamesController {

    private static final int PAGE_SIZE = 20;

    @FXML private TableView<GameSummaryResult> gamesTable;
    @FXML private TableColumn<GameSummaryResult, String> titleColumn;
    @FXML private TableColumn<GameSummaryResult, String> releaseDateColumn;
    @FXML private TableColumn<GameSummaryResult, Void> actionColumn;

    @FXML private Label statusLabel;
    @FXML private Button prevButton;
    @FXML private Label pageLabel;
    @FXML private Button nextButton;
    @FXML private Button refreshButton;
    @FXML private Button createButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final GameService gameService;
    private HelloApplication application;

    /**
     * Constructs the controller with the specified game service.
     *
     * @param gameService the service used to manage games
     */
    public ManageGamesController(GameService gameService) {
        this.gameService = gameService;
    }

    private int currentPage = 0;
    private int totalPages = 1;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);

        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));
        releaseDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getReleaseDate() == null ? "—"
                        : cellData.getValue().getReleaseDate().toString()));

        setupActionColumn();
        fetchGames(currentPage);
    }

    /**
     * Sets the main application instance.
     *
     * @param application the main application instance
     */
    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Éditer");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox container = new HBox(6, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("search-button");
                deleteBtn.getStyleClass().add("destructive-button");

                editBtn.setOnAction(event -> {
                    GameSummaryResult game = getTableView().getItems().get(getIndex());
                    openForm(game.getId());
                });
                deleteBtn.setOnAction(event -> {
                    GameSummaryResult game = getTableView().getItems().get(getIndex());
                    confirmAndDelete(game);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        fetchGames(currentPage);
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            fetchGames(currentPage);
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage + 1 < totalPages) {
            currentPage++;
            fetchGames(currentPage);
        }
    }

    @FXML
    private void onCreateGame() {
        openForm(null);
    }

    private void openForm(String gameId) {
        if (application == null) {
            return;
        }
        try {
            FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/game-form-view.fxml");
            Node view = loader.load();
            GameFormController controller = loader.getController();
            controller.setApplication(application);
            controller.setGameId(gameId);
            application.setContent(view);
        } catch (IOException e) {
            statusLabel.setText("Erreur lors du chargement du formulaire : " + e.getMessage());
        }
    }

    private void fetchGames(int page) {
        loadingIndicator.setVisible(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        refreshButton.setDisable(true);
        statusLabel.setText("Chargement...");

        Task<PagedResponse<GameSummaryResult>> task = new Task<>() {
            @Override
            protected PagedResponse<GameSummaryResult> call() throws Exception {
                return gameService.getGames(page, PAGE_SIZE);
            }
        };

        task.setOnSucceeded(event -> {
            PagedResponse<GameSummaryResult> result = task.getValue();
            List<GameSummaryResult> games = result.getContent();
            gamesTable.getItems().setAll(games == null ? List.of() : games);
            if (result.getMetadata() != null) {
                totalPages = Math.max(1, result.getMetadata().getTotalPages());
            }
            updatePagination(result);
            loadingIndicator.setVisible(false);
            refreshButton.setDisable(false);
            statusLabel.setText(games == null || games.isEmpty()
                    ? "Aucun jeu à afficher."
                    : games.size() + " jeu(x) sur cette page.");
        });

        task.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            refreshButton.setDisable(false);
            Throwable ex = task.getException();
            if (isUnauthorized(ex)) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        });

        new Thread(task, "manage-games-fetch").start();
    }

    private void updatePagination(PagedResponse<GameSummaryResult> result) {
        int displayPage = currentPage + 1;
        pageLabel.setText("Page " + displayPage + "/" + totalPages);
        prevButton.setDisable(currentPage <= 0);
        nextButton.setDisable(currentPage + 1 >= totalPages);
    }

    private void confirmAndDelete(GameSummaryResult game) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le jeu");
        confirm.setHeaderText("Supprimer \"" + game.getTitle() + "\" ?");
        confirm.setContentText("Cette action est définitive. La suppression sera refusée si le jeu est encore référencé.");
        confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }
        performDelete(game);
    }

    private void performDelete(GameSummaryResult game) {
        loadingIndicator.setVisible(true);
        statusLabel.setText("Suppression de \"" + game.getTitle() + "\"...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                gameService.deleteGame(game.getId());
                return null;
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            statusLabel.setText("\"" + game.getTitle() + "\" supprimé.");
            fetchGames(currentPage);
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            Throwable ex = task.getException();
            if (isUnauthorized(ex)) {
                redirectToLogin();
                return;
            }
            if (ex instanceof com.seyzeriat.desktop.exception.GameReferencedException referenced) {
                showReferencedAlert(game, referenced.getBlockingReferences());
                statusLabel.setText("Suppression refusée : jeu encore référencé.");
                return;
            }
            statusLabel.setText("Erreur lors de la suppression : " + ex.getMessage());
        }));

        new Thread(task, "manage-games-delete").start();
    }

    private void showReferencedAlert(GameSummaryResult game, Map<String, Long> blocking) {
        StringBuilder body = new StringBuilder();
        body.append("Impossible de supprimer \"").append(game.getTitle())
            .append("\" : il est encore référencé par :\n\n");
        if (blocking.isEmpty()) {
            body.append("• des données utilisateurs");
        } else {
            for (Map.Entry<String, Long> entry : blocking.entrySet()) {
                body.append("• ").append(frenchLabel(entry.getKey()))
                    .append(" : ").append(entry.getValue()).append("\n");
            }
        }
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Suppression impossible");
        alert.setHeaderText("Ce jeu est référencé.");
        alert.setContentText(body.toString());
        alert.showAndWait();
    }

    private static String frenchLabel(String key) {
        return switch (key) {
            case "library" -> "bibliothèques utilisateurs";
            case "playLogs" -> "sessions de jeu";
            case "reviews" -> "critiques";
            case "backlogs" -> "backlogs";
            case "wishlists" -> "wishlists";
            case "favorites" -> "favoris";
            case "ratings" -> "notes";
            case "likes" -> "likes";
            case "listEntries" -> "listes personnalisées";
            case "dlcs" -> "DLC enfants";
            default -> key;
        };
    }

    private boolean isUnauthorized(Throwable ex) {
        return ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException;
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }
}

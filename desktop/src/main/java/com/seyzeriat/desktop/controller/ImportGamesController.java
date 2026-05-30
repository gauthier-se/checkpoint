package com.seyzeriat.desktop.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.ExternalGameResult;
import com.seyzeriat.desktop.service.GameService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Import Games view.
 * Allows searching for external games and importing them into the local database.
 */
public class ImportGamesController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private FlowPane resultsPane;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator searchProgress;
    @FXML private ScrollPane scrollPane;

    private final GameService gameService;
    private final Set<Long> importedIds = new HashSet<>();
    private HelloApplication application;

    public ImportGamesController(GameService gameService) {
        this.gameService = gameService;
    }

    @FXML
    public void initialize() {
        searchProgress.setVisible(false);
        statusLabel.setText("Recherchez des jeux pour commencer l'import.");

        // Allow pressing Enter to search
        searchField.setOnAction(event -> onSearch());
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Veuillez entrer un mot-clé de recherche.");
            return;
        }

        searchProgress.setVisible(true);
        searchButton.setDisable(true);
        resultsPane.getChildren().clear();
        statusLabel.setText("Recherche en cours...");

        Task<List<ExternalGameResult>> searchTask = new Task<>() {
            @Override
            protected List<ExternalGameResult> call() throws Exception {
                return gameService.searchExternalGames(query, 20);
            }
        };

        searchTask.setOnSucceeded(event -> {
            List<ExternalGameResult> results = searchTask.getValue();
            searchProgress.setVisible(false);
            searchButton.setDisable(false);

            if (results.isEmpty()) {
                statusLabel.setText("Aucun résultat trouvé pour \"" + query + "\".");
            } else {
                statusLabel.setText(results.size() + " résultat(s) trouvé(s) pour \"" + query + "\".");
                displayResults(results);
            }
        });

        searchTask.setOnFailed(event -> {
            searchProgress.setVisible(false);
            searchButton.setDisable(false);
            Throwable ex = searchTask.getException();
            if (isUnauthorized(ex)) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        });

        new Thread(searchTask, "search-thread").start();
    }

    private void displayResults(List<ExternalGameResult> results) {
        resultsPane.getChildren().clear();

        for (ExternalGameResult game : results) {
            VBox card = createGameCard(game);
            resultsPane.getChildren().add(card);
        }
    }

    private VBox createGameCard(ExternalGameResult game) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(10));
        card.setPrefWidth(200);
        card.setMinWidth(200);
        card.setMaxWidth(200);
        card.getStyleClass().add("game-card");

        // Cover image
        ImageView coverView = new ImageView();
        coverView.setFitWidth(180);
        coverView.setFitHeight(240);
        coverView.setPreserveRatio(true);
        coverView.getStyleClass().add("game-cover");

        if (game.getCoverUrl() != null && !game.getCoverUrl().isEmpty()) {
            String imageUrl = game.getCoverUrl();
            // Ensure HTTPS and use bigger image
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            }
            imageUrl = imageUrl.replace("t_thumb", "t_cover_big");
            try {
                Image image = new Image(imageUrl, 180, 240, true, true, true);
                coverView.setImage(image);
            } catch (Exception e) {
                // Keep placeholder if image fails
            }
        }

        // Title
        Label titleLabel = new Label(game.getTitle() != null ? game.getTitle() : "Sans titre");
        titleLabel.getStyleClass().add("game-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);
        titleLabel.setAlignment(Pos.CENTER);

        // Release year
        String yearText = game.getReleaseYear() != null ? String.valueOf(game.getReleaseYear()) : "N/A";
        Label yearLabel = new Label(yearText);
        yearLabel.getStyleClass().add("game-year");

        // Import button
        Button importButton = new Button("Importer");
        importButton.getStyleClass().add("import-button");
        importButton.setMaxWidth(Double.MAX_VALUE);

        // Check if already imported
        if (importedIds.contains(game.getExternalId())) {
            importButton.setText("✓ Importé");
            importButton.setDisable(true);
            importButton.getStyleClass().add("imported");
        }

        // Import action
        importButton.setOnAction(event -> importGame(game, importButton));

        card.getChildren().addAll(coverView, titleLabel, yearLabel, importButton);
        return card;
    }

    private void importGame(ExternalGameResult game, Button importButton) {
        importButton.setDisable(true);
        importButton.setText("Import...");

        // Add a small spinner next to button text
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(16, 16);
        spinner.setMaxSize(16, 16);
        importButton.setGraphic(spinner);

        Task<Void> importTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                gameService.importGame(game.getExternalId());
                return null;
            }
        };

        importTask.setOnSucceeded(event -> Platform.runLater(() -> {
            importButton.setText("✓ Importé");
            importButton.setGraphic(null);
            importButton.setDisable(true);
            importButton.getStyleClass().add("imported");
            importedIds.add(game.getExternalId());
            statusLabel.setText("\"" + game.getTitle() + "\" importé avec succès !");
        }));

        importTask.setOnFailed(event -> Platform.runLater(() -> {
            Throwable ex = importTask.getException();
            if (isUnauthorized(ex)) {
                redirectToLogin();
                return;
            }
            importButton.setText("Importer");
            importButton.setGraphic(null);
            importButton.setDisable(false);
            statusLabel.setText("Erreur lors de l'import de \"" + game.getTitle() + "\" : " + ex.getMessage());
        }));

        new Thread(importTask, "import-thread").start();
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
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

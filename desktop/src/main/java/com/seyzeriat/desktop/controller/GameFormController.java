package com.seyzeriat.desktop.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.CatalogOption;
import com.seyzeriat.desktop.dto.GameDetailResult;
import com.seyzeriat.desktop.dto.GameFormPayload;
import com.seyzeriat.desktop.service.GameService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for the admin game form, reused for both create and edit modes.
 *
 * <p>{@link #setGameId(String)} drives the mode: {@code null} for create,
 * non-null for edit (pre-fills fields from {@code GET /api/games/{id}}).</p>
 */
public class GameFormController {

    @FXML private Label titleLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private TextField coverUrlField;
    @FXML private TextField artworkUrlField;
    @FXML private TextField trailerYoutubeIdField;
    @FXML private TextField timeNormallyField;
    @FXML private TextField timeHastilyField;
    @FXML private TextField timeCompletelyField;
    @FXML private DatePicker releaseDatePicker;
    @FXML private ListView<CatalogOption> genresList;
    @FXML private ListView<CatalogOption> platformsList;
    @FXML private ListView<CatalogOption> companiesList;

    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;

    private final GameService gameService;
    private HelloApplication application;

    public GameFormController(GameService gameService) {
        this.gameService = gameService;
    }
    private String gameId;

    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);
        genresList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        platformsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        companiesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    /**
     * Sets the form mode. Pass {@code null} for create; an existing game ID
     * for edit (will fetch and pre-fill).
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
        boolean editMode = gameId != null;
        titleLabel.setText(editMode ? "Éditer un jeu" : "Créer un jeu");
        loadCatalogOptions(editMode);
    }

    private void loadCatalogOptions(boolean editMode) {
        loadingIndicator.setVisible(true);
        saveButton.setDisable(true);
        statusLabel.setText("Chargement des options...");

        Task<CatalogBundle> task = new Task<>() {
            @Override
            protected CatalogBundle call() throws Exception {
                List<CatalogOption> genres = gameService.getGenres();
                List<CatalogOption> platforms = gameService.getPlatforms();
                List<CatalogOption> companies = gameService.getCompanies();
                GameDetailResult detail = editMode ? gameService.getGameDetail(gameId) : null;
                return new CatalogBundle(genres, platforms, companies, detail);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            CatalogBundle bundle = task.getValue();
            genresList.getItems().setAll(bundle.genres());
            platformsList.getItems().setAll(bundle.platforms());
            companiesList.getItems().setAll(bundle.companies());
            if (bundle.detail() != null) {
                applyDetail(bundle.detail());
            }
            loadingIndicator.setVisible(false);
            saveButton.setDisable(false);
            statusLabel.setText("");
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            saveButton.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur lors du chargement : " + ex.getMessage());
        }));

        new Thread(task, "game-form-load").start();
    }

    private void applyDetail(GameDetailResult detail) {
        titleField.setText(nullToEmpty(detail.getTitle()));
        descriptionField.setText(nullToEmpty(detail.getDescription()));
        coverUrlField.setText(nullToEmpty(detail.getCoverUrl()));
        artworkUrlField.setText(nullToEmpty(detail.getArtworkUrl()));
        trailerYoutubeIdField.setText(nullToEmpty(detail.getTrailerYoutubeId()));
        timeNormallyField.setText(detail.getTimeToBeatNormally() == null ? "" : String.valueOf(detail.getTimeToBeatNormally()));
        timeHastilyField.setText(detail.getTimeToBeatHastily() == null ? "" : String.valueOf(detail.getTimeToBeatHastily()));
        timeCompletelyField.setText(detail.getTimeToBeatCompletely() == null ? "" : String.valueOf(detail.getTimeToBeatCompletely()));
        releaseDatePicker.setValue(detail.getReleaseDate());
        preselect(genresList, detail.getGenres());
        preselect(platformsList, detail.getPlatforms());
        preselect(companiesList, detail.getCompanies());
    }

    private static void preselect(ListView<CatalogOption> list, List<GameDetailResult.NamedRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        Set<String> targetIds = refs.stream()
                .map(GameDetailResult.NamedRef::getId)
                .collect(Collectors.toSet());
        for (CatalogOption option : list.getItems()) {
            if (targetIds.contains(option.getId())) {
                list.getSelectionModel().select(option);
            }
        }
    }

    @FXML
    private void onBack() {
        if (application == null) {
            return;
        }
        try {
            FXMLLoader loader = application.createLoader("/com/seyzeriat/desktop/manage-games-view.fxml");
            Node view = loader.load();
            ManageGamesController controller = loader.getController();
            controller.setApplication(application);
            application.setContent(view);
        } catch (IOException e) {
            statusLabel.setText("Erreur lors du retour : " + e.getMessage());
        }
    }

    @FXML
    private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            statusLabel.setText("Le titre est obligatoire.");
            return;
        }

        Long timeNormally = parseOptionalLong(timeNormallyField.getText(), "temps normal");
        Long timeHastily = parseOptionalLong(timeHastilyField.getText(), "temps rapide");
        Long timeCompletely = parseOptionalLong(timeCompletelyField.getText(), "temps complet");
        if (timeNormally == null && !timeNormallyField.getText().isBlank()) return;
        if (timeHastily == null && !timeHastilyField.getText().isBlank()) return;
        if (timeCompletely == null && !timeCompletelyField.getText().isBlank()) return;

        GameFormPayload payload = new GameFormPayload();
        payload.setTitle(title);
        payload.setDescription(nullIfBlank(descriptionField.getText()));
        payload.setCoverUrl(nullIfBlank(coverUrlField.getText()));
        payload.setArtworkUrl(nullIfBlank(artworkUrlField.getText()));
        payload.setTrailerYoutubeId(nullIfBlank(trailerYoutubeIdField.getText()));
        payload.setTimeToBeatNormally(timeNormally);
        payload.setTimeToBeatHastily(timeHastily);
        payload.setTimeToBeatCompletely(timeCompletely);
        payload.setReleaseDate(releaseDatePicker.getValue());
        payload.setGenreIds(collectSelectedIds(genresList));
        payload.setPlatformIds(collectSelectedIds(platformsList));
        payload.setCompanyIds(collectSelectedIds(companiesList));

        loadingIndicator.setVisible(true);
        saveButton.setDisable(true);
        cancelButton.setDisable(true);
        statusLabel.setText(gameId == null ? "Création..." : "Mise à jour...");

        Task<GameDetailResult> task = new Task<>() {
            @Override
            protected GameDetailResult call() throws Exception {
                return gameId == null
                        ? gameService.createGame(payload)
                        : gameService.updateGame(gameId, payload);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            statusLabel.setText("Enregistrement effectué.");
            onBack();
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            saveButton.setDisable(false);
            cancelButton.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                redirectToLogin();
                return;
            }
            statusLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(task, "game-form-save").start();
    }

    private Long parseOptionalLong(String text, String fieldLabel) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(text.trim());
            if (value < 0) {
                statusLabel.setText("Le champ \"" + fieldLabel + "\" doit être positif.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            statusLabel.setText("Le champ \"" + fieldLabel + "\" doit être un nombre.");
            return null;
        }
    }

    private static Set<String> collectSelectedIds(ListView<CatalogOption> list) {
        return new HashSet<>(list.getSelectionModel().getSelectedItems().stream()
                .map(CatalogOption::getId)
                .toList());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String nullIfBlank(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }

    private record CatalogBundle(
            List<CatalogOption> genres,
            List<CatalogOption> platforms,
            List<CatalogOption> companies,
            GameDetailResult detail
    ) {}
}

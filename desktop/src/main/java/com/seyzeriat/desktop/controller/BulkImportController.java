package com.seyzeriat.desktop.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.dto.ImportJobStatus;
import com.seyzeriat.desktop.service.GameService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * Controller for the Bulk Import view.
 * Lets an admin batch-import top-rated or recent games from IGDB.
 *
 * <p>Imports run asynchronously on the server: starting one returns a job id,
 * which this controller polls until the job reaches a terminal state, updating
 * a live progress label as it goes.</p>
 */
public class BulkImportController {

    private static final int SAFE_LIMIT_THRESHOLD = 200;
    private static final long POLL_INTERVAL_MS = 2000L;

    @FXML private Spinner<Integer> topRatedLimitSpinner;
    @FXML private Spinner<Integer> minRatingCountSpinner;
    @FXML private Button topRatedStartButton;
    @FXML private Label topRatedStatusLabel;

    @FXML private Spinner<Integer> recentLimitSpinner;
    @FXML private Button recentStartButton;
    @FXML private Label recentStatusLabel;

    @FXML private ProgressIndicator globalProgress;
    @FXML private Label globalStatusLabel;

    private final GameService gameService;
    private HelloApplication application;

    public BulkImportController(GameService gameService) {
        this.gameService = gameService;
    }

    /** Guards the polling loop so it can be stopped (e.g. when a new import starts). */
    private volatile boolean polling;

    @FXML
    public void initialize() {
        topRatedLimitSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5000, 100));
        minRatingCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 100));
        recentLimitSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 100));

        globalProgress.setVisible(false);
        globalStatusLabel.setText("");
        topRatedStatusLabel.setText("");
        recentStatusLabel.setText("");
    }

    @FXML
    private void onStartTopRated() {
        int limit = topRatedLimitSpinner.getValue();
        int minRatingCount = minRatingCountSpinner.getValue();

        if (!confirmLargeImport(limit)) {
            return;
        }

        startImport(
                topRatedStatusLabel,
                "Import des jeux les mieux notés en cours...",
                () -> gameService.startTopRatedImport(limit, minRatingCount)
        );
    }

    @FXML
    private void onStartRecent() {
        int limit = recentLimitSpinner.getValue();

        if (!confirmLargeImport(limit)) {
            return;
        }

        startImport(
                recentStatusLabel,
                "Import des sorties récentes en cours...",
                () -> gameService.startRecentImport(limit)
        );
    }

    private boolean confirmLargeImport(int limit) {
        if (limit <= SAFE_LIMIT_THRESHOLD) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Import volumineux");
        alert.setContentText("Vous allez importer " + limit + " jeux. L'opération peut prendre plusieurs minutes. Continuer ?");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Starts an import job and polls its status on a background thread until it
     * reaches a terminal state, updating the UI on the JavaFX thread.
     */
    private void startImport(Label cardStatusLabel, String runningMessage, JobStarter starter) {
        setRunning(true, runningMessage);
        cardStatusLabel.setText("");
        polling = true;

        Thread worker = new Thread(() -> {
            try {
                ImportJobStatus job = starter.start();
                String jobId = job.getJobId();
                Platform.runLater(() -> cardStatusLabel.setText(formatProgress(job)));

                while (polling) {
                    ImportJobStatus status = gameService.getImportJob(jobId);
                    if (status == null) {
                        Platform.runLater(() -> {
                            setRunning(false, "");
                            cardStatusLabel.setText("Statut indisponible (job introuvable).");
                        });
                        return;
                    }
                    Platform.runLater(() -> cardStatusLabel.setText(formatProgress(status)));

                    if (status.isTerminal()) {
                        Platform.runLater(() -> finish(cardStatusLabel, status));
                        return;
                    }
                    Thread.sleep(POLL_INTERVAL_MS);
                }
            } catch (com.seyzeriat.desktop.exception.UnauthorizedException ex) {
                Platform.runLater(this::redirectToLogin);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setRunning(false, "");
                    cardStatusLabel.setText("Erreur : " + describeFailure(ex));
                    globalStatusLabel.setText("Échec de l'import.");
                });
            } finally {
                polling = false;
            }
        }, "bulk-import-poll");
        worker.setDaemon(true);
        worker.start();
    }

    private void finish(Label cardStatusLabel, ImportJobStatus status) {
        setRunning(false, "");
        cardStatusLabel.setText(formatSummary(status));
        globalStatusLabel.setText(status.isFailed() ? "Échec de l'import." : "Import terminé.");
    }

    private String describeFailure(Throwable ex) {
        return ex != null && ex.getMessage() != null ? ex.getMessage() : "erreur inconnue";
    }

    private void setRunning(boolean running, String message) {
        topRatedStartButton.setDisable(running);
        recentStartButton.setDisable(running);
        globalProgress.setVisible(running);
        globalStatusLabel.setText(message);
    }

    /** Live one-liner shown while the job runs. */
    private String formatProgress(ImportJobStatus status) {
        StringBuilder sb = new StringBuilder();
        if (status.getTotalFetched() > 0) {
            sb.append(status.getProcessed()).append(" / ").append(status.getTotalFetched()).append(" traités");
        } else {
            sb.append(status.getProcessed()).append(" traités");
        }
        sb.append(" — ").append(status.getImported()).append(" importé(s), ")
                .append(status.getSkipped()).append(" ignoré(s), ")
                .append(status.getFailed()).append(" échec(s)");
        return sb.toString();
    }

    /** Final summary shown once the job is terminal. */
    private String formatSummary(ImportJobStatus status) {
        if (status.isFailed()) {
            String reason = status.getErrorMessage() != null ? status.getErrorMessage() : "erreur inconnue";
            return "Échec : " + reason;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(status.getImported()).append(" importé(s), ");
        sb.append(status.getSkipped()).append(" ignoré(s) (déjà existants), ");
        sb.append(status.getFailed()).append(" échec(s)");
        sb.append(" sur ").append(status.getTotalFetched()).append(" récupéré(s).");

        List<String> errors = status.getErrors();
        if (errors != null && !errors.isEmpty()) {
            int shown = Math.min(errors.size(), 3);
            sb.append(" Échecs : ");
            for (int i = 0; i < shown; i++) {
                if (i > 0) sb.append(", ");
                sb.append(errors.get(i));
            }
            if (errors.size() > shown) {
                sb.append("…");
            }
        }
        return sb.toString();
    }

    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    private void redirectToLogin() {
        if (application != null) {
            application.showLoginView();
        }
    }

    @FunctionalInterface
    private interface JobStarter {
        ImportJobStatus start() throws IOException, InterruptedException, com.seyzeriat.desktop.exception.UnauthorizedException;
    }
}

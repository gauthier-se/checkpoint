package com.seyzeriat.desktop.controller;

import com.seyzeriat.desktop.dto.NewsRequestPayload;
import com.seyzeriat.desktop.dto.NewsResult;
import com.seyzeriat.desktop.service.NewsService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the news editor dialog. Used in two modes:
 *
 * <ul>
 *   <li>Create — call {@link #setNews(NewsResult)} with {@code null}.</li>
 *   <li>Edit — call {@link #setNews(NewsResult)} with the existing news.</li>
 * </ul>
 *
 * <p>The {@code onSaved} callback is invoked once the news has been
 * successfully created or updated, so the parent view can refresh.</p>
 */
public class NewsEditorDialogController {

    @FXML private Label titleLabel;
    @FXML private TextField titleField;
    @FXML private TextField pictureField;
    @FXML private TextArea contentArea;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final NewsService newsService;

    private NewsResult existing;
    private Runnable onSaved;
    private Runnable onUnauthorized;

    /**
     * Constructs the controller with the specified news service.
     *
     * @param newsService the service used to handle news operations
     */
    public NewsEditorDialogController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    public void initialize() {
        loadingIndicator.setVisible(false);
        errorLabel.setText("");
    }

    /**
     * Configures the dialog. Pass {@code null} to create a new news article,
     * or an existing {@link NewsResult} to edit it.
     *
     * @param news the news to edit, or {@code null} for creation
     */
    public void setNews(NewsResult news) {
        this.existing = news;
        if (news == null) {
            titleLabel.setText("Nouvelle actualité");
            titleField.setText("");
            pictureField.setText("");
            contentArea.setText("");
        } else {
            titleLabel.setText("Modifier l'actualité");
            titleField.setText(news.getTitle() != null ? news.getTitle() : "");
            pictureField.setText(news.getPicture() != null ? news.getPicture() : "");
            contentArea.setText(news.getDescription() != null ? news.getDescription() : "");
        }
    }

    /**
     * Sets a callback invoked after a successful save (create or update).
     * The callback runs on the JavaFX application thread.
     *
     * @param onSaved the callback to invoke
     */
    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    /**
     * Sets a callback invoked when the API returns 401/403, so the parent
     * can redirect to the login view.
     *
     * @param onUnauthorized the callback to invoke
     */
    public void setOnUnauthorized(Runnable onUnauthorized) {
        this.onUnauthorized = onUnauthorized;
    }

    @FXML
    private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            errorLabel.setText("Le titre est obligatoire.");
            return;
        }
        errorLabel.setText("");

        String picture = pictureField.getText() == null ? "" : pictureField.getText().trim();
        String content = contentArea.getText() == null ? "" : contentArea.getText();

        NewsRequestPayload payload = new NewsRequestPayload(
                title,
                content,
                picture.isEmpty() ? null : picture);

        setLoading(true);

        Task<NewsResult> saveTask = new Task<>() {
            @Override
            protected NewsResult call() throws Exception {
                if (existing == null) {
                    return newsService.createNews(payload);
                }
                return newsService.updateNews(existing.getId(), payload);
            }
        };

        saveTask.setOnSucceeded(event -> Platform.runLater(() -> {
            setLoading(false);
            if (onSaved != null) {
                onSaved.run();
            }
            closeDialog();
        }));

        saveTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = saveTask.getException();
            if (ex instanceof com.seyzeriat.desktop.exception.UnauthorizedException) {
                if (onUnauthorized != null) {
                    onUnauthorized.run();
                }
                closeDialog();
                return;
            }
            errorLabel.setText("Erreur : " + ex.getMessage());
        }));

        new Thread(saveTask, "save-news-thread").start();
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        saveButton.setDisable(loading);
        cancelButton.setDisable(loading);
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}

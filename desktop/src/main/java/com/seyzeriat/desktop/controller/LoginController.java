package com.seyzeriat.desktop.controller;

import com.seyzeriat.desktop.HelloApplication;
import com.seyzeriat.desktop.service.AuthenticationService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * Controller for the Login view.
 *
 * <p>Sends credentials to {@code POST /api/v1/auth/token}, expects a JSON response
 * containing a JWT, and stores it via {@link com.seyzeriat.desktop.service.TokenManager}.
 * On success, navigates to the main application view.</p>
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loginProgress;

    private final AuthenticationService authService;

    /**
     * Constructs the controller with the specified authentication service.
     *
     * @param authService the service used for user authentication
     */
    public LoginController(AuthenticationService authService) {
        this.authService = authService;
    }

    /**
     * Reference to the application instance, set by {@link HelloApplication}
     * so the controller can trigger navigation after login.
     */
    private HelloApplication application;

    /**
     * Sets the main application instance.
     *
     * @param application the main application instance
     */
    public void setApplication(HelloApplication application) {
        this.application = application;
    }

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    public void initialize() {
        errorLabel.setText("");
        // Allow pressing Enter in the password field to trigger login
        passwordField.setOnAction(event -> onLogin());
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Client-side validation
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        setLoading(true);
        errorLabel.setText("");

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                authService.login(email, password);
                return null;
            }
        };

        loginTask.setOnSucceeded(event -> Platform.runLater(() -> {
            setLoading(false);
            if (application != null) {
                application.showMainView();
            }
        }));

        loginTask.setOnFailed(event -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = loginTask.getException();
            showError(ex.getMessage());
        }));

        new Thread(loginTask, "login-thread").start();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginProgress.setVisible(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
    }
}

package com.finsec.accounting.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.finsec.accounting.ui.client.ApiClient;

import java.io.InputStream;

public class JavaFxApp extends Application {

    private final ApiClient apiClient = new ApiClient();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FinSec Accounting");

        // Load logo from resources folder
        InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        } else {
            System.out.println("Logo not found! Check if src/main/resources/images/logo.png exists.");
        }

        // Header
        Label headerLabel = new Label("FinSec Portal Login");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Inputs
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Sign In");
        Button signUpButton = new Button("Sign Up");
        signUpButton.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");

        Label statusLabel = new Label();

        // Trigger login & Open DashboardView
        loginButton.setOnAction(e -> {
            boolean success = apiClient.login(usernameField.getText(), passwordField.getText());
            if (success) {
                DashboardView dashboard = new DashboardView(apiClient);
                dashboard.show(primaryStage);
            } else {
                statusLabel.setText("Invalid credentials or server down.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });

        signUpButton.setOnAction(e -> showRegisterDialog(primaryStage));

        HBox buttonBox = new HBox(10, loginButton, signUpButton);

        // Layout setup
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        grid.add(headerLabel, 0, 0, 2, 1);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(buttonBox, 1, 3);
        grid.add(statusLabel, 0, 4, 2, 1);

        Scene scene = new Scene(grid, 420, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showRegisterDialog(Stage stage) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Account");
        dialog.setHeaderText("Sign Up for FinSec");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        // Security Warning Label
        Label securityNotice = new Label("⚠️ Security Note: Do NOT use your personal Google or email password!");
        securityNotice.setStyle("-fx-text-fill: #E67E22; -fx-font-size: 11px; -fx-font-weight: bold;");
        securityNotice.setWrapText(true);

        VBox content = new VBox(10,
                new Label("Username:"), usernameField,
                new Label("Email:"), emailField,
                new Label("Password:"), passwordField,
                new Label("Confirm Password:"), confirmPasswordField,
                securityNotice);
        content.setPadding(new Insets(15));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(button -> {
            if (button == ButtonType.OK) {
                String user = usernameField.getText().trim();
                String email = emailField.getText().trim();
                String pass = passwordField.getText().trim();
                String confirmPass = confirmPasswordField.getText().trim();

                if (user.isBlank() || email.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "All fields are required!");
                    alert.show();
                    return;
                }

                if (!pass.equals(confirmPass)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Passwords do not match!");
                    alert.show();
                    return;
                }

                apiClient.registerAsync(user, email, pass)
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 201) {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Account created successfully! You can now log in.");
                                alert.show();
                            } else {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Registration failed: " + res.body());
                                alert.show();
                            }
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).show());
                            return null;
                        });
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
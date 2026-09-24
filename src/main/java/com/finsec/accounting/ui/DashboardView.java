package com.finsec.accounting.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsec.accounting.dto.TransactionSummaryDTO;
import com.finsec.accounting.model.Organization;
import com.finsec.accounting.model.Transaction;
import com.finsec.accounting.ui.client.ApiClient;
import com.finsec.accounting.ui.components.BulkTransactionModal;
import com.finsec.accounting.ui.components.SummaryCard;
import com.finsec.accounting.ui.components.TransactionModal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DashboardView {

    private final ApiClient apiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionSummaryDTO latestSummaryDTO;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    private final SummaryCard bbfCard = new SummaryCard("Opening Balance (BBF)", "₦0.00", "#2980B9");
    private final SummaryCard incomeCard = new SummaryCard("Total Income", "₦0.00", "#27AE60");
    private final SummaryCard expenseCard = new SummaryCard("Total Expense", "₦0.00", "#E74C3C");
    private final SummaryCard closingCard = new SummaryCard("Closing Balance", "₦0.00", "#8E44AD");

    private Button newTransactionBtn;
    private Button bulkTransactionBtn;
    private Button deleteOrgBtn;
    private Button addMemberBtn;

    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker endDatePicker = new DatePicker();
    private final TextArea categoryBreakdownArea = new TextArea();
    private final TableView<Transaction> transactionTable = new TableView<>();
    private ComboBox<Organization> orgDropdown = new ComboBox<>();

    public DashboardView(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void show(Stage stage) {
        stage.setTitle("FinSec Accounting - Dashboard");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #F8F9FA;");

        // 1. Header Bar with Organization Dropdown & New Org Button
        HBox headerBar = new HBox(10);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label("Financial Dashboard");
        headerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        // Dynamic Organization Dropdown (hardcoded items removed)
        orgDropdown.setPromptText("Select Org");
        orgDropdown.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        orgDropdown.setOnAction(e -> {
            // Change String to Organization
            Organization selectedOrg = orgDropdown.getValue();

            // Extract the ID from the Organization object to check against the session
            if (selectedOrg != null && !selectedOrg.getId().equals(UserSession.getInstance().getActiveOrganizationId())) {
                UserSession.getInstance().setActiveOrganizationId(selectedOrg.getId());
                refreshAllData();
            }
        });

        // + New Org Button
        Button newOrgBtn = new Button("+ New Org");
        newOrgBtn.setStyle("-fx-background-color: #34495E; -fx-text-fill: white; -fx-font-weight: bold;");
        newOrgBtn.setOnAction(e -> promptCreateOrganization());

        addMemberBtn = new Button("+ Add Member");
        addMemberBtn.setStyle("-fx-background-color: #8E44AD; -fx-text-fill: white; -fx-font-weight: bold;");
        addMemberBtn.setOnAction(e -> promptAddMember());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        newTransactionBtn = new Button("+ Record Transaction");
        newTransactionBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold;");
        newTransactionBtn.setOnAction(e -> openTransactionModal(stage));

        bulkTransactionBtn = new Button("+ Bulk Record");
        bulkTransactionBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold;");
        bulkTransactionBtn.setOnAction(e -> openBulkTransactionModal(stage));

        Button deleteUserBtn = new Button("Delete Account");
        deleteUserBtn.setStyle("-fx-background-color: #7F8C8D; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteUserBtn.setOnAction(e -> promptDeleteUser(stage));

        // Inside DashboardView.java -> show(Stage stage) Section 1:
        deleteOrgBtn = new Button("Delete Org");
        deleteOrgBtn.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteOrgBtn.setOnAction(e -> promptDeleteOrganization());

        // Update headerBar.getChildren().addAll(...) to include addMemberBtn:
        headerBar.getChildren().addAll(headerTitle, orgDropdown, newOrgBtn, addMemberBtn, deleteOrgBtn, spacer, newTransactionBtn, bulkTransactionBtn, deleteUserBtn);

        // 2. Date Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        startDatePicker.setPromptText("Start Date (YYYY-MM-DD)");
        endDatePicker.setPromptText("End Date (YYYY-MM-DD)");

        Button filterBtn = new Button("Apply Range");
        filterBtn.setStyle("-fx-background-color: #2C3E50; -fx-text-fill: white; -fx-font-weight: bold;");
        filterBtn.setOnAction(e -> refreshAllData());

        Button clearBtn = new Button("Reset All-Time");
        clearBtn.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refreshAllData();
        });

        // NEW: Dedicated Refresh Button
        Button refreshBtn = new Button("🔄 Refresh Data");
        refreshBtn.setStyle("-fx-background-color: #16A085; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> refreshAllData());

        filterBar.getChildren().addAll(
                new Label("From:"), startDatePicker,
                new Label("To:"), endDatePicker,
                filterBtn, clearBtn, refreshBtn
        );

        HBox exportPanel = createExportPanel();

        // 3. Summary Cards Row
        HBox cardsRow = new HBox(15);
        cardsRow.getChildren().addAll(bbfCard, incomeCard, expenseCard, closingCard);

        // 4. Category Breakdown Section
        VBox breakdownBox = new VBox(10);
        Label breakdownTitle = new Label("Period Category Breakdown");
        breakdownTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495E;");

        categoryBreakdownArea.setEditable(false);
        categoryBreakdownArea.setPrefRowCount(5);
        categoryBreakdownArea.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
        breakdownBox.getChildren().addAll(breakdownTitle, categoryBreakdownArea);

        // 5. Setup Transactions Table
        setupTransactionTable();

        VBox tableBox = new VBox(10);
        Label tableTitle = new Label("Recent Transactions (Right-click row to Edit / Delete)");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495E;");
        tableBox.getChildren().addAll(tableTitle, transactionTable);
        VBox.setVgrow(transactionTable, Priority.ALWAYS);

        root.getChildren().addAll(headerBar, filterBar, exportPanel, cardsRow, breakdownBox, tableBox);

        // Load organizations from database on launch
        loadOrganizations();

        Scene scene = new Scene(root, 1050, 750);
        stage.setScene(scene);
        stage.setTitle("FinSec Accounting Client");
        stage.show();
    }

    private void promptAddMember() {
        String activeOrgId = UserSession.getInstance().getActiveOrganizationId();
        if (activeOrgId == null || activeOrgId.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select an organization first.");
            alert.show();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Member");
        dialog.setHeaderText("Attach User to Active Organization");
        dialog.setContentText("Enter Username or Email:"); // <--- Clean user prompt

        dialog.showAndWait().ifPresent(identifier -> {
            if (!identifier.isBlank()) {
                apiClient.addUserToOrgAsync(activeOrgId, identifier.trim())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 201) {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "User added to organization successfully!");
                                alert.show();
                            } else {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add user: " + res.body());
                                alert.show();
                            }
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Network error: " + ex.getMessage()).show());
                            return null;
                        });
            }
        });
    }

    private void promptDeleteUser(Stage stage) {
        String activeUserId = UserSession.getInstance().getActiveUserId();
        if (activeUserId == null || activeUserId.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No active user session found.");
            alert.show();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete your account?\nThis will remove your membership across all organizations!",
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                apiClient.deleteUserAsync(activeUserId)
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 204) {
                                Alert info = new Alert(Alert.AlertType.INFORMATION, "Account deleted successfully.");
                                info.showAndWait();

                                // Clear session data
                                UserSession.getInstance().setActiveUserId(null);
                                UserSession.getInstance().setActiveOrganizationId(null);

                                // Relaunch login screen on existing stage
                                try {
                                    new JavaFxApp().start(stage);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to delete account: " + res.body());
                                error.show();
                            }
                        }));
            }
        });
    }

    private void promptDeleteOrganization() {
        Organization selectedOrg = orgDropdown.getValue();
        if (selectedOrg == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select an organization to delete.");
            alert.show();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete '" + selectedOrg.getName() + "'?\nThis will permanently delete all associated categories and transactions!",
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                apiClient.deleteOrganizationAsync(selectedOrg.getId())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 204) {
                                UserSession.getInstance().setActiveOrganizationId(null);
                                loadOrganizations();
                            } else {
                                Alert error = new Alert(Alert.AlertType.ERROR, "Failed to delete: " + res.body());
                                error.show();
                            }
                        }));
            }
        });
    }

    private void loadOrganizations() {
        // Tell the ComboBox to display the Organization's NAME
        orgDropdown.setConverter(new StringConverter<Organization>() {
            @Override
            public String toString(Organization org) {
                return org == null ? "" : org.getName();
            }
            @Override
            public Organization fromString(String string) {
                return null;
            }
        });

        // Add listener to update UserSession and toggle action buttons
        orgDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasOrg = (newVal != null);
            if (hasOrg) {
                UserSession.getInstance().setActiveOrganizationId(newVal.getId());
                refreshAllData();
            } else {
                UserSession.getInstance().setActiveOrganizationId(null);
            }

            // Toggle action buttons dynamically based on active org state
            updateButtonStates(hasOrg, newTransactionBtn, bulkTransactionBtn, deleteOrgBtn, addMemberBtn);
        });

        apiClient.getAllOrganizationsAsync()
                .thenAccept(orgs -> Platform.runLater(() -> {
                    orgDropdown.getItems().clear();

                    if (orgs != null && !orgs.isEmpty()) {
                        orgDropdown.getItems().addAll(orgs);

                        String currentId = UserSession.getInstance().getActiveOrganizationId();
                        Organization toSelect = orgs.get(0); // Default to first

                        // Find the organization that matches the active ID (if any)
                        if (currentId != null) {
                            for (Organization o : orgs) {
                                if (o.getId().equals(currentId)) {
                                    toSelect = o;
                                    break;
                                }
                            }
                        }

                        orgDropdown.setValue(toSelect); // Triggers the listener above
                    } else {
                        // No orgs exist for this user - explicitly disable action buttons
                        updateButtonStates(false, newTransactionBtn, bulkTransactionBtn, deleteOrgBtn, addMemberBtn);
                    }
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private void promptCreateOrganization() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Organization");
        dialog.setHeaderText("Register a New Community, Club, or Society");
        dialog.setContentText("Enter Organization Name:");

        dialog.showAndWait().ifPresent(orgName -> {
            if (!orgName.isBlank()) {
                apiClient.createOrganizationAsync(orgName.trim())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 201) {
                                // Reload the list so the new org appears in the dropdown
                                loadOrganizations();
                            } else {
                                Alert alert = new Alert(Alert.AlertType.ERROR,
                                        "Failed to create organization. Message: " + res.body());
                                alert.show();
                            }
                        }))
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Network error: " + ex.getMessage());
                                alert.show();
                            });
                            return null;
                        });
            }
        });
    }

    private HBox createExportPanel() {
        HBox exportBox = new HBox(10);
        exportBox.setStyle("-fx-padding: 15; -fx-background-color: #e8f4f8; -fx-border-color: #b8daff; -fx-border-radius: 5;");
        exportBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Export Reports:");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0c5460;");

        // Dropdown for Report Type
        ComboBox<String> reportTypeCombo = new ComboBox<>();
        reportTypeCombo.getItems().addAll("Comprehensive Ledger", "Monthly Summary", "Category Breakdown");
        reportTypeCombo.setValue("Monthly Summary"); // Default selection

        // Export Button
        Button btnExport = new Button("Download Excel");
        btnExport.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");

        btnExport.setOnAction(e -> {
            String activeOrgId = UserSession.getInstance().getActiveOrganizationId();

            if (activeOrgId == null || activeOrgId.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Missing Organization");
                alert.setHeaderText(null);
                alert.setContentText("Please select or log into an Organization before exporting.");
                alert.showAndWait();
                return;
            }

            // Open File Chooser so user can pick where to save it
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Excel Report");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            fileChooser.setInitialFileName(reportTypeCombo.getValue().replace(" ", "_") + ".xlsx");

            File file = fileChooser.showSaveDialog(exportBox.getScene().getWindow());

            if (file != null) {
                try {
                    // Pulls dates from the main date filters above it
                    apiClient.downloadExcelReport(
                            reportTypeCombo.getValue(),
                            startDatePicker.getValue(),
                            endDatePicker.getValue(),
                            activeOrgId,
                            file
                    );

                    // Show success message
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText("Report saved successfully to:\n" + file.getAbsolutePath());
                    alert.showAndWait();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to generate report");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        // Notice we are binding this to the main startDatePicker and endDatePicker for consistency
        exportBox.getChildren().addAll(title, new Label("Selected Dates Above -> Format:"), reportTypeCombo, btnExport);
        return exportBox;
    }

    private void updateButtonStates(boolean hasActiveOrg, Button... buttons) {
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setDisable(!hasActiveOrg);
            }
        }
    }

    private void setupTransactionTable() {
        TableColumn<Transaction, String> monthYearCol = new TableColumn<>("Month/Year");
        monthYearCol.setCellValueFactory(new PropertyValueFactory<>("monthYear"));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));

        TableColumn<Transaction, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        TableColumn<Transaction, BigDecimal> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Transaction, String> methodCol = new TableColumn<>("Payment Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        transactionTable.getColumns().addAll(monthYearCol, dateCol, categoryCol, amountCol, methodCol, descCol);
        transactionTable.setItems(transactionList);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        setupTableContextMenu();
    }

    private void setupTableContextMenu() {
        transactionTable.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            MenuItem editDateItem = new MenuItem("Edit Date...");
            editDateItem.setOnAction(e -> promptEditDate(row.getItem()));

            MenuItem editMonthYearItem = new MenuItem("Edit Month/Year...");
            editMonthYearItem.setOnAction(e -> promptEditMonthYear(row.getItem()));

            MenuItem deleteItem = new MenuItem("Delete Transaction");
            deleteItem.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            deleteItem.setOnAction(e -> confirmAndDelete(row.getItem()));

            contextMenu.getItems().addAll(editDateItem, editMonthYearItem, new SeparatorMenuItem(), deleteItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );
            return row;
        });
    }

    private void promptEditDate(Transaction transaction) {
        if (transaction == null) return;

        DatePicker datePicker = new DatePicker(transaction.getTransactionDate());
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Date");
        dialog.setHeaderText("Select new date for transaction");
        dialog.getDialogPane().setContent(new VBox(10, new Label("New Date:"), datePicker));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK && datePicker.getValue() != null) {
                apiClient.patchTransactionDate(transaction.getId(), datePicker.getValue().toString())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200) {
                                refreshAllData();
                            }
                        }));
            }
        });
    }

    private void promptEditMonthYear(Transaction transaction) {
        if (transaction == null) return;

        TextInputDialog dialog = new TextInputDialog(transaction.getMonthYear());
        dialog.setTitle("Update Month/Year");
        dialog.setHeaderText("Enter new Month/Year (e.g., August 2026):");
        dialog.setContentText("Month/Year:");

        dialog.showAndWait().ifPresent(newMonthYear -> {
            if (!newMonthYear.isBlank()) {
                apiClient.patchTransactionMonthYear(transaction.getId(), newMonthYear)
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200) {
                                refreshAllData();
                            }
                        }));
            }
        });
    }

    private void confirmAndDelete(Transaction transaction) {
        if (transaction == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this transaction?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                apiClient.deleteTransactionAsync(transaction.getId())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 200 || res.statusCode() == 204) {
                                refreshAllData();
                            } else {
                                System.err.println("Delete failed with status: " + res.statusCode() + " - " + res.body());
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Failed to delete (HTTP " + res.statusCode() + ")");
                                errorAlert.show();
                            }
                        }));
            }
        });
    }

    private void openTransactionModal(Stage parentStage) {
        try {
            Set<String> savedCategories = new HashSet<>();
            if (latestSummaryDTO != null && latestSummaryDTO.getIncomeCategoryBreakdown() != null) {
                savedCategories.addAll(latestSummaryDTO.getIncomeCategoryBreakdown().keySet());
            }

            TransactionModal modal = new TransactionModal(this.apiClient, parentStage, savedCategories);
            modal.showAndWait();

            refreshAllData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openBulkTransactionModal(Stage parentStage) {
        try {
            Set<String> savedCategories = new HashSet<>();
            if (latestSummaryDTO != null && latestSummaryDTO.getIncomeCategoryBreakdown() != null) {
                savedCategories.addAll(latestSummaryDTO.getIncomeCategoryBreakdown().keySet());
            }

            BulkTransactionModal modal = new BulkTransactionModal(this.apiClient, parentStage, savedCategories);
            modal.showAndWait();

            refreshAllData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshAllData() {
        if (UserSession.getInstance().getActiveOrganizationId() == null) {
            return; // Don't try to load data if no organization is selected yet
        }
        loadSummaryData();
        loadTableData();
    }

    private void loadSummaryData() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        String startDateStr = start != null ? start.toString() : null;
        String endDateStr = end != null ? end.toString() : null;

        // Call the ASYNC method
        apiClient.getSummaryAsync(startDateStr, endDateStr)
                .thenAccept(jsonResponse -> Platform.runLater(() -> {
                    try {
                        this.latestSummaryDTO = objectMapper.readValue(jsonResponse, TransactionSummaryDTO.class);

                        bbfCard.updateValue(formatCurrency(latestSummaryDTO.getOpeningBalance()));
                        incomeCard.updateValue(formatCurrency(latestSummaryDTO.getTotalIncome()));
                        expenseCard.updateValue(formatCurrency(latestSummaryDTO.getTotalExpenses()));
                        closingCard.updateValue(formatCurrency(latestSummaryDTO.getClosingBalance()));

                        StringBuilder sb = new StringBuilder();
                        boolean hasData = false;

                        if (latestSummaryDTO.getIncomeCategoryBreakdown() != null && !latestSummaryDTO.getIncomeCategoryBreakdown().isEmpty()) {
                            latestSummaryDTO.getIncomeCategoryBreakdown().forEach((category, amount) ->
                                    sb.append(String.format("%-25s : %s%n", category + " (I)", formatCurrency(amount)))
                            );
                            hasData = true;
                        }

                        if (latestSummaryDTO.getExpenseCategoryBreakdown() != null && !latestSummaryDTO.getExpenseCategoryBreakdown().isEmpty()) {
                            latestSummaryDTO.getExpenseCategoryBreakdown().forEach((category, amount) ->
                                    sb.append(String.format("%-25s : %s%n", category + " (E)", formatCurrency(amount)))
                            );
                            hasData = true;
                        }

                        if (!hasData) {
                            sb.append("No category records found for selected period.");
                        }
                        categoryBreakdownArea.setText(sb.toString());

                    } catch (Exception ex) {
                        categoryBreakdownArea.setText("Error parsing summary: " + ex.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    // Update UI safely on error
                    Platform.runLater(() -> categoryBreakdownArea.setText("Error loading summary: " + ex.getMessage()));
                    return null;
                });
    }

    private void loadTableData() {
        apiClient.getAllTransactionsAsync()
                .thenAccept(list -> Platform.runLater(() -> {
                    LocalDate start = startDatePicker.getValue();
                    LocalDate end = endDatePicker.getValue();

                    List<Transaction> filteredList = list;

                    if (start != null || end != null) {
                        java.time.YearMonth startMonth = start != null ? java.time.YearMonth.from(start) : null;
                        java.time.YearMonth endMonth = end != null ? java.time.YearMonth.from(end) : null;

                        java.time.format.DateTimeFormatter formatter = new java.time.format.DateTimeFormatterBuilder()
                                .parseCaseInsensitive()
                                .appendPattern("MMMM yyyy")
                                .toFormatter(Locale.ENGLISH);

                        filteredList = list.stream()
                                .filter(t -> {
                                    String my = t.getMonthYear();
                                    // Fallback if monthYear is null on the record
                                    if ((my == null || my.trim().isEmpty()) && t.getTransactionDate() != null) {
                                        my = t.getTransactionDate().format(formatter);
                                    }
                                    if (my == null || my.trim().isEmpty()) return false;

                                    try {
                                        java.time.YearMonth txMonth = java.time.YearMonth.parse(my.trim().replaceAll("\\s+", " "), formatter);
                                        boolean isAfterStart = (startMonth == null) || !txMonth.isBefore(startMonth);
                                        boolean isBeforeEnd = (endMonth == null) || !txMonth.isAfter(endMonth);
                                        return isAfterStart && isBeforeEnd;
                                    } catch (Exception e) {
                                        return false;
                                    }
                                })
                                .toList();
                    }

                    transactionList.setAll(filteredList);
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "NG"));
        return fmt.format(amount);
    }
}
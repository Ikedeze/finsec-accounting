package com.finsec.accounting.ui.components;

import com.finsec.accounting.dto.TransactionRequest;
import com.finsec.accounting.model.PaymentMethod;
import com.finsec.accounting.ui.client.ApiClient;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BulkTransactionModal extends Stage {

    private final ApiClient apiClient;
    private final VBox rowsContainer = new VBox(10);
    private final List<BulkRowInput> rowInputs = new ArrayList<>();

    private final ToggleGroup typeToggleGroup = new ToggleGroup();
    private final RadioButton incomeRadio = new RadioButton("INCOME BATCH");
    private final RadioButton expenseRadio = new RadioButton("EXPENSE BATCH");
    private final Button saveAllButton = new Button("Save All Records");

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    // NEW: Standard Default Categories matching TransactionModal
    private final List<String> defaultIncomeCategories = List.of(
            "Membership Dues", "Voluntary Donations", "Fundraising Proceeds", "Levies / Special Contributions", "Grants & Sponsorships"
    );

    private final List<String> defaultExpenseCategories = List.of(
            "Printing & Stationery", "Venue Hire & Event Expenses", "Refreshment & Catering", "Transport & Travel", "Office Supplies"
    );

    // NEW: User-saved categories passed from DashboardView
    private final Set<String> savedUserCategories;

    // UPDATED: Constructor now accepts savedUserCategories
    public BulkTransactionModal(ApiClient apiClient, Stage parentStage, Set<String> savedUserCategories) {
        this.apiClient = apiClient;
        this.savedUserCategories = savedUserCategories != null ? savedUserCategories : Collections.emptySet();

        initModality(Modality.APPLICATION_MODAL);
        initOwner(parentStage);
        setTitle("Bulk Transaction Entry");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #FFFFFF;");
        layout.setPrefSize(920, 500);

        Label titleLabel = new Label("Batch Record Transactions");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 1. Batch Type Selection
        incomeRadio.setToggleGroup(typeToggleGroup);
        expenseRadio.setToggleGroup(typeToggleGroup);
        incomeRadio.setSelected(true);
        HBox typeBox = new HBox(15, incomeRadio, expenseRadio);

        // NEW: Listener to update all row category dropdown options when switching batch types
        typeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            for (BulkRowInput row : rowInputs) {
                populateCategoryCombo(row.categoryCombo);
            }
        });

        // 2. Action Toolbar (Add Row button)
        Button addRowBtn = new Button("+ Add Entry Row");
        addRowBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-font-weight: bold;");
        addRowBtn.setOnAction(e -> addInputRow());

        HBox toolBar = new HBox(10, addRowBtn);

        // 3. Scrollable Rows Container
        ScrollPane scrollPane = new ScrollPane(rowsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Add 3 default rows to start
        addInputRow();
        addInputRow();
        addInputRow();

        // 4. Bottom Action Buttons
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> close());

        saveAllButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        saveAllButton.setOnAction(e -> handleSaveAll());

        HBox bottomBox = new HBox(10, cancelBtn, saveAllButton);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(
                titleLabel,
                typeBox,
                toolBar,
                scrollPane,
                bottomBox
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        setScene(new Scene(layout));
    }

    // NEW: Populate a row's ComboBox with saved user categories + defaults
    private void populateCategoryCombo(ComboBox<String> combo) {
        LinkedHashSet<String> combinedList = new LinkedHashSet<>(savedUserCategories);
        combinedList.addAll(incomeRadio.isSelected() ? defaultIncomeCategories : defaultExpenseCategories);
        combo.setItems(FXCollections.observableArrayList(combinedList));
        combo.getSelectionModel().clearSelection();
    }

    private void addInputRow() {
        BulkRowInput row = new BulkRowInput();
        populateCategoryCombo(row.categoryCombo); // NEW: Populate dropdown on row creation
        rowInputs.add(row);

        row.removeBtn.setOnAction(e -> {
            rowsContainer.getChildren().remove(row.rowLayout);
            rowInputs.remove(row);
        });

        rowsContainer.getChildren().add(row.rowLayout);
    }

    private void handleSaveAll() {
        List<TransactionRequest> requests = new ArrayList<>();

        for (BulkRowInput row : rowInputs) {
            TransactionRequest req = row.toTransactionRequest();
            if (req != null) {
                requests.add(req);
            }
        }

        if (requests.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill in at least one valid row (Amount and Category are required).");
            alert.show();
            return;
        }

        saveAllButton.setDisable(true);
        String endpoint = incomeRadio.isSelected() ? "/income/bulk" : "/expense/bulk";

        apiClient.sendBulkTransactions(endpoint, requests)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        System.out.println("Successfully saved " + requests.size() + " bulk transactions!");
                        close();
                    } else {
                        saveAllButton.setDisable(false);
                        System.err.println("Bulk save failed (" + response.statusCode() + "): " + response.body());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        saveAllButton.setDisable(false);
                        System.err.println("Connection failed: " + ex.getMessage());
                    });
                    return null;
                });
    }

    // UPDATED: Helper class now uses ComboBox<String> for categories instead of TextField
    private static class BulkRowInput {
        HBox rowLayout = new HBox(8);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField monthYearField = new TextField();
        TextField amountField = new TextField();

        // NEW: Category ComboBox instead of TextField
        ComboBox<String> categoryCombo = new ComboBox<>();

        ComboBox<PaymentMethod> methodCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.CASH, PaymentMethod.BANK));
        TextField descField = new TextField();
        Button removeBtn = new Button("X");

        BulkRowInput() {
            rowLayout.setAlignment(Pos.CENTER_LEFT);
            rowLayout.setPadding(new Insets(5));
            rowLayout.setStyle("-fx-background-color: #F2F4F4; -fx-background-radius: 5;");

            datePicker.setPrefWidth(125);
            monthYearField.setPrefWidth(110);
            amountField.setPrefWidth(90);
            amountField.setPromptText("0.00");

            // NEW: Setup Category ComboBox properties
            categoryCombo.setEditable(true);
            categoryCombo.setPrefWidth(160);
            categoryCombo.setPromptText("Select/Type Category...");

            methodCombo.setPrefWidth(90);
            methodCombo.setValue(PaymentMethod.CASH);
            descField.setPromptText("Description");
            HBox.setHgrow(descField, Priority.ALWAYS);

            removeBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold;");

            updateMonthYear(datePicker.getValue());
            datePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateMonthYear(newVal));

            // UPDATED: Added categoryCombo into layout
            rowLayout.getChildren().addAll(
                    datePicker, monthYearField, amountField, categoryCombo, methodCombo, descField, removeBtn
            );
        }

        private void updateMonthYear(LocalDate date) {
            if (date != null) {
                monthYearField.setText(date.format(MONTH_YEAR_FORMATTER));
            }
        }

        TransactionRequest toTransactionRequest() {
            String amount = amountField.getText().trim();

            // UPDATED: Pull value directly from ComboBox
            String category = categoryCombo.getValue() != null ? categoryCombo.getValue().trim() : "";

            if (amount.isEmpty() || category.isEmpty()) {
                return null; // Skip incomplete row
            }

            TransactionRequest req = new TransactionRequest();
            req.setTransactionDate(datePicker.getValue());
            req.setMonthYear(monthYearField.getText());
            req.setAmount(amount);
            req.setCategoryName(category);
            req.setPaymentMethod(methodCombo.getValue());
            req.setDescription(descField.getText());
            req.setRecordedBy("Desktop Admin (Bulk)");
            return req;
        }
    }
}
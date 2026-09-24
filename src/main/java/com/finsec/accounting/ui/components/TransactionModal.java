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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransactionModal extends Stage {

    private final ApiClient apiClient;

    private final ToggleGroup typeToggleGroup = new ToggleGroup();
    private final RadioButton incomeRadio = new RadioButton("INCOME");
    private final RadioButton expenseRadio = new RadioButton("EXPENSE");

    private final ToggleGroup paymentToggleGroup = new ToggleGroup();
    private final RadioButton cashRadio = new RadioButton("Cash at Hand");
    private final RadioButton bankRadio = new RadioButton("Bank Account");
    private final TextField bankDetailsField = new TextField();

    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField monthYearField = new TextField();
    private final TextField amountField = new TextField();
    private final ComboBox<String> categoryComboBox = new ComboBox<>();
    private final TextField descriptionField = new TextField();
    private final Button saveButton = new Button("Save Transaction");

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    // Standard Default Categories
    private final List<String> defaultIncomeCategories = List.of(
            "Membership Dues", "Voluntary Donations", "Fundraising Proceeds", "Levies / Special Contributions", "Grants & Sponsorships"
    );

    private final List<String> defaultExpenseCategories = List.of(
            "Printing & Stationery", "Venue Hire & Event Expenses", "Refreshment & Catering", "Transport & Travel", "Office Supplies"
    );

    // Collection of user-saved custom categories
    private final Set<String> savedUserCategories;

    public TransactionModal(ApiClient apiClient, Stage parentStage, Set<String> savedUserCategories) {
        this.apiClient = apiClient;
        this.savedUserCategories = savedUserCategories != null ? savedUserCategories : Collections.emptySet();

        initModality(Modality.APPLICATION_MODAL);
        initOwner(parentStage);
        setTitle("New Transaction Entry");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #FFFFFF;");
        layout.setPrefWidth(420);

        Label titleLabel = new Label("New Transaction");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 1. Type Switcher
        incomeRadio.setToggleGroup(typeToggleGroup);
        expenseRadio.setToggleGroup(typeToggleGroup);
        incomeRadio.setSelected(true);
        HBox typeBox = new HBox(10, incomeRadio, expenseRadio);

        // 2. Payment Destination
        cashRadio.setToggleGroup(paymentToggleGroup);
        bankRadio.setToggleGroup(paymentToggleGroup);
        cashRadio.setSelected(true);
        HBox paymentBox = new HBox(10, cashRadio, bankRadio);

        bankDetailsField.setPromptText("Enter Bank Name (e.g., Fidelity Bank PLC)");
        bankDetailsField.setVisible(false);
        bankDetailsField.setManaged(false);

        bankRadio.selectedProperty().addListener((obs, oldVal, isBank) -> {
            bankDetailsField.setVisible(isBank);
            bankDetailsField.setManaged(isBank);
            if (!isBank) bankDetailsField.clear();
        });

        // 3. Date & Month-Year
        datePicker.setMaxWidth(Double.MAX_VALUE);
        monthYearField.setPromptText("e.g. June 2025");
        updateMonthYearFromDate(datePicker.getValue());

        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) updateMonthYearFromDate(newDate);
        });

        // 4. Amount & Category Setup
        amountField.setPromptText("0.00");
        categoryComboBox.setEditable(true);
        categoryComboBox.setMaxWidth(Double.MAX_VALUE);
        categoryComboBox.setPromptText("Select or type custom category...");

        // Load Income categories initially (Saved first, Default below)
        updateCategoryOptions(defaultIncomeCategories);

        typeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (incomeRadio.isSelected()) {
                updateCategoryOptions(defaultIncomeCategories);
            } else {
                updateCategoryOptions(defaultExpenseCategories);
            }
            categoryComboBox.getSelectionModel().clearSelection();
        });

        // 5. Particulars
        descriptionField.setPromptText("Enter transaction details...");

        // Buttons
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> close());

        saveButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");
        saveButton.setOnAction(e -> handleSave());

        HBox buttonBox = new HBox(10, cancelButton, saveButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(
                titleLabel,
                new VBox(5, new Label("Transaction Type:"), typeBox),
                new VBox(5, new Label("Payment Destination:"), paymentBox, bankDetailsField),
                new VBox(5, new Label("Date:"), datePicker),
                new VBox(5, new Label("Month/Year Assignment:"), monthYearField),
                new VBox(5, new Label("Amount:"), amountField),
                new VBox(5, new Label("Category:"), categoryComboBox),
                new VBox(5, new Label("Description / Particulars:"), descriptionField),
                buttonBox
        );

        setScene(new Scene(layout));
    }

    /**
     * Places saved categories at the TOP and standard defaults BELOW
     */
    private void updateCategoryOptions(List<String> defaultList) {
        LinkedHashSet<String> combinedList = new LinkedHashSet<>();

        // 1. Add previously saved user categories first
        combinedList.addAll(savedUserCategories);

        // 2. Add standard default suggested categories below
        combinedList.addAll(defaultList);

        categoryComboBox.setItems(FXCollections.observableArrayList(combinedList));
    }

    private void updateMonthYearFromDate(LocalDate date) {
        if (date != null) {
            monthYearField.setText(date.format(MONTH_YEAR_FORMATTER));
        }
    }

    private void handleSave() {
        saveButton.setDisable(true);

        PaymentMethod method = cashRadio.isSelected() ? PaymentMethod.CASH : PaymentMethod.BANK;
        String bankDetails = bankRadio.isSelected() ? bankDetailsField.getText() : null;

        TransactionRequest request = new TransactionRequest();
        request.setPaymentMethod(method);
        request.setBankDetails(bankDetails);
        request.setTransactionDate(datePicker.getValue());
        request.setMonthYear(monthYearField.getText());
        request.setAmount(amountField.getText());
        request.setCategoryName(categoryComboBox.getValue());
        request.setDescription(descriptionField.getText());
        request.setRecordedBy("Desktop Admin");

        String endpoint = incomeRadio.isSelected() ? "/income" : "/expense";

        apiClient.sendTransaction(endpoint, request)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 201) {
                        System.out.println("Transaction saved with monthYear: " + request.getMonthYear());
                        close();
                    } else {
                        saveButton.setDisable(false);
                        System.err.println("Error (" + response.statusCode() + "): " + response.body());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        System.err.println("Connection Failed: " + ex.getMessage());
                    });
                    return null;
                });
    }
}
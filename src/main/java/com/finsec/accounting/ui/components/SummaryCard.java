package com.finsec.accounting.ui.components;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;

public class SummaryCard extends VBox {

    private final Label valueLabel = new Label("₦0.00");

    public SummaryCard(String title, String initialValue, String accentColorHex) {
        setPadding(new Insets(15));
        setSpacing(8);
        setPrefWidth(220);
        setMinHeight(100);
        setAlignment(Pos.CENTER_LEFT);

        // Modern CSS styling with left accent border
        setStyle(String.format(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-border-color: transparent transparent transparent %s; " +
                        "-fx-border-width: 0 0 0 5px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 3);",
                accentColorHex
        ));

        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7F8C8D;");

        valueLabel.setText(initialValue);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        getChildren().addAll(titleLabel, valueLabel);
    }

    public void updateValue(String newValue) {
        this.valueLabel.setText(newValue);
    }
}

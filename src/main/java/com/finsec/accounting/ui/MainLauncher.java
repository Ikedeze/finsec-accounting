package com.finsec.accounting.ui;

import com.finsec.accounting.FinSecApplication;

public class MainLauncher {
    public static void main(String[] args) {
        // 1. Start Spring Boot backend server in a background thread
        new Thread(() -> FinSecApplication.main(args)).start();

        // 2. Launch JavaFX Frontend
        JavaFxApp.main(args);
    }
}
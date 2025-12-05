package com.garden.sim.ui;

import com.garden.sim.api.*;
import com.garden.sim.core.logger.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application entry point for the Garden Simulator.
 * Initializes the simulation API and displays the dashboard UI.
 */
public class GardenApp extends Application {
    private static final String LOG_FILE = "log.txt";
    private static final int WINDOW_WIDTH = 1100;
    private static final int WINDOW_HEIGHT = 700;
    
    private final Logger log = new Logger(LOG_FILE);

    @Override
    public void start(Stage stage) {
        try {
            var api = new GertenSimulationImpl();
            var view = new DashboardView(api);
            stage.setTitle("Computerized Garden Simulator");
            stage.setScene(new Scene(view, WINDOW_WIDTH, WINDOW_HEIGHT));
            stage.show();
            api.initializeGarden();
        } catch (Exception e) {
            log.error("UI start failed", e);
            Platform.exit();
        }
    }

    /**
     * Application entry point.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        launch(args);
    }
}

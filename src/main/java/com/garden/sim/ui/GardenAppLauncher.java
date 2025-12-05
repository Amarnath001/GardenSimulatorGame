package com.garden.sim.ui;

/**
 * Launcher class for JavaFX application.
 * Required for JavaFX 11+ when running from IDE or as JAR.
 * This class is needed because JavaFX Application must extend Application,
 * but the main method cannot be in the Application class when using modules.
 */
public class GardenAppLauncher {
    public static void main(String[] args) {
        GardenApp.main(args);
    }
}



package com.garden.sim.ui;

import com.garden.sim.api.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import com.garden.sim.core.EventBus;
import com.garden.sim.core.Species;
import com.garden.sim.core.logger.Logger;
import com.garden.sim.core.factory.PlantType;
import java.util.*;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Stardew Valley-style 2D garden game with 6 planting spots.
 * Features: Plant seeds, watch them grow, harvest, manage resources.
 */
public class DashboardView extends BorderPane {
    // Constants
    private static final String FONT_ARIAL = "Arial";
    private static final String SEED_TOMATO = "Tomato";
    private static final String SEED_ROSE = "Rose";
    private static final String SEED_BASIL = "Basil";
    private static final String SEED_PEPPER = "Pepper";
    private static final String SEED_CUCUMBER = "Cucumber";
    private static final String SEED_LETTUCE = "Lettuce";
    private static final String SEED_CARROT = "Carrot";
    private static final String SEED_STRAWBERRY = "Strawberry";
    private static final String SEED_SUNFLOWER = "Sunflower";
    private static final String SEED_MARIGOLD = "Marigold";
    private static final String COINS_LABEL_PREFIX = "Coins: ";
    private static final int INITIAL_COINS = 100;
    private static final int INITIAL_DAY = 1;
    private static final int REFRESH_INTERVAL_SECONDS = 2;
    private static final int INDIVIDUAL_PLOT_LOG_INTERVAL_SECONDS = 30; // Log each occupied plot every 30 seconds
    private static final int PLOT_STATUS_SUMMARY_INTERVAL_SECONDS = 2400; // Log summary every 40 minutes (2400 seconds)
    
    private final GertenSimulationAPI api;
    private final GertenSimulationImpl impl;
    private final GridPane gardenGrid = new GridPane();
    private final Label statusLabel = new Label("Welcome to your garden! Select a seed and click a plot to plant.");
    private final Label dayLabel = new Label("Day: " + INITIAL_DAY);
    private final Label dateLabel = new Label();
    private final Label temperatureLabel = new Label("Temp: 72°F");
    private final Label realTimeLabel = new Label();
    private final Label coinsLabel = new Label(COINS_LABEL_PREFIX + INITIAL_COINS);
    private final TextArea logArea = new TextArea();
    private static final int MAX_LOG_LINES = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    
    // UI state (display only - all game logic in backend)
    private final Map<String, PlotData> plots = new HashMap<>(); // "row,col" -> PlotData (for display only)
    private int day = INITIAL_DAY;
    private int currentTemperature = 72; // Default temperature (synced from backend)
    private String selectedSeed = null; // No seed selected initially
    private Button seedButton; // Reference to seed selection button
    private Button clearSeedButton; // Reference to clear seed button
    private Button parasiteButton; // Reference to parasite selection button
    private Button clearParasiteButton; // Reference to clear parasite button
    private String selectedParasite = null; // Selected parasite for manual plot infection
    private String userName = "Player"; // User's name, default to "Player"
    private Label gardenTitleLabel; // Reference to garden title label
    
    // Plot data for UI display only (all state comes from backend)
    private static class PlotData {
        String plantName;
        String species;
        int growthStage = 0; // Synced from backend
        int health = 100; // Synced from backend
        int moisture = 50; // Synced from backend
        int daysPlanted = 0; // Synced from backend
        int daysToHarvest;  // Plant-specific harvest time
        List<String> parasites = new ArrayList<>(); // Synced from backend - list of parasite names
        boolean readyToHarvest = false; // Synced from backend
        
        PlotData(String plantName, String species, int daysToHarvest) {
            this.plantName = plantName;
            this.species = species;
            this.daysToHarvest = daysToHarvest;
        }
    }

    public DashboardView(GertenSimulationAPI api) {
        this.api = api;
        this.impl = (GertenSimulationImpl) api;
        
        // Set background
        setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #98D8C8 100%);");
        setPadding(new Insets(15));
        
        // Initialize date and real-time clock
        dateLabel.setText(LocalDate.now().format(DATE_FORMATTER));
        realTimeLabel.setText("Time: " + LocalTime.now().format(TIME_FORMATTER));
        
        // Build UI
        setTop(buildHeader());
        setLeft(buildLeftPanel());
        setCenter(buildGameArea());
        setBottom(buildStatusBar());
        setRight(buildSidePanel());
        
        // Initialize garden
        initializePlots();
        
        // Subscribe to events for log updates
        subscribeToEvents();
        
        // Apply log area styling after scene is ready
        Platform.runLater(() -> {
            var content = logArea.lookup(".content");
            if (content != null) {
                content.setStyle("-fx-background-color: #000000;");
            }
        });
        
        // Real-time clock: update every second
        Timeline clockTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                realTimeLabel.setText("Time: " + LocalTime.now().format(TIME_FORMATTER));
            })
        );
        clockTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clockTimeline.play();
        
        // Game loop: update every REFRESH_INTERVAL_SECONDS - sync UI from backend
        Timeline gameLoop = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> updateGame())
        );
        gameLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
        gameLoop.play();
        
        // Individual plot status logging: every 30 seconds (only occupied plots)
        Timeline individualPlotLogger = new Timeline(
            new KeyFrame(Duration.seconds(INDIVIDUAL_PLOT_LOG_INTERVAL_SECONDS), e -> logIndividualPlots())
        );
        individualPlotLogger.setCycleCount(javafx.animation.Animation.INDEFINITE);
        individualPlotLogger.play();
        
        // Plot status summary logging: every 40 minutes
        Timeline plotStatusSummaryLogger = new Timeline(
            new KeyFrame(Duration.seconds(PLOT_STATUS_SUMMARY_INTERVAL_SECONDS), e -> logPlotStatusSummary())
        );
        plotStatusSummaryLogger.setCycleCount(javafx.animation.Animation.INDEFINITE);
        plotStatusSummaryLogger.play();
        
        // Random parasite attacks are now handled by backend - no UI logic needed
    }

    private Node buildHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: rgba(139, 90, 43, 0.9); -fx-background-radius: 10;");
        
        Label title = new Label("🌱 Garden Simulator");
        title.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        
        dayLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        dayLabel.setTextFill(Color.WHITE);
        
        dateLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        dateLabel.setTextFill(Color.WHITE);
        
        temperatureLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        temperatureLabel.setTextFill(Color.WHITE);
        
        realTimeLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        realTimeLabel.setTextFill(Color.WHITE);
        
        coinsLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 16));
        coinsLabel.setTextFill(Color.GOLD);
        
        header.getChildren().addAll(title, dayLabel, dateLabel, temperatureLabel, realTimeLabel, coinsLabel);
        
        // Sync coins from backend
        updateCoinsDisplay();
        return header;
    }

    private Node buildLeftPanel() {
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setMinWidth(250);
        leftPanel.setPrefWidth(300);
        leftPanel.setMaxWidth(350);
        leftPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10;");
        
        Label panelTitle = new Label("⚙️ Controls");
        panelTitle.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 18));
        panelTitle.setTextFill(Color.DARKGREEN);
        
        // Seeds selection button
        Label seedLabel = new Label("🌱 Select Seed:");
        seedLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 12));
        
        HBox seedButtonContainer = new HBox(10);
        seedButtonContainer.setAlignment(Pos.CENTER_LEFT);
        
        seedButton = new Button("Select Plant");
        seedButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(seedButton, Priority.ALWAYS);
        seedButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        seedButton.setOnAction(e -> {
            e.consume();
            showSeedSelectionDialog();
        });
        
        clearSeedButton = new Button("Clear");
        clearSeedButton.setPrefWidth(60);
        clearSeedButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold;");
        clearSeedButton.setOnAction(e -> {
            e.consume();
            clearSeedSelection();
        });
        clearSeedButton.setVisible(false); // Initially hidden
        
        seedButtonContainer.getChildren().addAll(seedButton, clearSeedButton);
        updateSeedButtonDisplay(); // Initialize button display
        
        // Rain button with dialog
        Label rainLabel = new Label("🌧️ Rain:");
        rainLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 12));
        
        Button rainButton = new Button("Add Rain");
        rainButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rainButton, Priority.ALWAYS);
        rainButton.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-weight: bold;");
        rainButton.setOnAction(e -> {
            e.consume(); // Consume event to prevent double-triggering
            
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Rain Amount");
            dialog.setHeaderText("Enter Rain Amount");
            dialog.setContentText("Enter amount of rain (0-40):");
            
            // Create a custom TextField with numeric validation
            TextField inputField = dialog.getEditor();
            inputField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    inputField.setText(oldValue);
                }
            });
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isEmpty()) {
                try {
                    int amount = Integer.parseInt(result.get());
                    if (amount < 0 || amount > 40) {
                        Alert alert = new Alert(AlertType.WARNING);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Rain amount out of range");
                        alert.setContentText("Please enter a number between 0 and 40.");
                        alert.showAndWait();
                        return;
                    }
                    
                    // Trigger rain event (this will also log via event subscription)
                    api.rain(amount);
                    
                    // Update status message
                    if (amount == 0) {
                        statusLabel.setText("No rain added.");
                    } else if (amount < 10) {
                        statusLabel.setText("Light rain! Plants are being watered.");
                    } else if (amount < 25) {
                        statusLabel.setText("Moderate rain! Plants are well watered.");
                    } else {
                        statusLabel.setText("Heavy rain! Plants are very well watered.");
                    }
                    // Note: Rain event is already logged via RAIN event subscription, so we don't need to log again
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid number format");
                    alert.setContentText("Please enter a valid number between 0 and 40.");
                    alert.showAndWait();
                }
            }
        });
        
        // Temperature button with dialog
        Label tempLabel = new Label("🌡️ Temperature:");
        tempLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 12));
        
        Button tempButton = new Button("Set Temperature");
        tempButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tempButton, Priority.ALWAYS);
        tempButton.setStyle("-fx-background-color: #E67E22; -fx-text-fill: white; -fx-font-weight: bold;");
        tempButton.setOnAction(e -> {
            e.consume(); // Consume event to prevent double-triggering
            
            TextInputDialog dialog = new TextInputDialog("72");
            dialog.setTitle("Temperature Setting");
            dialog.setHeaderText("Enter Temperature");
            dialog.setContentText("Enter temperature (50-100°F):");
            
            // Create a custom TextField with numeric validation
            TextField inputField = dialog.getEditor();
            inputField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    inputField.setText(oldValue);
                }
            });
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isEmpty()) {
                try {
                    int temp = Integer.parseInt(result.get());
                    if (temp < 50 || temp > 100) {
                        Alert alert = new Alert(AlertType.WARNING);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Temperature out of range");
                        alert.setContentText("Please enter a temperature between 50 and 100°F.");
                        alert.showAndWait();
                        return;
                    }
                    
                    // Set temperature via backend - backend handles all logic
                    api.temperature(temp);
                    // Temperature change will be logged via TEMPERATURE event subscription
                    statusLabel.setText("Temperature set to " + temp + "°F");
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid number format");
                    alert.setContentText("Please enter a valid number between 50 and 100.");
                    alert.showAndWait();
                }
            }
        });
        
        // Parasite button with tile selection dialog
        Label parasiteLabel = new Label("🐛 Parasite Attack:");
        parasiteLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 12));
        
        HBox parasiteButtonContainer = new HBox(10);
        parasiteButtonContainer.setAlignment(Pos.CENTER_LEFT);
        
        parasiteButton = new Button("Select Parasite");
        parasiteButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(parasiteButton, Priority.ALWAYS);
        parasiteButton.setStyle("-fx-background-color: #8B4513; -fx-text-fill: white; -fx-font-weight: bold;");
        parasiteButton.setOnAction(e -> {
            e.consume();
            showParasiteSelectionDialog();
        });
        
        clearParasiteButton = new Button("Clear");
        clearParasiteButton.setPrefWidth(60);
        clearParasiteButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold;");
        clearParasiteButton.setOnAction(e -> {
            e.consume();
            clearParasiteSelection();
        });
        clearParasiteButton.setVisible(false); // Initially hidden
        
        parasiteButtonContainer.getChildren().addAll(parasiteButton, clearParasiteButton);
        updateParasiteButtonDisplay();
        
        // Actions section - two buttons
        Label actionLabel = new Label("⚙️ Actions:");
        actionLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 12));
        
        Button harvestAllButton = new Button("✂️ Harvest All Ready");
        harvestAllButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(harvestAllButton, Priority.ALWAYS);
        harvestAllButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        harvestAllButton.setOnAction(e -> {
            e.consume();
            harvestAllReady();
        });
        
        Button pestControlButton = new Button("🛡️ Pest Control");
        pestControlButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pestControlButton, Priority.ALWAYS);
        pestControlButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        pestControlButton.setOnAction(e -> {
            e.consume();
            triggerPestControl();
        });
        
        // FYI note about probabilistic nature
        Label pestControlNote = new Label("FYI: Treatment is probabilistic - efficacy 50-99% has 60% cure chance, 100% efficacy is guaranteed.");
        pestControlNote.setFont(Font.font(FONT_ARIAL, 9));
        pestControlNote.setTextFill(Color.GRAY);
        pestControlNote.setWrapText(true);
        pestControlNote.setMaxWidth(Double.MAX_VALUE);
        
        leftPanel.getChildren().addAll(
            panelTitle,
            new Separator(),
            seedLabel, seedButtonContainer,
            new Separator(),
            rainLabel, rainButton,
            new Separator(),
            tempLabel, tempButton,
            new Separator(),
            parasiteLabel, parasiteButtonContainer,
            new Separator(),
            actionLabel, harvestAllButton, pestControlButton, pestControlNote
        );
        
        return leftPanel;
    }

    private Node buildGameArea() {
        VBox gameArea = new VBox(20);
        gameArea.setAlignment(Pos.CENTER);
        gameArea.setPadding(new Insets(30));
        VBox.setVgrow(gameArea, Priority.ALWAYS);
        HBox.setHgrow(gameArea, Priority.ALWAYS);
        
        // Garden title
        gardenTitleLabel = new Label(userName + "'s Garden");
        gardenTitleLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 20));
        gardenTitleLabel.setTextFill(Color.DARKGREEN);
        
        // 3x9 grid (27 plots) - centered with better spacing
        gardenGrid.setHgap(12);
        gardenGrid.setVgap(12);
        gardenGrid.setAlignment(Pos.CENTER);
        gardenGrid.setPadding(new Insets(10));
        
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                gardenGrid.add(createPlot(r, c), c, r);
            }
        }
        
        // Wrap grid in a centered container for better centering
        VBox gridContainer = new VBox();
        gridContainer.setAlignment(Pos.CENTER);
        gridContainer.getChildren().add(gardenGrid);
        
        gameArea.getChildren().addAll(gardenTitleLabel, gridContainer);
        return gameArea;
    }
    
    /**
     * Shows a dialog to ask for the user's name when the app launches.
     * Called from GardenApp after the stage is shown.
     */
    public void askForUserName() {
        TextInputDialog dialog = new TextInputDialog("Player");
        dialog.setTitle("Welcome to Garden Simulator");
        dialog.setHeaderText("Enter Your Name");
        dialog.setContentText("Please enter your name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (name != null && !name.trim().isEmpty()) {
                userName = name.trim();
            } else {
                userName = "Player"; // Default if empty
            }
            // Update garden title
            if (gardenTitleLabel != null) {
                gardenTitleLabel.setText(userName + "'s Garden");
            }
        });
        
        // If user cancelled, use default
        if (!result.isPresent()) {
            userName = "Player";
            if (gardenTitleLabel != null) {
                gardenTitleLabel.setText(userName + "'s Garden");
            }
        }
    }

    private Node createPlot(int row, int col) {
        String key = row + "," + col;
        PlotData plot = plots.get(key);
        
        StackPane plotPane = new StackPane();
        plotPane.setPrefSize(130, 130);
        plotPane.setMinSize(130, 130);
        plotPane.setMaxSize(130, 130);
        
        // Soil background
        Rectangle soil = new Rectangle(130, 130);
        if (plot != null && plot.health < 30) {
            // Dead/dying plant - brownish
            soil.setFill(Color.rgb(101, 67, 33));
        } else {
            // Healthy soil - gradient
            soil.setFill(Color.rgb(139, 90, 43));
        }
        soil.setArcWidth(15);
        soil.setArcHeight(15);
        soil.setStroke(Color.rgb(78, 59, 37));
        soil.setStrokeWidth(3);
        
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        
        if (plot == null) {
            // Empty plot
            Label emptyLabel = new Label("Empty");
            emptyLabel.setFont(Font.font(FONT_ARIAL, 12));
            emptyLabel.setTextFill(Color.WHITE);
            content.getChildren().add(emptyLabel);
        } else {
            // Plant visualization
            String plantEmoji = getPlantEmoji(plot.species, plot.growthStage);
            Label plantLabel = new Label(plantEmoji);
            plantLabel.setFont(Font.font(FONT_ARIAL, 32));
            
            Label nameLabel = new Label(plot.plantName);
            nameLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 10));
            nameLabel.setTextFill(Color.WHITE);
            
            // Health indicator - bigger and more prominent
            String healthColor = plot.health > 60 ? "green" : plot.health > 30 ? "orange" : "red";
            Label healthLabel = new Label("❤ " + plot.health + "%");
            healthLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 16)); // Increased from 9 to 16, made bold
            healthLabel.setTextFill(Color.web(healthColor));
            
            // Parasite emojis - show all parasites that have infected this plant
            if (!plot.parasites.isEmpty()) {
                StringBuilder parasiteEmojis = new StringBuilder();
                for (String parasite : plot.parasites) {
                    parasiteEmojis.append(getParasiteEmoji(parasite));
                }
                Label parasiteLabel = new Label(parasiteEmojis.toString());
                parasiteLabel.setFont(Font.font(FONT_ARIAL, 16));
                parasiteLabel.setTextFill(Color.RED);
                content.getChildren().add(parasiteLabel);
            }
            
            content.getChildren().addAll(plantLabel, nameLabel, healthLabel);
        }
        
        plotPane.getChildren().addAll(soil, content);
        
        // Make plotPane mouse transparent for children, but clickable itself
        plotPane.setPickOnBounds(true);
        
        // Click handlers - ensure they work properly
        plotPane.setOnMouseClicked(e -> {
            e.consume(); // Consume the event to prevent propagation
            if (e.getButton().toString().equals("SECONDARY") || e.isSecondaryButtonDown()) {
                // Right-click: Harvest
                harvestPlot(row, col);
            } else if (e.getButton().toString().equals("PRIMARY")) {
                // Left-click: Check if parasite is selected, otherwise plant seed
                if (selectedParasite != null && !selectedParasite.isEmpty()) {
                    // Infect plot with selected parasite
                    infectPlotWithParasite(row, col);
                } else {
                    // Plant seed
                    plantSeed(row, col);
                }
            }
        });
        
        // Hover effect - make it more visible
        plotPane.setOnMouseEntered(e -> {
            soil.setStroke(Color.YELLOW);
            soil.setStrokeWidth(5);
            plotPane.setStyle("-fx-cursor: hand;");
        });
        plotPane.setOnMouseExited(e -> {
            soil.setStroke(Color.rgb(78, 59, 37));
            soil.setStrokeWidth(3);
            plotPane.setStyle("-fx-cursor: default;");
        });
        
        // Add cursor pointer on hover
        plotPane.setCursor(javafx.scene.Cursor.HAND);
        
        return plotPane;
    }

    private String getPlantEmoji(String species, int growthStage) {
        String base = switch(species) {
            case SEED_TOMATO -> "🍅";
            case SEED_ROSE -> "🌹";
            case SEED_BASIL -> "🌿";
            case SEED_PEPPER -> "🌶️";
            case SEED_CUCUMBER -> "🥒";
            case SEED_LETTUCE -> "🥬";
            case SEED_CARROT -> "🥕";
            case SEED_STRAWBERRY -> "🍓";
            case SEED_SUNFLOWER -> "🌻";
            case SEED_MARIGOLD -> "🌼";
            default -> "🌱";
        };
        
        // Growth stages: seed → sprout → small → medium → ready
        if (growthStage == 0) return "🌱"; // Seed
        if (growthStage == 1) return "🌿"; // Sprout
        if (growthStage == 2) return base; // Small plant
        if (growthStage == 3) return base + base; // Medium plant
        return base + base + base; // Ready to harvest
    }

    private Node buildSidePanel() {
        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(15));
        sidePanel.setMinWidth(300);
        sidePanel.setPrefWidth(400);
        sidePanel.setMaxWidth(500);
        sidePanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10;");
        
        // Event Log section
        Label logTitle = new Label("📋 Event Log");
        logTitle.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 16));
        logTitle.setTextFill(Color.DARKGREEN);
        
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFont(Font.font("Courier New", FontWeight.NORMAL, 12)); // Increased from 10 to 12 for better resolution
        logArea.getStyleClass().add("log-area");
        logArea.setStyle(
            "-fx-background-color: #000000; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-control-inner-background: #000000; " +
            "-fx-text-box-border: #000000; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent; " +
            "-fx-font-smoothing-type: lcd; " + // Better text rendering
            "-fx-font-family: 'Courier New', monospace;"
        );
        logArea.setPrefRowCount(20);
        logArea.setPrefColumnCount(50); // Increased for better readability
        VBox.setVgrow(logArea, Priority.ALWAYS); // Allow log area to grow vertically
        HBox.setHgrow(logArea, Priority.ALWAYS); // Allow log area to grow horizontally
        
        // Load CSS for log area styling
        try {
            var cssUrl = getClass().getResource("/log-area.css");
            if (cssUrl != null) {
                logArea.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            // CSS loading failed, inline styles should still work
        }
        
        // Style the content area after it's added to scene
        Platform.runLater(() -> {
            var content = logArea.lookup(".content");
            if (content != null) {
                content.setStyle("-fx-background-color: #000000;");
            }
        });
        
        // Initial log message
        addLogEntry("System initialized - Garden Simulator started");
        
        sidePanel.getChildren().addAll(logTitle, logArea);
        return sidePanel;
    }

    private Node buildStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 5;");
        
        statusLabel.setFont(Font.font(FONT_ARIAL, 12));
        statusLabel.setTextFill(Color.WHITE);
        statusLabel.setWrapText(true);
        
        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void initializePlots() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                plots.put(r + "," + c, null);
            }
        }
    }

    private void plantSeed(int row, int col) {
        String key = row + "," + col;
        if (plots.get(key) != null) {
            statusLabel.setText("Plot is already occupied! Right-click to harvest first.");
            return;
        }
        
        if (selectedSeed == null || selectedSeed.isEmpty()) {
            statusLabel.setText("Please select a seed first!");
            return;
        }
        
        String species = selectedSeed;
        int price = api.getSeedPrice(species);
        
        // Check coins from backend
        if (api.getCoins() < price) {
            statusLabel.setText("Not enough coins! Need " + price + " coins to buy " + species + " seeds.");
            return;
        }
        
        // Get days to harvest from PlantType
        PlantType plantType = PlantType.fromString(species);
        int daysToHarvest = plantType != null ? plantType.getDaysToHarvest() : 5; // Default 5 days if not found
        
        String plantName = species + "-" + (row * 9 + col + 1);
        
        // Get species info from backend - use species definitions from config
        Species speciesInfo = impl.getSpeciesInfo(species);
        
        int waterRequirement = 10; // Default
        int tempMin;
        int tempMax;
        List<String> vulns = List.of(); // Default no vulnerabilities
        
        if (speciesInfo != null) {
            waterRequirement = speciesInfo.getDailyWaterNeed();
            tempMin = speciesInfo.getTempMin();
            tempMax = speciesInfo.getTempMax();
            vulns = new ArrayList<>(speciesInfo.getParasiteVulns());
        } else {
            // Fallback to hardcoded values if species not found (shouldn't happen if config is correct)
            switch (species) {
                case SEED_ROSE -> { tempMin = 50; tempMax = 90; }
                case SEED_TOMATO -> { tempMin = 55; tempMax = 95; }
                case SEED_BASIL -> { tempMin = 60; tempMax = 95; }
                case SEED_PEPPER -> { tempMin = 60; tempMax = 90; }
                case SEED_CUCUMBER -> { tempMin = 65; tempMax = 95; }
                case SEED_LETTUCE -> { tempMin = 45; tempMax = 75; }
                case SEED_CARROT -> { tempMin = 50; tempMax = 80; }
                case SEED_STRAWBERRY -> { tempMin = 50; tempMax = 85; }
                case SEED_SUNFLOWER -> { tempMin = 55; tempMax = 90; }
                case SEED_MARIGOLD -> { tempMin = 50; tempMax = 85; }
                default -> { tempMin = 50; tempMax = 90; } // Safe defaults
            }
        }
        
        // Add plant to backend - backend handles coin deduction, health generation, plot assignment
        String plotKey = row + "," + col;
        boolean success = api.addPlant(plotKey, plantName, species, waterRequirement, tempMin, tempMax, vulns, price);
        
        if (!success) {
            statusLabel.setText("Failed to plant! Check if plot is occupied or you have enough coins.");
            return;
        }
        
        // Create PlotData for UI display - all state will be synced from backend
        PlotData plotData = new PlotData(plantName, species, daysToHarvest);
        plots.put(key, plotData);
        
        // Sync all state from backend immediately
        syncPlotFromBackend(plotData);
        updateCoinsDisplay();
        
        statusLabel.setText("Planted " + plantName + " at plot (" + row + "," + col + ")! Cost: " + price + " coins. Harvest in " + daysToHarvest + " days.");
        addLogEntry("🌱 Planted " + plantName + " (" + species + ") at plot (" + row + "," + col + ") - Harvest in " + daysToHarvest + " days");
        refreshGarden();
    }

    private void harvestPlot(int row, int col) {
        String key = row + "," + col;
        PlotData plot = plots.get(key);
        
        if (plot == null) {
            statusLabel.setText("Nothing to harvest here!");
            return;
        }
        
        // Harvest via backend - backend handles all logic
        int reward = api.harvestPlant(plot.plantName);
        
        if (reward == -1) {
            statusLabel.setText("Cannot harvest " + plot.plantName + " - plant not found or dead.");
            return;
        }
        
        if (reward == -2) {
            int daysRemaining = plot.daysToHarvest - plot.daysPlanted;
            statusLabel.setText(plot.plantName + " is not ready yet! " + daysRemaining + " day(s) remaining.");
            return;
        }
        
        // Update UI
        plots.put(key, null);
        updateCoinsDisplay();
        
        statusLabel.setText("Harvested " + plot.plantName + "! Earned " + reward + " coins. (Days: " + plot.daysPlanted + ", Health: " + plot.health + "%)");
        addLogEntry("Harvested " + plot.plantName + " - Earned " + reward + " coins (Days: " + plot.daysPlanted + ", Health: " + plot.health + "%)");
        refreshGarden();
    }
    
    private void harvestAllReady() {
        // Harvest all ready plants via backend - backend handles all logic
        int totalEarned = api.harvestAllReady();
        
        if (totalEarned > 0) {
            // Remove harvested plants from UI plots
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
                PlotData plot = entry.getValue();
                if (plot != null) {
                    // Check if plant still exists in backend
                    Map<String, Object> states = impl.getPlantStates();
                    @SuppressWarnings("unchecked")
                    List<String> plantNames = (List<String>) states.get("plants");
                    if (plantNames == null || !plantNames.contains(plot.plantName)) {
                        toRemove.add(entry.getKey());
                    }
                }
            }
            for (String key : toRemove) {
                plots.put(key, null);
            }
            
            updateCoinsDisplay();
            
            int harvested = toRemove.size();
            statusLabel.setText("Harvested " + harvested + " plant(s)! Earned " + totalEarned + " coins total.");
            addLogEntry("Harvested " + harvested + " plant(s) - Total earnings: " + totalEarned + " coins");
            refreshGarden();
        } else {
            statusLabel.setText("No plants ready to harvest!");
            addLogEntry("Harvest all ready: No plants ready to harvest");
        }
    }

    /**
     * Syncs all plot data from backend for a specific plot.
     * Backend is the single source of truth for all plant data.
     */
    private void syncPlotFromBackend(PlotData plot) {
        try {
            Map<String, Object> states = impl.getPlantStates();
            @SuppressWarnings("unchecked")
            List<String> plantNames = (List<String>) states.get("plants");
            
            if (plantNames == null || !plantNames.contains(plot.plantName)) {
                return; // Plant doesn't exist in backend
            }
            
            int idx = plantNames.indexOf(plot.plantName);
            if (idx < 0) return;
            
            @SuppressWarnings("unchecked")
            List<Integer> healthList = (List<Integer>) states.get("health");
            @SuppressWarnings("unchecked")
            List<Integer> moistureList = (List<Integer>) states.get("moisture");
            @SuppressWarnings("unchecked")
            List<List<String>> parasites = (List<List<String>>) states.get("activeParasites");
            @SuppressWarnings("unchecked")
            List<Integer> daysPlantedList = (List<Integer>) states.get("daysPlanted");
            @SuppressWarnings("unchecked")
            List<Integer> growthStageList = (List<Integer>) states.get("growthStage");
            @SuppressWarnings("unchecked")
            List<Boolean> readyToHarvestList = (List<Boolean>) states.get("readyToHarvest");
            
            if (healthList != null && idx < healthList.size()) {
                plot.health = healthList.get(idx);
            }
            if (moistureList != null && idx < moistureList.size()) {
                plot.moisture = moistureList.get(idx);
            }
            if (parasites != null && idx < parasites.size()) {
                @SuppressWarnings("unchecked")
                List<String> parasiteList = (List<String>) parasites.get(idx);
                plot.parasites = parasiteList != null ? new ArrayList<>(parasiteList) : new ArrayList<>();
            }
            if (daysPlantedList != null && idx < daysPlantedList.size()) {
                plot.daysPlanted = daysPlantedList.get(idx);
            }
            if (growthStageList != null && idx < growthStageList.size()) {
                plot.growthStage = growthStageList.get(idx);
            }
            if (readyToHarvestList != null && idx < readyToHarvestList.size()) {
                plot.readyToHarvest = readyToHarvestList.get(idx);
            }
        } catch (Exception e) {
            // Ignore sync errors
        }
    }
    
    /**
     * Updates coins display from backend.
     */
    private void updateCoinsDisplay() {
        int coins = api.getCoins();
        coinsLabel.setText(COINS_LABEL_PREFIX + coins);
    }
    
    private void updateGame() {
        // Sync all UI state from backend - backend is single source of truth
        boolean needsRefresh = false;
        
        // Update coins display
        updateCoinsDisplay();
        
        // Sync all plots from backend
        for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
            PlotData plot = entry.getValue();
            if (plot != null) {
                int oldHealth = plot.health;
                int oldGrowthStage = plot.growthStage;
                List<String> oldParasites = new ArrayList<>(plot.parasites);
                
                syncPlotFromBackend(plot);
                
                // Check if anything changed
                boolean parasitesChanged = !plot.parasites.equals(oldParasites);
                if (plot.health != oldHealth || plot.growthStage != oldGrowthStage || parasitesChanged) {
                    needsRefresh = true;
                }
            }
        }
        
        if (needsRefresh) {
            refreshGarden();
        }
    }

    private void refreshGarden() {
        gardenGrid.getChildren().clear();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                gardenGrid.add(createPlot(r, c), c, r);
            }
        }
    }
    
    private void subscribeToEvents() {
        EventBus bus = impl.getEventBus();
        
        // Subscribe to DAY_TICK events
        bus.subscribe(EventBus.Topic.DAY_TICK, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("Day " + payload + " has begun");
                day = (Integer) payload;
                dayLabel.setText("Day: " + day);
                
                // Random temperature generation is handled by backend
                // Temperature change will be logged via TEMPERATURE event subscription
                
                // Plant growth is handled by backend - just sync UI state
                updateGame();
            });
        });
        
        // Subscribe to RAIN events
        bus.subscribe(EventBus.Topic.RAIN, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("Rain event: +" + payload + " water units");
            });
        });
        
        // Subscribe to TEMPERATURE events - update display and log to UI
        bus.subscribe(EventBus.Topic.TEMPERATURE, (payload) -> {
            Platform.runLater(() -> {
                int newTemperature = (Integer) payload;
                currentTemperature = newTemperature;
                temperatureLabel.setText("Temp: " + currentTemperature + "°F");
                // Log temperature change to UI log area
                addLogEntry("Temperature changed to " + newTemperature + "°F");
            });
        });
        
        // Subscribe to HEATING_ACTIVATED events - log heating system activation
        bus.subscribe(EventBus.Topic.HEATING_ACTIVATED, (payload) -> {
            Platform.runLater(() -> {
                @SuppressWarnings("unchecked")
                Map<String, Integer> temps = (Map<String, Integer>) payload;
                int original = temps.get("original");
                int mitigated = temps.get("mitigated");
                addLogEntry("Heating System: Set temperature " + original + "°F is too cold (minimum: 60°F). " +
                           "Heating system activated and raised temperature to " + mitigated + "°F");
            });
        });
        
        // Subscribe to PARASITE events
        bus.subscribe(EventBus.Topic.PARASITE, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("Parasite detected: " + payload);
            });
        });
    }
    
    private void addLogEntry(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String logLine = "[" + timestamp + "] " + message;
        
        Platform.runLater(() -> {
            logArea.appendText(logLine + "\n");
            
            // Keep only last MAX_LOG_LINES lines
            String[] lines = logArea.getText().split("\n");
            if (lines.length > MAX_LOG_LINES) {
                StringBuilder sb = new StringBuilder();
                for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
                    sb.append(lines[i]).append("\n");
                }
                logArea.setText(sb.toString());
            }
            
            // Auto-scroll to bottom
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    /**
     * Logs individual occupied plot status every 30 seconds.
     * Only logs plots that have plants in them.
     * Syncs with backend data to ensure accurate status reporting.
     */
    private void logIndividualPlots() {
        // First, sync plot data with backend for accurate status
        try {
            Map<String, Object> states = impl.getPlantStates();
            @SuppressWarnings("unchecked")
            List<String> plantNames = (List<String>) states.get("plants");
            @SuppressWarnings("unchecked")
            List<Integer> healthList = (List<Integer>) states.get("health");
            @SuppressWarnings("unchecked")
            List<Integer> moistureList = (List<Integer>) states.get("moisture");
            @SuppressWarnings("unchecked")
            List<List<String>> parasites = (List<List<String>>) states.get("activeParasites");
            
            // Sync each plot with backend data
            for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
                PlotData plot = entry.getValue();
                if (plot != null && plantNames != null && plantNames.contains(plot.plantName)) {
                    int idx = plantNames.indexOf(plot.plantName);
                    if (idx >= 0) {
                        if (healthList != null && idx < healthList.size()) {
                            plot.health = healthList.get(idx);
                        }
                        if (moistureList != null && idx < moistureList.size()) {
                            plot.moisture = moistureList.get(idx);
                        }
                        if (parasites != null && idx < parasites.size()) {
                            @SuppressWarnings("unchecked")
                            List<String> parasiteList = (List<String>) parasites.get(idx);
                            plot.parasites = parasiteList != null ? new ArrayList<>(parasiteList) : new ArrayList<>();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.LogLevel.WARNING, "Failed to sync plot data for individual plot logging: " + e.getMessage());
        }
        
        // Log individual occupied plots only
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                String key = r + "," + c;
                PlotData plot = plots.get(key);
                
                if (plot != null) {
                    // Calculate days remaining for display only (not game logic)
                    int daysRemaining = Math.max(0, plot.daysToHarvest - plot.daysPlanted);
                    String status = String.format(
                        "Plot (%d,%d): %s | Health: %d%% | Moisture: %d%% | Stage: %d/4 | Days: %d/%d (%s) | Parasites: %s",
                        r, c, plot.plantName, plot.health, plot.moisture, 
                        plot.growthStage, plot.daysPlanted, plot.daysToHarvest,
                        daysRemaining > 0 ? daysRemaining + " days left" : "Ready!",
                        plot.parasites.isEmpty() ? "No" : String.join(", ", plot.parasites)
                    );
                    
                    Logger.log(Logger.LogLevel.INFO, status);
                    
                    // Also add to UI log
                    Platform.runLater(() -> {
                        addLogEntry("[PLOT] " + status);
                    });
                }
            }
        }
    }
    
    /**
     * Logs the summary status of all plots every 40 minutes.
     * Provides an overview of the entire garden state.
     * Syncs with backend data to ensure accurate status reporting.
     */
    private void logPlotStatusSummary() {
        // First, sync plot data with backend for accurate status
        try {
            Map<String, Object> states = impl.getPlantStates();
            @SuppressWarnings("unchecked")
            List<String> plantNames = (List<String>) states.get("plants");
            @SuppressWarnings("unchecked")
            List<Integer> healthList = (List<Integer>) states.get("health");
            @SuppressWarnings("unchecked")
            List<Integer> moistureList = (List<Integer>) states.get("moisture");
            @SuppressWarnings("unchecked")
            List<List<String>> parasites = (List<List<String>>) states.get("activeParasites");
            
            // Sync each plot with backend data
            for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
                PlotData plot = entry.getValue();
                if (plot != null && plantNames != null && plantNames.contains(plot.plantName)) {
                    int idx = plantNames.indexOf(plot.plantName);
                    if (idx >= 0) {
                        if (healthList != null && idx < healthList.size()) {
                            plot.health = healthList.get(idx);
                        }
                        if (moistureList != null && idx < moistureList.size()) {
                            plot.moisture = moistureList.get(idx);
                        }
                        if (parasites != null && idx < parasites.size()) {
                            @SuppressWarnings("unchecked")
                            List<String> parasiteList = (List<String>) parasites.get(idx);
                            plot.parasites = parasiteList != null ? new ArrayList<>(parasiteList) : new ArrayList<>();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.log(Logger.LogLevel.WARNING, "Failed to sync plot data for summary logging: " + e.getMessage());
        }
        
        // Calculate summary statistics
        int totalPlots = 0;
        int occupiedPlots = 0;
        int healthyPlants = 0;
        int lowHealthPlants = 0;
        int lowMoisturePlants = 0;
        int infestedPlants = 0;
        int readyToHarvest = 0;
        
        StringBuilder plotDetails = new StringBuilder();
        
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                String key = r + "," + c;
                PlotData plot = plots.get(key);
                totalPlots++;
                
                if (plot != null) {
                    occupiedPlots++;
                    
                    // Check health status
                    if (plot.health > 60) {
                        healthyPlants++;
                    } else if (plot.health < 30) {
                        lowHealthPlants++;
                    }
                    
                    // Check moisture status
                    if (plot.moisture < 30) {
                        lowMoisturePlants++;
                    }
                    
                    // Check parasite status
                    if (!plot.parasites.isEmpty()) {
                        infestedPlants++;
                    }
                    
                    // Check harvest readiness based on days planted
                    if (plot.daysPlanted >= plot.daysToHarvest) {
                        readyToHarvest++;
                    }
                    
                    // Build detailed log entry for this plot (display only)
                    int daysRemaining = Math.max(0, plot.daysToHarvest - plot.daysPlanted);
                    plotDetails.append(String.format(
                        "Plot (%d,%d): %s | Health: %d%% | Moisture: %d%% | Stage: %d/4 | Days: %d/%d (%s) | Parasites: %s%n",
                        r, c, plot.plantName, plot.health, plot.moisture, 
                        plot.growthStage, plot.daysPlanted, plot.daysToHarvest,
                        daysRemaining > 0 ? daysRemaining + " days left" : "Ready!",
                        plot.parasites.isEmpty() ? "No" : String.join(", ", plot.parasites)
                    ));
                }
            }
        }
        
        // Log summary
        String summary = String.format(
            "Plot Status Summary - Total: %d | Occupied: %d | Healthy: %d | Low Health: %d | Low Moisture: %d | Infested: %d | Ready to Harvest: %d",
            totalPlots, occupiedPlots, healthyPlants, lowHealthPlants, lowMoisturePlants, infestedPlants, readyToHarvest
        );
        
        Logger.log(Logger.LogLevel.INFO, summary);
        
        // Log individual plot details at DEBUG level
        if (occupiedPlots > 0) {
            Logger.log(Logger.LogLevel.DEBUG, "Detailed Plot Status:\n" + plotDetails.toString().trim());
        }
        
        // Also add to UI log
        Platform.runLater(() -> {
            addLogEntry("[SUMMARY] " + summary);
        });
    }
    
    /**
     * Shows a custom dialog with plant/seed tiles for selection.
     * Each tile displays the plant emoji, name, price, and harvest time information.
     */
    private void showSeedSelectionDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Plant Seed");
        dialog.setHeaderText("Choose a plant to grow in your garden");
        
        // Create plant data
        class PlantInfo {
            String displayName;
            String emoji;
            String backendId;
            String description;
            
            PlantInfo(String displayName, String emoji, String backendId, String description) {
                this.displayName = displayName;
                this.emoji = emoji;
                this.backendId = backendId;
                this.description = description;
            }
        }
        
        // Build plant info array with accurate days to harvest from PlantType
        // Include all 10 plant types from config
        PlantInfo[] plants = new PlantInfo[10];
        String[] seedNames = {
            SEED_TOMATO, SEED_ROSE, SEED_BASIL, SEED_PEPPER, SEED_CUCUMBER,
            SEED_LETTUCE, SEED_CARROT, SEED_STRAWBERRY, SEED_SUNFLOWER, SEED_MARIGOLD
        };
        String[] displayNames = {
            "Tomato", "Rose", "Basil", "Pepper", "Cucumber",
            "Lettuce", "Carrot", "Strawberry", "Sunflower", "Marigold"
        };
        
        for (int i = 0; i < seedNames.length; i++) {
            String seedName = seedNames[i];
            PlantType plantType = PlantType.fromString(seedName);
            int daysToHarvest = plantType != null ? plantType.getDaysToHarvest() : 5;
            String emoji = plantType != null ? plantType.getEmoji() : "🌱";
            int price = api.getSeedPrice(seedName);
            String description = "Harvest in " + daysToHarvest + " days\nPrice: " + price + " coins";
            plants[i] = new PlantInfo(displayNames[i], emoji, seedName, description);
        }
        
        // Create grid for plant tiles
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(25));
        grid.setAlignment(Pos.CENTER);
        
        // Store selected plant
        final String[] selectedPlant = {null};
        
        // Create tiles for each plant
        for (int i = 0; i < plants.length; i++) {
            PlantInfo plant = plants[i];
            
            VBox tile = new VBox(15);
            tile.setAlignment(Pos.CENTER);
            tile.setPadding(new Insets(20));
            tile.setPrefWidth(200);
            tile.setPrefHeight(220);
            tile.setStyle(
                "-fx-background-color: #F5F5F5; " +
                "-fx-border-color: #CCCCCC; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;"
            );
            
            // Emoji (big)
            Label emojiLabel = new Label(plant.emoji);
            emojiLabel.setFont(Font.font(FONT_ARIAL, 64));
            
            // Name
            Label nameLabel = new Label(plant.displayName);
            nameLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 18));
            nameLabel.setTextFill(Color.DARKGREEN);
            
            // Description/Info
            Label infoLabel = new Label(plant.description);
            infoLabel.setFont(Font.font(FONT_ARIAL, 14));
            infoLabel.setTextFill(Color.DARKGRAY);
            infoLabel.setWrapText(true);
            infoLabel.setAlignment(Pos.CENTER);
            infoLabel.setMaxWidth(180);
            
            tile.getChildren().addAll(emojiLabel, nameLabel, infoLabel);
            
            // Hover effect
            tile.setOnMouseEntered(ev -> {
                tile.setStyle(
                    "-fx-background-color: #E8F5E9; " +
                    "-fx-border-color: #4CAF50; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;"
                );
            });
            
            tile.setOnMouseExited(ev -> {
                if (selectedPlant[0] != null && selectedPlant[0].equals(plant.backendId)) {
                    tile.setStyle(
                        "-fx-background-color: #C8E6C9; " +
                        "-fx-border-color: #4CAF50; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
                    );
                } else {
                    tile.setStyle(
                        "-fx-background-color: #F5F5F5; " +
                        "-fx-border-color: #CCCCCC; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
                    );
                }
            });
            
            // Click handler
            tile.setOnMouseClicked(ev -> {
                // Reset all tiles
                for (int j = 0; j < plants.length; j++) {
                    Node node = grid.getChildren().get(j);
                    if (node instanceof VBox) {
                        VBox vbox = (VBox) node;
                        vbox.setStyle(
                            "-fx-background-color: #F5F5F5; " +
                            "-fx-border-color: #CCCCCC; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10;"
                        );
                    }
                }
                
                // Highlight selected tile
                tile.setStyle(
                    "-fx-background-color: #C8E6C9; " +
                    "-fx-border-color: #4CAF50; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10;"
                );
                
                selectedPlant[0] = plant.backendId;
            });
            
            tile.setCursor(javafx.scene.Cursor.HAND);
            
            // Add to grid (3 columns for better layout with 10 plants: 3x4 grid)
            grid.add(tile, i % 3, i / 3);
        }
        
        // Create dialog content
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.getChildren().add(grid);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(700, 800); // Larger dialog to fit 10 plants in 3 columns
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        
        // Style OK button
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Handle result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType && selectedPlant[0] != null) {
                return selectedPlant[0];
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(plantType -> {
            selectedSeed = plantType;
            updateSeedButtonDisplay();
            statusLabel.setText("Selected " + plantType + " seed (" + api.getSeedPrice(plantType) + " coins)");
        });
    }
    
    /**
     * Updates the seed button display to show the selected plant.
     */
    private void updateSeedButtonDisplay() {
        if (selectedSeed != null) {
            PlantType plantType = PlantType.fromString(selectedSeed);
            String emoji = plantType != null ? plantType.getEmoji() : "🌱";
            String displayName = selectedSeed.substring(0, 1).toUpperCase() + selectedSeed.substring(1).toLowerCase();
            seedButton.setText(emoji + " " + displayName);
            clearSeedButton.setVisible(true);
        } else {
            seedButton.setText("Select Plant");
            clearSeedButton.setVisible(false);
        }
    }
    
    /**
     * Clears the seed selection.
     */
    private void clearSeedSelection() {
        selectedSeed = null;
        updateSeedButtonDisplay();
        statusLabel.setText("Seed selection cleared. Please select a seed to plant.");
    }
    
    /**
     * Shows a custom dialog with parasite tiles for selection.
     * Each tile displays the parasite emoji, name, and damage information.
     */
    private void showParasiteSelectionDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Select Parasite Attack");
        dialog.setHeaderText("Choose a parasite to attack your garden");
        
        // Create parasite data
        class ParasiteInfo {
            String displayName;
            String emoji;
            String backendId;
            String description;
            
            ParasiteInfo(String displayName, String emoji, String backendId, String description) {
                this.displayName = displayName;
                this.emoji = emoji;
                this.backendId = backendId;
                this.description = description;
            }
        }
        
        ParasiteInfo[] parasites = {
            new ParasiteInfo("Aphid", "🦗", "aphid", "Mild damage\n2 health/day"),
            new ParasiteInfo("Spider Mite", "🕷️", "spider_mite", "Moderate damage\n5 health/day"),
            new ParasiteInfo("Whitefly", "🦟", "whitefly", "Moderate damage\n3 health/day"),
            new ParasiteInfo("Thrips", "🐛", "thrips", "Severe damage\n6 health/day")
        };
        
        // Create grid for parasite tiles
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(25));
        grid.setAlignment(Pos.CENTER);
        
        // Store selected parasite in dialog (local variable)
        final String[] dialogSelectedParasite = {null};
        
        // Create tiles for each parasite
        for (int i = 0; i < parasites.length; i++) {
            ParasiteInfo parasite = parasites[i];
            
            VBox tile = new VBox(15);
            tile.setAlignment(Pos.CENTER);
            tile.setPadding(new Insets(20));
            tile.setPrefWidth(200);
            tile.setPrefHeight(220);
            tile.setStyle(
                "-fx-background-color: #F5F5F5; " +
                "-fx-border-color: #CCCCCC; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;"
            );
            
            // Emoji (big)
            Label emojiLabel = new Label(parasite.emoji);
            emojiLabel.setFont(Font.font(FONT_ARIAL, 64));
            
            // Name
            Label nameLabel = new Label(parasite.displayName);
            nameLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 18));
            nameLabel.setTextFill(Color.DARKGREEN);
            
            // Description/Damage info
            Label infoLabel = new Label(parasite.description);
            infoLabel.setFont(Font.font(FONT_ARIAL, 14));
            infoLabel.setTextFill(Color.DARKGRAY);
            infoLabel.setWrapText(true);
            infoLabel.setAlignment(Pos.CENTER);
            infoLabel.setMaxWidth(180);
            
            tile.getChildren().addAll(emojiLabel, nameLabel, infoLabel);
            
            // Hover effect
            tile.setOnMouseEntered(ev -> {
                tile.setStyle(
                    "-fx-background-color: #E8F5E9; " +
                    "-fx-border-color: #4CAF50; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10; " +
                    "-fx-cursor: hand;"
                );
            });
            
            tile.setOnMouseExited(ev -> {
                if (dialogSelectedParasite[0] != null && dialogSelectedParasite[0].equals(parasite.backendId)) {
                    tile.setStyle(
                        "-fx-background-color: #C8E6C9; " +
                        "-fx-border-color: #4CAF50; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
                    );
                } else {
                    tile.setStyle(
                        "-fx-background-color: #F5F5F5; " +
                        "-fx-border-color: #CCCCCC; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
                    );
                }
            });
            
            // Click handler
            tile.setOnMouseClicked(ev -> {
                // Reset all tiles
                for (int j = 0; j < parasites.length; j++) {
                    Node node = grid.getChildren().get(j);
                    if (node instanceof VBox) {
                        VBox vbox = (VBox) node;
                        vbox.setStyle(
                            "-fx-background-color: #F5F5F5; " +
                            "-fx-border-color: #CCCCCC; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 10; " +
                            "-fx-background-radius: 10;"
                        );
                    }
                }
                
                // Highlight selected tile
                tile.setStyle(
                    "-fx-background-color: #C8E6C9; " +
                    "-fx-border-color: #4CAF50; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10;"
                );
                
                dialogSelectedParasite[0] = parasite.backendId;
            });
            
            tile.setCursor(javafx.scene.Cursor.HAND);
            
            // Add to grid (2 columns)
            grid.add(tile, i % 2, i / 2);
        }
        
        // Create dialog content
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.getChildren().add(grid);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(500, 600);
        
        // Add OK and Cancel buttons
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        
        // Style OK button
        Button okButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Handle result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType && dialogSelectedParasite[0] != null) {
                return dialogSelectedParasite[0];
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(parasiteType -> {
            selectedParasite = parasiteType;
            updateParasiteButtonDisplay();
            statusLabel.setText("Selected " + getParasiteDisplayName(parasiteType) + ". Click on plots to infect them.");
        });
    }
    
    /**
     * Gets the display name for a parasite type.
     */
    private String getParasiteDisplayName(String parasiteType) {
        return switch(parasiteType) {
            case "aphid" -> "Aphid";
            case "spider_mite" -> "Spider Mite";
            case "whitefly" -> "Whitefly";
            case "thrips" -> "Thrips";
            default -> parasiteType;
        };
    }
    
    /**
     * Gets the emoji for a parasite type.
     */
    private String getParasiteEmoji(String parasiteType) {
        return switch(parasiteType) {
            case "aphid" -> "🦗";
            case "spider_mite" -> "🕷️";
            case "whitefly" -> "🦟";
            case "thrips" -> "🐛";
            default -> "🐛";
        };
    }
    
    /**
     * Infects a specific plot with the selected parasite.
     * Just calls backend API - backend handles all logic.
     */
    private void infectPlotWithParasite(int row, int col) {
        String key = row + "," + col;
        PlotData plot = plots.get(key);
        
        if (plot == null) {
            statusLabel.setText("Cannot infect empty plot! Plant a seed first.");
            addLogEntry("Parasite attack: Nothing to infect at plot (" + row + "," + col + ") - plot is empty");
            return;
        }
        
        if (selectedParasite == null || selectedParasite.isEmpty()) {
            statusLabel.setText("Please select a parasite first!");
            return;
        }
        
        // Infect specific plant via backend - backend handles all logic and logging
        boolean success = api.infectPlant(plot.plantName, selectedParasite);
        if (success) {
            statusLabel.setText("Parasite " + getParasiteDisplayName(selectedParasite) + " applied to " + plot.plantName);
            addLogEntry("Parasite attack: " + getParasiteDisplayName(selectedParasite) + " infected " + plot.plantName + " at plot (" + row + "," + col + ")");
        } else {
            statusLabel.setText("Failed to infect " + plot.plantName);
            addLogEntry("Parasite attack: Failed to infect " + plot.plantName + " at plot (" + row + "," + col + ")");
        }
        
        // Refresh to show parasite status (will sync from backend)
        refreshGarden();
    }
    
    /**
     * Updates the parasite button display to show the selected parasite.
     */
    private void updateParasiteButtonDisplay() {
        if (selectedParasite != null && !selectedParasite.isEmpty()) {
            String emoji = getParasiteEmoji(selectedParasite);
            String displayName = getParasiteDisplayName(selectedParasite);
            parasiteButton.setText(emoji + " " + displayName);
            clearParasiteButton.setVisible(true);
        } else {
            parasiteButton.setText("Select Parasite");
            clearParasiteButton.setVisible(false);
        }
    }
    
    /**
     * Clears the parasite selection.
     */
    private void clearParasiteSelection() {
        selectedParasite = null;
        updateParasiteButtonDisplay();
        statusLabel.setText("Parasite selection cleared.");
    }
    
    // Random parasite attacks are now handled by backend - no UI logic needed
    
    /**
     * Manually triggers pest control to treat all infested plants.
     * This applies treatment to all plants with parasites.
     */
    private void triggerPestControl() {
        // Count infested plots before treatment
        final int[] infestedBefore = {0};
        for (PlotData plot : plots.values()) {
            if (plot != null && !plot.parasites.isEmpty()) {
                infestedBefore[0]++;
            }
        }
        
        // Trigger pest control
        impl.triggerPestControl();
        
        // Refresh to sync parasite status
        Platform.runLater(() -> {
            // Sync parasite status after treatment
            try {
                Map<String, Object> states = impl.getPlantStates();
                @SuppressWarnings("unchecked")
                List<String> plantNames = (List<String>) states.get("plants");
                @SuppressWarnings("unchecked")
                List<List<String>> parasites = (List<List<String>>) states.get("activeParasites");
                
                int infestedAfter = 0;
                for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
                    PlotData plot = entry.getValue();
                    if (plot != null && plantNames != null && plantNames.contains(plot.plantName)) {
                        int idx = plantNames.indexOf(plot.plantName);
                        if (idx >= 0 && parasites != null && idx < parasites.size()) {
                            @SuppressWarnings("unchecked")
                            List<String> parasiteList = (List<String>) parasites.get(idx);
                            plot.parasites = parasiteList != null ? new ArrayList<>(parasiteList) : new ArrayList<>();
                            if (!plot.parasites.isEmpty()) {
                                infestedAfter++;
                            }
                        }
                    }
                }
                
                int cured = infestedBefore[0] - infestedAfter;
                if (cured > 0) {
                    statusLabel.setText("Pest control applied! Cured " + cured + " infestation(s).");
                    addLogEntry("Pest control: Cured " + cured + " infestation(s)");
                } else if (infestedBefore[0] > 0) {
                    statusLabel.setText("Pest control applied! Some parasites resisted treatment.");
                    addLogEntry("Pest control: Treatment applied, " + infestedAfter + " infestation(s) remain");
                } else {
                    statusLabel.setText("No parasites detected. Pest control not needed.");
                    addLogEntry("Pest control: No infestations found");
                }
                
                refreshGarden();
            } catch (Exception e) {
                statusLabel.setText("Pest control applied!");
                addLogEntry("Pest control treatment applied");
                refreshGarden();
            }
        });
    }
    
}

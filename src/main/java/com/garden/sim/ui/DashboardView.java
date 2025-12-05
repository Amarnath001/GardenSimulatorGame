package com.garden.sim.ui;

import com.garden.sim.api.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import com.garden.sim.core.EventBus;
import java.util.*;
import java.time.LocalTime;
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
    private static final String SEED_LAVENDER = "Lavender";
    private static final String SEED_PEPPER = "Pepper";
    private static final String SEED_CUCUMBER = "Cucumber";
    private static final String COINS_LABEL_PREFIX = "Coins: ";
    private static final int INITIAL_COINS = 100;
    private static final int INITIAL_DAY = 1;
    private static final int REFRESH_INTERVAL_SECONDS = 2;
    
    private final GertenSimulationAPI api;
    private final GertenSimulationImpl impl;
    private final ToggleGroup seedGroup = new ToggleGroup();
    private final GridPane gardenGrid = new GridPane();
    private final Label statusLabel = new Label("Welcome to your garden! Select a seed and click a plot to plant.");
    private final Label dayLabel = new Label("Day: " + INITIAL_DAY);
    private final Label coinsLabel = new Label(COINS_LABEL_PREFIX + INITIAL_COINS);
    private final TextArea logArea = new TextArea();
    private static final int MAX_LOG_LINES = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Game state
    private final Map<String, PlotData> plots = new HashMap<>(); // "row,col" -> PlotData
    private int coins = INITIAL_COINS;
    private int day = INITIAL_DAY;
    private final Map<String, Integer> seedPrices = Map.of(
        SEED_TOMATO, 10, SEED_ROSE, 15, SEED_BASIL, 5, 
        SEED_LAVENDER, 12, SEED_PEPPER, 8, SEED_CUCUMBER, 10
    );
    
    // Plant growth stages (0-4: seed, sprout, small, medium, ready)
    private static class PlotData {
        String plantName;
        String species;
        int growthStage = 0;
        int health = 100;
        int moisture = 50;
        int daysPlanted = 0;
        boolean hasParasite = false;
        
        PlotData(String plantName, String species) {
            this.plantName = plantName;
            this.species = species;
        }
    }

    public DashboardView(GertenSimulationAPI api) {
        this.api = api;
        this.impl = (GertenSimulationImpl) api;
        
        // Set background
        setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #98D8C8 100%);");
        setPadding(new Insets(15));
        
        // Build UI
        setTop(buildHeader());
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
        
        // Game loop: update every REFRESH_INTERVAL_SECONDS
        Timeline gameLoop = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> updateGame())
        );
        gameLoop.setCycleCount(javafx.animation.Animation.INDEFINITE);
        gameLoop.play();
    }

    private Node buildHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: rgba(139, 90, 43, 0.8); -fx-background-radius: 10;");
        
        dayLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 16));
        dayLabel.setTextFill(Color.WHITE);
        
        coinsLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 16));
        coinsLabel.setTextFill(Color.GOLD);
        
        Label title = new Label("🌱 Garden Simulator");
        title.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        
        header.getChildren().addAll(title, dayLabel, coinsLabel);
        return header;
    }

    private Node buildGameArea() {
        VBox gameArea = new VBox(15);
        gameArea.setAlignment(Pos.CENTER);
        gameArea.setPadding(new Insets(20));
        
        // Garden title
        Label gardenTitle = new Label("Your Garden");
        gardenTitle.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 18));
        gardenTitle.setTextFill(Color.DARKGREEN);
        
        // 2x3 grid (6 plots)
        gardenGrid.setHgap(15);
        gardenGrid.setVgap(15);
        gardenGrid.setAlignment(Pos.CENTER);
        
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                gardenGrid.add(createPlot(r, c), c, r);
            }
        }
        
        gameArea.getChildren().addAll(gardenTitle, gardenGrid);
        return gameArea;
    }

    private Node createPlot(int row, int col) {
        String key = row + "," + col;
        PlotData plot = plots.get(key);
        
        StackPane plotPane = new StackPane();
        plotPane.setPrefSize(140, 140);
        
        // Soil background
        Rectangle soil = new Rectangle(140, 140);
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
            plantLabel.setFont(Font.font(FONT_ARIAL, 36));
            
            Label nameLabel = new Label(plot.plantName);
            nameLabel.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 10));
            nameLabel.setTextFill(Color.WHITE);
            
            // Health indicator
            String healthColor = plot.health > 60 ? "green" : plot.health > 30 ? "orange" : "red";
            Label healthLabel = new Label("❤ " + plot.health + "%");
            healthLabel.setFont(Font.font(FONT_ARIAL, 9));
            healthLabel.setTextFill(Color.web(healthColor));
            
            // Moisture indicator
            Circle moistureDot = new Circle(5);
            if (plot.moisture > 50) {
                moistureDot.setFill(Color.BLUE);
            } else if (plot.moisture > 30) {
                moistureDot.setFill(Color.ORANGE);
            } else {
                moistureDot.setFill(Color.RED);
            }
            
            // Parasite warning
            if (plot.hasParasite) {
                Label parasiteLabel = new Label("⚠");
                parasiteLabel.setFont(Font.font(FONT_ARIAL, 16));
                parasiteLabel.setTextFill(Color.RED);
                content.getChildren().add(parasiteLabel);
            }
            
            content.getChildren().addAll(plantLabel, nameLabel, healthLabel, moistureDot);
        }
        
        plotPane.getChildren().addAll(soil, content);
        
        // Click handlers
        plotPane.setOnMouseClicked(e -> {
            if (e.getButton().toString().equals("SECONDARY") || e.isSecondaryButtonDown()) {
                // Right-click: Harvest
                harvestPlot(row, col);
            } else {
                // Left-click: Plant
                plantSeed(row, col);
            }
        });
        
        // Hover effect
        plotPane.setOnMouseEntered(e -> {
            soil.setStroke(Color.YELLOW);
            soil.setStrokeWidth(4);
        });
        plotPane.setOnMouseExited(e -> {
            soil.setStroke(Color.rgb(78, 59, 37));
            soil.setStrokeWidth(3);
        });
        
        return plotPane;
    }

    private String getPlantEmoji(String species, int growthStage) {
        String base = switch(species) {
            case SEED_TOMATO -> "🍅";
            case SEED_ROSE -> "🌹";
            case SEED_BASIL -> "🌿";
            case SEED_LAVENDER -> "💜";
            case SEED_PEPPER -> "🌶️";
            case SEED_CUCUMBER -> "🥒";
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
        VBox sidePanel = new VBox(15);
        sidePanel.setPadding(new Insets(15));
        sidePanel.setPrefWidth(400);
        sidePanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 10;");
        
        // Seed selection
        Label seedTitle = new Label("Seeds");
        seedTitle.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        
        VBox seedBox = new VBox(8);
        String[] seeds = {SEED_TOMATO, SEED_ROSE, SEED_BASIL, SEED_LAVENDER, SEED_PEPPER, SEED_CUCUMBER};
        for (String seed : seeds) {
            RadioButton rb = new RadioButton(seed + " (" + seedPrices.getOrDefault(seed, 10) + " coins)");
            rb.setToggleGroup(seedGroup);
            rb.setFont(Font.font(FONT_ARIAL, 11));
            seedBox.getChildren().add(rb);
        }
        if (!seedGroup.getToggles().isEmpty()) {
            ((RadioButton) seedGroup.getToggles().get(0)).setSelected(true);
        }
        
        // Actions
        Label actionTitle = new Label("Actions");
        actionTitle.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        
        ComboBox<String> actionComboBox = new ComboBox<>();
        actionComboBox.setPrefWidth(370);
        actionComboBox.getItems().addAll(
            "🌧️ Rain +10",
            "🌧️ Rain +20",
            "🌤️ Normal Temp (72°F)",
            "☀️ Hot (100°F)",
            "❄️ Cold (40°F)",
            "🐛 Parasite Attack",
            "✂️ Harvest All Ready"
        );
        actionComboBox.setPromptText("Select an action...");
        actionComboBox.setOnAction(e -> {
            String selected = actionComboBox.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            
            if (selected.contains("Rain +10")) {
                api.rain(10);
                statusLabel.setText("It's raining! Plants are being watered.");
            } else if (selected.contains("Rain +20")) {
                api.rain(20);
                statusLabel.setText("Heavy rain! Plants are well watered.");
            } else if (selected.contains("Normal Temp")) {
                api.temperature(72);
                statusLabel.setText("Temperature set to normal (72°F).");
            } else if (selected.contains("Hot")) {
                api.temperature(100);
                statusLabel.setText("It's getting hot! Plants may stress.");
            } else if (selected.contains("Cold")) {
                api.temperature(40);
                statusLabel.setText("It's cold! Heating system activated.");
            } else if (selected.contains("Parasite")) {
                api.parasite("aphid");
                statusLabel.setText("Parasites detected! Pest control activated.");
            } else if (selected.contains("Harvest All")) {
                harvestAllReady();
            }
            
            // Reset selection after action
            actionComboBox.getSelectionModel().clearSelection();
        });
        
        // Event Log section
        Label logTitle = new Label("📋 Event Log");
        logTitle.setFont(Font.font(FONT_ARIAL, FontWeight.BOLD, 14));
        
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFont(Font.font("Courier New", 10));
        logArea.getStyleClass().add("log-area");
        logArea.setStyle(
            "-fx-background-color: #000000; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-control-inner-background: #000000; " +
            "-fx-text-box-border: #000000; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );
        logArea.setPrefRowCount(15);
        logArea.setPrefColumnCount(45);
        logArea.setPrefWidth(370);
        logArea.setPrefHeight(250);
        
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
        
        sidePanel.getChildren().addAll(seedTitle, seedBox, new Separator(), actionTitle, actionComboBox, new Separator(), logTitle, logArea);
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
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
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
        
        if (seedGroup.getSelectedToggle() == null) {
            statusLabel.setText("Please select a seed first!");
            return;
        }
        
        String species = ((RadioButton) seedGroup.getSelectedToggle()).getText().split(" ")[0];
        int price = seedPrices.getOrDefault(species, 10);
        
        if (coins < price) {
            statusLabel.setText("Not enough coins! Need " + price + " coins to buy " + species + " seeds.");
            return;
        }
        
        coins -= price;
        coinsLabel.setText(COINS_LABEL_PREFIX + coins);
        
        String plantName = species + "-" + (row * 3 + col + 1);
        plots.put(key, new PlotData(plantName, species));
        
        statusLabel.setText("Planted " + plantName + " at plot (" + row + "," + col + ")! Cost: " + price + " coins.");
        addLogEntry("🌱 Planted " + plantName + " (" + species + ") at plot (" + row + "," + col + ")");
        refreshGarden();
    }

    private void harvestPlot(int row, int col) {
        String key = row + "," + col;
        PlotData plot = plots.get(key);
        
        if (plot == null) {
            statusLabel.setText("Nothing to harvest here!");
            return;
        }
        
        // Allow harvesting at stage 3 or 4 (make it easier)
        if (plot.growthStage < 3) {
            statusLabel.setText(plot.plantName + " is not ready yet! Growth stage: " + plot.growthStage + "/4 (need 3+)");
            return;
        }
        
        // Harvest reward based on health and growth stage
        int baseReward = plot.growthStage >= 4 ? 30 : 15; // Full grown = more coins
        int healthBonus = plot.health > 80 ? 20 : plot.health > 50 ? 10 : 5;
        int reward = baseReward + healthBonus;
        
        coins += reward;
        coinsLabel.setText(COINS_LABEL_PREFIX + coins);
        
        statusLabel.setText("Harvested " + plot.plantName + "! Earned " + reward + " coins. (Stage: " + plot.growthStage + ", Health: " + plot.health + "%)");
        addLogEntry("✂️ Harvested " + plot.plantName + " - Earned " + reward + " coins (Stage: " + plot.growthStage + ", Health: " + plot.health + "%)");
        plots.put(key, null);
        refreshGarden();
    }
    
    private void harvestAllReady() {
        int harvested = 0;
        int totalEarned = 0;
        
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                String key = r + "," + c;
                PlotData plot = plots.get(key);
                if (plot != null && plot.growthStage >= 3) {
                    int baseReward = plot.growthStage >= 4 ? 30 : 15;
                    int healthBonus = plot.health > 80 ? 20 : plot.health > 50 ? 10 : 5;
                    int reward = baseReward + healthBonus;
                    totalEarned += reward;
                    plots.put(key, null);
                    harvested++;
                }
            }
        }
        
        if (harvested > 0) {
            coins += totalEarned;
            coinsLabel.setText(COINS_LABEL_PREFIX + coins);
            statusLabel.setText("Harvested " + harvested + " plant(s)! Earned " + totalEarned + " coins total.");
            addLogEntry("✂️ Harvested " + harvested + " plant(s) - Total earnings: " + totalEarned + " coins");
            refreshGarden();
        } else {
            statusLabel.setText("No plants ready to harvest! (Need growth stage 3+)");
        }
    }

    private void updateGame() {
        // This method only updates visual indicators (moisture, health)
        // Growth happens only on DAY_TICK events (real-time: 1 hour = 1 day)
        
        // Update visual indicators only
        boolean needsRefresh = false;
        for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
            PlotData plot = entry.getValue();
            if (plot != null) {
                // Sync with backend for accurate state
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
                    
                    if (plantNames.contains(plot.plantName)) {
                        int idx = plantNames.indexOf(plot.plantName);
                        if (idx >= 0) {
                            // Sync health and moisture from backend
                            if (healthList != null && idx < healthList.size()) {
                                plot.health = healthList.get(idx);
                            }
                            if (moistureList != null && idx < moistureList.size()) {
                                plot.moisture = moistureList.get(idx);
                            }
                            if (parasites != null && idx < parasites.size()) {
                                plot.hasParasite = !parasites.get(idx).isEmpty();
                            }
                            needsRefresh = true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore sync errors
                }
            }
        }
        
        if (needsRefresh) {
            refreshGarden();
        }
    }

    private void refreshGarden() {
        gardenGrid.getChildren().clear();
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                gardenGrid.add(createPlot(r, c), c, r);
            }
        }
    }
    
    private void subscribeToEvents() {
        EventBus bus = impl.getEventBus();
        
        // Subscribe to DAY_TICK events
        bus.subscribe(EventBus.Topic.DAY_TICK, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("📅 Day " + payload + " has begun");
                day = (Integer) payload;
                dayLabel.setText("Day: " + day);
                
                // Advance plant growth on each day tick (real-time: 1 hour = 1 day)
                boolean needsRefresh = false;
                for (Map.Entry<String, PlotData> entry : plots.entrySet()) {
                    PlotData plot = entry.getValue();
                    if (plot != null) {
                        plot.daysPlanted++;
                        // Advance growth stage every day (up to stage 4)
                        if (plot.growthStage < 4) {
                            plot.growthStage++;
                            needsRefresh = true;
                        }
                    }
                }
                
                if (needsRefresh) {
                    refreshGarden();
                }
            });
        });
        
        // Subscribe to RAIN events
        bus.subscribe(EventBus.Topic.RAIN, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("🌧️ Rain event: +" + payload + " water units");
            });
        });
        
        // Subscribe to TEMPERATURE events
        bus.subscribe(EventBus.Topic.TEMPERATURE, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("🌡️ Temperature changed to " + payload + "°F");
            });
        });
        
        // Subscribe to PARASITE events
        bus.subscribe(EventBus.Topic.PARASITE, (payload) -> {
            Platform.runLater(() -> {
                addLogEntry("🐛 Parasite detected: " + payload);
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
    
}

# Computerized Garden Simulator

A JavaFX-based garden simulation application that allows users to manage a virtual garden with multiple plant types, automated systems, and environmental controls.

## Overview

The Garden Simulator is a comprehensive simulation system featuring:
- Interactive JavaFX user interface with a 27-plot garden grid (9 rows × 3 columns)
- 10 different plant species with unique growth requirements
- Automated modules: Watering System, Heating System, and Pest Control
- Sensor-based monitoring: Moisture sensors, Temperature sensors, and Parasite sensors
- Automatic sprinkler irrigation system
- Detailed logging system for all events and interactions
- Event-driven architecture for modular system design

## Prerequisites

- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.6 or higher
- **Operating System**: Windows, macOS, or Linux

### Verifying Installation

Check if Java and Maven are installed:

```bash
java -version
mvn -version
```

Both commands should display version information. If not installed, download from:
- Java: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
- Maven: [Apache Maven](https://maven.apache.org/download.cgi)

## Project Structure

```
GardenSimulator/
├── src/
│   ├── main/
│   │   ├── java/com/garden/sim/
│   │   │   ├── api/              # Public API interface and implementation
│   │   │   ├── core/             # Core simulation logic
│   │   │   │   ├── factory/      # Plant factory and types
│   │   │   │   ├── logger/       # Logging system
│   │   │   │   ├── sensors/      # Sensor implementations
│   │   │   │   └── sprinklers/   # Sprinkler system
│   │   │   ├── modules/          # Automated systems (Watering, Heating, Pest Control)
│   │   │   └── ui/               # JavaFX user interface
│   │   └── resources/
│   │       ├── config/           # Garden configuration (garden.json)
│   │       └── log-area.css      # UI stylesheet
├── pom.xml                       # Maven project configuration
└── README.md                     # This file
```

## Setup Instructions

### 1. Clone or Download the Project

If using Git:
```bash
git clone <repository-url>
cd GardenSimulator
```

Or download and extract the project archive to your desired location.

### 2. Install Dependencies

Maven will automatically download all required dependencies (JavaFX libraries) when you compile or run the project. No manual installation needed.

To verify dependencies are available:
```bash
mvn dependency:resolve
```

## Running the Application

### Method 1: Using Maven (Recommended)

This is the simplest method and works on all platforms:

```bash
# Navigate to project directory
cd GardenSimulator

# Run the application
mvn javafx:run
```

The application window will open automatically.

### Method 2: Compile and Run with Maven

```bash
# Compile the project
mvn clean compile

# Run using Maven exec plugin
mvn exec:java
```

### Method 3: Using IntelliJ IDEA

1. **Open the Project**:
   - Open IntelliJ IDEA
   - Select `File` → `Open`
   - Navigate to the `GardenSimulator` directory and select it
   - Click `OK`

2. **Reload Maven Project**:
   - Right-click on `pom.xml` in the Project window
   - Select `Maven` → `Reload Project`
   - Wait for dependencies to download and index

3. **Configure Run Configuration**:
   - Go to `Run` → `Edit Configurations...`
   - Click `+` → `Application`
   - Set the following:
     - **Name**: `GardenApp`
     - **Main class**: `com.garden.sim.ui.GardenApp`
     - **VM options**: `--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml`
       - Note: Replace `/path/to/javafx-sdk/lib` with your actual JavaFX SDK path, or use Maven's JavaFX plugin instead
     - **Working directory**: `$PROJECT_DIR$`
   - Click `OK`

4. **Run**:
   - Click the green Run button or press `Shift+F10`

**Alternative for IntelliJ**: Use the Maven run configuration:
- Go to `Run` → `Edit Configurations...`
- Click `+` → `Maven`
- Set **Name**: `GardenApp (Maven)`
- Set **Command line**: `javafx:run`
- Click `OK` and run

### Method 4: Build Executable JAR

```bash
# Build the JAR with all dependencies
mvn clean package

# Run the JAR (requires JavaFX modules on module path)
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/computerized-garden-1.0.0.jar
```

## Features

### Plant Management

- **10 Plant Types**: Tomato, Rose, Basil, Pepper, Cucumber, Lettuce, Carrot, Strawberry, Sunflower, Marigold
- **Plant Growth**: Each plant has unique growth stages and days to harvest
- **Health System**: Plants have health that decreases when conditions are poor
- **Moisture System**: Plants require water; moisture naturally decays 20% per day
- **Temperature Requirements**: Each plant species has optimal temperature ranges

### Automated Modules

#### Watering System
- Uses moisture sensors to monitor soil moisture levels
- Automatically activates sprinklers when moisture is low
- Responds to rain events
- Logs all irrigation activities

#### Heating System
- Uses temperature sensors to monitor garden temperature
- Automatically activates when temperature drops below 60°F
- Raises temperature by up to 10°F to protect plants
- Logs all heating events

#### Pest Control
- Uses parasite sensors to detect infestations
- Probabilistic treatment system:
  - 50-99% efficacy: 60% chance of successful cure
  - 100% efficacy: Guaranteed cure
- Logs all detection and treatment attempts
- Supports multiple parasite types

### User Interface

- **27-Plot Garden Grid**: Visual representation of planting areas (9 rows × 3 columns)
- **Real-time Updates**: UI refreshes every 2 seconds with current plant status
- **Interactive Controls**:
  - Plant seeds (select from 10 species)
  - Harvest individual plants or all ready plants
  - Trigger rain events
  - Set temperature
  - Infect plants with parasites (for testing)
  - Activate pest control treatment
- **Status Display**: Shows coins, current day, temperature, plant health, moisture, growth stage, and parasites
- **Log Viewer**: Scrollable log area showing all system events

### Logging System

- **File Logging**: All events written to `log.txt` in the project root
- **UI Logging**: Real-time log display in the application window
- **Log Levels**: INFO, WARNING, ERROR
- **Detailed Events**: Every interaction, sensor reading, module activation, and state change is logged

## Configuration

### Garden Configuration (`src/main/resources/config/garden.json`)

The `garden.json` file defines plant species templates with their properties:

```json
{
  "species": [
    {
      "name": "Rose",
      "tempMin": 50,
      "tempMax": 90
    }
  ]
}
```

Each species defines:
- `name`: Plant species name
- `tempMin`: Minimum temperature in Fahrenheit
- `tempMax`: Maximum temperature in Fahrenheit

Water requirements and parasite vulnerabilities are defined in the code (`PlantType` enum and `Species` class).

## Troubleshooting

### "package javafx.geometry does not exist"

This error occurs when IntelliJ doesn't recognize JavaFX dependencies.

**Solution**:
1. Right-click `pom.xml` → `Maven` → `Reload Project`
2. If that doesn't work: `File` → `Invalidate Caches...` → `Invalidate and Restart`
3. After restart, reload Maven project again
4. Alternatively, use `mvn javafx:run` from the command line

### "Error: JavaFX runtime components are missing"

This happens when running directly from IntelliJ without proper VM options.

**Solution**:
- Use `mvn javafx:run` instead, or
- Configure IntelliJ run configuration with proper JavaFX module path (see Method 3 above)

### Application Window Doesn't Open

**Check**:
1. Verify Java version: `java -version` (should be 17+)
2. Check Maven compilation: `mvn clean compile`
3. Look for errors in the console output
4. Check `log.txt` for error messages

### Dependencies Not Downloading

**Solution**:
1. Check internet connection
2. Verify Maven settings: `mvn -v`
3. Try clearing Maven cache: `rm -r ~/.m2/repository/org/openjfx` (Linux/Mac) or delete the folder manually (Windows)
4. Run `mvn clean install` to force re-download

### Log File Not Created

**Check**:
1. Verify write permissions in the project directory
2. Check if `log.txt` exists but is empty (may indicate no events logged yet)
3. Look for errors in the console output

## Development

### Building the Project

```bash
# Compile
mvn clean compile

# Package (creates JAR)
mvn clean package

# Install to local repository
mvn clean install
```

### Code Style

The project follows standard Java conventions:
- Package naming: `com.garden.sim.*`
- Class naming: PascalCase
- Method naming: camelCase
- Constants: UPPER_SNAKE_CASE

## Architecture

### Event-Driven Design

The system uses an `EventBus` for decoupled communication:
- Modules subscribe to events (DAY_TICK, RAIN, TEMPERATURE, etc.)
- Components publish events when state changes
- UI listens to events for real-time updates

### API Layer

The `GardenSimulationAPI` interface provides a clean separation between UI and backend:
- UI calls API methods for all operations
- API implementation orchestrates core systems
- All game logic resides in the backend

### Module System

Each automated module (Watering, Heating, Pest Control) is independent:
- Subscribes to relevant events
- Uses sensors for monitoring
- Logs all activities
- Can be enabled/disabled independently

## License

This project is provided as-is for educational purposes.

## Support

For issues or questions:
1. Check the `log.txt` file for error details
2. Review the troubleshooting section above
3. Verify all prerequisites are installed correctly
4. Ensure you're using the recommended Maven run method (`mvn javafx:run`)


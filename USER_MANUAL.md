# Garden Simulator - User Manual 
**Version**: 1.0.0 | **Date**: December 5, 2025

---

## Quick Start (5 Minutes)

### Running the Application

```bash
cd GardenSimulatorGame
mvn javafx:run
```

**Requirements**: Java 17+, Maven 3.6+

### First Steps

1. **Enter your name** when prompted (appears on garden title)
2. **You start with 100 coins** and **27 empty plots** (9 rows × 3 columns)
3. **Plant a seed**: Click "Plant Seed" → Select Lettuce (6 coins, 3 days) → Click empty plot
4. **Wait**: 1 simulated day = 1 real hour (adjustable for testing)
5. **Harvest**: When "Ready to Harvest!" appears, click "Harvest Plant"

---

## Interface Overview

### Layout
```
┌────────────────────────────────────────────────┐
│  [Day | Date | Time | Temperature | Coins]     │
├──────────┬─────────────────┬──────────────────┤
│  Control │  Garden Grid    │  Plant Status    │
│  Panel   │  (9×3 = 27)     │  Panel           │
├──────────┴─────────────────┴──────────────────┤
│  Status Bar                                    │
├────────────────────────────────────────────────┤
│  Event Log (scrollable)                        │
└────────────────────────────────────────────────┘
```

### Control Panel (Left)
- **Plant Seed** / **Clear Selection**: Select and plant seeds
- **Harvest Plant** / **Harvest All**: Collect mature plants
- **Trigger Rain**: Water all plants manually
- **Set Temperature**: Adjust environment
- **Treat Parasites**: Manual pest control
- **Show All Plants**: View complete garden status

### Garden Grid (Center)
- **27 plots** in 9×3 arrangement
- **Left-click**: Plant selected seed
- **Right-click**: View detailed plant status
- **Visual**: Empty (brown) or occupied (emoji + name)

### Status Panel (Right)
Shows selected plant details:
- Species, Health (0-100%), Moisture (0-100%)
- Days planted, Growth stage (0-4)
- Parasites, Harvest readiness

### Log Area (Bottom)
- Real-time event feed with timestamps
- Shows planting, harvesting, system actions, errors

---

## How to Use

### Planting
1. Click **"Plant Seed"**
2. Select from 10 plant types (see table below)
3. Click **empty plot** in grid
4. Coins deducted, seed planted

### Monitoring
- **Right-click plot**: See health, moisture, parasites, days to harvest
- **Status bar**: Shows action results and errors
- **Log area**: Tracks all events chronologically

### Harvesting
- **Single**: Click "Harvest Plant" → Select plant → OK
- **All**: Click "Harvest All Ready" (automatic batch harvest)
- **Reward**: Base value × (Health ÷ 100)

### Environmental Controls
- **Rain**: Add 20-30 water units to all plants
- **Temperature**: Set 50-100°F (optimal: 70-75°F)
- **Parasites**: Manual infection for testing only

---

## Plant Species

| Plant | Cost | Days | Reward | Temp Range | Notes |
|-------|------|------|--------|------------|-------|
| **Marigold** 🌼 | 5 | 4 | 10 | 55-85°F | Best starter |
| **Lettuce** 🥬 | 6 | 3 | 12 | 50-75°F | Fastest |
| **Basil** 🌿 | 7 | 5 | 14 | 60-90°F | Mid-tier |
| **Tomato** 🍅 | 8 | 5 | 16 | 60-95°F | Popular |
| **Rose** 🌹 | 9 | 6 | 18 | 55-85°F | Aphid-prone |
| **Cucumber** 🥒 | 10 | 6 | 20 | 65-90°F | High water |
| **Carrot** 🥕 | 10 | 7 | 20 | 50-80°F | Cold-tolerant |
| **Pepper** 🌶️ | 11 | 7 | 22 | 70-100°F | Heat-loving |
| **Strawberry** 🍓 | 12 | 8 | 24 | 60-85°F | Highest reward |
| **Sunflower** 🌻 | 12 | 8 | 24 | 65-90°F | Highest reward |

**Growth Stages**: 🌱 (0) → 🌿 (1) → 🍃 (2) → 🌾 (3) → [Species emoji] (4 - Ready!)

---

## Automated Systems

### 1. Watering System
- **Trigger**: Moisture < 40%
- **Action**: Sprinkler adds 10 units automatically
- **Frequency**: Continuous monitoring
- **Log**: "Sprinkler activated for [Plant]"

### 2. Heating System
- **Trigger**: Temperature < 60°F
- **Action**: Raises temp by up to 10°F
- **Frequency**: Continuous monitoring
- **Log**: "Heating system activated"

### 3. Pest Control
- **Trigger**: Hourly scan (every 60 minutes)
- **Action**: Treats infected plants
- **Success Rate**: 60-100% depending on parasite
- **Log**: "[PEST CONTROL] Detected/Treated [Parasite] on [Plant]"

**Note**: These systems work automatically. Manual intervention rarely needed.

---

## Gameplay Mechanics

### Health Factors
- **Moisture < 20%**: -10 health/day
- **Temperature stress**: -10 health/day (outside plant's range)
- **Parasites**: -5 to -15 health/day
- **Plant dies at 0% health** (auto-removed)

### Resource Management
- **Starting coins**: 100
- **Daily moisture loss**: 20% per plant
- **No saving**: State resets on application restart

### Strategy Tips
- **Beginner**: Start with 3-5 cheap plants (Marigold, Lettuce)
- **Fast profit**: Plant only Lettuce (3-day cycles)
- **Max profit**: Fill all 27 plots with diverse plants
- **Balanced**: 30% fast, 40% medium, 30% slow plants

---

## Troubleshooting

### App Won't Start
```bash
java -version   # Must be 17+
mvn clean compile javafx:run
```

### Plants Not Growing
- Check day number increases (1 day = 1 hour by default)
- View log for daily tick messages
- Right-click plant to see "Days Planted"

### Health Decreasing
1. Right-click plant for status
2. Check: Moisture (>20%), Temperature (in range), Parasites (any?)
3. Fix: Trigger rain / Set temp / Treat parasites

### Cannot Plant
- Selected seed first? (Click "Plant Seed" button)
- Plot empty?
- Enough coins?
- Check status bar for error

### Sprinklers Not Working
- Activate at 40%, not 0%
- Check log for "Sprinkler activated" messages
- Manual override: "Trigger Rain" (30 units)

---

## Testing & Configuration

### Speed Up Time (For Grading)
Edit `src/main/java/com/garden/sim/api/GardenSimulationImpl.java` (line ~50):
```java
clock.setScaleSecondsPerDay(5); // 1 day = 5 seconds
```
Recompile: `mvn clean compile javafx:run`

### Edit Plant Properties
Edit `src/main/resources/config/garden.json`:
- Change costs, growth times, temperature ranges
- Modify parasite vulnerabilities
- Adjust harvest rewards

### Change Grid Size
Edit `DashboardView.java`:
```java
private static final int GRID_ROWS = 9;  // Current
private static final int GRID_COLS = 3;  // 27 total plots
```

### View Logs
- **UI log area**: Bottom panel (scrollable)
- **File log**: `log.txt` in project root
- **Auto-rotate**: At 10MB → `log_archive_[timestamp].txt`

---

## Key Metrics

| Metric | Value |
|--------|-------|
| Starting Coins | 100 |
| Grid Size | 27 plots (9×3) |
| Time Scale | 1 day = 1 hour (default) |
| Sprinkler Threshold | 40% moisture |
| Heating Threshold | 60°F |
| Pest Control Frequency | Every 60 minutes |
| Daily Moisture Loss | 20% |
| UI Refresh Rate | Every 2 seconds |

---

## Quick Reference

### Essential Actions
| Action | Steps |
|--------|-------|
| Plant | "Plant Seed" → Select → Click plot |
| Harvest | "Harvest Plant" → Select → OK |
| Status | Right-click plot |
| Water | "Trigger Rain" → Enter amount |
| Treat | "Treat Parasites" |

### Health Status
- 🟢 **100-80%**: Healthy
- 🟡 **79-50%**: Moderate  
- 🟠 **49-20%**: Stressed
- 🔴 **19-1%**: Critical
- ⚫ **0%**: Dead

---

## Additional Resources

- **README.md**: Installation and build instructions
- **Gardening System APIs.md**: Complete API documentation
- **Source code**: Fully commented (JavaDoc style)
- **Log file**: `log.txt` - detailed event history

---

## Project Information

**Course**: Object-Oriented Analysis, Design & Programming  
**Institution**: Santa Clara University  
**Implementation**: Java 17, JavaFX 21, Maven  
**Architecture**: Event-driven with automated subsystems  
**Design Patterns**: Factory, Observer, Pub/Sub (EventBus)

---

**End of User Manual** | For questions, check README.md or review source code comments.

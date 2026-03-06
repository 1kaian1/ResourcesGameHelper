# ResourcesGameHelper

**ResourcesGameHelper** is a companion app for the mobile geolocation economic simulation game **Resources** ([https://hq.resources-game.ch/en/](https://hq.resources-game.ch/en/)). The game simulates a real-world mining and production economy based on your GPS location. This helper app allows players to optimize their mining empire, track profits in real-time, and plan future expansions more efficiently.

## 🚀 Current Features

The app provides several specialized modules covering key aspects of the game:

### 1. Mines Management
- **Complete Overview**: Fetches a full list of your mines directly from the official API.
- **Smart Grouping**: Mines are automatically grouped by resource type into expandable/collapsible sections.
- **Real-time Monitoring**: Condition and production (m³/h) values update every second based on the game's precise decay rate (4% per 24 hours).
- **Advanced Sorting**: Sort your mines by Quality (Q), current Condition, or Defense strength, with a toggle for ascending or descending order.

### 2. Detailed Analytics & History
- **Financial Breakdown**: Calculates current hourly income, hourly loss due to wear, and estimated total earnings since construction.
- **Performance Charts**: Interactive line charts showing the history of Condition, Income, and Lost Income.
- **Maintenance Tracking**: Persistent storage of maintenance events. The app automatically records new timestamps from the API and allows manual management.

### 3. Factory Profitability
- **Live Profit Calculation**: Uses real-time market prices and static production data to calculate the net profit of every factory per hour.
- **Efficiency Ranking**: Factories are sorted by profit to identify the most lucrative production lines.

### 4. Expansion & Optimization
- **Dynamic Pricing**: Calculates the exact price of your next mine, accounting for the 2% linear price increase.
- **HQ Optimization**: Finds the mathematically optimal spot for your HQ to cover the maximum number of mines within its 145m radius.

## 🛠 Getting Started

### Prerequisites
- An Android device or emulator running **Android 7.0 (API 24)** or higher.
- An active Internet connection.
- A personal **API Key** from the Resources game (obtainable in the game settings or profile page).

### Configuration
Before running the app, you must configure your API key:
1. Open the file: `app/src/main/java/at/uastw/resourcesgamehelper/Config.kt`.
2. Locate the `API_KEY` constant.
3. Replace the placeholder string with your actual API key:
   ```kotlin
   object Config {
       const val API_KEY = "YOUR_ACTUAL_API_KEY_HERE"
       const val BASE_URL = "https://api.resources-game.ch/"
   }
   ```

### Installation
1. Clone this repository to your local machine.
2. Open the project in **Android Studio**.
3. Sync the project with Gradle files.
4. Click **Run** or use `./gradlew installDebug` via terminal.

## 📅 Future Roadmap

Ideas for future development:

- **Conflict Analysis**: Calculate total losses caused by specific players' attacks on your mines.
- **Maintenance Strategy**: Determine the mathematically ideal moment for individual or global repairs/overhauls.
- **Defense Optimization**: Intelligent distribution of defense units based on attack patterns and available capital.
- **Offensive Intelligence**: Tools to identify the most effective targets for resource raiding or mine destruction.
- **Activity Visualization**: A combined graph comparing your attack frequency vs. other players over time.
- **Anomalies Detection**: Display and analyze individual data deviations in production and decay.
- **Global Map View**: Integrated map showing the geographical coverage and distribution of your empire.
- **Logistics & Upgrades**: Cost calculators for warehouse expansions and factory updates.
- **Micro-Repair Analytics**: Predict basic repair costs for 1-minute or 1-hour intervals.
- **Goal Tracking**: Set and monitor long-term financial targets (e.g., reaching 1T or 2T total value).
- **Leaderboard Projections**: Calculate rank points and estimate the time required to reach milestones like 1 billion points.
- **Advanced Economic Logic**: 
    - Determine the "diminishing returns" point for low-tier mines (e.g., when to stop building Clay pits).
    - Analyze the correlation between earnings growth and inflation.
    - Calculate the exact pivot point where switching investment from Clay to Diamond mines becomes more profitable.

## 📝 Technical Notes
- Some values (ROI, exact historical production) are calculated using retrospective estimations.

---
*Disclaimer: This is an unofficial fan-made tool and is not affiliated with the creators of the Resources game.*

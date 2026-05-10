<div align="center">

# AgriTwin-ZM

### A Digital Simulation Platform for Maize and Tomato Supply Chains in Zambia

![Java](https://img.shields.io/badge/Java-11%2B-007396?style=flat&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17%2B-0277BD?style=flat&logo=java&logoColor=white)
![Build](https://img.shields.io/badge/build-passing-2D8648?style=flat)
![Tests](https://img.shields.io/badge/tests-18%2F18%20passing-2D8648?style=flat)
![Course](https://img.shields.io/badge/BIT%20213%20%2F%20BSC%20120-University%20of%20Lusaka-0F3B1E?style=flat)
![Status](https://img.shields.io/badge/version-1.0-FEF3D7?style=flat&labelColor=B36B00&color=FEF3D7)

*Simulate seasonal supply, demand, post-harvest losses, dynamic pricing,
and Food Reserve Agency policy alerts — all from a single Java console
or JavaFX window.*

</div>

---

## Contents

- [Why this project exists](#why-this-project-exists)
- [What AgriTwin-ZM does](#what-agritwin-zm-does)
- [Demo](#demo)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Usage](#usage)
- [Running the tests](#running-the-tests)
- [Architecture at a glance](#architecture-at-a-glance)
- [Future roadmap](#future-roadmap)
- [Documentation](#documentation)
- [Team](#team)
- [Acknowledgements](#acknowledgements)
- [Licence](#licence)

---

## Why this project exists

Zambia loses an estimated **18–30 % of its maize harvest** each year between
field and market — a silent shock that costs smallholder farmers their income
and the country its food security buffer. Tomato losses are worse, often
exceeding 40 % in regions without cold-chain access. Decisions made by the
**Food Reserve Agency (FRA)** and the Ministry of Agriculture — when to
release reserve stock, when to restrict exports, when to step in on prices —
currently rely on lagging spreadsheets and gut feel.

There is no widely available digital tool a Zambian policy-maker can open in
the morning, plug in this season's production estimates, and watch the
likely consequences play out month by month.

**AgriTwin-ZM** is a working prototype of exactly that tool.

---

## What AgriTwin-ZM does

You enter the production volume, expected demand, storage parameters and
pricing for a maize or tomato season. The simulator then runs the season
month-by-month and shows you:

- **Compounded post-harvest losses** — exponential decay for stored maize,
  linear daily spoilage for tomatoes, with cold storage cutting the spoilage
  rate by 40 %.
- **Dynamic price elasticity** — when a surplus persists, the simulated
  market price drops; when a shortage appears, it rises.
- **Food Security Indicator** — the percentage of demand covered by
  available production, capped at 100 %.
- **Live policy alerts** — FRA reserve threshold, export-review trigger,
  maize storage time warning, tomato shelf-life critical alert.
- **Persistent reports** — every simulation can be exported to a readable
  `.txt` report or a structured `.dat` file that can be reloaded later.

All of this runs in either a console menu (always available, vanilla JDK)
or an optional three-tab JavaFX window (Inputs / Results / Reports).

---

## Demo

```text
============================================================
  AgriTwin-ZM v1.0  --  Digital Twin for Zambian Agriculture
  Maize & Tomato Supply Chain Simulation
============================================================

---------------- MAIN MENU -----------------
  1) Run Maize Simulation
  2) Run Tomato Simulation
  3) View Reports (this session)
  4) Load Previous Simulation (from file)
  5) Save Current Simulation (to file)
  6) Exit
--------------------------------------------
Choose an option: 1

>> Maize Simulation Setup
Enter production volume (tons): [10000.0] <enter>
Enter projected demand (tons): [8000.0] <enter>
Number of months to simulate: [6] 3

------------------------------------------------------------
  AgriTwin-ZM v1.0 -- Month 1 Report (Maize)
------------------------------------------------------------
  Production            : 9700.00 tons
  Demand                : 2666.67 tons
  Surplus / Shortage    : 7033.33 tons (surplus)
  Waste / Spoilage      : 300.00 tons
  Revenue               : ZMW 18,666,666.67
  Food Security Indicator: 100.00%
  Policy Alerts         : FRA RESERVE OK; EXPORT REVIEW REQUIRED
                          (72.5% of production); STORAGE OK;
------------------------------------------------------------
```

---

## Tech stack

| Layer | Technology | Notes |
| --- | --- | --- |
| Language | Java 11+ | Vanilla JDK, no external runtime libraries |
| Optional GUI | JavaFX 17+ | Loaded reflectively — console always works without it |
| Build | `javac` + `.bat` scripts | No Maven, Gradle, or JUnit required |
| Persistence | Pipe-delimited text files | Human-readable, hand-inspectable |
| Testing | Plain-main `TestRunner` | 18 unit tests, no third-party dependencies |
| IDE | Apache NetBeans (any version) | Project layout is also IntelliJ / Eclipse friendly |

**Why no external libraries?** This is a BIT 213 / BSC 120 academic
prototype. Keeping the dependency surface zero means the marker, a future
maintainer, or a cloud sandbox can build and run the project with nothing
more than a working JDK installation.

---

## Project structure

```text
AgriTwin-ZM/
├── src/
│   └── agritwin/
│       ├── Main.java                         ← entry point (console default)
│       ├── TestRunner.java                   ← 18 unit tests
│       ├── model/
│       │   ├── Commodity.java                ← abstract base class
│       │   ├── Maize.java                    ← maize commodity POJO
│       │   ├── Tomato.java                   ← tomato commodity POJO
│       │   ├── SimulationRecord.java         ← per-month snapshot DTO
│       │   └── StorageType.java              ← COLD / OPEN enum
│       ├── service/
│       │   ├── SupplyDemandEngine.java       ← surplus, revenue, price elasticity, FSI
│       │   ├── StorageLossSimulator.java     ← compounded loss + spoilage
│       │   ├── PolicyEngine.java             ← FRA / export / storage rules
│       │   ├── ReportGenerator.java          ← console + .txt + .csv report output
│       │   └── Severity.java                 ← INFO / WARNING / CRITICAL enum
│       ├── util/
│       │   ├── FileHandler.java              ← save / load .dat files
│       │   └── RecordStore.java              ← persistence interface (DI seam)
│       └── ui/
│           ├── ConsoleUI.java                ← Scanner-based menu
│           ├── GuiApp.java                   ← optional JavaFX window
│           ├── DashboardController.java      ← 6-page JavaFX dashboard
│           └── components/
│               ├── AlertPanel.java           ← FRA policy status panel
│               ├── SidebarMenu.java          ← sidebar navigation
│               └── StatCard.java             ← metric card with progress bar
├── docs/
│   ├── UML_ClassDiagram.puml                 ← PlantUML source
│   └── UML_ClassDiagram.txt                  ← text-only fallback
├── build.bat                                 ← Windows build (console-only)
├── build_with_javafx.bat                     ← Windows build (with JavaFX)
├── BUILD.txt                                 ← plain-text build instructions
└── README.md                                 ← this file
```

Every `.java` file starts with a comment block listing its module, purpose,
the OOP concepts it demonstrates, the data structures it uses, and the
algorithms it implements — designed to lift straight into the technical
report.

---

## Getting started

### Prerequisites

- **JDK 11 or newer.** Run `javac -version` to confirm.
- **Optional:** JavaFX SDK 17+ if you want the GUI front-end. Get it from
  [Gluon](https://gluonhq.com/products/javafx/).

### Clone and build (console-only)

```bash
git clone <your-repo-url> AgriTwin-ZM
cd AgriTwin-ZM
mkdir -p out
find src -name "*.java" ! -name "GuiApp.java" > sources.txt
javac -d out @sources.txt
```

On Windows, just run **`build.bat`** instead.

### Build with the JavaFX GUI

```bash
export JAVAFX_LIB=/path/to/javafx-sdk-25/javafx-sdk-25.0.3/lib
javac --module-path "$JAVAFX_LIB" --add-modules javafx.controls -d out \
      $(find src -name "*.java")
```

On Windows: set `JAVAFX_LIB=C:\javafx-sdk-25\javafx-sdk-25.0.3\lib` and run **`build_with_javafx.bat`**.

---

## Usage

### Console mode (default)

```bash
java -cp out agritwin.Main
```

Pick an option from the main menu, accept default values with Enter, and
the simulator will walk you month by month through a season.

### GUI mode (requires JavaFX)

```bash
java --module-path "$JAVAFX_LIB" --add-modules javafx.controls \
     -cp out agritwin.Main --gui
```

A window opens with a sidebar and six pages:

1. **Dashboard** — commodity tabs, stat cards, live/demo toggle, recent simulations, FRA policy panel.
2. **Simulation** — input form with maize/tomato tabs, Run button, stat cards, bar/line/pie charts, policy evaluation.
3. **Storage** — compounded maize storage-loss chart across three moisture levels.
4. **Policy Alerts** — interactive FRA/export/storage/shelf-life checker with custom inputs.
5. **Reports** — full textual season summary generated after each simulation run.
6. **Settings** — application version, build info, and policy threshold reference.

If JavaFX isn't on the classpath, `Main` falls back to the console UI
gracefully — never crashes.

---

## Running the tests

```bash
java -cp out agritwin.TestRunner
```

Expected output:

```text
============================================================
 AgriTwin-ZM v1.0 -- Test Runner
============================================================
  [PASS] Test 1  (Surplus 10000-8000)
  [PASS] Test 2  (oversupply lowers price)
  [PASS] Test 3  (Maize 3% loss x3 months)
  [PASS] Test 4  (cold < open spoilage)
  [PASS] Test 5  (FSI when prod<demand)
  [PASS] Test 6  (FRA alert at 400k)
  [PASS] Test 7  (save + reload roundtrip)
  [PASS] Test 8  (zero days -> zero waste)
  [PASS] Test 9  (negative production throws)
  [PASS] Test 10 (load missing file throws)
  [PASS] Test 11 (infinite stock -> FRA RESERVE OK)
  [PASS] Test 12 (zero surplus keeps price)
  [PASS] Test 13 (dry-season demand > wet-season demand)
  [PASS] Test 14 (moisture 18% leaves less than 12%)
  [PASS] Test 15 (loss schedule size = 7 for 6 months)
  [PASS] Test 16 (aflatoxin HIGH at moisture=16, months=3)
  [PASS] Test 17 (season summary total production = 18000)
  [PASS] Test 18 (CSV header starts with monthNumber,commodityName)
------------------------------------------------------------
 Results: 18 passed, 0 failed
============================================================
```

The 18 tests cover: surplus calculation, price elasticity, compounded maize
loss, cold vs open tomato spoilage, food security indicator, FRA reserve alert,
file save-and-reload round-trip, zero-days waste, validation exceptions, missing
file IOException, infinite stock, zero-surplus price, seasonal demand modelling,
moisture-adjusted loss, loss schedule size, aflatoxin detection, season summary
aggregation, and CSV export header.

In NetBeans: open `TestRunner.java`, right-click anywhere in the editor →
**Run File** (`Shift+F6`).

---

## Architecture at a glance

AgriTwin-ZM follows the **MVC + Service Layer** pattern. Calls only flow
downward — a class in the UI layer can call a class in the Service layer,
but no class in the Model layer ever reaches up into the UI.

```text
┌──────────────────────────────────────────────────────────────┐
│  ENTRY POINT     Main · TestRunner                           │
├──────────────────────────────────────────────────────────────┤
│  UI LAYER        ConsoleUI · GuiApp                          │
├──────────────────────────────────────────────────────────────┤
│  SERVICE LAYER   SupplyDemandEngine · StorageLossSimulator   │
│                  PolicyEngine · ReportGenerator              │
├──────────────────────────────────────────────────────────────┤
│  UTIL LAYER      FileHandler                                 │
├──────────────────────────────────────────────────────────────┤
│  MODEL LAYER     Maize · Tomato · SimulationRecord           │
└──────────────────────────────────────────────────────────────┘
```

This layering is what lets us swap the console for a JavaFX GUI — and a
future REST controller, or a mobile front-end — without touching a single
line of business logic.

---

## Future roadmap

AgriTwin-ZM v1.0 is a foundation, not a finish line. The architecture was
deliberately built to scale:

### v2.0 — Real data, real forecasts

- **IoT moisture and temperature sensors** at FRA grain depots feed live
  spoilage parameters into the simulator.
- **AI demand forecasting** using historical Zambia Statistics Agency data
  replaces the hand-entered demand inputs.
- **Multi-region simulation** so a single run can compare Eastern Province
  to Lusaka district side by side.

### v3.0 — Production deployment

- **Blockchain traceability** linking each batch from farmer → trader →
  retailer.
- **Cloud-deployed back-end** with SQL persistence replacing the flat-file
  layer.
- **Mobile interface for smallholder farmers** delivering price signals
  and storage advice in real time over USSD or a lightweight Android app.

The clean MVC + Service Layer means each of these phases plugs in without
rewriting the simulation core.

---

## Documentation

Three documents accompany the codebase, each written for a different
reader:

| Document | For | What it covers |
| --- | --- | --- |
| `README.md` | Anyone visiting the repo | This file. Project overview, how to run it. |
| Code Explanation Guide *(separate `.docx`)* | The 10 team members | Per-role walkthroughs and Q&A prep for the May 13 pitch. |
| Developer Documentation *(separate `.docx`)* | A future maintainer | Full module API reference, data flow, extension points, gotchas. |
| `BUILD.txt` | Anyone building from source | Plain-text install + run instructions. |
| `docs/UML_ClassDiagram.puml` | System designers | PlantUML source for the class diagram. |

---

## Team

Built by a 10-person team of BIT 213 / BSC 120 students at the
**University of Lusaka**:

- **Sangu** — Integration Lead & Project Manager (`Main.java`, demo, integration)
- **9 team members** — supply-demand engine, storage loss simulator,
  policy engine, reporting, UML & system design, algorithm modelling,
  QA & testing, technical writing, and pitch direction

The project was developed for the BIT 213 / BSC 120 final assessment with
a pitch presentation to the Ministry of Agriculture, Food Reserve Agency,
and agritech incubator representatives on **13 May 2026**.

---

## Acknowledgements

- **Food Reserve Agency (FRA)** for the published Strategic Maize Reserve
  policy framework that anchors the policy thresholds in
  `PolicyEngine.java`.
- **Ministry of Agriculture, Republic of Zambia** for the agricultural
  context that motivates the entire simulation.
- **Food and Agriculture Organization of the United Nations (FAO)** for
  the post-harvest loss modelling literature that informed the compound
  decay formulas.
- **Zambia Statistics Agency** for the Crop Forecast Survey reports used
  to calibrate price elasticity coefficients.
- **University of Lusaka, School of Information and Communications
  Technology** — and our BIT 213 / BSC 120 lecturer — for setting an
  assignment ambitious enough to be worth pitching outside the classroom.

---

## Licence

**Copyright © 2026 AgriTwin-ZM Team, University of Lusaka. All rights reserved.**

This project was developed as an academic submission for **BIT 213 / BSC 120
— Introduction to Programming (Java)** at the University of Lusaka. The
source code is published on GitHub for educational reference and academic
review only.

You may **read** the code and the accompanying documentation. You may
**not**:

- Reuse the code, in whole or in part, in another academic submission.
- Redistribute the code in compiled or source form.
- Modify and republish the code under your own name.
- Use the code in any commercial product or service.

For permission to use any part of this work outside the listed exceptions,
contact the project lead through the issue tracker.

---

<div align="center">

*AgriTwin-ZM is a research prototype, not a production system.
Decisions affecting national food security should always be made
with current field data and qualified human judgement.*

**🌽 🍅 🇿🇲**

</div>

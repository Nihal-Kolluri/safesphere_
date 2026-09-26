# SafeSphere — Emergency Orchestration Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)]()
[![Security](https://img.shields.io/badge/Cryptography-AES--256--GCM-blue.svg)]()
[![AI CV](https://img.shields.io/badge/AI%20Vision-YOLOv8-blueviolet.svg)]()
[![Database](https://img.shields.io/badge/Database-SQLite%20JDBC-lightgrey.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)]()

> **Intelligent, Resilient Emergency Orchestration Platform**  
> *Developed for Hackathon Sprint · Chaitanya Bharathi Institute of Technology*  
> **Lead Architect:** Katukuri Saketh · **Repository Owner:** Nihal Kolluri

---

## 1. Executive Summary

**SafeSphere** is not just another SOS button; it is an intelligent, resilient emergency-orchestration platform. It detects crises through sensor fusion, gathers contextual telemetry, preserves device evidence via cryptography, dynamically conserves remaining device power through software-level degradation (**Survival Mode**), and coordinates dispatch between victims, verified responders, 112 command hubs, and autonomous IoT agents.

### Strict Separation of Concerns: Deterministic Safety
- **AI (Computer Vision / YOLOv8):** Used strictly for **interpretation** and contextual hazard verification (identifying weapons, fire, or vehicular crash impact).
- **Deterministic Rules Engine (Java FSM):** Enforces all **safety-critical actions** (initiating countdown windows, dispatching responders, escalating to 112 ERSS, and encrypting evidence).

```
                        ┌───────────────────────────────────────────┐
                        │              DETECTION LAYER              │
                        │ (SOS / Voice / Simulated Sensors / CV)    │
                        └───────────────────────┬───────────────────┘
                                                │
            ┌───────────────────────────────────┼────────────────────────────────────┐
            ▼                                   ▼                                    ▼
 ┌─────────────────────┐          ┌──────────────────────────┐         ┌──────────────────────────┐
 │  M1  Victim Client  │          │  M2  Computer Vision     │         │  M3  Survival Engine     │
 │  (Java Swing UI)    │          │  (Python + YOLOv8)       │         │  (Battery/Network Rules) │
 └──────────┬──────────┘          └─────────────┬────────────┘         └─────────────┬────────────┘
            │ raw signals                       │ confidence scores                  │ telemetry limits
            └────────────────────────┬──────────┴──────────────────┬─────────────────┘
                                     ▼                             ▼
                        ┌───────────────────────────────────────────────────┐
                        │   M4  Core Orchestrator & State Machine           │
                        │   (Java FSM, AES-256 Crypto, Agent Gateway)       │
                        └───────────────────────┬───────────────────────────┘
                                                ▼ (Encrypted Capsule)
            ┌───────────────────────────────────┴────────────────────────────────────┐
            ▼                                                                        ▼
 ┌───────────────────────────────┐                                     ┌───────────────────────────────┐
 │  M5  Dispatcher Command Desk  │                                     │  M6  Relational Matcher       │
 │  (Java Swing, Tactical GIS)   │                                     │  (SQLite, JDBC)               │
 └───────────────────────────────┘                                     └───────────────────────────────┘
```

---

## 2. Core Modules Architecture

### **M1 — Victim Interface & Edge Telemetry (Java Swing)**
- Simulates a mobile Android operating environment.
- **SOS Button:** High-visibility emergency trigger with silent confirmation countdown.
- **Simulated Hardware Panel:** Live sliders for Battery % (0–100%), Network Quality (`STRONG`, `MODERATE`, `WEAK`, `OFFLINE`), and buttons for Crash Impact (8.6G spike) and Safe Corridor Route Deviation (+420m anomaly).
- **"I Can't Speak" Silent Triage:** One-tap questionnaire (`Injured?`, `Threat Nearby?`) packaged directly into the incident capsule.
- Built using `SwingWorker` for zero UI freezes on the Event Dispatch Thread (EDT).

### **M2 — Threat Verification (Python + YOLOv8 / Java Bridge)**
- Analyzes optical webcam frames or telemetry context using Ultralytics YOLOv8.
- Detects weapons (knives, firearms), fire/smoke plumes, and severe vehicular deformation.
- **Autonomous Bypass Rule:** If threat confidence exceeds **0.85**, it instantly skips the 10-second silent confirmation window and escalates straight to `EMERGENCY`.
- Features an autonomous deterministic Java fallback so the entire platform operates out of the box even without Python installed.

### **M3 — SafeSphere Survival Engine (Dynamic Power & Network Decay)**
- Dynamically adapts phone resource usage based on real-time battery and network telemetry:
  - **Battery > 50% (`OPTIMAL`):** GPS Polling: 5s | UI: Standard | Evidence: High-Res Video + Audio.
  - **15% < Battery ≤ 50% (`CONSERVATIVE`):** GPS Polling: 15s | UI: Standard | Evidence: Compressed Audio Only.
  - **Battery ≤ 15% (`EXTREME_SURVIVAL`):** GPS Polling: 45s heartbeat | UI: **Pitch Black (OLED zero-emission)** | Evidence: Disabled (Radio preserved for SOS beacon) | Non-essential UI buttons hidden.
- **Store-and-Forward Mesh Fallback:** When cellular connectivity drops to `OFFLINE`, packets are buffered locally and flushed automatically when reconnected.

### **M4 — Core Orchestrator & State Machine (Java Core + Cryptography)**
- **Deterministic FSM:** Strict state machine with states: `SAFE`, `SUSPICIOUS`, `CHECKING`, `EMERGENCY`, `ESCALATING`, `RESPONDER_ASSIGNED`, `RESOLVED`.
- Rejects illegal state jumps (e.g., `SAFE` $\rightarrow$ `RESPONDER_ASSIGNED` throws `IllegalStateException`).
- **Evidence Vault (AES-256-GCM):** Encrypts incident evidence with 128-bit authentication tag and SHA-256 digital fingerprint, ensuring tamper-evident chain of custody.
- **SafeSphereEventBus:** Thread-safe concurrent pub/sub bus decoupling all modules.
- **Embedded Agent REST Gateway:** Built-in HTTP server listening on port 8080 (`POST /api/v1/agent/trigger`) for IoT wearables and ride-share services.

### **M5 — Dispatcher Command Dashboard (Java Swing)**
- Dual-pane command console with `JSplitPane`:
  - **Left Pane:** Active Incident Queue table with live status badges.
  - **Right Pane:** Emergency Capsule details, Medical Profile, and auto-scrolling **Live Incident Timeline & Audit Log**.
- **Role-Based Access Control (RBAC) Selector:** Toggles between *Police / 112 Dispatcher View* (full coordinates, raw medical records) and *Volunteer View* (mathematically blurred distance, redacted medical history).
- **Tactical GIS Map:** Visual radar and map rendering route deviation and responder distance rings, with one-click export to an interactive browser Leaflet HTML map.

### **M6 — Relational Capability Matcher (SQLite JDBC)**
- Evaluates verified volunteers based on multi-factor capability weighting rather than crude proximity:
  $$\text{match\_score} = \left( \frac{1}{\text{distance\_km}} \times 0.4 \right) + (\text{cpr\_cert} \times 0.3) + (\text{vehicle\_access} \times 0.3)$$
- Proves that a responder with CPR certification and vehicle access further away (e.g. 0.8 km) receives a **higher priority score (1.10)** than an untrained volunteer closer by (0.4 km, score 1.00).

---

## 3. Core Data Model — Emergency Capsule

### Raw Encrypted Payload (Generated by M4)
```json
{
  "capsule_id": "CR-8924",
  "timestamp": "2026-09-26T22:28:40Z",
  "fsm_state": "ACTIVE_EMERGENCY",
  "telemetry": {
    "latitude": 17.3850,
    "longitude": 78.4867,
    "battery_level": 12,
    "network_quality": "WEAK",
    "crash_impact_g": 8.4
  },
  "medical_summary": "Blood: O+, Allergies: Penicillin, Emergency Contact: +91-98480-12345",
  "encrypted_evidence": "6c5Cp8sIIMWijvnDBBLjGrPzqWXtzKo7CT5ehtxx9id7ecEqLoc13+IaHlb6gsfz0CE..."
}
```

### Privacy-Preserved Filtered Output (RBAC: Volunteer View)
```json
{
  "capsule_id": "CR-8924",
  "fsm_state": "ACTIVE_EMERGENCY",
  "approximate_distance_meters": 450,
  "task_requirement": "Immediate First Aid Required. Victim cannot speak.",
  "eta_target_mins": 3
}
```

---

## 4. Quickstart & Execution Guide

### Prerequisites
- **Java 21 LTS** (or Java 17+)
- *Optional:* Python 3.10+ (for running YOLOv8 / FastAPI scripts; fallback is built-in)

### Zero-Friction Launch (One Command)
Run the convenience scripts to compile and launch the unified presentation suite:

#### On Windows (CMD / PowerShell):
```cmd
# Compile the project
build.bat

# Launch the Unified Side-by-Side Presentation Demo (Recommended for Judges!)
run.bat

# Run the Automated Test Suite (All 8 Verification Tests)
test.bat
```

#### On PowerShell:
```powershell
.\build.ps1
.\run.ps1
.\test.ps1
```

#### Launch Specific Modes:
```cmd
# Standalone Victim Mobile Client (M1 / M3)
java -cp "bin;lib/*" com.safesphere.Main --victim

# Standalone 112 Dispatch Command Desk (M5 / M6)
java -cp "bin;lib/*" com.safesphere.Main --dispatch

# Headless CLI Verification Mode
java -cp "bin;lib/*" com.safesphere.Main --cli
```

#### With Maven (Alternative):
```bash
mvn clean package
mvn test
java -jar target/safesphere-1.0.0.jar
```

---

### 📱 Native Android Mobile App (`android/`)

SafeSphere includes a full native Android project under [`android/`](file:///d:/Sem%20Project/safesphere/android) implementing the Victim Client (M1) and Survival Mode (M3) connected directly to **real Android hardware APIs**:

- **Real Battery Monitoring:** Uses Android `BatteryManager` broadcast receiver to dynamically trigger the `SurvivalEngine` decay loop.
- **Real Crash Impact Detection:** Uses Android `SensorManager` with `Sensor.TYPE_ACCELEROMETER` to detect severe physical deceleration spikes ($>4.5G$).
- **Real Carrier / Wi-Fi Monitoring:** Uses `ConnectivityManager.NetworkCallback` to automatically engage the `MeshStoreAndForward` offline buffer when disconnected.
- **OLED Extreme Survival Transformation:** At $\le 15\%$ battery, automatically switches the window to `#000000` pitch black, lowers window brightness, and hides non-essential views.
- **Native AES-256-GCM Evidence Vault:** Runs natively on Android using `javax.crypto`.

#### Building the Android APK:
```cmd
# One-click build via batch script:
build_android.bat

# Or via Gradle inside android/:
cd android
.\gradlew assembleDebug
```
The output APK is generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

#### Installing on an Android Device or Emulator:
```cmd
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```
Or open the `android/` folder directly in **Android Studio** and click **Run (Shift+F10)**!

---

## 5. Live 5-Minute Demo Script (For Pitch & Judging)

Follow this exact flow during hackathon presentations:

1. **Start Baseline Monitoring:** Launch `run.bat`. Show the mobile client on the left and the 112 Dispatch desk on the right. Both show `SAFE` status.
2. **Simulate Route Deviation:** Click **"Route Deviation"** on the mobile simulator. Watch the state transition to `SUSPICIOUS` and the 10-second silent confirmation timer begin.
3. **Trigger CV Threat Verification:** Click **"CV Weapon/Fire"**. Watch the YOLOv8 threat analyzer identify the hazard frame (confidence: 0.94 > 0.85). Show that it **instantly bypasses the countdown** directly to `EMERGENCY`!
4. **Demonstrate Survival Mode:** Slide the **Battery Telemetry slider down to 12%**.
   - Watch the mobile screen instantly snap to **Pitch Black (OLED zero-emission mode)**.
   - Non-essential UI buttons vanish to conserve rendering power.
   - The Dispatcher timeline prints: `[M3_SURVIVAL] CRITICAL: Extreme Survival Mode Engaged! GPS throttled to 45s heartbeat`.
5. **Toggle RBAC Perspective:** On the Dispatch desk, switch the dropdown from **"Police Dispatcher View"** to **"Volunteer View"**.
   - Show the exact coordinates vanish and become `~450 meters away (Approximate Distance)`.
   - Show medical records become `[REDACTED FOR VOLUNTEER PRIVACY]`.
6. **Execute Capability Dispatch (M6 Matcher):** Click **"Dispatch Best Match (M6)"**.
   - Explain to the judges why **Dr. Ananya Reddy (VOL-204, 0.8 km away, CPR=YES, Vehicle=YES)** was chosen over Ramesh Sharma (VOL-101, 0.4 km away, CPR=NO, Vehicle=NO).
   - Capability weighting mathematically prioritizes life-saving skills over crude distance.
7. **Inspect Tactical GIS Map:** Click **"Interactive Tactical Map"** to display the visual radar rings, victim marker, route deviation path, and assigned responder vectors, or click **"Open in Browser"** to launch the Leaflet map.

---

## 6. External IoT / Agent REST API

SafeSphere includes an embedded API gateway running on `http://localhost:8080`. External IoT wearables and ride-hailing platforms can trigger emergency incidents programmatically:

### `POST /api/v1/agent/trigger`
```bash
curl -X POST http://localhost:8080/api/v1/agent/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "sensor_type": "ACCELEROMETER_CRASH_SPIKE",
    "intensity_g": 8.4,
    "threat_hint": "fire"
  }'
```

**Response:**
```json
{
  "capsule_id": "CR-8924",
  "fsm_state": "EMERGENCY",
  "telemetry": {
    "latitude": 17.385,
    "longitude": 78.4867,
    "crashImpactG": 8.4
  },
  "threat_verification": {
    "hazardDetected": true,
    "hazardType": "FIRE",
    "confidence": 0.92
  },
  "encrypted_evidence": "6c5Cp8sIIMWijvnDBBLjGrPzqWXtzKo7CT5ehtxx9id7ecEqLoc13..."
}
```

---

## 7. Automated Test Suite

Run `test.bat` or `test.ps1` to verify all 8 test suites:

```text
=========================================================
  SAFESPHERE AUTOMATED VERIFICATION TEST SUITE
=========================================================
  [PASS] FSM Legal State Transitions
  [PASS] FSM Illegal Transition Rejection (SAFE -> RESPONDER_ASSIGNED rejected)
  [PASS] CV Threat Verification (Confidence > 0.85 bypasses countdown)
  [PASS] Evidence Vault AES-256-GCM Encryption / Decryption
  [PASS] AES-256-GCM Tamper Detection (AEAD Bad Tag detected)
  [PASS] Resource Conservation Model (Survival Mode Decay)
  [PASS] M6 Relational Capability Matcher (Section 10 Weighted Formula)
  [PASS] Role-Based Access Control (RBAC Data Blurring & Redaction)
---------------------------------------------------------
  TOTAL: 8 | PASSED: 8 | FAILED: 0
=========================================================
```

---

## 8. Technology Stack Summary

| Layer | Technology | Rationale |
|---|---|---|
| **User Interfaces (M1, M5)** | Java Swing | Fast desktop executables; zero cloud frontend latency risks during hackathon demos. |
| **Core Orchestrator (M4)** | Java 21 LTS | Strict typing, robust concurrency (`java.util.concurrent`), native AES-256-GCM. |
| **Threat CV (M2)** | Python + YOLOv8 + OpenCV | Object detection for weapons/hazards; integrated via `ProcessBuilder` with autonomous fallback. |
| **Relational Matcher (M6)** | SQLite + JDBC | Zero-config SQL engine; handles complex capability weighting queries natively. |
| **Agent API Gateway** | Embedded Java HTTP & Python FastAPI | Exposes OpenAPI / REST endpoints for external IoT hardware triggers. |
| **Tactical GIS Maps** | Leaflet / Folium & Swing 2D Graphics | Visual tactical overlays for route deviation vectors and volunteer distance rings. |

---

## 9. Team Structure & Hackathon Sprint Allocation

- **Backend & Core Lead (M4, Cryptography, Agent Gateway):** State machine validation, AES-256 crypto vault, EventBus, REST API.
- **Edge UI & CV Engineer (M1, M2, M3):** Swing mobile client, YOLOv8 threat analyzer, Survival mode OLED theme loop.
- **Dispatch & Data Engineer (M5, M6):** Dispatcher command dashboard, SQLite JDBC relational capability matcher, tactical GIS mapping.

---
*SafeSphere — Turning emergency response into resilient, intelligent infrastructure.*

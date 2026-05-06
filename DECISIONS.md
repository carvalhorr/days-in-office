# Days in Office — Project Decisions Log

> This document captures the key requirements and decisions made during the planning phase, in enough detail to replicate the process from scratch.

---

## 1. Problem Statement

The user works at a company with a **50% in-office mandate**. They wanted an Android app that:
- Automatically tracks which days they were at the office
- Calculates whether they are meeting the mandate
- Excludes non-working days (weekends, public holidays, PTO) from the calculation

---

## 2. App Requirements

### Detection Methods
The requirement was to support **all of the following**, letting each user pick their preferred method:

| Method | Reason included |
|---|---|
| Wi-Fi connected SSID | User connects to a single known office Wi-Fi network |
| Wi-Fi scan (without connecting) | For users on mobile data who don't join the office network |
| Geofencing | GPS-based, for users who prefer location detection |
| Manual check-in | Full control, always available as fallback |

A user may combine methods (any positive result counts as an office day).

### Mandate Calculation
- Default target: **50%**
- Configurable target and period
- Periods supported: **Weekly, Monthly, Quarterly, Rolling 4 weeks**
  - Quarterly was added explicitly (Q1 = Jan–Mar, Q2 = Apr–Jun, Q3 = Jul–Sep, Q4 = Oct–Dec)
- Days excluded from calculation: weekends, public holidays, PTO

### Holiday and PTO Data
The company uses **Workday** for HR. Direct Workday API integration was considered but deferred — it requires IT involvement to enable OAuth 2.0 endpoints. The chosen approach:
- Read PTO and public holidays from the **device calendar** (Google Calendar / Outlook), which most Workday deployments sync to automatically
- Manual entry as fallback
- Workday API deferred to a future phase

---

## 3. Key Technical Decisions

### Minimum SDK: 28 (Android 9.0)
Wi-Fi SSID detection (both connected and scan) requires `ACCESS_FINE_LOCATION` from Android 9 (API 28) onwards. Setting `minSdk = 28` eliminates the need for version-branched permission handling and aligns the minimum with the most restrictive detection feature.

### Wi-Fi Detection on API 31+
`WifiManager.connectionInfo` was deprecated in API 31. The `WifiConnectedDetector` uses a version switch:
- API >= 31: `ConnectivityManager.getNetworkCapabilities(activeNetwork)?.transportInfo as? WifiInfo`
- API < 31: `WifiManager.connectionInfo.ssid` with `@Suppress("DEPRECATION")` scoped to that branch only

### Application ID
`com.carvalhorr.daysInOffice` — updated from the initial placeholder `com.daysInOffice` to include the developer's namespace.

### Architecture Pattern
MVVM + Clean Architecture with three layers:
- `core/domain` — pure Kotlin, zero Android dependencies
- `core/data` — Room, DataStore, ContentProvider
- `feature/*` — Compose UI + ViewModels

Chosen because it maps well to agent-based task decomposition: domain, data, and UI tasks are independent and can be implemented in sequence without circular dependencies.

### Agent-Driven Implementation
The implementation plan was designed from the start to be executed by **AI coding agents**, not humans directly. Each task in `TASKS.md` has:
- Exact file paths to create
- Step-by-step implementation details
- Acceptance criteria as a checklist
- Copy-paste QA verification shell commands

This allows agents to pick up, implement, verify, and hand off tasks autonomously.

---

## 4. Experiment Design Decisions

### Why a Benchmark Experiment
Instead of a single implementation, the decision was made to run the same app implementation across multiple **AI coding tools** and **LLM models** to measure comparative performance. The goal is to produce a data-driven comparison of which combination works best for Android development.

### Infrastructure
- Local LLM server: **Ollama** running on a local server at `192.168.68.74`
- Server GPU: **NVIDIA RTX 3090 Ti** (24 GB VRAM)
- Models selected to fit within 24 GB VRAM

### Models Selected
| Model | Reason |
|---|---|
| `gemma4:31b` | Already available; Google's latest |
| `devstral` | Mistral's purpose-built coding agent model |
| `qwen2.5-coder:32b` | Highest coding benchmark scores in class |
| `deepseek-r1:32b` | Strong reasoning model, good at complex logic |

`kimi-k2` was considered but rejected — only available as a 1T cloud API call via Ollama, not locally runnable.

### Tools Selected
| Tool | Reason |
|---|---|
| Aider | Mature CLI coding agent, fully scriptable via `--message` |
| OpenHands | Open-source agent platform, headless mode available |
| Goose | Block's agent, uses OpenAI-compatible endpoint |

### Experiment Structure
Full matrix: **4 models × 3 tools = 12 runs × 20 tasks = 240 task executions**.

Run order: all models for one tool before moving to the next (minimises GPU model swap overhead).

### Ollama Setup
Ollama was already installed on the server as a systemd service but was bound to `127.0.0.1` only. The service was updated to add `Environment="OLLAMA_HOST=0.0.0.0"` to `/etc/systemd/system/ollama.service` so it listens on all interfaces, enabling remote access from the development machine.

---

## 5. Replication Checklist

To replicate this project planning from scratch:

1. Define the app: automatic office day tracking, configurable mandate percentage and period, multiple detection methods, calendar integration for PTO/holidays.
2. Decide on detection methods: Wi-Fi connected SSID, Wi-Fi scan, geofencing, manual.
3. Set `minSdk = 28` for Wi-Fi detection compatibility.
4. Set application ID to your namespace: `com.<you>.daysInOffice`.
5. Choose MVVM + Clean Architecture; define domain models before data or UI.
6. Write `ARCHITECTURE.md` defining all package structure, class names, interfaces, and invariants before any code.
7. Write `TASKS.md` breaking the implementation into ~20 atomic tasks with acceptance criteria and QA commands.
8. Write `CLAUDE.md` as the agent operating manual (how to pick up tasks, commit conventions).
9. Set up Ollama on a local GPU server with `OLLAMA_HOST=0.0.0.0` in the systemd service.
10. Pull the models you want to benchmark.
11. Write `EXPERIMENT.md` defining the tool × model matrix, isolation strategy, metrics, and comparison report format.

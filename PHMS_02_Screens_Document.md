# PHMS — Screens Document

**Purpose of this document**: Every screen the app needs, with its purpose, UI elements, states (empty/loading/error), and available user actions. Use this alongside the Module Breakdown when building UI or briefing a design tool.

**Navigation pattern**: Bottom navigation bar for top-level sections (Dashboard, Pigs, Alerts, Feed, More) + drawer or "More" screen for secondary modules (Breeding, Market, Reports, Settings).

---

## 1. Dashboard

**Purpose**: Landing screen — farm health at a glance, fast access to common actions.

**Elements**:
- KPI cards (horizontal scroll or grid): Total active pigs, Active alerts count, Pigs market-ready, This month's mortality rate
- "Active Alerts" list (top 3-5, priority-sorted) with tap-through to Alerts screen
- Quick-action buttons: Add Pig, Log Weight, Log Feeding, Record Health Event
- Recent activity feed (last 5-10 logged events across modules)

**States**:
- Empty (no pigs registered yet): show onboarding prompt "Register your first pig"
- Loading: skeleton cards
- Normal: populated KPIs and alerts

**Actions**: Tap KPI card → drill into relevant module; tap alert → Alert detail; tap quick action → relevant Add/Log screen

---

## 2. Pig List

**Purpose**: Browse, search, and filter the full herd.

**Elements**:
- Search bar (by tag number or name)
- Filter chips: Batch, Pen, Status (Active/Sold/Dead/Culled), Stage
- List/grid toggle
- Each row: photo thumbnail, tag number, breed, current stage badge (color-coded), status icon, current weight
- Floating action button: Add Pig

**States**:
- Empty (no results matching filter): "No pigs match your filters"
- Empty (no pigs at all): onboarding CTA
- Loaded: scrollable list, lazy-loaded for large herds

**Actions**: Tap row → Pig Profile; tap FAB → Add/Edit Pig; long-press → quick actions (log weight, promote)

---

## 3. Pig Profile

**Purpose**: Full record for one pig — the most-visited screen in the app.

**Elements** (tabbed layout):
- Header: photo, tag number, breed, sex, age, current stage badge, status
- Tab: **Overview** — key stats (current weight, ADG, FCR, days in current stage)
- Tab: **Weight Chart** — line chart of weight over time (MPAndroidChart), "Log Weight" button
- Tab: **Breeding** — (if sow/boar) reproductive history, upcoming events
- Tab: **Health** — vaccination history, treatments, active withdrawal flag if applicable
- Tab: **Stage History** — timeline of all past promotions with dates/weights
- Prominent action buttons: "Log Weight", "Promote", "Edit"

**States**:
- Loading
- Error (pig not found — e.g., stale QR scan pointing to archived pig)
- Withdrawal-active banner (red, persistent) if pig currently under drug withdrawal

**Actions**: Log Weight → inline dialog or bottom sheet; Promote → Promotion flow; Edit → Add/Edit Pig screen pre-filled

---

## 4. Add/Edit Pig

**Purpose**: Register a new pig or edit an existing record.

**Elements**:
- Form fields: Tag number (with QR scan button to auto-fill from scanned tag), Breed (dropdown), Sex (toggle M/F), Date of birth (date picker), Source (Born on farm / Purchased — toggle), Pen (dropdown), Batch (dropdown, optional), Photo capture/upload
- Save / Cancel buttons

**States**:
- New (empty form, all defaults)
- Edit (pre-filled from existing record)
- Validation errors (duplicate tag number, missing required field)

**Actions**: Scan QR → auto-fill tag; Save → validate and persist, return to Pig Profile or Pig List

---

## 5. Promotion Dashboard

**Purpose**: Central place to act on all pending stage promotions — individual and batch.

**Elements**:
- Grouped list by current stage ("5 pigs ready: Grower → Finisher")
- Each card: pig/batch name, current stage → next stage, days/weight over threshold
- Action buttons per card: Promote, Snooze, View Details
- Batch cards expand to show a checklist of included pigs with exclude toggles

**States**:
- Empty ("No promotions pending")
- Loaded: sorted by priority/oldest-pending-first

**Actions**: Promote (individual) → confirm dialog → Stage History updated; Promote (batch) → batch promotion flow with exclusions; Snooze → re-surface in N days

---

## 6. Batch View / Promote Batch

**Purpose**: Manage a batch as a unit and execute batch-level promotions.

**Elements**:
- Batch header: name, stage, pig count, average age/weight
- Pig checklist (all pigs in batch, checkbox to include/exclude from promotion)
- "Promote Batch" button (disabled until at least 1 pig selected)
- Batch stats: average ADG, average FCR

**States**:
- Loading
- Confirmation state after promotion (success toast + updated stage badge)

**Actions**: Toggle pig inclusion; Promote Batch → confirmation dialog listing what will change (stage, pen, feed plan) → confirm

---

## 7. Pen Management

**Purpose**: Track pen occupancy and movement history.

**Elements**:
- List of pens: name, current occupancy / capacity (progress bar), overcrowding warning if over capacity
- Tap pen → occupant list + movement history log (who moved in/out, when, why)

**States**:
- Overcrowded pen: red warning badge
- Empty pen: "No pigs currently housed here"

**Actions**: Tap pen → Pen Detail; Add Pen (Settings-adjacent action)

---

## 8. Growth Curve Screen

**Purpose**: Visualize actual growth vs target — spot slow growers early.

**Elements**:
- Line chart: actual weight (solid line) vs target weight curve (dashed line) for selected pig or batch average
- Color-coded status badge: Green (on track), Yellow (slightly behind), Red (significantly behind, >15%)
- Pig/batch selector dropdown

**States**:
- No weight data yet: "Log at least 2 weights to see growth trend"

**Actions**: Switch between pig/batch view; tap data point → see exact date/weight

---

## 9. Alerts Screen

**Purpose**: Central inbox for all system-generated alerts.

**Elements**:
- Grouped by priority: Critical, High, Medium, Low (collapsible sections)
- Each alert card: icon (per alert type), message, related pig/batch link, timestamp
- Swipe actions: Snooze, Mark Done
- Filter: Active / Snoozed / Done (history)

**States**:
- Empty ("All caught up — no active alerts")
- Critical alert present: red banner at top of app until addressed (persistent, not just in this screen)

**Actions**: Tap alert → related Pig Profile or Batch View; swipe → Snooze/Done; tap "Done" → moves to history

---

## 10. Health Module Screens

### 10a. Health Overview
- Elements: Upcoming vaccinations/deworming list, active quarantine list, active withdrawal list
- Actions: Tap item → Health Event detail or Pig Profile Health tab

### 10b. Log Health Event
- Elements: Pig/batch selector, event type (vaccination/treatment/deworming), product, dosage, route, withdrawal days (auto-suggested per product if templated), vet name, notes
- Actions: Save → creates health_event, auto-schedules withdrawal flag if applicable

### 10c. Symptom Log / Outbreak Alert Detail
- Elements: List of reported symptoms by pen, outbreak alert banner if threshold triggered
- Actions: Log new symptom; acknowledge outbreak alert

---

## 11. Breeding Module Screens

### 11a. Sow Card
- Elements: Reproductive history table (litters, born alive/stillborn, weaning rate), performance ranking badge
- Actions: Log heat check, Log mating/insemination, View farrowing records

### 11b. Farrowing Entry
- Elements: Form — date, total born, born alive, stillborn, mummified, average birth weight
- Actions: Save → optionally auto-generate piglet records

### 11c. Sow Performance Dashboard
- Elements: Ranked list of sows by productivity score, flag icons for low performers
- Actions: Tap sow → Sow Card

---

## 12. Feed Module Screens

### 12a. Feed Inventory
- Elements: Ingredient list with stock level bars, reorder-level warnings, "Add Purchase" button
- Actions: Log purchase → updates stock

### 12b. Feeding Log Entry
- Elements: Pen/batch selector, feed type, quantity, date
- Actions: Save → decrements inventory, feeds FCR calc

### 12c. Feed Formulation Calculator
- Elements: Ingredient selector with available quantities, target stage selector, calculated ration output (proportions + cost/kg)
- Actions: Save as formula, link to a growth stage

---

## 13. Market Module Screens

### 13a. Market Readiness Dashboard
- Elements: List of market-ready pigs, estimated value (weight × price/kg input field), withdrawal-block indicator
- Actions: Tap pig → initiate sale

### 13b. Buyer Directory
- Elements: Buyer list (name, type, phone), transaction history per buyer
- Actions: Add buyer, call/message shortcut, view past sales

### 13c. Record Sale
- Elements: Buyer selector, pig multi-select (blocked pigs greyed out), total weight (auto-sum), price/kg, payment status
- Actions: Save → marks pigs Sold, logs sale, updates revenue analytics

---

## 14. Reports Screens

### 14a. Reports Hub
- Elements: List of available reports (P&L, herd inventory, sow performance, feed consumption, sales, promotion summary, stage duration, growth performance, pen utilization)
- Actions: Tap report → generated view; Export (PDF)

### 14b. P&L / KPI Dashboard
- Elements: Revenue, costs breakdown (feed/health/labor), net profit, KPI tiles (FCR, ADG, mortality rate, weaning rate), anomaly flags
- Actions: Change reporting period; export

---

## 15. Stage Settings

**Purpose**: Configure growth stage thresholds, linked feed formulas, and health protocols per farm/breed.

**Elements**:
- Editable table of growth stages (age/weight ranges)
- Link feed formula and health protocol dropdowns per stage
- "Restore Defaults" button

**Actions**: Edit thresholds, save, restore defaults

---

## 16. Settings

**Elements**:
- Target weight/age defaults, alert timing preferences, notification toggles per alert type
- Backup/restore (export/import DB file)
- User/farm profile info

**Actions**: Export data, import data, reset alert preferences

---

## Cross-Cutting UI Requirements (apply to ALL screens)

- Minimum tap target: 48dp (outdoor/gloved-hand use)
- High-contrast color scheme, readable in direct sunlight
- Every list screen needs empty, loading, and error states designed — not just the happy path
- Every destructive action (delete, mark dead, cancel sale) needs a confirmation dialog
- Offline indicator is NOT needed as a persistent banner (app is offline-first by design) — but sync/backup status should be visible in Settings only

*End of screens document. Cross-reference with PHMS_03_Features_Checklist.md to confirm no feature lacks a corresponding screen.*

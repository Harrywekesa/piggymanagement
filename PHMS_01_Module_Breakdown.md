# PHMS — Module-by-Module Breakdown

**Project**: Pig House Management System (PHMS)
**Platform**: Android (Kotlin, offline-first, Room database)
**Purpose of this document**: Full technical breakdown of every module — what it does, why it exists, its data model, its business rules, and what it depends on. Use this as the source of truth when building or reviewing any module.

---

## Build Order & Dependency Map

Modules are NOT independent. Build in this order or later modules will have nothing to reference:

1. **Core Schema + Module 1 (Pig Records)** — foundation, everything references pigs
2. **Module 8 (Growth Stages)** — stage definitions needed before feed/health can auto-link
3. **Module 4 (Feed & Nutrition)** — needs stage definitions
4. **Module 3 (Health & Veterinary)** — needs stage definitions for auto-scheduled protocols
5. **Module 2 (Breeding & Reproduction)** — mostly independent, slot in any time after Module 1
6. **Module 5 (Alerts Engine)** — needs data from modules 1-4 and 8 to have anything to alert on
7. **Module 6 (Trade & Market)** — needs pig/stage data
8. **Module 7 (Financial Management)** — needs cost data from feed, health, sales — build last

---

## Module 1: Animal Records & Traceability

**Purpose**: Single source of truth for every pig on the farm — identity, location, history, performance.

**Why it matters**: Without reliable individual/batch records, every other module (health, feed, breeding, financials) has nothing to attach to. This is the foundation module.

### Features
- Register pig with unique ID (auto-generated or ear tag), breed, sex, DOB, source (born/purchased), pen location, photo
- Batch grouping (nursery, grower, finisher) with inventory view per batch
- QR/barcode scan (ZXing) to open pig profile instantly
- Weekly weight logging with auto-calculated ADG (Average Daily Gain) and FCR (Feed Conversion Ratio)
- Mortality/culling records with cause of death
- Search and filter by ID, batch, pen, status

### Database Schema
```
pigs
  id INTEGER PK
  tag_number TEXT UNIQUE
  breed TEXT
  sex TEXT (M/F)
  birth_date DATE
  source TEXT (Born/Purchased)
  pen_id INTEGER FK -> pens
  batch_id INTEGER FK -> batches
  status TEXT (Active/Sold/Dead/Culled)
  photo_path TEXT

weight_records
  id INTEGER PK
  pig_id INTEGER FK -> pigs
  date DATE
  weight_kg REAL

pens
  id INTEGER PK
  name TEXT
  capacity INTEGER
  current_occupancy INTEGER (derived/cached)

batches
  id INTEGER PK
  name TEXT
  stage_id INTEGER FK -> growth_stages
  created_date DATE
```

### Business Rules
- `tag_number` must be unique across active pigs; reused tags allowed only after a pig is marked Dead/Sold and archived
- ADG = (latest_weight - previous_weight) / days_between_readings
- FCR = total_feed_consumed_kg / total_weight_gain_kg (pulls from Module 4 feeding logs)
- A pig cannot be deleted, only status-changed to Dead/Sold/Culled — preserve history

### Depends on
Nothing (foundation module)

### Feeds into
All other modules reference `pigs.id`

---

## Module 2: Breeding & Reproduction

**Purpose**: Maximize reproductive efficiency — the single biggest driver of pig farm profitability.

### Features
- Sow card: full reproductive history (litters, piglets born alive/stillborn, weaning rate)
- Heat detection log (heat checks, boar exposure, mating/insemination dates)
- Pregnancy tracking with auto-calculated expected farrowing date (insemination + 114 days)
- Farrowing records (date, litter size, born alive, stillborn, mummified, piglet weights)
- Weaning records (date, number weaned, average weaning weight)
- Sow performance dashboard — rank by litters/year, piglets weaned/litter, flag low performers
- Boar management — usage tracking, fertility rates, rest periods

### Database Schema
```
breeding_events
  id INTEGER PK
  sow_id INTEGER FK -> pigs
  boar_id INTEGER FK -> pigs
  date DATE
  type TEXT (heat/insemination/mating)
  notes TEXT

pregnancies
  id INTEGER PK
  sow_id INTEGER FK -> pigs
  insemination_date DATE
  expected_farrowing_date DATE (= insemination_date + 114 days)
  confirmed_date DATE NULLABLE

farrowing_records
  id INTEGER PK
  sow_id INTEGER FK -> pigs
  farrowing_date DATE
  total_born INTEGER
  born_alive INTEGER
  stillborn INTEGER
  mummified INTEGER
  avg_birth_weight REAL

weaning_records
  id INTEGER PK
  sow_id INTEGER FK -> pigs
  weaning_date DATE
  piglets_weaned INTEGER
  avg_weaning_weight REAL
```

### Business Rules
- Expected farrowing date is auto-calculated and read-only once set; confirmed via ultrasound updates `confirmed_date` only
- Weaning date should default to farrowing_date + 28 days but is editable
- Sow performance score = weighted formula of (litters/year × piglets weaned/litter) — flag sows below farm-configurable threshold for culling review
- New piglets from a farrowing record should optionally auto-generate `pigs` records tagged with source = "Born"

### Depends on
Module 1 (pigs table)

### Feeds into
Module 8 (weaning triggers stage assignment for new piglets), Module 5 (heat check / farrowing alerts)

---

## Module 3: Health & Veterinary Management

**Purpose**: Prevent disease outbreaks, ensure timely treatment, protect food safety via withdrawal tracking.

### Features
- Vaccination scheduler with protocol templates (FMD, CSF, Parvovirus, etc.) and auto-reminders
- Treatment log (date, drug, dosage, route, withdrawal period, vet name)
- Deworming schedule with automated reminders
- Disease outbreak detection: flag if >2 pigs in same pen show similar symptoms within 48 hours
- Quarantine management for newly purchased pigs
- Vet visit log (visits, diagnoses, prescriptions)
- Withdrawal period tracking — flags pigs currently under drug withdrawal (food safety critical)

### Database Schema
```
health_events
  id INTEGER PK
  pig_id INTEGER FK -> pigs
  date DATE
  type TEXT (vaccination/treatment/deworming)
  product TEXT
  dosage TEXT
  route TEXT
  withdrawal_days INTEGER
  vet_name TEXT
  notes TEXT

symptoms
  id INTEGER PK
  pig_id INTEGER FK -> pigs
  date DATE
  symptom TEXT
  severity TEXT (Mild/Moderate/Severe)

quarantine
  id INTEGER PK
  pig_id INTEGER FK -> pigs
  start_date DATE
  end_date DATE
  status TEXT (Active/Cleared)
```

### Business Rules
- Withdrawal end date = health_event.date + withdrawal_days; pig is auto-flagged "Not Ready for Sale" until this date passes
- Outbreak detection: query `symptoms` grouped by `pen_id` (joined via pigs) — if count >= 2 with matching or similar symptom text within a rolling 48-hour window, generate Critical alert
- Quarantine default duration configurable in Settings (typically 21-30 days); pig cannot be mixed into general population while `status = Active`
- Each growth stage (Module 8) can link a default health protocol — when a pig is promoted into that stage, matching health_events are auto-scheduled

### Depends on
Module 1 (pigs table), Module 8 (stage-linked protocols)

### Feeds into
Module 5 (vaccination due, outbreak, withdrawal alerts), Module 6 (withdrawal status blocks market readiness)

---

## Module 4: Feed & Nutrition Management

**Purpose**: Control the single largest cost driver in pig production (up to 70% of total cost).

### Features
- Feed formulation calculator: input available ingredients, get least-cost ration meeting nutritional requirements per growth stage
- Feed inventory tracking with low-stock alerts
- Daily feeding log per pen/batch, auto-computes FCR per batch
- Feed cost tracking (cost per kg, total cost per pig)
- Feeding plan generator — auto weekly plans based on pig count/weight per pen

### Database Schema
```
feed_ingredients
  id INTEGER PK
  name TEXT
  stock_kg REAL
  cost_per_kg REAL
  reorder_level REAL

feed_formulas
  id INTEGER PK
  name TEXT
  stage TEXT (or stage_id FK -> growth_stages)
  ingredients_json TEXT (ingredient_id: proportion pairs)
  cost_per_kg REAL (derived)

feeding_logs
  id INTEGER PK
  pen_id INTEGER FK -> pens
  batch_id INTEGER FK -> batches
  date DATE
  feed_type TEXT
  quantity_kg REAL

feed_purchases
  id INTEGER PK
  ingredient_id INTEGER FK -> feed_ingredients
  date DATE
  quantity_kg REAL
  total_cost REAL
```

### Business Rules
- Formulation calculator solves for least-cost mix meeting minimum protein/energy targets per stage (can start rule-based with fixed reference ratios before building a true linear-optimization solver)
- Feed purchase automatically increments `feed_ingredients.stock_kg`; feeding logs decrement it
- Low-stock alert triggers when `stock_kg <= reorder_level`
- `feed_formulas.cost_per_kg` recalculates whenever linked ingredient costs change

### Depends on
Module 8 (stage-linked feed formulas)

### Feeds into
Module 5 (low-stock alerts), Module 7 (feed cost feeds P&L), Module 1 (FCR calculation)

---

## Module 5: Alerts & Notifications Engine

**Purpose**: Central nervous system — surfaces time-sensitive actions the farmer would otherwise miss.

### Alert Catalog
| Alert Type | Trigger | Priority |
|---|---|---|
| Vaccination due | 3 days before scheduled date | High |
| Heat check due | Based on sow reproductive cycle | Medium |
| Farrowing expected | 114 days after insemination | High |
| Low feed stock | Below reorder level | High |
| Mortality spike | 2 deaths in 24hrs, same pen | Critical |
| Drug withdrawal active | Pig under withdrawal period | High |
| Market readiness | Pig reaches target weight | Medium |
| Weaning due | 28 days after farrowing | Medium |
| Promotion ready (individual) | Pig meets stage threshold | Medium |
| Promotion ready (batch) | Batch average meets threshold | Medium |
| Slow growth warning | Pig >15% below target weight | High |
| Pen overcrowding | Pen exceeds capacity | High |
| Stage overdue | Pig >20% beyond max stage age | Medium |

### Implementation
- WorkManager runs a daily check (default 6 AM, configurable)
- Query relevant tables per alert type, generate `NotificationCompat` notifications
- Alerts are actionable inline: Snooze or Mark Done
- All generated alerts persist to an `alerts` table (not just fired-and-forgotten) so there's a historical log

### Database Schema
```
alerts
  id INTEGER PK
  type TEXT
  priority TEXT (Low/Medium/High/Critical)
  related_pig_id INTEGER NULLABLE FK -> pigs
  related_batch_id INTEGER NULLABLE FK -> batches
  message TEXT
  created_date DATE
  status TEXT (Active/Snoozed/Done)
  snoozed_until DATE NULLABLE
```

### Depends on
Modules 1, 2, 3, 4, 8 (needs their data to evaluate triggers against)

### Feeds into
Nothing further — this is a terminal/UI-facing module, but it's the most demo-critical for a live show (trigger a mortality spike or promotion alert on stage)

---

## Module 6: Trade & Market Linkages

**Purpose**: Connect farmers to buyers, track sales, estimate value of market-ready stock.

### Features
- Market readiness dashboard — pigs at target weight, estimated value (weight × user-entered price/kg)
- Buyer directory (wholesalers, retailers, butcheries, slaughterhouses) with transaction notes
- Sales record (buyer, pig count, weight, price, payment status)
- Revenue analytics (monthly/annual revenue, profit margin per batch, cost-per-pig)

### Database Schema
```
buyers
  id INTEGER PK
  name TEXT
  phone TEXT
  type TEXT (Wholesaler/Retailer/Butchery/Slaughterhouse)
  notes TEXT

sales
  id INTEGER PK
  buyer_id INTEGER FK -> buyers
  date DATE
  pig_ids_json TEXT
  total_weight REAL
  price_per_kg REAL
  total_amount REAL (derived)
  payment_status TEXT (Paid/Partial/Pending)
```

### Business Rules
- A pig flagged "Not Ready for Sale" (active withdrawal, Module 3) cannot be added to a `sales` record — hard block, not just a warning
- Completing a sale auto-updates `pigs.status` to "Sold" for all pigs in `pig_ids_json`
- Revenue analytics pull sales + Module 7 cost data to compute margin

### Depends on
Module 1 (pig status), Module 3 (withdrawal block), Module 8 (market-ready flag)

### Feeds into
Module 7 (sales revenue feeds P&L)

---

## Module 7: Financial Management & Reports

**Purpose**: Turn all the operational data into profitability insight — the module that answers "is this farm making money?"

### Features
- Auto-generated P&L (feed costs + medication costs + labor − sales revenue)
- Budget & forecast — project feed costs and revenue from current herd + growth curves
- Cost-per-pig tracking (birth to market)
- KPI dashboard (FCR, ADG, mortality rate, weaning rate, litters/sow/year)
- Anomaly detection — flag deviations from standard performance (e.g., FCR spike)

### Reports
- Monthly P&L
- Herd inventory summary
- Sow performance ranking
- Feed consumption report
- Sales report
- Promotion summary report
- Stage duration analysis
- Growth performance report
- Pen utilization report

### Business Rules
- P&L period is farm-configurable (default monthly)
- Cost-per-pig = sum of all `feed_purchases` + `health_events` costs allocated to that pig ÷ pigs sharing that cost pool, plus a labor estimate input
- Anomaly detection: compare current period KPI to trailing 3-period average; flag if deviation exceeds a configurable threshold (default 20%)

### Depends on
Modules 1, 2, 3, 4, 6 (aggregates data from all of them — build LAST)

### Feeds into
Nothing — terminal reporting module

---

## Module 8: Growth Stage & Promotion Management

**Purpose**: Automate the single most operationally complex part of pig farming — moving pigs through life stages with correct feed, health protocols, and housing at each step.

### Growth Stages (default, editable per farm/breed)
| Stage | Age Range | Weight Range | Duration |
|---|---|---|---|
| Piglet | 0–4 weeks | 1.5–7 kg | 28 days |
| Weaner | 4–10 weeks | 7–25 kg | 42 days |
| Grower | 10–16 weeks | 25–60 kg | 42 days |
| Finisher | 16–24 weeks | 60–90 kg | 56 days |
| Market Ready | 24+ weeks | 90–110 kg | Until sale |
| Sow | 8+ months | 120+ kg | Breeding life |
| Boar | 8+ months | 130+ kg | Breeding life |
| Gilt | 6–8 months | 90–120 kg | Until first mating |

### Features
1. **Automatic stage assignment** — on registration and daily re-check, based on age and/or weight (configurable which takes precedence)
2. **Promotion readiness alerts** — daily WorkManager check flags individuals and batches meeting next-stage thresholds
3. **Individual promotion** — pig profile → Promote → select stage → confirm; logs date, old/new stage, weight, pen change
4. **Batch promotion** — promote whole batch at once, with per-pig exclusion option for slow growers
5. **Pen/house movement tracking** — full occupancy history, not just current state
6. **Feed plan auto-update** — promotion ends old feeding plan, starts new stage's plan, adjusts inventory deduction rate
7. **Growth curve monitoring** — plot actual vs target weight, flag pigs >15% below target
8. **Stage-specific health protocols** — auto-schedule next stage's required health events on promotion
9. **Promotion history log** — permanent record: pig/batch, old→new stage, date, weight, age, user, notes
10. **Market readiness flag** — auto-appear on Module 6 dashboard when stage = Market Ready, blocked if withdrawal active
11. **Reversal/undo** — undo a mistaken promotion within 24 hours, restores previous stage/pen/feed plan

### Database Schema
```
growth_stages
  id INTEGER PK
  name TEXT
  min_age_weeks INTEGER
  max_age_weeks INTEGER
  min_weight_kg REAL
  max_weight_kg REAL
  feed_formula_id INTEGER FK -> feed_formulas
  health_protocol_id INTEGER FK -> health_protocols (or linked health_events template)
  display_order INTEGER

pig_stage_history
  id INTEGER PK
  pig_id INTEGER FK -> pigs
  batch_id INTEGER NULLABLE FK -> batches
  old_stage_id INTEGER FK -> growth_stages
  new_stage_id INTEGER FK -> growth_stages
  promotion_date DATE
  weight_at_promotion REAL
  age_at_promotion_weeks INTEGER
  old_pen_id INTEGER FK -> pens
  new_pen_id INTEGER FK -> pens
  promoted_by TEXT
  notes TEXT
  is_reversed BOOLEAN
  reversal_date DATE NULLABLE

pen_occupancy_history
  id INTEGER PK
  pen_id INTEGER FK -> pens
  pig_id INTEGER NULLABLE FK -> pigs
  batch_id INTEGER NULLABLE FK -> batches
  start_date DATE
  end_date DATE NULLABLE
  reason TEXT (promotion/space/health isolation)

growth_targets
  id INTEGER PK
  stage_id INTEGER FK -> growth_stages
  week_number INTEGER
  target_weight_kg REAL
  target_feed_kg_per_day REAL
```

### Promotion Logic (Daily WorkManager Job, 6 AM)
```
FOR EACH active pig:
    current_stage = pig.current_stage
    next_stage = get_next_stage(current_stage)
    IF next_stage is null: SKIP

    age_ready = pig.age_weeks >= next_stage.min_age_weeks
    weight_ready = pig.latest_weight >= next_stage.min_weight_kg

    IF age_ready OR weight_ready:
        IF pig not already flagged:
            CREATE promotion_alert(pig, next_stage)

FOR EACH batch:
    avg_age = average(batch.pigs.age_weeks)
    avg_weight = average(batch.pigs.latest_weight)
    IF avg_age >= next_stage.min_age OR avg_weight >= next_stage.min_weight:
        CREATE batch_promotion_alert(batch, next_stage)
```

### Depends on
Module 1 (pigs table)

### Feeds into
Modules 2, 3, 4, 5, 6, 7 — this is the most heavily depended-upon module after Module 1. Build it second.

---

*End of module breakdown. See PHMS_02_Screens_Document.md and PHMS_03_Features_Checklist.md for UI and QA detail.*

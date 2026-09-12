# PHMS — Features Checklist

**Purpose of this document**: Atomic, checkable feature list to track build progress and confirm nothing from the spec gets dropped during implementation. Use as a literal checklist — mark items done as you build/verify them.

---

## Module 1: Animal Records & Traceability
- [ ] Register new pig with auto-generated or manual tag number
- [ ] Enforce tag number uniqueness across active pigs
- [ ] Capture breed, sex, DOB, source, pen, batch, photo on registration
- [ ] Edit existing pig record
- [ ] Batch grouping (create batch, assign pigs, view batch inventory)
- [ ] QR/barcode generation for new pig tags
- [ ] QR/barcode scan → open Pig Profile
- [ ] Weekly weight log entry
- [ ] Auto-calculate ADG from weight history
- [ ] Auto-calculate FCR (requires feed log data — Module 4 dependency)
- [ ] Mortality record (cause of death, date)
- [ ] Culling record (reason, date)
- [ ] Status transitions: Active → Sold/Dead/Culled (no hard delete)
- [ ] Search by tag number
- [ ] Filter by batch, pen, status, stage
- [ ] Pig cannot be deleted once created (data integrity rule)

## Module 2: Breeding & Reproduction
- [ ] Sow card view (full reproductive history)
- [ ] Log heat check event
- [ ] Log boar exposure / mating event
- [ ] Log artificial insemination event
- [ ] Auto-calculate expected farrowing date (insemination + 114 days)
- [ ] Confirm pregnancy (ultrasound date entry)
- [ ] Farrowing record entry (total born, alive, stillborn, mummified, avg birth weight)
- [ ] Weaning record entry (date, count weaned, avg weaning weight)
- [ ] Auto-generate piglet pig-records from farrowing record (optional toggle)
- [ ] Sow performance scoring (litters/year, piglets weaned/litter)
- [ ] Sow performance ranked dashboard
- [ ] Flag low-performing sows for culling review
- [ ] Boar usage tracking
- [ ] Boar fertility rate tracking
- [ ] Boar rest period tracking/warning

## Module 3: Health & Veterinary Management
- [ ] Vaccination protocol templates (FMD, CSF, Parvovirus, custom)
- [ ] Vaccination scheduling with auto-reminder
- [ ] Treatment log entry (drug, dosage, route, vet name)
- [ ] Deworming schedule with auto-reminder
- [ ] Withdrawal period auto-calculation from treatment date + withdrawal days
- [ ] Withdrawal-active flag visible on Pig Profile
- [ ] Withdrawal-active block on sale eligibility (hard block, Module 6 dependency)
- [ ] Symptom logging per pig
- [ ] Outbreak detection: >2 pigs, same pen, similar symptoms, 48hr window
- [ ] Outbreak alert generation (Critical priority)
- [ ] Quarantine record creation (start/end date)
- [ ] Quarantine status blocks mixing into general population
- [ ] Vet visit log (diagnosis, prescription, date)
- [ ] Stage-linked default health protocols (Module 8 dependency)
- [ ] Auto-schedule next-stage health events on promotion

## Module 4: Feed & Nutrition Management
- [ ] Feed ingredient inventory (add, edit, view stock)
- [ ] Feed purchase log (increments stock)
- [ ] Low-stock alert trigger (stock <= reorder level)
- [ ] Daily feeding log entry (per pen/batch)
- [ ] Feeding log decrements ingredient stock
- [ ] Feed formulation calculator (input ingredients → output ration)
- [ ] Least-cost ration logic (start rule-based, upgrade to solver later)
- [ ] Save formula and link to a growth stage
- [ ] Auto-recalculate formula cost when ingredient prices change
- [ ] Feed cost per kg tracking
- [ ] Feed cost per pig tracking (feeds Module 7)
- [ ] Auto-generate weekly feeding plan per pen based on pig count/weight
- [ ] FCR calculation per batch (requires weight data — Module 1 dependency)

## Module 5: Alerts & Notifications Engine
- [ ] Daily WorkManager job (configurable time, default 6 AM)
- [ ] Vaccination-due alert (3 days before)
- [ ] Heat-check-due alert
- [ ] Farrowing-expected alert (114 days post-insemination)
- [ ] Low-feed-stock alert
- [ ] Mortality-spike alert (2 deaths/24hrs same pen, Critical)
- [ ] Drug-withdrawal-active alert
- [ ] Market-readiness alert
- [ ] Weaning-due alert (28 days post-farrowing)
- [ ] Promotion-ready alert (individual)
- [ ] Promotion-ready alert (batch)
- [ ] Slow-growth-warning alert (>15% below target)
- [ ] Pen-overcrowding alert
- [ ] Stage-overdue alert (>20% beyond max stage age)
- [ ] Alert persistence to database (not fire-and-forget)
- [ ] Snooze action (re-surface after N days)
- [ ] Mark-done action (moves to history)
- [ ] Alert history view (past Snoozed/Done items)
- [ ] Critical alert persistent banner across app until addressed

## Module 6: Trade & Market Linkages
- [ ] Market readiness dashboard (pigs at target weight)
- [ ] Estimated value calculation (weight × user-input price/kg)
- [ ] Withdrawal-block indicator on market dashboard
- [ ] Buyer directory (add/edit buyer: name, phone, type, notes)
- [ ] Buyer transaction history view
- [ ] Sale record entry (buyer, pigs, weight, price, payment status)
- [ ] Hard block: cannot add withdrawal-active pig to a sale
- [ ] Sale completion auto-updates pig status to Sold
- [ ] Revenue analytics: monthly/annual revenue
- [ ] Profit margin per batch calculation
- [ ] Cost-per-pig analysis (Module 7 dependency)

## Module 7: Financial Management & Reports
- [ ] Auto-generated P&L (feed + medication + labor − revenue)
- [ ] Configurable P&L reporting period (default monthly)
- [ ] Budget/forecast projection from current herd + growth curves
- [ ] Cost-per-pig tracking (birth to market, aggregated)
- [ ] KPI dashboard: FCR, ADG, mortality rate, weaning rate, litters/sow/year
- [ ] Anomaly detection (deviation from trailing 3-period average, configurable threshold)
- [ ] Report: Monthly P&L
- [ ] Report: Herd inventory summary
- [ ] Report: Sow performance ranking
- [ ] Report: Feed consumption
- [ ] Report: Sales
- [ ] Report: Promotion summary
- [ ] Report: Stage duration analysis
- [ ] Report: Growth performance
- [ ] Report: Pen utilization
- [ ] PDF export for any report

## Module 8: Growth Stage & Promotion Management
- [ ] Default growth stage definitions pre-populated (Piglet through Gilt)
- [ ] Growth stages editable per farm/breed in Settings
- [ ] Automatic stage assignment on pig registration
- [ ] Daily automatic stage re-check (age and/or weight based, configurable precedence)
- [ ] Promotion-readiness detection (individual)
- [ ] Promotion-readiness detection (batch average)
- [ ] Individual promotion flow (select stage, confirm, log event)
- [ ] Batch promotion flow with per-pig exclusion
- [ ] Pen/house movement logging on promotion
- [ ] Pen occupancy history (full audit trail, not just current state)
- [ ] Feed plan auto-switch on promotion (ends old, starts new)
- [ ] Feed inventory deduction rate adjusts to new stage
- [ ] Growth curve chart: actual vs target weight
- [ ] Slow-grower flag (>15% below target)
- [ ] Stage-linked health protocol auto-scheduling on promotion
- [ ] Promotion history log (permanent, per pig and per batch)
- [ ] Market-ready auto-flag when stage = Market Ready
- [ ] Market-ready flag respects active withdrawal block
- [ ] Promotion reversal/undo (within 24 hours)
- [ ] Reversal restores previous stage, pen, and feed plan

## Cross-Cutting / Non-Functional
- [ ] Fully offline — zero features require internet connectivity
- [ ] Room database with proper foreign key constraints and indices
- [ ] Data export/import (backup to file — Drive or SD card)
- [ ] No hard deletes anywhere — status/soft-delete pattern throughout
- [ ] All monetary and weight fields support the farm's configured units/currency
- [ ] Confirmation dialogs on all destructive/irreversible actions
- [ ] Empty, loading, and error states designed for every list/detail screen
- [ ] Minimum 48dp tap targets throughout (outdoor/gloved use)
- [ ] Demo data seeding script (30-40 fake pigs across all stages, sample alerts, sample sales) for show demo readiness

*Cross-reference with PHMS_01_Module_Breakdown.md for schema/logic detail and PHMS_02_Screens_Document.md for where each feature surfaces in the UI.*

# PHMS — AI Agent Build Prompt

Paste this into Claude Code, Cursor, or any AI coding agent. Attach the three companion documents (Module Breakdown, Screens Document, Features Checklist) as context/reference files in the project — the agent should treat them as the spec of record.

---

```
You are building "PHMS" (Pig House Management System) — a complete, offline-first 
native Android application for pig farm management, targeting smallholder and 
medium-scale pig farmers in rural Kenya. This is a real project being built for 
demonstration at the ASK Kitale National Show 2026.

REFERENCE DOCUMENTS (attached — read these fully before writing any code):
1. PHMS_01_Module_Breakdown.md — full data model, business rules, and dependency 
   order for all 8 modules. This is the authoritative spec for database schema 
   and logic.
2. PHMS_02_Screens_Document.md — every screen's purpose, UI elements, states, 
   and actions. This is the authoritative spec for UI.
3. PHMS_03_Features_Checklist.md — atomic feature checklist across all modules. 
   Use this to self-verify nothing is missed as you build.

TECH STACK (non-negotiable)
- Language: Kotlin
- Min SDK 21, Target SDK 34
- Local database: Room (SQLite abstraction) — zero backend, zero cloud dependency 
  for core functionality
- Background jobs: WorkManager (for the daily alert/promotion check)
- Notifications: NotificationManager + NotificationCompat
- Charts: MPAndroidChart (weight history, growth curves)
- QR/Barcode: ZXing
- PDF export: iText or native Android PdfDocument
- Architecture: MVVM with Repository pattern — ViewModels per screen, Repositories 
  per module, Room DAOs per table
- UI: Jetpack Compose preferred (Material Design 3) unless you determine XML 
  layouts are meaningfully faster to ship correctly — justify the choice if you 
  deviate

BUILD ORDER (strict — do not build modules out of this sequence, later modules 
depend on earlier ones for schema and data)
1. Core Room database setup + Module 1 (Animal Records) — pigs, pens, batches, 
   weight_records tables and full CRUD
2. Module 8 (Growth Stage & Promotion) — growth_stages, pig_stage_history, 
   pen_occupancy_history, growth_targets tables + auto-assignment logic
3. Module 4 (Feed & Nutrition) — depends on growth_stages existing
4. Module 3 (Health & Veterinary) — depends on growth_stages for protocol linking
5. Module 2 (Breeding & Reproduction) — mostly independent, build here
6. Module 5 (Alerts Engine) — depends on modules 1-4 and 8 having real data 
   to evaluate against
7. Module 6 (Trade & Market) — depends on pig/stage/withdrawal data
8. Module 7 (Financial Management & Reports) — aggregates everything, build LAST

WORKING METHOD
- After each module, run through the corresponding section of 
  PHMS_03_Features_Checklist.md and confirm every item is implemented before 
  moving to the next module. Report which checklist items are done and which 
  are deferred, with a reason if deferred.
- Write Room entities, DAOs, and migrations FIRST for each module before touching 
  UI — schema mistakes compound if UI is built against wrong tables.
- Follow the exact business rules in PHMS_01_Module_Breakdown.md — e.g., 
  withdrawal-active pigs must be HARD BLOCKED from sales (not just warned), 
  no hard deletes anywhere (status/soft-delete pattern), tag numbers must be 
  unique among active pigs.
- Build the Alerts Engine (Module 5) with real, testable trigger conditions — 
  this is the module most likely to be demoed live on stage, so it needs to 
  work reliably with seeded data, not just in theory.

DEMO READINESS (do this as a final step, not an afterthought)
- Write a data-seeding script/function that populates the database with 
  30-40 realistic fake pigs distributed across all growth stages, some with 
  active health events, at least 2-3 active alerts of different priorities, 
  and a few completed sales — so the app looks alive on first launch for a 
  live demo, not empty.
- Include one guaranteed-to-fire alert scenario (e.g., a batch at exact 
  promotion threshold) that can be triggered live in front of judges.

CONSTRAINTS
- No internet dependency anywhere in the core app. Backup/export to file is 
  fine (local file system or user-triggered Drive export), but nothing should 
  break if the device has zero connectivity.
- Do not introduce a backend/API — this explicitly stays on-device per the spec.
- Keep the color palette and UI tone aligned with PHMS_02_Screens_Document.md's 
  cross-cutting requirements: high contrast, large tap targets (48dp minimum), 
  usable in bright outdoor light.

DELIVERABLE FORMAT
Work module by module. After completing each module, give me:
1. A short summary of what was built (entities, key logic, screens)
2. Any deviations from the spec and why
3. The updated status of that module's checklist items
Then wait for confirmation before moving to the next module in the build order 
above, unless I tell you to proceed through multiple modules autonomously.
```

---

## Notes on using this prompt

- **If your agent supports file attachments** (Claude Code, Cursor with folder context), drop all three companion `.md` files into the project root so the agent can reference them directly rather than you pasting content inline.
- **If your tool has a context window limit**, you may need to paste the Module Breakdown document's relevant section again when starting each new module, since some agents lose earlier context over a long session.
- **Checkpoint after Module 1 and Module 8 especially** — these are the foundation and the most heavily-depended-upon module. A schema mistake here cascades into every later module. Review the generated Room entities yourself before letting the agent continue.
- **Don't let the agent skip the "wait for confirmation" step** even if it offers to build faster — catching a wrong assumption after Module 2 is cheap; catching it after Module 7 means redoing everything downstream.

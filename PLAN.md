# Project Specification & Implementation Plan: Zero-Knowledge Personal Activity Tracker (`plan.md`)

## 1. System Overview & Core Constraints
Build a native Android application (Kotlin, Jetpack Compose, Material 3) for discreetly logging and analyzing personal habit/activity sessions.

### Strict Architectural & Security Rules
1. **Zero Local Persistence:** Nothing may be stored permanently on the device (no Room/SQLite database, no DataStore/SharedPreferences for records or keys, no disk caching of decrypted payloads).
2. **Zero-Knowledge Client-Side Encryption:** All questionnaire data and session timestamps are encrypted on-device in volatile RAM before transmission to Supabase. Supabase stores only opaque ciphertext.
3. **Ephemeral Password Handling:** The user's vault password is NEVER saved to disk, Android Keystore, logs, or analytics. The derived AES-256 key lives strictly in a volatile `ByteArray` in RAM while the app is unlocked and is zeroed out (`Arrays.fill(key, 0)`) immediately when the app goes to background (`ProcessLifecycleOwner` `onStop`) or locks.
4. **Screen Privacy:** Enable `WindowManager.LayoutParams.FLAG_SECURE` on `MainActivity` to prevent screenshots and hide content in the Android Recents switcher.
5. **Discreet Naming & Privacy:** Use neutral, non-judgmental wellness terminology throughout the UI. Never send questionnaire data to third-party analytics or crash-reporting SDKs.

---

## 2. Tech Stack & Project Structure
- **Language / UI:** Kotlin, Jetpack Compose, Material 3 (Light & Dark theme support), Navigation Compose.
- **Backend:** Supabase (Auth + PostgreSQL via `supabase-kt`).
- **Cryptography:** `javax.crypto` (`PBKDF2WithHmacSHA256`, 600,000 iterations, 256-bit key; `AES/GCM/NoPadding`, 12-byte random `SecureRandom` IV per encryption, 128-bit auth tag).
- **Serialization:** `kotlinx.serialization.json`.
- **Charting & Export:** Native Compose Canvas / Vico for charts; `android.graphics.pdf.PdfDocument` for PDF export; `ActivityResultContracts.CreateDocument` for CSV export.

### Directory Structure
```text
app/src/main/java/com/app/activitylog/
├── security/
│   ├── CryptoManager.kt          # PBKDF2 derivation, AES-GCM encrypt/decrypt, RAM wipe
│   └── VaultLifecycleObserver.kt # Auto-lock & memory wipe on background
├── data/
│   ├── model/
│   │   └── HabitRecord.kt        # Full 20 + 5 + 2 timestamp serializable domain model
│   ├── remote/
│   │   └── SupabaseClient.kt     # Supabase initialization & DTOs
│   └── repository/
│       └── SessionRepository.kt  # In-memory StateFlow<List<HabitRecord>>, CRUD + re-encryption
├── analytics/
│   ├── model/
│   │   ├── DateRangeFilter.kt
│   │   └── AnalyticsBundle.kt
│   ├── utils/
│   │   ├── DateHelpers.kt        # Local timezone grouping, 6 time buckets, inter-entry gaps
│   │   ├── Statistics.kt         # Safe denominators, mean, median, ordinal control mapping
│   │   └── Filters.kt            # Equivalent previous-period window calculation
│   └── engine/
│       ├── OverviewAnalyzer.kt
│       ├── FrequencyAnalyzer.kt
│       ├── TimeAnalyzer.kt
│       ├── TriggerAnalyzer.kt
│       ├── EmotionalAnalyzer.kt
│       ├── ControlAnalyzer.kt
│       ├── LifestyleAnalyzer.kt
│       ├── CorrelationAnalyzer.kt
│       ├── InsightGenerator.kt
│       └── ReportGenerator.kt
└── ui/
    ├── vault/                    # Vault setup & unlock screen
    ├── components/               # Reusable HabitFormContent, charts, calendar heatmap, drill-down sheet
    ├── screen1_log/              # Screen 1: New entry logging
    ├── screen2_history/          # Screen 2: Decrypted past entries list & immutable-timestamp editor
    └── screen3_analysis/         # Screen 3: 10-section behavioral analytics dashboard
```

---

## 3. Supabase Database Schema (`PostgreSQL`)
Execute this schema in Supabase. Keep `session_date` and `session_time` inside `encrypted_payload` so zero behavioral metadata is exposed in plaintext on the server.

```sql
-- Stores user salt and encrypted verification canary to validate password on unlock
create table user_vault_meta (
  user_id uuid primary key references auth.users(id) default auth.uid(),
  salt text not null,              -- Base64(16-byte random salt)
  canary_payload text not null,    -- AES-GCM encrypted literal string "VAULT_VERIFIED"
  created_at timestamptz default now()
);

alter table user_vault_meta enable row level security;
create policy "Users manage own vault meta"
  on user_vault_meta for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

-- Stores opaque encrypted session blobs
create table habit_entries (
  id uuid primary key,
  user_id uuid references auth.users(id) not null default auth.uid(),
  encrypted_payload text not null, -- Base64(12-byte IV + Ciphertext + 16-byte GCM Tag)
  created_at timestamptz default now()
);

alter table habit_entries enable row level security;
create policy "Users manage own entries"
  on habit_entries for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
```

---

## 4. Data Schema: Questionnaire & Automatic Fields

### Automatic Fields (Captured at Creation, Immutable on Edit)
| Field | Format / Example | Rules |
| :--- | :--- | :--- |
| `session_date` | `YYYY-MM-DD` (e.g., `2026-11-15`) | Captured automatically in local timezone on Screen 1 submit. **Locked/read-only on Screen 2 edit.** |
| `session_time` | `HH:mm:ss` (e.g., `22:30:00`) | Captured automatically in local timezone on Screen 1 submit. **Locked/read-only on Screen 2 edit.** |

### Part 1: Core 20 Questions
#### A. Session Context (3 questions)
- **Q1 (`location`)** — *Where were you?* | Single choice | Required: **Yes**
  - Options: `Bedroom`, `Bathroom`, `Living room`, `Workplace`, `Outdoors`, `Other`
- **Q2 (`activity_before`)** — *What were you doing beforehand?* | Single choice | Required: **Yes**
  - Options: `Scrolling social media`, `Watching videos`, `Working / Studying`, `Gaming`, `Lying in bed`, `Just woke up`, `Trying to sleep`, `Nothing particular`, `Other`
- **Q3 (`alone`)** — *Were you alone?* | Single choice | Required: **No** (`String?`)
  - Options: `Yes`, `No`

#### B. Triggers and Influences (4 questions)
- **Q4 (`triggers`)** — *What influenced this moment?* | Multiple choice (`List<String>`) | Required: **Yes**
  - Options: `Something online`, `Something I saw`, `Something on my mind`, `Boredom`, `Stress`, `Anxiety`, `Loneliness`, `Physical feelings`, `Daily routine`, `Difficulty sleeping`, `Something unexpected`, `Other`
- **Q5 (`primary_trigger`)** — *What was the main influence?* | Single choice | Required: **Yes**
  - Options: Dynamically constrained to options selected in **Q4** (if Q4 has 1 item, auto-select it; if Q5 is unchecked in Q4, reset Q5).
- **Q6 (`urge_intensity`)** — *How strong was the feeling?* | Numeric rating (`Int`, `1–10`) | Required: **Yes** (`1 = Very weak`, `10 = Extremely strong`)
- **Q7 (`urge_duration`)** — *How long had you been feeling this way?* | Single choice | Required: **No** (`String?`)
  - Options: `Less than 5 minutes`, `5–15 minutes`, `15–30 minutes`, `30–60 minutes`, `More than an hour`

#### C. Emotional State (3 questions)
- **Q8 (`emotions_before`)** — *How were you feeling beforehand?* | Multiple choice (`List<String>`) | Required: **Yes**
  - Options: `Happy`, `Relaxed`, `Bored`, `Lonely`, `Stressed`, `Anxious`, `Sad`, `Angry`, `Frustrated`, `Tired`, `Excited`, `Neutral`, `Other`
- **Q9 (`motivation`)** — *What were you looking for?* | Single choice | Required: **No** (`String?`)
  - Options: `Entertainment`, `Relaxation`, `Emotional comfort`, `Stress relief`, `A distraction`, `Better sleep`, `Personal enjoyment`, `Nothing specific`, `Other`
- **Q10 (`emotions_after`)** — *How did you feel afterward?* | Multiple choice (`List<String>`) | Required: **Yes**
  - Options: `Happy`, `Relaxed`, `Satisfied`, `Sleepy`, `Neutral`, `Guilty`, `Regretful`, `Sad`, `Frustrated`, `Disappointed`, `Emotionally drained`, `Other`

#### D. Control and Media (5 questions)
- **Q11 (`planned`)** — *Was this planned or spontaneous?* | Single choice | Required: **Yes**
  - Options: `Planned`, `Spontaneous`
- **Q12 (`tried_resisting`)** — *Did you try to postpone it?* | Single choice | Required: **Yes**
  - Options: `Yes`, `No`, `Not sure`
- **Q13 (`media_consumed`)** — *Did you consume any media beforehand?* | Single choice | Required: **Yes**
  - Options: `Yes`, `No`, `Not sure`
- **Q14 (`stimulation_type`)** — *What type of content or experience was involved?* | Multiple choice (`List<String>?`) | Required: **No**
  - Options: `Social media`, `Videos`, `Something I read`, `Imagination`, `Memories`, `Physical sensations`, `Other`, `None`
- **Q15 (`unexpected_attention`)** — *Did anything unexpected catch your attention?* | Single choice | Required: **No** (`String?`)
  - Options: `Yes`, `No`, `Not sure`

#### E. Outcome and Reflection (5 questions)
- **Q16 (`experience_satisfaction`)** — *How satisfied were you with the experience?* | Numeric rating (`Int`, `1–10`) | Required: **Yes** (`1 = Not satisfied`, `10 = Extremely satisfied`)
- **Q17 (`decision_satisfaction`)** — *Are you satisfied with your decision?* | Single choice | Required: **Yes**
  - Options: `Yes`, `Somewhat`, `No`, `Unsure`
- **Q18 (`activity_impact`)** — *Did this affect your planned activities?* | Single choice | Required: **Yes**
  - Options: `No`, `Delayed something`, `Interrupted something`, `Missed something`, `Other`
- **Q19 (`perceived_control`)** — *How much control did you feel you had?* | Single choice | Required: **Yes**
  - Options: `Complete control`, `Mostly in control`, `Partially in control`, `Very little control`
- **Q20 (`sleep_duration`)** — *How much sleep did you get last night?* | Single choice | Required: **No** (`String?`)
  - Options: `Less than 4 hours`, `4–6 hours`, `6–8 hours`, `8–10 hours`, `More than 10 hours`, `Not sure`

### Part 4: Conditional Questions (5 Dynamic Branching Fields)
When a condition becomes false during editing or creation, immediately set the corresponding field to `null`.
1. **If `Q4 (triggers)` includes `"Something online"`** $\rightarrow$ `online_platform` (*Which platform or source was involved?*, Single choice): `Instagram`, `TikTok`, `YouTube`, `Reddit`, `Website`, `Other`
2. **If `Q4 (triggers)` includes `"Stress"`** $\rightarrow$ `stress_source` (*What was the main source of stress?*, Single choice): `Work / Studies`, `Relationships`, `Personal matters`, `Financial matters`, `General stress`, `Other`
3. **If `Q11 (planned) == "Spontaneous"`** $\rightarrow$ `spontaneous_reason` (*What made you change your original plan?*, Single choice): `A sudden feeling`, `Something I saw`, `Boredom`, `Stress`, `Habit`, `Other`
4. **If `Q13 (media_consumed) == "Yes"`** $\rightarrow$ `intentional_media` (*Did you intentionally look for that content?*, Single choice): `Yes`, `No`, `Partially`
5. **If `Q18 (activity_impact) != "No"`** $\rightarrow$ `affected_activity` (*Which activity was affected?*, Single choice): `Work`, `Studies`, `Sleep`, `Exercise`, `Social plans`, `Personal responsibilities`, `Other`

---

## 5. Screen Specifications

### Screen 1: Log Session
- Renders Part 1 (Sections A–E) + active Part 4 conditional questions.
- Generates a client-side `UUID` before network calls and disables the Submit button while in flight to prevent duplicate entries.
- Captures `session_date` and `session_time` in local timezone on submit, encrypts JSON, inserts into Supabase, and updates `SessionRepository._records`.

### Screen 2: Past Entries & Editor
- Lists decrypted entries from `SessionRepository` in reverse-chronological order (`session_date` + `session_time` descending) with search/filter by date or `primary_trigger`.
- Tapping an entry opens the shared `HabitFormContent` pre-populated with that record's answers.
- **Immutability Rule:** Display `session_date` and `session_time` in a locked read-only banner. On save, explicitly copy `original.session_date` and `original.session_time` onto the updated object, re-encrypt with a fresh random IV, update the row in Supabase, and update in-memory state (triggering immediate recalculation on Screen 3).
- Support single-entry deletion (with confirmation) and "Delete All History".

### Screen 3: Behavioral Analysis Dashboard
Must follow this exact vertical section order, driven by a sticky top filter bar (`Last 7 days`, `Last 30 days`, `Last 90 days`, `Last 6 months`, `This year`, `All time`, `Custom date range`, `Compare with previous period` toggle, `Reset`, `Refresh`):

1. **Overview & Key Statistics:** (1) Total entries, (2) Entries today, (3) Entries this week, (4) Entries this month, (5) Avg entries/week, (6) Avg entries/month, (7) Avg gap, (8) Median gap, (9) Longest gap, (10) Shortest gap, (11) Avg urge intensity, (12) Avg experience satisfaction, (13) Spontaneous %, (14) Planned %, (15) Media consumption %, (16) Full control %, (17) Activity affected %. Each displays current value, previous-period value (when toggle enabled and previous period has `> 0` records), difference, % change, and sample size `n`.
2. **Frequency Analysis:** Total entries, daily/weekly/monthly counts & averages, gap metrics, peak day/week/month + Daily, Weekly, Monthly graphs + Calendar heatmap.
3. **Time-Based Analysis:** Frequencies across 6 buckets (`Early morning: 5–8 AM`, `Morning: 8 AM–12 PM`, `Afternoon: 12–5 PM`, `Evening: 5–9 PM`, `Night: 9 PM–12 AM`, `Late night: 12–5 AM`), most common time/hour/weekday, weekday vs. weekend, monthly & seasonal distribution + charts.
4. **Trigger & Influence Analysis:** Primary ranking (`primary_trigger`), secondary triggers, overall trigger frequency (`triggers`, percentages allowed to exceed 100% combined), top combinations, trigger by time/weekday/month, trigger vs. emotion/motivation/planned, avg urge & satisfaction per trigger.
5. **Emotional Analysis:** Most common emotion before/after, motivation ranking, emotion combinations, satisfaction distribution, decision satisfaction %, avg satisfaction by primary trigger, avg urge by emotion, before-and-after emotion comparison (categorical co-occurrence only—never subtract emotion counts as numeric mood scores).
6. **Control & Behavior Analysis:** Planned vs. spontaneous %, postponement %, urge intensity (avg/max/min) & duration distribution, perceived control distribution, avg urge & satisfaction by control level, planned vs. spontaneous satisfaction, urge/duration/postponement vs. perceived control (ordinal score `1–4` used only for numerical correlation while preserving categorical labels).
7. **Lifestyle & Environment Analysis:** Location, activity beforehand, alone vs. not alone %, media consumption %, stimulation type, unexpected attention %, sleep duration distribution, activity interruption %, most common affected activity, cross-tabs with primary trigger, sleep vs. urge/frequency, activity vs. planned/spontaneous (strictly observational wording—never claim causation).
8. **Correlation & Relationship Analysis:** Compute all 17 cross-variable pairs (Trigger vs. Time/Emotion/Motivation/Satisfaction; Emotion vs. Urge/Control; Planned vs. Satisfaction; Urge intensity/duration vs. Control; Sleep vs. Urge/Frequency; Activity/Media vs. Trigger; Media vs. Stimulation; Activity impact vs. Control; Emotion before vs. after; Postponement vs. Satisfaction) with count, %, mean urge, mean satisfaction, and comparison to overall average.
9. **Personal Insights Generator:** Threshold-gated observations (`MIN_TOTAL = 5`, `MIN_SUBGROUP = 3`), neutral non-judgmental phrasing, supporting record count `n`, confidence badge (`Low`/`Moderate`/`High`), deduplication, and `recordIds`.
10. **Weekly & Monthly Reports:** Auto-generated period summaries + one-tap local PDF and CSV export.

- **Interactive Drill-Down:** Every KPI card, chart segment, and insight holds `recordIds: List<String>`. Tapping any item opens a `ModalBottomSheet` displaying the matching decrypted entries.

### Calculation Accuracy Rules (Mandatory)
1. Never treat unanswered optional questions as `"No"` or `0`.
2. Exclude `null`/unanswered values from question-specific denominators.
3. Never count multi-select selections as separate session entries.
4. Use `primary_trigger` for primary rankings and `triggers` for overall trigger frequency.
5. Use local device timezone for all date/time grouping.
6. Recalculate analytics immediately when entries are added, edited, or deleted.
7. Suppress period-over-period deltas when the previous period has `0` records.

---

## 6. Phased Execution & Verification Plan

### Phase 1: Zero-Knowledge Vault & Supabase Foundation
- **Scope:** Supabase Auth + RLS tables (`user_vault_meta`, `habit_entries`), `CryptoManager` (PBKDF2 + AES-256-GCM), Vault Setup/Unlock UI, `FLAG_SECURE`, and `ProcessLifecycleOwner` auto-wipe.
- **Tests:** Unit test round-trip encrypt/decrypt, wrong-password `AEADBadTagException` rejection, unique IV per encryption; verify zero plaintext/passwords on disk after backgrounding.
- **Definition of Done:**
  - [ ] Vault initializes with salt + encrypted canary and unlocks only with exact password.
  - [ ] Backgrounding the app wipes the key from RAM and locks the UI; screenshots are blocked.

### Phase 2: Screen 1 — Core & Conditional Questionnaire Logging
- **Scope:** `HabitRecord` model, Screen 1 Compose UI (20 core + 5 conditional questions), reactive conditional state cleanup, automatic local timestamping, duplicate-submit guard, and encrypted Supabase insert.
- **Tests:** Verify deselected conditional triggers set conditional fields to `null`; verify unanswered optional fields serialize as `null`; verify rapid double-taps create only 1 row.
- **Definition of Done:**
  - [ ] All 20 core and 5 conditional questions render, validate, and clean up stale state accurately.
  - [ ] Encrypted records save to Supabase with local `session_date` and `session_time`.

### Phase 3: Screen 2 — Decrypted Ledger, Editing & Deletion
- **Scope:** `SessionRepository` in-memory `StateFlow`, Screen 2 reverse-chronological list with search/filter, shared `HabitFormContent` edit mode with locked timestamp header, re-encryption on update, single/bulk deletion.
- **Tests:** Assert `updateEntry()` preserves original `session_date` and `session_time` even if modified inputs are passed; verify edit round-trip after lock/unlock; verify deletion removes rows from Supabase and RAM.
- **Definition of Done:**
  - [ ] Past entries display cleanly and can be edited while `session_date` and `session_time` remain immutable.
  - [ ] Edits re-encrypt with a new IV and update Supabase and in-memory state immediately.

### Phase 4: Core Analytics Engine (Sections 1–7)
- **Scope:** Pure Kotlin modules `OverviewAnalyzer`, `FrequencyAnalyzer`, `TimeAnalyzer`, `TriggerAnalyzer`, `EmotionalAnalyzer`, `ControlAnalyzer`, and `LifestyleAnalyzer` + date range & comparison filters.
- **Tests:** Run deterministic unit tests against a 30-record fixture (with missing optionals, multi-selects, and zero-record previous period) plus 0-record and 1-record edge cases.
- **Definition of Done:**
  - [ ] All 7 analyzers pass 100% of unit tests with exact sample sizes (`n`) and `recordIds` attached.
  - [ ] Both mean and median gaps compute safely without crashing on `< 2` records.

### Phase 5: Correlation Engine, Insights & Reports (Sections 8–10)
- **Scope:** `CorrelationAnalyzer` (17 relationships), `InsightGenerator` (sample thresholds, confidence badges, neutral wording), `ReportGenerator` (Weekly/Monthly), and local PDF/CSV export.
- **Tests:** Verify `InsightGenerator` returns empty list when `n < 5`; verify cross-tab counts against manual fixture math; generate and open PDF and CSV files on emulator/device.
- **Definition of Done:**
  - [ ] All 17 correlations and threshold-gated personal insights compute accurately with `recordIds`.
  - [ ] Weekly and Monthly reports export cleanly to PDF and CSV under user control.

### Phase 6: Screen 3 Dashboard UI, Drill-Downs & End-to-End QA
- **Scope:** Full Screen 3 Compose UI in exact 10-section order, sticky filter bar, charts + calendar heatmap, tap-to-drill-down `ModalBottomSheet` linked to Screen 2 editor, loading/empty/error states, Light/Dark themes.
- **Tests:** End-to-end flow: create entry on Screen 1 $\rightarrow$ verify Screen 3 updates $\rightarrow$ tap metric drill-down $\rightarrow$ edit entry $\rightarrow$ verify live recalculation; verify all 10 sections update across all 7 date filters.
- **Definition of Done:**
  - [ ] All 10 dashboard sections render in order with working charts, heatmaps, and drill-down sheets.
  - [ ] Responsive across phone and tablet viewports in Light and Dark modes with zero crashes.
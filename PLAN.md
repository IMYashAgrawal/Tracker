# Project Specification & Implementation Plan: Zero-Knowledge Personal Activity Tracker

## 1. System Overview & Core Constraints
Build a native Android application (Kotlin, Jetpack Compose, Material 3) for discreetly logging and analyzing personal habit/activity sessions.

### Strict Architectural & Security Rules
1. **Zero Local Persistence:** Nothing may be stored permanently on the device (no Room/SQLite database, no DataStore/SharedPreferences for records or keys, no disk caching of decrypted payloads). Android backup is fully disabled (`allowBackup=false`, data extraction rules exclude all domains).
2. **In-App Password Verification:** A PBKDF2-HMAC-SHA256 hash of the master password is hardcoded in the app binary. On unlock, the entered password is hashed with the same algorithm (salt: `TrackerFixedSalt01`, iterations: 100,000, dklen: 32) and compared. No network call is needed for authentication.
3. **Client-Side Encryption:** On successful password verification, a separate AES-256 encryption key is derived via PBKDF2 (600,000 iterations, random session salt) and held in volatile RAM. All data is encrypted with AES-256-GCM (12-byte random IV, 128-bit auth tag) before being sent to Supabase. Supabase stores only opaque ciphertext.
4. **Ephemeral Key Handling:** The derived AES key and session salt live strictly in volatile `ByteArray` fields and are zeroed out (`Arrays.fill(key, 0)`) immediately when the app goes to background (`ProcessLifecycleOwner` `onStop`) or locks. The password is never saved to disk, Android Keystore, logs, or analytics.
5. **Screen Privacy:** `WindowManager.LayoutParams.FLAG_SECURE` is enabled on `MainActivity` to prevent screenshots and hide content in the Android Recents switcher.
6. **Discreet Naming & Privacy:** Use neutral, non-judgmental wellness terminology throughout the UI. Never send questionnaire data to third-party analytics or crash-reporting SDKs.

---

## 2. Tech Stack & Project Structure
- **Language / UI:** Kotlin, Jetpack Compose, Material 3 (Light & Dark theme support), Navigation Compose.
- **Backend:** Supabase PostgreSQL via `supabase-kt` (Postgrest only — **no Supabase Auth**).
- **Authentication:** In-app PBKDF2 password hash comparison (no server-side auth).
- **Cryptography:** `javax.crypto` (`PBKDF2WithHmacSHA256`; `AES/GCM/NoPadding`, 12-byte random `SecureRandom` IV per encryption, 128-bit auth tag).
- **Serialization:** `kotlinx.serialization.json`.

### Directory Structure
```text
app/src/main/java/com/yash/tracker/
├── security/
│   ├── CryptoManager.kt          # Password hash verify, PBKDF2 key derivation, AES-GCM encrypt/decrypt, RAM wipe
│   └── VaultLifecycleObserver.kt  # Auto-lock & memory wipe on background
├── data/
│   ├── model/
│   │   └── HabitRecord.kt        # Full 20 + 5 + 2 timestamp serializable domain model
│   ├── remote/
│   │   └── SupabaseManager.kt    # Supabase Postgrest-only client (no Auth module)
│   └── repository/
│       └── SessionRepository.kt  # In-memory StateFlow<List<HabitRecord>>, CRUD against `entries` table
├── analytics/
│   ├── model/
│   │   ├── DateRangeFilter.kt
│   │   ├── AnalyticsBundle.kt    # OverviewMetrics and AnalyticsValue<T>
│   │   ├── Insight.kt
│   │   └── ...                   # Various metrics models
│   ├── utils/
│   │   ├── DateHelpers.kt
│   │   └── Statistics.kt
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
    ├── vault/                    # Password unlock screen (no setup — hash is in-app)
    ├── components/               # Reusable HabitFormContent
    ├── screen1_log/              # Screen 1: New entry logging
    ├── screen2_history/          # Screen 2: Decrypted past entries list & editor
    └── screen3_analysis/         # Screen 3: Behavioral analytics dashboard
```

---

## 3. Supabase Database Schema (`PostgreSQL`)

The database has a single table. No `user_vault_meta`, no `auth.users`, no `user_id` column. All security comes from client-side encryption.

```sql
-- Single table for encrypted session blobs
CREATE TABLE entries (
    id TEXT PRIMARY KEY,
    encrypted_payload TEXT NOT NULL,  -- Base64(12-byte IV + Ciphertext + 16-byte GCM Tag)
    created_at TIMESTAMPTZ DEFAULT now()
);

-- RLS enabled with a permissive policy (silences Supabase warnings)
ALTER TABLE entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "allow_all" ON entries
    FOR ALL
    USING (true)
    WITH CHECK (true);
```

> **Note:** The `true` RLS policy means anyone with the anon key can read/write. This is intentional — the data is opaque encrypted blobs that are useless without the AES key derived in-app. Security is provided by encryption, not by RLS.

---

## 4. Authentication Flow

```
User enters password
       │
       ▼
SHA-256? No. PBKDF2-HMAC-SHA256(password, "TrackerFixedSalt01", 100000, 32)
       │
       ▼
Base64 encode → compare with hardcoded hash
       │
       ├── Mismatch → Show "Incorrect password" error
       │
       └── Match → Derive AES-256 key via PBKDF2(password, random_salt, 600000, 256-bit)
                    Hold key + salt in volatile RAM only
                    → Unlock app → Allow Supabase operations
```

**On background/lock:** `VaultLifecycleObserver.onStop()` → `CryptoManager.wipeKey()` → zeros out key & salt → UI returns to lock screen.

---

## 5. Data Schema: Questionnaire & Automatic Fields

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
  - Options: Dynamically constrained to options selected in **Q4**.
- **Q6 (`urge_intensity`)** — *How strong was the feeling?* | Numeric rating (`Int`, `1–10`) | Required: **Yes**
- **Q7 (`urge_duration`)** — *How long had you been feeling this way?* | Single choice | Required: **No** (`String?`)
  - Options: `Less than 5 minutes`, `5–15 minutes`, `15–30 minutes`, `30–60 minutes`, `More than an hour`

#### C. Emotional State (3 questions)
- **Q8 (`emotions_before`)** — *How were you feeling beforehand?* | Multiple choice (`List<String>`) | Required: **Yes**
- **Q9 (`motivation`)** — *What were you looking for?* | Single choice | Required: **No** (`String?`)
- **Q10 (`emotions_after`)** — *How did you feel afterward?* | Multiple choice (`List<String>`) | Required: **Yes**

#### D. Control and Media (5 questions)
- **Q11 (`planned`)** — *Was this planned or spontaneous?* | Single choice | Required: **Yes**
- **Q12 (`tried_resisting`)** — *Did you try to postpone it?* | Single choice | Required: **Yes**
- **Q13 (`media_consumed`)** — *Did you consume any media beforehand?* | Single choice | Required: **Yes**
- **Q14 (`stimulation_type`)** — *What type of content or experience was involved?* | Multiple choice (`List<String>?`) | Required: **No**
- **Q15 (`unexpected_attention`)** — *Did anything unexpected catch your attention?* | Single choice | Required: **No** (`String?`)

#### E. Outcome and Reflection (5 questions)
- **Q16 (`experience_satisfaction`)** — *How satisfied were you with the experience?* | Numeric rating (`Int`, `1–10`) | Required: **Yes**
- **Q17 (`decision_satisfaction`)** — *Are you satisfied with your decision?* | Single choice | Required: **Yes**
- **Q18 (`activity_impact`)** — *Did this affect your planned activities?* | Single choice | Required: **Yes**
- **Q19 (`perceived_control`)** — *How much control did you feel you had?* | Single choice | Required: **Yes**
- **Q20 (`sleep_duration`)** — *How much sleep did you get last night?* | Single choice | Required: **No** (`String?`)

### Part 4: Conditional Questions (5 Dynamic Branching Fields)
1. **If `Q4 (triggers)` includes `"Something online"`** → `online_platform`
2. **If `Q4 (triggers)` includes `"Stress"`** → `stress_source`
3. **If `Q11 (planned) == "Spontaneous"`** → `spontaneous_reason`
4. **If `Q13 (media_consumed) == "Yes"`** → `intentional_media`
5. **If `Q18 (activity_impact) != "No"`** → `affected_activity`

---

## 6. Screen Specifications

### Screen 1: Log Session
- Renders Part 1 (Sections A–E) + active Part 4 conditional questions.
- Generates a client-side `UUID`, captures `session_date` and `session_time` in local timezone on submit.
- Encrypts JSON with AES-GCM, inserts into `entries` table, and updates `SessionRepository._records`.

### Screen 2: Past Entries & Editor
- Lists decrypted entries from `SessionRepository` in reverse-chronological order.
- Tapping an entry opens the shared `HabitFormContent` pre-populated with that record's answers.
- `session_date` and `session_time` are displayed as read-only.
- On save, re-encrypts with a fresh random IV, upserts to `entries`, updates in-memory state.
- Supports single-entry deletion with confirmation dialog.
- Shows error state with retry button on network/decryption failures.

### Screen 3: Behavioral Analysis Dashboard
- Date range filter bar: `7D`, `30D`, `90D`, `All Time`.
- **Overview section:** Total entries, this week, this month, avg urge, avg satisfaction, spontaneous %, full control %, media used %, avg gap, activity affected %.
- **Insights section:** Threshold-gated observations from `InsightGenerator` (peak time, primary trigger, planning impact) with confidence badges.
- Refresh button to re-analyze.

---

## 7. Security Architecture Summary

| Concern | Solution |
| :--- | :--- |
| Password storage | PBKDF2 hash hardcoded in APK — never stored on disk or sent over network |
| Data at rest (server) | AES-256-GCM encrypted blobs — Supabase sees only ciphertext |
| Data at rest (device) | Nothing persisted — all data lives in volatile RAM `StateFlow` |
| Key lifecycle | Derived on unlock, wiped on background/lock via `ProcessLifecycleOwner` |
| Screenshots/Recents | `FLAG_SECURE` on activity window |
| Backup leakage | `allowBackup=false`, all data extraction rules exclude everything |
| RLS | Permissive `true` policy — security from encryption, not access control |

---

## 8. Implementation Status

- [x] **Phase 1:** In-app password verification, AES key derivation, vault lock/unlock, auto-wipe on background
- [x] **Phase 2:** Screen 1 — Full questionnaire with 20 core + 5 conditional questions
- [x] **Phase 3:** Screen 2 — Decrypted history list, edit, delete with error handling
- [x] **Phase 4:** Analytics engine — OverviewAnalyzer, InsightGenerator, and all analyzer modules
- [x] **Phase 5:** Screen 3 — Analysis dashboard with date filters, overview stats, and insights
- [ ] **Phase 6:** Full 10-section dashboard UI with charts, heatmaps, drill-downs, and PDF/CSV export

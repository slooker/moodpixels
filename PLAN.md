# MoodPixels — Implementation Plan

## What This App Does

MoodPixels is an Android mood-tracking app that displays a calendar and lets you assign
colored "pixels" to time slots representing your mood. On first launch, you define a legend
(e.g. Red = Anger, Blue = Sadness, Green = Happiness). You can then tap any day or hour slot
to log a mood, optionally add a note, and later export everything as JSON to share.

---

## Project Setup

- **Package:** `us.slooker.moodpixels`
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35
- **Language:** Kotlin 2.0.21
- **Build:** AGP 8.9.0-rc02, KSP 2.0.21-1.0.28
- **UI paradigm:** View-based (no Jetpack Compose), Material Components

### Key dependencies added (beyond the scaffold)

| Library | Version | Purpose |
|---|---|---|
| Room + KSP | 2.7.0 | Local SQLite database |
| Lifecycle ViewModel/LiveData | 2.9.0 | MVVM state management |
| Kotlinx Coroutines Android | 1.9.0 | Async DB operations |
| Gson | 2.11.0 | JSON export serialization |
| Activity/Fragment KTX | 1.10.1 / 1.8.6 | `by viewModels()` delegate |
| Core library desugaring | 2.1.4 | `java.time` API on minSdk 24 |

`isCoreLibraryDesugaringEnabled = true` is set in `app/build.gradle.kts` so that
`java.time.LocalDate` works on devices below API 26.

---

## Architecture

```
UI Layer
  Activities / DialogFragments  <──>  ViewModels (LiveData)
        │                                    │
  Custom Canvas Views              MoodRepository
  (BaseCalendarView subclasses)         │
  LegendAdapter                  ┌──────┴──────┐
                                 │             │
                             Room DAO     LegendPrefs
                          (Flow<List>)   (SharedPrefs)
                                 │
                            AppDatabase
                          (mood_entries)
                                 │
                           JsonExporter
                     (writes cache file → share intent)
```

Data flows one way: Room emits `Flow<List<MoodEntry>>`, the ViewModel transforms it into
`Map<String, Map<Int, MoodEntry>>` (keyed by ISO date then hour slot), posts to LiveData,
and the active calendar View calls `invalidate()` in the observer.

---

## File Structure

```
app/src/main/java/us/slooker/moodpixels/
│
├── MoodPixelsApp.kt                    Application class; lazy singletons for DB/repo/prefs
│
├── data/
│   ├── db/
│   │   ├── MoodEntry.kt                Room @Entity
│   │   ├── MoodEntryDao.kt             Room @Dao (upsert, range queries, export)
│   │   └── AppDatabase.kt              Room @Database singleton
│   ├── model/
│   │   └── LegendEntry.kt              Data class (id, colorValue Int, moodName)
│   ├── prefs/
│   │   └── LegendPrefs.kt              SharedPrefs wrapper; stores legend as JSON array
│   └── repository/
│       └── MoodRepository.kt           Single source of truth; wraps DAO
│
├── export/
│   └── JsonExporter.kt                 Builds share Intent from all MoodEntries
│
└── ui/
    ├── adapter/
    │   └── LegendAdapter.kt            RecyclerView adapter for setup/settings legend list
    ├── dialogs/
    │   ├── ColorPickerDialog.kt         18 preset swatches + hex input; returns @ColorInt
    │   └── MoodEntryDialog.kt           Tap-to-log dialog: legend picker + optional note
    ├── main/
    │   ├── MainActivity.kt             5-tab calendar host; first-run redirect
    │   └── MainViewModel.kt            View mode, anchor date, entriesMap LiveData
    ├── settings/
    │   └── SettingsActivity.kt         Legend preview, Edit, Export, Delete All
    ├── setup/
    │   ├── LegendSetupActivity.kt      First-run + edit-mode legend builder
    │   └── LegendSetupViewModel.kt     In-memory list of LegendEntry before saving
    └── views/
        ├── BaseCalendarView.kt         Abstract View; shared Paint objects, onSlotClick callback
        ├── TimeGridView.kt             Day / 3-Day / Week (controlled by columnCount 1/3/7)
        ├── MonthView.kt                Month grid with micro pixel mosaic per day cell
        └── YearView.kt                 4×3 grid of mini-months; dominant color per day
```

---

## Data Model

### Room: `mood_entries` table

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK autoincrement | |
| `entry_date` | TEXT (indexed) | ISO-8601 `"YYYY-MM-DD"` |
| `hour_slot` | INTEGER | 0–23; `-1` = whole-day entry |
| `color_value` | INTEGER | Android `@ColorInt` (ARGB) |
| `mood_name` | TEXT | Copied from legend at log time (denormalized) |
| `note` | TEXT nullable | Optional free-text, max 500 chars enforced in UI |
| `created_at` | INTEGER | Unix epoch millis |

**Upsert strategy:** before inserting, `deleteSlot(date, hour)` removes any existing entry
for that exact slot, then `insert()` adds the new one. No UNIQUE constraint needed.

### SharedPreferences: legend

Key `"legend_entries"` → Gson JSON array of `LegendEntry`:
```json
[
  {"id": "uuid", "colorValue": -65536, "moodName": "Anger"},
  {"id": "uuid", "colorValue": -16711936, "moodName": "Happy"}
]
```
Key `"legend_setup_complete"` → Boolean; `false` triggers first-run flow.

---

## Activity Flow

```
Cold start
    │
    ▼
MainActivity.onCreate()
    │
    ├─ legendPrefs.isSetupComplete() == false
    │       │
    │       └─► LegendSetupActivity (isFirstRun mode)
    │               User adds ≥1 legend entry with name + color
    │               Tap "Done" → saves legend, sets flag true
    │               → startActivity(MainActivity), finish()
    │
    └─ isSetupComplete() == true
            │
            └─► Show calendar (default: Month view)
                    Toolbar menu: [Share icon = Export] [Gear icon = Settings]
                    Tabs: Day | 3-Day | Week | Month | Year
                    Nav row: < [date label] Today >

Settings flow:
    Settings → "Edit Legend" → LegendSetupActivity (edit mode, EXTRA_EDIT_MODE=true)
                                    saves on Done, finishes back to Settings
```

---

## Calendar Views

All five modes share `BaseCalendarView`:
- `var anchorDate: LocalDate` — setter calls `invalidate()`
- `var entriesMap: Map<String, Map<Int, MoodEntry>>` — setter calls `invalidate()`
- `var onSlotClick: (date: String, hour: Int, existing: MoodEntry?) -> Unit`

### TimeGridView (`columnCount` = 1 / 3 / 7)

```
[52dp time labels] | [col 1] | [col 2] | ... | [col N]
                     ─────────────────────────────────
Hour 0  (12am)  │   [colored rect if mood logged]   │
Hour 1  (1am)   │                                   │
...
Hour 23 (11pm)  │                                   │
```
- Wrapped in a `ScrollView` so all 24 rows are accessible
- Today's column gets a light-yellow background highlight
- Tapping a filled cell re-opens the entry dialog (pre-selected with existing mood)

### MonthView

- 7-column grid (Mon–Sun headers), one cell per day
- Each day cell shows a micro pixel mosaic: up to 24 tiny colored squares (6 cols × 4 rows)
  arranged left-to-right by hour, representing that day's logged moods
- Day number in top-left corner; today highlighted red
- **Tapping a day navigates to Day view** for that date (hour sentinel = `-1`)

### YearView

- 3 columns × 4 rows of month blocks
- Each day cell = single pixel = **dominant color** (most-logged color that day)
- Gray cell = no entries; yellow cell = today (no entries)
- **Tapping a month block navigates to Month view** (hour sentinel = `-2`)
- Wrapped in a `ScrollView`

---

## Mood Entry Dialog

Opened by tapping any time slot in any view:

```
[Day name, Month Day — Hour:00 AM/PM]

How are you feeling?
[ ● Anger ] [ ● Happy ] [ ● Calm ] ...  (horizontal scroll)

[Note (optional)________________]

[Clear This Entry]        [Cancel] [Save]
```

- "Clear This Entry" button only visible when editing an existing entry
- Tapping a legend swatch highlights it with a border
- Save → `MainViewModel.upsertEntry()` → Room → Flow emits → LiveData updates → View redraws

---

## Color Picker Dialog

Used in legend setup/edit:
- 18 preset color ovals in a `GridLayout` (6 columns)
- Hex input field (`#RRGGBB` format)
- Live oval preview of selected color
- Returns a raw `@ColorInt` via `onColorSelected` callback

---

## Export Format

`JsonExporter.buildShareIntent()`:
1. Queries all entries via `getAllEntriesSnapshot()` (suspend, off main thread)
2. Serializes to pretty-printed JSON array
3. Writes to `<cacheDir>/exports/mood_pixels_export.json`
4. Creates a `FileProvider` URI (authority: `us.slooker.moodpixels.fileprovider`)
5. Returns an `ACTION_SEND` intent → caller wraps with `Intent.createChooser`

Sample output:
```json
[
  {
    "date": "2025-03-15",
    "hour": 14,
    "color": "#E53935",
    "mood": "Anger",
    "note": "Stressful meeting"
  },
  {
    "date": "2025-03-15",
    "hour": 20,
    "color": "#43A047",
    "mood": "Happy",
    "note": null
  }
]
```
No legend settings or app config is included in the export.

---

## Scheduled Questions & Notifications

Users can create questions asked on a schedule (e.g. "Did you take your meds?"). Each
question has a notification that fires at a configured time and leads to an answer screen.

### Data Model additions (DB version 2)

**`questions` table** (`Question.kt`):

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK autoincrement | |
| `text` | TEXT | The question to ask |
| `answer_type` | TEXT | `"TEXT"` / `"YES_NO"` / `"NUMBER"` |
| `schedule_type` | TEXT | `"DAILY"` / `"SPECIFIC_DAYS"` |
| `schedule_days` | INTEGER | Bitmask: bit0=Mon…bit6=Sun; 127 = all days |
| `notify_hour` | INTEGER | 0–23 |
| `notify_minute` | INTEGER | 0–59 |
| `is_active` | INTEGER (Boolean) | 1 = active (alarms scheduled) |
| `created_at` | INTEGER | Unix epoch millis |

**`question_answers` table** (`QuestionAnswer.kt`):

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK autoincrement | |
| `question_id` | INTEGER FK → questions(id) CASCADE DELETE | |
| `question_text` | TEXT | Denormalized snapshot of question at answer time |
| `answer_text` | TEXT nullable | For TEXT type |
| `answer_bool` | INTEGER nullable | For YES_NO type |
| `answer_number` | REAL nullable | For NUMBER type |
| `answered_at` | INTEGER | Unix epoch millis |

Migration `MIGRATION_1_2` in `AppDatabase.kt` handles upgrading existing installs.

### Notification flow

```
AlarmScheduler.scheduleNext(context, question)
    │  Calculates next calendar trigger based on schedule_type + schedule_days
    │  Uses setExactAndAllowWhileIdle (or setAndAllowWhileIdle on API 31+
    │  if SCHEDULE_EXACT_ALARM not granted)
    ▼
QuestionAlarmReceiver.onReceive()   [BroadcastReceiver, goAsync()]
    │  Loads question from DB
    │  Posts notification via NotificationHelper
    └─ Reschedules next alarm

Notification tap → AnswerActivity
    │  Shows question text + input widget matching answer_type
    └─ On submit: saves QuestionAnswer, cancels notification, finishes

BootReceiver.onReceive()  [BOOT_COMPLETED]
    └─ Reschedules all active questions
```

### Files added for this feature

```
notifications/
    NotificationHelper.kt       Channel creation + posting + cancellation
    AlarmScheduler.kt           scheduleNext() / cancel() using AlarmManager
    QuestionAlarmReceiver.kt    BroadcastReceiver: post notification + reschedule
    BootReceiver.kt             BroadcastReceiver: reschedule on device boot

data/db/
    Question.kt                 Room @Entity
    QuestionAnswer.kt           Room @Entity (FK to questions)
    QuestionDao.kt              Room @Dao

data/repository/
    QuestionRepository.kt       Wraps QuestionDao, singleton

ui/questions/
    QuestionsActivity.kt        RecyclerView list of questions + FAB to add
    QuestionsViewModel.kt       save/delete/toggleActive with alarm scheduling
    QuestionAdapter.kt          ListAdapter with edit/delete/switch per row

ui/answer/
    AnswerActivity.kt           Answer input screen (opened from notification)
    AnswerViewModel.kt          Loads question, saves answer by type

res/layout/
    activity_questions.xml      CoordinatorLayout: Toolbar + RecyclerView + FAB
    activity_answer.xml         Toolbar + question text + 3 answer layouts (visibility toggled)
    dialog_edit_question.xml    Question form: text, answer type, schedule, day checkboxes, time
    item_question.xml           Row: question text, schedule summary, active Switch, edit/delete

res/drawable/
    ic_notification.xml         Bell icon (white) for notification
    ic_edit.xml                 Pencil icon for edit button in question list
```

### Permissions added to AndroidManifest.xml

- `RECEIVE_BOOT_COMPLETED` — reschedule alarms after reboot
- `POST_NOTIFICATIONS` — required on Android 13+ (runtime permission requested in QuestionsActivity)
- `SCHEDULE_EXACT_ALARM` — for precise alarm timing

### Export format (updated)

`JsonExporter.buildShareIntent(context, moodEntries, questions, answers)` now outputs:

```json
{
  "exportedAt": "2025-03-15T14:30:00",
  "moodEntries": [ { "date": "...", "hour": 14, "color": "#E53935", "mood": "Anger", "note": null } ],
  "questions": [
    {
      "id": 1,
      "text": "Did you take your meds?",
      "answerType": "YES_NO",
      "scheduleType": "DAILY",
      "scheduleDays": ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"],
      "notifyTime": "09:00",
      "isActive": true,
      "answers": [
        { "answeredAt": "2025-03-15T09:05:00", "value": true }
      ]
    }
  ]
}
```

---

## Things to Know When Resuming on Another Machine

1. **Android Studio version:** any version that supports AGP 8.9+ (Ladybug or newer)
2. **Java:** JDK 17+ required (JDK 21 recommended for AGP 8.9)
3. **First Gradle sync** will download all dependencies (~150 MB)
4. **KSP** generates Room code into `app/build/generated/ksp/` — do not edit those files
5. **Schemas** will be generated into `app/schemas/` on first build — safe to commit
6. **No signing config** is set; debug builds work fine for development

### To run
```
File → Open → select the MoodPixels folder
Wait for Gradle sync
Run → select device/emulator → Run 'app'
```

### To add a new calendar view mode
1. Create a new `class FooView : BaseCalendarView`
2. Override `onDraw`, `onMeasure`, `onTouchEvent`
3. Add an enum value to `CalendarViewMode`
4. Handle it in `MainViewModel.currentRange()`, `shiftPeriod()`, `getDateLabel()`
5. Handle it in `MainActivity.switchCalendarView()`
6. Add a tab in `MainActivity.setupTabs()`

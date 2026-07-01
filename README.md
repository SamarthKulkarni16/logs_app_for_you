# logs_app_for_you

A minimal daily-logging Android app. One entry per calendar day — open the
app and you're always looking at *today*, resuming whatever you already
wrote if you've opened it earlier the same day. History is a month grid,
not a list: every day is a dot, dot size reflects how much you wrote, and
only days you actually logged are tappable.

This is a sibling app to `notes_app_for_you`, not the same product wearing
a different name — notes are freeform, multi-entry, anytime; logs are one
continuous thread per day, structured around the calendar.

## Current status: Phase 1 — Build

- [x] Day-keyed local storage (`logs/yyyy-MM-dd.md`, one file per day)
- [x] Month-grid history with word-count-scaled dots, real calendar
      alignment, only-written-days tappable
- [x] AI monthly reflection via Gemini (reads the month's logs, returns a
      short reflective summary — the one feature that's genuinely
      logs-specific rather than borrowed from the notes app)
- [x] Auth (email/password + Google Sign-In via Credential Manager, shared
      Supabase project)
- [x] Offline-first local write + background sync to Supabase
- [ ] **Not done yet:** the `daily_logs` table doesn't exist in Supabase yet
      (see below) — sync will silently no-op until it's created
- [ ] **Not done yet:** CI secrets aren't set on this repo yet — first
      build won't trigger until they are
- [ ] **Not done yet:** landing page (Phase 2) and public download flow
      (Phase 3)

## Stack

- Kotlin + Jetpack Compose
- Supabase Auth (`auth-kt` 3.6.0, same shared Supabase project as
  `notes_app_for_you` and Flow Timer)
- Google Sign-In via Android Credential Manager
- Gemini API (`gemini-2.0-flash`) for the monthly reflection, called
  directly via `HttpURLConnection` — no extra SDK dependency
- minSdk 26, targetSdk/compileSdk 35

## Backend setup needed before this can sync (Phase 1, not yet done)

Run this against the shared Supabase project:

```sql
create table daily_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id),
  log_date date not null,
  body text not null,
  updated_at timestamptz not null default now(),
  unique (user_id, log_date)
);

alter table daily_logs enable row level security;

create policy "Users can manage their own logs"
  on daily_logs for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
```

The `unique (user_id, log_date)` constraint is what makes upsert-by-day
work — writing today's log repeatedly updates one row instead of creating
duplicates.

## Local setup

1. Copy `local.properties.example` to `local.properties` (gitignored).
2. Fill in `sdk.dir`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`,
   `GOOGLE_WEB_CLIENT_ID`, and `GEMINI_API_KEY`.
3. Open in Android Studio, let Gradle sync, run on a device/emulator.

## CI

`.github/workflows/build.yml` builds a signed release APK on every push to
`main` (or manually via "Run workflow"). Needs these repo secrets:

| Secret | Purpose |
|---|---|
| `SUPABASE_URL` | Same value as local.properties |
| `SUPABASE_ANON_KEY` | Same value as local.properties |
| `GOOGLE_WEB_CLIENT_ID` | Same value as local.properties — **needs its own OAuth client registered in Google Cloud Console with this app's release SHA-1**, separate from notes_app_for_you's client |
| `GEMINI_API_KEY` | Powers the monthly reflection feature |
| `RELEASE_KEYSTORE_BASE64` | Your release keystore, base64-encoded |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias inside the keystore |
| `RELEASE_KEY_PASSWORD` | Key password |

The built APK is uploaded as a GitHub Release asset named `daily-logs.apk`.

## What's NOT done yet (later phases)

- **Phase 2 — Landing page:** app name, one-line description, download
  button, hosted the same way as the other Perspective Library sites.
- **Phase 3 — Download flow:** link in bio → landing page → APK download
  → first-launch sign-in prompt for backup/sync.

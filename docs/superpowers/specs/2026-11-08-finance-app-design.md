# Finance App — Design (v1)

Date: 2026-11-08. Owner: director. Status: draft, waiting for user review.

## Intent

A personal finance app for the user's own Android phone. It answers one question fast: "How much can I still spend this month, and where did my money go?" The user keeps several accounts (cash, bank, card, e-wallet), each with its own budget, sorts every transaction into a category, and tracks savings goals. Simple features first.

## Goals (v1)

1. Several accounts. Each has a name, a type and an opening balance. Balance = opening + income - expense +/- transfers.
2. Transactions: expense, income, transfer between own accounts. Each expense/income has one category.
3. Fast entry: one "+" button, big number pad, most used categories first. Target: a transaction in about 3 seconds.
4. Categories: default Vietnamese list, user can add, rename, archive.
5. Monthly budgets: a limit per month for an account, optionally narrowed to one category. Shows spent, remaining, and a warning colour.
6. Savings goals: name, target amount, optional deadline, manual contributions, progress bar, "save X per month to make it".
7. Home screen: money left to spend this month across all budgets, budgets near or over the limit, account balances.
8. Works fully offline. Data stays on the phone.

## Non-goals (v1)

- No login, no server, no sync between devices.
- No multi-currency. Everything is VND, whole đồng (no decimals).
- No charts, no recurring transactions, no reading bank SMS, no OCR. (Phase 2 candidates.)
- No iOS. Android only.

## Anti-goals

- Entering a transaction feels slow or needs more than one screen.
- A transfer between own accounts shows up as spending.
- The app loses or corrupts data.

## Rules the builder must keep

- Money is an integer number of đồng. Never use floating point for stored amounts.
- Amount input accepts `1250000`, `1.250.000`, `1,250,000`, `1 250 000` and a `k` suffix (`50k` = 50000). Anything else is invalid.
- Display format: `1.250.000 ₫`.
- A transaction belongs to the month of its own date, not the month it was entered.
- Transfers never count in any budget or in income/expense totals.
- Budget status: `ok` when spent < 80% of limit, `warn` from 80% up to and including 100%, `over` when spent > limit.
- An account or category that has transactions cannot be deleted, only archived. Archived items are hidden from pickers but kept in history.
- Goal "per month" = remaining / months left (current month through deadline month, at least 1), rounded up. No deadline or goal reached: none shown.
- Goal contributions are tracked separately from accounts in v1 (they do not move money between accounts).

## Target device and weight (user's requirements)

- The user's phone: Xiaomi Redmi, Android 16 (API 36), Xiaomi HyperOS 3.0.
- The app must stay light: small APK, fast start, no background service.
- Later phase: home-screen widgets. They must work on HyperOS, so they cannot depend on background work that HyperOS may kill.
- The user does not want Android Studio installed. Everything that goes on the PC stays in `D:\android-tools` and as little as possible on drive C:.

## Decisions

User decided: Android first; native Kotlin; command-line Android tools only (no Android Studio, no emulator), kept in `D:\android-tools`.

Director's call (user may override):

- Stack: Kotlin, Jetpack Compose (Material 3), Room (SQLite), Navigation Compose, Jetpack Glance for widgets. No DI framework, no network library, no image library.
- Modules: `:core` = pure Kotlin/JVM (all money rules, tested with JUnit, no Android), `:app` = Android app (UI, Room, widgets).
- Package `com.superduperlover.finance`. minSdk 26, targetSdk and compileSdk 36.
- Release build: R8 minify + shrinkResources. Weight target: release APK under 10 MB.
- Debug build uses `applicationIdSuffix ".debug"`, so the debug app installs next to the release app with separate data. Debug app = test data; release app = the user's real data.
- Testing by the user: the debug APK is installed on the Redmi over wireless ADB (no cable needed) at each milestone, with a short click-through checklist.
- Widgets (phase D): refreshed by the app right after each save (`updateAll`), not by periodic background jobs.
- Default expense categories: Ăn uống, Đi lại, Nhà ở, Hóa đơn, Mua sắm, Giải trí, Sức khỏe, Giáo dục, Khác. Default income categories: Lương, Thưởng, Thu nhập khác.
- UI language: Vietnamese.

## Left to the builder

Visual style, icons, navigation layout (tabs vs stack), component library choice within Expo defaults.

# ChiLi (Finance App) v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. In this vibe deck, the director assigns each task to the owner named in it (see `task_plan.txt`); AGENTS.md rules apply.

**Goal:** A light, offline Android finance app with accounts, categorised transactions, monthly budgets, savings goals and (phase D) home-screen widgets.

**Architecture:** Two Gradle modules. `:core` is pure Kotlin/JVM: every money rule, tested with JUnit, no Android. `:app` is the Android app: Compose screens, Room storage that maps rows to `:core` types, and Glance widgets. Screens read through `Repo`, compute with `:core`.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Navigation Compose, Jetpack Glance, JUnit. Versions: current stable, compatible set chosen in A1 and pinned in `gradle/libs.versions.toml`.

**Spec:** `docs/superpowers/specs/2026-11-08-finance-app-design.md`

## Global Constraints

- Toolchain: only `D:\android-tools` (JDK 21, Android SDK command-line tools, platform 36, build-tools 36.0.0). Every shell that builds first runs `. D:\android-tools\env.ps1`. No Android Studio, no emulator, nothing new on C:.
- App name **ChiLi**; applicationId `com.chili.app`; Kotlin package/namespace `com.superduperlover.finance`; minSdk 26; compileSdk/targetSdk 36; debug `applicationIdSuffix ".debug"`.
- No DI framework, no network library, no image library. Release: R8 minify + shrinkResources; release APK < 10 MB.
- Money: `Long` đồng. No `Double`/`Float` for amounts anywhere.
- Display: `1.250.000 ₫`. Input: `1250000`, `1.250.000`, `1,250,000`, `1 250 000`, `50k`.
- Transaction types and labels: `EXPENSE` "Chi tiêu" (red), `INCOME` "Thu nhập" (green), `TRANSFER` "Chuyển khoản" (neutral). Transfers never count as income, expense or budget spending.
- Budget status: `OK` < 80%, `WARN` 80%–100% inclusive, `OVER` > 100%.
- Items with transactions are archived, never deleted.
- UI text Vietnamese. Use the `ui-ux-pro-max` skill (`--stack jetpack-compose`) for visual decisions.
- Build command: `.\gradlew assembleDebug`. Test command: `.\gradlew :core:test`.
- The director gets the user's OK before any new dependency outside the stack above.

## Review Focus

1. Transfer between own accounts: changes both balances, appears in no budget or total. (A2 `budgetUsage` + `accountBalance` tests.)
2. Amount typed as `1.250.000` or `50k`: saved as 1250000 / 50000, never 1 or a crash. (A2 `parseVndInput` tests.)
3. Backdated transaction (last month): counts in last month's budget only. (A2 `budgetUsage` test.)
4. Spent exactly equal to limit: `WARN`, not `OVER`. (A2 `budgetUsage` test.)
5. Deleting an account or category that has transactions: blocked, archive offered. (A3 / B1 acceptance check.)

---

## File Structure

| Path | Responsibility |
|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradlew*`, `gradle/wrapper/` | Build setup |
| `core/src/main/kotlin/com/superduperlover/finance/core/Model.kt` | Data types shared by every layer |
| `core/.../core/Money.kt` | Parse and format VND |
| `core/.../core/Months.kt` | Month keys and month counting |
| `core/.../core/Balance.kt` | Account balances |
| `core/.../core/Budgets.kt` | Budget usage and status |
| `core/.../core/Goals.kt` | Goal progress |
| `core/.../core/Validate.kt` | Transaction validation |
| `core/src/test/kotlin/...` | JUnit tests, one file per source file |
| `app/src/main/java/com/superduperlover/finance/data/` | Room entities, DAOs, `AppDatabase`, `Repo`, default category seed |
| `app/.../ui/` | Compose screens: `home/`, `accounts/`, `transactions/`, `add/`, `budgets/`, `goals/`, `categories/`, shared `components/` |
| `app/.../widget/` | Glance widgets (phase D) |
| `MainActivity.kt` | Single activity, navigation host, 5 bottom tabs |

---

## Phase A — foundation (head_agent)

### Task A1: Gradle project skeleton

**Files:** Create the build files above, `core/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `core/src/test/kotlin/.../SmokeTest.kt`. Modify `.gitignore` (`build/`, `.gradle/`, `local.properties`, `*.apk`).

**Interfaces:** Produces the working commands `.\gradlew assembleDebug`, `.\gradlew :core:test`, and the app shell with 5 bottom tabs: Tổng quan, Tài khoản, Giao dịch, Ngân sách, Mục tiêu.

- [ ] **Step 1:** `. D:\android-tools\env.ps1`. Download the Gradle distribution zip (current stable) into `D:\android-tools\gradle-dist\` and run its `gradle wrapper` once in the repo root. After that only `.\gradlew` is used.
- [ ] **Step 2:** Write `local.properties` with `sdk.dir=D\:\\android-tools\\sdk` (git-ignored). Write the version catalog and the two module build files with the constraints above.
- [ ] **Step 3:** `SmokeTest` asserts `1 + 1 == 2`. Run `.\gradlew :core:test`. Expected: BUILD SUCCESSFUL, 1 test passed.
- [ ] **Step 4:** `MainActivity` with Compose, a Material 3 theme, Navigation Compose and 5 empty tabs. Run `.\gradlew assembleDebug`. Expected: `app\build\outputs\apk\debug\app-debug.apk` exists.
- [ ] **Step 5:** Run `.\gradlew assembleRelease` (unsigned is fine) and report the APK size.
- [ ] **Step 6:** Report the pinned versions and commands to the director (director updates AGENTS.md). Commit `chore: android project skeleton`.

### Task A2: Core logic (TDD, module `:core`)

**Interfaces — Produces (package `com.superduperlover.finance.core`):**

```kotlin
enum class AccountType { CASH, BANK, CARD, EWALLET }
data class Account(val id: Long, val name: String, val type: AccountType, val openingBalance: Long, val archived: Boolean = false)
enum class CategoryKind { EXPENSE, INCOME }
data class Category(val id: Long, val name: String, val kind: CategoryKind, val icon: String, val archived: Boolean = false)
enum class TxnKind { EXPENSE, INCOME, TRANSFER }
data class Txn(val id: Long, val kind: TxnKind, val amount: Long, val accountId: Long,
               val toAccountId: Long?, val categoryId: Long?, val date: LocalDate, val note: String = "")
data class Budget(val id: Long, val month: YearMonth, val accountId: Long?, val categoryId: Long?, val limit: Long)
data class Goal(val id: Long, val name: String, val target: Long, val deadline: LocalDate?, val archived: Boolean = false)
data class GoalContribution(val id: Long, val goalId: Long, val amount: Long, val date: LocalDate)
enum class BudgetStatus { OK, WARN, OVER }
data class BudgetUsage(val spent: Long, val limit: Long, val remaining: Long, val ratio: Double, val status: BudgetStatus)
data class GoalProgress(val saved: Long, val remaining: Long, val ratio: Double, val done: Boolean, val perMonth: Long?)

fun parseVndInput(text: String): Long?
fun formatVnd(amount: Long): String
fun monthsBetween(from: YearMonth, to: YearMonth): Int          // 2026-11 -> 2027-01 = 2
fun accountBalance(account: Account, txns: List<Txn>): Long
fun budgetUsage(budget: Budget, txns: List<Txn>): BudgetUsage
fun goalProgress(goal: Goal, contributions: List<GoalContribution>, today: LocalDate): GoalProgress
fun validateTxn(txn: Txn, accounts: List<Account>, categories: List<Category>): List<String>  // empty = valid
```

(`ratio` is a display value only; it is never stored or added up.)

- [ ] **Step 1: Failing tests — Money.** `parseVndInput`: `"1250000"`→1250000, `"1.250.000"`→1250000, `"1,250,000"`→1250000, `"1 250 000"`→1250000, `"50k"`→50000, `"50K"`→50000, `""`→null, `"abc"`→null, `"0"`→null, `"-5"`→null, `"1.5"`→null, `"12.50.000"`→null. `formatVnd(1250000)`→`"1.250.000 ₫"`, `formatVnd(0)`→`"0 ₫"`, `formatVnd(-50000)`→`"-50.000 ₫"`.
- [ ] **Step 2:** Run `.\gradlew :core:test`. Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement `Money.kt`. Separators `.` `,` space are valid only between groups of exactly 3 digits.
- [ ] **Step 4: Failing tests — Months.** `monthsBetween(2026-11, 2027-01)`=2; same month = 0; earlier `to` = negative.
- [ ] **Step 5: Failing tests — Balance.** A opening 1000000; txns: EXPENSE 200000 from A, INCOME 500000 to A, TRANSFER 300000 A→B. `accountBalance(A)`=1000000; `accountBalance(B, opening 0)`=300000.
- [ ] **Step 6: Failing tests — Budgets.** Budget `(month=2026-11, accountId=1, categoryId=null, limit=1000000)`:
  - EXPENSEs on account 1 in 2026-11 totalling 500000 → `spent=500000, remaining=500000, ratio=0.5, OK`
  - spent 800000 → `WARN`; spent 1000000 → `WARN`; spent 1000001 → `OVER`, `remaining=-1`
  - EXPENSE dated 2026-10-31 → not counted; TRANSFER or INCOME in 2026-11 → not counted
  - EXPENSE on account 2 → not counted; with `accountId=null` → counted
  - `categoryId=5` → counts only EXPENSEs with `categoryId=5`
- [ ] **Step 7: Failing tests — Goals.** Target 12000000, deadline 2027-04-30, contributions 2000000 + 1000000, today 2026-11-08 → `saved=3000000, remaining=9000000, ratio=0.25, done=false, perMonth=1500000`. Deadline passed, not done → `perMonth = remaining`. No deadline → `perMonth=null`. Saved ≥ target → `done=true, remaining=0, perMonth=null`.
- [ ] **Step 8: Failing tests — Validate.** Non-empty result for: amount ≤ 0; account missing or archived; TRANSFER with `toAccountId` null or equal to `accountId`; TRANSFER with a category; EXPENSE/INCOME without category; category kind not matching txn kind; archived category. Valid EXPENSE → empty. Messages in Vietnamese; tests check only emptiness.
- [ ] **Step 9:** Implement `Months.kt`, `Balance.kt`, `Budgets.kt`, `Goals.kt`, `Validate.kt`. `perMonth = ceil(remaining / max(1, monthsBetween(YearMonth.from(today), YearMonth.from(deadline)) + 1))` in `Long` math.
- [ ] **Step 10:** Run `.\gradlew :core:test`. Expected: all pass. Commit `feat(core): money, balance, budget, goal logic`.

### Task A3: Storage + accounts + add transaction (first vertical slice)

**Interfaces:**
- Consumes: all of A2.
- Produces (`data/Repo.kt`, suspend functions and `Flow`s returning `:core` types):
  `accounts(includeArchived: Boolean = false): Flow<List<Account>>`, `saveAccount(a: Account): Long`, `archiveAccount(id: Long)`, `deleteAccount(id: Long): DeleteResult`, `categories(kind: CategoryKind? = null, includeArchived: Boolean = false): Flow<List<Category>>`, `saveCategory`, `archiveCategory`, `deleteCategory(id): DeleteResult`, `txns(month: YearMonth? = null, accountId: Long? = null, categoryId: Long? = null): Flow<List<Txn>>`, `saveTxn(t: Txn): Long`, `deleteTxn(id)`, `budgets(month: YearMonth): Flow<List<Budget>>`, `saveBudget`, `deleteBudget`, `goals(): Flow<List<Goal>>`, `saveGoal`, `archiveGoal`, `contributions(goalId): Flow<List<GoalContribution>>`, `addContribution(c)`, `categoryUsage(kind): Map<Long, Int>`.
  `enum class DeleteResult { DELETED, HAS_TRANSACTIONS }`. `id = 0` in `save*` means insert.
- Room: tables `accounts, categories, txns, budgets, goals, goal_contributions`; amounts `Long`; dates stored as ISO text; DB version 1 with `exportSchema = true` into `app/schemas/`; default categories from the spec seeded on create. `Repo` is built once in a small `AppContainer` held by the `Application` class.

- [ ] **Step 1:** Entities, DAOs, `AppDatabase`, `Repo`. `deleteAccount`/`deleteCategory` return `HAS_TRANSACTIONS` without deleting when rows reference them.
- [ ] **Step 2:** Accounts tab: list with balance (`accountBalance`) and total; add/edit sheet (name, type, opening balance via `parseVndInput`); archive. Delete on an account with transactions shows "Tài khoản đã có giao dịch, chỉ có thể lưu trữ".
- [ ] **Step 3:** Add-transaction screen, opened from a "+" button on every tab: type switch with three full labels "Chi tiêu" / "Thu nhập" / "Chuyển khoản", each with its own colour and icon; big number pad; account picker (and "đến tài khoản" for transfer); category grid sorted by `categoryUsage`; date (default today); note. Save runs `validateTxn`; errors shown next to the field.
- [ ] **Step 4:** Transactions tab: current month, newest first, grouped by day; tap to edit; long-press to delete with confirm.
- [ ] **Step 5:** Run `.\gradlew :core:test assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 6:** Report to the director: ready for milestone M1 (director installs the debug APK on the user's phone). Commit `feat: storage, accounts, add transaction`.
- [ ] **Step 7:** Write `specs/B1.txt` … `specs/B4.txt` from the patterns built here. Send the director a note.

---

## Phase B — screens (agy_director, from head_agent's specs)

Each B task consumes A2 + A3 interfaces only. Check: `.\gradlew :core:test assembleDebug`, plus the check listed. One commit per task.

### Task B1: Categories screen
Opened from the Accounts tab header. List by kind; add, rename, change icon, archive. Deleting a used category is refused with a message. Check: archived category hidden in the category grid, still shown on old transactions.

### Task B2: Budgets tab
Month switcher (‹ Tháng 11/2026 ›). Budgets of the month with `budgetUsage`; progress bar green/amber/red; add/edit (account or "Tất cả", category or "Tất cả", limit); "Sao chép từ tháng trước". Check: budget at exactly the limit shows amber.

### Task B3: Goals tab
List with progress bar and "Cần để dành 1.500.000 ₫/tháng". Detail screen: contributions, add contribution, edit, archive. Check: reached goal shows done state, no per-month line.

### Task B4: Transaction filters and search
Transactions tab: month switcher, filter by account and category, month totals (income, expense; transfers excluded), search in notes. Check: transfer not in totals.

---

## Phase C — home, backup, release

### Task C1: Home dashboard (head_agent)
"Còn được tiêu tháng này" = sum of `remaining` of this month's budgets with `categoryId == null` (none: show "Tạo ngân sách" button). Then budgets in `WARN`/`OVER`, account balances and total, the 3 nearest goals. Check against hand math with test data.

### Task C2: Backup export/import (agy_director)
Settings screen: export all tables to one JSON file through the Storage Access Framework (`CreateDocument`); import (`OpenDocument`) replaces all data after a confirm dialog. `validateBackup(json: String): List<String>` lives in `:core` with tests (wrong version, missing table, non-integer amount). Uses only `org.json` from the Android platform, no new library. Check: export, clear data, import gives the same balances.

### Task C3: Release build (director)
Create a release signing key in `D:\android-tools\keys\` (never committed). Build `.\gradlew assembleRelease`, check size < 10 MB, install on the user's phone. From here the release app holds real data; the `.debug` app stays test data.

---

## Phase D — widgets (head_agent first, then agy)

Separate plan written after C3, with the user, after testing the app on the phone. Fixed so far: Jetpack Glance; a 2×2 widget "Còn được tiêu" with a "+" button that opens the add screen; a 4×2 widget with the top budgets; refreshed by `updateAll` after each save, no periodic jobs; checked on the user's HyperOS 3.0 phone (sizes, rounded corners, dark mode, launcher behaviour).

---

## Phase U — UI redesign (after M1 feedback, 2026-11-08)

User decisions: bottom navigation is the director's call; green tone; calculator keys on the number pad; spending donut chart stays in phase 2. Layout rules: `docs/ui-layout.md` (binding for every UI task).

### Task U1: Design foundation + Home (head_agent)
- Theme tokens from `docs/ui-layout.md` §3 (dark + light), no dynamic colour.
- Shared components in `ui/components/`: `ChiLiTopBar`, `SectionCard`, `EmptyState`, `SectionHeaderBand`, `FullScreenForm` scaffold (✕ / title / ✓), `OneLineText` (maxLines 1, no wrap, step auto-size, min 10sp).
- Navigation: bottom bar Trang chủ · Giao dịch · Cài đặt; Trang chủ = TabRow + pager (Tài khoản | Ngân sách & Mục tiêu) as in §1; Cài đặt lists Danh mục and Sao lưu (existing screens).
- Account colour: `Account.colorKey: String` (one of the 8 keys in §3, default by type) in `:core`; Room v1 -> v2 migration adding the column with a default; backup format accepts files without the field (defaults) and exports it; tests in `:core` for the backup default.
- Account tile grid on the Tài khoản page (§2).
- `:core` calculator, TDD: `fun evalAmountExpr(text: String): Long?` — digits with the same grouping/`k` rules as `parseVndInput`, operators `+ − × ÷` (also `- * /`), × ÷ before + −, left to right, ÷ rounds half up to whole đồng, result must be > 0 else null, malformed -> null. Tests: `"50k+30k"`→80000, `"100000-25000"`→75000, `"12000*3"`→36000, `"100000/3"`→33333, `"200000/3"`→66667, `"10+2*3"`→16, `"5-10"`→null, `"1.250.000+50k"`→1300000, `"+5"`→null, `"5+"`→null, `"5/0"`→null.
- Then write specs U2–U4 for agy and send the director a note.

### Task U2: Add transaction redesign (agy, spec by head_agent) — §2 "Add transaction", uses `evalAmountExpr`.
### Task U3: Full-screen forms (agy) — account (with colour picker), budget, goal, category; contribution stays a sheet.
### Task U4: Top bars + empty states everywhere + Cài đặt screen polish (agy).

### Task U1b: Currencies in :core, DB v2, backup v2 (head_agent, TDD) — spec section "Currencies"

Replaces U1 parts 3 and 5. One Room migration v1 -> v2 covers colour and currency.

**Interfaces — Produces (package `com.superduperlover.finance.core`):**

```kotlin
enum class Currency(val decimals: Int) { VND(0), TWD(0), USD(2) }   // main = TWD
// Account += currency: Currency, colorKey: String
// Txn += toAmount: Long?   (TRANSFER only; amount received in the target account's currency; null = same as amount)
// Budget += currency: Currency
// Goal += currency: Currency
fun parseAmount(text: String, currency: Currency): Long?          // minor units; replaces parseVndInput
fun formatMoney(amount: Long, currency: Currency): String         // replaces formatVnd
fun evalAmountExpr(text: String, currency: Currency): Long?       // + − × ÷ (also - * /), × ÷ first, ÷ rounds half up to the minor unit, result > 0
fun accountBalance(account: Account, txns: List<Txn>): Long       // transfer in adds toAmount ?: amount
fun budgetUsage(budget: Budget, txns: List<Txn>, accounts: List<Account>): BudgetUsage   // accountId null -> only accounts with budget.currency
fun txnTotals(txns: List<Txn>, accounts: List<Account>): Map<Currency, TxnTotals>
fun balancesByCurrency(accounts: List<Account>, txns: List<Txn>): Map<Currency, Long>
// validateTxn: TRANSFER between different currencies needs toAmount > 0; same currency -> toAmount must be null or == amount
```

Keep `parseVndInput`/`formatVnd` as thin wrappers (VND) until U2-U4 move the screens, then delete them.

**Tests (add to the existing ones):**
- `parseAmount`: VND keeps all old cases. TWD `"1,250"`→1250, `"1 250"`→1250, `"5k"`→5000, `"12.5"`→null. USD `"12.50"`→1250, `"12.5"`→1250, `"12"`→1200, `"1,250.75"`→125075, `"1.5k"`→150000, `"12.345"`→null, `"0.00"`→null.
- `formatMoney`: `(1250000, VND)`→`"1.250.000 ₫"`, `(1250, TWD)`→`"NT$1,250"`, `(125075, USD)`→`"$1,250.75"`, `(5, USD)`→`"$0.05"`, `(-50, TWD)`→`"-NT$50"`.
- `evalAmountExpr`: VND `"50k+30k"`→80000, `"100000/3"`→33333, `"200000/3"`→66667, `"10+2*3"`→16, `"5-10"`→null, `"+5"`→null, `"5+"`→null, `"5/0"`→null, `"1.250.000+50k"`→1300000; USD `"10.50+2.25"`→1275, `"10/3"`→333; TWD `"100/3"`→33.
- Balance: TRANSFER 1000000 VND from A (VND) to B (TWD) with `toAmount=1250` → A −1000000, B +1250.
- Budget: `accountId=null, currency=TWD` ignores EXPENSEs on VND accounts.
- `balancesByCurrency` / `txnTotals`: one entry per currency in use; transfers never in totals.
- Validate: cross-currency TRANSFER without `toAmount` → error.
- Backup: version 2 exports `currency`, `colorKey`, `toAmount`; a version 1 file imports with currency VND, default colour, `toAmount` null.

**DB:** v2 migration adds `accounts.currency` (default `'VND'` for existing rows), `accounts.colorKey`, `txns.toAmount` (null), `budgets.currency` (`'VND'`), `goals.currency` (`'VND'`). New rows default to TWD. Commit `app/schemas/.../2.json`. The user's debug data on the phone must survive.

## Phase V — after first phone test (spec section "After first phone test")

### Task V1: Budget periods + rollover + goal completion in :core, DB v3, backup v3 (head_agent, TDD)

**Interfaces — Produces:**

```kotlin
enum class BudgetPeriod { DAY, WEEK, MONTH, QUARTER, YEAR }
// Budget: replaces `month` with  period: BudgetPeriod, startDate: LocalDate, rollover: Boolean (keeps id, accountId, categoryId, currency, limit)
data class PeriodRange(val start: LocalDate, val endInclusive: LocalDate)
fun periodRange(period: BudgetPeriod, startDate: LocalDate, index: Int): PeriodRange      // index 0 = first period
fun periodIndexAt(period: BudgetPeriod, startDate: LocalDate, day: LocalDate): Int?       // null before startDate
// BudgetUsage += range: PeriodRange, carried: Long (0 when rollover off), effectiveLimit = limit + carried
fun budgetUsage(budget: Budget, txns: List<Txn>, accounts: List<Account>, today: LocalDate): BudgetUsage
// Goal += completedAt: LocalDate?
fun shouldAutoComplete(goal: Goal, savedBefore: Long, savedAfter: Long): Boolean          // true only when crossing target and completedAt == null
```

**Tests (values):**
- MONTH from 2026-11-25: index 0 = 25/11–24/12, index 1 = 25/12–24/01; from 2026-01-31: index 0 = 31/01–27/02, index 1 = 28/02–30/03 (2026 not leap), index 2 = 31/03–29/04 (index 3 starts 30/04).
- WEEK from 2026-11-05 (Thu): index 0 = 05/11–11/11, index 1 = 12/11–18/11. DAY index 3 from 2026-11-05 = 08/11–08/11. QUARTER from 2026-11-15: index 0 = 15/11–14/02. YEAR from 2026-03-01: index 0 = 01/03/2026–28/02/2027.
- `periodIndexAt` before startDate → null; on endInclusive → same index; next day → index + 1.
- Rollover: MONTH limit 1000, spent 700 in period 0, 1200 in period 1, today in period 2 → period 1 carried +300 (limit 1300, remaining 100), period 2 carried +100 (effective 1100). Overspend: period 0 spent 1500 → period 1 carried −500. Rollover off → carried 0 always.
- Status rules unchanged, but against `effectiveLimit` (WARN at ≥ 80 %, OVER above; effectiveLimit ≤ 0 with any spend → OVER).
- `shouldAutoComplete`: 900→1000 of 1000 → true; 1000→1100 when completedAt set → false; 500→800 → false.

**DB v3:** budgets: drop `month`, add `period` ('MONTH'), `startDate` (first day of the old month), `rollover` (0); goals add `completedAt` (null). Manual Migration(2,3) for the budgets table (rebuild), schema 3.json committed; existing rows survive. **Backup v3**; v1/v2 files import (old month budgets -> MONTH from the 1st, rollover off).
**Repo:** `addContribution` sets `completedAt = today` when `shouldAutoComplete`; `completeGoal(id, date)`, `reopenGoal(id)`.
Then write specs V2–V4 for agy.

### Task V2 (agy): Budgets UI — period picker (Ngày/Tuần/Tháng/Quý/Năm), start date, rollover switch with one-line explanation, card shows date range + carried amount; budgets page lists current periods (month switcher removed).
### Task V3 (agy): Goals UI — sections Đang thực hiện / Đã hoàn thành, "Đánh dấu hoàn thành" and "Mở lại" actions, completion date.
### Task V4 (agy): Light mode setting (SharedPreferences, no new library) + Home summary card with currency chips; check every screen in light mode against ui-layout §3/§7.

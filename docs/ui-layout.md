# ChiLi — UI layout guide

Owner: director. Every agent that builds or changes UI follows this file. A change to the layout rules goes through the director first.

Sources: user's reference screenshots (a finance app the user likes; we copy its structure, never its brand, colours or name), the `ui-ux-pro-max` skill (finance palette, UX rules: 48dp touch targets, 8dp gaps, empty states with action, visible active nav state), and the user's phone (Redmi, 1080x2400, 450 dpi, font_scale 1.25, HyperOS serif system font, dark mode).

Rejected from the skill output, on purpose: glassmorphism (blur is costly on Android and hurts contrast) and bundled Google Fonts (APK weight; the user's system font is respected).

## 1. Navigation

- Bottom bar, 3 tabs: **Trang chủ** · **Giao dịch** · **Cài đặt**. Icons + labels, labels one line (see 4).
- **Trang chủ** has two top tabs (TabRow, swipeable pager): **Tài khoản** | **Ngân sách & Mục tiêu**.
  - Tài khoản page, top to bottom: summary card (Tổng số dư; Còn được tiêu tháng này; Thu nhập / Chi tiêu tháng này), account tile grid, "Ngân sách cần chú ý" card (WARN/OVER budgets only).
  - Ngân sách & Mục tiêu page: month switcher, budget list, then goal list. Each list has its own empty state.
- **Giao dịch**: current list with filters and search (as today).
- **Cài đặt**: Danh mục, Sao lưu và khôi phục. Later: Giao diện.
- "+" FAB on Trang chủ and Giao dịch opens Add transaction. Not on Cài đặt.
- Every screen has a top app bar with its title. Sub-screens have a back arrow. Back always goes to the previous screen.

## 2. Screen patterns

**Account tiles.** Grid, 3 columns on this phone (adaptive: min tile width 104dp). Tile = rounded 16dp, filled with the account colour (darkened for contrast), icon by account type + name (1 line) + balance (1 line, auto-size). Last tile: outlined "+ Thêm tài khoản". Tap a tile: Giao dịch tab filtered to that account. Long-press: edit form.

**Cards.** Surface colour, 16dp corners, 16dp inner padding, 12dp between cards, no shadows on dark (tonal elevation only). Card title = titleMedium; optional "›" action at the right of the title row.

**Empty state.** Inside a card: 56dp circle tinted with the section colour holding an icon, title (titleMedium), one sentence (bodyMedium, muted), one filled-tonal button. Example: "Hãy tạo ngân sách đầu tiên" / "Đặt hạn mức để biết mỗi tháng còn được tiêu bao nhiêu." / [Tạo ngân sách].

**Full-screen form** (account, budget, goal, category). Top bar: ✕ left (discard, confirm if changed), title, ✓ right (save; disabled while invalid). Fields grouped under section header bands (surfaceVariant strip, labelLarge, 16dp horizontal padding). Field = label above, value below, error text right under the field. Short single-field inputs (goal contribution, rename) stay as bottom sheets.

**Add transaction** (one screen, speed first):
1. Top bar: ✕ · ✓.
2. Type tabs, full width, equal thirds: Chi tiêu · Thu nhập · Chuyển khoản. One line each, auto-size; never wrap.
3. Amount header, tinted with the type colour (container tone, not full saturation): sign + amount (displayMedium, auto-size down to fit one line) + "₫". Below the number, the expression being typed when it has an operator (e.g. `50k+30k`).
4. Row of two pickers: Tài khoản | Danh mục (transfer: Từ tài khoản | Đến tài khoản). Tap opens a bottom sheet list.
5. One horizontal row of the 8 most used categories as chips (most used first) + "Tất cả" chip. Selecting a chip sets the category. Hidden for transfer.
6. Compact row: date chip (default Hôm nay) + note field.
7. Number pad fixed at the bottom: 3 digit columns (7 8 9 / 4 5 6 / 1 2 3 / 000 0 ⌫) + 1 operator column (÷ × − + =). Keys ≥ 56dp tall. "=" shows the result; ✓ evaluates first, then saves.

## 3. Colour (dark first; light mirrors the roles)

| Role | Dark | Light |
|---|---|---|
| Background | `#0F1512` | `#F6FAF7` |
| Surface (cards) | `#18211C` | `#FFFFFF` |
| Surface variant (bands, inputs) | `#212C26` | `#E7EFEA` |
| Outline | `#FFFFFF14` | `#D5E0D9` |
| Primary (brand green) | `#10B981` | `#047857` |
| On primary | `#04241A` | `#FFFFFF` |
| Primary container | `#0B3D2E` | `#D1FAE5` |
| Text | `#E8F0EC` | `#0F1F17` |
| Muted text | `#94A3B8` | `#4B5B53` |
| Income (Thu nhập) | `#34D399` | `#059669` |
| Expense (Chi tiêu) | `#F87171` | `#DC2626` |
| Transfer (Chuyển khoản) | `#94A3B8` | `#475569` |
| Budget WARN | `#FBBF24` | `#B45309` |
| Budget OVER | = Expense | = Expense |

- Defined once as tokens in the theme; no raw hex in screens.
- Never colour alone: amounts carry a sign (+/−), budget status carries a word (Ổn / Sắp hết / Vượt mức).
- Account colours (user picks one per account): emerald `#10B981`, teal `#14B8A6`, sky `#0EA5E9`, indigo `#6366F1`, violet `#8B5CF6`, rose `#F43F5E`, amber `#F59E0B`, slate `#64748B`. Tile text must reach 4.5:1 contrast on the tile.
- No dynamic colour (Material You): ChiLi keeps its own green.

## 4. Text and large fonts

- System font only. Material 3 type scale. Body ≥ 14sp, never below 12sp.
- The user's phone runs font_scale 1.25. Check every screen at 1.0 and 1.25.
- Any label that must stay on one line (nav, tabs, chips, tiles, buttons, amounts): `maxLines = 1`, `softWrap = false`, auto-size (step-based, min 10sp). Never let a label wrap into a clipped second line.
- Amounts always via `formatVnd`.

## 5. Spacing, touch, motion

- 4dp grid. Screen side padding 16dp. Card padding 16dp. 12dp between cards, 8dp minimum between touch targets.
- Touch targets ≥ 48dp. Number pad keys ≥ 56dp.
- Respect system bars and the keyboard (`navigationBarsPadding`, `imePadding`).
- Motion 150–250 ms, standard easing. Tab/pager slide, sheet slide. No decorative animation.

## 6. Icons

- Material icons from `material-icons-core`. Do not add `material-icons-extended`.
- Extra icons (account types, empty states, categories): single Material Symbols SVGs converted to vector drawables in `app/src/main/res/drawable/` (Apache 2.0). No emoji as icons.

## 7. Before calling a UI task done

- [ ] Looks right at font_scale 1.0 and 1.25; no clipped or wrapped single-line labels.
- [ ] Colours come from theme tokens; contrast ≥ 4.5:1 for text.
- [ ] Every list has an empty state with an action.
- [ ] Touch targets ≥ 48dp; back works from every screen.
- [ ] Release APK size reported (budget: < 10 MB).

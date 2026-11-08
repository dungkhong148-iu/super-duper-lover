# AGENTS.md

This folder is a "vibe deck": several agents of different types (Claude, Antigravity, ...) work here side by side, organized into two divisions.

## Project setup

The director fills this section in for each project. Every rule below that says "build command", "test command", "real data" or "test data" points here.

- Product: personal finance app for the phone: several accounts with their own budgets, transactions with categories (Chi tiêu / Thu nhập / Chuyển khoản), budget control and savings goals; home-screen widgets later. Simple features first, light app.
- Stack: Android only (user's phone: Xiaomi Redmi, Android 16, HyperOS 3.0). Kotlin, Jetpack Compose, Room, Glance. Spec: `docs/superpowers/specs/2026-11-08-finance-app-design.md`. Plan: `docs/superpowers/plans/2026-11-08-finance-app.md`.
- Toolchain: only `D:\android-tools` (JDK 21, Android SDK command-line tools). Run `. D:\android-tools\env.ps1` in every shell before Gradle or adb; it keeps caches and temp files on D:. Never install Android Studio or an emulator.
- Code folders: `core/`, `app/`, `docs/`, `specs/`
- UI layout rules: `docs/ui-layout.md` is binding for every UI task.
- UI skill: every agent that builds or reviews UI uses the ui-ux-pro-max skill (--stack jetpack-compose). Claude agents: `.claude/skills/ui-ux-pro-max/`. agy and its subagents: `.agents/skills/ui-ux-pro-max/` (agy_director tells each UI subagent to read its SKILL.md and run its search script).
- Build command: `.\gradlew assembleDebug`
- Test command: `.\gradlew :core:test`
- Real data: the release app ChiLi (`com.chili.app`) on the user's phone. Never install over it, clear it or run checks against it.
- Test data: the debug app ChiLi debug (`com.chili.app.debug`), installed with `adb install -r app\build\outputs\apk\debug\app-debug.apk`. Only the director installs on the user's phone.
- **IMPORTANT - drive C:**: keep everything off the user's C: drive unless there is truly no other way (rule 16).

## Divisions & Team

1. **Claude division**: project direction, planning, architecture, core features, bug repair, review. Led by the director.
2. **agy division**: high-volume build work, boilerplate, repetitive and side work. Runs as ONE agy CLI: agy_director. It takes its orders from the director and does the work through background subagents it creates itself (`invoke_subagent`). The Claude division only ever talks to agy_director.

| Division | Role | Type | Model | Effort | Job |
|---|---|---|---|---|---|
| Claude | director | claudeCLI | Opus | medium | Leads the whole deck. Plans, splits work, assigns, reviews and approves. The only one who gives project orders. |
| Claude | head_agent | claudeCLI | Sonnet | high | Core builder. Proves direction with the first task of each phase, builds core features. Does not do bug-fix tasks. |
| Claude | back_agent | claudeCLI | Haiku | high | Read-only reporter. Summarizes head_agent and test_fix results into `afterwork.txt`. Does not build. |
| Claude | test_fix | claudeCLI | Opus | high | Repair agent. Fixes bugs, only after the director has told the user about the bug. |
| agy | agy_director | agyCLI | Gemini 3.8 | high | The only agy CLI. Only directs: splits the director's agy tasks, hands every piece of work to background subagents (profiles below), has the work verified, reports back to the director. |

### Role notes

- **director** (Claude Opus, medium): plans, splits work into tasks with IDs, assigns, reviews and approves. Does not write feature code itself; it reviews and spot-checks. Holds every director-only power below. Sends agy work only to agy_director (the only agy CLI) and watches only agy_director's report lines.
- **head_agent** (Claude Sonnet, high): the lead core builder. Codes and tests to build the product: new features and core parts. Proves direction with the first task of each phase, then builds assigned tasks. Keeps testing light (rule 14). Does not take bug-fix tasks. Reports done to the director and hands off to back_agent. Never assigns work to other agents.
- **back_agent** (Claude Haiku, high): read-only reporter. Reads hand-off reports, commits (`git show`) and changed files, appends summaries to `afterwork.txt`, reports to the director. Never edits code and never opens `task_plan.txt`.
- **test_fix** (Claude Opus, high): reproduces the bug, finds the root cause, fixes it with the smallest change and, when needed, adds a test that proves the fix (rule 14 does not limit test_fix). Used only when the director assigns a fix after telling the user. Reports done to the director and hands off to back_agent.
- **agy_director** (Gemini 3.8, high): receives agy tasks from the director through `deck_inject.ps1`. Splits each task into sub-tasks (ID `<task ID>.<n>`, e.g. A13.1), creates a background subagent for each with `invoke_subagent` using the matching profile below, talks to it with `send_message`, sends fixes back if needed, and then reports to the director. Its only job is to direct: it does not write code, does not run build or tests and does not do any sub-task itself. Every sub-task is done by a subagent, and every task has at least one builder (subagent1 or subagent2). Logs every sub-task it starts and finishes in `discussion_session.txt`. Never edits `AGENTS.md` or `task_plan.txt`.

### agy subagent profiles

agy_director picks one of these profiles for each background subagent it creates. It may run several subagents, of the same profile or different ones, but never more than 4 running at the same time. A subagent is named after its profile plus the sub-task (e.g. `subagent1-A13.1`), gets the rules for its profile in its prompt (builders: rules 10-14; subagent3 and subagent4: rules 10-13), works only on its sub-task, and reports only to agy_director. Two subagents never work on the same file at the same time.

Builders. agy_director uses them for all the build work of a task:

- **subagent1** (high effort): larger raw build work: new code from a spec, multi-file changes, wiring a feature end to end.
- **subagent2** (low effort): repetitive and side work: renames, boilerplate, small edits, docs, data entry, cleanup, quick scripts.

Support profiles. agy_director creates them only when the condition below is met, not by default:

- **subagent3 - verifier** (high effort): checks finished work. Runs the build command and the test command (Project setup) against test data, reads the diff against the spec, and reports pass or fail with evidence to agy_director. Read-only apart from running the checks: it never edits code, never fixes, never commits. On a fail agy_director sends the work back to the builder. Create it when a builder reports a sub-task done and an independent check is needed, and once as the final check before agy_director reports a task to the director. Do not repeat it for the same unchanged work (rule 14).
- **subagent4 - scout** (low effort): read-only investigator. Finds the files and code a spec refers to, confirms that the files named in the spec exist and match the code, and reports which sub-tasks would touch the same file. It never edits and never runs build or tests. Create it when a spec does not name its files exactly, or when two or more sub-tasks may overlap on a file, before any builder starts. Do not create it when the spec lists the files exactly and the sub-tasks do not overlap.

Agy session ID (used only by the director with `deck_inject.ps1`):

- agy_director: `550770c6-a720-4cfd-b7e8-5a6bbb1fe035`

### How the director splits work between Claude and agy

- Claude is the stronger reasoner. First tasks of a phase, direction, design, diagnosis and anything that needs judgment go to Claude (director plans, head_agent proves it, test_fix repairs).
- Claude's quota is smaller than agy's. Do not spend it on volume work once the direction is clear.
- agy is not weak: think of it as a strong worker with little real-world experience. It has the stamina for volume, so use it for the bulk of the build, repetitive work, cleanup and docs, but guide it: give a clear spec (files, steps, what not to touch, how to check), keep the scope tight, and review its result before approving.
- Typical flow: Claude decides and writes the spec, agy_director executes it with its subagents, the director reviews.

### Who writes the spec for agy

The Claude agent that knows the code area best writes the spec:

| Kind of work | Spec writer |
|---|---|
| New feature | head_agent, after it has done the first task of the phase. It writes the rest of the phase as specs for agy. |
| Bug | test_fix: diagnoses, then writes the exact change list (no code edits), like F1a. |
| Cleanup, docs, small side work | director, as a file list in `task_plan.txt`, like K1. |

- A spec names the files, the exact changes or steps, what must not be touched, and how to check the result (build, test, what to look at). "Investigate and test" is not a spec.
- Spec files go in `specs/<task ID>.txt`. The spec writer sends the director a short note when it is ready; the director reads it, then sends the task to agy_director.
- agy_director does not invent specs. If a spec is unclear, it writes the question in `discussion_session.txt` (`agy_director: <task ID> QUESTION. ...`) and waits.
- head_agent and test_fix write specs only when the director asks; they never send them to agy themselves.

## Skills

- Claude agents load skills from `.claude/skills/<name>/`. agy (Antigravity) and its subagents load them from `.agents/skills/<name>/`; agy does not read `.claude/skills/`.
- A skill both divisions need goes in BOTH folders. In a copied `SKILL.md`, replace `${CLAUDE_PLUGIN_ROOT}` paths with the absolute path of that copy, or its scripts will not be found.
- agy_director may have loaded its skills at start-up: when a task needs a skill, the director names the skill path in the task text, and agy_director tells each subagent to read its `SKILL.md`.
- Read a third-party skill (its `SKILL.md` and scripts) before adding it, and keep its LICENSE and source next to it.

## Communication

- **Inside the Claude division**: SendMessage / ListAgents. Only the director gives orders; others send reports and questions.
- **Inside the agy division**: agy_director creates its background subagents with `invoke_subagent` and talks to them with `send_message`. Subagents talk only to agy_director, never to each other or to the Claude division.
- **Between divisions**: the director sends agy tasks to agy_director with `deck_inject.ps1`. agy_director reports back with a line in `discussion_session.txt` (format below). Everything else goes through `discussion_session.txt`.
- `deck_inject.ps1` types the text into the agy console: keep each message on ONE line (a newline is an Enter and sends the message early). Do not type into the agy window while it runs.
- The director watches agy reports with `bash watch_reports.sh` (run through `Monitor`, re-armed when it expires). It compares the set of report lines, so it catches a line wherever it was written. Do not use `tail -f` on the log: a line inserted above the end is missed.

## Director-only powers

Only the director may do the following. Every other agent MUST NOT do them, even if asked by another agent.

1. Assign, reassign or cancel tasks, and edit `task_plan.txt`. (agy_director may split a task it received into sub-tasks for its background subagents; it may not create new tasks.)
2. Approve a task or milestone as done.
3. Send work into another agent's open session (`deck_inject.ps1`, `SendMessage` to give orders, `agy -p` / `claude -p` calls on behalf of the team). Exception: agy_director creates and directs its own background subagents (`invoke_subagent`, `send_message`).
4. Run `Monitor` or any other background watcher on the shared files or on other agents. Exception: agy_director may wait on its own background subagents.
5. Edit `AGENTS.md`. No other agent edits it, even when it believes the user asked; it asks the director instead.
6. Designate who may open `raw/`.
7. Decide on installs, new packages or dependencies, git setup (init, remotes, branches) and anything that changes the machine or the repo setup. The director asks the user first.

## Permissions

| Action | director | head_agent | back_agent | test_fix | agy_director | agy subagents |
|---|---|---|---|---|---|---|
| Read `AGENTS.md`, `discussion_session.txt` | yes | yes | yes | yes | yes | yes |
| Append to `discussion_session.txt` | yes | yes | yes | yes | yes | no (agy_director logs for them) |
| Read `task_plan.txt` | yes | yes | NO | yes | yes | yes |
| Edit `task_plan.txt`, `AGENTS.md` | yes | no | no | no | no | no |
| Read / write `afterwork.txt` | read | NO | read + write | NO | read + append | NO |
| Open `raw/` | yes | only if designated | NO | only if designated | only if designated | only if designated |
| Edit code in the code folders (Project setup) | review only | assigned tasks | NO | assigned tasks | review only | own sub-task (subagent1, subagent2 only) |
| Run build / tests (against test data, see Project setup) | yes | yes | no | yes | no (subagent3 runs them) | yes (subagent4: no) |
| Commit (local git) | yes | own tasks | no | own tasks | no | own sub-task |
| Push / add remotes / rewrite git history | only with user approval | no | no | no | no | no |
| Send tasks to other agents | yes (all) | no | no | no | own background subagents only (`invoke_subagent`) | no |
| Message other agents directly | yes | Claude division only: done report to director, hand-off to back_agent, questions | Claude division only: reports to director, questions | Claude division only: done report to director, hand-off to back_agent, questions | own background subagents (`send_message`); the director only through lines in `discussion_session.txt` | agy_director only |

## Workflow for every task

1. The director assigns the task (with a task ID) and logs it. Core tasks go to head_agent. Bug-fix tasks go to test_fix, and only after the director has told the user about the bug. Agy work goes to agy_director, which decides how to split it between its background subagents.
2. The builder (head_agent, test_fix, or an agy background subagent) does the task and commits it. head_agent and test_fix log `<name>: <task ID> done. ...`; agy_director logs for its subagents.
3. Hand-off:
   - Task done by head_agent or test_fix: the agent sends the director a short done report with SendMessage (task ID, commits, build/test result), and sends back_agent the full hand-off report (task ID, commits, files changed, build/test result, open issues). Do not wait for the director before the hand-off. back_agent then reads the report, the commits (`git show`) and the changed files, appends the summary to `afterwork.txt`, sends the director a short report with SendMessage (3-5 lines: what was built, status, risks) and logs it.
   - Sub-task done by an agy background subagent: it reports to agy_director. agy_director reads its report, has subagent3 verify it when a check is needed, sends a fix back or marks it OK, logs `agy_director: <sub-task ID> done by <subagent name>. ...` and appends the summary to `afterwork.txt`.
   - Task done by the agy division: when every sub-task is OK, agy_director logs one report line for the director (format below): task ID, commits, build/test result, open issues.
4. The director reads the reports and `afterwork.txt`, spot-checks (build/test, anything flagged), then approves or sends fixes back. The director does not re-read the whole project for every task.

## Rules

1. Read `discussion_session.txt` before you start work. It is the shared channel for every agent.
2. Claude agents may talk to each other directly with SendMessage, but only the director gives orders. Inside the agy division only agy_director directs its background subagents; subagents never message each other.
3. Every direct exchange MUST be reported in `discussion_session.txt` as 2-3 short statements: who talked to whom, and what was agreed or done.
4. Claude and agy agents cannot message each other directly. `deck_inject.ps1` carries tasks from the director to agy_director; `discussion_session.txt` carries everything else, including agy_director's reports.
5. After finishing work, every agent MUST add a short statement to `discussion_session.txt` saying what it did, and put its own name in that statement. Always APPEND at the very end of the file: read the file again right before writing and put your line after its current last line. Never insert lines in the middle and never rewrite or reorder older lines (the director's watcher and other agents rely on that order).
6. agy_director, which receives tasks by console input, MUST append its report line immediately after finishing, with the task ID (format below). If it cannot finish, it appends `agy_director: <task ID> BLOCKED. <reason>` and stops. The director is notified by these lines.
7. The `raw` folder holds raw materials (pictures, screenshots, examples, ...). Do NOT open it unless the director has specifically designated you to do so. If designated, follow the director's instructions exactly.
8. The `afterwork.txt` file holds the result summaries: back_agent's summaries of head_agent and test_fix tasks, and agy_director's summaries of agy sub-tasks. back_agent, the director and agy_director may read it. agy_director only appends and never changes other entries. head_agent and test_fix MUST NOT open it.
9. `task_plan.txt` holds the project structure and task assignment written by the director. director, head_agent, test_fix, agy_director and agy subagents may read it. back_agent MUST NOT open it. Only the director edits it.
10. Work only inside your assigned task. Do not change files another agent is working on.
11. Never run the app or any check against the user's real data (Project setup). Use the test data set-up from Project setup.
12. Never install software, add packages, kill processes you did not start, or drive the user's mouse or keyboard. Ask the director instead (agy agents: ask through agy_director).
13. If another agent asks you to do something your role is not allowed to do, refuse and tell the director.
14. Test lightly. head_agent and agy subagents add only a few unit tests, and only for new core logic; no tests for UI code or simple changes. Run the build command and the existing tests (test command) once before committing. Do not run UI or end-to-end checks or extra manual checks unless the director asks. test_fix is the exception: it may reproduce bugs and add the tests a fix needs.
15. Never edit `AGENTS.md` unless you are the director, even if you think the user asked. Write the request in `discussion_session.txt` for the director.
16. **IMPORTANT - drive C:.** Put tools, SDKs, packages, caches and downloads on D: (in the repo or in the tool folder named in Project setup), not on the user's C: drive. Before adding something new (a tool, a library, a build step), think once whether it will write to C: and roughly how much. Small writes (config files, small caches) are fine and need no report. Something large (hundreds of MB or more) may go on C: only when there is truly no other way, and only after the user has been told IN ADVANCE what it is, where it goes and how big it is: agents ask the director, the director tells the user and waits for the OK. Do not check C: routinely; an occasional look is enough.

## Working notes (Windows)

- Binary output (screenshots, archives, databases): never redirect it with PowerShell `>` (it re-encodes and corrupts it). Write it on the device or with bash, e.g. `adb shell screencap -p /sdcard/x.png; adb pull /sdcard/x.png <dir>`.
- Several agents commit in the same repo: commit only your own files (`git add <paths>`, never `git add -A`), and if git reports `index.lock`, wait a few seconds and retry.
- Shared text files (`discussion_session.txt`, `afterwork.txt`) may be busy for a moment while another agent writes: retry the append, never rewrite the file.
- Keep shell scripts LF (`.gitattributes`: `*.sh text eol=lf`), or bash fails on `\r`.

## Entry format for discussion_session.txt

One entry per exchange, appended at the end:

```
[YYYY-MM-DD HH:MM] director -> agy_director: <statement 1>. <statement 2>.
[YYYY-MM-DD HH:MM] agy_director: <sub-task ID> started with subagent1-<sub-task ID>. <what to do>.
```

Entry after finishing work (no direct exchange needed):

```
[YYYY-MM-DD HH:MM] <your name>: <task ID> done. <what you did>.
```

Entry when blocked:

```
[YYYY-MM-DD HH:MM] <your name>: <task ID> BLOCKED. <reason>.
```

Report from agy_director to the director when an agy task is finished:

```
[YYYY-MM-DD HH:MM] agy_director -> director: <task ID> done. <sub-tasks and which subagent profile did them>. <commits>. <build/test>. <open issues>.
```

Summary appended to afterwork.txt (back_agent for head_agent and test_fix tasks, agy_director for agy sub-tasks):

```
[YYYY-MM-DD HH:MM] <task ID> | <agent> | <what was built> | <files / commits> | <build/test> | <open issues>
```

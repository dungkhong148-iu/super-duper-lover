#!/usr/bin/env bash
# watch_reports.sh - director only. Prints each NEW agy_director report line in discussion_session.txt,
# one line per event, wherever in the file the line was written (agents sometimes insert lines
# in the middle instead of appending, which byte-offset watchers like `tail -f` miss).
#
#   bash watch_reports.sh [file] [interval-seconds]
#
# Compares the set of matching lines every few seconds; lines already present at start are not printed.
f="${1:-$(dirname "$0")/discussion_session.txt}"
interval="${2:-5}"
pat='agy_director( ->|:).*(done|DONE|BLOCKED|QUESTION|FAIL|fail)'
seen="$(mktemp)"; now="$(mktemp)"
trap 'rm -f "$seen" "$now"' EXIT
grep -E "$pat" "$f" 2>/dev/null | tr -d '\r' | sort -u > "$seen"
while true; do
  sleep "$interval"
  # A failed read (file busy while another agent writes) skips this round instead of ending the watch.
  grep -E "$pat" "$f" 2>/dev/null | tr -d '\r' | sort -u > "$now" || continue
  [ -s "$now" ] || continue
  comm -13 "$seen" "$now"
  cp "$now" "$seen"
done

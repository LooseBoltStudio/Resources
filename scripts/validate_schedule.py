#!/usr/bin/env python3
"""Reject malformed assignment schedules before they reach the app."""

import csv
import re
import sys
from pathlib import Path


REQUIRED_COLUMNS = [
    "location_group",
    "job_id",
    "craft",
    "crew_count",
    "starting_location",
    "workday",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday",
    "notes",
]
DAYS = REQUIRED_COLUMNS[6:13]
DAY_VALUE = re.compile(r"^(OFF|\d{2}:\d{2}( .+)?)$")


def validate(path: Path) -> list[str]:
    errors: list[str] = []
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source, restkey="__extra__", restval=None)
        if reader.fieldnames != REQUIRED_COLUMNS:
            errors.append(
                "Header must be exactly: " + ",".join(REQUIRED_COLUMNS)
            )
        rows = list(reader)

    if len(rows) < 5:
        errors.append("Schedule must contain at least five assignments")

    seen: set[str] = set()
    for line_number, row in enumerate(rows, start=2):
        job_id = (row.get("job_id") or "").strip()
        label = job_id or f"line {line_number}"

        if row.get("__extra__"):
            errors.append(f"{label}: contains extra columns")
        missing_values = [name for name in REQUIRED_COLUMNS if row.get(name) is None]
        if missing_values:
            errors.append(f"{label}: missing columns: {', '.join(missing_values)}")
            continue

        if not job_id:
            errors.append(f"line {line_number}: job_id is required")
        elif job_id in seen:
            errors.append(f"{label}: duplicate job_id")
        seen.add(job_id)

        for name in ("location_group", "craft", "starting_location", "workday", "notes"):
            if not (row.get(name) or "").strip():
                errors.append(f"{label}: {name} is required")

        try:
            if int(row["crew_count"]) < 1:
                raise ValueError
        except (TypeError, ValueError):
            errors.append(f"{label}: crew_count must be a positive integer")

        for day in DAYS:
            value = (row.get(day) or "").strip()
            if not DAY_VALUE.fullmatch(value):
                errors.append(f"{label}: invalid {day} value: {value!r}")

    return errors


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: validate_schedule.py PATH", file=sys.stderr)
        return 2

    path = Path(sys.argv[1])
    errors = validate(path)
    if errors:
        print(f"Schedule validation failed for {path}:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    with path.open("r", encoding="utf-8-sig", newline="") as source:
        assignment_count = sum(1 for _ in csv.DictReader(source))
    print(f"Schedule validation passed: {assignment_count} assignments")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

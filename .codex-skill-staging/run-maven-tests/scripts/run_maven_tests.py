#!/usr/bin/env python3
import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path


def detect_command(workdir: Path) -> list[str]:
    wrapper = workdir / "mvnw"
    if wrapper.exists():
        return ["./mvnw", "test"]
    return ["mvn", "test"]


def parse_log(text: str) -> dict:
    lines = text.splitlines()
    summary = {
        "build_result": "success" if "BUILD SUCCESS" in text else "failed",
        "failing_tests": [],
        "compilation_errors": [],
        "error_summary": [],
        "fix_hints": [],
    }

    test_failure_pattern = re.compile(r"^\[ERROR\]\s+([A-Za-z0-9_.$]+)\.([A-Za-z0-9_$<>,\[\]-]+):(\d+)\s+(.*)$")
    compilation_pattern = re.compile(
        r"^\[ERROR\]\s+(.+?\.java):\[(\d+),(\d+)\]\s+(.*)$"
    )
    surefire_pattern = re.compile(r"^\[ERROR\]\s+([A-Za-z0-9_.$]+)\.([A-Za-z0-9_$]+)\s+Time elapsed:")
    tests_run_pattern = re.compile(r"Tests run: .* - in ([A-Za-z0-9_.$]+)")

    current_test_class = None

    for line in lines:
        m = tests_run_pattern.search(line)
        if m:
            current_test_class = m.group(1)

        m = surefire_pattern.search(line)
        if m:
            summary["failing_tests"].append(
                {"class": m.group(1), "method": m.group(2), "message": ""}
            )
            continue

        m = test_failure_pattern.search(line)
        if m and (m.group(1).endswith("Test") or m.group(1).endswith("Tests")):
            summary["failing_tests"].append(
                {
                    "class": m.group(1),
                    "method": m.group(2),
                    "line": m.group(3),
                    "message": m.group(4),
                }
            )
            continue

        m = compilation_pattern.search(line)
        if m:
            summary["compilation_errors"].append(
                {
                    "file": m.group(1),
                    "line": m.group(2),
                    "column": m.group(3),
                    "message": m.group(4),
                }
            )

    if summary["compilation_errors"]:
        first = summary["compilation_errors"][0]
        summary["error_summary"].append(
            f"Compilation failed in {first['file']}:{first['line']}:{first['column']} - {first['message']}"
        )
        summary["fix_hints"].append("Fix the compiler error before rerunning the test suite.")

    if summary["failing_tests"]:
        first = summary["failing_tests"][0]
        location = first.get("class", current_test_class or "unknown test")
        method = f".{first['method']}" if first.get("method") else ""
        message = first.get("message") or "See Maven log for assertion or exception details."
        summary["error_summary"].append(f"Test failure in {location}{method} - {message}")
        summary["fix_hints"].append("Start with the first failing test; later failures may be cascading.")

    if not summary["error_summary"] and summary["build_result"] == "failed":
        if "COMPILATION ERROR" in text:
            summary["error_summary"].append("Compilation failed; inspect the Maven compiler output.")
        elif "There are test failures." in text:
            summary["error_summary"].append("Test execution failed; inspect Surefire output for details.")
        elif "ContainerLaunchException" in text or "Testcontainers" in text:
            summary["error_summary"].append("Testcontainers failed to start; inspect Docker and container logs.")
            summary["fix_hints"].append("Check Docker availability and PostgreSQL container startup logs.")
        else:
            summary["error_summary"].append("Build failed; inspect the raw Maven log for the first [ERROR] block.")

    if summary["build_result"] == "success":
        summary["error_summary"].append("All Maven tests passed.")

    return summary


def print_text_summary(summary: dict) -> None:
    print(f"build_result: {summary['build_result']}")
    if summary["failing_tests"]:
        print("failing_tests:")
        for failure in summary["failing_tests"]:
            cls = failure.get("class", "unknown")
            method = failure.get("method")
            suffix = f".{method}" if method else ""
            message = failure.get("message")
            if message:
                print(f"- {cls}{suffix}: {message}")
            else:
                print(f"- {cls}{suffix}")
    else:
        print("failing_tests: []")

    if summary["compilation_errors"]:
        print("compilation_errors:")
        for error in summary["compilation_errors"]:
            print(f"- {error['file']}:{error['line']}:{error['column']} {error['message']}")

    print("error_summary:")
    for item in summary["error_summary"]:
        print(f"- {item}")

    if summary["fix_hints"]:
        print("fix_hints:")
        for hint in summary["fix_hints"]:
            print(f"- {hint}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run Maven tests and summarize failures.")
    parser.add_argument("--workdir", default=".", help="Project directory")
    parser.add_argument("--log-file", default=None, help="Optional path for raw Maven output")
    parser.add_argument("--json", action="store_true", help="Print JSON summary")
    parser.add_argument("--dry-run", action="store_true", help="Only print the detected command")
    args = parser.parse_args()

    workdir = Path(args.workdir).resolve()
    cmd = detect_command(workdir)

    if args.dry_run:
        print("command:", " ".join(cmd))
        return 0

    completed = subprocess.run(
        cmd,
        cwd=workdir,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        env=os.environ.copy(),
    )
    output = completed.stdout

    if args.log_file:
        Path(args.log_file).write_text(output)

    summary = parse_log(output)
    summary["command"] = cmd
    summary["exit_code"] = completed.returncode

    if args.json:
        print(json.dumps(summary, indent=2))
    else:
        print_text_summary(summary)

    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())

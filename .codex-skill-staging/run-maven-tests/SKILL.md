---
name: run-maven-tests
description: Run Maven builds and test suites for Spring Boot backends that use Java, JUnit 5, jqwik, Testcontainers, and PostgreSQL. Use when Codex needs to run project tests, validate code changes, verify build success, detect failing tests, analyze compilation errors, or summarize Maven output into concise actionable failures.
---

# Run Maven Tests

## Overview

Run the Maven test suite with the project's normal entrypoint, capture the output, and convert failures into a short actionable report. Prefer the Maven wrapper when present and separate test failures from compilation or build configuration errors.

## Workflow

1. Detect whether `./mvnw` exists and use it; otherwise use `mvn`.
2. Run the full test suite with `test` unless the user asks for a narrower scope.
3. Capture the full log and extract failing tests, compilation errors, and the most useful stack-trace or message details.
4. Report the result as success or failure, then list failing classes or methods, summarize the error, and add brief fix hints.
5. If output is noisy, use [`scripts/run_maven_tests.py`](scripts/run_maven_tests.py) to run and summarize consistently.

## Authoring Rules

- Prefer `./mvnw test` when the wrapper exists.
- Fall back to `mvn test` only when the wrapper is missing.
- Run all tests unless the task explicitly asks for a subset.
- Preserve the raw failure signal; do not paraphrase away the actual failing class, method, file, or line number.
- Distinguish test failures from compilation failures and container or environment failures.
- For compilation errors, report the file, line number, and compiler message.
- For test failures, report the test class, test method when available, and the first meaningful assertion or exception message.
- Keep fix hints short and grounded in the actual error rather than generic advice.

## Output Format

Return all of the following:

1. Build result: success or failed.
2. List of failing tests.
3. Error summary.
4. Recommended fix hints.

## Safety Checks

- Verify the command used the wrapper when available.
- Verify the summary does not hide a compilation failure behind later Maven noise.
- Verify failing tests are listed individually when Maven exposes them.
- Verify environment problems such as Docker or Testcontainers startup issues are called out separately from application failures.
- When the build succeeds, say so clearly and avoid inventing issues.

## Reference

Read [`references/maven-test-triage-checklist.md`](references/maven-test-triage-checklist.md) when you need a compact checklist for command selection, failure classification, and concise reporting.

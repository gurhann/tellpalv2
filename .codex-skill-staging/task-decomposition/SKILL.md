---
name: task-decomposition
description: Decompose large software development work into small atomic tasks for autonomous coding agents in Spring Boot modular monolith backends that use Spring Modulith, PostgreSQL, Flyway, DDD, REST APIs, property testing, and Testcontainers. Use when Codex needs to break down backlog items, generate implementation steps, organize development workflow, or prepare dependency-ordered tasks with files, acceptance criteria, and verification steps.
---

# Task Decomposition

## Overview

Break large backend work into small, independently executable tasks that an autonomous coding agent can complete reliably in one step. Keep tasks narrow in file scope, explicit in outcome, and ordered so schema, domain, application, API, and testing work build on each other without circular dependency.

## Workflow

1. Identify the user-visible outcome and the bounded context or module it belongs to.
2. Split the work into thin slices that follow the normal backend sequence: schema, domain, repository, application service, controller, then tests.
3. Keep each task small enough to modify only a small number of files and produce one clear result.
4. Record dependencies between tasks and order them so later tasks do not block earlier ones.
5. For each task, specify objective, files to create or modify, acceptance criteria, and verification steps.

## Decomposition Rules

- Make each task small enough to implement in one coding step.
- Prefer tasks that touch one layer or one narrow cross-layer slice, not the entire feature at once.
- Avoid tasks that mix unrelated modules.
- Call out blocking dependencies explicitly.
- Avoid circular task dependencies; if they appear, split the shared prerequisite into its own earlier task.
- Keep file scope tight. If a task touches many files, split it further.
- Use repo-specific file paths when they can be inferred.

## Preferred Task Order

1. database schema
2. domain model
3. repository
4. application service
5. controller
6. tests

When test-first sequencing is useful, create the test task immediately before or alongside the implementation task it validates, but keep the overall dependency chain aligned with the architecture above.

## Task Template

For each task include:

- objective
- files to create or modify
- dependencies
- acceptance criteria
- verification steps

## Testing Rules

- Generate unit or property-test tasks before or beside the implementation that they validate when feasible.
- Include integration-test or Testcontainers tasks when persistence, migrations, API flows, or transaction behavior matter.
- Make verification concrete, such as specific test classes, commands, or observable outcomes.

## Output Format

Return all of the following:

1. A list of atomic tasks.
2. Dependency ordering.
3. Files affected by each task.
4. Acceptance criteria for each task.

## Safety Checks

- Verify no task is too large for a single coding agent step.
- Verify dependencies are acyclic and explicit.
- Verify tasks respect module boundaries.
- Verify verification steps are actionable.
- When a task still feels broad, split it again before returning the plan.

## Reference

Read [`references/task-decomposition-checklist.md`](references/task-decomposition-checklist.md) when you need a compact checklist for task sizing, dependency ordering, file scoping, and acceptance-criteria quality.

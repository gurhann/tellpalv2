# Task Decomposition Checklist

## Task Size

- each task should be implementable in one coding step
- each task should touch a small number of files
- split any task that spans multiple modules or too many layers

## Dependency Order

- prefer schema before domain
- prefer domain before repository
- prefer repository before application service
- prefer application service before controller
- place tests near the implementation they verify
- break shared prerequisites into separate earlier tasks

## Required Fields Per Task

- objective
- files to create or modify
- dependencies
- acceptance criteria
- verification steps

## Acceptance Criteria

- describe observable completion, not vague intent
- tie criteria to business behavior or architecture outcomes
- keep criteria small enough to verify in one pass

## Verification

- name the test, command, or manual check
- include integration verification when persistence or APIs are involved
- avoid generic steps like "make sure it works"

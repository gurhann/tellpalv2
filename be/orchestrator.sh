#!/bin/bash

TASK="$1"

if [ -z "$TASK" ]; then
  echo "Usage: ./orchestrator.sh \"task description\""
  exit 1
fi

echo ""
echo "================================="
echo "STEP 1: PLAN TASK"
echo "================================="

codex exec "
Use the task-decomposition skill.

Break down this task into atomic steps:

$TASK

Update NEXT_TASK.md with the steps.
"

echo ""
echo "================================="
echo "STEP 2: WRITE TESTS"
echo "================================="

codex exec "
Read NEXT_TASK.md.

Write unit tests or property tests first.

Use skills:
- write-jqwik-property-test
"

echo ""
echo "================================="
echo "STEP 3: IMPLEMENT CODE"
echo "================================="

codex exec "
Implement the next step in NEXT_TASK.md.

Use relevant skills:
- write-flyway-migration
- write-jpa-entity
- write-spring-service
- write-rest-controller
"

echo ""
echo "================================="
echo "STEP 4: RUN TESTS"
echo "================================="

./mvnw test

if [ $? -ne 0 ]; then
  echo ""
  echo "Tests failed. Asking Codex to fix..."

  codex exec "
  Tests failed.

  Use run-maven-tests skill to analyze failures
  and fix the code.
  "
fi

echo ""
echo "================================="
echo "STEP 5: REVIEW CODE"
echo "================================="

codex exec "
Review the last code changes.

Use skills:
- code-review-checklist
- spring-modulith-boundary-check
"

echo ""
echo "================================="
echo "PIPELINE COMPLETE"
echo "================================="
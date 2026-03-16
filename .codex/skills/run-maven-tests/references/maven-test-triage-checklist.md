# Maven Test Triage Checklist

## Command Selection

- use `./mvnw test` when the wrapper exists
- otherwise use `mvn test`
- run the full suite unless the task explicitly narrows scope

## Failure Types

- compilation errors
- failing tests
- surefire or failsafe configuration errors
- Testcontainers or Docker startup failures
- Flyway or database startup failures

## What To Report

- overall build result
- failing test class and method when available
- compiler file and line number when available
- first useful assertion or exception message
- short fix hint tied to the actual error

## Reporting Style

- be concise
- separate root cause from cascading Maven noise
- avoid copying the full log when a short summary is enough
- call out environment failures distinctly from code failures

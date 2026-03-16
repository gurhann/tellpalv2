# Modulith Boundary Checklist

## Module Structure

- treat each top-level package as a module unless the project defines modules differently
- identify public API packages versus internal implementation packages
- flag external access to internal packages

## Dependency Review

- look for direct imports of another module's internal classes
- look for repository-to-repository calls across modules
- look for entity references crossing module boundaries
- look for cycles in module dependencies

## Architectural Smells

- business logic living in controllers, adapters, or infrastructure classes
- modules depending on persistence details from other modules
- shared utility packages becoming hidden cross-module backdoors
- APIs that expose internal entities instead of stable contracts

## Recommended Fixes

- move logic behind an application service in the owning module
- publish an event instead of calling into internals
- replace cross-module entity references with IDs or DTOs
- extract a published API type if another module needs a stable contract

## Verification

- recommend `ApplicationModules.of(Application.class).verify()`
- suggest an architecture test that runs in CI
- mention residual risk when verification is manual only

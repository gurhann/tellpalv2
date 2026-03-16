# Spring Service Modulith Checklist

## Service Role

- treat the service as an application-layer orchestrator
- coordinate repositories, aggregates, policies, and domain events
- keep persistence details out of the service
- keep business method names use-case oriented

## Transactions

- use `@Transactional` on state-changing use cases
- keep transactions as small as the business workflow allows
- use read-only transactions only when the module already uses that pattern
- avoid mixing external calls inside long database transactions

## Repository Use

- inject only repositories from the same module
- load aggregates, invoke domain behavior, then persist
- avoid using repositories as query bags inside complex loops
- keep existence and uniqueness checks explicit when they protect a business rule

## Domain Coordination

- prefer aggregate methods over direct field mutation
- keep invariants in domain objects where possible
- use domain services only when logic truly spans aggregates and does not belong to one root
- return DTOs or results only when the use case boundary requires them

## Modulith Boundaries

- do not inject another module's repository
- across modules, use IDs, application APIs, or published events
- avoid returning foreign-module entities from the service
- keep package placement inside the owning module

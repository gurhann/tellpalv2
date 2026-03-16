# jqwik Property Test Checklist

## Property Focus

- test laws and invariants, not fixed examples
- name each property after the rule it proves
- prefer one invariant per property
- prove behavior across many inputs, not a few curated cases

## jqwik Basics

- use `@Property`
- use `@ForAll`
- use provider methods with `Arbitraries` for domain-specific values
- set tries to at least 100 if the project default is unclear

## Generator Design

- generate values that match domain constraints
- include edge-heavy values such as empty, max-length, duplicates, negatives, and boundary-sized collections when relevant
- prefer composed generators over filtering with assumptions
- keep generators readable so shrinking stays useful

## Invariant Categories

- uniqueness constraints
- ordering invariants
- validation boundaries
- normalization consistency
- round-trip or idempotency behavior

## Persistence and Spring

- isolate whether the property is pure domain logic or needs Spring context
- avoid expensive integration setup unless the invariant actually depends on persistence behavior
- if testing repository constraints, keep the property focused on the rule being checked
- do not mix many unrelated concerns in one property

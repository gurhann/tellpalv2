# Decision Record Standard

## Purpose

This standard defines how engineering decisions are stored so that a future project, a new team
member, or an autonomous agent can recover why the system looks the way it does.

The decision system has two layers:

- `ADR`: durable record for major architecture, product, platform, and policy decisions
- `Project Memory`: living summary of currently active defaults and references

## When to Create an ADR

Create an ADR when a decision:

- changes architecture, module ownership, or public boundaries
- introduces or removes a technical policy
- changes persistence, migration, security, observability, or integration strategy
- would otherwise be rediscovered through Slack, PR history, or tribal memory

Do not create an ADR for a narrow refactor with no durable policy impact.

## ADR Format

Each ADR must contain:

- Title
- Status
- Context
- Decision
- Consequences
- Alternatives considered
- Related files or modules
- Supersedes / Superseded by

## Project Memory Format

Each project keeps one living `project-memory.md` that contains:

- canonical documents
- active architectural decisions
- coding and documentation defaults
- module interaction defaults
- testing defaults
- review red flags

Project memory is the fast entry point. ADRs are the deep record.

## Update Rules

- When a durable decision changes, update the ADR first or create a new superseding ADR.
- Then update project memory so the current default remains easy to discover.
- If code comments mention a large decision, reference the ADR or project memory entry.

## Read Order

Agents and engineers should read in this order:

1. `AGENTS.md`
2. `project-memory.md`
3. relevant ADRs
4. architecture or design documents

## Quality Bar

A good decision record is:

- specific enough that the implementer does not need to guess
- short enough to scan in minutes
- linked to the modules or files it affects
- updated when the decision stops being true

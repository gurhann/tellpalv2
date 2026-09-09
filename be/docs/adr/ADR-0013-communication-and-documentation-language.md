# ADR-0013: Communication and Documentation Language

## Status

Accepted on 2026-09-09.

## Context

The project owner prefers Turkish for day-to-day communication and has chosen English for long
technical documents. English may reduce token consumption for equivalent technical prose, but
the project has not measured the savings; no fixed cost or usage-limit reduction is assumed.
ADR-0003 already requires English for durable code documentation.

## Decision

- Use Turkish for conversations, clarification questions, progress updates, and final summaries.
- Write new long technical documents in English, including architecture documents, specifications,
  implementation plans, technical reports, and operational runbooks.
- Continue writing ADRs, project memory, code comments, and API documentation in English.
- Keep code identifiers, API field names, and established technical terms consistent in English.
- Follow an explicit user request for a different language for a particular deliverable.
- Preserve existing Turkish documents during routine edits; translate them when explicitly requested
  or when a replacement document is agreed. This decision does not require bulk translation.
- Product content and localization retain their intended audience languages.

## Consequences

- The owner can discuss requirements and review outcomes in Turkish.
- Long technical artifacts share the language of code and most technical terminology.
- Potential token savings depend on the text and model and are not a guaranteed financial benefit.
- Existing Turkish documents can coexist with new English documents.

## Alternatives Considered

- English for all communication: not selected because Turkish is the owner's preferred conversation language.
- Turkish for all documents: not selected because the owner prefers English for long technical artifacts.

## Related Files or Modules

- `AGENTS.md`
- `be/docs/project-memory.md`
- `be/docs/adr/ADR-0003-code-documentation-policy.md`

## Supersedes / Superseded By

- None. Extends ADR-0003 without replacing its code documentation policy.

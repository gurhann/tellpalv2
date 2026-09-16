---
topic: TellPal screen UX review workflow
goal: Review one CMS screen at a time without losing domain-backed functionality
updated: 2026-09-16T11:28
---

- (decision) Use a complex workflow, not a persistent autonomous agent, for the first version.
- (direction) Process exactly one screen per run: function map, domain evidence, UX audit, function-preserving mockup improvement, verification, then user feedback loop.
- (decision) Default scope is mockup changes and verification; production CMS changes require explicit user approval.
- (decision) Decline end-user customization; ship fixed TellPal-specific paths and rules in the first version.
- (event) Initial workflow build complete; handoff pending validation summary.

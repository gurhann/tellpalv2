# Project Bootstrap Checklist

Use this checklist when applying the standards package to a new repository.

1. Copy the standards package into a dedicated standards repository or vendor it into the target repo.
2. Add the AGENTS snippet to the root `AGENTS.md`.
3. Create `docs/project-memory.md` from the template.
4. Create `docs/adr/` and add the first ADRs:
   - architecture baseline
   - module ownership and boundaries
   - documentation/commenting policy
   - shared kernel or shared module policy
5. Copy the code documentation guidelines template into the project docs area and project-tune it.
6. Add the `apply-project-standards` Codex skill or link to it from the standards repository.
7. Add a PR template or review checklist that checks comments plus decision updates.
8. Update the project architecture doc to point to `project-memory.md` and `docs/adr/`.
9. Ensure agents and reviewers know the read order:
   - `AGENTS.md`
   - `project-memory.md`
   - ADRs
   - architecture docs

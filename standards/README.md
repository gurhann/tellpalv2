# Engineering Standards Seed

This directory is the seed content for a standalone `engineering-standards` repository.

It packages reusable standards for:

- code comments and Javadoc
- REST API documentation
- architecture decision records
- project memory
- Codex/agent bootstrap

When adopted by another project, copy this directory into a dedicated standards repository or
consume the files directly during project bootstrap.

## Contents

- `commenting-standard.md`
- `rest-api-documentation-standard.md`
- `decision-record-standard.md`
- `project-bootstrap-checklist.md`
- `templates/`
- `.codex/skills/apply-project-standards/`

## Intended Bootstrap Flow

1. Copy the template set into the target project.
2. Add the AGENTS snippet to the target repository root `AGENTS.md`.
3. Create `docs/project-memory.md` from the template.
4. Create the first ADRs from the template for architecture, module ownership, and documentation.
5. Add the PR review checklist so review enforces the standard before automation exists.

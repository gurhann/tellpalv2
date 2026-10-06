# Mobile implementation tracking

This directory tracks the mobile replacement plan only. Its story IDs overlap with the CMS/backend plan; never infer progress from `../sprint-status.yaml` or use that file for mobile work.

- Epic source: [epics-mobile.md](../../planning-artifacts/epics-mobile.md).
- Tracker: [sprint-status.yaml](./sprint-status.yaml).
- Story files: this directory, using the keys generated in the mobile tracker.
- Readiness: [October 2 assessment](../../planning-artifacts/implementation-readiness-mobile-2026-10-02.md), `CONCERNS`, proceeding with recorded story-specific gates.
- Architecture: [mobile spine](../../planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md), ADR-0018 and applicable repository instructions.

Pass the epic source, tracker and story directory explicitly to `bmad-build`, sprint status and tracking refresh sessions. The default implementation-artifacts tracker belongs to CMS/backend work. Backlog entries are approved scope, not verified integration readiness or accepted implementation. Advance statuses only through the applicable build workflow and actual evidence.

## Next build invocation

Use `bmad-build` for Story 1.1, "Discover eligible content on the Library home screen", with:

```text
epic source: _bmad-output/planning-artifacts/epics-mobile.md
tracker: _bmad-output/implementation-artifacts/mobile/sprint-status.yaml
story directory: _bmad-output/implementation-artifacts/mobile/
story: 1.1
```

Begin with the compatible toolchain/generator check and required backend catalog companion contract. Establish authorized development/test access and verify redacted fixtures for category order, preview limits, selected-language isolation and explicit different-language categories, audience/locks and empty/error cases before mobile listing integration acceptance. Story 1.2 follows accepted 1.1; do not include the reader or production auth/migration in this first work unit.

AD-5 signed Android/iOS upgrades remain required before auth/migration feature development. Other external contracts, physical platform evidence and release gates close at their documented consumer/acceptance points. Sprint tracking does not waive them.

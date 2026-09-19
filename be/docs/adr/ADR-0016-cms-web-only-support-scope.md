# ADR-0016: TellPal CMS Is a Web-Only Admin Surface

- Status: Accepted
- Date: 2026-09-19

## Context

TellPal CMS is an internal editorial operations application used by administrators in a desktop web browser. The product does not promise a mobile CMS workflow, and mobile-sized visual comparisons have been consuming implementation and review effort without representing a supported operator surface.

## Decision

Treat the CMS as a web-only admin surface. The supported visual QA matrix is laptop and desktop browser widths of `1280` and `1440` pixels. Mobile-sized and tablet-sized viewport baselines, mobile-specific composition work, and mobile fidelity comparisons are out of scope unless the product scope is explicitly changed by a future decision.

The existing responsive CSS may remain when it is inexpensive and harmless, but it is not a compatibility contract. New CMS layout work must optimize first for the supported web widths and must not be blocked by mobile-only visual differences.

This decision applies only to the CMS admin application. Public mobile delivery, backend mobile APIs, and consumer mobile experiences are unaffected.

## Consequences

- CMS visual regression suites use `1280` and `1440` web viewports by default.
- New CMS work does not require `390` or `768` screenshot baselines or mobile-specific layout tuning.
- Desktop hierarchy, readable density, keyboard access, and absence of horizontal overflow remain required.
- A future request for a mobile CMS workflow must explicitly revisit this ADR and restore a supported mobile viewport matrix.

## Affected Artifacts

- `cms/docs/ui-standards.md`
- `cms/docs/ui-regression-task-list.md`
- `cms/e2e/visual/visual-test-helpers.ts`

---
title: 'Complete Variant A content detail mockups for all content types'
type: 'feature'
created: '2026-09-16'
status: 'done'
baseline_commit: 'ba1d9db7139852d2985ffc94a7a93f4414001b9b'
review_loop_iteration: 0
context:
  - 'C:/github/tellpalv2/AGENTS.md'
  - 'C:/github/tellpalv2/cms/AGENTS.md'
  - 'C:/github/tellpalv2/cms/docs/ui-standards.md'
  - 'C:/github/tellpalv2/cms/docs/ui-regression-task-list.md'
  - 'C:/github/tellpalv2/_bmad-output/ux-screen-reviews/contents-detail/function-map.json'
  - 'C:/github/tellpalv2/_bmad-output/ux-screen-reviews/contents-detail/domain-evidence.json'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The Variant A Content Detail mockup family still has visible fidelity and completeness gaps across the Story, Meditation, and Lullaby detail variants when compared with the supplied reference composition.

**Approach:** Complete the fixture-backed detail mockup route for all three canonical content types. Align shared metadata, locale workspace actions, compact tabs, type-specific asset/workflow cards, contributor/source sections, responsive composition, and interactive mockup states while keeping the production route and domain contracts unchanged.

## Boundaries & Constraints

**Always:** Keep one dominant mockup editor workspace per variant; preserve all verified functions in the contents-detail function map; keep Story, Meditation, and Lullaby fields domain-specific; keep the rail operational and compact; match the reference’s calm surfaces, 8px spacing rhythm, compact controls, responsive stacking, visible labels, and text-plus-color status treatment; add regression guards for each variant and viewport.

**Ask First:** If matching the reference requires a new domain capability, API contract, persisted behavior, or removal of a verified function, stop and ask the user.

**Never:** Change `cms/src/app/routes/contents/detail.tsx` or other production detail behavior; add unverified fields/actions; make mockup controls look like persisted production success; show Story-only pages/source concepts for Meditation or Lullaby; add generic notes or duplicate metadata/status cards to the rail.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|-----------------------------|----------------|
| Story mockup | `/labs/mockups/contents/demo-content` | Locale workspace, preview, story-page handoff, narration/cover cards, contributors, and source-image section remain visible and interactive | Mockup actions update fixture state only |
| Meditation mockup | `/labs/mockups/contents/reference-meditation` | Locale audio/duration, shared listening cover, metadata, status, and asset dialog are shown without Story-only sections | Type-specific unsupported controls stay absent |
| Lullaby mockup | `/labs/mockups/contents/reference-lullaby` | Shared playback, duration/instrument entry, listing/playback cover variants, metadata, and locale status are shown | Playback and cover actions remain mockup-only |
| Narrow viewport | 390px or 768px width | Each variant stacks without horizontal overflow; header actions and asset cards remain usable | Preserve focusable controls and readable labels |

</frozen-after-approval>

## Code Map

- `cms/src/app/routes/mockups/content-detail.tsx` -- sole implementation target; Story, Meditation, and Lullaby detail compositions and mockup interactions.
- `cms/src/features/mockups/fixtures.ts` and `cms/src/features/mockups/types.ts` -- deterministic type-specific fixture data and verified visual state shape.
- `cms/src/features/mockups/components/mockup-ui.tsx` and `cms/src/components/workspace/workspace-shell.tsx` -- reusable mockup/production-safe primitives for cards, pills, and main-lane/rail layout.
- `cms/src/app/routes/mockups/contents.tsx` and `cms/src/app/router.tsx` -- mockup registry-to-detail route mapping; preserve the three canonical IDs.
- `cms/src/features/mockups/mockup-routes.test.tsx` -- interaction and route coverage for all detail variants.
- `cms/e2e/visual/registry-toolbar.visual.spec.ts` and its snapshots -- deterministic mockup visual guard at 390, 768, 1280, and 1440px.
- `cms/src/app/routes/contents/detail.tsx` -- read-only production comparison; must remain unchanged.

## Tasks & Acceptance

**Execution:**
- [x] `cms/src/app/routes/mockups/content-detail.tsx` -- align shared detail hierarchy and complete Story, Meditation, and Lullaby-specific mockup states without adding unverified capabilities.
- [x] `cms/src/features/mockups/fixtures.ts` and `cms/src/features/mockups/types.ts` -- correct deterministic fixture values and type-specific state needed by the reference composition.
- [x] `cms/src/features/mockups/mockup-routes.test.tsx` -- assert all three detail routes expose their applicable controls and preserve mockup-only interactions.
- [x] `cms/e2e/visual/registry-toolbar.visual.spec.ts` -- add/update detail visual coverage for Story, Meditation, and Lullaby at 390, 768, 1280, and 1440px.
- [x] `_bmad-output/ux-screen-reviews/contents-detail/` -- record the revised mockup-only scope, findings, change plan, and loss-prevention verification.

**Acceptance Criteria:**
- Given the Story mockup route, when the detail renders, then the reference hierarchy includes shared metadata, locale workspace actions/tabs, localized assets, contributor assignments, source images, and the operational rail.
- Given the Meditation mockup route, when the detail renders, then locale audio/duration and shared listening-cover controls appear while Story pages, narration, and source-image concepts remain absent.
- Given the Lullaby mockup route, when the detail renders, then shared playback/instrument entry and separate listing/playback cover controls appear while Story-only controls remain absent.
- Given any mockup variant, when its controls are activated, then state changes are deterministic, clearly mockup-only, and do not call production APIs.
- Given 390px, 768px, 1280px, or 1440px width, when each variant is inspected, then no horizontal overflow or clipped primary action is present.
- Given the updated mockup routes, when focused tests and visual checks run, then all three variants and the four required detail viewports pass.

## Design Notes

The target image is a visual reference, not a source of new domain capability. The implementation target is the fixture-backed mockup route, so its controls may update local fixture state but must remain visibly and semantically separate from production persistence. The three variants share the calm Variant A shell while exposing only the verified fields and workflows for their content type.

## Verification

**Commands:**
- `npm.cmd run build` -- expected: TypeScript/Vite build succeeds without production-route changes.
- `npm.cmd exec -- eslint <changed mockup files> --max-warnings=0` -- expected: no findings in changed files.
- `npm.cmd run test -- --run src/features/mockups/mockup-routes.test.tsx` -- expected: Story, Meditation, and Lullaby mockup interactions pass.
- `npm.cmd exec -- playwright test e2e/visual/registry-toolbar.visual.spec.ts --grep "contents mockup (detail visual|meditation detail visual|lullaby detail visual)" --workers=1` -- expected: all three detail mockups pass at the four required viewports.

## Suggested Review Order

**Detail composition**

- Shared reference route now owns locale state, metadata, type-specific cards, and operational rail.
  [`content-detail.tsx:1146`](../../cms/src/app/routes/mockups/content-detail.tsx#L1146)

- Locale tabs and editor state keep Meditation and Lullaby aligned with Story without Story-only sections.
  [`content-detail.tsx:1514`](../../cms/src/app/routes/mockups/content-detail.tsx#L1514)

- Lullaby playback duration is kept at content level instead of being coupled to a locale draft.
  [`content-detail.tsx:1193`](../../cms/src/app/routes/mockups/content-detail.tsx#L1193)

**Regression coverage**

- Route tests exercise Meditation metadata save and Lullaby shared playback editing.
  [`mockup-routes.test.tsx:146`](../../cms/src/features/mockups/mockup-routes.test.tsx#L146)

- The visual matrix covers Story, Meditation, and Lullaby at all four configured viewports.
  [`registry-toolbar.visual.spec.ts:224`](../../cms/e2e/visual/registry-toolbar.visual.spec.ts#L224)

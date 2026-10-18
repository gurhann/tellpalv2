# ADR-0018: Flutter Skill Architecture Policy

## Status

Accepted on 2026-09-28 at the user's request.

## Context

Flutter development needs a consistent implementation and review baseline. The user selected
the skills in https://github.com/RobertAlvv/flutter-skills and requires client architecture to
follow them. The repository contains the Flutter client under `app/mobile/`.

## Decision

- Use `flutter-scalable-app/SKILL.md` and its task-relevant references before Flutter planning,
  implementation, or refactoring. Its architectural rules govern Flutter technical choices.
- Use feature-first Clean Architecture with `data`, `domain`, and `presentation` boundaries;
  keep domain business logic independent of Flutter and infrastructure adapters.
- Follow the skill's BLoC/Cubit state management, `auto_route` navigation, `get_it` +
  `injectable` dependency injection, immutable state, repository contracts, and design tokens.
- Use the appropriate review skill: `architecture-auditor`, `state-management-auditor`,
  `widget-performance-analyzer`, `runtime-performance-auditor`, `atomic-design-system-auditor`,
  `testability-architecture-auditor`, or `cicd-architecture-auditor`.
- Keep all eight upstream skills, including their supporting references, in the repository's
  `.codex/skills/` directory so project contributors share the same guidance. The initial
  installation is pinned to upstream commit `4772c71f182e628df8601b4868ded9b6482d3891`.
- Keep the existing BMad process requirements; these skills supply Flutter-specific technical
  rules within that process. Record explicitly approved deviations here and in project memory.
- The 2026-09-28 CA update applies this decision to the mobile architecture spine and the
  adopted architecture copied into `epics-mobile.md`. Earlier Riverpod/Notifier and `go_router`
  selections are superseded; their historical memlog entries remain as decision history.
  Preserve AD identifiers and product invariants; AD-12 specifies composition and disposal
  ownership so route-owned BLoCs cannot terminate the application media session.
- Skill examples do not redefine TellPal backend contracts or require every optional Firebase
  service. Preserve the established API, authentication, and storage ownership decisions.
- Evaluate SDK/package compatibility when implementing; example version numbers are not an
  instruction to downgrade an existing project. Review upstream changes before updating skills.

## Consequences

New Flutter work has an explicit architectural baseline and focused review tools. Existing code
may require scoped migration during future development; installation does not perform or certify
that migration. Contributors can use the checked-in skills directly from the repository.
The backend remains governed by `architecture.md`; CMS rules remain in `cms/AGENTS.md`.

## Alternatives Considered

- Install skills without a project rule: rejected because future work could ignore the baseline.
- Migrate the entire Flutter client during installation: deferred because this request configures
  future development and does not define a migration scope.

## Related Files or Modules

- `AGENTS.md`
- `be/docs/project-memory.md`
- `app/mobile/`
- `_bmad-output/planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md`
- `_bmad-output/planning-artifacts/epics-mobile.md`
- https://github.com/RobertAlvv/flutter-skills/tree/4772c71f182e628df8601b4868ded9b6482d3891

## Supersedes / Superseded By

Supersedes the state-management/dependency-composition and routing technology selections in
the 2026-09-25 mobile spine (AD-2 and AD-6), while preserving their behavioral invariants.
No prior ADR is superseded.

## P4a2 için sınırlı QA istisnası (2026-10-17)

[ADR-0024](ADR-0024-reading-image-comparison.md), yalnız bağımsız local/test görsel karşılaştırmasında
`CachedNetworkImage` kuralı yerine sahipliği açık bellek raster'ı ve klonlamayan `CustomPainter`/`paintImage` kullanımını onaylar.
HTTP, buffer, descriptor, codec ve image açıkça kapatılır; disk/global ImageCache/precache kullanılmaz.
Feature-first katmanlar, Freezed Cubit, injectable ve auto_route kuralları sürer. Normal tüketici
cache davranışı ve P4b üretim politikası bu istisnayla değişmez.

## P4a4 ortak sunum kapsamı (2026-10-18)

Onaylı [P4a4 planı](../../../_bmad-output/implementation-artifacts/spec-p4a4-qa-consumer-delivery.md),
bu istisnayı ayrı opt-in QA detail/reader rotalarına taşır. Normal ekranlarla yalnız saf sunum
frame’leri paylaşılır. Frame’ler `core/presentation` altında kalır; feature bağımlılığı taşımaz.
QA kompozisyonu `app/` altında mevcut injectable Cubit, selector ve owned raster hattını kullanır.
Tek parent akış anchor ve kapatma sahibidir; child route’lar yalnız ID taşır.
Normal uygulamanın provider, ses, ön yükleme, erişim ve cache sahipliği korunur.
Bu kapsam genel tüketici geçişi veya yeni bir medya/cache politikası değildir (ADR-0024).

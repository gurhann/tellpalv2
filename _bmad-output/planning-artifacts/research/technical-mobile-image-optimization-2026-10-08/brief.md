# Mobile image optimization research brief

Date: 2026-10-08. Type: technical. Intent: run. Shape: explore. User-approved scope.

## Decision
Select a role-aware publication-to-display approach that preserves readable, correctly framed artwork on phones/tablets while minimizing network transfer, decoded image residency and low-memory crash risk. Recommendations feed the existing post-Story 1.2 image optimization draft; they do not adopt production budgets or alter code/storage.

## Questions and decomposition
1. Flutter/runtime: physical-pixel decode, fit/aspect ratio, decoded-cache versus live images, concurrency, precache/disposal, stable cache identity and low-memory handling.
2. Codec/pipeline: JPEG/WebP/AVIF/PNG, quality equivalence, alpha/color/orientation, source reuse, input/pipeline bounds and immutable verified renditions.
3. Device validation: Android API 24 and iOS/iPadOS 15 floor, old/low-RAM device measurement, transfer/decode/paint timing, memory signals and actionable failure evidence.
4. Lead synthesis: integrate source evidence with a separately attributed repository inventory; define experiment profiles and task refinement.

## Method and budget
Breadth-first fan-out, 3 researcher slots maximum, primary sources only. Deep coverage requested, up to 12 sources per dimension/round and 3 rounds; stop on coverage or novelty exhaustion. Normal verification, independent checks for load-bearing claims where available; mark unmatched framework-specific claims as single-primary-source, unverified by an independent publisher. No workflow orchestration surface available; use collaboration agents. Search via web.run. Target 15–25 minutes after approval, stop and report open evidence gaps if exceeded.

## Research firewall
Researchers get questions and explicit platform constraints, no project files or ambient project context. Repository inventory is implementation context, not independent scientific evidence. Every general technical conclusion must trace to a source retrieved in this run. Previous local JPEG results remain imported historical measurements with a narrow corpus.

## Deliverables
research.md (English), digests, claims ledger/staleness map, local-context.md and task-refinement.md. Turkish user-facing explanation. No final profile, deployment, production backfill, app rewrite or manufactured performance claims.

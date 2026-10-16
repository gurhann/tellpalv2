# P2 BMad review triage

Date: 2026-10-16. All three context-free reviewers were launched before collecting/acting on results: blind hunter, edge-case hunter and verification-gap reviewer. No model override was used. Review snapshot covered all tracked/untracked changes from `7dafccf22862decb001a56cd5b904480b1528912`; large raw logs remained in the full diff/manifest. The unrelated pre-existing `.memlog.md` modification is preserved and excluded from implementation ownership.

All findings below are accepted local patches/tests within already explicit frozen requirements. No product intent, selected profile, access policy or architecture decision is missing; no frozen-block edit or spec re-derivation is required. Severity is assigned here by practical consequence, independently of reviewer labels. The two duplicate deadline findings from edge review are merged with the blind review's same claim/action.

| ID | Source | Severity | Category | Required correction |
| --- | --- | --- | --- | --- |
| R1 | Blind | High | patch | Recheck lease time after acquiring the PostgreSQL row lock; stale caller time cannot extend ownership. Test real lock waits beyond lease expiry for success and failure. |
| R2 | Blind + edge | High | patch | Carry one monotonic revision budget through capture, final binding rechecks, schedule, READY reread and worker stages. Limit operations by remaining time and reject late success. |
| R3 | Edge | High | patch | Parent allocation admission must compare required/configured reservation with actual available parent JVM heap before parent buffering/decoding. Keep this separate from child profile deserialization. |
| R4 | Edge | Medium | patch | An exiting child can lose Linux `/proc/.../status` or macOS `ps` RSS during the exit transition. Reconfirm actual exit within a bounded monitor wait before tolerating an unavailable probe; retain fail-closed behavior for a live child. |
| R5 | Edge | High | patch | Snapshot REPEATABLE_READ must not silently join an outer READ_COMMITTED transaction. Enforce an independent consistent committed snapshot and test that call path. |
| R6 | Verification | High | patch | Real child/disk tests and packaged probe need independent known-color source-content assertions; valid dimensions/checksums alone cannot catch missing image drawing. Demonstrate the blank-output mutant is rejected. |
| R7 | Verification | High | patch | Mutate generation/registry binding during capture at an explicit barrier, rather than only between calls. Assert no new job/manifest and preserved previous READY. |
| R8 | Verification | High | patch | Test success/failure of an expired but unreplaced PROCESSING token separately from terminal replay and replacement-token mismatch. |
| R9 | Verification | Medium | patch | Exercise a real admitted baseline JFIF RGB8 JPEG through capture, child conversion and stored PNG verification, with independent source-content assertions. |

All nine patches are implemented. The post-review focused run passed 39 tests with zero failures/errors/skips ([raw log](review-rss-transition-focused.txt)); the final full-suite and packaged-runtime results are recorded in the [verification report](verification.md). Original review results are retained in the conversation; this table preserves every actionable claim and disposition without translating test gaps into claims of a known production failure. The read-only review layers found no other supported actionable issue. The implementation remains test-only; P0 evidence limits, unverified live Firebase behavior, production qualification and P3 gates are unchanged.

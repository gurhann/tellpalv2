# Independent review and patch disposition

Date: 2026-10-16. Baseline: `543c7ccf86377e88df74994463807d0f85b5f97d`.

Three context-free BMad reviewers were launched before collecting or acting on any result. They inspected the complete tracked/untracked change bundle; unrelated `.memlog.md` was identified as outside this task. The blind reviewer returned **No actionable findings**. The other results and two concrete parent findings are triaged independently below. All accepted actions are local patches within the approved diagnostic scope; none requires a production storage process layer or a change to the frozen intent.

| Source / claim | Severity | Route | Required patch |
| --- | --- | --- | --- |
| Verification gap: Python dry-run test mocks `launch`, leaving actual Java argument forwarding unobserved | high | patch | Run actual CLI/main → launch → supervised controlled executable; fail if `--execute-live` is forwarded, and verify no preparation or private-file effects. |
| Edge: launcher SIGTERM leaves the independent child session running | high | patch | Route SIGTERM through owned-process cleanup; kill the group, observe direct-child exit and restore the previous handler. Exercise an actual signal and child. |
| Edge: selector setup/registration failure happens before cleanup protection | medium | patch | Protect every post-spawn setup operation with cleanup; inject setup failure and observe the child is reaped. |
| Edge: ownership file is forced, but its new parent-directory entry is not forced before upload | medium | patch | Force the receipt parent directory before returning from exclusive creation; failure must prevent upload. |
| Parent: exit zero from a wrong executable can be reported as a successful live probe without worker evidence | medium | patch | Require a bounded private matching worker receipt with every completed case before emitting PASS; test missing/partial/mismatched receipts without cloud calls. |
| Parent: explicit `--java` appends a list to the already quoted command string | medium | patch | Assemble the entire argument list before shell quoting; cover explicit-runtime dry run. |

All six patches were applied in the existing diagnostic files. No further production change followed review. Actual post-patch checks: [18 Python tests](review-patches-python.txt), [7 Java guards](review-patches-focused.txt), and [real explicit-Java dry run](review-dry-run.txt) passed. The Python checks include an executed live-flag mutation, real CLI SIGTERM/child PID teardown, selector-registration failure with observed child exit, and missing/partial/mismatched/pre-existing versus complete synthetic worker receipts. Synthetic complete-receipt acceptance is launcher protocol evidence only, not a live GCS result.

Post-patch [full test](review-full-test.txt) passed 431 cases and [verify](review-full-verify.txt) passed 431 Surefire plus 1 Failsafe case, all with zero failures/errors/skips. The [current packaged JAR](review-packaged-temurin.txt) passed normal content and one-attempt metadata/body stalls in pinned Temurin with external network disabled. Historical failures remain preserved. Live GCS and absolute in-flight slow-trickle qualification remain unexecuted/unproven. No intent gap, spec loopback or deferred unrelated finding was required; the frozen approval block remains unchanged.

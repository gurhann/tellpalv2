# Story 1.6 final review — iteration 1

Baseline: `6252fdb7787ae0db8f7dc4edb47e25834212f547`.

The complete tracked/untracked review diff was 446,494 bytes, SHA256 `709254a2ba154e984da99f44aacbe9e24527fb99eac7b4c1184a24a811bd627d`. Three context-free reviewers used the session model without overrides. The blind reviewer returned no actionable findings. The edge and verification reviewers reported the five findings below. All are changes introduced by this story, with an unambiguous intended outcome; they are classified as patches rather than intent/spec loopbacks. No application intent or approved budget change is required.

| Finding | Severity | Route | Required action |
|---|---|---|---|
| Actual decode/layout sizes can agree with each other while differing from the admitted expected decode | high | patch | Require independent expected dimensions from admission and reject absent/malformed or mismatching dimensions; add regression mutations. |
| Transfer/decode/paint can occur in baseline while phase durations remain valid | high | patch | Require the target sequence through layout within the load phase for primary and quality runs; retain phase/duration checks. |
| Empty/partial capture expectation makes identity comparison vacuous | high | patch | Require the exact expected identity field set and strict types before comparison; cover empty/partial/extra/bool inputs. |
| Native Android PSS-to-byte conversion has no executable unit regression | medium | patch | Execute the actual Kotlin sampler with controlled OS inputs; verify positive/zero/large values and missing/failed readings. |
| Tests bypass the actual cache-key absence check | medium | patch | Execute the real method with existing immutable/transfer disk and decoded live/pending/keepAlive states; require refusal without a false absence event, plus a valid absent case. |

Physical application builds and original evidence remain immutable. The patches are limited to host qualification and additional tests outside the bound mobile source tree. The implementation agent has been re-engaged for all five patches. After verification, every original primary ledger and quality capture must be requalified using the stricter host observer; original receipts must not be overwritten. The maximum13 cloud windows is exhausted and no new allocation is authorized.

Status: all five patches resolved and verified. Host suite271/271, unchanged mobile regressions96/96, external Flutter7/7, analyzer and diff-check passed. The host suite executes the actual unchanged Kotlin sample across five scenarios, with no skipped native coverage. Independent replay under host observer r4 qualified all24 original primary ledgers and12 captures. Every original file and result dictionary remained unchanged. No app source change, new allocation, push or deployment occurred.

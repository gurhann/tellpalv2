# Editorial review: mobile image optimization research

This document exists to help engineering decision makers choose an image optimization approach and identify the next P0 experiments.

Reviewed exactly **structure**, then **prose** using the structure findings. Reader: humans. Style: plain, precise technical writing. Model: **Strategic/Context (Pyramid)**. Suggestions only; no research content was rewritten.

Standing-context input unavailable: `file:{project-root}/**/project-context.md` matched no file in the repository inventory; the review proceeded without it.

**Exact count: 4,299 words**, computed by `word_metrics.py` after the final scope-label corrections. The script counts whitespace-separated tokens, including Markdown and frontmatter. The report already meets the **3,500–4,500-word** target including its appendix. Relevant exact section counts: Executive decision **254**; §1 **310**; §2 geometry/contract **350 + 281**; §3 codec/normalization/worker **282 + 149 + 198**; §4 memory/scheduling/signals **172 + 257 + 90**; §5 **378**; §6 **186**; §7 **229**; appendix **755**; staleness/verification **225**.

Preserve the direct technical voice, concrete examples, measurement/proposal distinctions, safeguards and caveats. Preserve the required per-source **publisher/date/accessed/confidence** schema, even where field values repeat.

| Pass | Original Text | Revised Text | Changes |
| --- | --- | --- | --- |
| structure | §7 opening paragraph and open-decisions table — **153 words**; final HTTP policy paragraph — **76 words** | **MOVE** the first 153 words immediately after Executive decision, preserving the heading and renumbering for reading order. Move the HTTP paragraph intact to §3's publication/delivery discussion. | Expose P0 actions and dependencies before technical detail; group HTTP policy with its pipeline. Preserve new-publications-first rollout, historical transition, access safeguards and citations. Net word impact: **0**. |
| prose | Executive decision: “decode selection ignores actual slot/DPR” and “not measured blur, OOM or savings” | “decode selection ignores the actual image slot and device pixel ratio (DPR)” and “not measurements of blur, out-of-memory failures or savings” | Define abbreviations at first use and clarify the image slot. Preserve the distinction between inspection findings and measured outcomes. |
| prose | §2 role table: “the draft's 960/1440/1920 longest-edge candidates”; §3: “the separately cited inventories and existing draft” | Consider: name and link the existing SPEC on first occurrence, if it is the intended referent, and use that name consistently. | This report is also marked draft, so the unnamed reference can identify either document. |

**Summary:** three actionable rows; other findings concern minor copy edits or explicit preservation of existing evidence scaffolding. No cuts or condensation are recommended. Estimated structural reduction: **0 words (0%)**. Abbreviation expansions add approximately **12 words**, giving approximately **4,311 words** if accepted; the target remains met. There is no proposed loss of examples, engagement scaffolding or safeguards. Factual correctness and citation support were outside these editorial lenses.

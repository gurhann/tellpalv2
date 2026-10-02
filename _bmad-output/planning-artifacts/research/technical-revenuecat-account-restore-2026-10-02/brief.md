# Research brief

Date: 2026-10-02. Intent: run. Type: technical. Shape: explore. Topology: straightforward, one researcher. Scope explicitly authorized by the user: research RevenueCat account conflicts and restore behavior with concrete examples in a separate subagent.

Decision: What product rule and RevenueCat restore configuration should be proposed for TellPal? The earlier no-automatic-transfer suggestion is not approved and must be assessed against current official guidance.

Planning premises (not evidence about RevenueCat): existing subscribers must retain access during replacement; Firebase identity and backend-authoritative protected-media access are planned; anonymous use, conversion to a new account, sign-in to an existing account, multiple devices and no continuous legacy synchronization are in scope.

Questions:
1. Explain each current dashboard restore/transfer behavior, default and applicable platforms; distinguish restore, SDK login/alias/merge and store account ownership.
2. Explain stable App User IDs, Firebase UID mapping, anonymous conversion, logIn/logOut, and alias behavior.
3. Compare scenarios: A purchases then B restores on same store account; anonymous purchase then registration; fresh install/second device; anonymous sign-in to existing account; active versus expired subscription; wrong store account; Android/iOS cross-platform; old-to-new identity compatibility.
4. Recommend conditional options, support costs and failure modes; identify dashboard facts required before a final product decision.

Sources: official RevenueCat documentation/support examples and primary platform documentation where relevant. No account settings inspection or mutation. Budget: about 8 directly read sources, up to 2 targeted rounds, approximately 10 tool calls; stop on answered questions rather than filling the budget. No implementation/version claims requiring untested compatibility assumptions.

Deliverable: evidence digest with URLs, accessed dates, unknown publication dates stated as unknown, claim confidence and remaining questions. Scenario conclusions must distinguish documented vendor behavior from deductions under explicit configuration assumptions. Synthesized report in English; user summary in Turkish. No product decision is silently finalized.

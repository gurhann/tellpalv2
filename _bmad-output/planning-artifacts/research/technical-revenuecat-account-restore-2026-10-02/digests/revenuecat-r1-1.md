# RevenueCat identity and restore evidence digest

Accessed: 2026-10-02. Researcher: revenuecat_research. Scope: official documentation, read-only. Eight primary pages were read in two targeted discovery rounds. No dashboard, API, or application changes were made. No runtime behavior was tested. This digest supports a conditional proposal; it does not select TellPal's product policy.

## Epistemics

1. Never conclude from training data alone. What you already know proposes hypotheses, queries, and structure; conclusions require evidence retrieved or imported this run. A claim you cannot evidence is stated as an unverified belief or not at all.
2. The research firewall. Project context shapes what to ask, never what is true. It is inadmissible as evidence: every claim in a research artifact traces to a digest or import file with a source. You receive only this brief; no project files except explicitly named brief.

Only the named research brief was read locally. An upstream note about existing Firebase UID usage is a planning premise, not vendor evidence, and is not independently verified here. Guest purchasing, current dashboard settings, and existing aliases remain unknown.

## Source register

Access date for every entry: 2026-10-02. Publication date is unknown unless stated; crawl dates are not publication dates. Each page carried a 200-word synthesis allowance, so evidence summaries and dependent scenarios are intentionally compact.

| ID | Official primary source | Publisher | Publication / update date |
|---|---|---|---|
| S1 | [Restore Behavior](https://www.revenuecat.com/docs/projects/restore-behavior) | RevenueCat | Publication unknown; update unknown |
| S2 | [Identifying Customers](https://www.revenuecat.com/docs/customers/identifying-customers) | RevenueCat | Publication unknown; update unknown |
| S3 | [Restoring Purchases](https://www.revenuecat.com/docs/getting-started/restoring-purchases) | RevenueCat | Publication unknown; update unknown |
| S4 | [Customer Profile](https://www.revenuecat.com/docs/dashboard-and-metrics/customer-profile) | RevenueCat | Publication unknown; update unknown |
| S5 | [Firebase integration](https://www.revenuecat.com/docs/integrations/third-party-integrations/firebase-integration) | RevenueCat | Publication unknown; update unknown |
| S6 | [Link Multiple Auth Providers](https://firebase.google.com/docs/auth/web/account-linking) | Google / Firebase | Publication unknown; last updated 2026-10-01 UTC |
| S7 | [General Troubleshooting](https://www.revenuecat.com/docs/revenuecat-support/general-troubleshooting) | RevenueCat | Publication unknown; update unknown |
| S8 | [Importing your Historical Purchases](https://www.revenuecat.com/docs/migrating-to-revenuecat/migrating-existing-subscriptions) | RevenueCat | Publication unknown; update unknown |

Class D = directly documented vendor statement; I = inference from cited statements under listed assumptions; P = conditional proposal. Confidence M = medium: authoritative documentation, but the RevenueCat claims have one publisher and no independent implementation test. Firebase documentation establishes Firebase behavior, not independent validation of RevenueCat. Applicability to TellPal is unverified.

## C1: Restore policy and A-to-B scenario

Class D, confidence M, S1. A/B: distinct identified customers sharing a store receipt. T/N/K/L: shorthand.

| Policy | Active subscription | Expired subscription |
|---|---|---|
| T: Transfer to new App User ID (default) | B receives purchases; A loses them | Transfer permitted |
| N: Transfer if there are no active subscriptions | Transfer blocked | Transfer permitted; one-time purchases do not block |
| K: Keep with original App User ID | Different ID receives error | Different ID receives error |
| L: Share between App User IDs (legacy) | Customer identities merge | Customer identities merge |

Project-wide; sandbox override available. T/N/K also cover conflicting new purchases. L only covers restore; conflicting purchases error. L cannot be enabled on new projects or re-enabled after leaving it. Anonymous-owned purchases alias instead.

Vendor guidance: T for optional-login purchasing; also recommended with required accounts. K requires pre-purchase registration and recovery support. On iOS, a later subscription under K can remain attached to the old receipt owner. N accommodates expired subscribers.

## C2: SDK identity and login

Class D, confidence M, S2. RevenueCat-generated `$RCAnonymousID:` is device-cached; reinstall generates another. Custom IDs are unique, unguessable, case-sensitive and project-scoped.

| Current RevenueCat identity | Target custom ID | `logIn()` outcome |
|---|---|---|
| Anonymous | New | Merge |
| Anonymous | Existing, no anonymous alias | Merge |
| Anonymous | Existing, already has anonymous alias | Switch; no merge/transfer |
| Identified | Any | Switch; no merge/transfer |

Direct switching uses `logIn(B)`; no logout required. SDK logout generates an anonymous ID. The custom-only pattern avoids SDK logout.

The same custom ID retrieves subscriptions without restore after reinstall and across platforms; management stays on the purchasing platform. Anonymous IDs lack cross-platform sharing. Expose the current ID for support.

## C3: Store restoration versus account access

Class D, confidence M, source S3. Restore resynchronizes purchases available from the same Apple, Google, or Amazon store account. A user-triggered restore path is recommended even with application accounts. `restorePurchases` can invoke OS sign-in; programmatic migration uses `syncPurchases`, which can also alias/transfer. Consumables and non-renewing subscriptions require custom accounts for recovery because completed transactions disappear from receipts. Consumed Google one-time products can also be unavailable to restore.

Class I, confidence M, S3 + C2: a wrong store account cannot supply the missing receipt; signing into the correct app ID can nevertheless retrieve an already-known RevenueCat entitlement. iOS-to-Android receipt restoration is not entitlement sharing: use the same custom ID in the same RevenueCat project. Store-account access and app-account identity are separate proofs.

## C4: Original IDs, aliases, and recovery

Class D, confidence M, source S4. Customer profiles show original ID plus aliases; every alias references the same merged customer. Event `app_user_id` selection prefers the last-seen identified alias, then last-seen alias, then any customer ID. Alias display order is not last-seen order.

Class I, confidence M, S4: `original_app_user_id` is historical identity, potentially an anonymous alias, not necessarily the active Firebase UID or an authenticated account. It locates a RevenueCat customer, not Firebase credentials. No universal store-account-to-original-Firebase-UID recovery API was verified. Account recovery must establish the app account; receipt-based transfer is separate.

## C5: Firebase mapping and migration boundary

Class D, confidence M, source S5. RevenueCat's Firebase integration recommends Firebase UID as App User ID for Analytics and requires it for its Firebase Extension; the example calls `logIn(uid)` after Firebase auth changes. This is integration-specific guidance, not a universal requirement for every backend design.

Class I, confidence M, S5 + C2: passing a Firebase anonymous-auth UID is supplying a custom/identified RevenueCat ID. Firebase anonymity does not make it `$RCAnonymousID:`. Therefore Firebase-guest-UID G to existing account UID B follows C2's identified switch, not its anonymous merge rows.

## C6: Anonymous registration and existing-account sign-in

Class D, confidence M, source S6. Firebase linking lets providers retain the same Firebase UID, including linking an anonymous account to a provider. Linking fails when credentials already belong to another account; application data merging needs explicit handling. The page notes a known linking issue for some projects; no affected SDK/version compatibility was established here.

Class I, confidence M, S6 + C5: if registration links credentials to guest G and preserves UID G, RevenueCat identity remains G; no purchase transfer is needed. Signing into existing UID B instead changes identity and needs separate entitlement reconciliation. Firebase data merging alone does not establish a RevenueCat merge or transfer.

## C7: Support and expiration

Class D, confidence M, source S7. Lost access may mean changed IDs or expiration. Restore can recover active purchases under the configured behavior. RevenueCat describes receipt claims by another app ID as a store-owner recovery case, because transactions belong to the store account; transfer settings still control the result.

Class I, confidence M, S7 + C1: T minimizes forgotten-account friction but permits same-store account moves and associated A/B support disputes. K shifts these cases to app-account recovery. N blocks moves while active, so it does not by itself recover an active subscriber whose guest custom UID was lost. Transferring expired history does not renew an expired subscription or grant active premium access.

## C8: Legacy-to-new compatibility

Class D, confidence M, source S8. Vendor migration guidance prefers server import of saved receipts/tokens; client migration sees only subscribers who open the updated app. A targeted initial sync is acceptable; repeated sync/restore on every launch is discouraged. Historical Google purchase availability is limited.

Class I, confidence M, S8 + C2: preserving the old customer ID avoids unnecessary identity conflicts. A new ID is not automatically compatible with an old identified ID. Distinguish importing purchases into RevenueCat from re-identifying existing RevenueCat customers. If old IDs are abandoned, prove mapping or policy-controlled receipt recovery before replacement; legacy anonymous and identified populations need separate checks.

## Remaining scenario comparison

These are inferences, confidence M, under the cited claims. No assumption of existing TellPal guest purchasing is made.

| Scenario | Outcome / distinction | Evidence |
|---|---|---|
| A buys; B restores, same store | C1 matrix; C2 login | S1, S2 |
| RevenueCat anonymous buys; registers new custom account | C2 new-target branch | S2 |
| RevenueCat anonymous buys; signs into existing custom account | C2 existing-alias branch; C3 restore | S2, S3 |
| Firebase guest UID buys; registers by linking | C6 | S5, S6 |
| Firebase guest UID buys; signs into existing UID | C5; C1 if restoring | S1, S2, S5 |
| Second device/reinstall, same known custom ID | C2 | S2 |
| Reinstall loses anonymous/custom guest identity | C3 receipt availability plus C1 owner class/policy | S1, S3 |
| Wrong store / cross-platform device | C3 account lookup versus store restore | S2, S3 |
| Old custom ID replaced with new custom ID | C8 compatibility must be demonstrated | S2, S8 |
| Active versus expired | C1 ownership matrix; C7 access distinction | S1, S7 |

## Conditional proposal and facts required

Class P, confidence M, C1–C8. Account-optional access favors T. N is a compromise only with proven active-subscriber recovery. K depends on registered-account continuity. L needs existing-setting evidence. Any choice needs explicit account context and backend entitlement reconciliation.

No final selection is justified until these facts are established:

- Production policy, sandbox override, and whether L is currently enabled.
- Whether guest purchasing is possible, and whether each guest uses a RevenueCat anonymous ID or a supplied Firebase UID.
- Existing identified/anonymous customer distribution, aliases, and UID retention during registration/reinstall.
- Firebase project/account continuity and old-to-new ID mapping, without assuming ongoing legacy synchronization.
- Correct RevenueCat project/app/entitlement configuration across iOS and Android.
- Server-notification tracking and store account-token alignment; S1 warns of restrictive-policy identity mismatch. Configuration uninspected.
- Active, expired, grace/billing-retry, lifetime/non-consumable, and mixed-receipt test cases. The retrieved policy text does not precisely define every borderline “active” state.
- Authenticated account-recovery support and backend handling of revoked/transferred entitlements. No webhook contract or backend mechanism was implemented or tested in this digest.

Gaps: no exact RevenueCat/Flutter SDK compatibility test; no dashboard observation; no Apple/Google primary-platform rule audit; no promise of transaction-level isolation for mixed receipts; no verified administrative ID-migration procedure. Further browsing is warranted only for a chosen option's unresolved implementation details.

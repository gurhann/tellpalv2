---
type: technical
topic: RevenueCat account identity and purchase restoration
decision: Recommend account-conflict behavior without disrupting existing subscribers
source: official documentation retrieved in this run
status: complete
created: 2026-10-02
updated: 2026-10-02
---

# RevenueCat account and restore research

## Decision summary

Changing an identified customer through login does not itself transfer purchases; store restoration applies the configured ownership policy. [1] [2]

For an account-optional purchase flow, the provisional recommendation is the vendor's default policy plus stable customer identification and backend reconciliation. A strict no-transfer policy should only be considered if registration is required before every purchase and account recovery is supported. The production dashboard setting, historical aliases and deployed purchase flow have not been inspected. The earlier no-transfer suggestion was superseded by the user’s October 2 approval of default transfer.

## Policy comparison

These policies operate at RevenueCat project scope. Sandbox may have a separate override. [1]

| Policy | Identified customer A owns the receipt; identified B restores |
| --- | --- |
| Default transfer | Ownership and entitlements move to B; A loses them. |
| Transfer only without active subscriptions | Active subscriptions block transfer; an expired subscription can allow it. |
| Original-owner retention | Restore is rejected for B; requires account recovery. |
| Legacy sharing | Restoring can alias customers; available only to projects already using it. |

The setting also affects new purchases against another identified customer's receipt. On iOS, strict retention can associate a later purchase with an old account even after expiration. [1]

## Identity boundaries

RevenueCat generates `$RCAnonymousID:` identities. Supplying a Firebase UID is a custom identity, even for Firebase-anonymous sessions (inference from these definitions). Firebase integration guidance explicitly uses the Firebase UID; preserve verified historical IDs. [2] [7]

Identified A → identified B through `logIn` does not merge purchases. For RevenueCat-anonymous → custom identity, aliasing depends on whether the target already exists and already has an anonymous alias; it is not unconditional. `logOut` creates a new anonymous identity; direct custom-account switching need not call it first. [2]

## Example scenarios

Scenario outcomes assume a correctly integrated SDK, the relevant store purchase and explicit policy selection. They are documented expectations or deductions, not executed TellPal acceptance results.

| Scenario | Expected outcome / decision point | Basis |
| --- | --- | --- |
| A buys; B signs in without restore | An identified-to-identified login alone does not transfer A's purchase. | [2] |
| A buys; B restores using the purchasing store account | Apply the selected policy above. Test both customers' resulting entitlement state. | [5] |
| Same custom A signs in on a second device or after reinstall | Retrieve A's existing entitlement; no cross-account transfer. | [2] |
| RevenueCat-anonymous purchaser creates a new custom identity | Login can alias the billing identities; application data stays separately governed. | [2] |
| RevenueCat-anonymous user signs into an already anonymous-aliased custom identity | Login switches without merging/transferring; restore is separate. | [2] |
| Firebase-anonymous UID F becomes registered without changing F | The supplied custom RevenueCat ID stays F; preserving that ID avoids introducing a new purchase identity. Verify actual UID continuity. | [6] [7]; actual registration implementation must preserve UID. |
| User selects the wrong store account and restores | Restore reads that store's purchases, not purchases belonging to arbitrary email addresses or another platform's store. Existing custom-customer entitlement lookup is separate. | [3] |
| iOS purchase, same A on Android | Same project/custom identity exposes entitlement; management stays on the purchasing platform. | [2] |

Restore should be triggered by the user; programmatic purchase synchronization has different UX expectations. A no-account restore must remain reachable when that purchase flow is supported. [3]

## TellPal recommendation and contrary considerations

Preserve historical Firebase-based RevenueCat IDs wherever verified. The legacy working-tree source observation is recorded separately in [local-context.md](./local-context.md); it does not prove historical customer aliases or live configuration.

An account-optional TellPal flow favors default transfer to reduce recovery friction. Its tradeoff is explicit: restoring under B can remove A's premium rights. A strict original-owner policy favors account separation but increases account-recovery support and can strand pre-registration purchasers. The active-only compromise cannot recover an active subscription from a lost Firebase guest custom UID by itself; prove its fit before selection. These are conditional recommendations, not approved product changes. [1]

RevenueCat aliasing is a billing-customer relationship, not permission to merge TellPal histories or profiles. That separation follows the project's account-isolation requirements and must be maintained independently of the selected billing policy.

The backend integration should reconcile both sides of a transfer. RevenueCat's `TRANSFER` webhook uses `transferred_from`/`transferred_to` arrays and is delivered for the destination; treating it as an ordinary single-user purchase event is insufficient. Match aliases as well as the original ID when resolving relevant customer events. [4]

## Remaining facts and downstream work

Before closing OQ-7 and refining Stories 6.4/6.7:

1. Resolved by the user on 2026-10-02: purchases without registration are supported in the legacy app and must remain supported. This is an application requirement, not external RevenueCat evidence.
2. Read production and sandbox restore settings and representative historical aliases; configuration is currently unknown.
3. Verify stable Firebase/RevenueCat identity through conversion, account switching and replacement.
4. Test default and intended policies with distinct customer/store identities. Validate source revocation, destination access and backend account isolation. The vendor supplies account/restore test procedures; these expectations have not been executed here. [5]

No dashboard settings or application code were changed. The subsequent user confirmation of pre-registration purchasing was added to FR-19 and Story 6.3. The restore/transfer policy remains a recommendation approved by the user on 2026-10-02; implementation verification remains pending; no dashboard setting was changed.

## Evidence quality and freshness

All service-behavior sources are official RevenueCat documentation read in this run. They are authoritative but share one publisher; confidence is medium under the research skill's independent-publisher rule. RevenueCat publication dates are not supplied; the Firebase linking page has a stated update date. Re-read the policy/identity pages before implementation or configuration change, and check real dashboard facts then. No current SDK compatibility, store-policy compliance or sandbox success is claimed.

## Source appendix

| Ref | Claim/finding | Publisher | Publication date | Accessed | Confidence |
| --- | --- | --- | --- | --- | --- |
| [1] | Restore-policy options, defaults and registration constraints | [RevenueCat: Restore Behavior](https://www.revenuecat.com/docs/projects/restore-behavior) | Not stated | 2026-10-02 | Medium; authoritative vendor documentation, no independent publisher check |
| [2] | Custom/anonymous identity, login/alias and device continuity | [RevenueCat: Identifying Customers](https://www.revenuecat.com/docs/customers/identifying-customers) | Not stated | 2026-10-02 | Medium; authoritative vendor documentation, no independent publisher check |
| [3] | Store restore operation | [RevenueCat: Restoring Purchases](https://www.revenuecat.com/docs/getting-started/restoring-purchases) | Not stated | 2026-10-02 | Medium; authoritative vendor documentation, no independent publisher check |
| [4] | Transfer events and source/destination identity arrays | [RevenueCat: Event Types and Fields](https://www.revenuecat.com/docs/integrations/webhooks/event-types-and-fields) | Not stated | 2026-10-02 | Medium; authoritative vendor documentation, no independent publisher check |
| [5] | Account/restore scenario test procedures | [RevenueCat: Testing Use Cases](https://www.revenuecat.com/docs/guides/testing-guide/use-cases) | Not stated | 2026-10-02 | Medium; authoritative vendor documentation, no independent publisher check |

| [6] | Provider linking, including anonymous account upgrade, retains Firebase UID | [Firebase: Link Multiple Auth Providers](https://firebase.google.com/docs/auth/web/account-linking) | Updated 2026-10-01 UTC, as retrieved by researcher | 2026-10-02 | Medium; primary documentation, actual TellPal flow not tested |
| [7] | Firebase UID mapping in RevenueCat Firebase integration | [RevenueCat: Firebase Integration](https://www.revenuecat.com/docs/integrations/third-party-integrations/firebase-integration) | Not stated | 2026-10-02 | Medium; authoritative vendor documentation |

## Staleness map

The staleness script calculated 2026-11-01 as the next review for the dated Firebase linking source, using a conservative one-month integration review interval. RevenueCat pages lack publication dates, so they were excluded from date arithmetic; their access date is not substituted for publication. Recheck them and the actual dashboard setting immediately before implementation or configuration changes.

## Verification record

A separate fresh-context citation checker corroborated the load-bearing policy and identity statements against official pages and identified a missing pre-purchase-registration qualifier; the report now includes it. The seven tracked claims remain `unverified` under the research skill’s independent-publisher/runtime standard, despite current authoritative documentary support. No cross-publisher or runtime verification is claimed. Mechanical citation validation found no dangling references or orphaned source rows.

## Subsequent TellPal clarification — 2026-10-02

The user confirmed that purchases without registration are already possible in the old app and must remain possible. This resolves the purchase-eligibility premise and strengthens the default-transfer recommendation. The user subsequently approved the default transfer policy. Production dashboard behavior and historical anonymous purchaser identities remain unverified.

## Product selection — 2026-10-02

The user approved the default RevenueCat transfer policy on 2026-10-02. Pre-registration purchases remain supported; a restore under another TellPal account using the purchasing store account transfers the subscription access, and the source loses that transferred entitlement. Identified login alone does not transfer purchases, and billing operations do not merge TellPal profiles/history. Production/sandbox settings, historical identity/aliases and backend reconciliation still require verification; no dashboard change or runtime acceptance is claimed.

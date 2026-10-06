# Semantic citation check

Checked: 2026-10-02. Scope: crucial RevenueCat claims in `research.md`; official pages retrieved anew in this check. No project premise was treated as external evidence. This is same-publisher documentary corroboration, not independent-publisher verification or an integration test.

## Mismatch requiring correction

- **Original-owner eligibility is understated.** The report's decision summary makes a recoverable application account the condition for considering strict retention. The source sets a narrower requirement: the policy is allowed only for apps requiring every customer to create an account **before purchasing**. Recovery alone, including registration after purchase, does not meet that documented requirement. State this explicitly in the decision summary and policy comparison. [Restore Behavior](https://www.revenuecat.com/docs/projects/restore-behavior#keep-with-original-app-user-id)

## Corroborated claims and caveat

- Default restore transfers the subscription to identified B and revokes identified A's access. The no-active-subscription option blocks transfer when the purchases contain an active subscription. The report is accurate; an optional clarification is that one-time purchases do not block transfer. [Restore Behavior](https://www.revenuecat.com/docs/projects/restore-behavior)
- Identified-to-identified `logIn` performs no merge or transfer. Anonymous-to-custom login merges for a new target or an existing target without an anonymous alias; it does not merge when the existing target already has such an alias. [Identifying Customers](https://www.revenuecat.com/docs/customers/identifying-customers)
- Firebase-anonymous UID classification is correctly marked as an inference: externally supplied IDs belong to RevenueCat's custom-ID category. Firebase UID lifecycle is not verified by this source. The same custom ID supports reinstalls and other devices/platforms within one project. [Identifying Customers](https://www.revenuecat.com/docs/customers/identifying-customers)
- The transfer webhook destination-only delivery and `transferred_from`/`transferred_to` arrays are supported. Searching original IDs and aliases is documented for relevant subscriber-identity events. [Event Types and Fields](https://www.revenuecat.com/docs/integrations/webhooks/event-types-and-fields)

No other mismatch found within this limited scope. Restoring Purchases and Testing Use Cases were not rechecked.

# Function and domain recon

Read only the assigned route, UI components, tests, and named model/API/ADR sources. Inventory visible and reachable functions before judging design. A function may be an action, navigation, field, state, asset operation, or visibility rule.

Return ONLY this JSON object, with no markdown or prose:

```json
{"functions":[{"id":"","purpose":"","trigger":"","result":"","evidence_ids":[]}],"evidence":[{"id":"","source":"","claim":"","verdict":"VERIFIED|UNVERIFIED"}],"gaps":[""]}
```

Never infer a product capability from a label or fixture. Use `UNVERIFIED` when a source does not establish the claim.


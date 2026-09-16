# Review artifact contract

Each run folder contains machine-checkable JSON plus a human summary. IDs are stable kebab-case strings and references use those IDs, not copied prose.

`function-map.json`

```json
{"screen":"contents-detail","functions":[{"id":"manage-localized-cover","purpose":"Replace a locale-specific cover","trigger":"Asset management button","result":"Selected cover is shown for the locale","evidence_ids":["content-localization-cover"]}]}
```

`domain-evidence.json`

```json
{"evidence":[{"id":"content-localization-cover","source":"cms/src/features/...","claim":"A localization has a cover asset","verdict":"VERIFIED"}]}
```

`findings.json`

```json
{"findings":[{"id":"duplicated-readiness","severity":"medium","user_impact":"Repeated status cards hide asset actions","affected_function_ids":["manage-localized-cover"]}]}
```

`change-plan.json`

```json
{"changes":[{"id":"compact-readiness","finding_id":"duplicated-readiness","scope":"mockup","preserved_function_ids":["manage-localized-cover"],"evidence_ids":["content-localization-cover"],"change":"Keep asset cards; replace duplicate status grid with compact chips."}]}
```

`screen-review.md` links visual output and marks every unverified item as an open question. `verification.json` records test commands, viewport coverage, and independent verdict.


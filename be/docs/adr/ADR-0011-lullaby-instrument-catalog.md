# ADR-0011: Lullaby Instrument Catalog Ownership

## Status

Accepted

## Context

Lullaby instrument metadata is shared by every localization of a lullaby. Storing it on a
localization would allow the same recording to expose different instrument selections per
language and would make ordering difficult to keep stable. The app also needs a controlled set of
instrument names rather than free-text contributor values.

## Decision

The `content` module owns a stable instrument catalog and content-level `LullabyInstrument`
links. Catalog `code` values are language-independent identities. Localized display names live in
`instrument_catalog_localizations`. A lullaby stores a complete, unique permutation of selected
catalog codes using zero-based `display_order` values. Selection is changed atomically and is not
copied to content localization rows.

The `MUSICIAN` contributor relationship remains a separate domain concept. Public/mobile
responses and catalog administration are outside this decision and are delivered by later work.

## Consequences

- All localizations of one lullaby observe the same ordered instrument selection.
- Admin reads must resolve display names using an explicitly requested supported locale.
- Adding, retiring, translating, or deleting catalog entries requires catalog-management policy
  and is intentionally separate from selection/reorder behavior.

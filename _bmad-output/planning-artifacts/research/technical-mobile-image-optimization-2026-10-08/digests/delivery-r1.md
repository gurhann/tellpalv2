# Delivery and geometry primary-source digest

Accessed: 2026-10-08. Round1, primary sources; no deployment action.

## D1 — Storage cache policy is an explicit server concern
Claim: Cloud Storage built-in edge caching requires publicly accessible objects plus further conditions. `private` permits requester-local cache but excludes Cloud Storage cache; `no-cache` allows storage with revalidation; `no-store` prevents caching. A signed URL alone is not evidence that this built-in public cache applies.
Source: https://docs.cloud.google.com/storage/docs/caching ; Google Cloud; last updated2026-10-07; class=integration; confidence=medium(single publisher).

## D2 — Metadata must be verified/configured
Claim: Cloud Storage sets response Cache-Control from stored object metadata, ignoring the request's Cache-Control. Content-Type metadata may differ from actual bytes. Inherently compressed media generally should not receive an additional gzip layer. An ETag must not be assumed to equal a content checksum.
Source: https://docs.cloud.google.com/storage/docs/metadata ; Google Cloud; last updated2026-10-07; class=integration; confidence=medium(single publisher).

## D3 — HTTP directives corroboration
Claim: RFC9111 distinguishes private,no-cache,no-store; private addresses shared cache storage and is not a complete content privacy/access mechanism. Policy for protected locally cached media remains an application decision.
Source: https://datatracker.ietf.org/doc/html/rfc9111 ; IETF; published2022-06; class=normative-standard; confidence=medium pending exact section spot-check.

## D4 — Flutter target pixel semantics and codec caveat
Claim: instantiateImageCodec sizes outputs in image pixels; specifying one dimension preserves ratio. Docs describe scaling after decoding, so an output target alone does not prove absence of full-size intermediate decode. Flutter documents JPEG,PNG,GIF,WebP,BMP/WBMP; extra formats depend on platform fallback. AVIF is not in this portable documented list.
Source: https://api.flutter.dev/flutter/dart-ui/instantiateImageCodec.html ; Flutter; publication date unknown; retrieved current2026-10-08; class=compatibility/decoder; confidence=medium single authoritative source.

## Synthesis proposals, not measured findings
Use immutable versioned output objects and explicit storage/cache metadata, preserving server admission and account/access-scoped local integrity semantics. Prioritize proper rendition/decode sizing before introducing CDN infrastructure. A public long-cache policy must not be copied into private story media by implication. Candidate width buckets and 32-bit decoded pixel calculations are mathematical design tools, not measured savings or final budgets. Round stopped on coverage; CDN pricing and deployment deliberately out of scope.

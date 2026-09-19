# Category Import Mapping

This companion records the source profile, target-field mapping, provisional multilingual grouping, and review blockers for `tellpal_public_categories.csv`.

## Source profile

| Measure | Result |
|---|---|
| Source columns | `id`, `name`, `description`, `language`, `type`, `image_url` |
| Rows | 105 |
| Languages | `tr` 30, `de` 26, `pt` 25, `en` 24 |
| Source types | `STORY` 93, `MEDITATION` 4, `LULLABY` 4, `AUDIO_STORY` 4 |
| Unique source IDs | 105 |
| Candidate aggregates | 33 |
| Candidate localizations | 105 |

## Target mapping

| Source field or concept | v2 target | Rule |
|---|---|---|
| `id` | External import mapping only | Do not use as v2 category ID; retain for audit and rerun reconciliation. |
| `name` | `category_localizations.name` | Trim whitespace; keep the source language and editorial copy. |
| `description` | `category_localizations.description` | Trim whitespace; preserve non-empty descriptions. |
| `language` | `category_localizations.language_code` | Supported values in this file are `tr`, `en`, `pt`, and `de`. |
| `type=STORY` | `categories.type=STORY` | Direct canonical mapping. |
| `type=MEDITATION` | `categories.type=MEDITATION` | Direct canonical mapping. |
| `type=LULLABY` | `categories.type=LULLABY` | Direct canonical mapping. |
| `type=AUDIO_STORY` | `categories.type=STORY` plus `audio-books` identity | Preserve `AUDIO_STORY` in source provenance; do not write it to the canonical type column. |
| `image_url` | `category_localizations.image_media_id` | Register or reuse a v2 media asset first; never persist the token-bearing URL. |
| Derived group key | `categories.slug` | One stable slug per reviewed multilingual group. |
| Missing `premium` | Import policy | Must be explicitly supplied; do not infer from the CSV. |
| Missing `active` | Import policy | Must be explicitly supplied; do not infer from the CSV. |
| Missing `status` | Localization import policy | `DRAFT` is safe only if accepted as policy; `PUBLISHED` requires `publishedAt`. |
| Missing `publishedAt` | Localization import policy | Required whenever status is `PUBLISHED`. |

## Provisional group candidates

The source IDs below are assigned exactly once. `High` means the names/descriptions are strong translation or localization matches. `Review` means the semantic match is plausible but needs editorial approval.

| Candidate slug | Source IDs | Languages | Confidence / note |
|---|---:|---|---|
| `audio-books` | 29, 31, 38, 163 | tr, en, pt, de | High; separate category identity, canonical type `STORY`. |
| `lullaby-and-relaxing-music` | 14, 15, 45, 156 | tr, en, pt, de | High. |
| `meditation` | 28, 32, 37, 162 | tr, en, pt, de | High. |
| `popular-favorites` | 1, 24, 84, 150 | tr, en, pt, de | Review; editorial wording differs by locale. |
| `family-and-friendship` | 13, 16, 88, 157 | en, tr, pt, de | High. |
| `empathy-and-tolerance` | 54, 55, 99, 169 | tr, en, pt, de | High. |
| `activities-and-fun` | 17, 18, 87, 158 | tr, en, pt, de | High. |
| `informative-stories` | 5, 19, 86, 153 | tr, en, pt, de | High. |
| `nature-and-discovery` | 10, 11, 89, 155 | tr, en, pt, de | High. |
| `sleeping-stories` | 22, 30, 82, 160 | tr, en, pt, de | High. |
| `serial-stories` | 34, 35, 81, 164 | tr, en, pt, de | High. |
| `earthworm-series` | 3, 60, 92, 152 | tr, en, pt, de | High. |
| `sharing-and-cooperation` | 50, 59, 98, 165 | tr, en, pt, de | Review; Portuguese uses `Solidariedade`. |
| `emotional-awareness` | 51, 58, 94, 166 | tr, en, pt, de | High. |
| `problem-solving` | 52, 57, 100, 167 | tr, en, pt, de | High. |
| `confidence-and-courage` | 53, 56, 101, 168 | tr, en, pt, de | High. |
| `science` | 61, 62, 97, 170 | tr, en, pt, de | Review; Portuguese adds technology. |
| `imagination-and-creativity` | 64, 66, 95, 171 | tr, en, pt, de | Review; Portuguese shortens the label to imagination. |
| `competition` | 65, 67, 96, 172 | tr, en, pt, de | High. |
| `magic-words` | 102, 103, 106, 173 | pt, tr, en, de | High. |
| `animated-stories` | 20, 21, 159 | tr, en, de | Partial; Portuguese localization absent. |
| `stories-with-songs` | 27, 93, 161 | tr, pt, de | Partial; English localization absent. |
| `brand-new-stories` | 104, 105, 174 | tr, en, de | Partial; Portuguese localization absent. |
| `animal-friends` | 2, 151 | tr, de | Partial; English and Portuguese matches not found. |
| `parent-favorites` | 6, 154 | tr, de | Partial; English and Portuguese matches not found. |
| `editors-pick` | 9, 90 | en, pt | Partial; Turkish and German matches not found. |
| `english-stories` | 7, 91 | tr, pt | Partial; English and German matches not found. |
| `adventure-stories` | 25, 83 | en, pt | Partial; Turkish and German matches not found. |
| `hello-summer` | 107, 175 | tr, de | Partial; English and Portuguese matches not found. |
| `choice-stories` | 4 | tr | Single-language category. |
| `special-occasions` | 12 | tr | Single-language category. |
| `recommendations` | 26 | tr | Single-language category. |
| `library-discovery` | 80 | pt | Single-language category. |

## Data-quality findings

- Image paths are not a safe grouping key: 17 image paths are reused across unrelated concepts. `doğa ve canlılar.png` spans nature, science, serial, sharing, and earthworm rows; `aktivite ve eğlence.png` spans activities, serial, science, magic words, and competition rows.
- Source row `37` (Portuguese Meditation) points to the audio-books image; source row `38` (Portuguese Audio Books) points to the meditation image.
- Source row `15` has a leading space in its description; source row `35` has a trailing space. The importer must normalize surrounding whitespace.
- The four `AUDIO_STORY` rows are semantically one separate Audio Books category candidate, not four canonical category types.

## Import boundary

1. Validate and normalize the CSV without writing v2 data.
2. Produce a reviewed mapping manifest with one group per stable slug and one localization per source row.
3. Resolve source images through backend-mediated media registration and record the resulting asset IDs.
4. Create or reconcile category aggregates by slug.
5. Create or reconcile localizations by category and language.
6. Emit source-to-category and source-to-localization audit mappings.
7. Handle category-content membership, display order, and Audio Books narration-readiness filtering in a later curation phase.

The current admin API exposes category and localization operations separately; no bulk category-import endpoint is part of the current contract.

# TellPal Story Import Contract

## Input contract

```text
story-directory/
├── metadata.csv
├── tr|en|es|pt|de/
│   ├── one *.docx
│   ├── a name containing kapak|cover with jpg|jpeg|png|gif
│   ├── 1..N-prefixed jpg|jpeg|png|gif
│   └── 1..N.mp3
└── yazısız|yazisiz/
    ├── a name containing kapak|cover with jpg|jpeg|png|gif
    └── 1..N-prefixed jpg|jpeg|png|gif
```

Required CSV columns are `id,is_publish,name,summary,page_count,author,dubbing,illustrator,duration,age_range`. Ignore `id` after validation. Require equal `page_count` and `age_range` values across rows. Map a language directory to its row by matching the DOCX first non-empty paragraph to `name` after trim, NFC normalization, and case-folding.

When a DOCX has no matching internal title but can be parsed into exactly `page_count` blank-separated blocks or exactly `page_count` non-empty paragraphs, first match a metadata title uniquely contained in its normalized filename. If no filename match exists, match it only if all titled/filename-matched DOCX files leave exactly one language directory and exactly one metadata row unresolved. Otherwise stop as ambiguous.

When multiple metadata rows have the same normalized title, require repeatable `--metadata-row LANGUAGE=LEGACY_ID` arguments for those languages on both inspection and import. Require each language and legacy ID to exist and prohibit assigning one ID to multiple languages. The legacy ID is selection-only and must not be included in API payloads.

Parse titled or titleless DOCX pages from decimal Word automatic-list numbering and ordered text markers such as `1. Text`, `2.Text`, `3 Text`, a standalone number paragraph, or a page number followed by a Word line break and text in the same paragraph. Treat unmarked paragraphs after a page start as continuations of that page until the next sequential marker. Respect numbering starts and per-list counters. As deterministic titleless fallbacks, accept blank-paragraph-separated blocks or non-empty paragraphs only when their count equals `page_count`. Preserve page paragraph order and internal line breaks, join block paragraphs with newlines, and do not translate or rewrite text.

Reject empty media and files whose bytes do not match their declared JPEG, PNG, GIF, or MP3 format before any remote write. Match cover candidates by the standalone `kapak` or `cover` token and page candidates by a leading page integer. Prefer an exact numeric filename, then a `revize`/`revised` candidate, then another decorated filename; stop when multiple candidates remain at the same best priority. Show non-exact choices and ignored lower-priority candidates in preflight warnings. Extra files are not uploaded, but remain part of the whole-folder fingerprint so any source-folder change invalidates approval.

If visual inspection establishes that textless sources are misnumbered, use repeatable `--textless-page PAGE=RELATIVE_PATH` arguments on both inspection and import. Overrides must remain inside the selected textless directory, must refer to valid images, must cover pages within `1..page_count`, and cannot assign one file to multiple pages. Include every override in the preview warnings; never rename or modify the source folder.

If the operator explicitly approves a story that has no textless media, use `--allow-missing-textless` on both inspection and import. Permit it only when the textless directory is empty or absent. Do not upload or attach textless assets, require stored `textlessCoverMediaId` and every `textlessIllustrationMediaId` to remain null, and reject partially populated textless media or simultaneous `--textless-page` overrides.

Generate the external key from the required Turkish title by mapping Turkish characters to ASCII, removing apostrophes/punctuation, and collapsing separators: `story.<lowercase-kebab-slug>`.

## Authentication

- `POST /api/admin/auth/login`: `{username,password}` → access/refresh token pair.
- `POST /api/admin/auth/refresh`: `{refreshToken}` → rotated pair. Retry the rejected request once after a definitive `401`.
- `POST /api/admin/auth/logout`: `{refreshToken}` → `204`; call best-effort at the end.

Keep all tokens in memory only.

## Read-only remote preflight

- `GET /api/admin/contents`: compare every returned `externalKey` exactly; any match blocks the run.
- `GET /api/admin/contributors?q=<encoded-name>&limit=100`: reuse one case-insensitive exact `displayName` match, plan a create for zero matches, and block on multiple exact matches.

No content, media, localization, page, or contributor mutation may occur before the user enters the standalone `import` confirmation keyword and the source fingerprint is rechecked.

## Mutation order and payloads

1. Create the story:
   - `POST /api/admin/contents`
   - `{type:"STORY",externalKey,ageRange,active}`
   - Set `active=true` when at least one row has `is_publish=true`.
2. Upload each localized cover:
   - `POST /api/admin/media/uploads` as multipart fields `kind`, `checksumSha256`, `file`.
   - Use `ORIGINAL_IMAGE` for JPG/JPEG/PNG/GIF and `ORIGINAL_AUDIO` for MP3.
3. Create each localization:
   - `POST /api/admin/contents/{contentId}/localizations/{languageCode}`
   - `{title,description,bodyText:null,coverMediaId,audioMediaId:null,durationMinutes,status:"DRAFT",processingStatus:"PENDING",publishedAt:null}`
4. Append `page_count` pages sequentially:
   - `POST /api/admin/contents/{contentId}/story-pages`
   - `{afterPageNumber:null}`
   - Require returned `pageNumber` to equal the expected sequence.
5. Upload each localized page image and MP3, then upsert:
   - `PUT /api/admin/contents/{contentId}/story-pages/{pageNumber}/localizations/{languageCode}`
   - `{bodyText,audioMediaId,illustrationMediaId}`
6. Upload and attach textless media:
   - `PUT /api/admin/contents/{contentId}` with `{externalKey,ageRange,active,textlessCoverMediaId}`.
   - `PUT /api/admin/contents/{contentId}/story-pages/{pageNumber}` with `{textlessIllustrationMediaId}`.
7. Resolve/create contributors and assign them:
   - `POST /api/admin/contributors` with `{displayName}` only when remote search has zero exact matches.
   - `POST /api/admin/contents/{contentId}/contributors` with `{contributorId,role,languageCode,creditName:null,sortOrder:0}`.
   - Map `author→AUTHOR`, `illustrator→ILLUSTRATOR`, `dubbing→NARRATOR`.
   - Use `languageCode:null` only when the normalized name for that role is identical across all rows; otherwise create one localized assignment per row.
8. Verify all stored state, then publish only rows where `is_publish=true`:
   - `POST /api/admin/contents/{contentId}/localizations/{languageCode}/publish`
   - `{publishedAt:null}`

## Verification

Before publication, require:

- `GET /api/admin/contents/{contentId}` to match base fields, localized metadata, cover IDs, `DRAFT`, and `PENDING`.
- `GET /api/admin/contents/{contentId}/story-pages` to contain exactly `1..N`, all localized text/media IDs, and all textless IDs.
- `GET /api/admin/contents/{contentId}/contributors` to match the planned role/language scopes.
- `GET /api/admin/media/{assetId}` for every uploaded asset to match ID, kind, byte size, and lowercase SHA-256.

After publication, repeat content/page/contributor reads. Require requested localizations to be `PUBLISHED` and unrequested localizations to remain `DRAFT`. Accept `processingStatus=PENDING` or `COMPLETED` because the backend may finish media processing during the import; never mutate that status from the importer. Reject failure or unknown processing states.

## Failure policy

Persist `manifest.json` and `result.json` outside the repo/source folder. Record content ID, uploaded asset IDs, completed phases, and the last method/path, but never credentials or tokens.

On any mutation error, stop immediately. Because the API has no asset delete endpoint and external keys remain unique, do not roll back or resume automatically. The operator must inspect and complete or clean up the partial state manually.

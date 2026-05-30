# Changelog

## Week Ending 2026-05-23

### Highlights

- Textless story content support expanded across the backend and CMS: content
  records gained textless cover export data, the CMS added dedicated textless
  cover editing flows, and follow-up story page structure changes aligned the
  content detail and story page experiences. Supported by `bbcd61b`,
  `631dfaa`, and `67bb717`.
- Story page admin flows were hardened around incomplete media data by handling
  missing illustration IDs in the CMS API layer, while backend verification was
  stabilized by isolating the admin auth rate-limit integration test. Supported
  by `631dfaa` and `ede0e93`.

### Important Links

- No PR numbers were recorded in the local git history for this period, so the
  links below point to the supporting commits and merge record.
- [bbcd61b](https://github.com/gurhann/tellpalv2/commit/bbcd61b056edeae411931b2645b7041bb690b22c) Add textless cover export to story content
- [67bb717](https://github.com/gurhann/tellpalv2/commit/67bb7176fc96c782968962f178ffae8454c03c31) changed textless images structure
- [631dfaa](https://github.com/gurhann/tellpalv2/commit/631dfaa826c524bc829cde2171e2d9fb3d0c6624) Handle missing story page illustration IDs
- [ede0e93](https://github.com/gurhann/tellpalv2/commit/ede0e938d29c0e7329e11e618eb1037a0e313a40) Isolate admin auth rate limit integration test
- [7579d40](https://github.com/gurhann/tellpalv2/commit/7579d4029438f3b6d41e9a940704d46c014f26e6) Merge branch 'codex/daily-bug-scan'

## Week Ending 2026-05-16

### Highlights

- Story page editing and preview flows were expanded across the CMS with modal
  navigation guards, dirty-close protection, and a new story content preview
  player, then tightened with follow-up layout refinements. Supported by
  `cc7b401`, `e148ee6`, `7276bd2`, `0c492a1`, and `ad1b865`.
- Asset handling became more resilient with drag-and-drop uploads in asset
  pickers, CMS audio preview stability fixes, fallback audio preview loading,
  and Railway proxy-aware backend preview URLs. Supported by `6e395f1`,
  `206030a`, `1399d0d`, and `6627ee4`.
- Content operations documentation added a manual story inventory workflow with
  checked-in output artifacts for the recorded runs. Supported by `63cbe14`.
- Backend and CMS asset workflows were refactored to support backend-mediated
  media upload and preview flows, with updated docs and Railway operational
  notes. Supported by `4ff2eaa`.

### Important Links

- No PR numbers or merge references were recorded in the local git history for
  this period, so the links below point to the supporting commits.
- [4ff2eaa](https://github.com/gurhann/tellpalv2/commit/4ff2eaa0cbb956d22ef142befe4b61a0fffdaf4b) Refactor content workflows and backend APIs
- [6627ee4](https://github.com/gurhann/tellpalv2/commit/6627ee4079221442a34c0eb285517ef2b79dbe68) Fix backend preview URLs behind Railway proxy
- [6e395f1](https://github.com/gurhann/tellpalv2/commit/6e395f167e06a553f206746967b4cdb7aba3e53b) Add drag-drop uploads to asset pickers
- [7276bd2](https://github.com/gurhann/tellpalv2/commit/7276bd2729149c48b69099e8697375aeeba3fbce) Add story content preview player
- [ad1b865](https://github.com/gurhann/tellpalv2/commit/ad1b86551c5c5fe8ee0a29f1dce059315d4e6d17) refine story preview player layout

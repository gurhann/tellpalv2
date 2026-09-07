## Deferred from: code review of spec-1-1-playback-isleme-hedeflerini-guvenilir-bicimde-ayirma (2026-09-06)

- `AssetProcessingPathBuilder` does not reject separator or traversal-like characters in `externalKey`; this predates Story 1.1 and is deferred for a dedicated storage-path hardening task.

## Deferred from: code review of spec-1-3-ortak-textless-kapak-sahipligi (2026-09-07)

- `planning-artifacts/epics.md` and the historical architecture spine still describe `textlessCoverMediaId` as the shared audio/lullaby/meditation cover. This predates Story 1.3; the canonical epic context, spec, ADR-0010, and project memory now carry the corrected ownership split.

## Deferred from: code review of spec-1-3-ortak-textless-kapak-sahipligi (2026-09-07)

- The full PUT metadata flow can overwrite a newer cover edited in another tab because the form submits a previously read source/listening-cover snapshot. This is pre-existing whole-record update behavior and needs a separate optimistic-locking or PATCH decision.
- The generic asset picker allows the parent metadata submit while a direct upload is still pending. This is pre-existing upload UX behavior and needs a dedicated coordination task.
- `_bmad-output/specs/spec-shared-textless-cover/SPEC.md` still describes one shared textless cover for audio stories, meditation, and lullabies. It is historical planning material; superseding or removing stale ownership documents is deferred.

## Deferred from: code review of spec-1-4-ninninin-ortak-playbackini-ve-baslik-only-localizationini (2026-09-07)

- Public/mobile registry and asset-bundle consumers still resolve localization-scoped processing. Story 1.4 intentionally limits the change to the admin content contract; mobile/public endpoint migration is a later roadmap item.
- The existing non-STORY processing worker requires an image listening cover, while Story 1.3 keeps that cover optional. Supporting coverless shared playback requires a separate asset-processing decision and is deferred without changing the new playback ownership model.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: Katalog enstrümanlarını emekliye ayırma, kullanılmayan kaydı silme ve bunların ayrı katalog yönetim yüzeyini sonraki çalışmaya bırak.
  evidence: Story 1.5’in ana teslimi ninni seçimleri ve sıralamasıdır; katalog yönetimi planlama notlarında ertelenmiş, bu nedenle mevcut spec’in 1.600 token sınırını korumak için ayrıştırılmıştır.

## Deferred from: review of spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme (2026-09-07)

- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: Katalog için EN/ES/PT/DE etiketlerinin seed edilmesi ve katalog localization yönetimi sonraki katalog yönetimi çalışmasına bırakıldı.
  evidence: `LanguageCode` bu dilleri destekliyor ancak V26 yalnızca TR etiketleri seed ediyor; locale etiketi eksik olduğunda admin read bilinçli olarak 400 dönüyor.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: LULLABY yayınlanabilirlik/readiness akışına “en az bir enstrüman” kuralının eklenmesi sonraki yayınlama çalışmasına bırakıldı.
  evidence: Mevcut `ContentPublicationPolicy` non-STORY içerikleri ortak akıştan geçiriyor ve Story 1.5 yalnızca katalog/selection/admin sözleşmesini kapsıyor; public/mobile yayın kapsamı da açıkça dışarıda.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: Katalog kaydının pasifleştirilmesinin mevcut seçimlere etkisi ve veritabanı seviyesinde aktif katalog zorlaması ayrı emeklilik politikasıyla ele alınacak.
  evidence: Selection write aktif olmayan kodu reddediyor; ancak emeklilik/silme davranışı frozen scope içinde deferred-work’e taşındı.

## Deferred from: code review of spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme (2026-09-07)

- source_spec: `spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: LULLABY yayınlama readiness akışında en az bir enstrüman zorunluluğu ayrı yayınlama çalışmasına bırakıldı.
  evidence: `ContentPublicationPolicy` non-STORY akışını mevcut haliyle kabul ediyor; Story 1.5 frozen scope’u selection ve admin sözleşmesiyle sınırlı, public/mobile yayın kapsamı dışarıda.
- source_spec: `spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: Pasif katalog kaydının mevcut linklere ve doğrudan SQL yazımlarına etkisi ayrı katalog emeklilik politikasıyla ele alınacak.
  evidence: Uygulama yeni seçimlerde inactive kodu reddediyor, fakat retire/delete davranışı bu story’de açıkça deferred-work’e taşındı.
- source_spec: `spec-1-5-ninni-icin-katalogdan-sirali-enstruman-secme.md`
  summary: Admin content read içindeki çoklu collection fetch-join performans optimizasyonu ayrı read-model çalışmasına bırakıldı.
  evidence: Yeni enstrüman collection’ı mevcut localization fetch-join’ine eklendi; davranışsal doğruluk korunuyor, ancak yüksek localization/enstrüman sayılarında Cartesian sonuç üretme riski performans çalışması gerektiriyor.

- source_spec: `spec-1-6-canonical-audio-story-turunu-guvenle-kaldirma.md`
  summary: Public/mobile `experienceType: AUDIO_STORY` projection regression coverage sonraki endpoint/projection yol haritası çalışmasına bırakıldı.
  evidence: Story 1.6 documented projection label’ı koruyor ancak public/mobile projection endpoint’lerini yeniden tasarlamıyor veya eklemiyor; bu story’de böyle bir implementation surface değişmedi.

## Deferred from: code review of spec-1-6-canonical-audio-story-turunu-guvenle-kaldirma (2026-09-07)

- CMS asset-processing request/response şemasında content-scoped job için nullable `languageCode` ve `targetScope/kind` alanlarının desteklenmemesi ayrı processing-contract çalışmasına bırakıldı; bu davranış Story 1.6 öncesinden geliyor.
- V13 `page_count` ve V22 parent/trigger invariant’larının migration regression kapsamının genişletilmesi ayrı schema-hardening çalışmasına bırakıldı; V28 bunları değiştirmiyor.
- `ops/content-migration` çıktısındaki legacy `AUDIO_STORY` category map davranışı import-pipeline çalışmasına bırakıldı; Story 1.6 legacy importu kapsam dışı tutuyor.
- Public/mobile `experienceType: AUDIO_STORY` projection regression coverage sonraki endpoint/projection yol haritasına bırakıldı; bu story public/mobile endpoint yüzeyi eklemiyor.
- V28’in ilk blocker sınıfında durup diğer sınıfları sonraki çalıştırmaya bırakması fail-fast tasarım tercihi olarak korundu.

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

- source_spec: `_bmad-output/implementation-artifacts/spec-1-7-cms-te-playback-ve-localization-editorlerini-ayristirma.md`
  summary: STORY narration kaydını formdan temizleyerek mevcut narration child’ını silme davranışı ayrı backend komutuyla ele alınacak.
  evidence: Mevcut admin upsert sözleşmesi null/omitted narration için no-op yapıyor; CMS bu story’de yeni delete endpointi icat etmedi.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-7-cms-te-playback-ve-localization-editorlerini-ayristirma.md`
  summary: LULLABY detail visual baseline’larının 390/768/1280/1440 viewport’larında koşturulması Playwright Chromium kurulumu sonrasına bırakıldı.
  evidence: Component/interaction testleri geçti, ancak bu makinede Chromium executable bulunmadığı için mevcut visual runner başlatılamadı.

- source_spec: `_bmad-output/implementation-artifacts/spec-meditation-staged-import.md`
  summary: Importer Python testlerinin CI iş akışına eklenmesi ayrı bir pipeline kapsamı çalışmasına bırakıldı.
  evidence: Mevcut backend workflow yalnızca `be/**` değişikliklerinde Maven verify çalıştırıyor; `.codex` importer testleri bu değişiklikten önce de CI’da koşulmuyordu.

- source_spec: `_bmad-output/implementation-artifacts/spec-story-page-media-delivery.md`
  summary: Story page rendition profile changes need an explicit reprocessing operation for unchanged page sources.
  evidence: Profile version participates in the output revision, but completed jobs cannot be rescheduled unless the source snapshot changes; this slice covers the initial profile, not profile rollout and regeneration operations.

- source_spec: `_bmad-output/implementation-artifacts/spec-android-launcher-pairing-lifecycle.md`
  summary: Investigate scoped reporting of uncaught Android experiment Activity constructor/lifecycle exceptions without swallowing unrelated framework failures.
  evidence: API 33 ActivityThread routes uncaught construction/onCreate exceptions through Instrumentation.onException; the inherited default returns false and can terminate the process before fixed launcher refusal output. This path also exists in baseline ad9a7c48fc9d588677efc7fcbc6b9836855653e1. Synthetic lifecycle injection does not establish a physical framework exception receipt; handling it requires a separate ownership design.

- source_spec: `_bmad-output/implementation-artifacts/image-optimization/spec-1-6-reader-resolution-extension.md`
  summary: Preserve bounded process-observation errors when owned process-group cleanup yields EPERM.
  evidence: The unchanged `capture_observation` function in `native_entry_driver.py` raised `PermissionError` from its `finally` cleanup during the first post-review 312-test host run, masking the expected bounded-output `EntryError`. Its function bytes equal baseline `4721dccf090b29ce7b2d773d789873dbfbb324ae`; the same isolated test and a complete 312-test rerun passed without source changes. The original failed run and audit remain retained; audit SHA-256 `66c967173ef835dd277e26ba15c76d603c17f0090f8be0f2aa32c1694aad6096`. Root cause remains unavailable.

- source_spec: `_bmad-output/implementation-artifacts/spec-p1-image-revision-readiness-contract.md`
  summary: Repair the pre-existing canonical architecture link to the missing backend tasks document.
  evidence: `architecture.md` links `.kiro/specs/tellpal-v2-backend/tasks.md`, which is absent; the link predates the P1 baseline124a5b22fd324793a5d14220715c839617b6848b.

- source_spec: `_bmad-output/implementation-artifacts/spec-p3-reading-publication-orchestration.md`
  summary: Qualify the existing broader CMS visual suite on macOS without blanket baseline regeneration.
  evidence: The incidental 31-test P3 review invocation reported 15 passes and 16 failures: four missing Darwin category/contributor baselines (the baseline Git tree contains win32 only) and twelve 1–13-pixel registry/mockup differences on unchanged routes/shared layout code; raw log and images remain at /private/tmp/p3-review-visual.log and /private/tmp/p3-review-unrelated-visual-artifacts/. The required P3 content-detail checks passed separately.


- source_spec: `_bmad-output/implementation-artifacts/spec-p4b5-devtools-consumer-observation.md`
  summary: P4b5 gerçek başlangıç kök nedenleri ve #94 detail/reader teslimat/yaşam döngüsü ölçümü kullanıcı kararıyla teknik borca alındı.
  evidence: 2026-10-20 tarihinde kullanıcı ek deneme hattını kapatıp sonraki adıma geçmeyi istedi. Yerel tanı testleri geçti; son gerçek deneme FAILED_NO_RETRY, kesin host/support nedeni ve gerçek mobil kabul açık. Açık talep olmadan yeni başlangıç/ölçüm yok; eski kayıtlar, tüketim ve tavanlar korunur. P4b veya üretim kabulü tamamlanmış sayılmaz.

---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories]
currentStep: step-04-final-validation
status: story-drafting-complete-final-validation-pending
inputDocuments:
  - "_bmad-output/planning-artifacts/mobile-evidence/search-2026-09-27/huzur-meditation-lullabies.jpg"
  - "_bmad-output/planning-artifacts/mobile-evidence/search-2026-09-27/huzur-reading-listening.jpg"
  - "_bmad-output/planning-artifacts/mobile-evidence/search-2026-09-27/uykusuzlar.jpg"
  - "AGENTS.md"
  - "be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md"
  - "_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/prd.md"
  - "_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/addendum.md"
  - "_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/extract-legacy-flows.md"
  - "_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/extract-contracts.md"
  - "_bmad-output/planning-artifacts/prds/prd-tellpalv2-2026-09-20/reconcile-discovery.md"
  - "_bmad-output/planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md"
  - "app/old/docs/design-system/README.md"
  - "app/old/docs/design-system/DESIGN.md"
  - "app/old/docs/design-system/EXPERIENCE.md"
  - "app/old/docs/design-system/SCREENS.md"
  - "app/old/docs/design-system/COVERAGE.md"
  - "app/old/docs/design-system/TABLET.md"
  - "app/old/docs/design-system/ASSET-MAP.md"
  - "app/old/docs/design-system/RAW-SCREENSHOTS.md"
  - "/Users/gurhankucuk/.codex/worktrees/ios-upgrade-proof/tellpalv2/_bmad-output/implementation-artifacts/ios-simulator-upgrade-evidence.md"
---
# TellPal Mobile Replacement - Epic Breakdown

## Overview

Story drafting is complete: 72 individually approved stories across seven epics (12 + 10 + 21 + 6 + 5 + 7 + 11). Final workflow validation of global FR/NFR/UX coverage, story sizing/dependencies, document consistency and unresolved readiness gates remains pending. Scope approval does not establish implementation readiness or acceptance evidence. No application code is changed.

The user approved applying all review recommendations on 2026-09-28. The updates clarify acceptance checks and delivery ownership; they do not close OQ gates or certify implementation. The current status is recorded here and in frontmatter; earlier progress snapshots are retained in [Planning history](#planning-history). Source-document reconciliation for the approved language and iOS parental-gate decisions remains part of final validation.

New planning prose is English under repository policy. Canonical FR/NFR excerpts below retain their original Turkish wording to preserve requirements without translation drift. There are 28 active FRs, one withdrawn FR identifier, 9 NFRs and 25 extracted UX requirements.

## Planning scope and execution boundaries

### Scope and evidence precedence

The latest explicit user decisions and adopted architecture govern implementation. The PRD owns product scope. Historical extraction and design documents provide evidence only where compatible with that scope. Flutter is selected; earlier framework-choice wording is superseded. This is a clean-room feature-first replacement, evolving app/mobile as the sole product client, with no application implementation authorized in this planning session.

FR-17 is withdrawn and never renumbered. Parent mode/guides, interactive branches, UI redesign, multiple profiles, continuous legacy synchronization and explicit offline-download management remain excluded. Their historical data is not authorized for deletion. Legacy ZIP boundaries, defects and Billing errors are not required behavior. Older extraction statements that restore excluded features, finalize general search, or leave FR-21 event categories undefined are superseded.

The existing epics.md remains the CMS/backend plan. FR-22–25 must remain traceable here as mobile behavior and backend/CMS dependencies; this inventory does not assert that backend publication, processing or authorization work is already complete, nor duplicate its implementation plan. Consult backend project memory, ADRs and canonical architecture before making any backend design or implementation decision.

### Delivery boundaries agreed with the user

- E1 and E2 are independently demonstrable under controlled development/test access. AD-5 is not a prerequisite for media/discovery work; it remains mandatory before E3 auth/migration feature development. Preparatory evidence collection can proceed earlier without implementing those features.
- Controlled access must use an authorized development/test mechanism. No hardcoded credentials, production auth bypass, production PoC endpoint or test-token UI is allowed. The specific mechanism is a prerequisite to settle from existing safe capabilities, not a new auth feature hidden in E1.
- Free eligible content is sufficient to validate the main media flow. Locked content stays visible as contracted and protected media stays denied. Full purchase/paywall journeys remain explicitly incomplete until E6; no early epic claims full FR-10/19/20 or release acceptance.
- E1/E2 consume verified backend identity/access contracts for controlled users. E3 later proves real session persistence, migration and account-switch isolation. Do not substitute in-memory progress for durable progress or a client flag for server authorization.
- Backend/CMS dependencies are tracked as contract/readiness prerequisites, not presumed complete from their existing epic plan. A fixture can support isolated tests but cannot alone satisfy real-backend integration acceptance.
- Crash/memory/latency diagnostics, privacy, error handling and visual evidence begin with their first feature. E7 aggregates release evidence rather than deferring all quality work.
- Shared core/media changes are introduced by the first consuming capability and extended by later capabilities. Do not create separate technical-layer epics or bulk-create future infrastructure/entities.

### Planning decisions and execution order

The user approved retaining all seven epics and 72 story identities. An epic is not a single implementation session. Final validation must assess individual story size and dependencies; split only an actual oversized story while preserving traceability. No acceptance criterion is removed by this review.

Track scope approval, implementation readiness and acceptance evidence separately. An approved story is not automatically ready to implement. Each story below records these dimensions; its prerequisites identify outstanding decisions, dependencies and evidence. Readiness changes only after those prerequisites are verified. This planning session has not produced implementation acceptance evidence.

#### Planning status definitions

The local delivery-status line in every story records three separate dimensions: scope is approved for planning; implementation readiness is pending verification of that story's prerequisites; acceptance evidence is absent from this planning exercise. An approved scope is not an implementation or release acceptance. Update readiness and evidence only against recorded verification outcomes. The current global workflow status is in Overview; historical snapshots do not change it.

#### Shared readiness and acceptance checks

Before Story 1.1, lock and verify a compatible Flutter/Dart, flutter_bloc, auto_route/generator, get_it/injectable/generator, Freezed, json_serializable and build_runner toolchain under ADR-0018 and the mobile spine. Record exact versions, successful code generation and analyze results, and compatibility with Android API 24 and iOS/iPadOS 15. Skill example versions are not compatibility evidence. Introduce only the bootstrap/composition needed by the first capability; do not create a separate technical epic or bulk-create future features.

For every affected UI story, collect UX-DR24 evidence with the feature: localized text at agreed normal and enlarged text scales; reachable controls and safe areas; icon role/name/selected semantics using platform accessibility tools; at least 48dp touch targets; and measured contrast over representative actual artwork. Resolve the scale/device matrix and contrast acceptance criteria before the affected screen's acceptance without silently redesigning it. Record screen/state/device/language/content and outcomes. Failed checks block the affected screen's acceptance. Story 7.5 repeats these checks on the release candidate rather than deferring them until release.

**First working package:** Stories 1.1 and 1.2, executed sequentially as separate work units. Demonstrate accurate Library listing followed by opening the correct story detail. Reader implementation starts subsequently with Story 1.3 and is not silently included in this package.

| Story | Conditions to establish readiness |
| --- | --- |
| 1.1 | Verify and lock the shared toolchain/generators with recorded generation, analyze and supported-platform compatibility evidence; then verify real catalog contract and redacted fixtures: CMS order, preview limits, audience/lock eligibility, language/fallback and empty/error states. Establish authorized development/test access and Library reference states. |
| 1.2 | Accept 1.1; verify content/experience/language identity, detail payload and error fixtures, valid/absent detail recommendations, navigation/back behavior and detail reference states. Verify EXT-CATALOG closure. |

AD-5 does not block this discovery package. Signed-upgrade and registered-session evidence can be collected independently; auth/migration development must still wait for AD-5 completion. Controlled access does not authorize production bypasses. Backend readiness is verified, not inferred from the separate CMS/backend plan.

**Context handling for implementation:** Work on one story at a time. Carry its acceptance criteria, relevant architecture invariants, resolved contracts/reference evidence and a concise dependency/outcome record into that session. Record changed files, verification results and remaining blockers before handoff. The complete epic remains a reference, not a request to implement every story in one context. Verify reference entry/action/result/back behavior while preparing each feature rather than deferring discovery to Epic 7.

**Epic 7 work types:** Stories 7.1–7.4 implement flow-event integrations; 7.5–7.9 consolidate release-candidate verification; 7.10–7.11 define and verify rollout/recovery operations. Keep their IDs and criteria. Verification stories are not all new product features, and an entire platform matrix need not execute in one work session.

**Initial exposure versus rollout expansion:** OQ-2/OQ-10 must distinguish evidence required before the first limited distribution from observation evidence required to expand each platform's cohort. Record measurement source (including any controlled pre-release cohort), candidate version, sample/window, owner and decision for each gate. The >=99.5% crash-free user target is unchanged. This distinction does not waive pre-release criteria or authorize exposure with known blockers. Do not assume production samples exist before users receive a build; resolve measurement source and progression before rollout. Missing evidence blocks the gate to which it belongs. Stories 7.6 and 7.10 must use this same resolved gate mapping.

### External dependency register

These records connect mobile consumers to separately delivered backend/CMS/provider capabilities. Module names describe the existing canonical ownership; they do not approve new backend designs. Accountable roles below must identify named assignees and link existing delivery work or explicitly assign missing work before the affected mobile story becomes ready. An unassigned item or a historical discovery/PoC document cannot satisfy that gate.

Reference documents: [CMS/backend epics](./epics.md), [canonical backend architecture](../../architecture.md), [ADR-0017 experimental manifest scope](../../be/docs/adr/ADR-0017-story-page-media-manifests.md), [mobile architecture spine](./architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md) and [historical contract extraction](./prds/prd-tellpalv2-2026-09-20/extract-contracts.md). These links establish context, not completion of the external delivery work.

For each record, the owner must record: exact external work link and status; named assignee; versioned contract path; redacted success/error fixture paths; real-backend/provider verification evidence; consuming mobile stories; and closure decision/date. Proposed evidence destination: `_bmad-output/implementation-artifacts/mobile-contracts/<dependency-id>/`. These are reserved destinations, not claims that files or evidence exist. Readiness remains pending until actual paths and outcomes are recorded and accepted by both the external owner and mobile implementer. Fixture-only success is insufficient for real integration acceptance.

| Dependency ID | Mobile consumers | Accountable role | Existing work/reference and missing assignment | Required closure evidence |
| --- | --- | --- | --- | --- |
| EXT-CATALOG | 1.1–1.2, 1.8–1.10, 2.1, 2.3, 2.7 | Backend content/category owner | `epics.md` defers public read composition/filtering/pagination; link or assign mobile catalog/detail/search delivery work. OQ-4/8 remain open. | Versioned identity/language/audience/list/detail/search contracts, pagination/error fixtures and actual backend results, including detail recommendations. |
| EXT-MEDIA | 1.3–1.4, 1.11–1.12, 2.2–2.3, 2.9–2.10 | Backend content/asset owner | `epics.md` Stories 1.1–1.4 and 1.8 cover related processing/asset ownership; ADR-0017 describes experimental manifests only. Assign production start/manifest/renewal and interrupted-transfer delivery work. | OQ-8 admission/immutable identity/integrity/renewal/resume contract; actual generated files and authorized real-backend transfer evidence. |
| EXT-HISTORY | 1.5, 1.7, 2.8, 3.12 | Backend user owner with mobile persistence owner | No delivery story for durable progress/history is established by `epics.md`; link or assign it before the first producer. | Versioned duration/progress/completion, stable event identity and deduplication contracts; durable pending-record/process-restart tests and backend field-level outcomes. |
| EXT-ACCOUNT | 3.3–3.13, 3.18–3.19 | Backend user owner with identity integration owner | `architecture.md` and PRD contract extraction are design/evidence references; assign or link verified Firebase UID/account/profile/session delivery work. | Verified UID resolution, provider and preference/profile fixtures, failure/retry and account-isolation evidence. AD-5 remains separate. |
| EXT-MIGRATION | 3.14–3.17, 3.20–3.21, 7.7–7.8 | Backend user/migration owner with mobile local-data owner | Account migration, local import and deletion delivery work is not established by `epics.md`; link or assign it under OQ-3/6. | Source/target fields and identity maps, durable receipts/status, interruption/multi-device fixtures, deletion/tombstone/legacy-isolation outcomes. |
| EXT-PUBLICATION | 5.1–5.4, 7.8 | Backend content/asset owner and CMS publication owner | `epics.md` processing/localization stories are partial inputs; link or assign candidate/live, approval invalidation, processing-error/retry and test-language delivery work. | Revision-specific readiness/approval, failed-candidate isolation, tester/subscriber/ordinary-user matrix and actual CMS/backend/mobile results. |
| EXT-RETENTION | 1.11–1.12, 2.9–2.10, 5.5 | Backend asset owner with mobile cache owner | ADR-0017 retains experiment revisions but does not establish production cleanup. Link or assign OQ-8 retention/withdrawal/revocation delivery work. | Actual retained file access for live/rollback/active sessions, cleanup safety and revision-addressed renewal/revocation tests. |
| EXT-PURCHASE | 6.1–6.7, 7.4, 7.7–7.8 | Purchase/backend owner and store/RevenueCat integration owner | `architecture.md` purchase ownership is a reference, not store integration evidence; link or assign OQ-7/8 entitlement/reconciliation delivery work. | Store/provider-to-application identity and products/offerings contracts; real purchase/restore/conflict and backend-authoritative access outcomes. |
| EXT-SUPPORT | 4.1–4.6, 7.9 | Support API/configuration owner and notification/provider integration owner | Assign or link OQ-9 feedback, localized documents, announcements, notification/link and rating configuration work; provider selection remains open. | Versioned limits/fallback/target/permission contracts; real service/provider fixtures and platform outcomes. |
| EXT-EVENTS | 7.1–7.4 | Backend event owner with mobile event-delivery owner | `architecture.md` event ownership does not establish implemented ingestion/deduplication. Link or assign OQ-7 event delivery work. | Event fields/privacy/retention/retry contracts; real backend delivery and deduplication evidence tied to account and operation IDs. |

All records currently have unverified closure and pending named-assignee/work/contract/evidence links. Update the affected records as prerequisite work is completed; do not mark all stories ready from this register's creation.

## Requirements Inventory

### Functional Requirements

#### FR-1 — Açılış ve navigasyon

İlk kurulum, geri dönen kullanıcı ve migration gereken kullanıcı doğru giriş akışına yönlendirilir. Onboarding, ana hikâye, rahatlama ve profil yüzeyleri ile geri navigasyon referans sürümle karşılaştırılır. Yeniden açılış yanlışlıkla onboarding veya boş profil başlatmaz.

İlk kurulumda normal kataloğa girişten önce kayıt/giriş veya anonim devam seçimi yapılır. Bu seçim, geri dönen kullanıcının geçerli oturumunu gereksiz yere sıfırlamaz.

#### FR-2 — Kimlik doğrulama

E-posta, Google ve Apple giriş/kayıt; e-posta doğrulama ve parola sıfırlama desteklenir. Referans kayıt formundaki zorunlu onay, ayrı pazarlama tercihi ve e-posta/parola doğrulamaları karşılanır; eksik zorunlu alan/onay başarıya dönüşmez. İptal/hata yeni başarılı oturum sayılmaz. Firebase kimliği doğru uygulama hesabıyla eşleşir; benzer e-posta adresleri nedeniyle hesaplar sessizce birleştirilmez.

Kimlik doğrulama otoritesi Firebase Auth'tur; doğrulanmış Firebase UID, backend'deki uygulama kullanıcısı/profiliyle eşleştirilir. Yeni sistemde uygulama profili için esas kayıt backend'dedir. Eski sistemin paralel çalışması bu yeni profil sahipliğini değiştirmez.

#### FR-3 — Anonim kullanım ve dönüşüm

Anonim devam Firebase Anonymous Auth ile mümkün olur. Anonim kullanıcı yeni kayıtlı hesap oluşturduğunda anonim dönemdeki profil, tercihler, geçmiş ve ilerleme yeni hesaba taşınır. Kullanıcı var olan hesaba giriş yaptığında o hesabın verileri kullanılır; anonim veri otomatik olarak birleştirilmez. Bu davranış kullanıcı tarafından onaylanmıştır. Eski uygulamadan anonim migration sırasında geçmişin korunması kararı da geçerlidir.

#### FR-4 — Profil kurulumu ve düzenleme

Referans sürümün profil kurulum alanları, atlanabilir adımları, avatar ve ad düzenleme işlevleri korunur. Migration her iki eski profil şemasının bilgisini korur; atlanan alanlar uygulamayı kullanmayı engellemez. Bir hesap için bir profil kullanılır.

#### FR-5 — Hesap ve tercih yönetimi

Çıkış, parola değiştirme, hesap silme, dil değiştirme ve bildirim tercihi mevcut işlevleriyle bulunur. Hesap değişiminde önceki hesabın özel verileri görünmez. Hesap silme yalnız yeni sistemdeki hesabı ve o sistemdeki kullanıcı verilerini kapsar. Eski sistemdeki kayıtlar ile eski uygulama sürümünü kullanan cihazlar etkilenmez. Kullanıcı bu kapsamı onaylamıştır.

Paylaşılan Firebase kimliğiyle bu etkinin nasıl sınırlandırılacağı ve yeniden girişin silinen veriyi eski sistemden geri taşımamasının nasıl sağlanacağı OQ-6'da teknik doğrulama olarak tutulur. Hesap silme kararından mağaza aboneliğinin otomatik iptali sonucu çıkarılmaz; kullanıcıya açıklanan abonelik etkisi sözleşmede netleştirilecektir.

#### FR-6 — Ana ekran ve kategori

Ana ekran CMS sıralı kategori satırları ve sınırlı önizleme içerikleri sunar; tam kategoriye geçiş vardır. Dil/erişim kitlesi için uygun içeriği olmayan kategori gösterilmez. Tam listede içeriklerin atlanmaması ve tekrarlanmaması sayfalama kabulüne dahildir.

Liste yanıtları küçük içerik özetleridir; tam teslim/oynatma verisi detay veya başlatma akışında alınır. Ana ekran satır bileşimi, sayfalama ve hata sözleşmesi OQ-4'te belirlenecektir; CMS kategori sırası onaylı kuraldır.

Editör seçimi ve benzer içerik yüzeyleri referans envanterine dahildir; seçilen öneri doğru içerik/deneyime gider. Uygulanabilir içerik bulunmadığında bozuk kart veya geçersiz hedef gösterilmez.

#### FR-7 — İçerik araması; tür kapsamı açık

Arama kapsamı henüz kararlaştırılmamıştır. Eski uygulamada `/story/search` ve `StorySearchByCategoryType[]` yanıt modeli belgelenmiştir; endpoint adı ve bu model, gerçekte hangi içerik türlerinin döndüğünü tek başına kanıtlamaz. Önceki “yalnız hikâye” ifadesi doğrulanmış bulgu olarak kullanılmayacaktır. Discovery uygun içerik türleri arasında genel aramayı yönlendirir; sonraki kullanıcı kararı gereği gerçek legacy request/response örnekleri ve yeni backend hedef sözleşmesi incelenerek kapsam belirlenecektir. Sonuçlar bağımsız kişi kaydı değil içeriktir; içerik ve katkı sağlayıcı bilgileri eşleşmede kullanılabilir. Sonuçlar uygun olduğunda deneyim/türe göre gruplanır; boş sonuç ve ağ hatası birbirinden ayrılır. Karakter eşiği ve debounce davranışı referans kanıtıyla doğrulanacaktır.

#### FR-8 — İçerik ve deneyim ayrımı

Hem okuma hem sesli hikâye deneyimi olan STORY ilgili iki keşif grubunda görünebilir. Seçilen deneyim doğru detay/oynatmayı açar; diğer deneyimin işleme hatası hazır deneyimi engellemez.

#### FR-9 — Dil ve fallback

> **Superseded language-availability excerpt:** The Turkish source paragraph below records the earlier PRD decision. For current planning, apply the 2026-09-27 user decision: Turkish, English and Portuguese are usable; newly added languages may be enabled remotely for testing first and for live users afterward. German is not an automatically enabled launch language. Other FR-9 rules remain unchanged.

Uygulama ve içerik dili birlikte yönetilir, tercih profilde saklanır. Dil görünürlüğü referans sürümdeki gibidir: Türkçe, İngilizce ve Almanca açıktır; Portekizce yalnız mevcut uzaktan yapılandırma tarafından açıldığında görünür; İspanyolca kapalıdır. Dil değişiminde önceki dile ait önbellek sonucu yeni dilmiş gibi gösterilmez. İngilizce fallback yalnız uygun yayın ve erişim koşulları sağlanırsa kullanılabilir. Kullanıcı bu davranışı onaylamıştır.

Fallback, seçilen dilde içerik bulunmadığında değerlendirilir; İngilizce deneyimin kendisi yayınlı ve kullanıcı için erişilebilir olmalıdır. Fallback, yayınlanmamış veya yetkisiz bir deneyimi açamaz.

#### FR-10 — Ücretsiz ve kilitli görünürlük

Ücretsiz ve kilitli içerik katalogda görünür; kilitli içeriği başlatma paywall açar. Dil bazlı CMS ücretsiz seçimi STORY'nin okuma ve sesli deneyimlerine uygulanır. Abone olmayanlara özel kategori CMS sırasıyla yalnız o kitleye gösterilir; aktif aboneye gösterilmez.

Bu kategori ücretsiz ve kilitli içerikleri birlikte barındırabilir. Anonim ve kayıtlı kullanıcılar aynı içerik erişim kurallarına tabidir; aynı dil, yayın ve erişim hakkı koşullarında yalnız giriş türü nedeniyle farklı içerik hakkı oluşmaz.

#### FR-11 — Detaydan başlatma

Hikâye, sesli hikâye ve meditasyon bilgi/detay yüzeyinden açık kullanıcı eylemiyle başlar. Ninni kartı doğrudan oynatıcıya ve kullanıcı tarafından başlatılmış dinlemeye gider. Detayı açmak kendiliğinden ses başlatmaz.

#### FR-12 — Sayfalı hikâye

Sayfa görseli ve anlatım sesi birlikte doğru sayfaya aittir. İlk sayfa hazır olunca başlayabilir; tüm hikâyeyi beklemek gerekmez. Anlatım bitişi animasyonla ilerler; manuel gezinme çalışır. Hazır olmayan sayfa yükleme durumuna geçer, sonsuz bekleme yerine toparlanabilir hata sunar.

İlk sürümün discovery yönü, revizyonlu manifest ile her sayfanın ayrı görsel ve anlatım sesinin teslimidir. İlk sayfanın görseli ve sesi hazır olduğunda tüketim başlar; sonraki sayfa ve sınırlı ileri hazırlık yapılır. Zayıf bağlantıda manuel geçişin yükleme göstermesi kabul edilebilir; bitişik sayfanın her koşulda hazır olması garanti edilmez. Manifestin detayda mı başlatmada mı alınacağı ve kesin şeması OQ-8'de açık kalır.

#### FR-13 — Hikâye kontrolleri ve tamamlanma

Referans sürümdeki anlatım/müzik kontrolleri, otomatik ve manuel sayfa ilerletme ile tamamlanma davranışları korunur. Hızlı gezinme ile otomatik ilerlemenin çakışması yanlış sayfa veya çift ses üretmez. Tamamlanma ekranındaki değerlendirme, benzer içerik ve paywall girişleri envanterden doğrulanır. Etkileşimli hikâye dalları ve seçimleri replacement kapsamı dışındadır.

#### FR-14 — Sesli oynatıcı

Referans sürümün ninni, meditasyon ve sesli hikâye oynatma kontrolleri aynı kalır: oynat/duraklat, ses konumuna gitme, oynatıcıdan çıkış ve ilgili meta bilgiler gösterilir. Ninni döngüsel oynar; meditasyon ve sesli hikâye döngüsel oynamaz. Arka plan ve sistem medya kontrolleri desteklenir. Çağrı geldiğinde veya kulaklık çıkarıldığında oynatma duraklar; kullanıcı kendi eylemiyle yeniden başlatır. Ekrandan çıkış, geri dönüş ve başka içerik açılışında birbiriyle yarışan oynatıcılar oluşmaz.

#### FR-15 — Medya hazırlığı ve görsel sunum

İlk sayfa ve sınırlı ileri hazırlık Wi-Fi ve hücresel bağlantıda kullanılabilir; tüm hikâyeyi spekülatif indirme anlamına gelmez. Detayda sınırlı ses hazırlığı başlatma anında yeniden kullanılır. Ninni liste ve oynatıcı görselleri ayrıdır; mevcut animasyon tek statik görsele indirgenmez. Hikâye görsel oranı korunur; metin güvenli alanı ve tablet kırpması cihazlarla doğrulanır.

Sesli hikâye ve meditasyon detayındaki hazırlık tüm ses dosyasının indirilmesini gerektirmez ve otomatik oynatma başlatmaz. Telefon ve tablette aynı portre hikâye görseli, oranı korunarak ekranı dolduracak şekilde kırpılır. Merkezden kırpma geçici varsayımdır; metin güvenli alanlarıyla temsilî cihazlarda doğrulanmadan kesin kural sayılmaz.

#### FR-16 — İlerleme, geçmiş ve istatistikler

Okuma/dinleme geçmişi, tamamlanma, saklanan ilerleme ve profil istatistikleri referans işlevleriyle korunur. Tek olayın tekrar işlenmesi tamamlanma veya süreyi iki kez saymaz. Uygulama tamamen kapatılıp yeniden açıldığında ana ekrandan açılır; kullanıcı doğrudan önceki hikâye/oynatıcıya geri götürülmez. Uygulama arka planda kaldığında açık hikâye veya oynatma oturumu kaldığı yerden devam eder. Kullanıcı bu davranışı onaylamıştır.

#### FR-17 — Geri çekildi: ebeveyn rehberleri

Kullanıcı kararıyla kapsam dışıdır; bu kimlik yeniden kullanılmaz. Eski belgelerdeki ebeveyn rehberi akışları uygulama gereksinimi değildir. Tarihsel kullanıcı kayıtlarının korunması OQ-3'te ayrıca ele alınır.

#### FR-18 — Destek ve iletişim

Geri bildirim gönderimi, doğrulama ve mevcut sınırlar; yerelleştirilmiş yasal/yardım belgeleri; uygulama değerlendirme çağrısı ve sürüm bilgisi; duyuru, bildirim izni ve deep-link giriş noktaları korunur. Başarısız gönderim başarılı gösterilmez. Duyuru/bildirim ve deep-link hedefleri geçerli ekrana gider veya anlaşılır durum sunar. Kullanıcıya görünen bildirim/duyuru/deep-link davranışı referans sürümle aynı kalır; teknik sağlayıcı veya SDK seçimi mimari aşamasında değişebilir. Mevcut operasyonel yapılandırmanın görünür özellikleri, ücretsiz içerikleri, duyuruları ve paywall tetiklerini yönetme ihtiyaçları envantere dahildir; aynı Remote Config anahtarlarının kullanılması şart değildir.

#### FR-19 — Satın alma ve geri yükleme

RevenueCat üzerinden mevcut aboneliklerin tanınması, satın alma, geri yükleme ve erişim yenileme yayın öncesinde tamamlanır. İptal edilmiş ama ücretli dönemi bitmemiş abonelik erişimi sürer. Mağaza hatası veya satın alma iptali erişim verilmiş gibi gösterilmez; anonim/kayıtlı kimlik tek başına premium hakkı yaratmaz.

#### FR-20 — Promosyon ve paywall

Referans sürümdeki promosyon kodu/teklif, paywall giriş ve kapatma, üyelik yönetimi işlevleri envanterden doğrulanarak korunur. Hatalı/geçersiz promosyon tanımlı hata sunar. RevenueCat ürün/teklif kimlikleri ile teknik tetikleme/entegrasyon ayrıntıları gerçek entegrasyon sözleşmesinde belirlenecektir. Kullanıcı bu yaklaşımı onaylamıştır.

#### FR-21 — Kullanıcı akışlarının backend kaydı

Giriş/kayıt, migration, katalog-arama-detay görüntüleme, okuma/dinleme başlatma-tamamlama, paywall/satın alma/restore ve hata-yeniden deneme akışları release öncesinde backende kaydedilir. Olaylar içerik metni, medya URL'leri veya gereksiz kişisel veri içermez. Veri alanları, saklama süresi ve hata/tekrar politikası gerçek backend sözleşmesinde belirlenecektir. Kayıt başarısızlığı okuma/dinlemeyi durdurmamalı; yeniden deneme aynı olayı çoğaltmamalıdır. Crash tanılaması bu son entegrasyona bağımlı değildir.

#### FR-22 — Dil ve deneyim bazlı yayın

Okuma ve sesli hikâye yayınları dil/deneyim bazında bağımsızdır. Bir kapsamda en fazla bir aktif aday bulunur. Adaydaki önemli değişiklik yeni bir aday revizyon oluşturur ve etkilenen test/onayları sıfırlar. Yeni aday hazırlanırken mevcut canlı revizyon kullanılabilir kalır; ilgili yayın kapsamıyla sınırlı rollback önceki canlı revizyonu geri getirebilir.

#### FR-23 — Tester erişimi ve onay

Tester, kilitli adaylar dahil yetkili olduğu test revizyonlarını açabilir; test/revizyon göstergesi görür. Normal kullanıcı aday alamaz. Gösterge yetkilendirme yerine geçmez. Medya değişikliği hazırlık ve yeniden cihaz testi gerektirir; genel yayın gerekli hazırlık ve açık onaydan sonra yapılır.

Test yayını genel yayından ayrı ara durumdur. Tester yetkisi abonelik hakkından ayrıdır; görünür test/revizyon göstergesi detay veya oynatıcıda sunulur. Üretilen medyayı değiştirmeyen yalnız metadata değişiklikleri daha hafif onaydan geçebilir; hangi alanların bu koşulu sağladığı OQ-8'de tanımlanır.

#### FR-24 — İşleme başarısızlığı

Medya hazırlama başarısızlığı ilgili adayın test/genel yayınını engeller; CMS hata ve yeniden deneme yolunu gösterir. Başarısız aday mevcut canlı içeriği değiştirmez. Üretilmiş medya hazır kabul edilmeden gerçekten erişilebilir ve bütün olmalıdır.

Hazırlanacak ve doğrulanacak çıktılar: liste küçük görselleri; gerektiğinde detay/oynatıcı görselleri; sayfa görselleri ve anlatım sesleri; gerektiğinde animasyonlu dinleme görselleri; sıralı sayfa, varlık kimliği ve hazır olma/bütünlük bilgisi taşıyan revizyonlu manifest. Yalnız işleme planı veya kayıt oluşturulması, dosyaların üretilmiş ve kullanılabilir olduğuna kanıt değildir. Boyutlar, codec, kalite bütçesi ve işleme orkestrasyonu OQ-8'de belirlenecektir.

#### FR-25 — Korumalı medya ve revizyon sürekliliği

Liste/detay görünürlüğü korumalı tam medya erişimi vermez. Başlatma yetkisi kontrol edilir; aktif oynatma kabul edilen revizyona bağlı kalır. Süresi dolan medya erişimi, yayından çekme ve güvenlik istisnaları OQ-8'de tamamlanacaktır. Temizlik canlı, rollback ve oturum için gerekli revizyonları korur.

Korumalı medya backend erişim kontrolünden sonra kısa ömürlü URL'lerle sunulur; URL ömrü ve yenileme mekanizması henüz seçilmemiştir. Metadata erişimi, detayda ön hazırlık veya cache varlığı korumalı tam medyaya erişim kontrolünü atlatamaz. İlk sürüm yayınına kadar backend'in güvenilir abonelik erişimi kontrolü tamamlanmalıdır; RevenueCat'in son adım olması erişim kontrolünü isteğe bağlı yapmaz. Saklama ve temizlik parametreleri backend/ortam yapılandırmasından yönetilir.

#### FR-26 — Hesap bazlı tek geçiş

İlk yeni sürüm kullanımı hesabın ilk migration'ını tetikler ve o ana kadar olan korunacak verileri taşır. Başarılı hesabı ikinci cihazda açmak migration'ı tekrarlamaz. Eski cihaz eski sistemde kalır; sonradan oluşan değişiklikler eşitlenmez.

#### FR-27 — Kayıpsız ve yeniden denenebilir taşıma

Eski/yeni içerik ve hesap kimlikleri doğru eşleşir. Tekrar deneme kayıt çoğaltmaz, daha yeni veriyi ezmez ve eksik eşleşmeleri sessizce atmaz. “Verileriniz taşınıyor” ve “Tekrar dene” deneyimleri uygulanır; tamamlanmamış migration boş profille normal kullanıma geçemez.

#### FR-28 — Yerel anonim veri ve temizlik

Anonim kullanıcı kayıt zorunluluğu olmadan geçer; korunacak yerel kayıtlar yeni anonim hesaba aktarılır. Eski medya/yeniden üretilebilir önbellek temizlenebilir; migration girdileri aktarım doğrulanmadan silinmez. Eski backend ve eski sürümlü diğer cihazların verisi bu temizliğe dahil değildir.

#### FR-29 — Mağaza güncellemesi olarak teslim

Replacement mevcut uygulamanın üzerine güncelleme olarak kurulabilmelidir; ayrı uygulama kurma ve hesabı baştan oluşturma zorunluluğu getirmez. Platform kimliği, imzalama, oturum ve yerel veri erişiminin seçilen teknolojiyle korunabildiği yayın öncesi doğrulanır. Eski sistemin eşzamanlı hizmeti devam eder.

### NonFunctional Requirements

- **NFR-1 — Kararlılık:** Etkin FR-1–FR-29 gereksinimlerinin (geri çekilmiş FR-17 hariç) kritik kabul senaryolarında bilinen crash, donma veya veri kaybı engeli kalmaz. Platform bazında en az %99,5 crash-free kullanıcı hedefi kararlaştırılmıştır; OQ-2'de açık olanlar ölçüm süresi/örneklemi ve cihaz bazlı bütçelerdir. Test cihazlarında bellek kaynaklı kapanma veya kritik oynatma hatası yayın engelidir.
- **NFR-2 — Bellek:** Düşük bellekli cihaz, en ağır içerik ve tekrarlı oturum testleri yapılır. Isınma sonrası tekrar döngülerinde açıklanamayan sürekli bellek artışı giderilir. Cihaz sınıfı başına tepe/kararlı kullanım bütçeleri OQ-2'de belirlenir.
- **NFR-3 — Akıcılık ve başlatma:** Soğuk/sıcak açılış, ilk sayfa/ilk ses, sayfa geçişi ve yeniden bağlanma ölçülür. Cihaz, ağ ve önbellek koşullarıyla p50/p95 hedefleri kararlaştırılır; eski performans kabul tavanı değildir.
- **NFR-4 — Veri bütünlüğü:** Migration ve tekrar deneme testleri korunacak alanlar ve kimlik ilişkilerinde kayıp/çoğalma göstermemelidir. Başarı, yalnız taşınan kayıt sayısıyla değil alan/ilişki doğrulamasıyla değerlendirilir.
- **NFR-5 — Erişim ve gizlilik:** Başka hesabın verisi veya yetkisiz revizyon/medya döndürülmez. Kimlik bilgileri, tokenlar ve özel medya URL'leri olay/crash kayıtlarına konmaz. Çocuk verisi, SDK kapsamı ve izinlerle ilgili mevcut davranışlar OQ-9 kapsamında doğrulanır.
- **NFR-6 — Uyumluluk:** Onaylanmış minimum işletim sistemi ve telefon/tablet kapsamı korunur. Gerçek cihazlarda oynatma, migration ve arka plan davranışı doğrulanır; yalnız emülatör başarısı yeterli değildir.
- **NFR-7 — Görsel parite:** Kararlaştırılmış referans ekranlar aynı cihaz boyutu, dil ve içerik koşullarında karşılaştırılır. Onaylı değişiklikler hariç görünür sapmalar kapanmadan ilgili ekran kabul edilmez; görüntü sıkıştırma metin okunabilirliğini bozamaz.
- **NFR-8 — Tanılama:** Crash, bellek kaynaklı sonlanma, donma ve oynatma hataları sürüm/platform/cihaz bağlamıyla araştırılabilir olmalıdır. Kullanıcı akış loglarıyla crash ölçümü ayrı veri amaçlarıdır.
- **NFR-9 — Gelecekte offline kullanım için temel:** Revizyon/varlık kimliği sabit, bütünlük ve tamlık doğrulanabilir, indirmeye devam etmeye uygun bir medya temeli tasarlanır. Silinebilir oynatma önbelleği ile gelecekte kullanıcının açıkça saklayacağı offline içerik ayrılır. İlk sürüm kullanıcı kontrollü offline indirme ekranı veya tam hikâyenin offline hazır olduğu garantisi sunmaz; cache bütçesi, boşaltma ve devam semantiği OQ-8'de belirlenecektir.

### Additional Requirements

#### Evidence gates and unresolved decisions

| Gate | Current evidence / required closure | Timing |
| --- | --- | --- |
| AD-5 / FR-29 | Media PoC complete per user. Separate unmerged branch codex/ios-legacy-upgrade-probe, commit a807a9f, reports iOS Simulator preservation of is_anonymous=true, language=tr and first_run=false across install/relaunch. Name/read-list absent; no populated-profile/history proof. Legacy build 1.0.34+332 → probe 1.0.35+333 differs from visual baseline 1.0.35+333. No production signing or registered Firebase session proof. | Signed representative in-place updates on BOTH platforms, correct live IDs/signing/provisioning and retained local records/sessions must pass BEFORE auth/migration feature development. No merge performed by this planning task. |
| OQ-1 | Complete reader/player controls and backend capability evidence; reference screenshots and redacted screen-to-API success/error fixtures. | Before finalizing affected epic scope and suitability. |
| OQ-2 | >=99.5% crash-free users per platform is fixed. Measurement window/sample, device/network matrix and memory/latency p50/p95 budgets remain open. | Measurement plan early; thresholds and sufficient evidence before release. |
| OQ-3 + OQ-6 | Later-device unique local records, source/target fields and identity mapping, invalid/unmapped data, removed-content history, shared-Firebase deletion, tombstones and remigration prevention. | Resolve before migration acceptance criteria; deletion contract before deletion implementation. |
| OQ-4 | Partially resolved by user screenshots: reading stories, audio stories, meditation and lullabies appear in legacy search. Real payloads, matching/ranking/group counts, threshold/debounce, lightweight catalog, pagination and errors remain open; see evidence update below. | Before finalizing the affected UX/API contracts or implementing the stories that depend on them. |
| OQ-8 / AD-10 | Authorization matrix, shared content/experience/asset IDs, immutable manifest, start/fetch point, renewal/revocation, actual renditions/integrity, metadata-only approval, retention, bounded prefetch/cache and retry/resume semantics. Experimental PoC response is insufficient as production contract. | Resolve relevant contracts before catalog/reader/player publication stories. |
| OQ-7 | RevenueCat products/offerings and event fields, retention, idempotency/retry contract; FR-21 event categories are already fixed. | Before late integration implementation; purchase/restore and events verified before release. |
| OQ-9 | Notification/announcement/deep-link behavior, permissions, SDK/data scope and store audience declarations. | Before finalizing release flows and related integrations. |
| OQ-10 / AD-11 | Rollout cohorts, expansion/stop thresholds, recovery and separate old-system shutdown decision. | Before rollout; backend compatibility precedes mobile cohorts. |

Supported minimums remain Android API 24 and iOS/iPadOS 15, phone and tablet. Known critical playback failure or memory termination on test devices blocks release. Do not infer production stability from an emulator run or insufficient crash-free samples.

DESIGN.md is verified-with-gaps; EXPERIENCE.md is marked final but remains historical evidence subject to PRD exclusions. COVERAGE.md has a stale blanket statement that tablet screenshots do not exist; its ending and TABLET.md describe emulated tablet captures. Treat these as screen-size evidence only. Images are indexed here and will be inspected for each relevant screen; this inventory does not claim fresh visual/runtime verification.

#### Language availability decision — 2026-09-27

The user reports that Turkish, English and Portuguese content is currently loaded and directs that these languages be usable. This supersedes earlier PRD/design-source statements that enabled German and conditionally exposed Portuguese. This is a product decision and user-reported content status, not an independent audit of all content/experience readiness.

New languages may first be enabled for test use through remote configuration and later enabled for live use. Plan distinct hidden, test-only and live visibility outcomes; exact configuration schema and test-audience selection remain contract work. Language visibility must not override candidate/live publication or protected-media authorization. A test-only language must not leak into ordinary users' choices or fallback results. Enabling a language does not mark unready media as ready.

E1 owns language consumption and safe configuration behavior for controlled test users, E2 applies it to listening, E3 owns persisted account preference/migration, and E5 verifies test-versus-live language exposure with authorized users. Story 1.10 is approved for planning; no implementation is authorized here. Existing FR-9 English fallback and language/revision continuity rules remain in force.

#### Search evidence update — 2026-09-27

User-supplied legacy screenshots are retained under `mobile-evidence/search-2026-09-27/`. They establish visible search outcomes, not captured network payloads or verified backend implementation. Exact app version/device OS was not independently established.

- `uykusuzlar.jpg`: query “uykusuzlar”; visible “Hikayeler (1)” and “Sesli Hikayeler (1)” groups, locked cards, clear-query control and persistent bottom navigation.
- `huzur-reading-listening.jpg`: query “huzur”; the reading cover and audio-story card for “Acelesiz Bir Yolculuk” are visible; “Sesli Hikayeler (1)” is explicit. The upper group heading is cropped.
- `huzur-meditation-lullabies.jpg`: the same query shows “Bilinçli Uyku Meditasyonu” and an explicit “Ninniler (2)” group including “Huzurlu Rüyaların Melodisi”. The meditation group heading/count is outside the visible area. The full second lullaby is not shown.

The legacy result scope therefore includes reading stories, audio stories, meditation and lullabies. Preserve these in replacement planning; this is evidence of existing scope rather than an added feature. Reading/audio-story entries remain experiences of STORY under the architecture, not proof of separate canonical content types or matching backend IDs. Visible group counts and distinct card variants are parity inputs; exact count semantics and complete ordering still require contract evidence.

The “huzur” results include visible titles without that literal word. Do not implement or claim literal title-only matching on that basis, and do not infer whether descriptions, tags, contributors or semantic matching caused the result. OQ-4 remains PARTIALLY RESOLVED: result experience coverage is evidenced; matching fields/rules, ranking, grouping/count semantics, threshold/debounce, pagination, errors and actual target API examples remain open. Screenshot evidence does not verify tap destinations.

The user recalls that legacy matching used content name, description and contributor names. Record this as user-reported behavior, not independently verified implementation or a finalized target contract. Validate each field with a redacted request/response case whose match is isolated to that field. Contributor-name matches should return the associated content, not standalone person records. Capture case/diacritic handling, partial-word behavior and multi-field ranking from evidence rather than inventing them. The supplied screenshots are consistent with non-title matching but do not prove which field produced a result. This recollection narrows the OQ-4 investigation; it does not close that gate or approve Story 1.9 by itself.

Story 1.9 is approved for planning, with OQ-4 contract verification still required before implementation. E1 can implement grouped search presentation and the already available reading-detail destination; E2 must own and verify listening destinations once available. Do not label all search destinations accepted while they depend on future E2 stories.

#### Adopted architecture requirements

Aligned on 2026-09-28 with ADR-0018 and the mobile architecture spine: BLoC/Cubit, auto_route and get_it + injectable replace the earlier Riverpod/go_router choices. AD-12 makes DI and disposal ownership explicit. This alignment does not complete final story validation, close OQ gates or change any story readiness status.

##### AD-1 — One feature-first Flutter codebase [ADOPTED]

- **Binds:** All mobile features and store-update delivery.
- **Prevents:** Competing Android/iOS implementations or feature logic scattering across global horizontal folders.
- **Rule:** Evolve `app/mobile` into the sole production client. Each feature owns `presentation` (screens, views, widgets, BLoCs/Cubits), `domain` (entities, repository interfaces, use cases) and `data` (models, datasources, repository implementations). Presentation depends on domain; data implements domain contracts; domain never imports data or platform adapters. The composition root wires implementations. Domain may use pure-Dart value types and DI annotation metadata, but not runtime container lookup. The PoC entry screen and test credentials remain development-only.

##### AD-2 — One state owner per feature capability [ADOPTED]

- **Binds:** Authentication, catalog, reader, profile, paywall and migration UI.
- **Prevents:** Conflicting copies of state, screen-specific loading/error conventions and business decisions inside widgets.
- **Rule:** Use `flutter_bloc` with one BLoC/Cubit state owner per feature capability instance; use BLoC for event-driven/concurrent flows and Cubit for simple linear state. Events and states use Freezed; represent loading, empty, error, retry and loaded outcomes as applicable. Widgets dispatch actions and render immutable state; use BlocSelector for partial-state consumption and BlocListener for one-time UI effects. Business operations call injected domain use cases, never HTTP or data implementations. Keep ephemeral widget state local without duplicating BLoC-owned state. Define ordering/cancellation for competing actions and reject results for obsolete account, language, request or media-session generations; changing libraries does not provide this automatically.

##### AD-3 — One application-scoped media session [ADOPTED]

- **Binds:** Illustrated reading, full-story narration, meditation, lullaby and system playback controls.
- **Prevents:** Overlapping players, stale page completion, playback loss on route replacement and contradictory background controls.
- **Rule:** A single media coordinator owns the logical session, serializes takeovers/source changes, rejects stale commands and completions, and is the sole producer of playback progress/completion transitions. It may coordinate separate narration and music channels; it must not permit unrelated sessions to overlap. Feature BLoCs/Cubits observe its shared contract and issue commands; they do not create players or own playback completion. Use `just_audio` for audio, `audio_service` for background/system controls and `audio_session` for interruptions. The coordinator owns player disposal and the admitted media revision; route disposal does not dispose the session or its active assets. Account change stops protected playback before the next account becomes ready. Preserve the PRD's loop, interruption, background and cold-relaunch behaviors on both platforms.

##### AD-4 — Backend-owned account migration [ADOPTED]

- **Binds:** Registered and anonymous upgrades, second devices, retained user data.
- **Prevents:** Duplicate imports, lost local records, new data overwritten by old data and repeated migration on later-upgraded devices.
- **Rule:** Firebase Auth establishes identity; backend maps the verified UID to the canonical application user and owns idempotent account migration and its terminal status. The client gates normal use on verified persisted completion and exposes pending/retry states. Device-only anonymous data is imported separately with a stable operation identity and retained until a durable receipt confirms its disposition; an accepted upload is not completion. A later-upgraded device never restarts completed account migration; old-version devices keep using the old backend without continuous synchronization. Anonymous-to-new-account transfer and existing-account sign-in are distinct paths; no automatic merge into an existing account. New-system-only account deletion must leave the shared legacy identity usable and prevent remigration of deleted data.

##### AD-5 — In-place update compatibility is an early gate [ADOPTED]

- **Binds:** Product app identity, signing, local data access and FR-29.
- **Prevents:** A working fresh install that cannot replace the live app or read migration inputs.
- **Rule:** Before auth/migration feature stories, verify the live Android application ID, iOS bundle ID and approved signing/provisioning; then prove representative signed in-place upgrades on both platforms can read retained local records and sessions. The documented legacy IDs are `com.scoustech.interactive_short_kid_stories_tellpal` (Android) and `com.scoustech.interactiveShortKidStoriesTellpal` (iOS). The PoC IDs are experimental and do not satisfy this gate.

##### AD-6 — Central navigation from authoritative session state [ADOPTED]

- **Binds:** First launch, sign-in, migration gate, tabs, detail and player entry.
- **Prevents:** Feature-local redirects, inconsistent back stacks and normal catalog access during unfinished migration.
- **Rule:** One application-scoped AppSessionBloc projects verified identity, backend user, migration and access into signed-out, resolving, migration-pending/failed, ready and account-switching states. One `auto_route` AppRouter admits routes from this projection; guards reevaluate when session state changes and cannot retain stale admission. Features use generated routes and `context.router`, not direct Navigator calls or local auth redirects. Pass stable identifiers and required scalar context (experience, language, revision), never full entities or credentials; resolve and authorize at the destination. During account change, invalidate old async results, stop protected media, clear account-scoped state/cache, then admit the new account only after backend resolution. Navigation never replaces server authorization. Preserve independent tab stacks and reference deep links, including guarded entry after cold launch.

##### AD-7 — One authenticated transport, typed feature adapters [ADOPTED]

- **Binds:** Every protected backend call and media manifest request.
- **Prevents:** Inconsistent token attachment, timeout/error handling and network calls from widgets.
- **Rule:** The shared transport obtains a current Firebase ID token, attaches it to protected calls and normalizes authentication, timeout and network failures. Feature data sources and models map verified contracts; repository implementations return domain entities through domain interfaces using `Either<Failure, T>` (dartz), wrapped in Future or Stream as required. Use cases depend on these interfaces; DTO JSON serialization stays in data (`json_serializable`, models extending domain entities). Retry mutations only with stable idempotency identity; stale-account responses cannot update current state. Start with handwritten typed clients and contract tests; reassess OpenAPI generation when contracts stabilize. Never ship the allowlisted PoC endpoint, test-token UI or development cleartext exception as production access.

##### AD-8 — Separate durable user data from evictable media [ADOPTED]

- **Binds:** Profile/progress, device-local migration, cache, offline foundation and account switching.
- **Prevents:** Deleting migration input with cache cleanup, showing another account's data or treating cached media as a protected entitlement.
- **Rule:** Backend owns application profile, progress and access decisions; Firebase owns authentication. Locally preserved migration inputs remain distinct from bounded, evictable media cache. Key private cache by account/access scope and immutable asset revision, expose only complete/integrity-verified entries, and retain active entries through playback. Purge account-private cache on account change; never delete unacknowledged migration input. Cache presence cannot bypass server admission. Explicit offline downloads are outside v1.

##### AD-9 — Environment and diagnostic boundaries [ADOPTED]

- **Binds:** Local/test/production builds, reliability work and release integrations.
- **Prevents:** Test access in production, leaked tokens/private URLs and unobservable playback failures.
- **Rule:** Inject backend endpoints and provider configuration per environment; never embed secrets or signed URLs in code or telemetry. Capture crash, playback, memory and latency diagnostics with version/platform/device and network/cache context in development and staged release. Add RevenueCat purchase/restore and backend user-flow events before release; client purchase state is advisory until backend access confirms it. A logical progress or flow event has one owner and stable event ID across retries; server deduplicates it, and delivery failure never blocks reading or listening.

##### AD-10 — One publication and protected-media admission boundary [ADOPTED]

- **Binds:** Catalog, candidate testing, story/meditation/lullaby playback, cache and URL renewal.
- **Prevents:** Candidate media leaking to ordinary users, failed processing replacing live media, revision mixing or cache bypassing access checks.
- **Rule:** Backend publication/access decisions distinguish candidate from live by language and experience; tester authorization is separate from subscription. Catalog visibility does not authorize full media. A protected start admits one immutable content/experience/language/revision manifest; renewal remains revision-addressed and rechecks the applicable access policy. Failed candidates never displace the last live revision. Shared content/asset identity and integrity semantics must be contracted before reader, catalog and media consumers implement against them.

##### AD-11 — Staged release with coexistence and recovery gate [ADOPTED]

- **Binds:** Mobile release, backend routing/migration, RevenueCat and old-system operations.
- **Prevents:** A store update exposing incompatible backend behavior to all users or retiring the old system before migration and entitlement evidence exists.
- **Rule:** Keep old clients on the old system while new-version users migrate; deploy backend compatibility before exposing new mobile cohorts. Expand staged store rollout only against reference-parity, signed-upgrade, migration, purchase/restore, event, crash-free and performance evidence by platform. Product owner and operations retain an explicit stop/containment and recovery decision; do not equate a store rollback with restoration of migrated data. Old-system shutdown is a separate decision, never implied by new-version release.

##### AD-12 — One composition root with explicit disposal ownership [ADOPTED]

- **Binds:** Feature creation, app session, navigation, media lifetime and account teardown.
- **Prevents:** Duplicate shared instances, service lookup hidden in business logic, double disposal and route closure terminating background playback.
- **Rule:** Use `get_it` with `injectable` in the composition root; register interfaces to implementations and constructor-inject dependencies. Resolve BLoCs/Cubits at provider/composition boundaries only. Each route/tab capability instance has one feature state owner, created by a factory and closed by its BlocProvider; retained tab ownership preserves its navigation state. AppSessionBloc, AppRouter and the media coordinator are app-scoped single instances; expose existing app BLoCs with BlocProvider.value and let the composition root alone dispose them. Feature close cancels its subscriptions, not shared playback. Account-switch orchestration invalidates and clears account-bound state before ready (AD-6), without retaining prior-user data in app singletons. Await async initialization before admitting dependent routes. Tests replace dependencies through the same contracts.

### UX Design Requirements

UX-DR1: **Visual tokens.** Preserve the DESIGN.md color roles, Lexend typography, spacing and shape tokens; retain screen-specific overrides, including gradient start buttons, illustrated auth backgrounds and the Sleep header fade. Compare equivalent device/language/content states. Source: DESIGN.md; NFR-7.

UX-DR2: **Assets and placeholders.** Map UI icons, fonts, logos and localized onboarding artwork separately from remote content covers and page media. Keep card/hero geometry during loading/failure; do not redraw embedded content text as UI text. Verify missing localized artwork fallback. Source: ASSET-MAP.md; FR-9, FR-15.

UX-DR3: **Shared controls.** Preserve three equal navigation items with selected/unselected icon states, 48dp primary buttons with 24dp corners, inputs with at least 48dp height and 12dp corners, and screen-specific disabled/error variants. Source: DESIGN.md Components.

UX-DR4: **Content cards and rows.** Preserve 3:4 story covers, 16dp clipping, 20dp placeholder frames, lock badges and card edge treatments; horizontal sections retain title, optional subtitle and all-items action. Lullaby cards retain the wide artwork and dark title band. Source: DESIGN.md; SCREENS 11, 13, 17.

UX-DR5: **Splash and onboarding.** Preserve the reference splash and three introduction pages with collage, indicator, wrapping title/body and per-page Skip/final Start behavior; keep alternate membership onboarding distinct. Startup destination follows authoritative session/migration state. Source: SCREENS 01–03; FR-1; AD-6.

UX-DR6: **Account choice sheet.** Preserve email/Google, existing-account and anonymous entry hierarchy over onboarding. Characterize Apple presentation on iOS; Android evidence does not establish iOS parity. Source: SCREENS 04; FR-2, FR-3.

UX-DR7: **Registration and login.** Preserve starry background, heading/panel proportions, rounded white panel, keyboard behavior, password visibility, required consent and separate marketing consent. Field errors remain next to their fields without clipping long consent text. Source: SCREENS 05–06; FR-2.

UX-DR8: **Recovery and verification.** Provide reset-request and action-link outcomes with distinct invalid/failure states and relevant new-password controls; characterize successful link flows where existing evidence is code-only. Source: SCREENS 07–08; FR-2.

UX-DR9: **Profile setup.** Preserve introduction and five skippable steps: purpose, age range, favorite genres, avatar and name. Retain segmented progress and selection-dependent Continue; verify populated values persist. Source: SCREENS 09; FR-4.

UX-DR10: **Library and navigation.** Preserve Library/Sleep/Profile shell, greeting, premium banner, search, vertical section flow and horizontal content rows. Preserve tab stacks/back behavior; omit the excluded parent-mode selector. CMS ordering/eligibility overrides older fixed composition. Source: SCREENS 11; EXPERIENCE.md; FR-1, FR-6.

UX-DR11: **Search and category.** Keep focused query/keyboard, unsent query, empty results, loading and network failure distinguishable. Preserve category app bar and cover grids. User screenshots establish reading, audio-story, meditation and lullaby result coverage. Matching, threshold and debounce still require OQ-4 evidence; do not infer them from endpoint names. Source: SCREENS 12–13; FR-6, FR-7.

UX-DR12: **Story detail.** Preserve hero, circular back button, duration/age pills, centered gradient start CTA, narration toggle, summary, credits and related content. Locked/free states remain distinct; opening details never starts audio. Source: SCREENS 14; FR-10, FR-11.

UX-DR13: **Reader.** Preserve full-screen page illustration, narration/music controls, manual/automatic transitions and recoverable loading. Exclude branching choices and the fixed second-ZIP boundary; first-page readiness and bounded preparation follow FR-12–15. Source: SCREENS 15; FR-12–15.

UX-DR14: **Completion.** Preserve completion message, rating, related-content and applicable paywall entry points. Capture a stable reference and actual action outcomes before accepting this surface. Source: SCREENS 16; FR-13, FR-16.

UX-DR15: **Sleep discovery.** Preserve the 250dp illustrated header/fade, wrapping title and separate lullaby, meditation and audio-story sections with correct card variants and all-items links. Source: SCREENS 17; FR-6, FR-8.

UX-DR16: **Listening details and player.** Meditation/audio-story details retain hero, metadata and explicit start. Lullaby card directly starts its player. Preserve title/artwork, time/seek, play/pause/exit and hidden shell tabs; preserve animated listening artwork and PRD loop/interruption behavior. Source: SCREENS 18–19; FR-11, FR-14, FR-15.

UX-DR17: **Profile and edit.** Preserve avatar/name, registered counters and verification reminder, anonymous membership variant and setting rows. Name/avatar editing must show real options and persist; missing avatar imagery in the reference capture is an evidence gap. Source: SCREENS 26–27; FR-4, FR-16.

UX-DR18: **Language.** Preserve the reference language-selection presentation while applying the latest user decision: TR/EN/PT are usable. Newly added languages can be remotely enabled for test use first, then enabled live. Do not automatically expose DE or other languages merely because translation assets exist. Verify UI/content refresh and persistence without stale-language results. Source: user decision 2026-09-27, SCREENS 28 (presentation only), FR-9 as overridden above.

UX-DR19: **Account management.** Preserve settings rows, old/new password validation, logout confirmation and deletion warning/reauthentication presentation. Deletion semantics follow new-system-only FR-5/OQ-6, never destructive legacy defaults. Source: SCREENS 29–31, 33; FR-5.

UX-DR20: **Feedback and documents.** Preserve multiline feedback, visible 0/200 counter, submission states and verified limits; preserve localized PDF viewer with back/loading/error, rating prompt and version information. Failed submission must not look successful. Source: SCREENS 10, 32; FR-18.

UX-DR21: **Premium surfaces.** Preserve app-owned premium banners, locks, membership entry and dismissal paths; verify real RevenueCat offerings, promotion and purchase/restore states with provider evidence. Billing Error 3 and emulator-paywall.png are not an accepted offer-layout baseline. Source: SCREENS 34; COVERAGE.md; FR-19, FR-20.

UX-DR22: **Responsive layout.** Use surface-specific phone/tablet rules: 600dp legacy tablet threshold, portrait baseline, horizontal rails, two-column story category grids with 16/32dp gaps, one-column lullaby categories, three-column tablet profile choices and centered approximately 343dp story CTA. Do not uniformly scale phone layouts. Source: TABLET.md; NFR-6, NFR-7.

UX-DR23: **Tablet forms and artwork.** Characterize code-derived tablet onboarding/auth padding, sheet proportions, hero ratios and tablet splash animation against actual devices. Preserve text-safe story crop; centered crop remains provisional. Source: TABLET.md; FR-15.

UX-DR24: **Text and accessibility.** Keep long localized titles/consent wrapping, all-items actions reachable and platform safe areas respected. Give tappable icons role/name/selected semantics; verify text scaling, at least 48dp touch targets and contrast over actual media without silently redesigning reference screens. These are acceptance checks, not proven legacy accessibility. Source: EXPERIENCE.md Accessibility Floor; SCREENS.md.

UX-DR25: **Evidence and gaps.** Attach screen/state/device/language/content evidence to each UI acceptance case. PNGs support observed appearance; mockups are illustrative. Android phone and emulated 800×1280dp tablet evidence do not prove iOS/iPad or real-tablet parity. Track code-only screens, populated profiles, successful social auth, stable completion and real offerings as open verification. Source: COVERAGE.md; RAW-SCREENSHOTS.md; TABLET.md.

### FR Coverage Map

FR-1: E3: initial onboarding (3.3), membership onboarding/conversion (3.16), authoritative startup/session and migration routing; E1–E2: incremental test shell (1.1/2.1), with production stack isolation (3.5) and full three-tab integration (3.12).

FR-2: E3: registration, sign-in, verification and recovery.

FR-3: E3: anonymous continuation and safe conversion.

FR-4: E3: profile setup and editing.

FR-5: E3: account management and preference ownership; E1: language consumption; E4: notification preference.

FR-6: E1: catalog/categories/editor selections/related reading; E2: listening discovery.

FR-7: E1: search against agreed OQ-4 scope; E2: listening result destinations where applicable.

FR-8: E1–E2: correct story reading/listening experience identity and destinations.

FR-9: E1: language-aware catalog/reader and eligible fallback; E2: listening; E3: durable profile preference.

FR-10: E1–E2: visibility, free access and denial of unauthorized playback; E6: integrated paywall and subscriber audience verification.

FR-11: E1: explicit reading start; E2: listening entry rules.

FR-12: E1: page-based reading and preparation.

FR-13: E1: reader controls/completion; E6: completion paywall integration.

FR-14: E2: listening controls, looping, interruptions and background playback.

FR-15: E1: bounded page preparation and image quality; E2: listening preparation and animated artwork.

FR-16: E1–E2: durable progress/completion and lifecycle outcomes for controlled users; E3: migrated history/profile statistics and production-account isolation.

FR-17: WITHDRAWN; no implementation epic. Historical records remain governed by E3/OQ-3.

FR-18: E4: feedback, documents, announcements, permissions, rating, version and deep links.

FR-19: E6: existing subscribers, purchase, restore and entitlement refresh.

FR-20: E6: promotions, paywall and membership management.

FR-21: E7: backend user-flow delivery; event ownership/retry boundaries considered in every producer epic.

FR-22: E5: mobile publication verification and backend/CMS dependency acceptance.

FR-23: E5: authorized candidate consumption and visible test/revision markers.

FR-24: E5: processing-failure isolation and actual readiness evidence; backend/CMS implementation remains externally owned.

FR-25: E1–E2: protected live-media admission and revision continuity; E5: candidate/live matrix; E6: paid-access integration.

FR-26: E3: one-time account migration and later-device behavior.

FR-27: E3: lossless, retryable migration and blocking pending/error states.

FR-28: E3: anonymous local import, durable receipt and safe cleanup.

FR-29: E3: AD-5 prerequisite and upgrade integration; E7: signed release-update/coexistence evidence.

## Epic List

### Epic 1: Discover content and read illustrated stories

A controlled, authorized development/test user can browse the real catalog, find an eligible story and read it through completion with persisted progress and reference visuals.

**FRs covered:** FR-6–13, FR-15–16, FR-25; navigation integration with FR-1.

**Implementation notes:** Resolve relevant OQ-1/4/8 evidence and production-shaped contracts before implementing the affected stories. Protected live-media authorization is mandatory. No production auth/migration work is implied.

### Epic 2: Listen to lullabies, meditations and audio stories

The same controlled user can discover and play listening experiences with correct lifecycle behavior and system controls.

**FRs covered:** FR-6–10, FR-11, FR-14–16, FR-25.

**Implementation notes:** Build on E1 media ownership and typed contracts. Extend the same coordinator rather than introducing an independent competing player.

### Epic 3: Keep my account and data when upgrading

New and returning users enter through real identity flows; existing users retain local/account data, preferences and access without repeated account migration.

**FRs covered:** FR-1–5, FR-9, FR-16, FR-26–29.

**Implementation notes:** AD-5 must pass BEFORE auth/migration feature development. Resolve OQ-3/6 before migration acceptance. Integrate real session gates, account switching and private-state invalidation into E1–E2.

### Epic 4: Manage support and communication

Users can submit feedback, read documents, manage notifications and reach valid destinations from announcements and links.

**FRs covered:** FR-5 notification preference, FR-18.

**Implementation notes:** Depends on E3 account/session behavior and earlier content destinations; characterize OQ-9/provider contracts.

### Epic 5: Test content safely before publication

Authorized testers consume candidate revisions with a clear marker while ordinary users remain on eligible live media.

**FRs covered:** FR-22–25.

**Implementation notes:** Depends on E1–E3 consumption/session capabilities and separately verified backend/CMS publication/processing contracts. Candidate isolation must already hold for ordinary users in E1–E2.

### Epic 6: Purchase and recover premium access

Users retain existing paid access, purchase, restore and redeem supported promotions with authoritative backend access confirmation.

**FRs covered:** FR-10, FR-19–20; paid-access integration of FR-13/25.

**Implementation notes:** Late pre-release integration, using real store/provider evidence and resolved OQ-7. Complete all earlier locked-content and completion paywall entry points here.

### Epic 7: Receive a verified and recoverable replacement release

The replacement has verified backend flow events, platform parity, reliability, signed-update evidence and staged rollout/recovery controls.

**FRs covered:** FR-21, FR-29; release verification of every active FR and NFR.

**Implementation notes:** Depends on E1–E6. OQ-2/7/9/10 release evidence must close; preserve legacy coexistence and separate shutdown approval.

## Epic 1: Discover content and read illustrated stories

A controlled, authorized development/test user can discover eligible content and complete an illustrated story with reference visuals and reliable progress.

Stories 1.1–1.12 are approved for planning. The first working package remains Library listing (1.1) followed by card-to-detail opening (1.2); open evidence/contract gates remain unchanged. Implementation remains outside this planning session.

### Story 1.1: Discover eligible content on the Library home screen

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to see category rows and content previews appropriate to my language and access audience,
So that I can see what is available to read.

**Requirements:** FR-6, FR-9, FR-10 (listing scope); NFR-5–8; UX-DR1–4, UX-DR10, UX-DR22–25.

**Prerequisites:** Agreed home-list contract and redacted response examples covering category order, preview limits, language/fallback and audience eligibility; authorized development/test access; existing reference design assets. AD-5 does not block this story. Production auth/migration is excluded. Card destinations are implemented in the next story; this story accepts listing behavior only. Verify the applicable EXT-CATALOG closure records in the [external dependency register](#external-dependency-register). Verify the toolchain/generator gate and relevant UX-DR24 checks under Shared readiness and acceptance checks. This first shell consumer owns Library navigation; later destination capabilities extend the same shell without being claimed as complete here.

**Acceptance Criteria:**

**Given** a real backend dataset with known eligible categories and preview items for a selected language/access audience,
**When** the Library opens,
**Then** it shows categories in CMS order with the contracted preview limits and matching content identities, titles, artwork and lock states,
**And** categories with no eligible content are absent; the expected preview set is checked against the contract rather than assumed to be the entire catalog.

**Given** content is loading,
**When** the response or artwork has not arrived,
**Then** reference-compatible loading placeholders retain card geometry,
**And** content does not shift into collapsed artwork areas.

**Given** the catalog request fails,
**When** the Library displays its outcome,
**Then** it shows a recoverable error distinct from an empty catalog,
**And** retry displays the returned content when the request succeeds.

**Given** a successful response contains no eligible categories/items,
**When** the Library renders it,
**Then** it shows the defined empty state rather than a network error or broken cards.

**Given** free and locked content are eligible for listing,
**When** the Library renders them,
**Then** their lock indicators match backend access information,
**And** listing visibility neither grants protected full-media access nor requires full playback payloads.

**Given** selected-language and fallback cases in the agreed contract,
**When** the Library loads or its test language context changes,
**Then** only eligible published language/fallback results appear,
**And** stale results from the previous language cannot replace the current result; production preference persistence remains in Epic 3.

**Given** matching reference device size, language and content,
**When** phone and tablet-size Library surfaces are compared,
**Then** typography, spacing, card aspect ratios, horizontal rows and their loading states preserve the in-scope reference appearance,
**And** tablet layout uses its documented rules rather than uniform phone scaling; emulated evidence is not labeled real-device proof.

**Given** the controlled-test Library shell,
**When** its bottom navigation and back behavior are exercised,
**Then** the three reference navigation items preserve their layout and selected/unselected states,
**And** Library owns its initial stack/scroll context; Sleep and Profile destinations remain explicitly incomplete until their consuming stories, without adding production-auth bypasses.


### Story 1.2: Open the selected story detail from Library

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to open the story I selected from Library and inspect its details,
So that I can decide whether to start reading it.

**Requirements:** FR-6, FR-8–11, FR-25 (metadata/access boundary); NFR-5–7; UX-DR2–4, UX-DR10, UX-DR12, UX-DR22–25.

**Prerequisites:** Story 1.1; agreed content/experience/language identity and detail response/error contracts, with representative real-backend fixtures. This story does not require the later reader implementation; its accepted outcome is the detail surface. Production auth/migration and real paywall integration remain in Epics 3 and 6. Verify the applicable EXT-CATALOG closure records in the [external dependency register](#external-dependency-register). Verify detail-related-content fixtures for valid and absent recommendations.

**Acceptance Criteria:**

**Given** a story card in Library,
**When** the reader taps it,
**Then** the corresponding reading-experience detail opens in the correct selected or contractually eligible fallback language,
**And** its cover, title, age, duration, summary and contributor information match the selected content and backend response.

**Given** the detail opens successfully,
**When** it is displayed without a start action,
**Then** neither narration nor reading starts automatically,
**And** the reference hero, metadata and start-action presentation are preserved on the relevant phone/tablet layouts.

**Given** free or locked content,
**When** its detail is rendered,
**Then** the start-action presentation reflects the access state,
**And** metadata visibility does not authorize protected media; the actual purchase/paywall journey remains explicitly pending until Epic 6.

**Given** detail data is loading, its request fails or the selected content is no longer available,
**When** that state is rendered,
**Then** loading, recoverable network failure and unavailable-content outcomes are distinguishable,
**And** retry is available where recovery is appropriate without presenting stale content as the selected story.

**Given** the reader entered detail from a scrolled Library,
**When** the reader returns,
**Then** Library retains its previous scroll position and navigation context.

**Given** rapid/repeated taps or responses arriving out of order,
**When** navigation resolves,
**Then** duplicate detail routes are not stacked for the same repeated action,
**And** a stale response cannot replace the currently selected story with a different one.

**Given** eligible related content on story detail,
**When** a recommendation is selected and the reader returns,
**Then** the correct content and reading experience detail opens under the same language/access rules,
**And** the originating detail/navigation context is preserved.

**Given** no eligible related content is available,
**When** story detail renders its recommendation area,
**Then** no broken card or invalid target is shown; completion-screen recommendations remain Story 1.5 scope.


### Story 1.3: Start reading the selected story

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to open the first page of my selected story with my narration preference,
So that I can begin reading without waiting for the entire story to download.

**Requirements:** FR-11–12, FR-13 (initial narration preference), FR-15, FR-25; NFR-2–3, NFR-5–8; UX-DR12–13, UX-DR22–25; AD-3, AD-7–10.

**Prerequisites:** Story 1.2; agreed protected-start and immutable manifest contracts, actual first-page image/audio outputs with readiness/integrity evidence, and authorized development/test access. Resolve the relevant OQ-1/8 contracts before implementation. Introduce the application-scoped media coordinator for this first consuming capability; no dependency on the later listening epic. Numerical performance budgets remain subject to OQ-2 and are not invented here. Verify the applicable EXT-MEDIA closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** the reader is viewing an eligible story detail,
**When** the reader taps Read,
**Then** the backend validates access and admits the correct content, reading experience, language and immutable revision,
**And** denied access cannot be bypassed through metadata or cached assets.

**Given** an admitted story with multiple pages,
**When** the first page image and narration audio are ready and verified,
**Then** reading can start without downloading the entire story,
**And** preparation remains bounded rather than speculatively fetching all pages.

**Given** the narration preference selected on detail,
**When** the first page opens,
**Then** the matching first-page narration starts if enabled,
**And** no narration starts automatically if that preference is disabled.

**Given** first-page preparation is pending or fails because of a network error or missing/corrupt media,
**When** the reader displays the preparation outcome,
**Then** it shows loading followed by a recoverable retry state for failure instead of waiting indefinitely,
**And** an incomplete or corrupt asset is never treated as ready.

**Given** repeated Read taps or navigation away during preparation,
**When** requests complete,
**Then** the same action does not create duplicate reader routes or overlapping audio,
**And** stale results after leaving cannot open the reader or start playback.

**Given** representative phone/tablet sizes and story artwork,
**When** the first page is displayed,
**Then** its aspect ratio and embedded text readability are preserved using verified text-safe crop behavior,
**And** memory and first-page readiness latency are measured with device/network/cache context without logging credentials or private media URLs.

### Story 1.4: Navigate story pages manually and automatically

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to change pages myself or advance automatically when narration ends,
So that I can follow the story at my preferred pace without mismatched pages or audio.

**Requirements:** FR-12–13, FR-15; NFR-1–3, NFR-7; UX-DR13, UX-DR22–25; AD-3, AD-8.

**Prerequisites:** Story 1.3; characterized reference navigation/automatic-advance behavior and agreed bounded preparation parameters under OQ-1/8. The existing media coordinator owns transitions. Completion presentation and durable completion recording are subsequent scope; page navigation is independently testable.

**Acceptance Criteria:**

**Given** an admitted story and an active page,
**When** the reader navigates forward or backward,
**Then** the requested valid page is displayed,
**And** enabled narration corresponds to that page within the same admitted language/revision.

**Given** automatic advance is enabled and a following page exists,
**When** the active page narration ends,
**Then** the reader advances exactly once using the reference transition animation.

**Given** manual navigation and narration completion occur concurrently,
**When** the coordinator processes them,
**Then** no page is skipped by competing transitions,
**And** a stale completion from the previous page cannot advance the new active page.

**Given** rapid forward/backward navigation,
**When** asynchronous image/audio preparation completes out of order,
**Then** the latest selected page remains authoritative,
**And** stale results produce neither a wrong image nor overlapping audio.

**Given** the requested page is not ready,
**When** navigation reaches it,
**Then** a loading state is shown without playing the previous page narration against the new page,
**And** preparation failure offers retry rather than indefinite waiting.

**Given** a multi-page story,
**When** playback and manual navigation trigger preparation,
**Then** only the contracted bounded lookahead is prepared,
**And** the reader does not speculatively download the entire story.

**Given** the first or last page is active,
**When** navigation requests movement outside the valid page range,
**Then** no invalid page or asset request is made,
**And** end-of-story completion presentation is left to the following story rather than implemented as an out-of-range page.

### Story 1.5: Complete a story and preserve its completion record

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to see the completion screen when I finish a story and retain my reading record,
So that my completed reading is recognized reliably.

**Requirements:** FR-13, FR-16; FR-6 (related content); NFR-4–7; UX-DR14, UX-DR21–25; AD-3, AD-7, AD-9.

**Prerequisites:** Stories 1.2–1.4; characterized reference completion trigger for narrated and silent/manual reading, stable completion-screen evidence, evaluation/related-content contracts, and a durable idempotent completion/duration contract for the controlled test user. OQ-1 must settle the trigger before implementation. User approval of this story does not select a trigger: merely opening the last page is not assumed to complete reading. Production-account migration is Epic 3 scope; backend flow analytics is separate Epic 7 scope. Verify the applicable EXT-HISTORY closure records in the [external dependency register](#external-dependency-register). Resolve durable local pending-record persistence/recovery, stable account/session/event identities, acknowledgement and storage-failure handling before implementation; do not select a storage engine here. Business progress/history records remain distinct from evictable media and bounded flow telemetry. Characterize reading-duration rules for narrated/silent/manual reading, page-preparation waits and background/lock intervals with OQ-1 reference evidence and the backend history contract.

**Acceptance Criteria:**

**Given** the reference-defined completion condition for narrated or silent/manual reading,
**When** that condition occurs,
**Then** the completion screen opens exactly once,
**And** each reading mode is verified independently against the reference rather than assuming last-page visibility alone means completion.

**Given** a completed reading session,
**When** its completion and duration are persisted or retried,
**Then** they belong to the correct controlled user, story and reading session,
**And** repeated delivery of the same logical event does not double-count completion or duration; a separate legitimate reading session is not silently conflated with that retry.

**Given** connectivity fails while completion is being recorded,
**When** the story ends,
**Then** the completion screen remains usable,
**And** the record can be retried safely without claiming server-confirmed persistence before acknowledgement.

**Given** the completion screen offers evaluation and related content,
**When** those controls are displayed and used,
**Then** their behavior matches the characterized reference,
**And** a related-content selection opens its correct detail; no invalid card is shown when no eligible recommendation exists.

**Given** the reference includes a completion premium entry point,
**When** completion is displayed,
**Then** that entry point is retained in the screen scope,
**And** actual paywall/purchase integration remains explicitly pending until Epic 6.

**Given** matching content, language and phone/tablet reference conditions,
**When** completion appearance is compared,
**Then** the in-scope reference layout and controls are preserved,
**And** incomplete reference evidence is resolved before visual acceptance is claimed.

**Given** progress, duration or completion delivery is unacknowledged when the process terminates,
**When** the application relaunches and delivery can resume,
**Then** durably pending records are recovered with their original account, session and stable event identities and retried until the contracted acknowledgement,
**And** no record is lost or counted twice because of process restart; an acknowledgement lost after server success is reconciled idempotently without attributing the record to the next account.

**Given** local pending-record persistence fails,
**When** recording continues or recovery is offered,
**Then** the resolved contract's failure behavior is shown without claiming durable or server-confirmed success,
**And** recording failure does not block content use or silently turn business records into evictable media cache.

**Given** timed narrated and silent/manual reading fixtures with page preparation waits and pauses,
**When** duration is calculated and persisted,
**Then** recorded totals match the characterized duration rules for each interval,
**And** excluded intervals and manual page jumps cannot inflate the total; elapsed wall-clock time is not assumed to equal eligible reading time.


### Story 1.6: Control narration and music while reading

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to change narration and music settings while reading,
So that I can use the story audio controls as in the reference application.

**Requirements:** FR-13; NFR-1, NFR-7; UX-DR13, UX-DR24–25; AD-3.

**Prerequisites:** Stories 1.3–1.4; reference control inventory and OQ-1 characterization, including whether re-enabling narration starts the current page from the beginning or resumes its position. This planning approval does not select that unresolved behavior. Extend the existing media coordinator; do not add competing feature-local players.

**Acceptance Criteria:**

**Given** an active illustrated story,
**When** its audio controls are shown and operated,
**Then** narration and music controls preserve their reference appearance and function.

**Given** narration and music have independently selected settings,
**When** the reader disables either channel,
**Then** the other channel setting remains unchanged.

**Given** the reader has selected audio settings,
**When** the active page changes,
**Then** those selections remain effective,
**And** a disabled channel does not spontaneously turn back on.

**Given** narration has been disabled,
**When** the reader enables it again,
**Then** the current page narration is used,
**And** its restart/resume position follows the characterized reference behavior.

**Given** rapid repeated changes to audio controls,
**When** asynchronous commands and player callbacks resolve,
**Then** no duplicate audio, wrong-page narration or unexpected page advance occurs.

**Given** any narration/music playback state,
**When** controls render their visible and accessibility states,
**Then** both accurately reflect the actual audio state.

### Story 1.7: Preserve reading sessions through interruptions

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to return from a brief interruption without losing my reading session,
So that I can continue with consistent page, audio and progress state.

**Requirements:** FR-13–16 (reader lifecycle scope); NFR-1, NFR-4, NFR-6, NFR-8; UX-DR13, UX-DR25; AD-3, AD-8–9.

**Prerequisites:** Stories 1.3–1.6; agreed durable progress/duration contract and characterized background/lock behavior. Continue using the application-scoped coordinator and controlled test user. Production account switching and migration remain in Epic 3. Dedicated listening experiences are Epic 2 scope. Verify the Story 1.5 duration rules for background, screen-lock and interruption intervals before lifecycle acceptance.

**Acceptance Criteria:**

**Given** an active reading session and an application process that remains alive,
**When** the app moves to the background and returns,
**Then** the open story and current session are preserved rather than restarted from page one.

**Given** background or screen-lock transitions during reading,
**When** playback and page state are updated,
**Then** the displayed page, narration and logical session remain consistent,
**And** returning to the reader reflects the current session rather than stale screen-local state.

**Given** a call or headset removal interrupts playback,
**When** the interruption occurs,
**Then** playback pauses,
**And** it does not resume without an explicit user action.

**Given** the application has been fully closed,
**When** it is launched again,
**Then** the home screen opens rather than automatically restoring the previous reader,
**And** no previous narration starts automatically; production startup/migration gates will be integrated in Epic 3.

**Given** persisted reading progress and duration for a session,
**When** lifecycle transitions or delivery retries cause that session to be processed again,
**Then** stored progress is not lost and duration is not counted twice.

**Given** representative supported physical Android and iOS devices,
**When** background/foreground, screen-lock, interruption and cold-relaunch scenarios are exercised,
**Then** each of the preceding outcomes is verified with platform-specific evidence,
**And** emulator-only results are not presented as physical-device acceptance.

**Given** measured foreground, background, screen-lock and interruption intervals,
**When** the session returns or its duration delivery is retried,
**Then** totals match the characterized reading-duration rules,
**And** intervals are neither lost nor counted twice; preservation of the session does not imply counting all background time.


### Story 1.8: Browse all content in a category

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to browse all eligible content in a Library category and open a selected story,
So that I can discover content beyond the home-screen previews.

**Requirements:** FR-6, FR-9–10; NFR-5–7; UX-DR4, UX-DR10–11, UX-DR22–25.

**Prerequisites:** Stories 1.1–1.2; agreed OQ-4 category/pagination/order/error contract and representative real-backend fixtures. Expected items are evaluated against the selected language and access audience. No new backend schema or endpoint is selected by this story. Verify the applicable EXT-CATALOG closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a category row in Library,
**When** the reader taps its all-items action,
**Then** the correct category title and eligible content for the selected language are shown.

**Given** a category with more items than one response page,
**When** the reader progresses through the list,
**Then** all eligible items are loaded without omissions or duplicates according to the agreed pagination contract,
**And** reaching the end does not cause repeated unnecessary page requests.

**Given** some category items have loaded and a subsequent page request fails,
**When** the failure is displayed and the reader retries,
**Then** already loaded items remain available,
**And** retry obtains the missing page without duplicating existing items or skipping ahead.

**Given** a story in the category list,
**When** the reader selects it and then returns from its detail,
**Then** the correct story detail has opened,
**And** the category list retains its previous position.

**Given** first-load, successful empty-category and network-failure outcomes,
**When** the category screen renders,
**Then** loading, empty and error states are distinguishable.

**Given** free and locked items on supported phone/tablet layouts,
**When** the category list is displayed,
**Then** lock indicators, card appearance and layout match the eligible reference behavior,
**And** listing visibility does not grant protected media access.

### Story 1.9: Search content in grouped results

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to find content in clearly identified reading and listening result groups,
So that I can choose the content and experience I am looking for.

**Requirements:** FR-7–10; NFR-5–7; UX-DR4, UX-DR11, UX-DR24–25.

**Prerequisites:** Stories 1.1–1.2; resolve OQ-4 matching, grouping/count, ranking, threshold/debounce, pagination and error contracts with real request/response evidence. User screenshots establish reading-story, audio-story, meditation and lullaby result coverage. Content name, description and contributor-name matching are user-recalled behavior requiring field-isolated validation; approval does not certify current backend support. Contributor matches return content, not person records. Verify the applicable EXT-CATALOG closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** eligible search results for the selected language and access audience,
**When** results are rendered,
**Then** reading stories, audio stories, meditations and lullabies are represented in reference-compatible groups with result counts according to the verified contract,
**And** story covers, audio-story/meditation artwork and wide lullaby cards preserve their distinct reference presentation.

**Given** matching examples for content name, description and contributor names,
**When** the verified search contract is exercised,
**Then** associated content is returned for the supported matches,
**And** no standalone contributor/person result is substituted for that content; the field-isolated evidence is retained before implementation acceptance.

**Given** eligible free and locked results,
**When** search displays them,
**Then** lock indicators reflect access state,
**And** search visibility cannot grant protected full-media access.

**Given** an empty query, pending request, successful search with no matches or failed request,
**When** the search surface renders,
**Then** query-empty, loading, no-results and network-error states are distinguishable,
**And** the query can be cleared and recoverable errors retried.

**Given** rapidly changing queries and out-of-order responses,
**When** responses arrive,
**Then** a stale response cannot replace results for the current query,
**And** request triggering follows the verified character threshold and debounce behavior rather than invented defaults.

**Given** a reading-story result,
**When** the reader selects it and returns from detail,
**Then** the correct story and reading experience have opened,
**And** the query and result-list position are preserved.

**Given** listening results in the grouped response,
**When** Epic 1 search presentation is accepted,
**Then** their identity and experience are retained accurately,
**And** their functional detail/player destinations remain explicitly assigned to Epic 2 and are not claimed as complete by this story. Cross-experience search acceptance requires that later integration; unavailable destinations must not be shipped as completed user flows.

**Given** the verified search contract paginates one or more result groups,
**When** the user loads subsequent search results,
**Then** all eligible matches are reachable without omissions or duplicates under that contract,
**And** group counts and end-of-results behavior remain consistent with the verified semantics.

**Given** a subsequent search-results page fails after earlier results loaded,
**When** the error is shown and retried,
**Then** existing results and query context remain available and retry obtains the failed page without skipping or duplicating matches,
**And** an older query/language/account page cannot be appended to the current results. A non-paginated verified contract does not acquire pagination by inference.


### Story 1.10: Discover and read content in enabled languages

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want to use supported languages and see content consistently in my selected language,
So that I can discover and read appropriate content without mixed-language results.

**Requirements:** FR-9 (latest user decision), FR-6–8, FR-25; NFR-5–7; UX-DR1–2, UX-DR18, UX-DR22–25; AD-8, AD-10.

**Prerequisites:** Stories 1.1–1.4, 1.8–1.9; agreed remote language visibility/test-audience and fallback contracts. Turkish, English and Portuguese content availability is user-reported; verify actual readiness for the tested experiences. Implement controlled-user language consumption here, production profile persistence/migration in Epic 3, listening integration in Epic 2 and full authorized test/live verification in Epic 5. Tests of audience separation must not depend on implementing production auth in this story.

**Acceptance Criteria:**

**Given** the currently enabled language set,
**When** language choices are displayed,
**Then** Turkish, English and Portuguese are usable,
**And** German or other languages are not automatically exposed because translation assets exist.

**Given** a newly added language and configured test audience,
**When** remote configuration enables that language only for testing,
**Then** it is available to the authorized test audience but absent from ordinary users' language choices, catalog results and fallback outcomes,
**And** enabling it live subsequently makes it available to ordinary users without treating unready or unpublished media as accessible.

**Given** a language change for the controlled test user,
**When** Library, category, search and newly opened details load,
**Then** they use the same selected-language context,
**And** delayed responses or cached results from the previous language cannot replace current-language results.

**Given** content is unavailable in the selected language,
**When** English fallback is evaluated,
**Then** it is used only when the English experience satisfies the applicable publication and access rules,
**And** fallback cannot expose test-only languages or unauthorized content.

**Given** an admitted reading session,
**When** its page images and narration are consumed,
**Then** they remain consistent with the admitted language and immutable revision.

**Given** long localized text on supported phone/tablet layouts,
**When** titles, buttons and controls are rendered,
**Then** they remain readable and operable without clipping that prevents use.

### Story 1.11: Renew media access without changing the active story revision

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want expired media access to be renewed for my current story session,
So that I can continue reading without mixing content versions or losing my position.

**Requirements:** FR-12, FR-25; NFR-4–5, NFR-9; UX-DR13; AD-3, AD-7–10.

**Prerequisites:** Stories 1.3–1.4; agreed OQ-8 revision-addressed renewal, access-denial, withdrawal and retention contracts. URL lifetime and withdrawal/revocation policy remain open until that contract is settled; this story does not invent a TTL or guarantee continuation after revocation. Verify the applicable EXT-MEDIA/EXT-RETENTION closure records in the [external dependency register](#external-dependency-register). Resolve interrupted-download continuation and safe-restart/integrity behavior in OQ-8 before implementing the shared NFR-9 transfer foundation; retaining playback position alone does not satisfy this requirement.

**Acceptance Criteria:**

**Given** a media URL expires during an admitted session,
**When** renewal is requested,
**Then** the backend rechecks applicable access and renews the URL only if permitted.

**Given** renewal succeeds,
**When** the reader consumes renewed media,
**Then** content, language and revision match the admitted session,
**And** page images and narration from different revisions are never mixed.

**Given** a new story revision is published during reading,
**When** the active session requests more media or renews a URL,
**Then** that session does not automatically switch to the new revision.

**Given** renewal fails because of a network error,
**When** the reader handles the failure,
**Then** the current page and progress are preserved,
**And** loading and recoverable retry states are presented where needed.

**Given** the backend denies media access,
**When** the client handles that response,
**Then** cached media cannot be used to bypass the applicable authorization policy.

**Given** the reader has switched to another story while renewal is pending,
**When** the old request completes,
**Then** its result cannot modify the new media session.

**Given** a partially received media asset and a connection or process interruption,
**When** its transfer is recovered under the resolved OQ-8 continuation contract,
**Then** the verified partial data is used only for the same immutable asset/revision and applicable account/access scope,
**And** evidence demonstrates continuation of the remaining transfer, not merely restoration of the playback position.

**Given** a renewed URL, rejected continuation, unsupported continuation response or changed/mismatched asset identity,
**When** transfer recovery is evaluated,
**Then** the contracted continuation or safe-restart outcome is followed without combining incompatible bytes,
**And** the complete asset passes integrity checks before being exposed as a ready cache entry; partial playback follows only the separately contracted streaming-readiness rules. This adds no user-controlled offline-download UI.


### Story 1.12: Read reliably through long and repeated sessions

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader,
I want long reading sessions and repeated story visits to remain stable,
So that I can read without accumulating slowdown or unexpected application termination.

**Requirements:** FR-12–16 (reading scope); NFR-1–3, NFR-4, NFR-6–9; UX-DR13, UX-DR22–25; AD-3, AD-8–9.

**Prerequisites:** Stories 1.3–1.7 and 1.11; representative heavy content and supported device/network cases; agreed OQ-2 memory/latency budgets and test durations plus OQ-8 cache budget/eviction parameters. These numbers remain open and must be agreed before quantitative acceptance. Diagnostics and correctness originate in earlier stories; this story verifies sustained use and addresses measured failures, not a deferral of all quality work.

**Acceptance Criteria:**

**Given** long reading, rapid paging and repeated story open/close scenarios,
**When** they are exercised over the agreed test duration,
**Then** no crash, freeze or overlapping unrelated audio occurs.

**Given** repeated sessions after warm-up,
**When** memory measurements are compared across the agreed cycles,
**Then** there is no unexplained continuous memory growth,
**And** image/audio resources no longer needed by the active session are released.

**Given** the configured media cache budget,
**When** repeated reading requires eviction,
**Then** cache use remains within that budget,
**And** assets required by active playback are not removed while in use.

**Given** user history, progress or retained migration inputs exist separately from evictable media,
**When** media cache cleanup runs,
**Then** those durable records and unacknowledged migration inputs are not deleted.

**Given** representative supported devices including low-memory devices and agreed poor-network conditions,
**When** first-page preparation and page transitions are measured,
**Then** the evidence records device/network/cache context and satisfies the agreed memory/latency budgets,
**And** emulator evidence is not substituted for required physical-device results.

**Given** memory or latency optimizations,
**When** the optimized reader is compared with reference content,
**Then** visual readability and audio integrity remain intact rather than being sacrificed for lower resource use.

### Epic 1 planning review

Twelve stories have been individually approved for planning; no implementation or runtime acceptance is claimed. Dependencies point to earlier stories or explicit external contract/evidence prerequisites.

| Requirement | Story coverage and remaining integration |
| --- | --- |
| FR-6 | 1.1/1.2 home previews, destinations and verified valid/absent detail recommendations; 1.8 complete categories; 1.5 completion recommendations. OQ-4 fixtures must include editor-selected home content and valid/absent recommendations. |
| FR-7 | 1.9 grouped search plus contract-dependent result pagination/error/retry; listening destinations complete in Epic 2. Matching fields remain user-reported pending evidence. |
| FR-8 | 1.2/1.3 reading-experience identity; cross-experience identity and listening independence require Epic 2. |
| FR-9 | 1.1/1.10 language consumption and latest TR/EN/PT decision; durable preference/migration in Epic 3, authorized test/live verification in Epic 5. |
| FR-10 | 1.1/1.2/1.8/1.9 listing and access separation; full paywall and subscriber-audience integration in Epic 6. |
| FR-11 | 1.2/1.3 explicit reading start; listening entry rules in Epic 2. |
| FR-12 | 1.3/1.4 preparation, page navigation and retry; sustained reliability in 1.12. |
| FR-13 | 1.4/1.5/1.6 transitions, completion and controls; completion paywall in Epic 6. |
| FR-15 | 1.3/1.4/1.12 bounded preparation, readable crop and sustained resource use; listening preparation/artwork in Epic 2. |
| FR-16 | 1.5/1.7 completion, progress, duration and lifecycle; full production history/statistics and migration in Epic 3. |
| FR-25 | 1.3/1.11 protected start and immutable revision renewal; test/live matrix in Epic 5, paid-access integration in Epic 6. |

Before implementation, close the relevant OQ-1 control/completion evidence, OQ-4 catalog/search contracts, OQ-8 media/access/cache contracts and controlled test-access mechanism. OQ-2 numeric budgets and durations must be established before quantitative reliability acceptance. AD-5 remains an Epic 3 auth/migration development gate rather than blocking Epic 1.

Source PRD language-availability wording is superseded by the explicit decision recorded here; source synchronization remains a documentation follow-up, not permission to apply the old language rule.

## Epic 2: Listen to lullabies, meditations and audio stories

A controlled, authorized development/test user can discover listening experiences and play them with the correct reference entry flows, playback controls, lifecycle behavior and system media controls.

**Scope:** FR-6–10, FR-11, FR-14–16, FR-25; NFR-1–9 as applicable; UX-DR1–4, UX-DR15–16, UX-DR18, UX-DR21–25. Reuse Epic 1 transport, content identity, diagnostics, language configuration and the single application-scoped media coordinator. Do not introduce independent competing players. Stories 2.1–2.10 are approved for planning. The user approved proceeding to Epic 3 after the Epic 2 planning review; open contract/evidence gates remain unchanged.

**Planning constraints:** AD-5 still gates Epic 3 auth/migration development, not controlled-user listening. Real backend admission remains mandatory. Lullaby selection starts its player directly; meditation/audio-story selection opens detail and requires an explicit start action. Looping is specific to lullabies. Language visibility follows the latest TR/EN/PT and test-to-live decision. Full purchase/paywall acceptance remains Epic 6 scope. Search-to-listening destinations deferred from Story 1.9 must be completed in this epic. Resolve relevant OQ-1/4/8 contracts before implementing the affected stories and OQ-2 budgets before quantitative acceptance.

### Story 2.1: Discover listening content on the Sleep screen

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want to see lullabies, meditations and audio stories in their respective sections,
So that I can discover content to listen to.

**Requirements:** FR-6, FR-8–10; NFR-5–7; UX-DR1–4, UX-DR15, UX-DR18, UX-DR22–25.

**Prerequisites:** Epic 1 shared catalog transport, language/access context and controlled test access; agreed Sleep catalog ordering, eligibility and error contracts under OQ-4/8. Actual listening start and detail destinations belong to subsequent stories; this story accepts listing only. Verify the applicable EXT-CATALOG closure records in the [external dependency register](#external-dependency-register). Story 1.1 owns the initial shell; extend that same shell with Sleep navigation and retained independent stacks rather than replacing Library navigation.

**Acceptance Criteria:**

**Given** eligible listening content exists,
**When** the Sleep tab opens,
**Then** lullabies, meditations and audio stories appear in their correct sections and agreed CMS order.

**Given** a section has no eligible content for the selected language and access audience,
**When** Sleep renders,
**Then** that empty section is not displayed.

**Given** free and locked listening items,
**When** their cards appear,
**Then** lullaby cards and meditation/audio-story cards preserve their distinct reference artwork/layout,
**And** lock indicators accurately reflect access state without granting protected media access.

**Given** matching phone/tablet, language and content reference conditions,
**When** Sleep appearance is compared,
**Then** its illustrated header, title, section descriptions and horizontal content rows preserve the in-scope legacy appearance.

**Given** loading, successful empty results or network failure,
**When** the screen displays the outcome,
**Then** those states are distinguishable,
**And** a recoverable request can be retried successfully.

**Given** a STORY whose reading experience is not ready but whose audio-story experience is ready, published and eligible,
**When** Sleep lists audio stories,
**Then** the unavailable reading experience does not prevent that audio-story experience from appearing.

**Given** Library and Sleep have independent navigation/scroll contexts,
**When** the user switches between those tabs and uses system back,
**Then** each ready tab retains its own context and back behavior matches the verified reference,
**And** changing tabs does not create duplicate shells or media sessions; full Profile integration remains assigned to Story 3.12.


### Story 2.2: Start listening directly from a lullaby card

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want to tap a lullaby card and enter its player directly,
So that I can begin listening without an intermediate detail screen.

**Requirements:** FR-11, FR-14 (basic player controls), FR-15, FR-25; NFR-1, NFR-5–7; UX-DR16, UX-DR22–25; AD-3, AD-7–10.

**Prerequisites:** Story 2.1 and Epic 1 shared media coordinator/transport; agreed protected lullaby-start contract and actual audio/player artwork, including animation. Characterize reference player exit behavior under OQ-1 before implementation. Looping, seeking and background/system controls are detailed in subsequent stories. Actual purchase/paywall integration remains Epic 6 scope. Verify the applicable EXT-MEDIA closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** an accessible lullaby card,
**When** the listener selects it,
**Then** its player opens without an intermediate detail screen,
**And** playback starts when the required media is ready.

**Given** the selected lullaby is displayed in the player,
**When** its presentation is rendered,
**Then** the title, player artwork and duration match that lullaby,
**And** list artwork is not substituted for player artwork and the reference animation is preserved.

**Given** a request to start a lullaby,
**When** the backend validates access,
**Then** playback is admitted only if authorized,
**And** a locked lullaby does not play without permission; the real paywall remains pending until Epic 6.

**Given** media preparation is pending or fails,
**When** the player handles that state,
**Then** loading is visible and a recoverable failure offers retry.

**Given** repeated taps or a new lullaby selection while an earlier request is pending,
**When** requests complete,
**Then** unrelated audio does not overlap,
**And** stale results cannot start the wrong lullaby.

**Given** playback has started,
**When** the listener uses pause/resume or exits the player,
**Then** pause/resume operates correctly and exit follows the characterized reference behavior.

### Story 2.3: Start meditation or audio-story playback from detail

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want to inspect meditation or audio-story information and explicitly start listening,
So that I can choose content before audio begins.

**Requirements:** FR-8, FR-11, FR-14–15, FR-25; NFR-1, NFR-5–7; UX-DR12, UX-DR16, UX-DR22–25; AD-3, AD-7–10.

**Prerequisites:** Stories 2.1–2.2 and the existing shared media coordinator; agreed detail, bounded preparation and protected-start contracts with real media outputs. Preparation must respect server authorization; metadata visibility is not permission to fetch protected full media. Production purchase/paywall integration remains Epic 6 scope. Verify the applicable EXT-CATALOG/EXT-MEDIA closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a meditation or audio-story card,
**When** the listener selects it,
**Then** the correct content and language detail opens,
**And** its cover, age, duration, description and contributor information match the selected experience.

**Given** a detail screen has opened,
**When** bounded media preparation occurs,
**Then** no audio starts automatically,
**And** the entire audio file need not be downloaded before listening can start.

**Given** an eligible detail and explicit Listen action,
**When** the backend admits playback,
**Then** the shared player starts the selected experience,
**And** compatible already-prepared media is reused rather than fetched unnecessarily again.

**Given** a STORY with a listening experience,
**When** the listener starts it from audio-story detail,
**Then** the listening experience opens rather than the illustrated reader.

**Given** loading or recoverable failure during detail/start,
**When** the UI presents that state and the listener retries,
**Then** loading/error outcomes are clear,
**And** retry cannot create duplicate playback.

**Given** the listener has left detail while preparation is pending,
**When** a delayed preparation result arrives,
**Then** it cannot start audio automatically.

**Given** matching content, language and phone/tablet reference conditions,
**When** detail and player appearance are compared,
**Then** both preserve the in-scope reference layout and visual behavior.

**Given** a listed meditation or audio-story experience is withdrawn or no longer available before detail or start resolves,
**When** the backend reports that outcome,
**Then** the UI distinguishes unavailability from a transient network failure and offers a valid return to discovery,
**And** playback is not started and an unavailable experience is not trapped in a success-looking or endless retry flow.


### Story 2.4: Control listening and handle track endings

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want to pause, resume and seek within audio,
So that I can control listening with the expected behavior when a track ends.

**Requirements:** FR-14; NFR-1, NFR-6–7; UX-DR16, UX-DR24–25; AD-3.

**Prerequisites:** Stories 2.2–2.3 and the shared media coordinator; characterized reference seeking and player-exit behavior under OQ-1. Player route disposal alone must not determine session lifetime. System/background controls are subsequent scope.

**Acceptance Criteria:**

**Given** an active listening session,
**When** play/pause controls and timing information are displayed or used,
**Then** they match actual playback state, elapsed time and total duration.

**Given** a seekable track,
**When** the listener selects a position, including rapid repeated seeks,
**Then** playback resolves to the selected valid position,
**And** no stale position or duplicate audio wins over the latest command.

**Given** the requested position is not ready,
**When** seeking requires preparation,
**Then** loading is shown,
**And** preparation failure produces a recoverable error.

**Given** a lullaby, meditation or audio story reaches its end,
**When** the coordinator processes the end event,
**Then** a lullaby loops,
**And** meditation and audio stories do not automatically restart.

**Given** control actions occur around the loop boundary or track end,
**When** callbacks and commands are processed,
**Then** they do not start two competing playbacks.

**Given** the listener exits the player screen,
**When** navigation completes,
**Then** playback follows the verified reference exit behavior,
**And** stopping or continuing is not inferred solely from screen disposal.

### Story 2.5: Listen in the background and use system media controls

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want playback to continue while the screen is locked or I use another application,
So that I can keep listening and control the same session from the operating system.

**Requirements:** FR-14, FR-16 (lifecycle); NFR-1, NFR-6, NFR-8; UX-DR16, UX-DR25; AD-3.

**Prerequisites:** Stories 2.2–2.4; existing shared media coordinator; supported platform background/system-control configuration and real-device test matrix. Keep platform-specific control availability explicit. This story does not implement production auth/migration.

**Acceptance Criteria:**

**Given** an active listening session,
**When** the app moves to the background or the screen locks,
**Then** playing content continues,
**And** paused content does not start automatically.

**Given** a listening session exposed to system media controls,
**When** those controls display or operate on it,
**Then** they show the correct content information and playback state,
**And** supported commands control the same session as the application player.

**Given** playback has continued while the app was in the background,
**When** the listener returns to the application,
**Then** the player reflects the current position and state,
**And** returning does not create another playback instance.

**Given** a call or headset removal interrupts listening,
**When** the interruption occurs,
**Then** playback pauses,
**And** playback resumes only after an explicit user action.

**Given** the application has been fully closed,
**When** it is launched again,
**Then** home opens without automatically playing the previous content,
**And** production session/migration routing remains subject to Epic 3 integration.

**Given** supported physical Android and iOS devices,
**When** background, lock-screen, system controls, interruption and cold-relaunch scenarios are exercised,
**Then** the preceding outcomes are verified on each platform,
**And** emulator-only evidence is not substituted for physical-device acceptance.

### Story 2.6: Switch safely between reading and listening sessions

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader or listener,
I want only my selected content to play when I start a different experience,
So that changing stories or listening content does not produce competing playback.

**Requirements:** FR-13–14, FR-16 (session transition correctness); NFR-1, NFR-4, NFR-6; UX-DR13, UX-DR16, UX-DR25; AD-3.

**Prerequisites:** Epic 1 reader and Stories 2.2–2.5; the single application-scoped media coordinator. Characterize under OQ-1 whether the previous session continues or stops when the new start fails. This approval does not select that unresolved behavior. Account switching remains Epic 3 scope.

**Acceptance Criteria:**

**Given** an existing media session,
**When** newly selected content begins playback,
**Then** the previous unrelated session audio does not overlap it.

**Given** reader-to-lullaby, lullaby-to-meditation or audio-story-to-reader transitions,
**When** the user starts the destination experience,
**Then** the shared media coordinator manages the transition rather than independent competing players.

**Given** rapid content selection and out-of-order preparation results,
**When** pending work resolves,
**Then** stale preparation cannot replace the latest selected content.

**Given** a previous session emits a delayed completion callback,
**When** another session is active,
**Then** that callback neither stops the current content nor marks it completed.

**Given** a successful change of active content,
**When** system media controls display or operate on the session,
**Then** they show and control the new active content.

**Given** a new content start fails,
**When** the error is handled,
**Then** wrong content does not start and success is not falsely shown,
**And** the previous session follows the verified reference continue/stop behavior.

### Story 2.7: Reach listening experiences from search and complete lists

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want content found in search or a complete category list to open the correct listening flow,
So that I can reach the intended experience from every supported discovery entry point.

**Requirements:** FR-6–11; NFR-4–7; UX-DR4, UX-DR11, UX-DR15–16, UX-DR18, UX-DR22–25.

**Prerequisites:** Stories 1.8–1.10 and 2.1–2.6; agreed OQ-4 listening-list/search identity, pagination and error contracts. This story closes the listening-destination integration left explicit in Story 1.9. Full paid purchase/paywall integration remains Epic 6 scope.

**Acceptance Criteria:**

**Given** a listening section in Sleep,
**When** the listener uses its all-items action and loads subsequent pages,
**Then** the correct content group's complete eligible list opens,
**And** pagination neither skips nor duplicates items.

**Given** some items have loaded and the next page fails,
**When** the failure is handled,
**Then** the existing list remains available,
**And** the failed page can be retried.

**Given** a search or full-list result,
**When** the listener selects it,
**Then** an authorized lullaby opens its player directly,
**And** meditation and audio-story results open detail before explicit playback start.

**Given** reading and audio-story results for the same underlying story,
**When** either result is selected,
**Then** the selected reading or listening experience opens correctly.

**Given** the user has opened content from search or a complete list,
**When** the user returns,
**Then** the query, selected list and scroll position are preserved.

**Given** language, publication and access eligibility rules,
**When** content is reached through any supported search/list entry point,
**Then** the same rules apply,
**And** locked media cannot be started without authorization through an alternative route.

### Story 2.8: Record listening progress and history accurately

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want my listening progress, duration and completion information to be preserved accurately,
So that my listening history reflects what I actually listened to.

**Requirements:** FR-16; NFR-4–5; AD-3, AD-7–9.

**Prerequisites:** Stories 2.2–2.6; durable backend progress/history contract for the controlled test user, including stable event identity and server deduplication. Characterize meditation/audio-story completion and lullaby-loop history semantics against reference behavior and the backend contract before implementation. User-visible profile history/statistics and production identity/migration integration remain Epic 3 scope. These records are not deferred backend flow analytics from Epic 7. Verify the applicable EXT-HISTORY closure records in the [external dependency register](#external-dependency-register). Resolve durable local pending-record persistence/recovery, stable account/session/event identities, acknowledgement and storage-failure handling before implementation; do not select a storage engine here. Business progress/history records remain distinct from evictable media and bounded flow telemetry.

**Acceptance Criteria:**

**Given** an active listening session,
**When** progress, duration or completion is recorded,
**Then** it is associated with the correct user, content, listening experience and session.

**Given** the player is paused or buffering,
**When** elapsed time is accumulated,
**Then** paused and buffering time is not added to listened duration.

**Given** the listener seeks forward,
**When** the position changes,
**Then** the skipped interval is not counted as listened duration.

**Given** playback continues in the background,
**When** records are persisted, the app returns to foreground or delivery is retried,
**Then** background listening time is included,
**And** the same duration is not counted twice.

**Given** a network failure while a record is being delivered,
**When** delivery fails and is retried,
**Then** retry is safe and does not duplicate the logical record,
**And** failure to record does not stop listening.

**Given** meditation/audio-story completion or a lullaby loop boundary,
**When** history is updated,
**Then** the result follows verified reference and backend contract semantics,
**And** a loop is not assumed to create a new completion without that evidence.

**Given** progress, duration or completion delivery is unacknowledged when the process terminates,
**When** the application relaunches and delivery can resume,
**Then** durably pending records are recovered with their original account, session and stable event identities and retried until the contracted acknowledgement,
**And** no record is lost or counted twice because of process restart; an acknowledgement lost after server success is reconciled idempotently without attributing the record to the next account.

**Given** local pending-record persistence fails,
**When** recording continues or recovery is offered,
**Then** the resolved contract's failure behavior is shown without claiming durable or server-confirmed success,
**And** recording failure does not block content use or silently turn business records into evictable media cache.


### Story 2.9: Recover listening from connectivity and media-access failures

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want to recover from temporary connection failures without losing my listening position,
So that I can continue the same content safely.

**Requirements:** FR-14–16, FR-25; NFR-1, NFR-4–6, NFR-9; AD-3, AD-7–10.

**Prerequisites:** Stories 1.11 and 2.2–2.8; agreed OQ-8 renewal, retry/resume, access-denial and immutable-revision contracts. Reuse the existing protected-media renewal infrastructure. URL lifetime and withdrawal/revocation policy remain contract decisions; this story does not promise playback after authorization is revoked. Apply Story 1.11 interrupted-transfer semantics to listening assets and verify EXT-MEDIA/EXT-RETENTION closure; position recovery alone is insufficient for NFR-9.

**Acceptance Criteria:**

**Given** connectivity fails during listening,
**When** further media data is required,
**Then** the current listening position is preserved,
**And** loading or a recoverable retry state is shown as appropriate.

**Given** a recoverable failure and successful retry,
**When** listening resumes,
**Then** it uses the same content, language and admitted revision,
**And** it does not restart from the beginning unintentionally.

**Given** a media URL has expired,
**When** renewal is requested,
**Then** the backend rechecks applicable access,
**And** permitted renewal addresses the same admitted revision.

**Given** the backend denies access,
**When** the client handles that result,
**Then** cached media cannot bypass the applicable authorization decision.

**Given** the user pauses or selects another content item while recovery is pending,
**When** the old request completes,
**Then** its stale result cannot restart audio against the user's latest action.

**Given** failures and repeated recovery attempts,
**When** playback and progress delivery resolve,
**Then** no duplicate playback occurs,
**And** listening duration is not recorded twice for the same logical activity.

**Given** an interrupted listening-media transfer, including process termination or URL renewal,
**When** recovery runs under the shared OQ-8 contract,
**Then** transfer continuation is verified for the same immutable asset/revision separately from playback-position recovery,
**And** rejected/unsupported continuation or incompatible partial data follows the safe-restart and integrity behavior established in Story 1.11 without unauthorized cache reuse.


### Story 2.10: Listen reliably through long sessions

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a listener,
I want long listening sessions and repeated content changes to remain stable,
So that I can keep listening without crashes, slowdown or competing audio.

**Requirements:** FR-14–16, FR-25 (sustained listening scope); NFR-1–3, NFR-6–9; UX-DR16, UX-DR22–25; AD-3, AD-8–9.

**Prerequisites:** Stories 2.2–2.9; representative audio and animated artwork, supported physical device/network matrix, OQ-2 test durations and memory/latency budgets, and OQ-8 cache/retention parameters. Quantitative acceptance requires agreed thresholds, not invented numbers. This story extends the diagnostics and bounded resource management already introduced by the consuming features and Epic 1.

**Acceptance Criteria:**

**Given** long listening sessions and repeated lullaby loops,
**When** they run for the agreed test duration,
**Then** no crash, freeze or duplicate audio occurs.

**Given** repeated transitions among reading, lullabies, meditations and audio stories,
**When** resource use is measured across the agreed cycles,
**Then** resources no longer needed are released,
**And** no continuous memory growth is observed.

**Given** reference animated player artwork,
**When** performance is optimized and verified,
**Then** its animation is preserved rather than replaced with a static image to reduce resource use.

**Given** a configured cache budget and active playback,
**When** caching and eviction occur,
**Then** the budget is respected,
**And** required active-playback files are not removed while in use.

**Given** agreed device/network conditions including low-memory devices and weak connectivity,
**When** playback startup and interruption recovery are measured,
**Then** the results are recorded and assessed against the agreed latency and memory thresholds.

**Given** representative supported physical Android and iOS devices,
**When** sustained listening verification completes,
**Then** platform-specific results include test durations, memory and latency evidence under OQ-2,
**And** emulator-only results are not used as substitutes for required real-device evidence.

### Epic 2 planning review

Ten stories have been individually approved for planning. No implementation, executed tests or release readiness is claimed. All numbered story dependencies refer to earlier stories; external contract/evidence gates remain explicit.

| Requirement | Story coverage and remaining integration |
| --- | --- |
| FR-6 | 2.1 listening sections and 2.7 complete-list pagination/destinations. |
| FR-7 | 2.7 integrates the listening destinations deferred from 1.9; actual OQ-4 contract verification remains required. |
| FR-8 | 2.1 independent audio-story availability, 2.3 listening experience selection and 2.7 correct reading/listening destinations. |
| FR-9 | 2.1/2.7 consume shared selected-language eligibility; 2.9 preserves admitted language during renewal. TR/EN/PT and remote test/live policy from 1.10 apply; full preference persistence is Epic 3 and authorized tester verification is Epic 5. |
| FR-10 | 2.1 listing locks and 2.2/2.3/2.7 protected start. Full purchase/paywall and subscription integration remains Epic 6. |
| FR-11 | 2.2 direct lullaby start and 2.3 explicit meditation/audio-story start from detail. |
| FR-14 | 2.2–2.6 entry, controls, looping, lifecycle, system commands and exclusive session changes; 2.9 recovery and 2.10 sustained verification. |
| FR-15 | 2.2 distinct animated artwork, 2.3 bounded detail preparation/reuse, 2.10 resource checks. |
| FR-16 | 2.5 cold-relaunch/background behavior, 2.6 stale-transition rejection, 2.8 durable progress/duration/completion and 2.9 retry correctness. Production history/profile statistics remain Epic 3. |
| FR-25 | 2.2/2.3 authorized start, 2.7 consistent entry-point enforcement and 2.9 revision-preserving access renewal; candidate/live integration remains Epic 5 and paid access Epic 6. |

Before implementation, resolve relevant OQ-1 reference exit/control/failed-takeover and loop-history behavior, OQ-4 discovery/search contracts, and OQ-8 admission/preparation/cache/renewal contracts. OQ-2 durations and quantitative budgets must close before reliability acceptance. AD-5 continues to gate auth/migration feature development in Epic 3; it does not block planning Epic 3 or invalidate the approved controlled-user media-first order.


## Epic 3: Keep my account and data when upgrading

New users can enter through registered or anonymous identity flows, while existing users can update in place and retain their account, profile, preferences, history, progress and access rights. Production identity/session routing and account-switch isolation are integrated with the media/discovery flows already planned in Epics 1–2.

**Scope:** FR-1–5, FR-9, FR-16, FR-26–29; NFR-1, NFR-4–8; UX-DR5–9, UX-DR17–19, UX-DR22–25; AD-4–8, AD-11. Stories 3.1–3.21 are approved for planning. The user approved proceeding to Epic 4 after the Epic 3 planning review; AD-5 and unresolved contract gates remain open.

**Planning constraints:** AD-5 is an evidence prerequisite before auth/migration feature development. Plan platform-specific signed-upgrade checks first, then admit identity/migration implementation only when both platforms satisfy that gate. The existing iOS anonymous-preference Simulator proof is partial evidence, not registered-session, populated-history or signed-device proof. Keep branch codex/ios-legacy-upgrade-probe at reported commit a807a9f as unmerged evidence unless separately verified; this planning task neither merges it nor modifies the probe. Resolve OQ-3/6 before migration acceptance criteria; unresolved local-record/removed-content/deletion semantics must not be silently selected. User approval to plan is not an assertion that these gates passed.

### Story 3.1: Prove a safe signed Android in-place update

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an existing Android user,
I want the replacement to install as an update without losing my session or local records,
So that I can continue using my account and retained data.

**Requirements:** FR-29, FR-26–28 (input/session preservation evidence only); NFR-4–6; AD-5.

**Prerequisites:** Approved test-device/account fixtures and verified live application ID/signing lineage compatible with the representative installed legacy build. This is an upgrade evidence task, not auth/migration feature implementation. Both Android and iOS signed-upgrade gates must pass before that development starts. Planning approval neither executes an installation nor marks AD-5 passed.

**Acceptance Criteria:**

**Given** a representative legacy installation and a replacement build with the verified live application identity and compatible approved signing lineage,
**When** the signed update is installed,
**Then** it updates the existing application without uninstalling it or clearing application data.

**Given** a registered test user signed into Firebase in the legacy app,
**When** the replacement launches after the update,
**Then** the retained session resolves to the same Firebase UID,
**And** successful authentication is demonstrated without merely trusting a stored logged-in flag.

**Given** an anonymous legacy test user with local preferences and populated records within the preservation scope,
**When** the replacement reads local state after the update,
**Then** those records remain readable,
**And** representative field values are compared rather than relying on counts alone.

**Given** the initial post-update observations,
**When** the replacement is closed and reopened,
**Then** the retained values remain unchanged across the observed launches.

**Given** this evidence collection,
**When** the upgrade and read checks run,
**Then** no account migration or legacy-data cleanup is performed.

**Given** completed checks on a physical Android device,
**When** the evidence report is written,
**Then** it records device, old/new versions, signing compatibility and outcomes,
**And** it contains no tokens or personal data.

### Story 3.2: Prove a safe signed iOS in-place update

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an existing iOS user,
I want the replacement to update my installed application while retaining its session and local records,
So that adopting the replacement does not require starting again.

**Requirements:** FR-29, FR-26–28 (input/session preservation evidence only); NFR-4–6; AD-5.

**Prerequisites:** Approved physical test device and registered/anonymous test fixtures; verified live bundle ID and compatible signing/provisioning and distribution path. Reuse the separate Simulator report only within its stated limits. This is evidence collection, not auth/migration implementation; AD-5 passes only after both platform evidence tasks meet its requirements. Story approval does not mark those checks passed.

**Acceptance Criteria:**

**Given** a representative legacy installation and signed replacement with the verified live bundle ID and compatible provisioning,
**When** the replacement is installed as an update,
**Then** the installation succeeds without uninstalling the legacy app or clearing its data.

**Given** a registered Firebase test user on a physical iOS device,
**When** the replacement launches after the update,
**Then** the retained session resolves to the same UID,
**And** valid authentication is demonstrated rather than inferred from a local session flag.

**Given** anonymous preferences and populated retained local records,
**When** the signed update launches and is then closed and reopened,
**Then** those records remain readable,
**And** representative field values match across the observed launches.

**Given** upgrade evidence is being collected,
**When** the checks run,
**Then** no migration or legacy-data cleanup occurs.

**Given** the checks and report,
**When** evidence is assessed,
**Then** the report identifies device, old/new versions, distribution path and outcomes without credentials or personal data,
**And** a development installation is not represented as proof of an App Store update.

**Given** the existing anonymous-preference Simulator report,
**When** it is reused,
**Then** it remains partial evidence,
**And** it is not substituted for registered Firebase session continuity or a signed physical-device upgrade.

### Story 3.3: Start anonymously as a new user

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a new user,
I want to continue without registration on first install,
So that I can begin using the application anonymously.

**Requirements:** FR-1–3, FR-10 (equal eligibility); NFR-4–7; UX-DR5–6, UX-DR22–25; AD-4–7.

**Prerequisites:** AD-5 passed with both platform upgrade evidence tasks; agreed verified-UID/backend account resolution, idempotency and session-state contracts before implementing the affected stories. This story covers a genuinely new installation without retained legacy records. Detection must not misclassify an existing legacy installation as new or discard its inputs. Legacy anonymous migration and anonymous-to-registered conversion remain separate stories. Verify the applicable EXT-ACCOUNT closure records in the [external dependency register](#external-dependency-register). This story owns the initial three-page onboarding and account-choice entry. Verify per-page Skip/final Start, interrupted-onboarding/relaunch and presentation behavior against SCREENS 01–04; alternate membership onboarding is delivered in Story 3.16.

**Acceptance Criteria:**

**Given** a new installation without legacy migration inputs,
**When** initial onboarding completes,
**Then** registration/sign-in and Continue without an account choices are offered.

**Given** the user selects anonymous continuation,
**When** identity and account resolution succeed,
**Then** Firebase anonymous identity is established and mapped to the correct backend application account.

**Given** account preparation is unresolved or fails,
**When** the session state is evaluated,
**Then** normal catalog admission remains blocked until preparation succeeds,
**And** recoverable failure offers retry.

**Given** repeated taps or retries after partial success,
**When** account resolution runs again,
**Then** multiple application accounts are not created for the same logical anonymous start.

**Given** a successfully established anonymous session,
**When** the app is closed and reopened,
**Then** the same anonymous account is used,
**And** onboarding does not repeat unnecessarily.

**Given** anonymous and registered users with equivalent language and access rights,
**When** content eligibility is evaluated,
**Then** the same content rules apply,
**And** anonymous identity alone does not grant premium access.

**Given** a genuinely new installation eligible for introduction,
**When** onboarding is displayed and traversed,
**Then** the three reference pages, artwork, indicators and localized title/body presentation appear in the verified order,
**And** each page's Skip and final Start produce the verified reference result and account-choice sheet without implicitly authenticating the user.

**Given** onboarding is interrupted or the application is reopened before or after account choice,
**When** authoritative startup eligibility is resolved,
**Then** continuation/repetition follows the verified reference and persisted state,
**And** existing valid sessions or retained legacy records are not routed as a fresh installation or reset. Initial introduction and alternate membership onboarding remain distinct surfaces.


### Story 3.4: Create a new account with email and password

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a new user,
I want to create an account using my email and password,
So that I can establish a registered identity in the application.

**Requirements:** FR-2; NFR-4–7; UX-DR7, UX-DR22–25; AD-4, AD-6–7.

**Prerequisites:** AD-5 passed; Story 3.3 identity/backend-resolution boundaries; verified reference field/consent validation and agreed Firebase/backend account contracts. This story accepts account creation and verification-message dispatch, not completed profile setup or action-link handling. Those capabilities follow in later stories. Anonymous-to-registered data preservation is separate scope; no existing anonymous identity/data may be silently replaced by this fresh-registration path.

**Acceptance Criteria:**

**Given** a new user opens registration,
**When** the email/password form is displayed and used,
**Then** reference appearance, password visibility and field validation are preserved.

**Given** required consent is absent or marketing consent is not selected,
**When** registration is submitted,
**Then** missing required consent prevents completion,
**And** marketing consent remains separate and optional.

**Given** successful Firebase registration,
**When** backend account resolution completes,
**Then** the verified Firebase UID maps to the correct application account.

**Given** the submitted email is already registered,
**When** the provider rejects new registration,
**Then** an understandable error is presented,
**And** accounts are not automatically merged.

**Given** Firebase registration succeeded but backend preparation failed,
**When** the user retries,
**Then** preparation continues with the established identity,
**And** a second account is not created.

**Given** verification email dispatch succeeds or fails,
**When** the result is displayed,
**Then** that dispatch outcome is represented accurately,
**And** a dispatch failure does not restart account creation.

**Given** invalid form data or a network failure,
**When** registration has not completed,
**Then** the user is not routed as though registration succeeded.

### Story 3.5: Sign in with email and password

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a registered user,
I want to sign into my existing account,
So that I can access my own application data safely.

**Requirements:** FR-1–3, FR-5 (account isolation), FR-26–27 (admission gate only); NFR-4–7; UX-DR7, UX-DR22–25; AD-4, AD-6–8.

**Prerequisites:** AD-5 passed; Stories 3.3–3.4 identity resolution and verified Firebase/backend session contracts. Implement persisted account/migration status admission here; do not implement or assume completed migration for legacy accounts. Migration-pending users must remain gated. The sign-in success path is independently verifiable with an existing ready backend account; data import is subsequent scope. Integrate production session admission with the shell introduced in Stories 1.1/2.1, including invalidation of every retained tab stack on account-context change.

**Acceptance Criteria:**

**Given** the email sign-in form,
**When** it is rendered and used,
**Then** form presentation, password visibility and field errors match the reference behavior.

**Given** successful Firebase authentication,
**When** backend account resolution runs,
**Then** the verified UID resolves to the correct application account.

**Given** a Firebase session with unresolved or incomplete backend account/migration state,
**When** navigation evaluates normal catalog admission,
**Then** Firebase sign-in success alone cannot admit normal use,
**And** only an eligible verified backend account/migration state permits it.

**Given** invalid credentials or network failure,
**When** sign-in fails,
**Then** an understandable error and appropriate retry path are provided.

**Given** an anonymous user signs into an existing registered account,
**When** resolution succeeds,
**Then** that account's data is used,
**And** anonymous data is not automatically merged into it.

**Given** an account transition,
**When** the new account becomes ready,
**Then** previous-account private data is no longer visible,
**And** old protected playback is stopped and stale responses cannot affect the new account; unacknowledged migration inputs must not be deleted as ordinary cache cleanup.

**Given** a valid established session,
**When** the app is reopened,
**Then** that session is retained and resolved,
**And** the user is not unnecessarily sent back to sign-in.

**Given** retained tab routes and state from a previous account,
**When** sign-out or an account switch changes authoritative session admission,
**Then** every old account-bound tab stack and pending destination is invalidated before the next account is ready,
**And** tab selection or system back cannot restore an old user's private screen.


### Story 3.6: Sign in or register with Google

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to sign in or create an account using Google,
So that I can access the application through that identity provider.

**Requirements:** FR-2; FR-1, FR-3 (existing-account separation); NFR-4–7; UX-DR6–7, UX-DR22–25; AD-4, AD-6–7.

**Prerequisites:** AD-5 passed; Stories 3.3–3.5 identity/backend resolution and session admission; verified platform/provider configuration and error contracts. Anonymous conversion with data preservation is separate scope; do not silently replace an anonymous identity through this registration path.

**Acceptance Criteria:**

**Given** a reference Google entry point,
**When** the user selects Google,
**Then** the provider account-selection flow opens.

**Given** the user cancels the provider flow,
**When** cancellation is returned,
**Then** it is not treated as successful sign-in,
**And** the existing session and data remain unchanged.

**Given** successful provider authentication,
**When** Firebase identity and backend resolution complete,
**Then** the verified Firebase UID maps to the correct application account.

**Given** the same email is associated with another sign-in method,
**When** an account conflict is encountered,
**Then** accounts are not silently merged,
**And** understandable guidance is presented.

**Given** backend preparation or migration state is unresolved or incomplete,
**When** normal-use admission is evaluated,
**Then** access remains gated until the required verified state permits it.

**Given** provider/backend failure or a repeated attempt,
**When** the flow resolves,
**Then** no second application account or duplicate navigation is created.

**Given** an existing Google-linked application account,
**When** the user signs in again,
**Then** the same account and its data are used.

### Story 3.7: Sign in or register with Apple

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to sign in or create an account using Apple,
So that I can access the application through that identity provider.

**Requirements:** FR-2; FR-1, FR-3 (existing-account separation); NFR-4–7; UX-DR6–7, UX-DR22–25; AD-4, AD-6–7.

**Prerequisites:** AD-5 passed; Stories 3.3–3.5 identity/backend resolution and session admission; verified Apple/Firebase platform configuration and reference entry-point coverage. Provider-specific field/error behavior must be verified before implementation. Anonymous-to-registered data preservation remains a separate story; this flow must not silently replace an anonymous identity.

**Acceptance Criteria:**

**Given** a platform and entry point where the reference supports Apple sign-in,
**When** sign-in methods are presented,
**Then** the Apple option is available there.

**Given** provider cancellation or failure,
**When** that result is returned,
**Then** it is not treated as successful sign-in,
**And** the existing session and data remain intact.

**Given** successful Apple authentication,
**When** Firebase identity and backend resolution complete,
**Then** the verified Firebase UID maps to the correct application account.

**Given** the user chooses to hide their email,
**When** identity is resolved,
**Then** account matching remains correct,
**And** accounts are not merged based on email similarity.

**Given** a subsequent sign-in omits name or email fields,
**When** the account/profile is resolved,
**Then** saved profile information is not overwritten with empty provider values.

**Given** backend preparation or migration state is unresolved or incomplete,
**When** normal-use admission is evaluated,
**Then** normal use remains gated until the required verified state permits it.

**Given** returning sign-in or retry after partial success,
**When** the flow resolves,
**Then** the same application account is used without creating a duplicate.

### Story 3.8: Verify an email address

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an email-account user,
I want to use my verification link and see my verified status in the application,
So that the application accurately reflects my email verification.

**Requirements:** FR-2; NFR-4–7; UX-DR8, UX-DR17, UX-DR24–25; AD-6–7.

**Prerequisites:** AD-5 passed; Stories 3.4–3.5 account creation/sign-in; verified action-link handling, provider result and session-refresh contracts. The reference reminder surface is accepted here; full profile setup/editing and statistics remain later scope. Verification is not a substitute for backend account/migration admission.

**Acceptance Criteria:**

**Given** an email account is unverified,
**When** its verification reminder is shown,
**Then** the reference reminder and resend action are available.

**Given** a resend request,
**When** dispatch succeeds or fails,
**Then** the outcome is shown accurately,
**And** no new account is created.

**Given** a valid verification link,
**When** it is opened and verification succeeds,
**Then** the application refreshes and reflects the verified state.

**Given** an expired, invalid or previously used verification link,
**When** it is opened,
**Then** an understandable result is shown,
**And** success is not falsely claimed.

**Given** a link belongs to another account,
**When** it is processed while a different account is active,
**Then** the active user is not incorrectly shown as verified,
**And** the link does not silently switch accounts.

**Given** reference content eligibility for an unverified email account,
**When** verification status is evaluated,
**Then** the reminder does not introduce a new content-access barrier absent from the reference,
**And** existing backend account/migration admission checks remain enforced.

### Story 3.9: Reset a forgotten password

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an email-account user,
I want to set a new password after forgetting my old one,
So that I can regain access to the same account.

**Requirements:** FR-2; NFR-4–7; UX-DR8, UX-DR24–25; AD-6–7.

**Prerequisites:** AD-5 passed; Story 3.5 email sign-in and Story 3.8 action-link routing boundaries; verified password-reset provider contract and reference field/password rules. Reset-request messaging must reflect the provider contract without falsely asserting delivery or account existence. Existing account/migration admission remains enforced after subsequent sign-in.

**Acceptance Criteria:**

**Given** the user selects Forgot password from sign-in,
**When** the reset form opens and is submitted,
**Then** empty or invalid email input is validated.

**Given** a valid reset request,
**When** the provider request resolves,
**Then** its result is accurately represented,
**And** network failure offers an appropriate retry path.

**Given** a valid reset link,
**When** the user opens it and enters a new password,
**Then** the password-entry screen is shown,
**And** reset cannot complete before the required password rules are satisfied.

**Given** an expired, invalid or already-used reset link,
**When** it is opened,
**Then** an understandable outcome and a path to request a new link are offered.

**Given** a successful password reset,
**When** the user signs in using the new password,
**Then** the same account is accessed,
**And** profile, history and access rights are not reset.

**Given** a reset link is opened,
**When** the action is handled,
**Then** link opening alone does not establish a session or switch the active user to another account.

**Given** failure or repeated submission,
**When** the reset flow resolves,
**Then** it does not create duplicate accounts or falsely report success.

### Story 3.10: Set up a new user profile

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a new user,
I want to enter profile information step by step or skip optional steps,
So that I can start using the application without providing optional profile data.

**Requirements:** FR-1, FR-4; NFR-4–7; UX-DR9, UX-DR22–25; AD-4, AD-6–8.

**Prerequisites:** AD-5 passed; established identity/account resolution from earlier Epic 3 stories; verified profile field, persistence and setup-eligibility contracts. One account has one application profile. Existing/migrated profile detection is tested with persisted backend states, not by depending on a future migration implementation. Do not infer new editable fields from historical profile schemas.

**Acceptance Criteria:**

**Given** a new user eligible for profile setup,
**When** setup is presented,
**Then** purpose, age range, favorite genres, avatar and name follow the reference order.

**Given** any optional setup step,
**When** the user skips it,
**Then** skipping is allowed,
**And** missing optional information does not prevent normal use once account admission conditions are satisfied.

**Given** entered setup selections,
**When** the user moves backward and forward,
**Then** the entered selections are retained,
**And** selection-dependent button states match the reference.

**Given** successful profile persistence,
**When** the application is reopened,
**Then** the saved fields remain associated with the correct backend account's single profile.

**Given** a profile save fails,
**When** the result is presented and retried,
**Then** failure is not presented as success,
**And** retry does not create a second profile.

**Given** an existing or migrated profile,
**When** setup eligibility is evaluated,
**Then** the user is not unnecessarily routed into new-profile setup,
**And** existing values are not overwritten with empty defaults.

**Given** supported phone/tablet sizes and long localized text,
**When** the setup flow is displayed and used,
**Then** layout and interaction remain consistent with the in-scope reference.

### Story 3.11: Edit the profile name and avatar

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to change my profile name and avatar,
So that my profile reflects my current choices.

**Requirements:** FR-4–5; NFR-4–7; UX-DR17, UX-DR22–25; AD-2, AD-7–8.

**Prerequisites:** AD-5 passed; Story 3.10 and a verified backend profile update contract. Characterize the actual available avatar options; the captured reference edit screen did not show all avatar choices. Historical profile fields remain preserved without being assumed to be editable on this screen.

**Acceptance Criteria:**

**Given** an existing profile,
**When** its edit screen opens,
**Then** the current name and selected avatar are shown,
**And** available avatar choices are presented according to verified reference behavior.

**Given** edited fields,
**When** the user selects an avatar, changes the name or submits invalid input,
**Then** selection, validation and save controls follow the reference behavior.

**Given** a successful save,
**When** the update completes,
**Then** the existing profile for the correct account is updated without creating another profile,
**And** the new name/avatar appear on the profile screen and survive reopening the app.

**Given** a failed save,
**When** the error is shown,
**Then** success is not claimed,
**And** entered values remain available for retry.

**Given** a partial profile edit,
**When** the backend applies it,
**Then** only intended fields change,
**And** other profile attributes, history and preferences remain intact.

**Given** an account switch while an edit request is pending,
**When** the old request completes,
**Then** its response cannot update the new account's profile screen.

### Story 3.12: View my reading and listening history and profile statistics

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to view my completed stories, reading/listening history and accumulated time,
So that I can see my own usage accurately.

**Requirements:** FR-16, FR-4–5; NFR-4–7; UX-DR17, UX-DR22–25; AD-6–8.

**Prerequisites:** AD-5 passed; Stories 3.5 and 3.10–3.11; verified backend history/statistics contracts and existing populated account fixtures. Completion and duration producers from Epics 1–2 are inputs. Historical records from migration are verified in later migration stories. Resolve OQ-3 before defining how removed-content history appears; this story must not infer deletion or a playable destination for removed experiences. Own completion of the Library/Sleep/Profile shell integration using the existing router/session/media ownership and verified tab/back reference behavior.

**Acceptance Criteria:**

**Given** a ready account with completed stories and recorded listening/reading time,
**When** its profile opens,
**Then** completion counts and accumulated time agree with backend records.

**Given** an eligible history entry,
**When** the user selects it,
**Then** the correct content and experience opens.

**Given** an empty history, loading state or network failure,
**When** the profile renders,
**Then** those outcomes are distinguished,
**And** recoverable failure offers retry.

**Given** a change of account,
**When** the next account becomes ready,
**Then** the previous account's history and statistics are not visible.

**Given** completion or duration events delivered more than once with the same stable identity,
**When** statistics are calculated,
**Then** they are not counted twice.

**Given** imported history or history referring to removed content,
**When** it is displayed,
**Then** imported entries are verified with the migration stories and removed-content presentation follows the resolved OQ-3 decision,
**And** the absence of a currently playable feature does not authorize deleting historical records.

**Given** the ready account's Library, Sleep and Profile tabs,
**When** the user navigates within each tab, switches tabs and uses system back,
**Then** each tab retains its own valid stack and scroll context under the verified reference behavior,
**And** account changes invalidate old tab contexts under Story 3.5 rather than restoring previous-user data.


### Story 3.13: Persist my language preference on my account

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want my chosen language to be retained for my account,
So that I see the same language after relaunch or sign-in on another device.

**Requirements:** FR-5, FR-9; NFR-4–7; UX-DR18, UX-DR22–25; AD-6–8, AD-10.

**Prerequisites:** AD-5 passed; Story 1.10 language consumption and Story 3.5 account resolution; verified profile preference and remote language-visibility contracts. The 2026-09-27 user decision supersedes older TR/EN/DE visibility text: Turkish, English and Portuguese are usable. New language test/live configuration applies. Migration of an existing stored preference remains separate scope.

**Acceptance Criteria:**

**Given** a ready account and an enabled language choice,
**When** the user selects the language,
**Then** the choice is stored on the correct backend profile,
**And** failed persistence is not presented as a successful saved preference.

**Given** a saved language preference,
**When** the app reopens or the same account signs in on another device,
**Then** that preference is loaded and applied.

**Given** a language change with outstanding requests,
**When** older-language responses arrive,
**Then** they cannot replace current-language results.

**Given** Turkish, English and Portuguese availability and remotely configured future languages,
**When** choices are displayed,
**Then** current languages are usable,
**And** a new language follows its test-only then live audience visibility state.

**Given** a stored language is no longer available to this user,
**When** the preference is resolved,
**Then** an understandable selection state is shown,
**And** a different language is not silently persisted as the user's chosen preference.

**Given** an account change,
**When** the next account becomes ready,
**Then** the previous account's language preference is not applied to it.

### Story 3.14: Migrate a registered account once on its first transition

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an existing registered user,
I want my account data preserved when I first adopt the replacement,
So that my profile, preferences, history and progress remain usable.

**Requirements:** FR-26–27, FR-29; FR-4, FR-9, FR-16 (retained data); NFR-4–6; AD-4–6, AD-11.

**Prerequisites:** AD-5 passed; earlier Epic 3 verified identity and session admission; resolved OQ-3 source/target identity and field mapping, exceptions and removed-content history treatment, plus OQ-6 deletion/tombstone/remigration prevention constraints. This is approved product-level planning, not finalized executable migration acceptance: refine field-level fixtures after those decisions close. Backend owns idempotency and persisted terminal status. Device-only inputs are separate subsequent scope; do not make them disappear through account migration. Verify the applicable EXT-MIGRATION closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a registered legacy user with verified Firebase identity,
**When** first transition is resolved,
**Then** the backend maps the verified UID to the correct legacy and new application account identities.

**Given** an account has not yet completed migration,
**When** its first migration is performed,
**Then** it includes the protected legacy data up to that user's first transition rather than only a release-day snapshot.

**Given** source records and their target mappings,
**When** migration is assessed,
**Then** preserved fields and relationships are compared with their sources,
**And** missing or unmapped records are not silently discarded.

**Given** migration is pending or its durable completion is unverified,
**When** the client resolves account admission,
**Then** it shows the localized migration-in-progress state,
**And** it cannot admit normal use with an empty/reset profile or mark the migration successful prematurely.

**Given** migration was interrupted or failed,
**When** the user retries,
**Then** already processed records are not duplicated,
**And** newer target data is not overwritten by older imported values.

**Given** account migration has completed successfully,
**When** another device later adopts the replacement,
**Then** account migration is not repeated,
**And** devices still on the old version continue using the old system without continuous synchronization of later legacy changes.

**Given** successful migration,
**When** the new account becomes usable,
**Then** the migration itself does not delete legacy source records.

### Story 3.15: Preserve legacy anonymous device records during upgrade

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an existing anonymous user,
I want my locally retained profile, preferences and history to survive the replacement update,
So that I can continue anonymously without losing my data.

**Requirements:** FR-3, FR-27–29; FR-4, FR-9, FR-16 (preservation); NFR-4–6; AD-4–8.

**Prerequisites:** AD-5 passed; verified new anonymous identity/session contracts from Story 3.3; resolved OQ-3 device record inventory, field/identity mapping, stable import operation identity and invalid/unmapped record disposition, with applicable OQ-6 remigration/deletion constraints. Migration acceptance remains conditional on those decisions and field-level evidence. Legacy anonymous flags are not proof of a pre-existing Firebase anonymous UID. This import is distinct from account-wide migration. Verify the applicable EXT-MIGRATION closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** retained legacy anonymous usage data,
**When** the replacement resolves the installation,
**Then** it recognizes legacy anonymous use and allows continuation with Firebase anonymous identity without requiring registration.

**Given** preserved device-only profile, language, history and progress records,
**When** import is performed,
**Then** those records are associated with the correct new anonymous application account.

**Given** an interruption or repeated import attempt,
**When** the operation is retried,
**Then** it does not create additional anonymous accounts or duplicate records.

**Given** an upload has been accepted but durable import completion is not confirmed,
**When** cleanup or normal-use admission is considered,
**Then** accepted upload alone is not treated as success,
**And** required local inputs remain preserved until backend durable confirmation.

**Given** import fails or remains incomplete,
**When** the user sees the result,
**Then** retry is offered,
**And** incomplete transfer is not represented as complete.

**Given** legacy media/cache cleanup,
**When** evictable files are removed,
**Then** protected user records and unacknowledged migration inputs are not removed.

**Given** malformed or unmapped local records,
**When** they are encountered,
**Then** the resolved OQ-3 disposition is applied,
**And** those records are not silently discarded.

### Story 3.16: Convert an anonymous account into a new registered account

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an anonymous user,
I want to register without losing my existing application data,
So that my profile and usage history remain available after registration.

**Requirements:** FR-3, FR-4, FR-9, FR-16; NFR-4–6; AD-4, AD-6–8.

**Prerequisites:** AD-5 passed; Stories 3.3–3.7 identity/provider boundaries and verified backend conversion contract. Resolve credential-linking, stable operation identity, partial-success recovery and durable conversion confirmation before implementation. Do not infer a required UID change or persistence engine. Legacy input handling follows Story 3.15 where applicable; conversion must not discard unacknowledged inputs. Own the alternate membership onboarding for anonymous-to-registered entry (SCREENS 03); verify its entry points, registration/login/Google/email hierarchy and applicable Apple presentation before acceptance. This is distinct from Story 3.3 initial introduction and the Epic 6 premium offering.

**Acceptance Criteria:**

**Given** an anonymous account with profile, language, preferences, history and progress,
**When** the user creates a new registered account through email, Google or Apple,
**Then** that data is preserved through the conversion.

**Given** successful conversion,
**When** the application continues,
**Then** it uses the correct registered account,
**And** no empty replacement profile is created.

**Given** interruption or retry after partial conversion,
**When** conversion resumes,
**Then** accounts and records are not duplicated,
**And** existing data is not lost.

**Given** the provider flow is cancelled,
**When** cancellation is handled,
**Then** the user retains anonymous use.

**Given** supplied credentials belong to an existing account,
**When** the conflict is resolved,
**Then** no automatic account/data merge occurs,
**And** the existing-account sign-in path is explicitly distinguished and uses that existing account's data.

**Given** backend completion is not yet durably confirmed,
**When** the client evaluates conversion success and cleanup,
**Then** success is not prematurely displayed,
**And** required source data is not removed early.

**Given** an anonymous user opens a reference membership entry point,
**When** alternate membership onboarding is displayed and its actions are used,
**Then** its reference illustration/benefits and registration, existing-account login and provider actions remain distinct from the initial three-page introduction,
**And** new-account actions use conversion while existing-account actions use the no-automatic-merge sign-in path; cancelling retains the anonymous account and its data.


### Story 3.17: Upgrade a later device on an already migrated account

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user with multiple devices,
I want a later-upgraded device to use my existing new-system account,
So that migration is not repeated and current data remains intact.

**Requirements:** FR-26–29; FR-4, FR-9, FR-16 (preservation); NFR-4–6; AD-4–6, AD-8, AD-11.

**Prerequisites:** AD-5 passed; Story 3.14 account migration and applicable local-import boundaries from Story 3.15. Resolve OQ-3 later-device local-record disposition, duplicate/conflict precedence and identity mapping before finalizing field-level migration tests. This planning approval does not select a merge policy or authorize continuous synchronization. OQ-6 remigration prevention constraints also apply.

**Acceptance Criteria:**

**Given** the account completed migration on another device,
**When** a later device updates and signs into it,
**Then** account migration is not started again.

**Given** newer profile, preferences and history exist in the new system,
**When** the older device joins,
**Then** its older values do not overwrite newer new-system data.

**Given** legacy-server changes made after the first account migration,
**When** another device upgrades,
**Then** those later legacy-server changes are not imported again,
**And** continuous synchronization between the systems is not introduced.

**Given** protected records unique to the later device's local storage,
**When** that installation is examined,
**Then** those records are identified and handled separately from account-wide migration.

**Given** local records require import or conflict resolution,
**When** their disposition is determined,
**Then** the resolved OQ-3 policy is applied,
**And** records are not silently deleted.

**Given** a local-record operation is interrupted,
**When** it is retried,
**Then** it resumes safely without duplicating records.

**Given** other devices have not upgraded,
**When** the later device adopts the replacement,
**Then** those other devices continue using the legacy system.

### Story 3.18: Sign out safely

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to sign out and allow another account to use the device safely,
So that my private data and playback are not exposed to the next user.

**Requirements:** FR-1, FR-5; NFR-4–7; UX-DR19, UX-DR24–25; AD-3, AD-6–8.

**Prerequisites:** AD-5 passed; Story 3.5 session/account isolation and verified reference logout confirmation/navigation. Keep retained migration inputs distinct from evictable private caches.

**Acceptance Criteria:**

**Given** an authenticated user requests sign-out,
**When** the reference confirmation is presented,
**Then** cancelling preserves the session.

**Given** sign-out is confirmed,
**When** it completes,
**Then** protected playback is stopped and the session is closed.

**Given** previous-account profile, history and private cached state,
**When** sign-out invalidates that account context,
**Then** those values are not exposed to a subsequent user,
**And** delayed responses cannot restore them into the active UI.

**Given** completed sign-out,
**When** navigation resolves,
**Then** the user reaches the reference account-choice/sign-in surface,
**And** back navigation cannot reenter the closed session.

**Given** the same user signs in again,
**When** backend resolution succeeds,
**Then** the account's backend data is available,
**And** sign-out itself has not deleted it.

**Given** local migration inputs lack durable import acknowledgement,
**When** sign-out cleanup runs,
**Then** those retained inputs are not deleted as cache.

### Story 3.19: Change my password from account settings

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to set a new password using my current password,
So that I can update my account credentials safely.

**Requirements:** FR-5; NFR-4–7; UX-DR19, UX-DR24–25; AD-6–7.

**Prerequisites:** AD-5 passed; Story 3.5 identity/session resolution; verified reference current/new password validation and provider reauthentication behavior. Characterize availability and behavior for Google/Apple accounts rather than assuming email/password credentials exist. No account conversion or provider consolidation is introduced here.

**Acceptance Criteria:**

**Given** an eligible account opens password settings,
**When** the form is shown and used,
**Then** current/new password fields, visibility controls and validation match the reference.

**Given** an incorrect current password or invalid new password,
**When** the change is submitted,
**Then** it does not complete,
**And** understandable field errors are shown.

**Given** the provider requires recent authentication,
**When** the user changes the password,
**Then** required reauthentication is performed,
**And** password-change success is not claimed before confirmation.

**Given** successful password change,
**When** the user signs in with the new password,
**Then** the same account is accessed with its profile and history intact.

**Given** network failure, repeated taps or an account-context change,
**When** outstanding work resolves,
**Then** success is not falsely shown,
**And** no action is applied to a different account.

**Given** a Google/Apple account,
**When** account settings determine password-change behavior,
**Then** the verified provider capabilities and reference behavior are respected,
**And** an email/password credential is not assumed to exist.

### Story 3.20: Delete my new-system account

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to understand the consequences of deleting my account and complete the action safely,
So that my new-system account and its user data are removed within the agreed scope.

**Requirements:** FR-5; FR-26–28 (remigration prevention); NFR-4–7; UX-DR19, UX-DR24–25; AD-4, AD-6–8.

**Prerequisites:** AD-5 passed; earlier account/session and migration boundaries. OQ-6 must be resolved before implementation: shared Firebase identity treatment, old-system isolation, deletion/remigration prevention, subscription consequences and safe retry semantics. Refine executable deletion/migration acceptance cases from that contract. This story does not assume deleting the shared Firebase identity is compatible with the product decision. Verify the applicable EXT-MIGRATION closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** the user requests account deletion,
**When** confirmation is presented,
**Then** deletion scope and subscription consequences are explained clearly,
**And** user confirmation and required reauthentication precede execution.

**Given** authorized account deletion,
**When** it completes,
**Then** deletion applies only to the new-system account and its associated user data.

**Given** legacy access depends on the shared Firebase identity,
**When** the new-system account is deleted,
**Then** legacy application access and legacy-system records remain unaffected.

**Given** the user later signs in again,
**When** migration eligibility is evaluated,
**Then** deleted data is not automatically restored from the legacy system through remigration.

**Given** confirmed deletion,
**When** client cleanup and navigation complete,
**Then** protected playback stops and the application session closes,
**And** private data from the deleted account is no longer visible on the device.

**Given** failure or interruption during deletion,
**When** the client handles or retries the operation,
**Then** incomplete work is not shown as success,
**And** retry yields a safe result under the agreed contract.

**Given** an associated store subscription,
**When** deletion consequences are presented or processed,
**Then** deleting the application account is not assumed to cancel that subscription automatically.

### Story 3.21: Safely remove obsolete legacy media and caches

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an upgrading user,
I want obsolete legacy downloads removed safely,
So that unused media does not keep occupying device storage.

**Requirements:** FR-28; NFR-4–5, NFR-9; AD-4, AD-8.

**Prerequisites:** AD-5 passed; Stories 3.14–3.17 retained-data/import boundaries; verified legacy storage inventory separating disposable media from protected records and active replacement assets. Cleanup eligibility must be explicit; unknown files are not presumed disposable.

**Acceptance Criteria:**

**Given** obsolete downloaded legacy media and reproducible caches,
**When** cleanup runs,
**Then** only those identified disposable targets are removed.

**Given** profile, preferences, history, progress and anonymous identity records,
**When** cleanup evaluates device storage,
**Then** those records are not deleted.

**Given** local migration inputs without durable transfer confirmation,
**When** cleanup runs,
**Then** required inputs remain preserved.

**Given** a file cannot be classified as safe to delete,
**When** cleanup encounters it,
**Then** it is retained.

**Given** cleanup was interrupted or previously removed some targets,
**When** it runs again,
**Then** it can finish safely,
**And** already absent targets do not cause failure or data loss.

**Given** replacement assets required by active playback, legacy-server records and other devices' data,
**When** legacy device cleanup runs,
**Then** those assets and records remain unaffected.

### Epic 3 planning review

Twenty-one stories have been individually approved for planning; this is not evidence that AD-5 or migration/deletion contracts have passed. Stories 3.1–3.2 are evidence work that precedes all auth/migration feature development. Subsequent stories must not be implemented before both platform gate results are accepted.

| Requirement | Story coverage and remaining integration |
| --- | --- |
| FR-1 | 3.3 initial onboarding/new anonymous start; 3.16 distinct membership onboarding/conversion; 3.5 session restoration/admission and retained-stack isolation; 3.12 full tab-shell integration; 3.10 setup routing; 3.14–3.17 migration gates. UX-DR5–6 reference checks remain mandatory. |
| FR-2 | 3.4–3.9 email/Google/Apple entry, verification and password recovery. |
| FR-3 | 3.3 new anonymous identity, 3.5 existing-account sign-in without merge, 3.15 legacy anonymous import and 3.16 new-account conversion. |
| FR-4 | 3.10–3.11 setup/editing; 3.14–3.17 retention across migration/conversion including both legacy profile generations under resolved field contracts. |
| FR-5 | 3.5 isolation, 3.13 language preference, 3.18 logout, 3.19 password change and 3.20 new-system-only deletion. Notification preference remains Epic 4. |
| FR-9 | 3.13 profile persistence and 3.14–3.17 preserved preferences; shared language/test-live consumption from 1.10 remains applicable. |
| FR-16 | 3.12 profile history/statistics consumes Epic 1–2 records; 3.14–3.17 preserve historical data and relations. |
| FR-26 | 3.14 one-time account transition and 3.17 later-device coexistence. |
| FR-27 | 3.14–3.17 durable completion, retry and no loss/duplication or silent dropping. |
| FR-28 | 3.15/3.17 device-local preservation and 3.21 explicitly bounded cleanup. |
| FR-29 | 3.1/3.2 signed platform upgrade evidence and 3.14–3.17 integration; release-cohort and store delivery verification remains Epic 7. |

Migration stories currently state approved product outcomes. Resolve OQ-3 source/target fields and IDs, malformed/unmapped records, later-device local conflicts and removed-content history presentation before finalizing executable migration acceptance. Resolve OQ-6 shared-Firebase deletion, remigration prevention and subscription consequences before deletion implementation and migration acceptance. The content inventory must include all PRD-preserved data, including meaningful historical feedback/promotion records and their identity relationships; this review does not reduce it to visible profile fields.

Real store purchase/restore and entitlement verification remain Epic 6 scope. Support/notification/deep-link surfaces belong to Epic 4; candidate/live test authorization belongs to Epic 5. Their drafted stories do not imply completed integration.

## Epic 4: Manage support and communication

Users can send feedback, access localized legal/help documents, manage notification preferences, receive announcements and reach valid application destinations through supported links. Reference rating/version and operational configuration behavior remains in scope.

**Scope:** FR-18 and FR-5 notification preference; NFR-1, NFR-4–8 as applicable; UX-DR17, UX-DR20, UX-DR24–25; AD-1, AD-6–9. Stories 4.1–4.6 are approved for planning. The user approved proceeding to Epic 5 after the Epic 4 planning review; contract and runtime evidence gates remain open.

**Planning constraints:** Use the authoritative session/account/migration gates from Epic 3 and content destinations from Epics 1–2. Resolve OQ-9 reference/provider/permission behavior and relevant backend contracts before implementing the affected stories. Preserve user-visible behavior without assuming every old SDK or remote configuration key must be copied. Document and notification links must not bypass admission or protected-content authorization. Actual premium purchase/restore remains Epic 6 scope. Do not infer feedback daily limits from the visible character counter; verify each independently.

### Story 4.1: Send feedback

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to send feedback about the application and see its outcome,
So that I know whether my message was submitted successfully.

**Requirements:** FR-18; NFR-4–7; UX-DR20, UX-DR24–25; AD-6–7, AD-9.

**Prerequisites:** Epic 3 authoritative account/session context; verified feedback validation, daily-limit and idempotent submission contracts. Preserve the observed 0/200 counter; verify daily limits independently rather than deriving them from that counter. Reference form appearance comes from SCREENS 32. Verify the applicable EXT-SUPPORT closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** the feedback entry point,
**When** the form opens,
**Then** the reference explanation, multiline input, 0/200 character counter and send control are preserved.

**Given** empty or over-limit input,
**When** submission is attempted,
**Then** validation prevents invalid submission,
**And** the daily submission limit follows verified legacy behavior and the backend contract.

**Given** a feedback submission,
**When** the result is shown,
**Then** success is displayed only after backend confirmation.

**Given** a network failure,
**When** submission fails,
**Then** the entered message is retained,
**And** retry is available.

**Given** repeated taps or redelivery of the same submission,
**When** the backend processes it,
**Then** no duplicate feedback record is created.

**Given** an account change while a message is pending,
**When** submission work resolves,
**Then** the message is not sent under the wrong user's identity.

**Given** the daily feedback limit is reached,
**When** another submission is attempted,
**Then** an understandable limit message is shown,
**And** the feedback is not represented as submitted.

### Story 4.2: View help and legal documents

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want help and legal links to open the correct document,
So that I can read the information relevant to my application use.

**Requirements:** FR-18; FR-2 (registration document entry); NFR-6–7; UX-DR20, UX-DR24–25.

**Prerequisites:** Reference document-entry inventory, localized document sources and agreed missing-translation fallback contract. Preserve existing registration/profile entry points. Localized document fallback is a separate contract from content playback fallback and must not be inferred from it. Verify the applicable EXT-SUPPORT closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a reference registration, profile or other document entry point,
**When** the user selects it,
**Then** the correct help or legal document opens.

**Given** the selected application language,
**When** a document is loaded,
**Then** the matching localization is used,
**And** missing translation follows the agreed document-language fallback contract.

**Given** the document viewer,
**When** it renders its current state,
**Then** title, back navigation, loading and error states are available.

**Given** connectivity fails or the document cannot be retrieved,
**When** the failure is presented,
**Then** an understandable error and retry path are offered.

**Given** a long document on a supported phone/tablet layout,
**When** it is read,
**Then** its contents remain readable and scrollable.

**Given** the user opened a document from a form or another screen,
**When** the user returns,
**Then** the previous screen and entered form values are preserved.

**Given** registration requires explicit consent,
**When** a linked document is opened or closed,
**Then** the application does not check the required consent on the user's behalf.

### Story 4.3: Manage notification permission and preference

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to choose whether to receive notifications and see the actual current status,
So that notification behavior reflects my choice and device permissions.

**Requirements:** FR-5, FR-18; NFR-4–7; UX-DR17, UX-DR24–25; AD-6–9.

**Prerequisites:** Epic 3 account context; OQ-9 reference permission timing, platform/provider configuration, device registration ownership and preference persistence contracts. Application preference and OS permission are distinct states; do not infer that changing one can override the other. Verify the applicable EXT-SUPPORT closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** the reference permission-request stage is reached,
**When** notification permission is requested,
**Then** the request follows the reference flow,
**And** refusal does not block normal application use.

**Given** an application preference and current OS permission,
**When** the profile notification control is rendered,
**Then** both are reflected accurately in the presented state.

**Given** OS notification permission is disabled,
**When** notification settings are shown or enabled from the app,
**Then** the application does not falsely present notifications as enabled,
**And** it offers the appropriate device-settings route when needed.

**Given** the user changes notification permission in device settings,
**When** the user returns to the app,
**Then** the visible permission state refreshes.

**Given** preference persistence fails,
**When** the result is displayed,
**Then** success is not claimed,
**And** retry is possible.

**Given** an account change,
**When** device notification registration is updated,
**Then** it is not associated with the wrong user.

**Given** Android and iOS permission flows,
**When** this capability is verified,
**Then** each platform is checked separately against the OQ-9 provider and registration contract.

### Story 4.4: Show applicable in-app announcements

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to see announcements relevant to me and reach their intended content,
So that I can act on current application information.

**Requirements:** FR-18; NFR-1, NFR-5–7; UX-DR24–25; AD-6–9.

**Prerequisites:** Earlier content/account navigation and admission gates; OQ-9 verified announcement presentation, targeting, timing, dismissal/repetition and target contracts. App-internal announcement targets must use the authoritative route graph already established; external notification/link intake is the following story. Existing configuration keys and SDKs are not mandatory implementation choices. Verify the applicable EXT-SUPPORT closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** announcement configuration and user language/eligibility,
**When** display conditions are evaluated,
**Then** the announcement appears only when its configured conditions are satisfied.

**Given** an eligible announcement,
**When** it is displayed and dismissed or acted upon,
**Then** artwork, text, action and dismissal follow the reference behavior.

**Given** announcement retrieval or artwork loading fails,
**When** the failure occurs,
**Then** application startup and normal use are not blocked.

**Given** a previously dismissed announcement,
**When** its eligibility is evaluated again,
**Then** redisplay follows verified reference/configured frequency rules.

**Given** an announcement action,
**When** the user selects it,
**Then** the correct valid target opens,
**And** an invalid or unavailable target produces an understandable outcome.

**Given** a target requiring identity, completed migration or protected-content access,
**When** it is reached from an announcement,
**Then** existing admission and authorization rules remain enforced.

**Given** targeting and timing requirements,
**When** announcement behavior is accepted,
**Then** it is validated against the resolved OQ-9 contract.

### Story 4.5: Reach the correct destination from notifications and external links

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want notifications and supported links to take me to their intended application destination,
So that I can reach the relevant experience safely.

**Requirements:** FR-18; FR-1, FR-8, FR-11, FR-25 (routing/access integration); NFR-1, NFR-5–7; UX-DR24–25; AD-6–9.

**Prerequisites:** Epics 1–3 content destinations and authoritative session/migration gates; Story 4.3 notification registration; resolved OQ-9 platform link formats, notification targets and reference start behavior. Use the existing route graph and media coordinator. Retain the logical destination through admission rather than treating a link as authorization. Verify the applicable EXT-SUPPORT closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a supported notification or external link,
**When** the app is foregrounded, backgrounded or cold-started by it,
**Then** the intended destination is resolved correctly.

**Given** sign-in or migration is required,
**When** the target is requested,
**Then** the required admission flow completes first while the logical destination is retained,
**And** access is rechecked before opening it afterward.

**Given** a story, audio-story, meditation or lullaby target,
**When** it is admitted,
**Then** the correct experience opens,
**And** start behavior follows the verified reference for that entry path.

**Given** an invalid, unsupported or missing target,
**When** the link is handled,
**Then** an understandable outcome is shown without crashing the application.

**Given** the same link is delivered repeatedly,
**When** routing resolves,
**Then** duplicate screen stacks or duplicate playback are not created.

**Given** protected content or account-private data,
**When** a link requests it,
**Then** the link cannot bypass authorization or expose another user's private data.

**Given** supported Android and iOS notification/link paths,
**When** integration is verified,
**Then** each platform's formats and destinations match the resolved OQ-9 contract.

### Story 4.6: Rate the application and view its installed version

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to rate the application and see which version I am using,
So that I can provide store feedback and identify my installed release.

**Requirements:** FR-18; NFR-6–7; UX-DR20, UX-DR24–25; AD-9.

**Prerequisites:** Verified reference rating entry points, eligibility/frequency configuration, platform review/store behavior and correct application listing identity. Runtime version information must come from the installed build. OQ-9 provider/configuration verification applies; a review request is not proof of a submitted review. Verify the applicable EXT-SUPPORT closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a reference rating entry point or verified display condition,
**When** the rating call to action is presented,
**Then** it follows those entry and eligibility rules.

**Given** the rating invitation,
**When** the user dismisses it or does not rate,
**Then** normal application use remains available.

**Given** a previously presented invitation,
**When** display eligibility is evaluated again,
**Then** configured repeat-frequency rules are respected rather than prompting on every launch without control.

**Given** a rating action,
**When** the user selects it,
**Then** the correct platform store/review flow is requested for this application.

**Given** the store/review flow cannot open,
**When** that outcome occurs,
**Then** the application remains usable,
**And** the user is not counted as having submitted a rating.

**Given** the profile version field,
**When** it is displayed,
**Then** it matches the actual installed application version.

### Epic 4 planning review

Six stories have been individually approved for planning. FR-18 is covered by feedback (4.1), localized documents (4.2), notification permissions (4.3), announcements (4.4), notification/external-link destinations (4.5), and rating/version surfaces (4.6). FR-5 notification preference is covered by 4.3. UX-DR20 is covered by 4.1/4.2/4.6; notification profile controls from UX-DR17 belong to 4.3, with UX-DR24–25 checks applied to relevant surfaces.

Operational configuration is traced across epics: language test/live visibility in 1.10/3.13 and Epic 5, category/free eligibility in Epics 1–2 with backend/CMS contracts, announcement rules in 4.4, rating rules in 4.6, and paywall trigger configuration in Epic 6. Preserve reference-visible controls while allowing replacement provider/configuration implementations. Their actual contracts remain prerequisites rather than presumed current backend capabilities.

OQ-9 must resolve permission timing, SDK/data scope, provider configuration and link/announcement behavior before implementing the affected stories. Verify feedback daily limits, document fallback, announcement repetition and rating eligibility explicitly. Platform-specific runtime evidence, notification delivery and external-link admission must be checked end to end; planning approval is not runtime acceptance.


## Epic 5: Test content safely before publication

Authorized testers can consume candidate content on devices and identify the exact revision under test while ordinary users remain on eligible live content. Backend/CMS publication, preparation, approval and retention contracts are verified through their effects on the mobile experiences.

**Scope:** FR-22–25; FR-9 new-language test/live visibility; NFR-4–7, NFR-9; AD-7–10. Stories 5.1–5.5 are approved for planning. The user approved proceeding to Epic 6 after the Epic 5 planning review; OQ-8 readiness gates remain open.

**Planning constraints:** Use the production-shaped account/session and reader/listening boundaries from Epics 1–3. Resolve OQ-8 candidate/live authorization matrix, immutable manifest, readiness, approval invalidation, renewal/withdrawal and retention contracts before implementing the affected stories. Tester permission is independent of subscription. Do not duplicate the existing CMS/backend epic plan or introduce a mobile publish/approval UI without an explicit requirement; the mobile contribution is consumption, clear revision identification and device evidence. Ordinary-user candidate isolation must already be enforced in Epics 1–2; this epic adds authorized candidate use and full integration verification. Test-only language visibility follows the user's updated remote configuration decision.

### Story 5.1: Open an authorized candidate revision on device

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an authorized tester,
I want to open candidate content and identify the revision under test,
So that I can verify the intended version on a device.

**Requirements:** FR-23–25; NFR-5–7; AD-7–10.

**Prerequisites:** Epics 1–3 production-shaped media/session foundations; resolved OQ-8 tester/candidate access matrix, candidate entry route, revision identity, readiness and renewal contracts. Do not introduce a mobile publication or approval UI by inference. Existing CMS/backend ownership remains intact. Verify the applicable EXT-PUBLICATION closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** a user requests candidate access,
**When** the backend verifies tester permissions,
**Then** an authorized tester may open permitted candidates including otherwise locked content.

**Given** subscription and tester permission states,
**When** candidate eligibility is evaluated,
**Then** tester authority remains independent of subscription,
**And** subscribing alone does not grant candidate access.

**Given** a candidate detail or player,
**When** it is displayed,
**Then** a clear test indicator and revision information are visible.

**Given** a selected candidate,
**When** it opens,
**Then** content, language and experience match the intended test revision.

**Given** an ordinary user,
**When** candidate access is attempted through a list, direct link or cache,
**Then** the candidate cannot be opened without tester authorization.

**Given** a visible test indicator,
**When** protected media is started or renewed,
**Then** backend authorization is still required; the indicator alone is not permission.

**Given** insufficient authority or unready media,
**When** the candidate is requested,
**Then** an understandable outcome is presented,
**And** the candidate is not falsely treated as playable.

### Story 5.2: Preserve live content while a candidate is prepared

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user,
I want to keep using published content while its next revision is prepared,
So that an unfinished or failed candidate does not interrupt my experience.

**Requirements:** FR-22–25; NFR-4–6; AD-3, AD-8, AD-10.

**Prerequisites:** Story 5.1 and existing reader/listening revision continuity; resolved OQ-8 publication, rollback, retention and active-session policies. Backend/CMS owns publication mutations; this story verifies their mobile effects rather than adding a mobile publishing surface.

**Acceptance Criteria:**

**Given** an existing live revision,
**When** a new candidate is created,
**Then** ordinary users retain access to the eligible live revision.

**Given** candidate media processing fails,
**When** ordinary users request the published experience,
**Then** the existing live revision remains available.

**Given** independently published language/experience scopes,
**When** one candidate fails or changes,
**Then** other eligible live scopes remain unaffected,
**And** an audio-story candidate failure does not block an already published reading experience.

**Given** a new approved revision is published,
**When** a subsequent session starts,
**Then** it is admitted against the correct live revision.

**Given** a session started before that publication,
**When** the active session continues,
**Then** it does not automatically switch to the new revision,
**And** images and audio from different revisions are not mixed.

**Given** a rollback for a specific language/experience scope,
**When** it takes effect,
**Then** the previous live revision is restored for that scope,
**And** unrelated publication scopes are not changed.

### Story 5.3: Verify ready candidate media and invalidate stale approval

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a tester,
I want complete candidate media and revision-specific test evidence,
So that approval cannot silently apply to different or incomplete media.

**Requirements:** FR-22–24; NFR-4–6, NFR-9; AD-8, AD-10.

**Prerequisites:** Stories 5.1–5.2; resolved OQ-8 actual rendition/manifest readiness, integrity, candidate replacement and approval invalidation contracts. Backend/CMS owns processing, publishing and approval mutations; mobile provides revision-addressed device evidence. No new mobile approval/publishing UI is assumed. Verify the applicable EXT-PUBLICATION/EXT-MEDIA closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** required candidate images, audio and manifest,
**When** test readiness is evaluated,
**Then** they must actually exist, be accessible and pass integrity/readiness checks before the candidate is ready for testing.

**Given** missing or corrupt candidate media,
**When** the tester attempts to consume it,
**Then** an understandable failure is shown,
**And** that failure is not recorded as a successful test.

**Given** a content/language/experience publication scope,
**When** candidates are managed,
**Then** at most one candidate is active in that scope.

**Given** a material candidate change,
**When** a new revision is created,
**Then** affected prior tests and approvals do not carry over to the new revision.

**Given** generated media changes,
**When** the candidate is reconsidered for publication,
**Then** media preparation and device testing are repeated for the changed revision.

**Given** metadata-only changes,
**When** a lighter approval path is evaluated,
**Then** it is limited to the fields explicitly permitted by the resolved OQ-8 contract and cannot conceal a media change.

**Given** a request to publish generally,
**When** publication prerequisites are checked,
**Then** required preparation and explicit approval for the applicable revision must be complete.

### Story 5.4: Enable a new language for testing before live use

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a content team member,
I want a new language verified by authorized test users before making it generally available,
So that language rollout can proceed without exposing unfinished experiences.

**Requirements:** FR-9 (latest user decision), FR-22–25; NFR-5–7; AD-7–10.

**Prerequisites:** Stories 1.10, 3.13 and 5.1–5.3; verified remote language visibility/audience and content publication/access contracts. Language audience eligibility and candidate-content permission are separate decisions. Existing Turkish, English and Portuguese availability follows the user's explicit decision.

**Acceptance Criteria:**

**Given** a new language is enabled for testing through remote configuration,
**When** language choices are resolved,
**Then** only the designated test users see it.

**Given** an ordinary user and a test-only language,
**When** that language is requested through selection, search, direct link or fallback,
**Then** the user cannot access it through those paths.

**Given** an eligible test user,
**When** content in the test language is requested,
**Then** the applicable publication and protected-media authorization rules still apply.

**Given** the language is enabled live,
**When** ordinary users resolve language choices,
**Then** the new language becomes visible,
**And** only eligible published content is available to them.

**Given** a language becomes visible to a broader audience,
**When** content eligibility is evaluated,
**Then** candidate revisions are not automatically published by that language change.

**Given** configuration changes while cached data or requests remain outstanding,
**When** those results are handled,
**Then** stale cache or responses cannot restore obsolete language visibility.

**Given** Turkish, English and Portuguese are currently usable,
**When** a new language is enabled,
**Then** their availability remains unaffected.

### Story 5.5: Protect required media revisions from cleanup

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a reader or listener,
I want cleanup to preserve the media needed by my active experience,
So that maintenance does not break reading or listening.

**Requirements:** FR-22, FR-25; NFR-1, NFR-5–6, NFR-9; AD-3, AD-8, AD-10.

**Prerequisites:** Stories 5.1–5.3 and existing revision-addressed playback/renewal; resolved OQ-8 server retention, rollback protection, active-session lifetime, withdrawal/revocation and device-cache policies. Backend owns retention cleanup; mobile integration verifies actual file access and playback. No duration or revocation behavior is invented by this planning approval. Verify the applicable EXT-RETENTION closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** live content and revisions retained for permitted rollback,
**When** server cleanup runs,
**Then** the media required for those uses is preserved.

**Given** an active admitted session,
**When** media cleanup runs,
**Then** its revision remains accessible for the agreed session-protection interval under the applicable access policy.

**Given** a media URL renewal,
**When** access is renewed,
**Then** it addresses the same revision,
**And** another revision's media is not silently substituted for a removed file.

**Given** active device playback,
**When** local media cache eviction runs,
**Then** files currently required by that session are not removed.

**Given** content withdrawal or access revocation,
**When** playback/renewal is evaluated,
**Then** the resolved OQ-8 policy is applied,
**And** cache cannot bypass authorization.

**Given** retention configuration and integration tests,
**When** cleanup behavior is verified,
**Then** retention durations are environment-configured,
**And** evidence checks actual file access and playback rather than only registered metadata.

### Epic 5 planning review

Five stories have been individually approved for planning. Backend/CMS publication and processing responsibilities remain externally owned; mobile stories verify their contract effects and device consumption without adding a publishing UI.

| Requirement | Story coverage |
| --- | --- |
| FR-22 | 5.2 independent language/experience live continuity and rollback; 5.3 one active candidate and invalidated approvals on material changes. |
| FR-23 | 5.1 authorized candidate access and visible revision/test marker; 5.3 explicit revision-specific readiness/test/approval requirements. |
| FR-24 | 5.2 failed-candidate isolation and 5.3 actual prepared media/integrity; CMS processing-error/retry implementation remains in the existing backend/CMS scope and must be verified as a dependency. |
| FR-25 | 5.1 authorization, 5.2 session revision continuity and 5.5 retention/renewal/revocation integration. Paid-access verification remains Epic 6. |
| FR-9 updated decision | 5.4 verifies test-only then live language visibility without implicitly publishing candidate media. |

OQ-8 remains an implementation/readiness prerequisite: candidate entry path and permission matrix, actual manifest/rendition integrity, material versus metadata-only changes, approval invalidation, URL renewal/lifetime, withdrawal/revocation behavior, cache and retention intervals. The existing backend/CMS epics do not by themselves prove these contracts are implemented. Final end-to-end evidence must include ordinary, tester and subscribed users with the applicable independent permission combinations.


## Epic 6: Purchase and recover premium access

Existing subscribers retain verified access; users can purchase, restore and use supported promotions through the reference premium journeys. Backend admission remains authoritative for protected media, with RevenueCat providing the contracted purchase/restore integration.

**Scope:** FR-10, FR-19–20, FR-25; FR-13 completion paywall integration; NFR-1, NFR-4–8; UX-DR21, UX-DR24–25; AD-4, AD-6–10. Stories 6.1–6.7 are approved for planning, including the iOS parental gate. Epic 6 coverage review is recorded below; the user approved proceeding to Epic 7.

**Planning constraints:** This is a late pre-release integration, never a post-release deferral. Verify real RevenueCat/store identity, products/offerings, entitlement refresh, restore and transition behavior before implementation (OQ-7 plus applicable OQ-6/8/9 contracts). Do not treat local purchase flags as protected-media authorization. Email/account resemblance cannot authorize a subscription transfer. Use registered/anonymous account boundaries from Epic 3 and real store test evidence; simulator Billing errors are not successful purchase or offer-layout evidence. Complete locked-content entry points left explicit in Epics 1–2 without changing ordinary-versus-tester permissions from Epic 5.

### Story 6.1: Recognize an existing subscriber's access

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an existing subscriber,
I want my subscription recognized by the replacement,
So that I can continue accessing eligible content after the update.

**Requirements:** FR-10, FR-19, FR-25; NFR-4–6; UX-DR21, UX-DR25; AD-4, AD-7–10.

**Prerequisites:** Epic 3 identity/account isolation; verified RevenueCat/store-to-application identity and entitlement contracts, backend access authority, refresh/error behavior and representative Android/iOS store fixtures. Resolve applicable OQ-7/8 before implementation. Existing subscription evidence must not be inferred from a local premium flag or simulator Billing failure. Verify the applicable EXT-PURCHASE closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** an existing RevenueCat subscription,
**When** the replacement resolves access,
**Then** it is associated with the correct application account.

**Given** a premium access decision,
**When** protected content is admitted,
**Then** the backend verifies access rather than relying solely on device purchase state.

**Given** a cancelled subscription whose paid period has not expired,
**When** access is evaluated,
**Then** access continues for that valid paid period.

**Given** subscription state cannot yet be verified,
**When** status is displayed,
**Then** the user is not definitively classified as non-subscriber or premium without evidence,
**And** understandable pending/error and retry behavior is available.

**Given** confirmed access rights,
**When** catalog and start decisions refresh,
**Then** locks and playback admission remain consistent with those rights.

**Given** an active subscriber,
**When** catalog categories are composed,
**Then** the category reserved for non-subscribers is not shown.

**Given** an account switch,
**When** the next account resolves,
**Then** the previous account's premium state is not inherited by it.

**Given** representative existing subscribers adopting the replacement,
**When** upgrade/subscription scenarios are verified,
**Then** Android and iOS store test evidence confirms the applicable outcomes.


### Story 6.2: Open and dismiss premium offers with the required parental gate

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user considering premium access,
I want to view the appropriate subscription offers through the reference access controls,
So that I can decide whether to continue with a purchase.

**Requirements:** FR-10, FR-19; FR-13 completion entry integration; UX-DR21, UX-DR25; user clarification on 2026-09-28: preserve the legacy iOS parental gate. This gate remains in scope independently of the excluded parent-mode content feature.

**Planning status:** Offer-screen and parental-gate criteria approved for planning. Exact legacy gate triggers, challenge and validity/reset behavior remain verification prerequisites. This records a legacy behavior requirement, not a verified statement of current Apple review policy.

**Prerequisites:** Story 6.1; verified OQ-7 offerings, store products, reference premium entry points and actual offer-screen evidence. Verify the legacy iOS gate UI, challenge, trigger points and validity/reset rules before implementation. Its coverage of purchase, restore, subscription management and external links must be mapped across Epics 4 and 6; do not infer these details from the excluded parent-mode feature or from simulator Billing errors.

**Acceptance Criteria — approved offer behavior:**

**Given** a locked-content, profile membership or other reference premium entry point,
**When** the user activates it and any required gate is satisfied,
**Then** the correct premium offering screen opens.

**Given** available store/RevenueCat offerings,
**When** options are displayed,
**Then** product, price, currency and billing period match the actual offering.

**Given** offerings are loading or cannot be retrieved,
**When** the screen is displayed,
**Then** it shows loading or an understandable error with retry,
**And** it does not show fabricated prices or a misleading empty offer.

**Given** an open offer screen,
**When** the user dismisses it,
**Then** they return to the originating context,
**And** no purchase is assumed and no protected content starts.

**Given** repeated entry taps,
**When** the offer flow is already opening or visible,
**Then** duplicate offer screens are not stacked.

**Given** premium hooks from earlier epics, including completion,
**When** integration is verified,
**Then** their triggers and presentation match the reference behavior,
**And** actual offer-screen evidence is used rather than a Billing error screenshot.

**Acceptance Criteria — approved parental-gate behavior:**

**Given** an iOS entry point that requires the reference parental gate,
**When** the user attempts to continue,
**Then** the gate is shown before the gated destination or action is invoked.

**Given** a displayed parental gate,
**When** the challenge is failed, cancelled or dismissed,
**Then** the gated offer, purchase or external destination does not open,
**And** the user remains in or returns to the originating context.

**Given** successful gate completion,
**When** the pending action continues,
**Then** only that intended action proceeds once,
**And** gate completion is not treated as purchase success or premium entitlement.

**Given** all applicable iOS entry points and a completed gate,
**When** navigation, repeated taps, background/foreground and relaunch cases are verified,
**Then** no alternate entry bypasses a required gate,
**And** validity and reset behavior follow the verified reference contract rather than an invented permanent unlock.


### Story 6.3: Purchase a subscription and receive verified premium access

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user choosing a subscription,
I want my purchase completed and verified for the correct account,
So that I can access eligible premium content without paying again after an interrupted confirmation.

**Requirements:** FR-10, FR-19, FR-25; NFR-4–6; UX-DR21, UX-DR25; AD-4, AD-7–10; approved iOS parental-gate clarification.

**Prerequisites:** Stories 6.1–6.2 and Epic 3 account boundaries; resolved OQ-7 product, purchase-state, identity association and reconciliation contracts, plus applicable OQ-8 admission behavior. Verify reference success navigation and representative Android/iOS store test fixtures before implementation. Planning approval is not evidence of a completed purchase integration.

**Acceptance Criteria:**

**Given** a selected subscription product,
**When** the user starts purchasing,
**Then** the store flow targets that product,
**And** the required iOS parental-gate behavior is preserved.

**Given** an opening or active purchase flow,
**When** the user taps repeatedly,
**Then** concurrent duplicate purchase flows are not started.

**Given** a cancelled or failed purchase,
**When** its result is handled,
**Then** it does not grant premium access,
**And** the user can retry the purchase when appropriate.

**Given** a purchase awaiting store approval,
**When** its state is displayed,
**Then** it is represented as pending rather than successful.

**Given** a completed store purchase,
**When** entitlement is resolved,
**Then** it is associated with the correct application account,
**And** protected-content access opens only after backend verification.

**Given** payment completed but verification is delayed or interrupted,
**When** the application retries or restarts,
**Then** it reconciles the existing transaction status,
**And** it does not direct the user to pay again to resolve that pending confirmation.

**Given** an account change while a purchase is in progress,
**When** its result arrives,
**Then** the result is not applied to the wrong account.

**Given** confirmed premium access,
**When** the purchase flow completes,
**Then** membership state and content locks refresh consistently,
**And** content does not start playing automatically.

**Given** Android and iOS store test environments,
**When** purchase acceptance is verified,
**Then** evidence covers success, cancellation, pending approval and connection interruption.


### Story 6.4: Restore existing purchases

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user with an existing purchase,
I want to restore my eligible subscription,
So that I can recover verified premium access without purchasing again.

**Requirements:** FR-19, FR-25; NFR-4–6; UX-DR21, UX-DR25; AD-4, AD-7–10; approved iOS parental-gate clarification.

**Prerequisites:** Stories 6.1–6.3 and Epic 3 account boundaries; resolved OQ-7 restore, store/application identity association, account-conflict and entitlement contracts, plus applicable OQ-8 admission behavior. Verify reference restore entry points and the applicable iOS parental gate before implementation. Cross-account transfer rules must be explicitly resolved, not inferred from store ownership or matching email addresses.

**Acceptance Criteria:**

**Given** the reference restore entry point,
**When** the user activates restore purchases,
**Then** the restore flow starts with the applicable iOS parental-gate rule preserved.

**Given** a restore operation in progress,
**When** its state is displayed or the user taps again,
**Then** loading is visible,
**And** concurrent duplicate restore operations are not started.

**Given** a valid subscription returned by restore,
**When** access is resolved,
**Then** the subscription is verified against the contracted account association rules,
**And** premium access opens after backend confirmation.

**Given** a restore request,
**When** it is processed,
**Then** it does not initiate a new purchase or charge.

**Given** a completed restore with no eligible subscription,
**When** the result is shown,
**Then** the user receives an understandable result,
**And** premium access is not granted by that result.

**Given** a connection or verification failure,
**When** restore cannot be completed,
**Then** the failure is not misrepresented as no purchase found,
**And** the user can retry.

**Given** a purchase associated with another application account,
**When** restore encounters that purchase,
**Then** it is not silently transferred,
**And** the mismatch follows the verified account-conflict contract.

**Given** successful restore and verified access,
**When** the flow completes,
**Then** membership state and content locks refresh,
**And** content does not start automatically.

**Given** Android and iOS store test environments,
**When** restore acceptance is verified,
**Then** evidence covers valid, expired, absent and account-mismatched purchases.


### Story 6.5: Redeem supported promotion codes and offers

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user with an eligible promotion,
I want to redeem it through the supported reference flow,
So that I can receive its verified offer or access benefit on the correct account.

**Requirements:** FR-20, FR-25; NFR-4–6; UX-DR21, UX-DR25; approved iOS parental-gate clarification.

**Prerequisites:** Stories 6.1–6.4 and Epic 3 account boundaries; resolve OQ-7 supported promotion types, eligibility, redemption identity, repeat-use constraints, interrupted-operation reconciliation and provider/backend responsibilities. Verify reference entry points and applicable iOS parental-gate behavior. Discounted purchase and direct access criteria below apply only if the respective promotion type is supported by the verified legacy integration; they do not introduce new promotion types.

**Acceptance Criteria:**

**Given** a supported promotion code or offer,
**When** the user opens its reference entry point,
**Then** the supported redemption flow is available,
**And** the applicable iOS parental-gate behavior is preserved.

**Given** a submitted promotion,
**When** redemption is evaluated,
**Then** the authoritative service verifies validity and user eligibility.

**Given** an invalid, expired or ineligible promotion,
**When** redemption is rejected,
**Then** the user receives an understandable error corresponding to that result.

**Given** a connection failure,
**When** redemption cannot be verified,
**Then** the failure is not displayed as an invalid code,
**And** the user can retry.

**Given** repeated taps or a retry after interruption,
**When** the same redemption is processed,
**Then** the promotion benefit is not applied more than once for that operation.

**Given** a supported promotion offering a discounted purchase,
**When** its offer is displayed and accepted,
**Then** the offer price and duration are accurate,
**And** purchase proceeds through Story 6.3.

**Given** a supported promotion granting direct access,
**When** redemption completes,
**Then** premium access opens only after backend verification.

**Given** a confirmed promotion outcome,
**When** the result is applied,
**Then** it belongs to the correct application account,
**And** membership state and content locks refresh consistently.


### Story 6.6: View membership status and manage a subscription

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a subscriber,
I want to view my verified membership status and open the appropriate store management screen,
So that I can manage renewal and understand my remaining access.

**Requirements:** FR-19–20, FR-25; NFR-4–6; UX-DR21, UX-DR25; approved iOS parental-gate clarification.

**Prerequisites:** Stories 6.1–6.5; resolve OQ-7 reference membership fields, renewal/expiry semantics and store management destinations. Confirm applicable iOS parental-gate behavior. Resolve OQ-8 revocation effects on active media sessions before implementing those effects. Planning approval does not establish provider status mappings or a new grace-period policy.

**Acceptance Criteria:**

**Given** the profile membership entry point,
**When** the user views membership,
**Then** verified status is displayed,
**And** duration and renewal information shown by the reference experience is preserved.

**Given** a subscription eligible for store management,
**When** the user activates membership management,
**Then** the correct management destination for that purchase opens,
**And** the applicable iOS parental-gate control is applied.

**Given** the management destination cannot be opened,
**When** navigation fails,
**Then** an understandable error and retry are available.

**Given** the user returns from store management,
**When** the application resumes,
**Then** subscription status is refreshed,
**And** returning alone is not treated as proof of cancellation or renewal.

**Given** cancelled renewal with a valid paid period remaining,
**When** access is evaluated,
**Then** access continues until that paid period ends.

**Given** verified expiry or entitlement revocation,
**When** access state refreshes,
**Then** membership, content locks and new-content admission reflect the verified state.

**Given** status refresh cannot be completed,
**When** the result is displayed,
**Then** the verification problem is communicated,
**And** an unconfirmed change is not shown as successful.

**Given** an active media session affected by an access change,
**When** the change is processed,
**Then** session behavior follows the resolved OQ-8 revocation contract.


### Story 6.7: Preserve subscription rights through account transitions

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an anonymous subscriber becoming a registered user,
I want my verified subscription rights preserved through the account transition,
So that I can continue using my subscription without purchasing again.

**Requirements:** FR-19, FR-25; Epic 3 account-transition integration; NFR-4–6; UX-DR21, UX-DR25; AD-4, AD-7–10.

**Prerequisites:** Epic 3 account conversion, existing-account sign-in and isolation; Stories 6.1–6.6. Resolve OQ-7 RevenueCat/application identity association, anonymous conversion, existing-account conflicts and interrupted-operation reconciliation before implementation. Anonymous-to-new-account conversion and signing into an existing account remain distinct; no implicit account consolidation is authorized.

**Acceptance Criteria:**

**Given** an anonymous user with verified premium rights,
**When** that user converts to a new registered account,
**Then** the verified rights are preserved,
**And** the user is not asked to purchase again.

**Given** an interrupted conversion caused by connection loss or application termination,
**When** the operation resumes,
**Then** it can complete safely,
**And** rights are neither duplicated nor associated with the wrong account.

**Given** an anonymous user signing into an existing application account,
**When** subscription identity is reconciled,
**Then** subscriptions are not automatically merged,
**And** the resolved OQ-7 account-association contract applies.

**Given** logout or an account switch,
**When** the next account becomes active,
**Then** the previous account's premium state is not inherited by it.

**Given** a purchase or restore started before the account transition,
**When** a delayed result arrives,
**Then** it does not change the wrong account's access.

**Given** a completed account transition,
**When** membership and content locks refresh,
**Then** they agree with backend-verified rights.

**Given** Android and iOS store test environments,
**When** account-transition acceptance is verified,
**Then** evidence covers anonymous-to-new-account conversion and existing-account sign-in.

### Epic 6 planning review

Seven stories have been individually approved for planning. They complete the premium entry points deferred by earlier media epics and preserve the explicitly requested iOS parental gate. Planning approval does not establish implementation readiness or successful store evidence.

| Requirement | Story coverage |
| --- | --- |
| FR-10 | 6.1 verified existing access and subscriber category visibility; 6.2 locked-content entry; 6.3–6.7 entitlement refresh after user actions and transitions. |
| FR-19 | 6.1 existing subscribers; 6.3 purchase; 6.4 restore; 6.6 membership status and management; 6.7 account transitions. |
| FR-20 | 6.2 paywall entry/dismissal; 6.5 supported promotions; 6.6 membership management. |
| FR-13 premium integration | 6.2 connects the previously deferred reference completion paywall entry points. |
| FR-25 paid-access integration | 6.1 and 6.3–6.7 retain backend-authoritative admission; candidate/test permissions remain independent under Epic 5. |
| iOS parental-gate clarification | 6.2 defines the gate; 6.3–6.6 preserve applicable controls. Exact triggers, challenge and validity/reset behavior require legacy verification, including external-link entry points from Epic 4. |

OQ-7 remains a prerequisite for real RevenueCat/store product and offering identities, purchase/restore states, account association/conflict rules, supported promotion types and membership management fields/destinations. OQ-8 must resolve effects of revocation on active sessions. OQ-6 deletion/subscription consequences remain an integration prerequisite; this epic does not authorize deletion of a shared legacy Firebase identity or assume deleting an application account cancels a store subscription. Reference offer and parental-gate evidence must include applicable iOS phone/tablet behavior; emulator Billing failures are not acceptance evidence.

FR-21 backend flow-event integration and release-wide purchase/restore/account-transition regression evidence remain in Epic 7. Required financial-flow events must be verified before release alongside all other contracted event categories; no successful plan review substitutes for those checks.


## Epic 7: Receive a verified and recoverable replacement release

Users receive a replacement whose critical journeys, data continuity, platform behavior and recovery controls have been verified before staged exposure. Operators can investigate contracted flow failures without interrupting content consumption or exposing unnecessary personal data.

**Scope:** FR-21, FR-29; release verification of all active FRs and NFR-1–9; UX-DR1–25; AD-5, AD-9–11. Stories 7.1–7.11 are approved for planning. Epic 7 coverage review is recorded below; final workflow validation is pending.

**Planning constraints:** Build on Epics 1–6 and actual external backend/provider readiness. Flow-event integration is required before release; crash and performance diagnostics remain earlier feature responsibilities. Split event integration into bounded journey groups, beginning with catalog/search/detail events and their shared delivery behavior. Resolve applicable OQ-7 event schema, identity, retention, queue limits and retry/deduplication contracts and OQ-9 data/SDK scope before implementation. Complete OQ-2 measurement budgets and sufficient device/platform evidence, and OQ-10 rollout/containment/recovery decisions before release. AD-5 remains an earlier auth/migration development gate and must not be postponed to this epic. The existing anonymous iOS Simulator probe is partial evidence, not signed real-device or registered-session proof. No rollout or legacy shutdown is authorized by this planning review.


### Story 7.1: Diagnose discovery journeys through backend flow records

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user browsing and searching the catalog,
I want discovery failures to be traceable without blocking my use of the application,
So that problems can be investigated while I continue using available content.

**Requirements:** FR-21; NFR-4–5, NFR-8; AD-7, AD-9.

**Prerequisites:** Epic 1 discovery journeys and Epic 3 account boundaries; verified backend event ingestion and deduplication. Resolve OQ-7 event fields, identity, retention, bounded pending-record storage and retry/delivery rules, plus applicable OQ-9 privacy scope, before implementation. Shared delivery behavior is introduced for these consuming journeys and extended by later event stories; this story does not implement those later producers. Verify the applicable EXT-EVENTS closure records in the [external dependency register](#external-dependency-register).

**Acceptance Criteria:**

**Given** catalog, search or detail viewing,
**When** a contracted view, error or retry event occurs,
**Then** the event is recorded by the backend according to the agreed contract.

**Given** event delivery is unavailable,
**When** the user browses or consumes available content,
**Then** recording failure does not block the screen or content use.

**Given** a logical event sent more than once,
**When** the backend processes its retries,
**Then** only one logical record is retained,
**And** separate user actions remain distinguishable.

**Given** a prolonged connection interruption,
**When** pending events and retries accumulate,
**Then** storage and retry behavior remain within agreed bounds,
**And** memory and disk usage do not grow without limit.

**Given** pending events from an earlier account context,
**When** the user changes accounts,
**Then** those events are not attributed to the wrong user.

**Given** event payloads and delivery diagnostics,
**When** they are recorded,
**Then** they exclude content text, tokens, private media URLs and unnecessary personal data,
**And** raw search text is not recorded by default.

**Given** a real backend integration environment,
**When** acceptance is verified,
**Then** backend records confirm successful delivery, connection-interruption handling and retry deduplication.


### Story 7.2: Diagnose reading and listening journeys through backend flow records

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user reading or listening to content,
I want playback and completion failures to be traceable without disrupting my session,
So that problems can be investigated without changing my recorded history or usage duration.

**Requirements:** FR-21; FR-13–16 event-trigger and history integration; NFR-4–5, NFR-8; AD-3, AD-9.

**Prerequisites:** Approved reading/listening behavior from Epics 1–2, Epic 3 account boundaries and Story 7.1 delivery behavior. Resolve applicable OQ-7 event fields and session correlation before implementation. Preserve the verified completion triggers and history ownership from earlier stories; flow telemetry is not a second writer of completion or duration.

**Acceptance Criteria:**

**Given** a reading or listening session,
**When** a contracted start, completion, error or retry occurs,
**Then** its flow event is recorded by the backend.

**Given** a start or completion event,
**When** its trigger is evaluated,
**Then** it follows the behavior approved in the corresponding earlier story,
**And** opening the final page alone is not treated as completion.

**Given** a media flow event,
**When** it is recorded,
**Then** it is associated with the correct content, language, revision and usage session.

**Given** repeated notifications or delivery retries,
**When** events are processed,
**Then** the same logical event is not duplicated,
**And** distinct reading/listening sessions are not conflated.

**Given** recorded flow events,
**When** they are processed or retried,
**Then** they do not add a second completion or duration entry to user history.

**Given** unavailable event delivery,
**When** the user changes pages or operates playback controls,
**Then** page transitions, audio and controls are not blocked by the recording failure.

**Given** media events awaiting delivery,
**When** they are stored, retried or encounter an account transition,
**Then** Story 7.1 bounded-queue, privacy and account-isolation rules apply.

**Given** a real backend integration environment,
**When** interruption, backgrounding and repeat-delivery scenarios are exercised,
**Then** actual backend records verify the contracted outcomes.


### Story 7.3: Diagnose authentication and migration journeys through backend flow records

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user signing in, registering or carrying my data into the replacement,
I want failures in these journeys to be traceable,
So that interrupted transitions can be investigated without exposing my credentials or bypassing data-continuity checks.

**Requirements:** FR-21; Epic 3 authentication and migration integration; NFR-4–5, NFR-8; AD-4–7, AD-9.

**Prerequisites:** Epic 3 authentication/account/migration behavior and Story 7.1 delivery behavior; resolved OQ-7 event fields, operation correlation and identity rules. AD-5 remains a prerequisite before auth/migration feature development, not a gate deferred to this telemetry story. Migration terminal-state evidence must come from the authoritative durable result.

**Acceptance Criteria:**

**Given** a sign-in, registration or migration journey,
**When** a contracted start, outcome, error or retry occurs,
**Then** its flow event is recorded by the backend.

**Given** authentication-related flow events,
**When** they are recorded,
**Then** anonymous sessions, new registration, existing-account sign-in and account conversion are distinguishable.

**Given** a migration operation,
**When** its successful outcome is recorded,
**Then** durable data migration completion has been verified,
**And** upload acceptance alone is not treated as success.

**Given** interrupted operations and retries,
**When** flow events are delivered again,
**Then** the same logical event is not duplicated,
**And** distinct migration operations are not conflated.

**Given** an event-recording failure,
**When** authentication or migration continues,
**Then** telemetry failure does not block sign-in or registration,
**And** it does not bypass the actual migration completion gate.

**Given** authentication or migration event payloads,
**When** they are recorded,
**Then** they exclude passwords, tokens, email addresses, migrated data contents and unnecessary personal data.

**Given** pending events,
**When** they are queued, retried or encounter an account change,
**Then** Story 7.1 bounded-queue and account-isolation rules apply.

**Given** a real backend integration environment,
**When** success, failure and resumption after interruption are exercised,
**Then** backend records verify the contracted outcomes.


### Story 7.4: Diagnose paywall, purchase and restore journeys through backend flow records

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user purchasing or restoring premium access,
I want transaction-flow problems to be traceable with accurate outcomes,
So that delayed or failed confirmation can be investigated without charging me again or changing my access incorrectly.

**Requirements:** FR-21; FR-19–20 and FR-25 integration; NFR-4–5, NFR-8; AD-7, AD-9; approved iOS parental-gate clarification.

**Prerequisites:** Epic 6 purchase/restore/paywall behavior, Epic 3 account isolation and Story 7.1 delivery behavior. Resolve applicable OQ-7 event fields, provider-to-backend correlation, outcome mappings and transaction identifiers before implementation. Store payment confirmation and backend access verification remain separate facts.

**Acceptance Criteria:**

**Given** paywall opening/dismissal, purchase or restore flows,
**When** a contracted outcome, error or retry occurs,
**Then** its event is recorded by the backend.

**Given** a transaction-flow result,
**When** it is recorded,
**Then** user cancellation, pending store approval, failure and success are distinguishable.

**Given** store payment confirmation and backend entitlement verification,
**When** their events are recorded,
**Then** they are represented as distinct stages.

**Given** an action blocked by an incomplete parental gate,
**When** flow events are evaluated,
**Then** that blocked action is not recorded as an opened paywall or started purchase.

**Given** delayed results or repeat delivery,
**When** events are processed,
**Then** the same logical event is not duplicated,
**And** results are not associated with the wrong account.

**Given** event-recording failure,
**When** purchase or restore continues,
**Then** the recording failure does not alter the transaction result, initiate another payment or bypass access verification.

**Given** event payloads and delivery,
**When** they are recorded or retried,
**Then** full receipts, payment details, tokens and unnecessary personal data are excluded,
**And** Story 7.1 delivery rules apply.

**Given** Android and iOS store test environments,
**When** transaction-flow acceptance is verified,
**Then** the observed outcomes are compared with actual backend event records.


### Story 7.5: Verify reference screen and interaction parity on the release candidate

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user upgrading from the legacy application,
I want the retained screens and interactions to remain familiar and usable,
So that I can continue my existing journeys without unintended visual or behavioral changes.

**Requirements:** NFR-6–7; UX-DR1–25 for retained scope; reference-parity verification across active FRs; approved language and iOS parental-gate decisions.

**Prerequisites:** Epics 1–6 and a representative release candidate; reference evidence for the retained scope, matched device dimensions, language, content and account conditions. Use feature-level evidence already collected; this story consolidates candidate acceptance rather than deferring visual checks until release. Missing real-device/tablet and actual premium-offer evidence remains a gap until supplied. Use the feature-level UX-DR24 matrix and recorded normal/enlarged text scales, device cases and contrast criteria from Shared readiness and acceptance checks; repeat verification on the candidate.

**Acceptance Criteria:**

**Given** an in-scope screen,
**When** parity is evaluated,
**Then** the candidate is compared with the legacy reference under matching device dimensions, language, content and account state.

**Given** library, search, detail, reader/player, profile and premium journeys,
**When** release-candidate parity is checked,
**Then** appearance, navigation and primary controls are verified.

**Given** loading, empty-result, error and retry states,
**When** the affected screens are checked,
**Then** those states are included in the comparison.

**Given** Turkish, English and Portuguese screens,
**When** localized layouts are verified,
**Then** overflow, clipping and readability are checked.

**Given** Android/iOS phone and tablet coverage,
**When** device acceptance is assessed,
**Then** the applicable real-device evidence is available,
**And** emulator screen-size overrides do not substitute for real tablet evidence.

**Given** applicable iOS parental-gate entry points,
**When** parity is verified,
**Then** appearance, triggers, cancellation and successful continuation are compared with the reference.

**Given** a difference from the legacy experience,
**When** it is reviewed,
**Then** approved scope changes are explicitly distinguished,
**And** other visible or behavioral deviations are resolved before the affected screen is accepted.

**Given** a completed comparison,
**When** evidence is recorded,
**Then** it includes the reference image, candidate image and outstanding differences,
**And** a screen without reference evidence is not marked verified.

**Given** localized candidate screens at the agreed normal and enlarged text scales,
**When** the accessibility matrix runs on supported phone/tablet layouts,
**Then** titles, consent text, controls and all-items actions remain readable/reachable with safe areas respected,
**And** tappable icons expose the correct role, name and selected state through platform accessibility tools.

**Given** interactive controls over representative actual media,
**When** touch targets and contrast are measured,
**Then** at least 48dp touch targets and the agreed contrast criteria are met,
**And** evidence identifies the screen/state/device/language/content and failed checks block affected-screen acceptance. Reference screenshots alone do not prove accessibility.


### Story 7.6: Verify stability and performance on real devices

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user reading and listening on a supported device,
I want the replacement to remain stable and responsive during sustained use,
So that I can complete my sessions without crashes, freezes or memory-related termination.

**Requirements:** NFR-1–3, NFR-5–6, NFR-8; AD-3, AD-9, AD-11.

**Prerequisites:** Representative release candidate and diagnostics introduced with earlier features. Resolve OQ-2 device/network/cache matrix, device-class memory budgets, latency p50/p95 targets and crash-free observation window/sample before measurement. The fixed crash-free target is at least 99.5% of users per platform; this story does not invent the remaining thresholds. Resolve applicable OQ-9 diagnostic privacy scope. Prior feature evidence informs release verification but does not substitute for candidate-specific results.

**Acceptance Criteria:**

**Given** the agreed supported-device matrix,
**When** candidate verification runs,
**Then** it covers Android API 24 and iOS/iPadOS 15 minimums and the agreed phone, tablet and low-memory device classes.

**Given** heavy content, sustained playback and repeated reading/listening sessions,
**When** stability is assessed,
**Then** no known crash, freeze, memory-related termination or critical playback failure remains in the tested scenarios.

**Given** repeated sessions after warm-up,
**When** memory usage is measured,
**Then** unexplained continuous growth is resolved,
**And** the agreed device-class memory budgets are met.

**Given** cold/warm launch, first page/audio, page transition and reconnection scenarios,
**When** timings are measured,
**Then** network and cache conditions are recorded,
**And** agreed p50/p95 targets are met.

**Given** per-platform crash-free measurement,
**When** the stability target is evaluated,
**Then** at least 99.5% crash-free users is demonstrated with the agreed sufficient sample and observation window,
**And** a small set of successful tests is not treated as sufficient evidence.

**Given** a diagnostic failure record,
**When** it is investigated,
**Then** version, platform and device context support investigation,
**And** sensitive data is excluded.

**Given** candidate acceptance results,
**When** release readiness is reviewed,
**Then** results are tied to the candidate version,
**And** failed criteria are resolved before release acceptance.


### Story 7.7: Verify session and data continuity through a signed release-candidate upgrade

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As an existing user updating the legacy application,
I want my session, preserved data and subscription to remain usable,
So that I can adopt the replacement without reinstalling or recreating my account.

**Requirements:** FR-29; Epic 3 migration/account-continuity and Epic 6 existing-subscription integration; NFR-4, NFR-6; AD-4–5, AD-9, AD-11.

**Prerequisites:** Epics 3 and 6, signed representative release candidate and real Android/iOS devices. AD-5 evidence must already have been completed before auth/migration feature development; this story repeats end-to-end verification on the finished candidate rather than deferring that gate. Resolve applicable OQ-3 field/identity mappings and later-device local-record rules, OQ-6 migration/deletion boundaries and OQ-7 subscription identity contracts. The existing anonymous iOS Simulator probe is partial evidence only.

**Acceptance Criteria:**

**Given** an installed legacy application on Android or iOS,
**When** the real release candidate is installed using the correct application identity and signing chain,
**Then** it replaces that installation in place,
**And** uninstall/reinstall is not used to satisfy the upgrade scenario.

**Given** a registered Firebase session or anonymous local user records,
**When** the upgrade completes,
**Then** the registered session is preserved,
**And** existing anonymous preferences and local records remain readable.

**Given** completed migration,
**When** data continuity is verified,
**Then** preserved fields and identity relationships are compared,
**And** equal record counts alone are not accepted as proof.

**Given** interruption and application restart during migration,
**When** migration resumes,
**Then** it completes safely without data loss or duplication.

**Given** a later-upgraded second device for an already migrated account,
**When** the replacement starts,
**Then** completed account migration is not repeated,
**And** device-specific local records follow the approved reconciliation contract.

**Given** an existing subscriber and devices remaining on the legacy version,
**When** the replacement upgrade is verified,
**Then** the subscription resolves to the correct account,
**And** legacy-version devices continue operating against the legacy system.

**Given** upgrade verification results,
**When** evidence is recorded,
**Then** real device, old/new versions and distribution method are identified,
**And** the existing anonymous iOS Simulator report is not substituted for registered-session or signed real-device evidence.


### Story 7.8: Verify account and content access boundaries on the release candidate

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user of the replacement,
I want my account data and content permissions isolated correctly,
So that other users cannot access my data or content outside their authorized scope.

**Requirements:** FR-23, FR-25; Epic 3 account deletion/isolation and Epic 5 candidate/language integration; NFR-4–5, NFR-8; AD-4, AD-7–10.

**Prerequisites:** Epics 3, 5 and 6; release candidate and real backend with representative permission fixtures. Resolve applicable OQ-6 deletion/remigration contract, OQ-8 authorization/renewal/revocation behavior and OQ-9 diagnostic privacy scope before execution. This verifies earlier feature boundaries rather than postponing their implementation or tests to release.

**Acceptance Criteria:**

**Given** anonymous, registered, subscribed and authorized test users,
**When** the access matrix is verified,
**Then** each receives only its contracted permissions,
**And** subscription and candidate-testing permission do not substitute for each other.

**Given** an ordinary user,
**When** candidate revisions or test-only languages are requested,
**Then** access is denied according to the verified authorization contract.

**Given** logout or an account switch,
**When** private state or pending responses from the previous account remain,
**Then** previous account data, responses and premium state are not applied to the next account.

**Given** cached media or a direct link,
**When** protected content is requested,
**Then** cache and navigation do not bypass backend access control.

**Given** media URL renewal or access revocation,
**When** the candidate handles the change,
**Then** renewal preserves the same revision,
**And** revocation follows the resolved OQ-8 behavior.

**Given** deletion of a new-system account,
**When** deletion and later legacy-account access are verified,
**Then** the shared legacy Firebase identity remains usable,
**And** remigration does not restore deleted new-system data.

**Given** flow, error and crash records,
**When** their payloads are inspected,
**Then** tokens, private media URLs and unnecessary personal data are absent.

**Given** the release candidate and real backend,
**When** access acceptance is evaluated,
**Then** actual authorization outcomes are evidenced,
**And** a visible UI lock alone is not treated as proof.


### Story 7.9: Verify store-package permissions, privacy and production configuration

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user installing the store release,
I want its permissions, privacy information and enabled capabilities to match its actual behavior,
So that I can make informed choices and use the intended production experience.

**Requirements:** NFR-5–6; FR-9 updated language decision; Epic 4 permission/legal-document integration; FR-29 package-readiness integration; AD-9, AD-11; approved iOS parental-gate clarification.

**Prerequisites:** Representative store-submission package and Epics 1–6 integrations; resolve OQ-9 SDK/data inventory, permission behavior, audience and privacy declarations against requirements current at implementation time. Verify production configuration and remote-language controls. This planning approval does not assert current store-policy compliance or authorize submission.

**Acceptance Criteria:**

**Given** the store release candidate,
**When** its configuration is verified,
**Then** it connects to the intended production services,
**And** it contains no development endpoints, test credentials or authorization bypass mechanism.

**Given** the SDKs and permissions used by the package,
**When** submission readiness is reviewed,
**Then** SDKs, collected data and permissions are inventoried,
**And** store privacy and audience declarations agree with actual behavior.

**Given** notification permission acceptance, denial or a later settings change,
**When** the application handles the choice,
**Then** the corresponding behavior is verified,
**And** denial does not block basic content use.

**Given** privacy and terms entry points,
**When** the user opens them,
**Then** accessible content in the correct language is shown.

**Given** applicable iOS parental-gate entry points,
**When** their coverage is verified,
**Then** the gate protects the required destinations,
**And** alternate paths cannot bypass it.

**Given** production language configuration,
**When** content languages are evaluated,
**Then** Turkish, English and Portuguese are available,
**And** new-language test/live visibility follows remote configuration and authorization rules.

**Given** the package intended for store submission,
**When** readiness results are recorded,
**Then** evidence identifies that package,
**And** unresolved discrepancies prevent submission-ready acceptance.


### Story 7.10: Expand or stop staged rollout based on verified evidence

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user receiving the replacement update,
I want rollout to expand only when its critical outcomes are verified,
So that unresolved failures are contained before more users are exposed.

**Requirements:** FR-29; release verification across active FRs and NFRs; AD-11.

**Prerequisites:** Release evidence from earlier stories, verified backend compatibility and resolved OQ-10 platform cohorts, expansion stages, observation periods, stop thresholds and decision ownership, coordinated with OQ-2 sample/measurement requirements. Planning approval neither initiates deployment nor authorizes store rollout or legacy-system shutdown.

**Acceptance Criteria:**

**Given** a replacement mobile release awaiting exposure,
**When** rollout readiness is assessed,
**Then** required backend compatibility is verified before mobile cohorts are exposed,
**And** the legacy application continues receiving service.

**Given** Android and iOS rollout plans,
**When** they are finalized,
**Then** initial cohorts, expansion stages, observation periods and decision owners are specified for each platform.

**Given** a proposed rollout expansion,
**When** its evidence is reviewed,
**Then** crash-free rate, performance, migration, purchase/restore and critical journey outcomes are evaluated.

**Given** insufficient samples or missing evidence,
**When** an expansion decision is made,
**Then** the gap is not treated as success,
**And** the affected rollout stage is not expanded.

**Given** a release blocker such as data loss, wrong-account access or critical playback failure,
**When** it is detected,
**Then** expansion stops,
**And** the agreed response process is initiated.

**Given** a stop decision,
**When** containment is planned,
**Then** it also addresses protection of users who already installed the release.

**Given** a rollout decision,
**When** it is recorded,
**Then** version, platform, supporting evidence and decision owner are identified.

**Given** successful replacement rollout,
**When** legacy-system continuity is considered,
**Then** rollout success is not treated as authorization to shut down the legacy system.


### Story 7.11: Recover safely after a release problem

**Delivery status:** Scope approved; readiness pending prerequisites; acceptance evidence absent. See **Prerequisites** and [status definitions](#planning-status-definitions).

As a user who has installed the replacement,
I want release problems contained and corrected without losing my data or access rights,
So that I can resume reliable use after recovery.

**Requirements:** FR-29; NFR-1, NFR-4–6, NFR-8; AD-4, AD-10–11.

**Prerequisites:** Story 7.10 and the earlier migration, access and publication contracts. Resolve OQ-10 incident-specific actions, owners, compatibility checks, recovery evidence and resumption criteria before rollout. Recovery must account for already installed clients and post-migration writes; stopping store rollout is not a device downgrade or data restoration mechanism.

**Acceptance Criteria:**

**Given** a stopped rollout,
**When** recovery is planned,
**Then** devices that already installed the update are not assumed to revert to the legacy version.

**Given** a release problem,
**When** the response is selected,
**Then** the applicable action, owner and verification steps follow the resolved OQ-10 plan.

**Given** migrated data, new user operations and subscription rights,
**When** containment or recovery runs,
**Then** those records and rights are preserved,
**And** migration is not restarted to overwrite existing data.

**Given** a proposed backend or content-revision rollback,
**When** it is considered,
**Then** installed-client compatibility and data effects are verified before execution.

**Given** unaffected users and legacy clients,
**When** containment is applied,
**Then** their service continues,
**And** any necessary restriction has an explicitly defined scope.

**Given** a required corrective release,
**When** its acceptance is evaluated,
**Then** the reproducing failure scenario and affected critical journeys are verified.

**Given** a proposed rollout resumption,
**When** it is approved and recorded,
**Then** recovery evidence and Story 7.10 thresholds have been satisfied.

**Given** the recovery plan before rollout,
**When** readiness is assessed,
**Then** the plan has been rehearsed in a controlled environment,
**And** an unverified assumption that rollback will work is not sufficient.

### Epic 7 planning review

Eleven stories have been individually approved for planning. This epic consolidates release-candidate evidence and completes flow-event integration; it does not postpone feature diagnostics, authorization checks or AD-5 until release.

| Requirement | Story coverage |
| --- | --- |
| FR-21 | 7.1 discovery and bounded delivery; 7.2 reading/listening; 7.3 authentication/migration; 7.4 paywall/purchase/restore. Together they cover the required event categories, errors and retries. |
| FR-29 | 7.7 signed in-place candidate upgrade; 7.9 store-package readiness; 7.10 staged exposure and legacy coexistence; 7.11 recovery for installed clients. |
| NFR-1–3, NFR-6, NFR-8 | 7.6 device stability, memory, latency and crash-free evidence; 7.7 real-device upgrade evidence; 7.10 evidence-based expansion. |
| NFR-4–5 | 7.1–7.4 event integrity/privacy; 7.7 data continuity; 7.8 access, deletion and privacy boundaries; 7.9 SDK/permission declarations; 7.11 preservation during recovery. |
| NFR-7 and retained UX-DRs | 7.5 candidate parity consolidation, with specific feature-level coverage in earlier epics. |
| NFR-9 | 1.11 owns interrupted-transfer continuation, safe restart and immutable-asset integrity; 2.9 applies the same contract to listening; 1.12/2.10 verify bounded cache/resource behavior. Final validation checks these outcomes separately from playback-position recovery. No offline download UI or full offline-readiness promise is added. |

Open prerequisites remain explicit: OQ-2 measurement matrix, budgets and sufficient sample/window; OQ-3/6 migration/deletion contracts; OQ-7 event and purchase contracts; OQ-8 media/access/revocation rules; OQ-9 SDK, permission and store declarations; OQ-10 staged rollout and rehearsed recovery. Earlier OQ-1/4 reference and discovery gaps also require closure for affected stories. Actual acceptance evidence has not been produced by this planning exercise.

Current drafting and validation status is recorded in [Overview](#overview). All prerequisite and acceptance-evidence gates remain open until independently verified.


## Planning history

These snapshots record earlier approval checkpoints in their original wording. They describe the state at those checkpoints, not current work remaining. The dates of individual checkpoints are not reconstructed here; the current drafting/validation state is authoritative in Overview and frontmatter. Unique scope/evidence safeguards in these records remain subject to the current decisions above.

**Initial inventory and epic-order approval:**

> The user approved the requirements inventory and the revised seven-epic order: discovery/reading and listening precede auth/migration. Step 3 is in progress. Story boundaries and acceptance criteria require collaborative review; no application code is changed.

**Earlier epic-planning snapshot:**

> The user approved starting with Library listing verification, followed by card-to-content opening. Stories 1.1–1.12 are approved for planning. The user approved proceeding to Epic 2 after the Epic 1 planning review; open evidence/contract gates remain unchanged. Implementation remains outside this session. Remaining epics will be detailed sequentially.

**Earlier epic-planning snapshot:**

> The remaining story-design workflow is not complete: Epic 2 onward, final global FR/UX coverage verification and final validation remain pending. Source PRD language-availability wording is superseded by the explicit decision recorded here; source synchronization remains a documentation follow-up, not permission to apply the old language rule.

**Earlier epic-planning snapshot:**

> Current totals: 12 Epic 1 stories and 10 Epic 2 stories approved for planning. Epic 3–7 story design and global final FR/UX coverage validation remain pending.

**Earlier epic-planning snapshot:**

> Real store purchase/restore and entitlement verification remain Epic 6. Support/notification/deep-link surfaces remain Epic 4; candidate/live test authorization remains Epic 5. All 43 stories across Epics 1–3 are planning approvals only. Final FR/UX coverage review and cross-epic integration validation remain pending after Epics 4–7 are detailed.

**Earlier epic-planning snapshot:**

> Current totals: 12 + 10 + 21 + 6 = 49 individually approved stories across Epics 1–4.

**Earlier epic-planning snapshot:**

> Current totals: 12 + 10 + 21 + 6 + 5 = 54 individually approved stories across Epics 1–5. Epics 6–7 and final global coverage/readiness validation remain pending.

**Earlier epic-planning snapshot:**

> Current totals: 12 + 10 + 21 + 6 + 5 + 7 = 61 individually approved stories across Epics 1–6. Epic 7 and final global FR/NFR/UX coverage/readiness validation remain pending. The user approved proceeding to Epic 7.

**Epic 4 planning snapshot:**

> Epics 5–7 and final global coverage/readiness validation remain pending.

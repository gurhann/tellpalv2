# Library reference cases established before implementation

Established: 2026-10-04, before Library widgets were written. Owner: Codex implementation handoff.
Reference: `app/old/docs/design-system/evidence/emulator-home2.png` and `tablet-emulated-home-loaded.png`; SCREENS, DESIGN and TABLET.

Matched matrix: 411×914dp phone and 800×1280dp tablet, TR/EN/PT, text scales 1.0/1.3. Tablet keeps horizontal rails. Canvas, typography, 16dp gutters, 24dp section gaps, 3:4 geometry and clip/frame radii follow the companion contract. Parent-mode control is omitted by approved scope.

| Case | Expected rendering and interaction |
| --- | --- |
| Disabled controls | Library selected and enabled; Sleep/Profile unselected and disabled. Premium/search/All/cards retain reference appearance but cannot start a flow. Names remain localized; disabled controls have no tap semantic action. Library reselection preserves scroll and route. |
| Artwork error | Same cover rectangle and rounded frame, placeholder token background, decorative broken-image mark and bounded localized title; lock badge remains top-left. Card remains disabled. A separate 48dp retry can retry the image only. Successful catalog and other cards remain visible. |
| Long title | TR: “Ay Işığında Birlikte Yolculuğa Çıkan Küçük Kahramanların Hikâyesi”; EN: “The Little Friends Who Journeyed Together Through the Moonlit Forest”; PT: “A História dos Pequenos Amigos que Viajaram Juntos pela Floresta ao Luar”. Error-title text uses at most three lines and ellipsis, with full title in semantics. Section title wraps beside a 48dp All control. |
| Loading | Greeting/banner/search/navigation keep fixed geometry; two row-shaped skeletons use the same cover proportions. Localized loading status is announced. |
| Empty/error | Distinct localized text; only catalog error has a 48dp Retry action. Neither is inferred from an artwork error. |

Acceptance must compare these same cases and record actual images, safe areas, disabled/selected semantics, control sizes and contrast. Source screenshots show loaded ordinary content only; their error/disabled variants are contract-derived expected cases, not existing legacy captures. Numeric waits initially selected for verification: catalog 15 seconds, artwork 10 seconds. These finite values remain provisional until measured runtime evidence is recorded; the story is not accepted by this document.

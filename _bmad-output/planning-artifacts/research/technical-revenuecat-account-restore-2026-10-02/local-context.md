# TellPal context inspected by the lead

Inspected 2026-10-02. This is a local implementation observation, not evidence about RevenueCat's service behavior or the deployed dashboard configuration.

The sibling legacy checkout reports HEAD `e0c31b8`. Its current working-tree file `lib/common/services/set_user_type_information_service.dart` waits for Firebase auth and passes `FirebaseAuth.instance.currentUser?.uid` as RevenueCat `appUserID` when a Firebase user exists (lines 169–175). The identity transition helper calls `Purchases.logIn(targetFirebaseUid)` (line 468). These paths favor preserving the existing Firebase UID in the replacement. The source observation does not establish runtime purchase identity, all historical aliases, anonymous purchase eligibility, actual production restore behavior, or whether the working tree matches deployed builds.

The product rule for cross-account subscription restore is still undecided. The earlier suggestion to prohibit automatic transfer was not accepted as a final decision; the user requested this investigation before deciding.

The user subsequently confirmed that legacy users can purchase without registration and approved preserving that ability. The cross-account restore policy remains undecided.

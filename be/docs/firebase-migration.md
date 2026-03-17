# Firebase Migration

Bu dokuman, Firebase'den PostgreSQL'e tek seferlik veri tasima akisinin staging formatini ve command contract'ini tanimlar.

## Staging Dizini

Migration, tek bir staging dizini altinda calisir. Varsayilan dizin:

```text
be/var/firebase-migration/
```

Bu dizinde beklenen dosyalar:

- `users.json`
- `content-events.json`
- `app-events.json`

Tum dosyalar UTF-8 encoded JSON array formatinda olmalidir.

## Kullanici Formati

`users.json` icerigi:

```json
[
  {
    "firebaseUid": "firebase-user-1",
    "allowMarketing": true,
    "defaultProfile": {
      "displayName": "Ada",
      "ageRange": "3-5",
      "avatarMediaId": null,
      "favoriteGenres": ["sleep", "animals"],
      "mainPurposes": ["bedtime"]
    }
  }
]
```

Kurallar:

- `firebaseUid` zorunludur ve bos olamaz.
- `defaultProfile` verilmezse sistem varsayilan primary profile uretir.
- Ayni `firebaseUid` ikinci kez import edilirse yeni kullanici olusturulmaz; kayit skip edilir.

## Content Event Formati

`content-events.json` icerigi:

```json
[
  {
    "firebaseUid": "firebase-user-1",
    "legacyEventKey": "firebase-content-1",
    "contentExternalKey": "moonlight-story",
    "languageCode": "tr",
    "firebaseEventType": "START_CONTENT",
    "occurredAt": "2026-03-17T08:15:00Z",
    "sessionId": "11111111-1111-1111-1111-111111111111",
    "leftPage": null,
    "engagementSeconds": 12,
    "metadata": {
      "source": "firebase-export"
    }
  }
]
```

Event tipi esleme kurali:

- `START_CONTENT` -> `START`
- `LEFT_CONTENT` -> `EXIT`
- `FINISH_CONTENT` -> `COMPLETE`

## App Event Formati

`app-events.json` icerigi:

```json
[
  {
    "firebaseUid": "firebase-user-1",
    "legacyEventKey": "firebase-app-1",
    "eventType": "APP_OPENED",
    "contentExternalKey": null,
    "occurredAt": "2026-03-17T08:20:00Z",
    "payload": {
      "source": "firebase-export"
    }
  }
]
```

Kurallar:

- `contentExternalKey`, yalnizca olay bir icerige bagliysa gonderilmelidir.
- `LOCKED_CONTENT_CLICKED` icin `contentExternalKey` zorunludur.

## Command Contract

Migration command contract'i Spring property tabanlidir:

```text
--tellpal.firebase-migration.enabled=true
--tellpal.firebase-migration.dry-run=true
--tellpal.firebase-migration.staging-dir=C:/temp/firebase-export
--tellpal.firebase-migration.import-users=true
--tellpal.firebase-migration.import-content-events=true
--tellpal.firebase-migration.import-app-events=true
```

Dry-run davranisi:

- Dosyalar ve JSON formatlari okunur.
- Import edilecek kayit sayilari raporlanir.
- Veritabani yazimi yapilmaz.

Import davranisi:

- `import-users=true` ise `users.json` kayitlari islenir.
- `import-content-events=true` ise `content-events.json` kayitlari islenir.
- `import-app-events=true` ise `app-events.json` kayitlari islenir.

## Operasyon Notlari

- Staging dosyalari source-control'a commit edilmemelidir.
- Import idempotent olacak sekilde tasarlanmistir; ayni kullanici veya ayni `legacyEventKey` tekrar insert edilmez.
- Once `dry-run=true`, sonra ayni staging dizini ile `dry-run=false` kosulmalidir.

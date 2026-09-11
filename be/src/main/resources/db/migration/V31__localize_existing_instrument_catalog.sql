INSERT INTO instrument_catalog_localizations (instrument_catalog_id, language_code, display_name)
SELECT catalog.id, labels.language_code, labels.display_name
  FROM instrument_catalogs catalog
  JOIN (
      VALUES
          ('CELESTA', 'en', 'Celesta'), ('BELL', 'en', 'Bell'), ('VIOLIN', 'en', 'Violin'), ('RHODES', 'en', 'Rhodes'), ('GLOCKENSPIEL', 'en', 'Glockenspiel'), ('HARP', 'en', 'Harp'), ('VIBRAPHONE', 'en', 'Vibraphone'), ('STRING_ORCHESTRA', 'en', 'String Orchestra'),
          ('CELESTA', 'pt', 'Celesta'), ('BELL', 'pt', 'Sino'), ('VIOLIN', 'pt', 'Violino'), ('RHODES', 'pt', 'Rhodes'), ('GLOCKENSPIEL', 'pt', 'Glockenspiel'), ('HARP', 'pt', 'Harpa'), ('VIBRAPHONE', 'pt', 'Vibrafone'), ('STRING_ORCHESTRA', 'pt', 'Orquestra de Cordas'),
          ('CELESTA', 'es', 'Celesta'), ('BELL', 'es', 'Campana'), ('VIOLIN', 'es', 'Violín'), ('RHODES', 'es', 'Rhodes'), ('GLOCKENSPIEL', 'es', 'Glockenspiel'), ('HARP', 'es', 'Arpa'), ('VIBRAPHONE', 'es', 'Vibráfono'), ('STRING_ORCHESTRA', 'es', 'Orquesta de Cuerdas'),
          ('CELESTA', 'de', 'Celesta'), ('BELL', 'de', 'Glocke'), ('VIOLIN', 'de', 'Violine'), ('RHODES', 'de', 'Rhodes'), ('GLOCKENSPIEL', 'de', 'Glockenspiel'), ('HARP', 'de', 'Harfe'), ('VIBRAPHONE', 'de', 'Vibraphon'), ('STRING_ORCHESTRA', 'de', 'Streichorchester')
  ) AS labels(code, language_code, display_name) ON labels.code = catalog.code;

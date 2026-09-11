INSERT INTO instrument_catalogs (code, is_active)
VALUES ('PIANO', true);

INSERT INTO instrument_catalog_localizations (instrument_catalog_id, language_code, display_name)
SELECT catalog.id, labels.language_code, labels.display_name
  FROM instrument_catalogs catalog
  JOIN (
      VALUES
          ('tr', 'Piyano'),
          ('en', 'Piano'),
          ('es', 'Piano'),
          ('pt', 'Piano'),
          ('de', 'Klavier')
  ) AS labels(language_code, display_name) ON true
 WHERE catalog.code = 'PIANO';

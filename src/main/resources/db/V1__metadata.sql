CREATE TABLE series (
    id INTEGER PRIMARY KEY CHECK (id > 0),
    original_title TEXT NOT NULL CHECK (length(trim(original_title)) > 0),
    origin_country TEXT NOT NULL,
    external_id TEXT UNIQUE,
    scope TEXT NOT NULL CHECK (scope = 'LIVE_ACTION_SCRIPTED')
);
CREATE TABLE series_alias (
    series_id INTEGER NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    locale TEXT NOT NULL,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    normalized_title TEXT NOT NULL,
    verified INTEGER NOT NULL CHECK (verified IN (0, 1)),
    evidence TEXT NOT NULL CHECK (verified = 0 OR length(trim(evidence)) > 0),
    PRIMARY KEY (series_id, locale, normalized_title)
);
CREATE INDEX alias_lookup ON series_alias(normalized_title, verified);
CREATE TABLE document (
    id TEXT PRIMARY KEY NOT NULL,
    series_id INTEGER NOT NULL REFERENCES series(id) ON DELETE CASCADE,
    url TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('COMMUNITY_DISCUSSION', 'SERIES_INFORMATION', 'PROFESSIONAL_REVIEW', 'STREAMING_PLATFORM')),
    provenance TEXT NOT NULL CHECK (length(trim(provenance)) > 0),
    license TEXT NOT NULL CHECK (length(trim(license)) > 0)
);
CREATE INDEX document_series ON document(series_id);

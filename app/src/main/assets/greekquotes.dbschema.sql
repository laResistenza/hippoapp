
DROP VIEW IF EXISTS v_quotes_and_translations;
DROP VIEW IF EXISTS v_schermate;

DROP TABLE IF EXISTS "playlists_schermate";
DROP TABLE IF EXISTS "playlists";
DROP TABLE IF EXISTS "easter_egg_comments";
DROP TABLE IF EXISTS "linguistic_notes";
DROP TABLE IF EXISTS "quotes_in_schermate";
DROP TABLE IF EXISTS "schermate";
DROP TABLE IF EXISTS "quotes_translations";
DROP TABLE IF EXISTS "translation_languages";
DROP TABLE IF EXISTS "android_metadata";
DROP TABLE IF EXISTS "greek_quotes";
DROP TABLE IF EXISTS "credits";

CREATE TABLE IF NOT EXISTS "credits" (
	"_id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"credit"	TEXT
);

CREATE TABLE IF NOT EXISTS "greek_quotes" (
	"_id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"quoteText"	TEXT UNIQUE,
	"phoneticTranscription" TEXT,
	"audioFileName"	TEXT
);

CREATE TABLE IF NOT EXISTS "android_metadata" (
	"locale"	TEXT DEFAULT 'en_US'
);

CREATE TABLE IF NOT EXISTS "translation_languages" (
	"_id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"LanguageName"	TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS "quotes_translations" (
	"greek_quote_id"	INTEGER NOT NULL,
	"language_id"	INTEGER NOT NULL,
	"translation"	TEXT,
	PRIMARY KEY("greek_quote_id","language_id"),
	FOREIGN KEY("greek_quote_id") REFERENCES "greek_quotes"("_id"),
	FOREIGN KEY("language_id") REFERENCES "translation_languages"("_id")
);

CREATE TABLE IF NOT EXISTS "schermate" (
	"_id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"title"	TEXT,
	"description"	TEXT,
	"author_ref" TEXT
);

CREATE TABLE IF NOT EXISTS "quotes_in_schermate" (
	"greek_quote_id"	INTEGER NOT NULL,
	"schermata_id"	INTEGER NOT NULL,
	"position"  INTEGER NOT NULL DEFAULT 1,

	PRIMARY KEY("greek_quote_id","schermata_id"),
	FOREIGN KEY("greek_quote_id") REFERENCES "greek_quotes"("_id"),
	FOREIGN KEY("schermata_id") REFERENCES "schermate"("_id")
);

CREATE TABLE IF NOT EXISTS "linguistic_notes" (
	"schermata_id"	INTEGER NOT NULL,
	"language_id"	INTEGER NOT NULL,
	"linguisticNote"	TEXT,
	PRIMARY KEY("schermata_id","language_id"),
	FOREIGN KEY("schermata_id") REFERENCES "schermate"("_id"),
	FOREIGN KEY("language_id") REFERENCES "translation_languages"("_id")
);

CREATE TABLE IF NOT EXISTS "easter_egg_comments" (
	"schermata_id"	INTEGER NOT NULL,
	"language_id"	INTEGER NOT NULL,
	"eeComment"	TEXT,
	PRIMARY KEY("schermata_id","language_id"),
	FOREIGN KEY("schermata_id") REFERENCES "schermate"("_id"),
	FOREIGN KEY("language_id") REFERENCES "translation_languages"("_id")
);

CREATE TABLE IF NOT EXISTS "playlists" (
	"_id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
	"description"   TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS "playlists_schermate" (
	"playlist_id"	INTEGER NOT NULL,
	"schermata_id"	INTEGER NOT NULL,
	PRIMARY KEY("schermata_id","playlist_id"),
	FOREIGN KEY("schermata_id") REFERENCES "schermate"("_id"),
	FOREIGN KEY("playlist_id") REFERENCES "playlists"("_id")
);

CREATE VIEW v_quotes_and_translations AS
    SELECT gq._id AS quote_id, tl.LanguageName AS translation_language, gq.quoteText AS quote, qt.translation AS translation
    FROM greek_quotes gq, quotes_translations qt, translation_languages tl
    WHERE gq._id = qt.greek_quote_id AND qt.language_id = tl._id
    ORDER BY translation_language;

CREATE VIEW v_schermate AS
    SELECT s._id AS s_id,
    gq._id AS gq_id,
    gq.quoteText AS quote,
    gq.phoneticTranscription AS phoneticTranscription,
    qs.position AS position,
    s.title AS title,
    s.description AS description,
    s.author_ref AS cit,
    gq.audioFileName as audioFileName
    FROM greek_quotes gq, quotes_in_schermate qs, schermate s
    WHERE  qs.greek_quote_id = gq._id AND qs.schermata_id = s._id
    ORDER BY s._id;

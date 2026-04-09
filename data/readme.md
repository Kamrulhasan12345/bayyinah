# Bayyinah data

This folder stores generated runtime data for Bayyinah.

## What lives here

- `quran.db`: main SQLite dataset used by the application
- `quran-audio-test.db`: optional test dataset used by targeted script runs
- `audio/`: downloaded recitation files

## How data is generated

Data artifacts are produced by the scripts pipeline in `../scripts`.

From `scripts/`:

```bash
npm install
node --env-file=.env --import=tsx src/index.ts
```

## Audio directory format

When audio generation is enabled, files are written to:

- `data/audio/recitation_<id>/surah_<chapter>/<surahayah>.mp3`

Example:

- `data/audio/recitation_2/surah_001/001001.mp3`

## Schema summary (`quran.db`)

Main tables created by the scripts schema setup:

- `chapters`
- `chapters_i18n`
- `verses`
- `translations`
- `translation_text`
- `audio_recitations`
- `verse_audio`

Indexes and unique constraints are created for chapter i18n, verse lookup, translation lookup, and verse-audio mapping.

## Important client note

The desktop client does not automatically read `data/quran.db` from this repository path.

By default, the client uses `~/.bayyinah/config.yaml` and local home-directory paths. Update `databasePath` and `audioRootPath` to point here, or copy assets to the default home paths.

## Safe test run

To test audio generation without touching `quran.db`:

```bash
cd ../scripts
npm run generate:audio:fatihah
```

That command writes `quran-audio-test.db` and a small audio subset.

## Related docs

- [Repository root guide](../readme.md)
- [Scripts guide](../scripts/readme.md)

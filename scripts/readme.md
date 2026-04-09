# scripts

TypeScript data pipeline for generating Bayyinah Quran datasets.

## Purpose

This project fetches Quran content and metadata through `@quranjs/api`, then writes local artifacts used by the rest of the system:

- SQLite database (`../data/quran.db` by default)
- Translation metadata and text
- Optional audio recitation metadata and downloaded files

## Requirements

- Node.js 22+
- Quran API credentials (`QURAN_CLIENT_ID`, `QURAN_CLIENT_SECRET`)

## Setup

```bash
npm install
```

Create `.env` (copy from `.env.example`) and set required values.

## Environment variables

| Variable | Required | Description |
| --- | --- | --- |
| `QURAN_CLIENT_ID` | yes | Quran API client id |
| `QURAN_CLIENT_SECRET` | yes | Quran API client secret |
| `TRANSLATION_IDS` | depends | Required unless `--skip-translation-text` is used |
| `CHAPTER_IDS` | no | Optional subset (comma separated) |
| `AUDIO_RECITATION_ID` | no | Audio reciter id; if missing, audio stage is skipped |
| `AUDIO_DIR` | no | Audio output root (default `../data/audio`) |
| `AUDIO_BASE_URL` | no | Base URL for relative audio paths |
| `SKIP_AUDIO` | no | Set to `1` or `true` to force audio skip |

## Commands

```bash
# Full generation
npm run generate

# Small test run for Fatihah + audio
npm run generate:audio:fatihah

# Remove main DB only
npm run clean

# Remove audio test DB and audio directory
npm run clean:audio-test
```

## CLI flags

`npm run generate -- --flag value`

Supported flags:

- `--db <path>`
- `--drop`
- `--per-page <number>`
- `--translations <id,id,...>`
- `--chapters <id,id,...>`
- `--chapter-langs <code,code,...>`
- `--chapters-i18n-names-only`
- `--audio-recitation <id>`
- `--audio-dir <path>`
- `--audio-base-url <url>`
- `--skip-chapters`
- `--skip-chapters-i18n`
- `--skip-verses`
- `--skip-audio`
- `--skip-translations`
- `--skip-translation-text`

## Pipeline stages

The orchestrator in `src/index.ts` runs stages in this order:

1. Open/create DB and schema
2. Chapters
3. Chapter i18n
4. Verses
5. Audio (optional)
6. Translation metadata
7. Translation text (optional)

Schema setup is implemented in `src/db.ts`.

## Outputs

Default outputs:

- `../data/quran.db`
- `../data/audio/recitation_<id>/surah_<chapter>/<surahayah>.mp3`

Console logs include inserted/updated counts per stage and total runtime.

## Troubleshooting

- Missing env values: verify `.env` and required credential variables.
- Empty translation stage: provide `TRANSLATION_IDS` or use `--skip-translation-text`.
- Audio download issues: validate recitation id, network, and base URL.
- Existing DB conflicts: rerun with `--drop` for a full rebuild.

## Related docs

- [Repository root guide](../readme.md)
- [Data directory guide](../data/readme.md)
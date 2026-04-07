import { Language, QuranClient } from '@quranjs/api';
import { createSchema, dropAll, openDb, withTransaction } from './db.js';
import { populateChapters } from './chapters.js';
import { populateChaptersI18n } from './chapters_i18n.js';
import { populateVerses } from './verses.js';
import { populateAudio } from './audio.js';
import { populateTranslationMetadata, populateTranslationText } from './translations.js';
import { formatDurationMs, parseArgs, parseNumberList, requireEnv, toDbPath } from './utils.js';

type Flags = {
  dbPath: string;
  drop: boolean;
  perPage: number;
  translationIds: number[];
  chapterIds: number[];
  chapterLangs: string[];
  chaptersI18nNamesOnly: boolean;
  audioRecitationId: number | null;
  audioDir: string;
  audioBaseUrl: string;
  skipChapters: boolean;
  skipChaptersI18n: boolean;
  skipVerses: boolean;
  skipAudio: boolean;
  skipTranslations: boolean;
  skipTranslationText: boolean;
};

function parseBooleanFlag(value: string | undefined): boolean {
  if (!value) return false;
  const normalized = value.trim().toLowerCase();
  return normalized === '1' || normalized === 'true';
}

function normalizeChapterIds(values: number[]): number[] {
  return [...new Set(values.map((v) => Math.trunc(v)).filter((v) => v >= 1 && v <= 114))];
}

function getFlags(): Flags {
  const args = parseArgs(process.argv.slice(2));
  const dbPath = toDbPath((args.db as string | undefined) ?? process.env.DB_PATH);
  const drop = Boolean(args.drop);
  const perPageRaw = Number((args['per-page'] as string | undefined) ?? process.env.PER_PAGE ?? 50);
  const perPage = Number.isFinite(perPageRaw) && perPageRaw > 0 ? perPageRaw : 50;

  const translationIds = parseNumberList((args.translations as string | undefined) ?? process.env.TRANSLATION_IDS).filter(
    (n) => n > 0
  );
  const chapterIds = normalizeChapterIds(
    parseNumberList((args.chapters as string | undefined) ?? process.env.CHAPTER_IDS)
  );

  const chapterLangsRaw =
    ((args['chapter-langs'] as string | undefined) ?? process.env.CHAPTER_LANGS ?? 'en').trim();
  const chapterLangs = chapterLangsRaw
    .split(',')
    .map((v: string) => v.trim())
    .filter(Boolean);

  const chaptersI18nNamesOnlyEnv = (process.env.CHAPTERS_I18N_NAMES_ONLY ?? '').trim();
  const chaptersI18nNamesOnly =
    Boolean(args['chapters-i18n-names-only']) || chaptersI18nNamesOnlyEnv === '1' || chaptersI18nNamesOnlyEnv === 'true';

  const audioRecitationRaw = Number(
    (args['audio-recitation'] as string | undefined) ?? process.env.AUDIO_RECITATION_ID ?? ''
  );
  const audioRecitationId =
    Number.isFinite(audioRecitationRaw) && audioRecitationRaw > 0 ? Math.trunc(audioRecitationRaw) : null;

  const skipAudio =
    Boolean(args['skip-audio']) ||
    parseBooleanFlag(process.env.SKIP_AUDIO) ||
    audioRecitationId == null;

  const audioDirRaw = ((args['audio-dir'] as string | undefined) ?? process.env.AUDIO_DIR ?? '../data/audio').trim();
  const audioDir = audioDirRaw.length > 0 ? audioDirRaw.replaceAll('\\', '/') : '../data/audio';

  const audioBaseUrlRaw =
    ((args['audio-base-url'] as string | undefined) ?? process.env.AUDIO_BASE_URL ?? 'https://verses.quran.com/').trim();
  const audioBaseUrl = audioBaseUrlRaw.length > 0 ? audioBaseUrlRaw : 'https://verses.quran.com/';

  return {
    dbPath,
    drop,
    perPage,
    translationIds,
    chapterIds,
    chapterLangs,
    chaptersI18nNamesOnly,
    audioRecitationId,
    audioDir,
    audioBaseUrl,
    skipChapters: Boolean(args['skip-chapters']),
    skipChaptersI18n: Boolean(args['skip-chapters-i18n']),
    skipVerses: Boolean(args['skip-verses']),
    skipAudio,
    skipTranslations: Boolean(args['skip-translations']),
    skipTranslationText: Boolean(args['skip-translation-text']),
  };
}

async function resolveChapterLangs(client: QuranClient, requested: string[]) {
  const normalized = requested.map((v) => v.trim().toLowerCase()).filter(Boolean);
  if (normalized.length === 0) return ['en'];

  if (normalized.includes('all')) {
    const langs = (await client.resources.findAllLanguages()) as Array<{ isoCode?: string }>;
    const codes = langs
      .map((l) => (l.isoCode ?? '').trim().toLowerCase())
      .filter(Boolean);
    // De-dupe while keeping order.
    return [...new Set(codes)];
  }

  return [...new Set(normalized)];
}

async function main() {
  const startedAt = Date.now();
  const flags = getFlags();

  const clientId = requireEnv('QURAN_CLIENT_ID');
  const clientSecret = requireEnv('QURAN_CLIENT_SECRET');
  const client = new QuranClient({
    clientId,
    clientSecret,
    defaults: {
      language: Language.ENGLISH,
    },
  });

  const db = openDb(flags.dbPath);
  withTransaction(db, () => {
    if (flags.drop) dropAll(db);
    createSchema(db);
  });

  if (!flags.skipChapters) {
    const { count } = await populateChapters(db, client);
    console.log(`chapters: inserted/updated ${count}`);
  }

  if (!flags.skipChaptersI18n) {
    const langs = await resolveChapterLangs(client, flags.chapterLangs);
    const { count } = await populateChaptersI18n(db, client, {
      languages: langs,
      includeInfo: flags.chaptersI18nNamesOnly ? false : 'auto',
    });
    console.log(`chapters_i18n: inserted/updated ${count}`);
  }

  if (!flags.skipVerses) {
    const { count } = await populateVerses(db, client, {
      perPage: flags.perPage,
      chapterIds: flags.chapterIds,
    });
    console.log(`verses: inserted/updated ${count}`);
  }

  if (!flags.skipAudio) {
    const { count, downloaded, reused, failedDownloads, missingVerses } = await populateAudio(db, client, {
      recitationId: flags.audioRecitationId as number,
      perPage: flags.perPage,
      chapterIds: flags.chapterIds,
      audioDir: flags.audioDir,
      audioBaseUrl: flags.audioBaseUrl,
    });
    console.log(
      `verse_audio: inserted/updated ${count} (downloaded=${downloaded}, reused=${reused}, failed=${failedDownloads}, missing_verses=${missingVerses})`
    );
  }

  if (!flags.skipTranslations) {
    const { count } = await populateTranslationMetadata(db, client);
    console.log(`translations: inserted/updated ${count}`);
  }

  if (!flags.skipTranslationText) {
    if (flags.translationIds.length === 0) {
      throw new Error(
        'No translation IDs provided. Set TRANSLATION_IDS="20,131" or pass --translations 20,131 (or use --skip-translation-text).'
      );
    }

    const { count } = await populateTranslationText(db, client, {
      translationIds: flags.translationIds,
      perPage: flags.perPage,
    });
    console.log(`translation_text: inserted/updated ${count}`);
  }

  console.log(`done in ${formatDurationMs(Date.now() - startedAt)} -> ${flags.dbPath}`);
}

await main();

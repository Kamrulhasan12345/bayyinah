import { existsSync, mkdirSync, statSync } from 'node:fs';
import { writeFile } from 'node:fs/promises';
import path from 'node:path';
import type { QuranClient } from '@quranjs/api';
import type { Db } from './db.js';
import { withTransaction } from './db.js';

type RecitationResource = {
  id?: number;
  reciterName?: string;
  style?: string;
  translatedName?: {
    name?: string;
  };
};

type VerseRecitation = {
  verseKey: string;
  url: string;
  format?: string;
};

type VerseRecitationResponse = {
  audioFiles?: VerseRecitation[];
  pagination?: {
    nextPage?: number | null;
  };
};

type VerseAudioRow = {
  verseId: number;
  verseKey: string;
  sourceUrl: string;
  localPath: string | null;
  format: string | null;
};

function normalizeChapterIds(chapterIds: number[] | undefined): number[] {
  const normalized = [...new Set((chapterIds ?? []).map((v) => Math.trunc(v)).filter((v) => v >= 1 && v <= 114))];
  if (normalized.length > 0) return normalized;
  return Array.from({ length: 114 }, (_v, i) => i + 1);
}

function ensureTrailingSlash(value: string): string {
  return value.endsWith('/') ? value : `${value}/`;
}

function toAbsoluteAudioUrl(rawUrl: string, baseUrl: string): string {
  if (/^https?:\/\//i.test(rawUrl)) return rawUrl;
  return new URL(rawUrl.replace(/^\/+/, ''), ensureTrailingSlash(baseUrl)).toString();
}

function parseVerseKey(verseKey: string) {
  const [surahRaw, ayahRaw] = verseKey.split(':', 2);
  const surah = Number(surahRaw);
  const ayah = Number(ayahRaw);
  if (!Number.isFinite(surah) || !Number.isFinite(ayah) || surah <= 0 || ayah <= 0) {
    throw new Error(`Invalid verse key: ${verseKey}`);
  }

  return {
    surah: Math.trunc(surah),
    ayah: Math.trunc(ayah),
  };
}

function toPosix(filePath: string): string {
  return filePath.replaceAll('\\', '/');
}

function resolveAudioTarget(
  audioRoot: string,
  recitationId: number,
  surah: number,
  ayah: number,
  format: string | null | undefined
) {
  const extRaw = (format ?? 'mp3').trim().toLowerCase();
  const ext = extRaw.replace(/[^a-z0-9]/g, '') || 'mp3';
  const chapterFolder = `surah_${String(surah).padStart(3, '0')}`;
  const fileName = `${String(surah).padStart(3, '0')}${String(ayah).padStart(3, '0')}.${ext}`;
  const relativePath = toPosix(path.join(`recitation_${recitationId}`, chapterFolder, fileName));
  const absolutePath = path.resolve(audioRoot, relativePath);

  return {
    relativePath,
    absolutePath,
  };
}

async function downloadAudioIfMissing(sourceUrl: string, targetPath: string): Promise<{ downloaded: boolean }> {
  if (existsSync(targetPath)) {
    try {
      const size = statSync(targetPath).size;
      if (size > 0) {
        return { downloaded: false };
      }
    } catch {
      // Fall through and re-download if stat check fails.
    }
  }

  mkdirSync(path.dirname(targetPath), { recursive: true });
  const response = await fetch(sourceUrl);
  if (!response.ok) {
    throw new Error(`Download failed: ${response.status} ${response.statusText}`);
  }

  const body = Buffer.from(await response.arrayBuffer());
  await writeFile(targetPath, body);
  return { downloaded: true };
}

async function fetchAllVerseRecitationsByChapter(
  client: QuranClient,
  chapterId: number,
  recitationId: number,
  perPage: number
) {
  const allAudioFiles: VerseRecitation[] = [];
  let page = 1;

  for (; ;) {
    const response = (await client.audio.findVerseRecitationsByChapter(String(chapterId), String(recitationId), {
      page,
      perPage,
      fields: {
        id: true,
        chapterId: true,
        format: true,
      },
    })) as VerseRecitationResponse;

    const batch = response.audioFiles ?? [];
    console.log(`Fetched ${batch.length} verse recitations for chapter ${chapterId} (page ${page})`);
    allAudioFiles.push(...batch);

    const nextPage = response.pagination?.nextPage ?? null;
    if (batch.length === 0 || nextPage == null) break;
    page = nextPage;
  }

  return allAudioFiles;
}

export async function populateAudio(
  db: Db,
  client: QuranClient,
  opts: {
    recitationId: number;
    perPage: number;
    chapterIds?: number[];
    audioDir: string;
    audioBaseUrl: string;
  }
) {
  const chapterIds = normalizeChapterIds(opts.chapterIds);
  const audioRoot = path.resolve(opts.audioDir);
  mkdirSync(audioRoot, { recursive: true });

  const allRecitations = (await client.resources.findAllRecitations()) as RecitationResource[];
  const selectedRecitation = allRecitations.find((r) => r.id === opts.recitationId);
  if (!selectedRecitation) {
    throw new Error(`Recitation ${opts.recitationId} is not available from resources.findAllRecitations().`);
  }

  const recitationUpsert = db.prepare(
    `INSERT INTO audio_recitations(id, reciter_name, style, translated_name)
     VALUES(?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       reciter_name=excluded.reciter_name,
       style=excluded.style,
       translated_name=excluded.translated_name`
  );

  withTransaction(db, () => {
    for (const recitation of allRecitations) {
      if (!recitation.id) continue;
      recitationUpsert.run(
        recitation.id,
        recitation.reciterName ?? null,
        recitation.style ?? null,
        recitation.translatedName?.name ?? null
      );
    }
  });

  console.log(
    `audio: using recitation ${opts.recitationId} - ${selectedRecitation.reciterName ?? selectedRecitation.translatedName?.name ?? 'unknown'}${selectedRecitation.style ? ` (${selectedRecitation.style})` : ''}`
  );

  const lookupVerseId = db.prepare('SELECT id FROM verses WHERE verse_key = ?');
  const upsertAudio = db.prepare(
    `INSERT INTO verse_audio(verse_id, recitation_id, verse_key, source_url, local_path, format)
     VALUES(?, ?, ?, ?, ?, ?)
     ON CONFLICT(verse_id, recitation_id) DO UPDATE SET
       verse_key=excluded.verse_key,
       source_url=excluded.source_url,
       local_path=COALESCE(excluded.local_path, verse_audio.local_path),
       format=COALESCE(excluded.format, verse_audio.format)`
  );

  let total = 0;
  let downloaded = 0;
  let reused = 0;
  let failedDownloads = 0;
  let missingVerses = 0;

  for (const chapterId of chapterIds) {
    const audioFiles = await fetchAllVerseRecitationsByChapter(client, chapterId, opts.recitationId, opts.perPage);
    const rows: VerseAudioRow[] = [];

    for (const item of audioFiles) {
      let verseRef: { id?: number } | undefined;
      try {
        verseRef = lookupVerseId.get(item.verseKey) as { id?: number } | undefined;
      } catch {
        verseRef = undefined;
      }

      if (!verseRef?.id) {
        missingVerses++;
        continue;
      }

      const sourceUrl = toAbsoluteAudioUrl(item.url, opts.audioBaseUrl);
      const { surah, ayah } = parseVerseKey(item.verseKey);
      const target = resolveAudioTarget(audioRoot, opts.recitationId, surah, ayah, item.format);

      let localPath: string | null = target.relativePath;
      try {
        const result = await downloadAudioIfMissing(sourceUrl, target.absolutePath);
        if (result.downloaded) {
          downloaded++;
        } else {
          reused++;
        }
      } catch (err) {
        localPath = null;
        failedDownloads++;
        const message = err instanceof Error ? err.message : String(err);
        console.warn(`audio download failed for ${item.verseKey}: ${message}`);
      }

      rows.push({
        verseId: verseRef.id,
        verseKey: item.verseKey,
        sourceUrl,
        localPath,
        format: item.format ?? null,
      });
    }

    const inserted = withTransaction(db, () => {
      let count = 0;
      for (const row of rows) {
        upsertAudio.run(
          row.verseId,
          opts.recitationId,
          row.verseKey,
          row.sourceUrl,
          row.localPath,
          row.format
        );
        count++;
      }
      return count;
    });

    total += inserted;
  }

  return {
    count: total,
    downloaded,
    reused,
    failedDownloads,
    missingVerses,
  };
}
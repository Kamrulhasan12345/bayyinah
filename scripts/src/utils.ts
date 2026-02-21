import path from 'node:path';

export type LogFn = (message: string) => void;

export function chunkArray<T>(items: readonly T[], chunkSize: number): T[][] {
  if (!Number.isFinite(chunkSize) || chunkSize <= 0) {
    throw new Error(`Invalid chunkSize: ${chunkSize}`);
  }

  const result: T[][] = [];
  for (let i = 0; i < items.length; i += chunkSize) {
    result.push(items.slice(i, i + chunkSize));
  }
  return result;
}

export function parseNumberList(value: string | undefined): number[] {
  if (!value) return [];
  return value
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean)
    .map((v) => Number(v))
    .filter((n) => Number.isFinite(n));
}

export function toDbPath(input: string | undefined): string {
  const defaultPath = path.join('..', 'data', 'quran.db');
  return (input && input.trim().length > 0 ? input.trim() : defaultPath).replaceAll('\\', '/');
}

export function formatDurationMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const seconds = ms / 1000;
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  const minutes = Math.floor(seconds / 60);
  const rem = seconds - minutes * 60;
  return `${minutes}m ${rem.toFixed(0)}s`;
}

export function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`Missing required env var: ${name}`);
  return value;
}

export function parseArgs(argv: string[]) {
  const args: Record<string, string | boolean> = {};
  for (let i = 0; i < argv.length; i++) {
    const token = argv[i];
    if (!token.startsWith('--')) continue;
    const [rawKey, maybeValue] = token.split('=', 2);
    const key = rawKey.slice(2);
    if (maybeValue !== undefined) {
      args[key] = maybeValue;
      continue;
    }
    const next = argv[i + 1];
    if (next && !next.startsWith('--')) {
      args[key] = next;
      i++;
      continue;
    }
    args[key] = true;
  }
  return args;
}

export async function mapWithConcurrency<T, R>(
  items: readonly T[],
  concurrency: number,
  mapper: (item: T, index: number) => Promise<R>
): Promise<R[]> {
  const sizeRaw = Number(concurrency);
  const size = Number.isFinite(sizeRaw) && sizeRaw > 0 ? Math.floor(sizeRaw) : 1;

  const results: R[] = new Array(items.length);
  let nextIndex = 0;

  const workers = Array.from({ length: Math.min(size, items.length) }, async () => {
    for (; ;) {
      const current = nextIndex++;
      if (current >= items.length) return;
      results[current] = await mapper(items[current], current);
    }
  });

  await Promise.all(workers);
  return results;
}

function decodeHtmlEntities(input: string): string {
  // Minimal decoder for common entities + numeric entities.
  return input
    .replaceAll('&nbsp;', ' ')
    .replaceAll('&amp;', '&')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&quot;', '"')
    .replaceAll('&#39;', "'")
    .replace(/&#x([0-9a-fA-F]+);/g, (_m, hex) => {
      const cp = Number.parseInt(hex, 16);
      return Number.isFinite(cp) ? String.fromCodePoint(cp) : '';
    })
    .replace(/&#(\d+);/g, (_m, dec) => {
      const cp = Number.parseInt(dec, 10);
      return Number.isFinite(cp) ? String.fromCodePoint(cp) : '';
    });
}

export function sanitizeTranslationText(input: string | null | undefined): string | null {
  if (input == null) return null;
  if (input.length === 0) return '';

  let text = input;

  // Remove footnote markers entirely (including the number inside).
  text = text.replace(/<sup\b[^>]*>[\s\S]*?<\/sup>/gi, '');

  // Treat some layout tags as spaces/newlines before stripping tags.
  text = text.replace(/<br\s*\/?>/gi, '\n');
  text = text.replace(/<\/?p\b[^>]*>/gi, '\n');
  text = text.replace(/<\/?div\b[^>]*>/gi, '\n');

  // Strip all remaining tags.
  text = text.replace(/<[^>]+>/g, '');

  // Decode HTML entities.
  text = decodeHtmlEntities(text);

  // Normalize whitespace.
  text = text
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[ \t\f\v]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim();

  return text;
}

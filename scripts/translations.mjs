import { spawnSync } from 'node:child_process';
import { readFile, readdir, mkdir, mkdtemp, copyFile, writeFile, rename } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const root = fileURLToPath(new URL('../', import.meta.url));
const languageDirectory = path.join(root, 'src/main/resources/assets/played/lang');
const cli = path.join(root, 'node_modules/@tolgee/cli/dist/cli.js');
const supportedLanguages = JSON.parse(readFileSync(new URL('./minecraft-languages.json', import.meta.url), 'utf8')).languages;

export function minecraftLocale(tag) {
  const normalized = tag.replaceAll('-', '_').toLowerCase();
  const aliases = {
    en: 'en_us', de: 'de_de', fr: 'fr_fr', es: 'es_es', it: 'it_it', pt: 'pt_pt',
    ru: 'ru_ru', uk: 'uk_ua', ja: 'ja_jp', ko: 'ko_kr', zh: 'zh_cn',
    zh_hans: 'zh_cn', zh_hant: 'zh_tw', pl: 'pl_pl', nl: 'nl_nl', tr: 'tr_tr',
    cs: 'cs_cz', sk: 'sk_sk', sv: 'sv_se', da: 'da_dk', fi: 'fi_fi',
    nb: 'no_no', no: 'no_no', hu: 'hu_hu', ro: 'ro_ro', bg: 'bg_bg'
  };
  const locale = aliases[normalized] ?? normalized;
  if (!Object.hasOwn(supportedLanguages, locale)) {
    throw new Error(`Unsupported Minecraft locale: ${tag}. Use its Minecraft language code in Tolgee or extend minecraftLocale().`);
  }
  return locale;
}

export function flatStrings(value) {
  if (!value || Array.isArray(value) || typeof value !== 'object'
      || Object.values(value).some(text => typeof text !== 'string')) {
    throw new Error('Expected flat JSON with string values; nested JSON cannot be used by Minecraft.');
  }
  return value;
}

function signature(text) {
  let nextArgument = 0;
  const tokens = [...text.matchAll(/%%|%(?:([1-9]\d*)\$)?([sd])|\{(?:time|session|current|total)\}|<[^<>]+>/g)]
    .map(match => match[2] ? `%${match[1] ?? ++nextArgument}$${match[2]}` : match[0]);
  return tokens.sort().join('\n');
}

export function validateTranslation(source, translated) {
  flatStrings(translated);
  const result = {};
  for (const [key, original] of Object.entries(source)) {
    const value = translated[key];
    // Missing/empty strings stay absent so Minecraft uses the English fallback.
    if (value === undefined || (value.trim() === '' && original.trim() !== '')) continue;
    if (signature(original) !== signature(value)) {
      throw new Error(`Placeholders or MiniMessage tags changed in ${key}. No translations were installed.`);
    }
    result[key] = value;
  }
  return result;
}

function runCli(args) {
  const child = spawnSync(process.execPath, [cli, ...args], { cwd: root, stdio: 'inherit' });
  if (child.error) throw child.error;
  if (child.status !== 0) throw new Error('Tolgee command failed. Authenticate with npm run translations:login.');
}

async function main(command) {
  if (command === 'login') { runCli(['login']); return; }
  if (command === 'push-languages') {
    const { ensureTolgeeLanguages } = await import('./tolgee-languages.mjs');
    await ensureTolgeeLanguages();
    return;
  }
  const source = flatStrings(JSON.parse(await readFile(path.join(languageDirectory, 'en_us.json'), 'utf8')));
  if (command === 'push') {
    runCli(['push']);
    console.log(`Uploaded ${Object.keys(source).length} English source strings to project 36408.`);
    return;
  }
  if (command !== 'pull') throw new Error('Usage: translations.mjs login|push|pull|push-languages');

  const stagingRoot = path.join(root, 'build/tolgee');
  await mkdir(stagingRoot, { recursive: true });
  const staging = await mkdtemp(path.join(stagingRoot, 'pull-'));
  runCli(['pull', '--path', staging]);
  const incoming = [];
  const destinations = new Set();
  for (const file of await readdir(staging, { withFileTypes: true })) {
    if (!file.isFile() || !file.name.endsWith('.json')) throw new Error(`Unexpected Tolgee export: ${file.name}`);
    const locale = minecraftLocale(file.name.slice(0, -5));
    if (locale === 'en_us') continue; // English is maintained locally, never overwritten by pull.
    if (destinations.has(locale)) throw new Error(`Multiple Tolgee languages map to ${locale}. No translations were installed.`);
    destinations.add(locale);
    const translated = flatStrings(JSON.parse(await readFile(path.join(staging, file.name), 'utf8')));
    const valid = validateTranslation(source, translated);
    if (Object.keys(valid).length) incoming.push({ locale, text: `${JSON.stringify(valid, null, 2)}\n` });
  }

  // Validate the complete export before replacing any resource files.
  const backupDirectory = path.join(staging, 'previous');
  await mkdir(backupDirectory, { recursive: true });
  for (const { locale, text } of incoming) {
    const destination = path.join(languageDirectory, `${locale}.json`);
    try { await copyFile(destination, path.join(backupDirectory, `${locale}.json`)); }
    catch (error) { if (error.code !== 'ENOENT') throw error; }
    const temporary = `${destination}.tmp`;
    await writeFile(temporary, text, 'utf8');
    await rename(temporary, destination);
    console.log(`Updated ${locale}.json`);
  }
  console.log(incoming.length
    ? `Installed ${incoming.length} language(s). Previous files: ${path.relative(root, backupDirectory)}`
    : 'No target translations available yet. English and existing language files were preserved.');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv[2]).catch(error => { console.error(error.message); process.exitCode = 1; });
}

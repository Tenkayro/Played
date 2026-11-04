import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const manifest = JSON.parse(await readFile(new URL('./minecraft-languages.json', import.meta.url), 'utf8'));
const resources = path.join(root, 'src/main/resources/assets/played/lang');
const templates = path.join(root, 'build/tolgee-language-templates');
const source = JSON.parse(await readFile(path.join(resources, 'en_us.json'), 'utf8'));
const emptyStrings = Object.fromEntries(Object.keys(source).map(key => [key, '']));
await mkdir(templates, { recursive: true });

let created = 0;
const files = [];
for (const locale of Object.keys(manifest.languages)) {
  if (locale === 'en_us') continue;
  try {
    // Empty resource objects preserve Minecraft's English fallback. Never overwrite existing translations.
    await writeFile(path.join(resources, `${locale}.json`), '{}\n', { encoding: 'utf8', flag: 'wx' });
    created++;
  } catch (error) {
    if (error.code !== 'EEXIST') throw error;
  }
  await writeFile(path.join(templates, `${locale}.json`), `${JSON.stringify(emptyStrings, null, 2)}\n`, 'utf8');
  files.push({ path: `./${locale}.json`, language: locale });
}

// This separate import adds empty target languages without replacing existing remote translations.
const config = {
  projectId: 36408,
  format: 'JSON_TOLGEE',
  push: { files, forceMode: 'KEEP', convertPlaceholdersToIcu: false, removeOtherKeys: false }
};
await writeFile(path.join(templates, '.tolgeerc.json'), `${JSON.stringify(config, null, 2)}\n`, 'utf8');
console.log(`Minecraft ${manifest.minecraftVersion}: ${Object.keys(manifest.languages).length} supported languages.`);
console.log(`Created ${created} missing resource files; existing translations preserved.`);
console.log(`Prepared ${files.length} upload templates in build/tolgee-language-templates.`);

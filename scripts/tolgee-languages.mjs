import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { getStoredCredentials } from '../node_modules/@tolgee/cli/dist/config/credentials.js';
import { createApiClient } from '../node_modules/@tolgee/cli/dist/client/ApiClient.js';
import { createOAuthSessionHandle } from '../node_modules/@tolgee/cli/dist/oauth/session.js';

const apiUrl = new URL('https://app.tolgee.io/');
const projectId = 36408;

export async function ensureTolgeeLanguages() {
  const manifest = JSON.parse(await readFile(new URL('./minecraft-languages.json', import.meta.url), 'utf8'));
  const stored = process.env.TOLGEE_API_KEY
    ? { type: 'apiKey', key: process.env.TOLGEE_API_KEY }
    : await getStoredCredentials(apiUrl, projectId);
  if (!stored) throw new Error('Log in first: npm run translations:login');
  const credentials = stored.type === 'oauth'
    ? { session: await createOAuthSessionHandle(apiUrl, stored.session) }
    : { apiKey: stored.key };
  const client = createApiClient({ baseUrl: apiUrl.href, projectId, ...credentials });

  function checked(result) {
    if (result.error || !result.response.ok) {
      if (result.response.status === 403) throw new Error('Tolgee requires languages.edit permission. Use a project API key with that permission via TOLGEE_API_KEY.');
      if (result.response.status === 401) throw new Error('Tolgee login expired or key revoked. Run npm run translations:login.');
      throw new Error(`Tolgee language request failed (HTTP ${result.response.status}, ${result.error?.code ?? 'unknown error'}).`);
    }
    return result.data;
  }

  async function list() {
    const result = [];
    for (let page = 0; ; page++) {
      const data = checked(await client.GET('/v2/projects/{projectId}/languages', {
        params: { path: { projectId }, query: { page, size: 250 } }
      }));
      const languages = data._embedded?.languages ?? data._embedded?.languageModels;
      if (!Array.isArray(languages)) {
        if (data.page?.totalElements === 0) return result;
        throw new Error('Unexpected Tolgee language list response.');
      }
      result.push(...languages);
      if (!data.page || page + 1 >= data.page.totalPages) return result;
    }
  }

  const normalize = tag => tag.replaceAll('-', '_').toLowerCase();
  const before = await list();
  const existing = new Set(before.map(language => normalize(language.tag)));
  const missing = Object.entries(manifest.languages).filter(([tag]) => !existing.has(tag));
  console.log(`Tolgee project ${projectId}: ${before.length} existing languages, ${missing.length} Minecraft languages to add.`);
  let created = 0;
  for (const [tag, language] of missing) {
    const body = { tag, name: `${language.name} (${language.region})`, originalName: language.name };
    const region = tag.split('_').at(-1);
    if (/^[a-z]{2}$/.test(region)) {
      const regionName = new Intl.DisplayNames(['en'], { type: 'region' }).of(region.toUpperCase());
      if (regionName !== region.toUpperCase()) body.flagEmoji = [...region.toUpperCase()]
        .map(letter => String.fromCodePoint(127397 + letter.charCodeAt(0))).join('');
    }
    checked(await client.POST('/v2/projects/{projectId}/languages', { params: { path: { projectId } }, body }));
    created++;
    if (created % 20 === 0) console.log(`Added ${created}/${missing.length} languages...`);
  }
  const after = await list();
  const tags = new Set(after.map(language => normalize(language.tag)));
  const absent = Object.keys(manifest.languages).filter(tag => !tags.has(tag));
  if (absent.length) throw new Error(`Still missing in Tolgee: ${absent.join(', ')}`);
  console.log(`Verified: all ${Object.keys(manifest.languages).length} Minecraft languages exist in Tolgee. Added ${created}; existing languages and translations unchanged.`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  ensureTolgeeLanguages().catch(error => { console.error(error.message); process.exitCode = 1; });
}

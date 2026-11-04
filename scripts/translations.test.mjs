import test from 'node:test';
import assert from 'node:assert/strict';
import { minecraftLocale, flatStrings, validateTranslation } from './translations.mjs';

test('maps Tolgee locale tags to safe Minecraft filenames', () => {
  assert.equal(minecraftLocale('de-DE'), 'de_de');
  assert.equal(minecraftLocale('de'), 'de_de');
  assert.equal(minecraftLocale('zh-Hant'), 'zh_tw');
  assert.equal(minecraftLocale('bar'), 'bar');
  assert.equal(minecraftLocale('be-Latn'), 'be_latn');
  assert.equal(minecraftLocale('sah_sah'), 'sah_sah');
  assert.throws(() => minecraftLocale('../outside'));
});

test('rejects nested exports and preserves flat dotted keys', () => {
  assert.throws(() => flatStrings({ played: { title: 'Played' } }));
  assert.deepEqual(flatStrings({ 'played.name': 'Played' }), { 'played.name': 'Played' });
});

test('preserves indexed placeholders, percent escapes, and MiniMessage tags', () => {
  const source = { title: '%s: %s', scale: '%s%%', hud: '<gold>{time}</gold>' };
  assert.deepEqual(validateTranslation(source, { title: '%2$s — %1$s', scale: '%s%%', hud: '<gold>{time}</gold>' }),
    { title: '%2$s — %1$s', scale: '%s%%', hud: '<gold>{time}</gold>' });
  assert.throws(() => validateTranslation(source, { title: '%s' }));
  assert.throws(() => validateTranslation(source, { hud: '<gold>{current}</gold>' }));
});

test('omits untranslated and obsolete keys for Minecraft fallback', () => {
  assert.deepEqual(validateTranslation({ title: 'Statistics', done: 'Done' }, { title: '', done: 'Fertig', old: 'Old' }), { done: 'Fertig' });
});

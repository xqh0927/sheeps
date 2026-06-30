const test = require('node:test');
const assert = require('node:assert/strict');

const {
  parseReleaseVersionCode,
  findApkAsset,
  isForceUpdateRelease,
  mapGitHubReleaseToUpdate,
  checkApkExists,
  clearApkCache,
  getDatabaseAppUpdate,
} = require('../.tmp-test/index.js');

test('parses numeric release tags as Android version codes', () => {
  assert.equal(parseReleaseVersionCode('v2'), 20000);
  assert.equal(parseReleaseVersionCode('release-12'), 120000);
});

test('parses semantic release tags without treating v1.0.0 as newer than code 1', () => {
  assert.equal(parseReleaseVersionCode('v1.0.0'), 10000);
  assert.equal(parseReleaseVersionCode('v1.2.3'), 10203);
});

test('selects the apk asset from a GitHub release', () => {
  const apk = findApkAsset([
    { name: 'notes.txt', browser_download_url: 'https://example.com/notes.txt' },
    { name: 'sheeps_2.apk', browser_download_url: 'https://example.com/sheeps_2.apk' },
  ]);

  assert.equal(apk.browser_download_url, 'https://example.com/sheeps_2.apk');
});

test('detects force update markers in release notes', () => {
  assert.equal(isForceUpdateRelease('bug fixes\n[force_update]'), true);
  assert.equal(isForceUpdateRelease('force_update=true'), true);
  assert.equal(isForceUpdateRelease('regular release'), false);
});

test('maps a newer GitHub release to the app update response', () => {
  const update = mapGitHubReleaseToUpdate({
    tag_name: 'v2',
    name: '1.1.0',
    body: '新增关卡和性能优化',
    assets: [
      { name: 'sheeps_1.1.0.apk', browser_download_url: 'https://github.com/xqh0927/sheeps-releases/releases/download/v2/sheeps_1.1.0.apk' },
    ],
  }, 1);

  assert.deepEqual(update, {
    has_update: true,
    version_name: '1.1.0',
    apk_url: 'https://github.com/xqh0927/sheeps-releases/releases/download/v2/sheeps_1.1.0.apk',
    update_log: '新增关卡和性能优化',
    force_update: false,
  });
});

test('does not report an update when the latest release is not newer', () => {
  assert.deepEqual(mapGitHubReleaseToUpdate({
    tag_name: 'v1.0.0',
    name: '1.0.0',
    assets: [
      { name: 'sheeps_1.0.0.apk', browser_download_url: 'https://example.com/sheeps.apk' },
    ],
  }, 10000), { has_update: false });
});

test('checkApkExists fetches URL status and caches result', async () => {
  let callCount = 0;
  const originalFetch = globalThis.fetch;
  
  globalThis.fetch = async (url) => {
    callCount++;
    if (url === 'https://example.com/exist.apk') {
      return { status: 200 };
    }
    return { status: 404 };
  };

  if (clearApkCache) clearApkCache();

  // First request: should call fetch and return true
  const ok = await checkApkExists('https://example.com/exist.apk');
  assert.equal(ok, true);
  assert.equal(callCount, 1);

  // Second request: should hit cache and not fetch again
  const okCached = await checkApkExists('https://example.com/exist.apk');
  assert.equal(okCached, true);
  assert.equal(callCount, 1);

  // Request a non-existent one: should return false
  const notOk = await checkApkExists('https://example.com/404.apk');
  assert.equal(notOk, false);
  assert.equal(callCount, 2);

  globalThis.fetch = originalFetch;
});

test('getDatabaseAppUpdate filters out unbuilt releases and returns the latest available', async () => {
  if (clearApkCache) clearApkCache();

  // Mock D1 database
  const mockDb = {
    prepare(query) {
      return {
        bind(currentCode) {
          return {
            async all() {
              // Mock returning two higher versions: v3.apk (unbuilt, 404), v2.apk (built, 200)
              return {
                results: [
                  { version_code: 3, version_name: '1.0.2', apk_url: 'https://example.com/v3.apk', update_log: 'v3更新', is_force_update: 0 },
                  { version_code: 2, version_name: '1.0.1', apk_url: 'https://example.com/v2.apk', update_log: 'v2更新', is_force_update: 0 }
                ]
              };
            }
          };
        }
      };
    }
  };

  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url) => {
    if (url === 'https://example.com/v2.apk') {
      return { status: 200 };
    }
    return { status: 404 }; // v3.apk 404
  };

  const env = { DB: mockDb };
  // Current app version is 1
  const updateResult = await getDatabaseAppUpdate(env, 1);
  
  assert.equal(updateResult.has_update, true);
  assert.equal(updateResult.version_name, '1.0.1'); // Should return v2
  assert.equal(updateResult.apk_url, 'https://example.com/v2.apk');
  assert.equal(updateResult.update_log, 'v2更新');

  globalThis.fetch = originalFetch;
});



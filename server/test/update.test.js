const test = require('node:test');
const assert = require('node:assert/strict');

const {
  parseReleaseVersionCode,
  findApkAsset,
  isForceUpdateRelease,
  mapGitHubReleaseToUpdate,
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

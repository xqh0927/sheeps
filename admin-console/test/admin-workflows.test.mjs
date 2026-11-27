import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { chromium } from 'playwright';
import { createServer } from 'vite';

let browser, server, origin;
before(async () => {
  server = await createServer({ server: { host: '127.0.0.1', port: 0 }, logLevel: 'error' });
  await server.listen();
  origin = 'http://127.0.0.1:' + server.httpServer.address().port;
  browser = await chromium.launch({ executablePath: process.env.CHROME_BIN });
});
after(async () => {
  await browser?.close();
  await server?.close();
});

async function openPage(t, routeName, respond) {
  const page = await browser.newPage({ timezoneId: 'Asia/Shanghai', viewport: { width: 1440, height: 900 } });
  t.after(() => page.close());
  await page.addInitScript(() => localStorage.setItem('miadmin_auth', JSON.stringify({
    token: 'test-access', refreshToken: 'test-refresh', user: { id: 'admin', username: 'Admin', role: 'super' },
  })));
  await page.route('**/api/admin/**', async route => {
    const response = await respond(new URL(route.request().url()), route.request().method());
    if (response === null) await route.abort();
    else await route.fulfill({ json: response, headers: { 'Access-Control-Allow-Origin': '*' } });
  });
  await page.goto(origin + routeName);
  return page;
}

const result = (list) => ({ success: true, list, total: list.length });
const user = (id) => ({ id, username: id, phone: id, role: 'user', points: 0, created_at: 1 });
const accountRow = (page, id) => page.getByRole('row').filter({ hasText: id });

test('a failed inventory read clears the previous player and requires a successful retry before saving', async t => {
  let failB = true;
  const writes = [];
  const page = await openPage(t, '/users', (url, method) => {
    if (url.pathname.endsWith('/users')) return result([user('player-A'), user('player-B')]);
    if (url.pathname.endsWith('/shop-items')) return result([]);
    if (url.pathname.endsWith('/items')) {
      if (method === 'POST') { writes.push(url.pathname); return { success: true }; }
      if (url.pathname.includes('/player-B/') && failB) { failB = false; return null; }
      return result([{ item_type: 'UNDO', count: url.pathname.includes('/player-A/') ? 8 : 0 }]);
    }
    throw new Error('Unexpected fixture request ' + url.pathname);
  });
  const dialog = page.getByRole('dialog');
  await accountRow(page, 'player-A').getByTitle('道具与皮肤').click();
  await page.waitForFunction(() => document.querySelector('[role=dialog] input[type=number]')?.value === '8');
  await dialog.getByRole('button', { name: '取消' }).click();
  await accountRow(page, 'player-B').getByTitle('道具与皮肤').click();
  const retry = dialog.getByRole('button', { name: '重试' });
  await retry.waitFor();
  assert.equal(await dialog.getByRole('spinbutton').count(), 0);
  assert.equal(await dialog.getByRole('button', { name: '保存' }).isEnabled(), false);
  await retry.click();
  assert.equal(await dialog.getByRole('spinbutton').first().inputValue(), '0');
  await dialog.getByRole('button', { name: '保存' }).click();
  await dialog.waitFor({ state: 'hidden' });
  assert.deepEqual(writes, ['/api/admin/users/player-B/items']);
});

test('the leaderboard editor displays the existing owner as read-only', async t => {
  const page = await openPage(t, '/leaderboards', url => {
    if (url.pathname.endsWith('/config')) return result([]);
    if (url.pathname.endsWith('/leaderboard')) {
      return result([{ id: 1, user_id: 'p1', username: 'Player One', score: 200, clear_time_ms: 500, level_id: 1 }]);
    }
    throw new Error('Unexpected fixture request ' + url.pathname);
  });
  await accountRow(page, 'Player One').getByRole('button').first().click();
  const owner = page.getByRole('dialog').getByRole('textbox', { name: '用户', exact: true });
  assert.equal(await owner.inputValue(), 'Player One');
  assert.equal(await owner.isEditable(), false);
});

test('two audit date inputs delimit the displayed local day, including its last millisecond', async t => {
  const page = await openPage(t, '/audit-logs', () => result([]));
  await page.getByLabel('起始日期').fill('2026-11-25');
  const bounds = page.waitForRequest(r => new URL(r.url()).searchParams.has('to'));
  await page.getByLabel('结束日期').fill('2026-11-25');
  const params = new URL((await bounds).url()).searchParams;
  assert.equal(Number(params.get('from')), Date.parse('2026-11-25T00:00:00+08:00'));
  assert.equal(Number(params.get('to')), Date.parse('2026-11-26T00:00:00+08:00') - 1);
});

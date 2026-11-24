const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');

test('a fresh database can load the full schema and initial game content', () => {
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(readFileSync(path.resolve(__dirname, '../schema.sql'), 'utf8'));
    for (const table of ['notice', 'shop_items', 'task']) {
      assert.ok(db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get().n > 0, table);
    }
  } finally {
    db.close();
  }
});

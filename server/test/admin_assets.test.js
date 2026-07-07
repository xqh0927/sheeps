const test = require('node:test');
const assert = require('node:assert/strict');

// Mock Env
const mockEnv = {
  DB: {
    prepare: (sql) => {
      return {
        bind: (...args) => {
          return {
            first: async () => {
              if (sql.includes('SELECT is_banned FROM users')) {
                return { is_banned: 0 };
              }
              return null;
            },
            all: async () => {
              if (sql.includes('SELECT item_type, count FROM user_items')) {
                return { results: [{ item_type: 'UNDO', count: 5 }] };
              }
              return { results: [] };
            },
            run: async () => {
              return { success: true };
            }
          };
        }
      };
    },
    batch: async (statements) => {
      return { success: true };
    }
  }
};

// Import compiled code
const { handleAdminRoutes } = require('../.tmp-test/handlers/admin.js');
const { generateJWT } = require('../.tmp-test/crypto.js');

test('GET /api/admin/users/:id/items returns user items successfully', async () => {
  const token = await generateJWT({ userId: 'user_123', phone: '13800000000', role: 'super', type: 'access', exp: Date.now() + 100000 });
  const req = {
    method: 'GET',
    headers: {
      get: (headerName) => {
        if (headerName.toLowerCase() === 'authorization') {
          return `Bearer ${token}`;
        }
        return null;
      }
    },
    url: 'https://example.com/api/admin/users/user_123/items'
  };

  const url = new URL(req.url);
  const response = await handleAdminRoutes(req, mockEnv, url.pathname, url);
  
  // Since we haven't implemented GET /api/admin/users/:id/items in admin.ts yet,
  // this should return 404. Let's assert it fails (returns 404) first.
  assert.equal(response.status, 404);
});

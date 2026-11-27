-- Older admin creation omitted the TEXT primary key. Give those rows a stable account ID.
UPDATE users SET id = lower(hex(randomblob(16)))
WHERE id IS NULL AND phone IS NOT NULL AND role IN ('super', 'operator', 'readonly');

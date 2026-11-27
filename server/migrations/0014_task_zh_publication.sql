-- Old task edits did not audit Chinese name/description. Publish a historical
-- translation only when its last edit proves exactly which base text it replaced.
CREATE VIEW migration_0014_task_text AS
WITH fields AS (
    SELECT id, 'name' AS field, name AS base FROM task
    UNION ALL
    SELECT id, 'description', description FROM task
)
SELECT f.id, f.field, i.value
FROM fields f
JOIN i18n_strings i ON i.module = 'task' AND i.locale = 'zh'
    AND i.str_key = 'task.' || f.id || '.' || f.field
JOIN admin_audit_log a ON a.action = 'UPDATE_I18N' AND a.target_type = 'i18n_strings'
    AND a.target_id = CAST(i.id AS TEXT)
JOIN admin_audit_changes c ON c.change_id = a.id AND c.field = 'value'
    AND c.old_val = json_quote(f.base) AND c.new_val = json_quote(i.value)
WHERE i.value IS NOT NULL AND i.value <> f.base
    AND NOT EXISTS (SELECT 1 FROM i18n_strings other WHERE other.id <> i.id
        AND other.module = i.module AND other.locale = i.locale AND other.str_key = i.str_key)
    AND NOT EXISTS (SELECT 1 FROM admin_audit_log later WHERE later.id > a.id AND (
        (later.target_type = 'task' AND later.target_id = f.id)
        OR (later.target_type = 'i18n_strings' AND later.target_id = CAST(i.id AS TEXT))
    ));

UPDATE task
SET name = COALESCE((SELECT value FROM migration_0014_task_text WHERE id = task.id AND field = 'name'), name),
    description = COALESCE((SELECT value FROM migration_0014_task_text WHERE id = task.id AND field = 'description'), description)
WHERE id IN (SELECT id FROM migration_0014_task_text);

DROP VIEW migration_0014_task_text;

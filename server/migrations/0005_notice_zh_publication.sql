-- Repair old title/body disagreements only where the last recorded edit proves both
-- the winning value and the previous value. Keep unaudited or ambiguous content intact.
CREATE VIEW migration_0005_notice_edits AS
WITH fields AS (
    SELECT id, 'title' AS field, title AS base FROM notice
    UNION ALL
    SELECT id, 'content', content FROM notice
), pairs AS (
    SELECT f.id, f.field, f.base, i.id AS translation_id, i.value AS translated
    FROM fields f
    JOIN i18n_strings i ON i.module='notice' AND i.locale='zh'
        AND i.str_key='notice.' || f.id || '.' || f.field
    WHERE i.value IS NOT NULL
      AND (SELECT COUNT(*) FROM i18n_strings x WHERE x.module=i.module
           AND x.locale=i.locale AND x.str_key=i.str_key)=1
      AND i.value <> f.base
), edits AS (
    SELECT p.*, 1 AS translation_wins, a.id AS audit_id, c.old_val, c.new_val
    FROM pairs p
    JOIN admin_audit_log a ON a.action='UPDATE_I18N' AND a.target_type='i18n_strings'
        AND a.target_id=CAST(p.translation_id AS TEXT)
    JOIN admin_audit_changes c ON c.change_id=a.id AND c.field='value' AND c.old_val IS NOT c.new_val
    UNION ALL
    SELECT p.*, 0, a.id, c.old_val, c.new_val
    FROM pairs p
    JOIN admin_audit_log a ON a.action='UPDATE_NOTICE' AND a.target_type='notice'
        AND a.target_id=CAST(p.id AS TEXT)
    JOIN admin_audit_changes c ON c.change_id=a.id AND c.field=p.field AND c.old_val IS NOT c.new_val
), ranked AS (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY id, field ORDER BY audit_id DESC) AS rank FROM edits
)
SELECT * FROM ranked WHERE rank=1
    AND old_val=json_quote(CASE WHEN translation_wins THEN base ELSE translated END)
    AND new_val=json_quote(CASE WHEN translation_wins THEN translated ELSE base END);

UPDATE notice
SET title=COALESCE((SELECT translated FROM migration_0005_notice_edits
                    WHERE id=notice.id AND field='title' AND translation_wins), title),
    content=COALESCE((SELECT translated FROM migration_0005_notice_edits
                      WHERE id=notice.id AND field='content' AND translation_wins), content)
WHERE id IN (SELECT id FROM migration_0005_notice_edits WHERE translation_wins);

UPDATE i18n_strings
SET value=(SELECT base FROM migration_0005_notice_edits WHERE translation_id=i18n_strings.id)
WHERE id IN (SELECT translation_id FROM migration_0005_notice_edits WHERE NOT translation_wins);

DROP VIEW migration_0005_notice_edits;

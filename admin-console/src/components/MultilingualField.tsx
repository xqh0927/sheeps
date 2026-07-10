import { Box, TextField, Typography } from '@mui/material';
import { I18N_LOCALES, LOCALE_SUFFIX, LOCALE_LABELS, LocaleCode } from '../constants/locales';

/** 多语言输入组件属性 */
export interface MultilingualFieldProps {
  /** 基础字段名（zh，无后缀），如 'name' / 'description' / 'title' / 'content' / 'update_log' */
  base: string;
  /** 字段中文标签，如 '名称' / '描述' */
  label: string;
  /** 共享表单状态，包含 base / base_en / base_tw / base_ja / base_ko */
  form: Record<string, any>;
  /** 受控更新：回传完整 form（含本次改动的某个语言键） */
  onChange: (next: Record<string, any>) => void;
  /** 新建态传 true → 任一语言为空时该语言输入框标红并给出 helper 提示 */
  required?: boolean;
  /** 是否多行（description / content / update_log 用 textarea） */
  multiline?: boolean;
  /** 禁用（提交中） */
  disabled?: boolean;
}

/**
 * 多语言输入组件：渲染 5 个输入框（zh 在前），受控读写
 * form[base] / form[base+'_en'/_tw/_ja/_ko]。
 *
 * 强制校验仅由 CrudPage 在「新建态」配合 required 触发；
 * 任一语言为空时该语言输入框标红（error + helperText）。
 * 编辑态 CrudPage 不会渲染本组件（退化为单 zh 输入），故此处无需处理编辑逻辑。
 */
export default function MultilingualField({
  base,
  label,
  form,
  onChange,
  required = false,
  multiline = false,
  disabled = false,
}: MultilingualFieldProps) {
  // 缺失语言集合（仅 required 时用于标红）
  const missing: LocaleCode[] = required
    ? I18N_LOCALES.filter((loc) => {
        const key = loc === 'zh' ? base : `${base}${LOCALE_SUFFIX[loc]}`;
        const v = form[key];
        return v === undefined || v === null || String(v).trim() === '';
      })
    : [];

  return (
    <Box sx={{ mt: 1, mb: 1 }}>
      <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
        {label}
        {required ? ' *' : ''}
      </Typography>
      {I18N_LOCALES.map((loc) => {
        const key = loc === 'zh' ? base : `${base}${LOCALE_SUFFIX[loc]}`;
        const value = form[key] ?? '';
        const isMissing = missing.includes(loc);
        return (
          <TextField
            key={loc}
            fullWidth
            margin="dense"
            size="small"
            label={LOCALE_LABELS[loc]}
            value={value}
            multiline={multiline}
            minRows={multiline ? 2 : undefined}
            disabled={disabled}
            error={isMissing}
            helperText={isMissing ? `请填写${LOCALE_LABELS[loc]}` : undefined}
            onChange={(e: any) => onChange({ ...form, [key]: e.target.value })}
          />
        );
      })}
    </Box>
  );
}

import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  CircularProgress,
  IconButton,
} from '@mui/material';
import { Close, CloudUpload } from '@mui/icons-material';
import { getItemIcon, saveItemIcon, uploadImage } from '../api/admin';
import { useFeedback } from './feedback';

interface Props {
  open: boolean;
  /** 道具 item_type，如 'UNDO' */
  itemType: string;
  /** 现有图标 URL（可选，传入则预填） */
  initialUrl?: string | null;
  onClose: () => void;
  onSaved?: () => void;
}

/**
 * 单道具「1 张图标」上传弹窗。
 * 上传拼 R2 key = images/items/{itemType}.png；
 * 保存写 item_icons（真身）并镜像到 shop_items.image_url。
 */
export default function ItemIconManager({ open, itemType, initialUrl, onClose, onSaved }: Props) {
  const { show } = useFeedback();
  // 当前图标 URL（受控），空串表示未上传
  const [url, setUrl] = useState<string>('');
  // uploading：上传中，禁用操作并展示进度
  const [uploading, setUploading] = useState(false);
  // saving：保存提交中，防止重复提交并禁用关闭/保存按钮
  const [saving, setSaving] = useState(false);

  // 打开弹窗时预填图标：优先 initialUrl，否则 getItemIcon 拉后端；
  // 依赖 [open, itemType, initialUrl]；cancelled 标志避免异步回写竞态，清理函数仅置 cancelled=true
  useEffect(() => {
    if (!open) return;
    if (initialUrl) {
      setUrl(initialUrl);
      return;
    }
    let cancelled = false;
    getItemIcon(itemType)
      .then((r) => {
        if (!cancelled && r.icon?.image_url) setUrl(r.icon.image_url);
      })
      .catch(() => {
        /* 忽略：无已传图标 */
      });
    return () => {
      cancelled = true;
    };
  }, [open, itemType, initialUrl]);

  // 上传图标：拼 R2 key = images/items/{itemType}.png → uploadImage → 回填 url
  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      fd.append('key', `images/items/${itemType}.png`);
      const r = await uploadImage(fd);
      setUrl(r.url);
      show('上传成功', 'success');
    } catch (e: any) {
      show(e?.response?.data?.error || e?.message || '上传失败', 'error');
    } finally {
      setUploading(false);
    }
  };

  // 保存图标：校验已上传 → saveItemIcon 写入 item_icons 并镜像到商店封面；
  // 成功回调 onSaved 刷新父列表并关闭
  const handleSave = async () => {
    if (!url) {
      show('请先上传图标', 'warning');
      return;
    }
    setSaving(true);
    try {
      await saveItemIcon(itemType, url);
      show('已保存并同步封面', 'success');
      onSaved?.();
      onClose();
    } catch (e: any) {
      show(e?.response?.data?.error || e?.message || '保存失败', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={() => !saving && onClose()} maxWidth="xs" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span>图标管理 · {itemType}</span>
          <IconButton size="small" onClick={() => !saving && onClose()}>
            <Close fontSize="small" />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Typography variant="caption" color="text.secondary">
          上传 1 张图标，保存后将写入 item_icons 并镜像到商店封面。
        </Typography>
        <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 80,
              height: 80,
              borderRadius: 2,
              bgcolor: 'action.hover',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              overflow: 'hidden',
            }}
          >
            {url ? (
              <img src={url} alt="item icon" style={{ width: 80, height: 80, objectFit: 'contain' }} />
            ) : uploading ? (
              <CircularProgress size={24} />
            ) : (
              <Typography variant="caption" color="text.secondary">
                未上传
              </Typography>
            )}
          </Box>
          <Button variant="outlined" component="label" disabled={uploading}>
            {uploading ? '上传中' : '上传图标'}
            <input
              hidden
              type="file"
              accept="image/png,image/webp,image/jpeg"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleUpload(file);
                e.target.value = '';
              }}
            />
          </Button>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button startIcon={<CloudUpload />} variant="contained" onClick={handleSave} disabled={saving || uploading}>
          {saving ? <CircularProgress size={18} color="inherit" /> : '保存'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

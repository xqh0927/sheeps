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
import { getSkinTiles, saveSkinTiles, uploadImage } from '../api/admin';
import { useFeedback } from './feedback';

interface Props {
  open: boolean;
  /** 皮肤渲染键（小写），如 'henan' */
  skinType: string;
  /** 对应 shop_items 的 item_type，用于提示，可不传 */
  skinItemType?: string;
  /** 现有 12 张卡面 URL（可选，传入则预填） */
  initialTiles?: (string | null)[] | null;
  onClose: () => void;
  onSaved?: () => void;
}

const TILE_COUNT = 12;

/**
 * 单皮肤「12 张卡面」批量上传弹窗。
 * 每张卡面上传时拼 R2 key = images/skins/{skinType}/{index}.png；
 * 保存时批量写 skin_tiles，第 1 张自动同步到 shop_items.image_url（封面，后端处理）。
 */
export default function SkinTilesManager({ open, skinType, skinItemType, initialTiles, onClose, onSaved }: Props) {
  const { show } = useFeedback();
  // 12 张卡面 URL 数组（index 对应 tile_index-1），null 表示未上传
  const [tiles, setTiles] = useState<(string | null)[]>(new Array(TILE_COUNT).fill(null));
  // 正在上传的卡面下标（null 表示无上传进行中），用于禁用其它上传并显示进度
  const [uploading, setUploading] = useState<number | null>(null);
  // saving：保存提交中，防止重复提交并禁用关闭/保存按钮
  const [saving, setSaving] = useState(false);

  // 打开弹窗时预填卡面：优先用 initialTiles，否则 getSkinTiles 拉后端；
  // 依赖 [open, skinType, initialTiles]；通过 cancelled 标志避免异步回写竞态（组件卸载/重开时丢弃旧结果）
  // 清理函数仅置 cancelled=true，无定时器/订阅泄漏
  // 打开时预填：优先 initialTiles，否则拉后端
  useEffect(() => {
    if (!open) return;
    if (initialTiles && initialTiles.length > 0) {
      const arr = new Array(TILE_COUNT).fill(null);
      initialTiles.slice(0, TILE_COUNT).forEach((u, i) => (arr[i] = u));
      setTiles(arr);
      return;
    }
    let cancelled = false;
    getSkinTiles(skinType)
      .then((r) => {
        if (cancelled) return;
        const arr = new Array(TILE_COUNT).fill(null);
        (r.tiles || []).forEach((t) => {
          const idx = t.tile_index - 1;
          if (idx >= 0 && idx < TILE_COUNT) arr[idx] = t.image_url;
        });
        setTiles(arr);
      })
      .catch(() => {
        /* 忽略：无已传卡面 */
      });
    return () => {
      cancelled = true;
    };
  }, [open, skinType, initialTiles]);

  // 上传单张卡面：拼 R2 key = images/skins/{skinType}/{index+1}.png → uploadImage → 回填对应 tile
  // 失败经 e.response.data.error / message 提示
  const handleUpload = async (index: number, file: File) => {
    setUploading(index);
    try {
      const fd = new FormData();
      fd.append('file', file);
      fd.append('key', `images/skins/${skinType}/${index + 1}.png`);
      const r = await uploadImage(fd);
      setTiles((prev) => {
        const next = [...prev];
        next[index] = r.url;
        return next;
      });
      show('上传成功', 'success');
    } catch (e: any) {
      show(e?.response?.data?.error || e?.message || '上传失败', 'error');
    } finally {
      setUploading(null);
    }
  };

  // 保存全部卡面：过滤掉未上传项，拼 [{tile_index, image_url}] → saveSkinTiles
  // 后端自动将第 1 张同步为商店封面；成功回调 onSaved 刷新父列表并关闭
  const handleSave = async () => {
    setSaving(true);
    try {
      const payload = tiles
        .map((url, i) => ({ tile_index: i + 1, image_url: url }))
        .filter((t) => t.image_url);
      if (payload.length === 0) {
        show('请至少上传一张卡面', 'warning');
        return;
      }
      await saveSkinTiles(skinType, payload);
      show('已保存（第 1 张已同步为封面）', 'success');
      onSaved?.();
      onClose();
    } catch (e: any) {
      show(e?.response?.data?.error || e?.message || '保存失败', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={() => !saving && onClose()} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span>
            卡面管理 · {skinType}
            {skinItemType ? `（${skinItemType}）` : ''}
          </span>
          <IconButton size="small" onClick={() => !saving && onClose()}>
            <Close fontSize="small" />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Typography variant="caption" color="text.secondary">
          共 12 张（tile 1–12）；第 1 张将自动同步为该皮肤的商店封面。建议上传满 12 张。
        </Typography>
        <Box
          sx={{
            mt: 2,
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: 2,
          }}
        >
          {tiles.map((url, i) => (
            <Box
              key={i}
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 2,
                p: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 1,
              }}
            >
              <Typography variant="caption" color="text.secondary">
                tile {i + 1}
              </Typography>
              <Box
                sx={{
                  width: 64,
                  height: 64,
                  borderRadius: 1,
                  bgcolor: 'action.hover',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  overflow: 'hidden',
                }}
              >
                {url ? (
                  <img src={url} alt={`tile ${i + 1}`} style={{ width: 64, height: 64, objectFit: 'cover' }} />
                ) : uploading === i ? (
                  <CircularProgress size={20} />
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    未上传
                  </Typography>
                )}
              </Box>
              <Button variant="outlined" size="small" component="label" disabled={uploading !== null}>
                {uploading === i ? '上传中' : '上传'}
                <input
                  hidden
                  type="file"
                  accept="image/png,image/webp,image/jpeg"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleUpload(i, file);
                    e.target.value = '';
                  }}
                />
              </Button>
            </Box>
          ))}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button startIcon={<CloudUpload />} variant="contained" onClick={handleSave} disabled={saving || uploading !== null}>
          {saving ? <CircularProgress size={18} color="inherit" /> : '保存全部'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

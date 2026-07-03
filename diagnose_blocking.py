#!/usr/bin/env python3
"""
诊断脚本：模拟服务端关卡生成和客户端遮挡检测
用于验证哪些 tile 应该被标记为 BLOCKED
"""

import math
import random
from dataclasses import dataclass
from typing import List, Dict, Tuple

@dataclass
class Tile:
    id: str
    type: int
    x: float
    y: float
    z: int
    state: str = "NORMAL"  # NORMAL, BLOCKED, IN_SLOT, MOVED_OUT
    is_blind: bool = False
    sealed_count: int = 0

TILE = 48.0
SPACING = 46.0
OVERLAP_MARGIN = 0.25

def blocking(a: Tile, b: Tile) -> bool:
    """b 遮挡 a 吗？要求 b.z > a.z 且在 x、y 轴都重叠 > OVERLAP_MARGIN"""
    if b.z <= a.z:
        return False
    ox = TILE - abs(b.x - a.x) * SPACING
    oy = TILE - abs(b.y - a.y) * SPACING
    return ox > OVERLAP_MARGIN and oy > OVERLAP_MARGIN

def build_grid(board: List[Tile]) -> Dict[Tuple[int, int], List[Tile]]:
    grid: Dict[Tuple[int, int], List[Tile]] = {}
    for t in board:
        if t.state not in ("NORMAL", "BLOCKED"):
            continue
        bx = math.floor(t.x)
        by = math.floor(t.y)
        grid.setdefault((bx, by), []).append(t)
    return grid

def is_blocked_grid(tile: Tile, grid: Dict[Tuple[int, int], List[Tile]]) -> bool:
    bx = math.floor(tile.x)
    by = math.floor(tile.y)
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            key = (bx + dx, by + dy)
            for o in grid.get(key, []):
                if o is tile:
                    continue
                if o.z <= tile.z:
                    continue
                if o.state not in ("NORMAL", "BLOCKED"):
                    continue
                if blocking(tile, o):
                    return True
    return False

def calculate_blocked_states(board: List[Tile]) -> List[Tile]:
    grid = build_grid(board)
    result = []
    for tile in board:
        if tile.state not in ("NORMAL", "BLOCKED"):
            result.append(tile)
        else:
            blocked = is_blocked_grid(tile, grid)
            result.append(Tile(
                id=tile.id,
                type=tile.type,
                x=tile.x,
                y=tile.y,
                z=tile.z,
                state="BLOCKED" if blocked else "NORMAL",
                is_blind=tile.is_blind,
                sealed_count=tile.sealed_count
            ))
    return result

def generate_level_simple(card_count: int, shape_type: int = 0) -> List[Tile]:
    """
    简化版关卡生成：生成多层网格坐标并归一化，与 server/src/level.ts 逻辑一致
    """
    level_id = 20  # 高关卡，产生 ~180 张
    base_size = 6 + level_id // 2  # 16
    layers_count = min(12, math.floor(12 - 8 / math.sqrt(level_id - 1)))

    possible = []
    for z in range(layers_count):
        size = max(3, base_size - z // 3)
        offset = 0.0 if z % 2 == 0 else 0.5
        center = (size - 1) / 2

        for r in range(size):
            for c in range(size):
                dx = (c - center) / center if center > 0 else 0
                dy = (r - center) / center if center > 0 else 0
                dist_man = abs(dx) + abs(dy)
                dist_euclid = math.sqrt(dx * dx + dy * dy)
                keep = True

                if shape_type == 0:  # 正方形
                    keep = True
                elif shape_type == 1:  # 金字塔
                    margin = z * 0.45
                    if r < margin or r >= size - margin or c < margin or c >= size - margin:
                        keep = False
                elif shape_type == 2:  # 十字
                    if abs(c - center) >= 1.2 and abs(r - center) >= 1.2:
                        keep = False
                elif shape_type == 3:  # 菱形
                    if dist_man > 1.15:
                        keep = False
                elif shape_type == 4:  # 圆环
                    if dist_euclid < 0.4 or dist_euclid > 1.05:
                        keep = False
                elif shape_type == 5:  # X字
                    if abs(abs(dx) - abs(dy)) > 0.28:
                        keep = False

                if keep:
                    possible.append({"x": c + offset + 1.0, "y": r + offset + 1.0, "z": z})

    # 先洗牌再截断，与 server 逻辑一致
    rng = random.Random(12345)
    shuffled = possible[:]
    for i in range(len(shuffled) - 1, 0, -1):
        j = rng.randint(0, i)
        shuffled[i], shuffled[j] = shuffled[j], shuffled[i]

    count = min(len(shuffled), card_count) - (min(len(shuffled), card_count) % 3)
    coordinates = shuffled[:count]
    coordinates.sort(key=lambda p: p["z"])

    # 归一化
    cx = sum(p["x"] for p in coordinates) / len(coordinates)
    cy = sum(p["y"] for p in coordinates) / len(coordinates)
    max_r = max(max(abs(p["x"] - cx), abs(p["y"] - cy)) for p in coordinates)
    max_r = max(max_r, 0.1)

    target_center = 5.5
    target_radius = 4.8
    norm_scale = target_radius / max_r

    tiles = []
    for i, p in enumerate(coordinates):
        tiles.append(Tile(
            id=f"tile_{i}",
            type=1,
            x=target_center + (p["x"] - cx) * norm_scale,
            y=target_center + (p["y"] - cy) * norm_scale,
            z=p["z"]
        ))
    return tiles

def find_suspicious_tiles(tiles: List[Tile]) -> List[Tile]:
    """
    找出"视觉上有覆盖者但实际上是 NORMAL"的 tile。
    这里用较宽松的判定：只要存在同层或上层 tile 在任一轴有显著重叠，就认为"视觉上可能被遮挡"。
    """
    grid = build_grid(tiles)
    suspicious = []
    for tile in tiles:
        if tile.state != "NORMAL":
            continue
        # 找任何在 tile 上方或同层、且与 tile 在 x 或 y 轴有重叠的 tile
        bx = math.floor(tile.x)
        by = math.floor(tile.y)
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                for o in grid.get((bx + dx, by + dy), []):
                    if o.id == tile.id:
                        continue
                    if o.z < tile.z:
                        continue
                    ox = TILE - abs(o.x - tile.x) * SPACING
                    oy = TILE - abs(o.y - tile.y) * SPACING
                    # 宽松的视觉重叠：任一轴重叠 > 5dp
                    if ox > 5 and oy > -10:
                        suspicious.append(tile)
                        break
                else:
                    continue
                break
            else:
                continue
            break
    return suspicious

def main():
    for shape_type in range(6):
        tiles = generate_level_simple(180, shape_type=shape_type)
        tiles = calculate_blocked_states(tiles)
        total = len(tiles)
        blocked = sum(1 for t in tiles if t.state == "BLOCKED")
        normal = sum(1 for t in tiles if t.state == "NORMAL")
        print(f"Shape {shape_type}: total={total}, BLOCKED={blocked}, NORMAL={normal}, ratio={blocked/total:.2%}")

        suspicious = find_suspicious_tiles(tiles)
        print(f"  -> 视觉可能遮挡但实际 NORMAL 的 tile: {len(suspicious)}")

if __name__ == "__main__":
    main()

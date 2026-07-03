"""
Generate 12 animated 64x64 WebP icons with PIL drawing primitives.
Each icon has bounce + glow + sparkle animation effects.
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

SIZE = 64
OUT_DIR = "E:/file/sheeps/assets/webp"
os.makedirs(OUT_DIR, exist_ok=True)

def new_frame():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

def draw_circle(draw, cx, cy, r, fill, outline=None, width=0):
    bounds = [cx - r, cy - r, cx + r, cy + r]
    draw.ellipse(bounds, fill=fill, outline=outline, width=int(width))

def draw_ellipse(draw, bounds, fill, outline=None, width=0):
    draw.ellipse(bounds, fill=fill, outline=outline, width=int(width))

def draw_polygon(draw, points, fill, outline=None, width=0):
    draw.polygon(points, fill=fill, outline=outline, width=width)

def add_glow(img, color=(255, 255, 220, 60), blur=5):
    """Add soft glow to image."""
    r, g, b, a = img.split()
    glow_a = a.point(lambda x: int(x * 0.6))
    glow_r = Image.new("L", img.size, color[0])
    glow_g = Image.new("L", img.size, color[1])
    glow_b = Image.new("L", img.size, color[2])
    glow = Image.merge("RGBA", (glow_r, glow_g, glow_b, glow_a))
    glow = glow.filter(ImageFilter.GaussianBlur(blur))
    return glow

def add_sparkles(img, phase):
    """Add animated sparkle crosses."""
    draw = ImageDraw.Draw(img)
    positions = [
        (12, 10), (52, 14), (18, 50), (48, 48),
        (32, 5), (56, 30), (8, 32), (30, 55)
    ]
    for i, (px, py) in enumerate(positions):
        alpha = int(200 * abs(math.sin(phase * 3 + i * 0.8)))
        if alpha < 50:
            continue
        c = (255, 255, 220, alpha)
        r = 1 + int(abs(math.sin(phase * 2 + i)))
        draw.ellipse([px - r, py - r, px + r, py + r], fill=c)
        draw.line([px - r * 3, py, px + r * 3, py], fill=c, width=1)
        draw.line([px, py - r * 3, px, py + r * 3], fill=c, width=1)
    return img

def paste_center(base, overlay, ox=0, oy=0):
    """Paste overlay centered on base."""
    x = (SIZE - overlay.width) // 2 + ox
    y = (SIZE - overlay.height) // 2 + oy
    base.paste(overlay, (x, y), overlay)

def draw_face(draw, cx, cy, eye_color=(80, 40, 20, 255), scale=1.0):
    """Draw cute face at position."""
    s = scale
    # Eyes
    draw_circle(draw, int(cx - 5 * s), int(cy - 1 * s), int(2.5 * s), eye_color)
    draw_circle(draw, int(cx + 5 * s), int(cy - 1 * s), int(2.5 * s), eye_color)
    # Mouth (smile)
    r = int(5 * s)
    draw.arc(
        [int(cx - r), int(cy + 2 * s), int(cx + r), int(cy + 2 * s + r)],
        start=10, end=170, fill=eye_color, width=max(1, int(1.5 * s))
    )
    # Blush
    blush_c = (255, 180, 150, 100)
    draw_circle(draw, int(cx - 9 * s), int(cy + 3 * s), int(3 * s), blush_c)
    draw_circle(draw, int(cx + 9 * s), int(cy + 3 * s), int(3 * s), blush_c)

def make_frame_icon(draw_func, phase):
    """Generate a single frame with animation effects."""
    t = (math.sin(phase * 2 * math.pi) + 1) / 2  # 0..1

    # Bounce scale
    scale = 0.86 + 0.14 * t
    # Wiggle rotation
    angle = math.sin(phase * 2 * math.pi) * 4

    # Create icon canvas
    icon_size = int(SIZE * 0.9 * scale)
    icon_img = Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 0))
    icon_draw = ImageDraw.Draw(icon_img)
    draw_func(icon_draw, icon_size, phase)

    # Rotate
    if abs(angle) > 0.3:
        icon_img = icon_img.rotate(angle, expand=True, resample=Image.BICUBIC,
                                     fillcolor=(0, 0, 0, 0))

    # Build final frame
    frame = new_frame()
    frame_draw = ImageDraw.Draw(frame)

    # Glow layers
    if t > 0.3:
        glow = add_glow(icon_img, color=(255, 255, 200, 80), blur=4)
        paste_center(frame, glow)
    if t > 0.6:
        glow2 = add_glow(icon_img, color=(255, 255, 240, 40), blur=8)
        paste_center(frame, glow2)

    paste_center(frame, icon_img)

    # Sparkles near peak
    if abs(t - 0.5) < 0.35:
        add_sparkles(frame, phase)

    return frame


# =============================================================
# Icon drawing functions (draw at given size, centered)
# =============================================================

def draw_star(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    r_out = sz * 0.43
    r_in = sz * 0.17
    pts = []
    for i in range(10):
        angle = math.pi / 2 + i * math.pi / 5
        r = r_out if i % 2 == 0 else r_in
        pts.append((cx + r * math.cos(angle), cy - r * math.sin(angle)))
    draw_polygon(draw, pts, fill=(255, 200, 0, 255), outline=(200, 120, 0, 255), width=2)
    draw_face(draw, cx, cy * 0.95, scale=0.9)

def draw_heart(draw, sz, phase):
    cx, cy = sz / 2, sz / 2 + sz * 0.08
    r = sz * 0.22
    # Two circles + triangle
    draw_circle(draw, cx - r, cy - r * 0.6, r, (240, 60, 60, 255))
    draw_circle(draw, cx + r, cy - r * 0.6, r, (240, 60, 60, 255))
    draw_polygon(draw, [
        (cx - r * 2, cy - r * 0.3),
        (cx + r * 2, cy - r * 0.3),
        (cx, cy + r * 1.8)
    ], fill=(240, 60, 60, 255))
    draw_circle(draw, cx - r, cy - r * 0.6, r, (255, 80, 80, 200))
    draw_circle(draw, cx + r, cy - r * 0.6, r, (255, 80, 80, 200))
    draw_polygon(draw, [
        (cx - r * 2, cy - r * 0.3),
        (cx + r * 2, cy - r * 0.3),
        (cx, cy + r * 1.8)
    ], fill=(255, 80, 80, 200))
    draw_face(draw, cx, cy - r * 0.2, eye_color=(150, 20, 20, 255), scale=0.85)

def draw_diamond(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    w = sz * 0.4
    h = sz * 0.45
    pts = [(cx, cy - h), (cx + w, cy), (cx, cy + h), (cx - w, cy)]
    draw_polygon(draw, pts, fill=(80, 200, 255, 255), outline=(0, 100, 180, 255), width=2)
    # Shine
    shine_t = (math.sin(phase * 3) + 1) / 2
    shine_alpha = int(80 + 60 * shine_t)
    draw_polygon(draw, [(cx, cy - h * 0.5), (cx + w * 0.3, cy), (cx, cy)], fill=(255, 255, 255, shine_alpha))
    draw_face(draw, cx, cy + h * 0.15, eye_color=(0, 60, 120, 255), scale=0.8)

def draw_sun(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    r = sz * 0.3
    # Rays
    for i in range(8):
        angle = i * math.pi / 4 + phase * 0.1
        x1 = cx + (r + 1) * math.cos(angle)
        y1 = cy - (r + 1) * math.sin(angle)
        x2 = cx + (r + r * 0.7) * math.cos(angle)
        y2 = cy - (r + r * 0.7) * math.sin(angle)
        draw.line([(x1, y1), (x2, y2)], fill=(255, 200, 50, 255), width=3)
    draw_circle(draw, cx, cy, r, (255, 220, 50, 255), outline=(230, 140, 0, 255), width=2)
    draw_face(draw, cx, cy, eye_color=(180, 80, 0, 255), scale=0.85)
    # Big smile
    draw.arc([cx - r * 0.5, cy - r * 0.1, cx + r * 0.5, cy + r * 0.5], start=0, end=180, fill=(180, 80, 0, 255), width=2)

def draw_moon(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    r = sz * 0.35
    draw_circle(draw, cx - r * 0.1, cy - r * 0.1, r, (255, 245, 180, 255))
    draw_circle(draw, cx + r * 0.5, cy - r * 0.3, r * 0.85, (0, 0, 0, 0))
    # Stars
    for i in range(3):
        sx = cx + 10 + i * 8
        sy = cy - 15 + i * 5
        star_r = 2
        draw_circle(draw, sx, sy, star_r, (255, 255, 200, 200))
    draw_face(draw, cx - r * 0.15, cy + r * 0.1, eye_color=(200, 150, 30, 255), scale=0.75)

def draw_flower(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    r = sz * 0.3
    colors = [(255, 150, 200, 255), (255, 180, 220, 255), (255, 130, 180, 255), (255, 170, 210, 255), (230, 120, 170, 255)]
    for i in range(5):
        angle = i * math.pi * 2 / 5
        px = cx + r * 0.7 * math.cos(angle)
        py = cy - r * 0.7 * math.sin(angle)
        draw_ellipse(draw, [
            px - r * 0.45, py - r * 0.7,
            px + r * 0.45, py + r * 0.7
        ], fill=colors[i])
    draw_circle(draw, cx, cy, r * 0.35, (255, 240, 100, 255), outline=(220, 180, 0, 255), width=2)
    draw_face(draw, cx, cy, eye_color=(150, 100, 0, 255), scale=0.55)

def draw_cherry(draw, sz, phase):
    offset = abs(math.sin(phase * 2)) * 1.5
    # Stem
    draw.line([(sz / 2, sz * 0.65), (sz / 2, sz * 0.88)], fill=(60, 160, 60, 255), width=2)
    draw.line([(sz / 2, sz * 0.7), (sz * 0.38, sz * 0.88)], fill=(60, 160, 60, 255), width=2)
    # Cherries
    draw_circle(draw, sz * 0.35, sz * 0.35, sz * 0.22, (230, 50, 50, 255))
    draw_circle(draw, sz * 0.62, sz * 0.4 - offset, sz * 0.2, (250, 70, 70, 255))
    # Shine
    draw_ellipse(draw, [sz * 0.25, sz * 0.22, sz * 0.35, sz * 0.28], fill=(255, 255, 255, 100))
    draw_ellipse(draw, [sz * 0.55, sz * 0.28, sz * 0.65, sz * 0.34], fill=(255, 255, 255, 80))
    # Leaf
    draw_ellipse(draw, [sz * 0.35, sz * 0.75, sz * 0.52, sz * 0.85], fill=(80, 200, 80, 255))
    draw_face(draw, sz * 0.35, sz * 0.32, eye_color=(100, 10, 10, 255), scale=0.6)
    draw_face(draw, sz * 0.62, sz * 0.37 - offset, eye_color=(100, 10, 10, 255), scale=0.55)

def draw_clover(draw, sz, phase):
    cx, cy = sz / 2, sz * 0.4
    r = sz * 0.25
    # Four leaves
    for i in range(4):
        angle = i * math.pi / 2
        leaf_cx = cx + r * math.cos(angle)
        leaf_cy = cy - r * math.sin(angle)
        draw_circle(draw, leaf_cx, leaf_cy, r, (100, 210, 100, 255))
        # Highlight
        draw_circle(draw, leaf_cx - r * 0.2, leaf_cy - r * 0.2, r * 0.3, (140, 240, 140, 150))
    # Stem
    draw.line([(cx, cy + r), (cx, sz * 0.88)], fill=(50, 150, 50, 255), width=3)

def draw_candy(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    w, h = sz * 0.25, sz * 0.35
    colors = [(255, 80, 80, 255), (255, 200, 50, 255), (80, 200, 255, 255), (100, 220, 100, 255)]
    draw_ellipse(draw, [cx - w, cy - h, cx + w, cy + h], (255, 255, 255, 255))
    # Stripes
    for i in range(-2, 3):
        line_y = cy + i * h * 0.35
        draw.line([(cx - w, line_y), (cx + w, line_y)], fill=colors[(i + 2) % 4], width=3)
    # Wrapper ends
    draw_polygon(draw, [(cx - w, cy - h), (cx - w - sz * 0.15, cy - h - sz * 0.1), (cx - w - sz * 0.12, cy - h)], fill=(255, 255, 255, 255))
    draw_polygon(draw, [(cx + w, cy - h), (cx + w + sz * 0.15, cy - h - sz * 0.1), (cx + w + sz * 0.12, cy - h)], fill=(255, 255, 255, 255))
    draw_polygon(draw, [(cx - w, cy + h), (cx - w - sz * 0.15, cy + h + sz * 0.1), (cx - w - sz * 0.12, cy + h)], fill=(255, 255, 255, 255))
    draw_polygon(draw, [(cx + w, cy + h), (cx + w + sz * 0.15, cy + h + sz * 0.1), (cx + w + sz * 0.12, cy + h)], fill=(255, 255, 255, 255))
    draw_ellipse(draw, [cx - w, cy - h, cx + w, cy + h], fill=(255,255,255,255), outline=(100, 50, 20, 255), width=2)

def draw_balloon(draw, sz, phase):
    cx, cy = sz / 2, sz * 0.35
    r = sz * 0.3
    draw_ellipse(draw, [cx - r, cy - r * 1.1, cx + r, cy + r * 1.1], (240, 60, 60, 255))
    # Shine
    draw_ellipse(draw, [cx - r * 0.5, cy - r * 0.7, cx - r * 0.1, cy - r * 0.3], (255, 255, 255, 120))
    # Tie
    draw_polygon(draw, [(cx - 4, cy + r * 1.05), (cx + 4, cy + r * 1.05), (cx, cy + r * 1.2)], (180, 30, 30, 255))
    # String
    draw.line([(cx, cy + r * 1.2), (cx + 2, sz * 0.75), (cx - 1, sz * 0.9)], fill=(120, 80, 50, 255), width=2)
    draw_face(draw, cx, cy - r * 0.15, eye_color=(100, 10, 10, 255), scale=0.8)

def draw_cat(draw, sz, phase):
    cx, cy = sz / 2, sz / 2
    r = sz * 0.3
    # Ears
    draw_polygon(draw, [(cx - r * 0.85, cy - r * 0.6), (cx - r * 1.1, cy - r * 1.5), (cx - r * 0.3, cy - r * 0.9)], (255, 200, 100, 255))
    draw_polygon(draw, [(cx + r * 0.85, cy - r * 0.6), (cx + r * 1.1, cy - r * 1.5), (cx + r * 0.3, cy - r * 0.9)], (255, 200, 100, 255))
    # Inner ears
    draw_polygon(draw, [(cx - r * 0.75, cy - r * 0.55), (cx - r * 0.9, cy - r * 1.1), (cx - r * 0.4, cy - r * 0.78)], (255, 150, 130, 255))
    draw_polygon(draw, [(cx + r * 0.75, cy - r * 0.55), (cx + r * 0.9, cy - r * 1.1), (cx + r * 0.4, cy - r * 0.78)], (255, 150, 130, 255))
    # Head
    draw_circle(draw, cx, cy, r, (255, 200, 100, 255), outline=(200, 120, 40, 255), width=2)
    # Eyes
    draw_ellipse(draw, [cx - r * 0.5, cy - r * 0.2, cx - r * 0.15, cy + r * 0.1], (255, 255, 255, 255))
    draw_ellipse(draw, [cx + r * 0.15, cy - r * 0.2, cx + r * 0.5, cy + r * 0.1], (255, 255, 255, 255))
    draw_circle(draw, cx - r * 0.3, cy - r * 0.05, r * 0.15, (40, 30, 20, 255))
    draw_circle(draw, cx + r * 0.3, cy - r * 0.05, r * 0.15, (40, 30, 20, 255))
    # Nose
    draw_polygon(draw, [(cx - 2, cy + r * 0.15), (cx + 2, cy + r * 0.15), (cx, cy + r * 0.25)], (255, 140, 130, 255))
    # Mouth
    draw.arc([cx - r * 0.3, cy + r * 0.1, cx, cy + r * 0.35], start=10, end=170, fill=(40, 30, 20, 255), width=1)
    draw.arc([cx, cy + r * 0.1, cx + r * 0.3, cy + r * 0.35], start=10, end=170, fill=(40, 30, 20, 255), width=1)
    # Whiskers
    for side in [-1, 1]:
        wx = cx + side * r * 0.55
        for wy_off in [-r * 0.1, r * 0.05, r * 0.2]:
            wy = cy + wy_off
            draw.line([(wx, wy), (wx + side * r * 0.5, wy - r * 0.1)], fill=(160, 90, 30, 200), width=1)

def draw_rainbow(draw, sz, phase):
    cx, cy = sz / 2, sz * 0.65
    colors = [(255, 80, 80, 200), (255, 200, 40, 200), (255, 240, 80, 200),
              (80, 220, 80, 200), (80, 180, 255, 200), (160, 100, 220, 200)]
    for i, c in enumerate(colors):
        r = sz * (0.45 - i * 0.05)
        draw.arc([cx - r, cy - r, cx + r, cy + r], start=180, end=360, fill=c, width=3)
    # Clouds
    for side, sx in [(-1, sz * 0.2), (1, sz * 0.8)]:
        cloud_cx = sx
        cloud_cy = sz * 0.75
        draw_circle(draw, cloud_cx - 8, cloud_cy, 8, (255, 255, 255, 230))
        draw_circle(draw, cloud_cx + 8, cloud_cy, 8, (255, 255, 255, 230))
        draw_circle(draw, cloud_cx, cloud_cy - 5, 7, (255, 255, 255, 230))
    # Face on right cloud
    draw_circle(draw, sz * 0.76, sz * 0.74, 1.5, (180, 180, 180, 255))
    draw_circle(draw, sz * 0.82, sz * 0.74, 1.5, (180, 180, 180, 255))
    draw.arc([sz * 0.76, sz * 0.76, sz * 0.82, sz * 0.80], start=10, end=170, fill=(180, 180, 180, 255), width=1)


# =============================================================
# Generate all 12 animated WebP icons
# =============================================================
ICONS = [
    ("star", draw_star),
    ("heart", draw_heart),
    ("diamond", draw_diamond),
    ("sun", draw_sun),
    ("moon", draw_moon),
    ("flower", draw_flower),
    ("cherry", draw_cherry),
    ("clover", draw_clover),
    ("candy", draw_candy),
    ("balloon", draw_balloon),
    ("cat", draw_cat),
    ("rainbow", draw_rainbow),
]

NUM_FRAMES = 14

print(f"Generating {len(ICONS)} animated WebP icons ({SIZE}x{SIZE}, {NUM_FRAMES} frames each)...")

for name, draw_func in ICONS:
    frames = []
    for i in range(NUM_FRAMES):
        phase = i / NUM_FRAMES
        frame = make_frame_icon(draw_func, phase)
        frames.append(frame)

    out_path = f"{OUT_DIR}/icon_{name}.webp"
    frames[0].save(
        out_path,
        format="WEBP",
        save_all=True,
        append_images=frames[1:],
        duration=100,
        loop=0,
        lossless=False,
        quality=88,
        method=6
    )
    kb = round(os.path.getsize(out_path) / 1024, 1)
    print(f"  [OK] {name:8s}  {kb:6} KB")

print(f"Done! {len(ICONS)} files saved to {OUT_DIR}")

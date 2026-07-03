"""Convert daimeng SVGs to 64x64 static WebP for Android drawable."""
import io, os, glob
from reportlab.graphics import renderPM
from svglib.svglib import svg2rlg
from PIL import Image

SRC = "E:/file/sheeps/assets/icons/svg"
DST = "E:/file/sheeps/assets/icons/svg_converted"
os.makedirs(DST, exist_ok=True)

for f in sorted(glob.glob(f"{SRC}/tile_daimeng_*.svg")):
    name = os.path.basename(f).replace(".svg", "")
    try:
        drawing = svg2rlg(f)
        # Scale to 64x64
        w, h = drawing.width, drawing.height
        scale = 64.0 / max(w, h)
        drawing.width = w * scale
        drawing.height = h * scale
        drawing.scale(scale, scale)

        # Render to PNG bytes
        png_bytes = renderPM.drawToString(drawing, fmt="PNG", dpi=72)
        img = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
        img = img.resize((64, 64), Image.LANCZOS)

        out = f"{DST}/{name}.webp"
        img.save(out, "WEBP", quality=90)
        kb = round(os.path.getsize(out) / 1024, 1)
        print(f"  {name}.svg -> {name}.webp ({kb}KB)")
    except Exception as e:
        print(f"  {name}: FAILED - {e}")

print(f"\nDone! {len(glob.glob(DST+'/*.webp'))} files")

import os
from PIL import Image, ImageDraw

def create_tile_background():
    # Card base (64x64) with 3D gold-red border
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Draw shadow
    draw.rounded_rectangle([4, 12, 124, 124], radius=16, fill=(128, 16, 16, 80))
    # Draw body (cream)
    draw.rounded_rectangle([4, 4, 124, 116], radius=16, fill=(253, 251, 247, 255))
    # Draw gold border
    draw.rounded_rectangle([4, 4, 124, 116], radius=16, outline=(230, 162, 60, 255), width=6)
    # Draw inner red frame
    draw.rounded_rectangle([10, 10, 118, 110], radius=12, outline=(200, 36, 35, 255), width=2)
    return img

def create_tile_back():
    # Mystery Card back (问号)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Draw body (dark red)
    draw.rounded_rectangle([4, 4, 124, 116], radius=16, fill=(128, 16, 16, 255))
    draw.rounded_rectangle([4, 4, 124, 116], radius=16, outline=(230, 162, 60, 255), width=6)
    # Draw golden "?" symbol
    draw.text((48, 28), "?", fill=(230, 162, 60, 255), font_size=64)
    return img

def create_tile_sealed():
    # Sealed talisman overlay (封)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Draw diagonal chains/lines
    draw.line([0, 0, 128, 128], fill=(230, 162, 60, 180), width=4)
    draw.line([128, 0, 0, 128], fill=(230, 162, 60, 180), width=4)
    # Yellow paper talisman
    draw.rectangle([34, 24, 94, 104], fill=(241, 196, 15, 240), outline=(230, 162, 60, 255), width=2)
    # Red character "封" (Seal)
    draw.text((46, 36), "封", fill=(200, 36, 35, 255), font_size=32)
    return img

# Sub-icons to draw on top of background
def draw_motif(img, motif_type):
    draw = ImageDraw.Draw(img)
    if motif_type == 1: # Red Lantern
        draw.ellipse([34, 34, 94, 94], fill=(200, 36, 35, 255), outline=(230, 162, 60, 255), width=4)
        draw.line([64, 20, 64, 34], fill=(230, 162, 60, 255), width=4)
        draw.line([64, 94, 64, 110], fill=(200, 36, 35, 255), width=6)
    elif motif_type == 2: # Chinese Knot
        draw.polygon([(64, 30), (94, 60), (64, 90), (34, 60)], fill=(200, 36, 35, 255))
        draw.ellipse([44, 40, 84, 80], fill=(0, 0, 0, 0), outline=(230, 162, 60, 255), width=3)
    elif motif_type == 3: # Copper Coin
        draw.ellipse([34, 30, 94, 90], fill=(230, 162, 60, 255), outline=(180, 120, 30, 255), width=4)
        draw.rectangle([52, 48, 76, 72], fill=(253, 251, 247, 255), outline=(200, 36, 35, 255), width=3)
    elif motif_type == 4: # Folding Fan
        draw.chord([24, 34, 104, 114], 180, 360, fill=(200, 36, 35, 255), outline=(230, 162, 60, 255), width=3)
        draw.line([64, 110, 64, 118], fill=(230, 162, 60, 255), width=5)
    elif motif_type == 5: # Porcelain Vase
        draw.rounded_rectangle([44, 34, 84, 94], radius=12, fill=(240, 244, 248, 255), outline=(43, 91, 132, 255), width=4)
        draw.line([48, 54, 80, 54], fill=(43, 91, 132, 255), width=3)
        draw.line([48, 74, 80, 74], fill=(43, 91, 132, 255), width=3)
    elif motif_type == 6: # Teapot
        draw.ellipse([40, 44, 88, 84], fill=(141, 91, 76, 255))
        draw.line([30, 64, 40, 54], fill=(141, 91, 76, 255), width=8)
        draw.arc([80, 50, 100, 78], 270, 90, fill=(141, 91, 76, 255), width=6)
    elif motif_type == 7: # Firecracker
        draw.rectangle([48, 34, 80, 94], fill=(200, 36, 35, 255), outline=(230, 162, 60, 255), width=3)
        draw.rectangle([48, 48, 80, 60], fill=(230, 162, 60, 255))
        draw.line([64, 24, 64, 34], fill=(128, 128, 128, 255), width=3)
    elif motif_type == 8: # Mooncake
        draw.ellipse([34, 30, 94, 90], fill=(230, 162, 60, 255), outline=(197, 133, 37, 255), width=6)
        draw.ellipse([46, 42, 82, 78], fill=(0, 0, 0, 0), outline=(197, 133, 37, 255), width=3)
    elif motif_type == 9: # Lion Mask
        draw.ellipse([34, 30, 94, 90], fill=(200, 36, 35, 255))
        draw.ellipse([42, 42, 58, 58], fill=(255, 255, 255, 255))
        draw.ellipse([70, 42, 86, 58], fill=(255, 255, 255, 255))
        draw.ellipse([47, 47, 53, 53], fill=(0, 0, 0, 255))
        draw.ellipse([75, 47, 81, 81], fill=(0, 0, 0, 255))
        draw.rectangle([54, 68, 74, 80], fill=(230, 162, 60, 255))
    elif motif_type == 10: # Koi Fish
        draw.chord([34, 34, 94, 94], 45, 225, fill=(230, 162, 60, 255))
        draw.chord([34, 34, 94, 94], 225, 45, fill=(200, 36, 35, 255))
    elif motif_type == 11: # Peach
        draw.ellipse([38, 44, 90, 90], fill=(255, 138, 128, 255))
        draw.polygon([(64, 24), (38, 54), (90, 54)], fill=(200, 36, 35, 255))
        draw.ellipse([44, 80, 84, 96], fill=(39, 174, 96, 255))
    elif motif_type == 12: # Red Envelope
        draw.rectangle([38, 30, 90, 94], fill=(200, 36, 35, 255), outline=(230, 162, 60, 255), width=3)
        draw.ellipse([54, 52, 74, 72], fill=(230, 162, 60, 255))
        draw.text((58, 50), "福", fill=(200, 36, 35, 255), font_size=16)

def generate_all():
    target_dir = "c:/Users/15613/Documents/file/app/core/src/main/res/drawable"
    os.makedirs(target_dir, exist_ok=True)
    
    # Save standard overlays
    create_tile_background().save(os.path.join(target_dir, "tile_background.webp"), "WEBP")
    create_tile_back().save(os.path.join(target_dir, "tile_back.webp"), "WEBP")
    create_tile_sealed().save(os.path.join(target_dir, "tile_sealed_overlay.webp"), "WEBP")
    
    # Save 12 motifs
    for i in range(1, 13):
        bg = create_tile_background()
        draw_motif(bg, i)
        bg.save(os.path.join(target_dir, f"tile_{i}.webp"), "WEBP")
        print(f"Generated tile_{i}.webp")

if __name__ == "__main__":
    generate_all()

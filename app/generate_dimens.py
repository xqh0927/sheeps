import os
import re

def generate_dimens():
    res_dir = "c:/Users/15613/Documents/file/app/core/src/main/res"
    values_dir = os.path.join(res_dir, "values")
    os.makedirs(values_dir, exist_ok=True)
    
    # 1. Generate base dimens.xml (360dp base)
    base_lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<resources>'
    ]
    for i in range(1, 361):
        base_lines.append(f'    <dimen name="dp_{i}">{i}dp</dimen>')
    for i in range(8, 49):
        base_lines.append(f'    <dimen name="sp_{i}">{i}sp</dimen>')
    base_lines.append('</resources>')
    
    base_content = "\n".join(base_lines)
    with open(os.path.join(values_dir, "dimens.xml"), "w", encoding="utf-8") as f:
        f.write(base_content)
    print("Baseline dimens.xml generated.")

    # 2. Emulate ScreenMatch: generate scaled dimens for target width folders
    target_widths = [320, 384, 400, 411, 432, 480, 533, 592, 600, 640, 662, 720, 768, 800]
    base_width = 360.0
    
    for width in target_widths:
        ratio = width / base_width
        target_val_dir = os.path.join(res_dir, f"values-sw{width}dp")
        os.makedirs(target_val_dir, exist_ok=True)
        
        scaled_lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            '<!-- Generated automatically by ScreenMatch compiler emulator -->',
            '<resources>'
        ]
        
        for i in range(1, 361):
            scaled_val = round(i * ratio, 2)
            scaled_lines.append(f'    <dimen name="dp_{i}">{scaled_val}dp</dimen>')
            
        for i in range(8, 49):
            scaled_val = round(i * ratio, 2)
            scaled_lines.append(f'    <dimen name="sp_{i}">{scaled_val}sp</dimen>')
            
        scaled_lines.append('</resources>')
        
        with open(os.path.join(target_val_dir, "dimens.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(scaled_lines))
        print(f"Generated values-sw{width}dp/dimens.xml (ratio: {ratio:.4f})")

if __name__ == "__main__":
    generate_dimens()

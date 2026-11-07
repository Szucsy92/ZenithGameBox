"""Derive Android colors and transparent marks from existing console assets."""
from pathlib import Path
from collections import deque
import re
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'android/app/src/main/res'

def background_hex():
    css = (ROOT / 'src/styles.css').read_text()
    light, chroma, hue = map(float, re.search(r'--background:\s*oklch\(([\d.]+) ([\d.]+) ([\d.]+)\)', css).groups())
    angle = np.deg2rad(hue)
    a, b = chroma * np.cos(angle), chroma * np.sin(angle)
    l, m, s = (light + .3963377774*a + .2158037573*b)**3, (light - .1055613458*a - .0638541728*b)**3, (light - .0894841775*a - 1.291485548*b)**3
    rgb = np.array([4.0767416621*l-3.3077115913*m+.2309699292*s, -1.2684380046*l+2.6097574011*m-.3413193965*s, -.0041960863*l-.7034186147*m+1.707614701*s])
    rgb = np.where(rgb <= .0031308, 12.92*rgb, 1.055*np.maximum(rgb, 0)**(1/2.4)-.055)
    return '#' + ''.join(f'{round(v*255):02x}' for v in np.clip(rgb, 0, 1))

def transparent_mark():
    image = Image.open(ROOT / 'src/assets/zenith-icon.jpg').convert('RGBA')
    pixels = np.array(image)
    h, w = pixels.shape[:2]
    # Remove only dark background connected to image edges, preserving the console body.
    dark = pixels[:, :, :3].max(axis=2) < 65
    seen = np.zeros((h, w), dtype=bool)
    queue = deque([(0,0), (0,w-1), (h-1,0), (h-1,w-1)])
    while queue:
        y,x = queue.popleft()
        if y < 0 or x < 0 or y >= h or x >= w or seen[y,x] or not dark[y,x]:
            continue
        seen[y,x] = True
        queue.extend(((y-1,x),(y+1,x),(y,x-1),(y,x+1)))
    pixels[seen,3] = 0
    result = Image.fromarray(pixels)
    bounds = result.getbbox()
    return result.crop(bounds) if bounds else result

if __name__ == '__main__':
    for folder in ('values', 'drawable'):
        (RES / folder).mkdir(parents=True, exist_ok=True)
    (RES / 'values/colors.xml').write_text(f'<resources><color name="console_background">{background_hex()}</color></resources>\n')
    mark = transparent_mark()
    for name, size, reach in (('launch_mark', 288, 164), ('launcher_foreground', 432, 240)):
        artwork = mark.copy()
        artwork.thumbnail((reach, reach), Image.Resampling.LANCZOS)
        canvas = Image.new('RGBA', (size,size), (0,0,0,0))
        canvas.alpha_composite(artwork, ((size-artwork.width)//2,(size-artwork.height)//2))
        canvas.save(RES / f'drawable/{name}.png')
    legacy = Image.new('RGBA', (192,192), background_hex())
    artwork = mark.copy()
    artwork.thumbnail((144,144), Image.Resampling.LANCZOS)
    legacy.alpha_composite(artwork, ((192-artwork.width)//2,(192-artwork.height)//2))
    legacy.save(RES / 'drawable/launcher_icon.png')

"""Contact sheet of review stills: python tools/sheet.py out.png 60 170 ..."""
import sys
from PIL import Image, ImageDraw
out, nums = sys.argv[1], sys.argv[2:]
tw, th, cols = 960, 540, 2
rows = (len(nums) + cols - 1) // cols
sheet = Image.new('RGB', (tw * cols, th * rows), (30, 30, 30))
d = ImageDraw.Draw(sheet)
for i, n in enumerate(nums):
    im = Image.open(f'frames/f{n}.png').convert('RGB').resize((tw, th))
    x, y = (i % cols) * tw, (i // cols) * th
    sheet.paste(im, (x, y))
    d.text((x + 10, y + 8), f'frame {n}', fill=(255, 255, 0))
sheet.save(out)

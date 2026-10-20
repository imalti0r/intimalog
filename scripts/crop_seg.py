from PIL import Image
im = Image.open(r"artifacts\seg_ripple.png")
im.crop((1200, 40, im.size[0], 240)).save(r"artifacts\seg_crop.png")

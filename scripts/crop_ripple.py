from PIL import Image
im = Image.open(r"artifacts\ripple_check.png")
im.crop((0, 600, im.size[0], 1300)).save(r"artifacts\ripple_crop.png")

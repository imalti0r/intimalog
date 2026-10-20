from PIL import Image
im = Image.open(r"artifacts\insets_check.png")
w, h = im.size
im.crop((0, 0, w, 300)).save(r"artifacts\insets_top.png")

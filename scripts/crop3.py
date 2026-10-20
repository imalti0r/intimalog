from PIL import Image
im = Image.open(r"artifacts\desktop_check.png")
w, h = im.size
im.resize((w//3, h//3), Image.LANCZOS).save(r"artifacts\desktop_check_small.png")

from PIL import Image
im = Image.open(r"artifacts\desktop_new_icon.png")
w, h = im.size
print("desktop size:", w, h)
# 缩小保存便于查看
im.resize((w//3, h//3), Image.LANCZOS).save(r"artifacts\desktop_small.png")

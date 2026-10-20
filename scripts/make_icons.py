from PIL import Image
import os
src = Image.open(r"artifacts\icon_preview.png").convert("RGBA")
sizes = {
    "mipmap-xxxhdpi": 192,
    "mipmap-xxhdpi": 144,
    "mipmap-xhdpi": 96,
    "mipmap-hdpi": 72,
    "mipmap-mdpi": 48,
}
for folder, s in sizes.items():
    out = os.path.join("app", "src", "main", "res", folder, "ic_launcher.png")
    out_round = os.path.join("app", "src", "main", "res", folder, "ic_launcher_round.png")
    im = src.resize((s, s), Image.LANCZOS)
    im.save(out)
    im.save(out_round)
    print(folder, s, "ok")

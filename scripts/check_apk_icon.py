import zipfile
from PIL import Image
import io
z = zipfile.ZipFile(r"app\build\outputs\apk\debug\app-debug.apk")
names = [n for n in z.namelist() if "ic_launcher" in n and n.endswith(".png")]
print(names[:6])
data = z.read([n for n in names if "xxxhdpi" in n or "xxx" in n][0]) if any("xxxhdpi" in n for n in names) else z.read(names[0])
im = Image.open(io.BytesIO(data))
print("icon size in apk:", im.size)
im.save(r"artifacts\icon_in_apk.png")
